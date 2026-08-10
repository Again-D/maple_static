package com.maple.growth.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maple.growth.config.AppProperties;
import com.maple.growth.dto.api.CharacterLookupResponseDto;
import com.maple.growth.dto.api.DashboardResponseDto;
import com.maple.growth.dto.api.RefreshResponseDto;
import com.maple.growth.dto.nexon.NexonCharacterSnapshot;
import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.entity.DailySnapshotEntity;
import com.maple.growth.repository.CharacterRepository;
import com.maple.growth.repository.DailySnapshotRepository;
import com.maple.growth.repository.GrowthEventLogRepository;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SnapshotSyncServiceTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-08-01T18:30:00Z"), ZoneOffset.UTC);
    private final AppProperties appProperties = new AppProperties("Asia/Seoul", "0 0 4 * * *", "http://localhost:3000", "https://open.api.nexon.com", 10, 300);
    private final KstClock kstClock = new KstClock(fixedClock, appProperties);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlatformTransactionManager transactionManager = new AbstractPlatformTransactionManager() {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    };

    @Test
    void firstSearchSuccessCreatesCharacterAndFirstSnapshot() {
        CharacterRepository characterRepository = mock(CharacterRepository.class);
        DailySnapshotRepository snapshotRepository = mock(DailySnapshotRepository.class);
        GrowthEventLogRepository eventRepository = mock(GrowthEventLogRepository.class);
        NexonApiClient client = mock(NexonApiClient.class);
        GrowthEventService eventService = mock(GrowthEventService.class);
        when(eventService.recomputeEvents(any(DailySnapshotEntity.class), isNull())).thenReturn(0);

        NexonCharacterSnapshot apiSnapshot = snapshot("ocid-1", "Aries92");
        when(characterRepository.findByCharacterName("Aries92")).thenReturn(Optional.empty());
        when(characterRepository.save(any(CharacterEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(snapshotRepository.findByCharacterAndSnapshotDate(any(CharacterEntity.class), any(LocalDate.class))).thenReturn(Optional.empty());
        when(snapshotRepository.save(any(DailySnapshotEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(snapshotRepository.findFirstByCharacterAndSnapshotDateLessThanOrderBySnapshotDateDescIdDesc(any(CharacterEntity.class), any(LocalDate.class))).thenReturn(Optional.empty());
        when(client.fetchCharacterSnapshot(anyString(), any(LocalDate.class))).thenReturn(apiSnapshot);

        SnapshotSyncService service = new SnapshotSyncService(characterRepository, snapshotRepository, eventRepository, client, eventService, kstClock, objectMapper, transactionManager);
        CharacterLookupResponseDto response = service.lookupOrRegister("Aries92");

        assertThat(response.profile().isAutoTrack()).isTrue();
        verify(characterRepository).save(any(CharacterEntity.class));
        verify(snapshotRepository).save(any(DailySnapshotEntity.class));
        verify(eventService).recomputeEvents(any(DailySnapshotEntity.class), isNull());
    }

    @Test
    void firstSearchFailureDoesNotCreateRows() {
        CharacterRepository characterRepository = mock(CharacterRepository.class);
        DailySnapshotRepository snapshotRepository = mock(DailySnapshotRepository.class);
        GrowthEventLogRepository eventRepository = mock(GrowthEventLogRepository.class);
        NexonApiClient client = mock(NexonApiClient.class);
        GrowthEventService eventService = mock(GrowthEventService.class);
        SnapshotSyncService service = new SnapshotSyncService(characterRepository, snapshotRepository, eventRepository, client, eventService, kstClock, objectMapper, transactionManager);
        when(characterRepository.findByCharacterName("Missing")).thenReturn(Optional.empty());
        when(client.fetchCharacterSnapshot(anyString(), any(LocalDate.class))).thenThrow(new NexonApiException(com.maple.growth.dto.api.ApiErrorCode.CHARACTER_NOT_FOUND, "캐릭터를 찾을 수 없습니다.", false));

        assertThatThrownBy(() -> service.lookupOrRegister("Missing"))
                .isInstanceOf(NexonApiException.class);
        verify(characterRepository, never()).save(any(CharacterEntity.class));
        verify(snapshotRepository, never()).save(any(DailySnapshotEntity.class));
    }

    @Test
    void existingCharacterDashboardUsesCacheWithoutNexonCall() {
        CharacterRepository characterRepository = mock(CharacterRepository.class);
        DailySnapshotRepository snapshotRepository = mock(DailySnapshotRepository.class);
        GrowthEventLogRepository eventRepository = mock(GrowthEventLogRepository.class);
        NexonApiClient client = mock(NexonApiClient.class);
        GrowthEventService eventService = mock(GrowthEventService.class);
        SnapshotSyncService service = new SnapshotSyncService(characterRepository, snapshotRepository, eventRepository, client, eventService, kstClock, objectMapper, transactionManager);

        CharacterEntity character = new CharacterEntity("ocid-1", "Aries92", "루나", "나이트로드", "male", null);
        character.setId(java.util.UUID.randomUUID());
        DailySnapshotEntity snapshot = snapshot(character, 278, 1000L, new BigDecimal("42.1234"), 7420500L, 8500, 42, 135);
        when(characterRepository.findByCharacterName("Aries92")).thenReturn(Optional.of(character));
        when(snapshotRepository.findFirstByCharacterOrderBySnapshotDateDescIdDesc(character)).thenReturn(Optional.of(snapshot));
        when(snapshotRepository.findByCharacterAndSnapshotDateBetweenOrderBySnapshotDateAsc(any(CharacterEntity.class), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(snapshot));
        when(eventRepository.findByCharacterOrderByEventDateDescIdDesc(any(CharacterEntity.class))).thenReturn(List.of());

        DashboardResponseDto dashboard = service.dashboard("Aries92");

        assertThat(dashboard.profile().name()).isEqualTo("Aries92");
        verify(client, never()).fetchCharacterSnapshot(anyString(), any());
    }

    @Test
    void sameDayRefreshUpdatesExistingSnapshotInsteadOfAppending() {
        CharacterRepository characterRepository = mock(CharacterRepository.class);
        DailySnapshotRepository snapshotRepository = mock(DailySnapshotRepository.class);
        GrowthEventLogRepository eventRepository = mock(GrowthEventLogRepository.class);
        NexonApiClient client = mock(NexonApiClient.class);
        GrowthEventService eventService = mock(GrowthEventService.class);
        when(eventService.recomputeEvents(any(DailySnapshotEntity.class), isNull())).thenReturn(0);

        CharacterEntity character = new CharacterEntity("ocid-1", "Aries92", "루나", "나이트로드", "male", "img");
        character.setId(java.util.UUID.randomUUID());
        DailySnapshotEntity existing = snapshot(character, 277, 1000L, new BigDecimal("35.1234"), 7300000L, 8380, 132, 130);
        existing.setId(10L);
        NexonCharacterSnapshot apiSnapshot = snapshot("ocid-1", "Aries92", "https://example.com/updated-image.png");
        when(characterRepository.findByCharacterName("Aries92")).thenReturn(Optional.of(character));
        when(snapshotRepository.findByCharacterAndSnapshotDate(character, kstClock.today())).thenReturn(Optional.of(existing));
        when(snapshotRepository.save(any(DailySnapshotEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(snapshotRepository.findFirstByCharacterAndSnapshotDateLessThanOrderBySnapshotDateDescIdDesc(character, kstClock.today())).thenReturn(Optional.empty());
        when(client.fetchCharacterSnapshot("Aries92", kstClock.today())).thenReturn(apiSnapshot);

        SnapshotSyncService service = new SnapshotSyncService(characterRepository, snapshotRepository, eventRepository, client, eventService, kstClock, objectMapper, transactionManager);
        RefreshResponseDto response = service.refresh("Aries92");

        assertThat(response.snapshotUpdated()).isTrue();
        assertThat(response.snapshotCreated()).isFalse();
        assertThat(response.profile().imageUrl()).isEqualTo("https://example.com/updated-image.png");
        verify(snapshotRepository).save(existing);
    }

    @Test
    void firstSearchUsesKstDateEvenWhenSystemClockIsUtc() {
        Clock utcClock = Clock.fixed(Instant.parse("2026-08-01T18:30:00Z"), ZoneOffset.UTC);
        KstClock localKstClock = new KstClock(utcClock, appProperties);

        CharacterRepository characterRepository = mock(CharacterRepository.class);
        DailySnapshotRepository snapshotRepository = mock(DailySnapshotRepository.class);
        GrowthEventLogRepository eventRepository = mock(GrowthEventLogRepository.class);
        NexonApiClient client = mock(NexonApiClient.class);
        GrowthEventService eventService = mock(GrowthEventService.class);
        when(eventService.recomputeEvents(any(DailySnapshotEntity.class), isNull())).thenReturn(0);

        NexonCharacterSnapshot apiSnapshot = snapshot("ocid-2", "Aries95");
        when(characterRepository.findByCharacterName("Aries95")).thenReturn(Optional.empty());
        when(characterRepository.save(any(CharacterEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(snapshotRepository.findByCharacterAndSnapshotDate(any(CharacterEntity.class), any(LocalDate.class))).thenReturn(Optional.empty());
        when(snapshotRepository.save(any(DailySnapshotEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(snapshotRepository.findFirstByCharacterAndSnapshotDateLessThanOrderBySnapshotDateDescIdDesc(any(CharacterEntity.class), any(LocalDate.class))).thenReturn(Optional.empty());
        when(client.fetchCharacterSnapshot("Aries95", localKstClock.today())).thenReturn(apiSnapshot);

        SnapshotSyncService service = new SnapshotSyncService(characterRepository, snapshotRepository, eventRepository, client, eventService, localKstClock, objectMapper, transactionManager);
        CharacterLookupResponseDto response = service.lookupOrRegister("Aries95");

        assertThat(response.profile().name()).isEqualTo("Aries95");
        verify(client).fetchCharacterSnapshot("Aries95", LocalDate.of(2026, 8, 2));
    }

    @Test
    void failedRefreshPreservesExistingLastFetchedAt() {
        CharacterRepository characterRepository = mock(CharacterRepository.class);
        DailySnapshotRepository snapshotRepository = mock(DailySnapshotRepository.class);
        GrowthEventLogRepository eventRepository = mock(GrowthEventLogRepository.class);
        NexonApiClient client = mock(NexonApiClient.class);
        GrowthEventService eventService = mock(GrowthEventService.class);
        SnapshotSyncService service = new SnapshotSyncService(characterRepository, snapshotRepository, eventRepository, client, eventService, kstClock, objectMapper, transactionManager);

        CharacterEntity character = new CharacterEntity("ocid-1", "Aries92", "루나", "나이트로드", "male", "img");
        character.setId(java.util.UUID.randomUUID());
        character.setLastFetchedAt(kstClock.now().minusHours(1));
        when(characterRepository.findByCharacterName("Aries92")).thenReturn(Optional.of(character));
        when(client.fetchCharacterSnapshot(anyString(), any(LocalDate.class))).thenThrow(new NexonApiException(com.maple.growth.dto.api.ApiErrorCode.RATE_LIMITED, "too many", true));

        assertThatThrownBy(() -> service.refresh("Aries92"))
                .isInstanceOf(NexonApiException.class);

        ArgumentCaptor<CharacterEntity> captor = ArgumentCaptor.forClass(CharacterEntity.class);
        verify(characterRepository).save(captor.capture());
        assertThat(captor.getValue().getLastFetchedAt()).isEqualTo(character.getLastFetchedAt());
        assertThat(captor.getValue().getLastSyncErrorCode()).isEqualTo("RATE_LIMITED");
    }

    private NexonCharacterSnapshot snapshot(String ocid, String name) {
        return snapshot(ocid, name, "img");
    }

    private NexonCharacterSnapshot snapshot(String ocid, String name, String imageUrl) {
        ObjectNode statJson = objectMapper.createObjectNode();
        statJson.put("character_level", "278");
        statJson.put("character_exp", "123456789");
        statJson.put("character_exp_rate", "42.1234");
        ObjectNode finalStat = objectMapper.createObjectNode();
        return new NexonCharacterSnapshot(
                ocid,
                name,
                "루나",
                "나이트로드",
                "male",
                imageUrl,
                278,
                123456789L,
                new BigDecimal("42.1234"),
                7420500L,
                8500,
                42,
                135,
                statJson,
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode()
        );
    }

    private DailySnapshotEntity snapshot(CharacterEntity character, int level, long exp, BigDecimal expRate, Long combatPower, Integer unionLevel, Integer artifactLevel, Integer hexa) {
        DailySnapshotEntity snapshot = new DailySnapshotEntity(character, LocalDate.of(2026, 8, 2));
        snapshot.setLevel(level);
        snapshot.setExp(exp);
        snapshot.setExpRate(expRate);
        snapshot.setCombatPower(combatPower);
        snapshot.setUnionLevel(unionLevel);
        snapshot.setUnionArtifactLevel(artifactLevel);
        snapshot.setHexaMatrixLevelSum(hexa);
        snapshot.setCapturedAt(kstClock.now());
        return snapshot;
    }
}
