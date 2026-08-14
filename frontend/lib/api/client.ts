import type {
  ApiResponse,
  CharacterLookupData,
  DashboardData,
  GrowthHistory,
  MetricOption,
  RangeOption,
  RefreshData
} from "./types";

const DEFAULT_API_BASE_URL = "http://localhost:8080";
const DEFAULT_APP_TIMEZONE = "Asia/Seoul";

export function normalizeCharacterName(value: string) {
  return value.trim();
}

export function canSubmitSearch(value: string, submitting: boolean) {
  return !submitting && normalizeCharacterName(value).length > 0;
}

export function buildCharacterRoute(value: string) {
  const trimmed = normalizeCharacterName(value);
  return `/character/${encodeURIComponent(trimmed)}`;
}

export function getApiBaseUrl() {
  return (process.env.NEXT_PUBLIC_API_BASE_URL || DEFAULT_API_BASE_URL).replace(/\/+$/, "");
}

export function getAppTimezone() {
  return process.env.NEXT_PUBLIC_APP_TIMEZONE || DEFAULT_APP_TIMEZONE;
}

async function requestJson<T>(path: string, init?: RequestInit): Promise<ApiResponse<T>> {
  const fallback = {
    success: false,
    error: {
      code: "INTERNAL_ERROR",
      message: "잠시 후 다시 시도해 주세요.",
      retryable: true
    },
    meta: {
      serverTime: new Date().toISOString(),
      timezone: getAppTimezone()
    }
  } as const satisfies ApiResponse<T>;

  try {
    const response = await fetch(`${getApiBaseUrl()}${path}`, {
      ...init,
      headers: {
        Accept: "application/json",
        ...(init?.headers ?? {})
      }
    });

    try {
      return (await response.json()) as ApiResponse<T>;
    } catch {
      return fallback;
    }
  } catch {
    return fallback;
  }
}

export function fetchCharacterLookup(name: string) {
  return requestJson<CharacterLookupData>(`/api/v1/characters/${encodeURIComponent(normalizeCharacterName(name))}`);
}

export function fetchDashboard(name: string) {
  return requestJson<DashboardData>(`/api/v1/characters/${encodeURIComponent(normalizeCharacterName(name))}/dashboard`);
}

export function fetchGrowthHistory(name: string, range: RangeOption, metric: MetricOption) {
  const query = new URLSearchParams({ range, metric });
  return requestJson<GrowthHistory>(`/api/v1/characters/${encodeURIComponent(normalizeCharacterName(name))}/growth-history?${query.toString()}`);
}

export function refreshCharacter(name: string) {
  return requestJson<RefreshData>(`/api/v1/characters/${encodeURIComponent(normalizeCharacterName(name))}/refresh`, {
    method: "POST"
  });
}
