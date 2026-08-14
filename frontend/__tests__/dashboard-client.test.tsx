import assert from "node:assert/strict";
import { describe, it } from "node:test";

import { fetchSelectedChartHistory, refreshDashboardWithSelectedHistory } from "../components/CharacterDashboardClient";
import type { DashboardData, GrowthHistory } from "../lib/api/types";

const baseDashboard: DashboardData = {
  profile: {
    id: "1",
    ocid: "ocid",
    name: "Aries92",
    worldName: "루나",
    jobName: "나이트로드",
    gender: "male",
    imageUrl: "/character-image.png",
    isAutoTrack: true
  },
  latestSnapshot: {
    snapshotId: 12,
    snapshotDate: "2026-08-02",
    level: 278,
    exp: 123456789,
    expRate: 42.1234,
    combatPower: 7420500,
    unionLevel: 8500,
    unionArtifactLevel: 42,
    hexaMatrixLevelSum: 135,
    capturedAt: "2026-08-02T04:00:12+09:00"
  },
  syncState: {
    state: "fresh",
    lastSuccessAt: "2026-08-02T04:00:12+09:00",
    lastAttemptAt: "2026-08-02T04:00:12+09:00",
    message: "오늘 수집됨"
  },
  summary: {
    rangeDays: 7,
    hasEnoughSnapshots: true,
    combatPowerDelta: 1420500,
    combatPowerDeltaRate: 1.8,
    levelFrom: 277,
    levelTo: 278,
    expRateFrom: 35.2,
    expRateTo: 42.1,
    unionLevelDelta: 120,
    hexaMatrixLevelDelta: 3,
    eventCount: 2
  },
  chart: {
    range: "7d",
    metric: "combatPower",
    hasEnoughSnapshots: true,
    points: [
      {
        snapshotDate: "2026-08-01",
        combatPower: 6000000,
        level: 277,
        expRate: 35.2,
        unionLevel: 8380,
        hexaMatrixLevelSum: 132
      },
      {
        snapshotDate: "2026-08-02",
        combatPower: 7420500,
        level: 278,
        expRate: 42.1234,
        unionLevel: 8500,
        hexaMatrixLevelSum: 135
      }
    ]
  },
  timeline: {
    events: [
      {
        id: 1,
        eventDate: "2026-08-02",
        eventType: "LEVEL_UP",
        importanceLevel: 3,
        title: "Lv.277 -> Lv.278 레벨업",
        description: "이전 대표 스냅샷 대비 레벨이 상승했습니다.",
        detail: null
      }
    ],
    hasMore: false,
    nextCursor: null
  }
};

function history(range: GrowthHistory["range"], metric: GrowthHistory["metric"], level: number): GrowthHistory {
  return {
    range,
    metric,
    hasEnoughSnapshots: true,
    points: [
      {
        snapshotDate: "2026-08-01",
        combatPower: 6000000,
        level: level - 1,
        expRate: 35.2,
        unionLevel: 8380,
        hexaMatrixLevelSum: 132
      },
      {
        snapshotDate: "2026-08-02",
        combatPower: 7420500,
        level,
        expRate: 42.1234,
        unionLevel: 8500,
        hexaMatrixLevelSum: 135
      }
    ]
  };
}

describe("dashboard client orchestration", () => {
  it("preserves non-default selector and refetches selected series after refresh success", async () => {
    const calls: string[] = [];
    const api = {
      refreshCharacter: async () => {
        calls.push("refresh");
        return {
          success: true as const,
          data: {
            profile: baseDashboard.profile,
            latestSnapshot: baseDashboard.latestSnapshot,
            syncState: baseDashboard.syncState,
            snapshotCreated: false,
            snapshotUpdated: true,
            createdEventCount: 3
          },
          meta: { serverTime: "2026-08-15T04:10:00+09:00", timezone: "Asia/Seoul" }
        };
      },
      fetchDashboard: async () => {
        calls.push("dashboard");
        return {
          success: true as const,
          data: {
            ...baseDashboard,
            chart: history("7d", "combatPower", 278)
          },
          meta: { serverTime: "2026-08-15T04:10:01+09:00", timezone: "Asia/Seoul" }
        };
      },
      fetchGrowthHistory: async (_name: string, range: "7d" | "30d" | "all", metric: GrowthHistory["metric"]) => {
        calls.push(`history:${range}:${metric}`);
        return {
          success: true as const,
          data: history(range, metric, 280),
          meta: { serverTime: "2026-08-15T04:10:02+09:00", timezone: "Asia/Seoul" }
        };
      }
    };

    const result = await refreshDashboardWithSelectedHistory({
      api,
      name: "Aries92",
      currentData: baseDashboard,
      selectedRange: "30d",
      selectedMetric: "level"
    });

    assert.equal(result.status, "ready");
    assert.deepEqual(calls, ["refresh", "dashboard", "history:30d:level"]);
    if (result.status !== "ready") {
      assert.fail("expected ready status");
    }
    assert.equal(result.data.chart.range, "30d");
    assert.equal(result.data.chart.metric, "level");
    assert.equal(result.data.chart.points.at(-1)?.level, 280);
    assert.equal(result.chartSubstate.status, "idle");
    assert.match(result.banner.title, /새로고침/);
  });

  it("keeps current chart and dashboard data when selected history fetch fails", async () => {
    const api = {
      fetchDashboard: async () => {
        return {
          success: true as const,
          data: baseDashboard,
          meta: { serverTime: "2026-08-15T04:10:00+09:00", timezone: "Asia/Seoul" }
        };
      },
      fetchGrowthHistory: async () => {
        return {
          success: false as const,
          error: {
            code: "NEXON_API_UNAVAILABLE" as const,
            message: "Nexon API를 사용할 수 없습니다.",
            retryable: true
          },
          meta: { serverTime: "2026-08-15T04:10:03+09:00", timezone: "Asia/Seoul" }
        };
      },
      refreshCharacter: async () => {
        return {
          success: true as const,
          data: {
            profile: baseDashboard.profile,
            latestSnapshot: baseDashboard.latestSnapshot,
            syncState: baseDashboard.syncState,
            snapshotCreated: false,
            snapshotUpdated: true,
            createdEventCount: 0
          },
          meta: { serverTime: "2026-08-15T04:10:00+09:00", timezone: "Asia/Seoul" }
        };
      }
    };

    const selectorFailure = await fetchSelectedChartHistory({
      api,
      name: "Aries92",
      data: baseDashboard,
      selectedRange: "30d",
      selectedMetric: "level"
    });

    assert.equal(selectorFailure.chartSubstate.status, "error");
    assert.equal(selectorFailure.data.chart.range, "30d");
    assert.equal(selectorFailure.data.chart.metric, "level");
    assert.deepEqual(selectorFailure.data.chart.points, baseDashboard.chart.points);
    assert.equal(selectorFailure.data.profile.name, baseDashboard.profile.name);
    assert.equal(selectorFailure.data.summary.eventCount, baseDashboard.summary.eventCount);
    assert.equal(selectorFailure.data.timeline.events.length, baseDashboard.timeline.events.length);

    const refreshResult = await refreshDashboardWithSelectedHistory({
      api,
      name: "Aries92",
      currentData: baseDashboard,
      selectedRange: "30d",
      selectedMetric: "level"
    });

    assert.equal(refreshResult.status, "ready");
    if (refreshResult.status !== "ready") {
      assert.fail("expected ready status");
    }
    assert.equal(refreshResult.chartSubstate.status, "error");
    assert.equal(refreshResult.data.profile.name, baseDashboard.profile.name);
    assert.equal(refreshResult.data.summary.eventCount, baseDashboard.summary.eventCount);
    assert.equal(refreshResult.data.timeline.events.length, baseDashboard.timeline.events.length);
    assert.equal(refreshResult.data.chart.range, "30d");
    assert.equal(refreshResult.data.chart.metric, "level");
    assert.deepEqual(refreshResult.data.chart.points, baseDashboard.chart.points);
  });
});
