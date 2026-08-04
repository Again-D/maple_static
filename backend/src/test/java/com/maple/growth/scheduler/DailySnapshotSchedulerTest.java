package com.maple.growth.scheduler;

import java.util.List;

import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.repository.CharacterRepository;
import com.maple.growth.service.SnapshotSyncService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailySnapshotSchedulerTest {

    @Test
    void schedulerProcessesAutoTrackedCharactersAndContinuesAfterFailure() {
        CharacterRepository characterRepository = mock(CharacterRepository.class);
        SnapshotSyncService snapshotSyncService = mock(SnapshotSyncService.class);
        DailySnapshotScheduler scheduler = new DailySnapshotScheduler(characterRepository, snapshotSyncService);

        CharacterEntity first = new CharacterEntity("ocid-1", "Aries92", "루나", "나이트로드", "male", "img");
        first.setAutoTrack(true);
        CharacterEntity second = new CharacterEntity("ocid-2", "Aries93", "루나", "히어로", "male", "img");
        second.setAutoTrack(true);
        CharacterEntity ignored = new CharacterEntity("ocid-3", "Aries94", "루나", "비숍", "female", "img");
        ignored.setAutoTrack(false);
        when(characterRepository.findAll()).thenReturn(List.of(first, second, ignored));
        when(snapshotSyncService.refresh("Aries92")).thenThrow(new RuntimeException("boom"));

        scheduler.collectDailySnapshots();

        verify(snapshotSyncService).refresh("Aries92");
        verify(snapshotSyncService).refresh("Aries93");
    }
}
