import assert from "node:assert/strict";
import { describe, it } from "node:test";

import { buildCharacterRoute, canSubmitSearch, getApiBaseUrl, normalizeCharacterName } from "../lib/api/client";
import type { DashboardData } from "../lib/api/types";

describe("api client helpers", () => {
  it("trims and encodes names for routes", () => {
    assert.equal(normalizeCharacterName("  아리엘  "), "아리엘");
    assert.equal(buildCharacterRoute("  아리엘  "), "/character/%EC%95%84%EB%A6%AC%EC%97%98");
  });

  it("blocks blank or pending search submits", () => {
    assert.equal(canSubmitSearch("   ", false), false);
    assert.equal(canSubmitSearch("Aries92", true), false);
    assert.equal(canSubmitSearch("Aries92", false), true);
  });

  it("uses the documented public api base default", () => {
    delete process.env.NEXT_PUBLIC_API_BASE_URL;
    assert.equal(getApiBaseUrl(), "http://localhost:8080");
  });

  it("keeps the dashboard response shape narrow", () => {
    const sample: DashboardData = {
      profile: {
        id: "1",
        ocid: "ocid",
        name: "Aries92",
        worldName: "루나",
        jobName: "나이트로드",
        gender: "male",
        imageUrl: null,
        isAutoTrack: true
      },
      latestSnapshot: null,
      syncState: {
        state: "fresh",
        lastSuccessAt: null,
        lastAttemptAt: null,
        message: "오늘 수집됨"
      },
      summary: {
        rangeDays: 7,
        hasEnoughSnapshots: false,
        combatPowerDelta: null,
        combatPowerDeltaRate: null,
        levelFrom: null,
        levelTo: null,
        expRateFrom: null,
        expRateTo: null,
        unionLevelDelta: null,
        hexaMatrixLevelDelta: null,
        eventCount: 0
      },
      chart: {
        rangeDays: 7,
        points: []
      },
      timeline: {
        items: [],
        hasMore: false,
        nextCursor: null
      }
    };

    assert.equal(sample.summary.hasEnoughSnapshots, false);
    assert.equal(sample.timeline.items.length, 0);
  });
});
