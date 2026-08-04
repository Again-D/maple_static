import assert from "node:assert/strict";
import { describe, it } from "node:test";

import {
  buildCharacterRoute,
  canSubmitSearch,
  fetchCharacterLookup,
  fetchDashboard,
  getApiBaseUrl,
  normalizeCharacterName,
  refreshCharacter
} from "../lib/api/client";
import type { DashboardData } from "../lib/api/types";

describe("api client helpers", () => {
  it("trims names for routes", () => {
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

  it("calls encoded api paths and parses wrapper responses", async () => {
    const requests: Array<{ url: string; init?: RequestInit }> = [];
    const originalFetch = globalThis.fetch;
    globalThis.fetch = (async (url: string | URL, init?: RequestInit) => {
      requests.push({ url: String(url), init });
      return new Response(
        JSON.stringify({
          success: true,
          data: {
            profile: {
              id: "1",
              ocid: "ocid",
              name: "아리엘",
              worldName: "루나",
              jobName: "나이트로드",
              gender: null,
              imageUrl: null,
              isAutoTrack: true
            },
            latestSnapshot: null,
            syncState: {
              state: "fresh",
              lastSuccessAt: null,
              lastAttemptAt: null,
              message: "오늘 수집됨"
            }
          },
          meta: {
            serverTime: "2026-08-02T04:10:00+09:00",
            timezone: "Asia/Seoul"
          }
        }),
        {
          status: 200,
          headers: {
            "Content-Type": "application/json"
          }
        }
      );
    }) as typeof fetch;

    try {
      const lookup = await fetchCharacterLookup("  아리엘  ");
      const dashboard = await fetchDashboard("  아리엘  ");
      const refresh = await refreshCharacter("  아리엘  ");

      assert.equal(lookup.success, true);
      assert.equal(dashboard.success, true);
      assert.equal(refresh.success, true);
      assert.equal(requests[0].url, "http://localhost:8080/api/v1/characters/%EC%95%84%EB%A6%AC%EC%97%98");
      assert.equal(requests[1].url, "http://localhost:8080/api/v1/characters/%EC%95%84%EB%A6%AC%EC%97%98/dashboard");
      assert.equal(requests[2].init?.method, "POST");
    } finally {
      globalThis.fetch = originalFetch;
    }
  });
});
