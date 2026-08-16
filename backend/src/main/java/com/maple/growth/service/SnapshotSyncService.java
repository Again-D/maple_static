package com.maple.growth.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import com.maple.growth.dto.api.ApiErrorCode;
import com.maple.growth.dto.api.CharacterLookupResponseDto;
import com.maple.growth.dto.api.CharacterProfileDto;
import com.maple.growth.dto.api.ChartPointDto;
import com.maple.growth.dto.api.DashboardResponseDto;
import com.maple.growth.dto.api.GrowthHistoryDto;
import com.maple.growth.dto.api.GrowthSummaryDto;
import com.maple.growth.dto.api.GrowthEventDto;
import com.maple.growth.dto.api.RefreshResponseDto;
import com.maple.growth.dto.api.SnapshotSummaryDto;
import com.maple.growth.dto.api.SyncStateDto;
import com.maple.growth.dto.api.TimelineDto;
import com.maple.growth.dto.nexon.NexonCharacterSnapshot;
import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.entity.DailySnapshotEntity;
import com.maple.growth.entity.GrowthEventLogEntity;
import com.maple.growth.repository.CharacterRepository;
import com.maple.growth.repository.DailySnapshotRepository;
import com.maple.growth.repository.GrowthEventLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SnapshotSyncService {

    private final CharacterRepository characterRepository;
    private final DailySnapshotRepository dailySnapshotRepository;
    private final GrowthEventLogRepository growthEventLogRepository;
    private final NexonApiClient nexonApiClient;
    private final GrowthEventService growthEventService;
    private final KstClock kstClock;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;
    private final EquipmentViewService equipmentViewService;

    @Autowired
    public SnapshotSyncService(
            CharacterRepository characterRepository,
            DailySnapshotRepository dailySnapshotRepository,
            GrowthEventLogRepository growthEventLogRepository,
            NexonApiClient nexonApiClient,
            GrowthEventService growthEventService,
            KstClock kstClock,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
            EquipmentViewService equipmentViewService
    ) {
        this.characterRepository = characterRepository;
        this.dailySnapshotRepository = dailySnapshotRepository;
        this.growthEventLogRepository = growthEventLogRepository;
        this.nexonApiClient = nexonApiClient;
        this.growthEventService = growthEventService;
        this.kstClock = kstClock;
        this.objectMapper = objectMapper;
        this.transactionManager = transactionManager;
        this.equipmentViewService = equipmentViewService;
    }

    public SnapshotSyncService(
            CharacterRepository characterRepository,
            DailySnapshotRepository dailySnapshotRepository,
            GrowthEventLogRepository growthEventLogRepository,
            NexonApiClient nexonApiClient,
            GrowthEventService growthEventService,
            KstClock kstClock,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        this(characterRepository, dailySnapshotRepository, growthEventLogRepository, nexonApiClient, growthEventService, kstClock, objectMapper, transactionManager, new EquipmentViewService());
    }

    @Transactional
    public CharacterLookupResponseDto lookupOrRegister(String rawName) {
        String name = normalizeName(rawName);
        CharacterEntity existing = characterRepository.findByCharacterName(name).orElse(null);
        if (existing != null) {
            return buildLookupResponse(existing);
        }
        SyncResult result = syncFreshCharacter(name);
        return buildLookupResponse(result.character());
    }

    @Transactional
    public DashboardResponseDto dashboard(String rawName) {
        String name = normalizeName(rawName);
        CharacterEntity existing = characterRepository.findByCharacterName(name).orElse(null);
        if (existing == null) {
            SyncResult result = syncFreshCharacter(name);
            return buildDashboard(result.character());
        }
        return buildDashboard(existing);
    }

    @Transactional
    public RefreshResponseDto refresh(String rawName) {
        String name = normalizeName(rawName);
        CharacterEntity character = characterRepository.findByCharacterName(name).orElse(null);
        if (character == null) {
            SyncResult result = syncFreshCharacter(name);
            return buildRefreshResponse(result.character(), result.latestSnapshot(), result.snapshotCreated(), result.snapshotUpdated(), result.createdEventCount());
        }
        try {
            SyncResult result = syncExistingCharacter(character);
            return buildRefreshResponse(result.character(), result.latestSnapshot(), result.snapshotCreated(), result.snapshotUpdated(), result.createdEventCount());
        } catch (NexonApiException exception) {
            recordFailure(character, exception);
            throw exception;
        }
    }

    @Transactional
    public DashboardResponseDto dashboardForExisting(CharacterEntity character) {
        return buildDashboard(character);
    }

    @Transactional
    public CharacterEntity requireExistingCharacter(String rawName) {
        String name = normalizeName(rawName);
        return characterRepository.findByCharacterName(name)
                .orElseThrow(() -> new NexonApiException(ApiErrorCode.CHARACTER_NOT_FOUND, "캐릭터를 찾을 수 없습니다.", false));
    }

    @Transactional
    public GrowthHistoryDto growthHistory(CharacterEntity character, String range, String metric, int rangeDays) {
        LocalDate endDate = kstClock.today();
        LocalDate startDate = "all".equals(range)
                ? LocalDate.of(1970, 1, 1)
                : endDate.minusDays(rangeDays - 1L);
        var snapshots = dailySnapshotRepository.findByCharacterAndSnapshotDateBetweenOrderBySnapshotDateAsc(character, startDate, endDate);
        var points = snapshots.stream().map(SnapshotSyncService::toChartPoint).toList();
        long comparablePointCount = points.stream().filter(point -> metricValue(point, metric) != null).count();
        boolean hasEnoughSnapshots = comparablePointCount >= 2;
        return new GrowthHistoryDto(range, metric, hasEnoughSnapshots, points);
    }

    @Transactional
    public TimelineDto events(CharacterEntity character, int limit) {
        var events = growthEventLogRepository.findByCharacterOrderByEventDateDescIdDesc(
                character,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "eventDate", "id"))
        );
        boolean hasMore = growthEventLogRepository.findByCharacterOrderByEventDateDescIdDesc(
                character,
                PageRequest.of(0, limit + 1, Sort.by(Sort.Direction.DESC, "eventDate", "id"))
        ).size() > limit;
        String nextCursor = events.isEmpty() ? null : String.valueOf(events.get(events.size() - 1).getId());
        return new TimelineDto(events.stream().map(this::toEventDto).toList(), hasMore, nextCursor);
    }

    private SyncResult syncFreshCharacter(String name) {
        NexonCharacterSnapshot snapshot = nexonApiClient.fetchCharacterSnapshot(name, kstClock.today());
        CharacterEntity character = new CharacterEntity(
                snapshot.ocid(),
                snapshot.name(),
                snapshot.worldName(),
                snapshot.jobName(),
                snapshot.gender(),
                snapshot.imageUrl()
        );
        character.setAutoTrack(true);
        character.setLastSyncAttemptedAt(kstClock.now());
        CharacterEntity savedCharacter = characterRepository.save(character);
        SnapshotPersistResult persistResult = saveSnapshot(savedCharacter, snapshot);
        DailySnapshotEntity savedSnapshot = persistResult.snapshot();
        int eventCount = recomputeEvents(savedSnapshot);
        savedCharacter.setLastFetchedAt(kstClock.now());
        savedCharacter.setLastSyncErrorCode(null);
        return new SyncResult(savedCharacter, savedSnapshot, persistResult.created(), persistResult.updated(), eventCount);
    }

    private SyncResult syncExistingCharacter(CharacterEntity character) {
        NexonCharacterSnapshot snapshot = nexonApiClient.fetchCharacterSnapshot(character.getCharacterName(), kstClock.today());
        character.setLastSyncAttemptedAt(kstClock.now());
        character.setCharacterImageUrl(snapshot.imageUrl());
        SnapshotPersistResult persistResult = saveSnapshot(character, snapshot);
        DailySnapshotEntity savedSnapshot = persistResult.snapshot();
        int eventCount = recomputeEvents(savedSnapshot);
        character.setLastFetchedAt(kstClock.now());
        character.setLastSyncErrorCode(null);
        return new SyncResult(character, savedSnapshot, persistResult.created(), persistResult.updated(), eventCount);
    }

    private SnapshotPersistResult saveSnapshot(CharacterEntity character, NexonCharacterSnapshot snapshot) {
        LocalDate today = kstClock.today();
        Optional<DailySnapshotEntity> existing = dailySnapshotRepository.findByCharacterAndSnapshotDate(character, today);
        DailySnapshotEntity entity = existing.orElseGet(() -> new DailySnapshotEntity(character, today));
        boolean created = entity.getId() == null;
        entity.setCharacter(character);
        entity.setSnapshotDate(today);
        entity.setLevel(snapshot.level());
        entity.setExp(snapshot.exp());
        entity.setExpRate(snapshot.expRate());
        entity.setCombatPower(snapshot.combatPower());
        entity.setUnionLevel(snapshot.unionLevel());
        entity.setUnionArtifactLevel(snapshot.unionArtifactLevel());
        entity.setHexaMatrixLevelSum(snapshot.hexaMatrixLevelSum());
        entity.setRawStatJson(snapshot.rawStatJson());
        entity.setRawEquipmentJson(resolveRawEquipmentJson(existing.orElse(null), snapshot.rawEquipmentJson()));
        entity.setRawHexaJson(snapshot.rawHexaJson());
        entity.setCapturedAt(kstClock.now());
        DailySnapshotEntity saved = dailySnapshotRepository.save(entity);
        return new SnapshotPersistResult(saved, created, !created);
    }

    private com.fasterxml.jackson.databind.JsonNode resolveRawEquipmentJson(DailySnapshotEntity existingSnapshot, com.fasterxml.jackson.databind.JsonNode fetchedRawEquipmentJson) {
        if (GrowthEventService.hasComparableActiveEquipment(fetchedRawEquipmentJson)) {
            return fetchedRawEquipmentJson;
        }
        if (existingSnapshot == null) {
            return fetchedRawEquipmentJson;
        }
        if (GrowthEventService.hasComparableActiveEquipment(existingSnapshot.getRawEquipmentJson())) {
            return existingSnapshot.getRawEquipmentJson();
        }
        return fetchedRawEquipmentJson;
    }

    private int recomputeEvents(DailySnapshotEntity snapshot) {
        CharacterEntity character = snapshot.getCharacter();
        Optional<DailySnapshotEntity> previous = dailySnapshotRepository
                .findFirstByCharacterAndSnapshotDateLessThanOrderBySnapshotDateDescIdDesc(character, snapshot.getSnapshotDate());
        return growthEventService.recomputeEvents(snapshot, previous.orElse(null));
    }

    protected void recordFailure(CharacterEntity character, NexonApiException exception) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> {
            character.setLastSyncAttemptedAt(kstClock.now());
            character.setLastSyncErrorCode(exception.getErrorCode().name());
            characterRepository.save(character);
        });
    }

    private CharacterLookupResponseDto buildLookupResponse(CharacterEntity character) {
        DailySnapshotEntity latestSnapshot = dailySnapshotRepository.findFirstByCharacterOrderBySnapshotDateDescIdDesc(character).orElse(null);
        return new CharacterLookupResponseDto(toProfile(character), toSnapshot(latestSnapshot), toSyncState(character, latestSnapshot != null));
    }

    private DashboardResponseDto buildDashboard(CharacterEntity character) {
        DailySnapshotEntity latestSnapshot = dailySnapshotRepository.findFirstByCharacterOrderBySnapshotDateDescIdDesc(character).orElse(null);
        var summary = buildSummary(character, latestSnapshot);
        var chartSnapshots = dailySnapshotRepository.findByCharacterAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                character,
                kstClock.today().minusDays(6),
                kstClock.today()
        );
        var chartPoints = chartSnapshots.stream().map(SnapshotSyncService::toChartPoint).toList();
        boolean hasEnoughSnapshots = chartPoints.stream().filter(point -> point.combatPower() != null).count() >= 2;
        var chart = new GrowthHistoryDto("7d", "combatPower", hasEnoughSnapshots, chartPoints);
        var timeline = events(character, 20);
        return new DashboardResponseDto(toProfile(character), toSnapshot(latestSnapshot), toSyncState(character, latestSnapshot != null), summary, chart, timeline, equipmentViewService.fromSnapshot(latestSnapshot));
    }

    private RefreshResponseDto buildRefreshResponse(CharacterEntity character, DailySnapshotEntity latestSnapshot, boolean created, boolean updated, int createdEventCount) {
        return new RefreshResponseDto(toProfile(character), toSnapshot(latestSnapshot), toSyncState(character, latestSnapshot != null), created, updated, createdEventCount);
    }

    private CharacterEntity characterOrThrow(String rawName) {
        String name = normalizeName(rawName);
        return characterRepository.findByCharacterName(name)
                .orElseThrow(() -> new NexonApiException(ApiErrorCode.CHARACTER_NOT_FOUND, "캐릭터를 찾을 수 없습니다.", false));
    }

    private static String normalizeName(String rawName) {
        if (rawName == null || rawName.trim().isBlank()) {
            throw new ValidationException("캐릭터 닉네임이 비어 있습니다.");
        }
        return rawName.trim();
    }

    private CharacterProfileDto toProfile(CharacterEntity character) {
        return new CharacterProfileDto(
                character.getId(),
                character.getOcid(),
                character.getCharacterName(),
                character.getWorldName(),
                character.getJobName(),
                character.getCharacterGender(),
                character.getCharacterImageUrl(),
                character.isAutoTrack()
        );
    }

    private SnapshotSummaryDto toSnapshot(DailySnapshotEntity snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new SnapshotSummaryDto(
                snapshot.getId(),
                snapshot.getSnapshotDate(),
                snapshot.getLevel(),
                snapshot.getExp(),
                snapshot.getExpRate(),
                snapshot.getCombatPower(),
                snapshot.getUnionLevel(),
                snapshot.getUnionArtifactLevel(),
                snapshot.getHexaMatrixLevelSum(),
                snapshot.getCapturedAt()
        );
    }

    private SyncStateDto toSyncState(CharacterEntity character, boolean hasSnapshot) {
        String state;
        String message;
        if (character.getLastSyncErrorCode() != null && character.getLastFetchedAt() == null) {
            state = "failed_empty";
            message = "최근 동기화에 실패했습니다.";
        } else if (character.getLastSyncErrorCode() != null) {
            state = "failed_with_cache";
            message = "최근 동기화에 실패했지만 저장된 데이터를 표시합니다.";
        } else if (character.getLastFetchedAt() != null && character.getLastFetchedAt().toLocalDate().equals(kstClock.today())) {
            state = "fresh";
            message = "오늘 수집됨";
        } else if (hasSnapshot) {
            state = "stale";
            message = "저장된 데이터를 표시합니다.";
        } else {
            state = "failed_empty";
            message = "아직 수집된 데이터가 없습니다.";
        }
        return new SyncStateDto(state, character.getLastFetchedAt(), character.getLastSyncAttemptedAt(), message);
    }

    private GrowthSummaryDto buildSummary(CharacterEntity character, DailySnapshotEntity latestSnapshot) {
        if (latestSnapshot == null) {
            return new GrowthSummaryDto(7, false, null, null, null, null, null, null, null, null, 0);
        }
        LocalDate endDate = kstClock.today();
        LocalDate startDate = endDate.minusDays(6);
        var snapshots = dailySnapshotRepository.findByCharacterAndSnapshotDateBetweenOrderBySnapshotDateAsc(character, startDate, endDate);
        if (snapshots.size() < 2) {
            return new GrowthSummaryDto(7, false, null, null, null, null, null, null, null, null, growthEventLogRepository.findByCharacterOrderByEventDateDescIdDesc(character).size());
        }
        DailySnapshotEntity first = snapshots.get(0);
        DailySnapshotEntity last = snapshots.get(snapshots.size() - 1);
        Long combatDelta = first.getCombatPower() != null && last.getCombatPower() != null ? last.getCombatPower() - first.getCombatPower() : null;
        java.math.BigDecimal combatRate = first.getCombatPower() != null && last.getCombatPower() != null && first.getCombatPower() > 0
                ? java.math.BigDecimal.valueOf(combatDelta)
                        .divide(java.math.BigDecimal.valueOf(first.getCombatPower()), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(java.math.BigDecimal.valueOf(100))
                : null;
        Integer levelDelta = last.getLevel() - first.getLevel();
        Integer unionDelta = last.getUnionLevel() != null && first.getUnionLevel() != null ? last.getUnionLevel() - first.getUnionLevel() : null;
        Integer hexaDelta = last.getHexaMatrixLevelSum() != null && first.getHexaMatrixLevelSum() != null ? last.getHexaMatrixLevelSum() - first.getHexaMatrixLevelSum() : null;
        return new GrowthSummaryDto(
                7,
                true,
                combatDelta,
                combatRate,
                first.getLevel(),
                last.getLevel(),
                first.getExpRate(),
                last.getExpRate(),
                unionDelta,
                hexaDelta,
                growthEventLogRepository.findByCharacterOrderByEventDateDescIdDesc(character).size()
        );
    }

    private static ChartPointDto toChartPoint(DailySnapshotEntity snapshot) {
        return new ChartPointDto(
                snapshot.getSnapshotDate(),
                snapshot.getCombatPower(),
                snapshot.getLevel(),
                snapshot.getExpRate(),
                snapshot.getUnionLevel(),
                snapshot.getHexaMatrixLevelSum()
        );
    }

    private static Object metricValue(ChartPointDto point, String metric) {
        return switch (metric) {
            case "combatPower" -> point.combatPower();
            case "level" -> point.level();
            case "expRate" -> point.expRate();
            case "unionLevel" -> point.unionLevel();
            case "hexaMatrixLevelSum" -> point.hexaMatrixLevelSum();
            default -> null;
        };
    }

    private GrowthEventDto toEventDto(GrowthEventLogEntity event) {
        return new GrowthEventDto(
                event.getId(),
                event.getEventDate().toString(),
                event.getEventType(),
                event.getImportanceLevel(),
                event.getTitle(),
                event.getDescription(),
                event.getDetailJson() == null ? java.util.Map.of() : toMap(event.getDetailJson())
        );
    }

    private java.util.Map<String, Object> toMap(JsonNode node) {
        return objectMapper.convertValue(node, java.util.Map.class);
    }

    private record SyncResult(CharacterEntity character, DailySnapshotEntity latestSnapshot, boolean snapshotCreated, boolean snapshotUpdated, int createdEventCount) {
    }

    private record SnapshotPersistResult(DailySnapshotEntity snapshot, boolean created, boolean updated) {
    }
}
