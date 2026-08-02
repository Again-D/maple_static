import type {
  ApiResponse,
  CharacterLookupData,
  DashboardData,
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
  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...init,
    headers: {
      Accept: "application/json",
      ...(init?.headers ?? {})
    }
  });
  return (await response.json()) as ApiResponse<T>;
}

export function fetchCharacterLookup(name: string) {
  return requestJson<CharacterLookupData>(`/api/v1/characters/${encodeURIComponent(normalizeCharacterName(name))}`);
}

export function fetchDashboard(name: string) {
  return requestJson<DashboardData>(`/api/v1/characters/${encodeURIComponent(normalizeCharacterName(name))}/dashboard`);
}

export function refreshCharacter(name: string) {
  return requestJson<RefreshData>(`/api/v1/characters/${encodeURIComponent(normalizeCharacterName(name))}/refresh`, {
    method: "POST"
  });
}
