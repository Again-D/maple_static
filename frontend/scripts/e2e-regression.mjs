import { execFile as execFileCallback, spawn } from "node:child_process";
import { createServer } from "node:http";
import { promisify } from "node:util";
import { setTimeout as delay } from "node:timers/promises";

const frontendUrl = "http://127.0.0.1:3100";
const browserSession = `maple-e2e-${process.pid}`;
const browser = process.env.AGENT_BROWSER_BIN || "./node_modules/.bin/agent-browser";
const meta = { serverTime: "2026-08-12T00:00:00Z", timezone: "Asia/Seoul" };
const execFile = promisify(execFileCallback);

const dashboard = {
  profile: {
    id: "character-1",
    ocid: "ocid-1",
    name: "Test Hero",
    worldName: "Scania",
    jobName: "Arch Mage",
    gender: null,
    imageUrl: "https://open.api.nexon.com/static/maplestory/character/look/e2e-fixture.png",
    isAutoTrack: true
  },
  latestSnapshot: {
    snapshotId: 1,
    snapshotDate: "2026-08-12",
    level: 285,
    exp: 123456,
    expRate: 12.5,
    combatPower: 123456789,
    unionLevel: 9000,
    unionArtifactLevel: null,
    hexaMatrixLevelSum: 20,
    capturedAt: "2026-08-12T00:00:00Z"
  },
  syncState: {
    state: "fresh",
    lastSuccessAt: "2026-08-12T00:00:00Z",
    lastAttemptAt: "2026-08-12T00:00:00Z",
    message: "최신 스냅샷입니다."
  },
  summary: {
    rangeDays: 7,
    hasEnoughSnapshots: true,
    combatPowerDelta: 1234567,
    combatPowerDeltaRate: 1.0,
    levelFrom: 284,
    levelTo: 285,
    expRateFrom: 80.0,
    expRateTo: 12.5,
    unionLevelDelta: 5,
    hexaMatrixLevelDelta: 1,
    eventCount: 2
  },
  chart: {
    range: "7d",
    metric: "combatPower",
    hasEnoughSnapshots: true,
    points: [
      {
        snapshotDate: "2026-08-12",
        combatPower: 123456789,
        level: 285,
        expRate: 12.5,
        unionLevel: 9000,
        hexaMatrixLevelSum: 20
      }
    ]
  },
  timeline: { events: [], hasMore: false, nextCursor: null },
  equipment: {
    available: true,
    snapshotDate: "2026-08-12",
    capturedAt: "2026-08-12T00:00:00Z",
    items: [{
      id: "무기:무기",
      part: "무기",
      slot: "무기",
      name: "에테르넬 스태프",
      iconUrl: null,
      shapeIconUrl: null,
      description: "E2E fixture",
      gender: null,
      equipmentLevel: "250",
      starforce: "22",
      potentialGrade: "레전드리",
      additionalPotentialGrade: null,
      totalOptions: {},
      baseOptions: { magic_power: "250" },
      additionalOptions: {},
      etcOptions: {},
      starforceOptions: {},
      potentialOptions: ["마력 12%"],
      additionalPotentialOptions: []
    }]
  }
};

function response(data) {
  return JSON.stringify({ success: true, data, meta });
}

function failure(code, message, retryable) {
  return JSON.stringify({ success: false, error: { code, message, retryable }, meta });
}

async function runBrowser(args) {
  try {
    const { stdout } = await execFile(browser, ["--session", browserSession, ...args], {
      encoding: "utf8",
      env: { ...process.env, AGENT_BROWSER_IDLE_TIMEOUT_MS: "0" }
    });
    return stdout;
  } catch (error) {
    throw new Error(`agent-browser ${args.join(" ")} failed:\n${error.stderr || error.stdout || error.message}`);
  }
}

function expectText(text, expected, description) {
  if (!text.includes(expected)) {
    throw new Error(`${description}: expected "${expected}" in:\n${text}`);
  }
}

async function waitForServer() {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    try {
      const response = await fetch(frontendUrl);
      if (response.ok) {
        return;
      }
    } catch {
      // Next.js has not started listening yet.
    }
    await delay(1000);
  }
  throw new Error("Next.js E2E server did not start within 60 seconds.");
}

async function main() {
  const apiServer = createServer((request, responseStream) => {
    const requestUrl = new URL(request.url, "http://127.0.0.1");
    const match = requestUrl.pathname.match(/^\/api\/v1\/characters\/([^/]+)\/(dashboard|growth-history|refresh)$/);

    responseStream.setHeader("Access-Control-Allow-Origin", frontendUrl);
    responseStream.setHeader("Content-Type", "application/json");
    if (!match) {
      responseStream.writeHead(404).end();
      return;
    }

    const [, encodedName, operation] = match;
    const name = decodeURIComponent(encodedName);
    if (operation === "dashboard" && name === "unknown") {
      responseStream.end(failure("CHARACTER_NOT_FOUND", "캐릭터가 존재하지 않습니다.", false));
      return;
    }
    if (operation === "dashboard" && name === "unavailable") {
      responseStream.end(failure("NEXON_API_UNAVAILABLE", "Nexon API를 사용할 수 없습니다.", true));
      return;
    }
    if (operation === "dashboard") {
      responseStream.end(response(dashboard));
      return;
    }
    if (operation === "growth-history") {
      const range = requestUrl.searchParams.get("range") || "7d";
      const metric = requestUrl.searchParams.get("metric") || "combatPower";
      responseStream.end(response({ ...dashboard.chart, range, metric }));
      return;
    }
    if (request.method === "POST") {
      responseStream.end(
        response({
          profile: dashboard.profile,
          latestSnapshot: dashboard.latestSnapshot,
          syncState: dashboard.syncState,
          snapshotCreated: false,
          snapshotUpdated: true,
          createdEventCount: 2
        })
      );
      return;
    }

    responseStream.writeHead(405).end();
  });
  await new Promise((resolve) => apiServer.listen(0, "127.0.0.1", resolve));
  const apiAddress = apiServer.address();
  const apiUrl = `http://127.0.0.1:${apiAddress.port}`;
  const server = spawn("npm", ["run", "dev", "--", "-p", "3100"], {
    cwd: process.cwd(),
    stdio: "inherit",
    env: { ...process.env, NEXT_TELEMETRY_DISABLED: "1", NEXT_PUBLIC_API_BASE_URL: apiUrl },
    detached: true
  });

  try {
    await waitForServer();

    await runBrowser(["open", frontendUrl]);
    expectText(await runBrowser(["is", "enabled", "button[type=submit]"]), "false", "Blank search must be disabled");
    await runBrowser(["fill", "input[name=nickname]", "  Test Hero  "]);
    expectText(await runBrowser(["is", "enabled", "button[type=submit]"]), "true", "Trimmed nickname must enable search");
    await runBrowser(["click", "button[type=submit]"]);
    try {
      await runBrowser(["wait", "--text", "최신 상태"]);
    } catch (error) {
      console.error("E2E page after dashboard navigation:\n", await runBrowser(["read"]));
      console.error("E2E API requests:\n", await runBrowser(["network", "requests", "--filter", "/api/v1/"]));
      throw error;
    }
    expectText(await runBrowser(["read"]), "Test Hero", "Search must navigate to the character dashboard");
    expectText(await runBrowser(["read"]), "현재 장비", "Dashboard must show current equipment");
    await runBrowser(["click", 'a[href*="/equipment/"]']);
    await runBrowser(["wait", "--url", "**/equipment/**"]);
    await runBrowser(["wait", "--text", "기본 옵션"]);
    expectText(await runBrowser(["read"]), "잠재능력", "Equipment detail must show populated detail groups");
    await runBrowser(["click", "a.back-link"]);
    await runBrowser(["wait", "--text", "현재 장비"]);
    await runBrowser(["set", "viewport", "1280", "720"]);
    await runBrowser([
      "wait",
      "--fn",
      "(() => { const image = document.querySelector('[data-testid=profile-image]'); if (!image) return false; const style = getComputedStyle(image); return style.width === '128px' && style.height === '128px' && style.objectFit === 'contain'; })()"
    ]);
    await runBrowser(["set", "viewport", "390", "844"]);
    await runBrowser([
      "wait",
      "--fn",
      "(() => { const image = document.querySelector('[data-testid=profile-image]'); if (!image) return false; const style = getComputedStyle(image); return style.width === '112px' && style.height === '112px' && style.objectFit === 'contain'; })()"
    ]);
    await runBrowser(["click", '[data-testid="growth-range-30d"]']);
    await runBrowser(["wait", "--text", "최근 30일 전투력 성장"]);
    await runBrowser(["click", '[data-testid="growth-metric-hexa-sum"]']);
    await runBrowser(["wait", "--text", "최근 30일 헥사 매트릭스 레벨 합계 성장"]);

    await runBrowser(["click", "button.refresh-button"]);
    await runBrowser(["wait", "--text", "새로고침이 완료되었습니다."]);
    expectText(await runBrowser(["read"]), "성장 이벤트 2개가 반영되었습니다.", "Refresh must show its result banner");

    await runBrowser(["open", `${frontendUrl}/character/unknown`]);
    await runBrowser(["wait", "--text", "캐릭터를 찾을 수 없습니다."]);

    await runBrowser(["open", `${frontendUrl}/character/unavailable`]);
    await runBrowser(["wait", "--text", "대시보드를 불러오지 못했습니다."]);
    await runBrowser(["click", "button.refresh-button--inline"]);
    await runBrowser(["wait", "--text", "대시보드를 불러오지 못했습니다."]);

    console.log("E2E regression checks passed.");
  } finally {
    try {
      await runBrowser(["close", "--all"]);
    } catch {
      // A browser launch failure can leave no session to close.
    }
    if (server.exitCode === null) {
      process.kill(-server.pid, "SIGTERM");
    }
    await Promise.race([new Promise((resolve) => server.once("exit", resolve)), delay(5000)]);
    if (server.exitCode === null) {
      process.kill(-server.pid, "SIGKILL");
    }
    await new Promise((resolve) => apiServer.close(resolve));
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
