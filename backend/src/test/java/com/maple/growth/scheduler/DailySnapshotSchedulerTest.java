package com.maple.growth.scheduler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.maple.growth.entity.CharacterEntity;
import com.maple.growth.config.AppProperties;
import com.maple.growth.repository.CharacterRepository;
import com.maple.growth.service.SnapshotSyncService;
import com.maple.growth.service.CollectionRunService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailySnapshotSchedulerTest {

    @Test
    void schedulerProcessesAutoTrackedCharactersAndContinuesAfterFailure() {
        CharacterRepository characterRepository = mock(CharacterRepository.class);
        SnapshotSyncService snapshotSyncService = mock(SnapshotSyncService.class);
        CollectionRunService collectionRunService = mock(CollectionRunService.class);
        when(collectionRunService.start(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(UUID.randomUUID());
        DailySnapshotScheduler scheduler = new DailySnapshotScheduler(characterRepository, snapshotSyncService, collectionRunService, schedulerProperties(1));

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

    @Test
    void skipsOverlappingRunAfterConfiguredWaitWithoutRefreshingCharacters() throws Exception {
        CharacterRepository characterRepository = mock(CharacterRepository.class);
        SnapshotSyncService snapshotSyncService = mock(SnapshotSyncService.class);
        CollectionRunService collectionRunService = mock(CollectionRunService.class);
        when(collectionRunService.start(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(UUID.randomUUID());
        DailySnapshotScheduler scheduler = new DailySnapshotScheduler(characterRepository, snapshotSyncService, collectionRunService, schedulerProperties(1));
        CharacterEntity character = new CharacterEntity("ocid-1", "Aries92", "루나", "나이트로드", "male", "img");
        character.setAutoTrack(true);
        CountDownLatch refreshStarted = new CountDownLatch(1);
        CountDownLatch releaseRefresh = new CountDownLatch(1);
        when(characterRepository.findAll()).thenReturn(List.of(character));
        org.mockito.Mockito.doAnswer(invocation -> {
            refreshStarted.countDown();
            releaseRefresh.await(5, TimeUnit.SECONDS);
            return null;
        }).when(snapshotSyncService).refresh("Aries92");

        Thread firstRun = new Thread(scheduler::collectDailySnapshots);
        firstRun.start();
        assertThat(refreshStarted.await(1, TimeUnit.SECONDS)).isTrue();

        assertThatCode(scheduler::collectDailySnapshots).doesNotThrowAnyException();

        org.mockito.Mockito.verify(snapshotSyncService, org.mockito.Mockito.times(1)).refresh("Aries92");
        releaseRefresh.countDown();
        firstRun.join(2_000);
        assertThat(firstRun.isAlive()).isFalse();
    }

    @Test
    void skipsWhenActiveRunCompletesWithinConfiguredWait() throws Exception {
        CharacterRepository characterRepository = mock(CharacterRepository.class);
        SnapshotSyncService snapshotSyncService = mock(SnapshotSyncService.class);
        CollectionRunService collectionRunService = mock(CollectionRunService.class);
        when(collectionRunService.start(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(UUID.randomUUID());
        DailySnapshotScheduler scheduler = new DailySnapshotScheduler(characterRepository, snapshotSyncService, collectionRunService, schedulerProperties(2));
        CharacterEntity character = new CharacterEntity("ocid-1", "Aries92", "루나", "나이트로드", "male", "img");
        character.setAutoTrack(true);
        CountDownLatch refreshStarted = new CountDownLatch(1);
        CountDownLatch releaseRefresh = new CountDownLatch(1);
        when(characterRepository.findAll()).thenReturn(List.of(character));
        org.mockito.Mockito.doAnswer(invocation -> {
            refreshStarted.countDown();
            releaseRefresh.await(5, TimeUnit.SECONDS);
            return null;
        }).when(snapshotSyncService).refresh("Aries92");

        Thread firstRun = new Thread(scheduler::collectDailySnapshots);
        firstRun.start();
        assertThat(refreshStarted.await(1, TimeUnit.SECONDS)).isTrue();

        Thread releaseAfterWaitBegins = new Thread(() -> {
            try {
                Thread.sleep(100);
                releaseRefresh.countDown();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        releaseAfterWaitBegins.start();
        scheduler.collectDailySnapshots();

        firstRun.join(2_000);
        releaseAfterWaitBegins.join(2_000);
        assertThat(firstRun.isAlive()).isFalse();
        verify(snapshotSyncService, org.mockito.Mockito.times(1)).refresh("Aries92");
    }

    @Test
    void releasesRunGuardWhenBatchFailsBeforeCharacterProcessing() {
        CharacterRepository characterRepository = mock(CharacterRepository.class);
        SnapshotSyncService snapshotSyncService = mock(SnapshotSyncService.class);
        CollectionRunService collectionRunService = mock(CollectionRunService.class);
        when(collectionRunService.start(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(UUID.randomUUID());
        DailySnapshotScheduler scheduler = new DailySnapshotScheduler(characterRepository, snapshotSyncService, collectionRunService, schedulerProperties(1));
        CharacterEntity character = new CharacterEntity("ocid-1", "Aries92", "루나", "나이트로드", "male", "img");
        character.setAutoTrack(true);
        when(characterRepository.findAll())
                .thenThrow(new RuntimeException("database unavailable"))
                .thenReturn(List.of(character));

        assertThatCode(scheduler::collectDailySnapshots).doesNotThrowAnyException();

        assertThatCode(scheduler::collectDailySnapshots).doesNotThrowAnyException();
        verify(snapshotSyncService).refresh("Aries92");
        org.mockito.Mockito.verify(collectionRunService, org.mockito.Mockito.times(1))
                .recordFailure(org.mockito.ArgumentMatchers.any());
    }

    private static AppProperties schedulerProperties(int waitSeconds) {
        return new AppProperties("Asia/Seoul", "0 0 4 * * *", "http://localhost:3000", "https://open.api.nexon.com", 10, waitSeconds);
    }
}
