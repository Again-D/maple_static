import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { renderToStaticMarkup } from "react-dom/server";

import { CharacterDashboardView } from "../components/CharacterDashboardView";

const sampleData = {
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
    state: "fresh" as const,
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
    rangeDays: 7,
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
    items: [
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

describe("dashboard page", () => {
  it("renders loading skeletons", () => {
    const html = renderToStaticMarkup(
      <CharacterDashboardView
        name="Aries92"
        status="loading"
        data={null}
        errorMessage={null}
        banner={null}
        refreshing={false}
        onRefresh={() => undefined}
        onRetry={() => undefined}
      />
    );

    assert.match(html, /대시보드를 불러오는 중입니다/);
    assert.match(html, /aria-busy="true"/);
  });

  it("renders one-snapshot or data-insufficient states clearly", () => {
    const html = renderToStaticMarkup(
      <CharacterDashboardView
        name="Aries92"
        status="ready"
        data={{ ...sampleData, summary: { ...sampleData.summary, hasEnoughSnapshots: false }, chart: { rangeDays: 7, points: [sampleData.chart.points[1]] }, timeline: { items: [], hasMore: false, nextCursor: null } }}
        errorMessage={null}
        banner={null}
        refreshing={false}
        onRefresh={() => undefined}
        onRetry={() => undefined}
      />
    );

    assert.match(html, /비교할 스냅샷이 아직 부족합니다/);
    assert.match(html, /다음 스냅샷이 쌓이면 성장 이벤트가 표시됩니다/);
  });

  it("renders ready dashboard content", () => {
    const html = renderToStaticMarkup(
      <CharacterDashboardView
        name="Aries92"
        status="ready"
        data={sampleData}
        errorMessage={null}
        banner={{ title: "새로고침이 완료되었습니다.", message: "성장 이벤트 2개가 반영되었습니다." }}
        refreshing={false}
        onRefresh={() => undefined}
        onRetry={() => undefined}
      />
    );

    assert.match(html, /Aries92/);
    assert.match(html, /캐릭터의 현재 상태와 최근 7일 성장 흐름/);
    assert.match(html, /전투력 변화/);
    assert.match(html, /새로고침이 완료되었습니다/);
    assert.match(html, /\/api\/character-image\?url=/);
  });

  it("renders refreshing state without hiding cached content", () => {
    const html = renderToStaticMarkup(
      <CharacterDashboardView
        name="Aries92"
        status="ready"
        data={sampleData}
        errorMessage={null}
        banner={null}
        refreshing={true}
        onRefresh={() => undefined}
        onRetry={() => undefined}
      />
    );

    assert.match(html, /Aries92/);
    assert.match(html, /새로고침 중\.\.\./);
    assert.match(html, /aria-busy="true"/);
  });

  it("shows distinct not-found and error states", () => {
    const notFound = renderToStaticMarkup(
      <CharacterDashboardView
        name="Missing"
        status="not_found"
        data={null}
        errorMessage="캐릭터를 찾을 수 없습니다."
        banner={null}
        refreshing={false}
        onRefresh={() => undefined}
        onRetry={() => undefined}
      />
    );
    const error = renderToStaticMarkup(
      <CharacterDashboardView
        name="Aries92"
        status="error"
        data={null}
        errorMessage="잠시 후 다시 시도해 주세요."
        banner={null}
        refreshing={false}
        onRefresh={() => undefined}
        onRetry={() => undefined}
      />
    );

    assert.match(notFound, /캐릭터를 찾을 수 없습니다/);
    assert.match(notFound, /href="\/"/);
    assert.match(error, /대시보드를 불러오지 못했습니다/);
  });
});
