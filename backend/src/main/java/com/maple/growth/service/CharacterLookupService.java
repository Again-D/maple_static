package com.maple.growth.service;

import com.maple.growth.dto.api.CharacterLookupResponseDto;
import com.maple.growth.dto.api.DashboardResponseDto;
import com.maple.growth.dto.api.EventsResponseDto;
import com.maple.growth.dto.api.GrowthHistoryDto;
import com.maple.growth.dto.api.RefreshResponseDto;
import com.maple.growth.entity.CharacterEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CharacterLookupService {

    private final SnapshotSyncService snapshotSyncService;

    public CharacterLookupResponseDto lookupOrRegister(String name) {
        return snapshotSyncService.lookupOrRegister(name);
    }

    public DashboardResponseDto dashboard(String name) {
        return snapshotSyncService.dashboard(name);
    }

    public RefreshResponseDto refresh(String name) {
        return snapshotSyncService.refresh(name);
    }

    public CharacterEntity requireExisting(String name) {
        return snapshotSyncService.requireExistingCharacter(name);
    }

    public GrowthHistoryDto growthHistory(CharacterEntity character, int rangeDays) {
        return snapshotSyncService.growthHistory(character, rangeDays);
    }

    public EventsResponseDto events(CharacterEntity character, int limit) {
        var timeline = snapshotSyncService.events(character, limit);
        return new EventsResponseDto(timeline.items(), timeline.hasMore(), timeline.nextCursor());
    }
}
