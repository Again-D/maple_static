export type ApiErrorCode =
  | "INVALID_CHARACTER_NAME"
  | "CHARACTER_NOT_FOUND"
  | "RATE_LIMITED"
  | "NEXON_API_AUTH_FAILED"
  | "NEXON_API_FAILED"
  | "NEXON_API_UNAVAILABLE"
  | "INTERNAL_ERROR";

export type ApiMeta = {
  serverTime: string;
  timezone: string;
};

export type ApiError = {
  code: ApiErrorCode;
  message: string;
  retryable: boolean;
};

export type ApiSuccess<T> = {
  success: true;
  data: T;
  meta: ApiMeta;
};

export type ApiFailure = {
  success: false;
  error: ApiError;
  meta: ApiMeta;
};

export type ApiResponse<T> = ApiSuccess<T> | ApiFailure;

export type CharacterProfile = {
  id: string;
  ocid: string;
  name: string;
  worldName: string;
  jobName: string;
  gender: string | null;
  imageUrl: string | null;
  isAutoTrack: boolean;
};

export type SnapshotSummary = {
  snapshotId: number;
  snapshotDate: string;
  level: number;
  exp: number;
  expRate: number | null;
  combatPower: number | null;
  unionLevel: number | null;
  unionArtifactLevel: number | null;
  hexaMatrixLevelSum: number | null;
  capturedAt: string;
};

export type SyncState = {
  state: "fresh" | "stale" | "refreshing" | "failed_with_cache" | "failed_empty";
  lastSuccessAt: string | null;
  lastAttemptAt: string | null;
  message: string;
};

export type GrowthSummary = {
  rangeDays: number;
  hasEnoughSnapshots: boolean;
  combatPowerDelta: number | null;
  combatPowerDeltaRate: number | null;
  levelFrom: number | null;
  levelTo: number | null;
  expRateFrom: number | null;
  expRateTo: number | null;
  unionLevelDelta: number | null;
  hexaMatrixLevelDelta: number | null;
  eventCount: number;
};

export type ChartPoint = {
  snapshotDate: string;
  combatPower: number | null;
  level: number;
  expRate: number | null;
  unionLevel: number | null;
  hexaMatrixLevelSum: number | null;
};

export type GrowthHistory = {
  rangeDays: number;
  points: ChartPoint[];
};

export type GrowthEvent = {
  id: number;
  eventDate: string;
  eventType: string;
  importanceLevel: number;
  title: string;
  description: string | null;
  detail: Record<string, unknown> | null;
};

export type Timeline = {
  items: GrowthEvent[];
  hasMore: boolean;
  nextCursor: string | null;
};

export type CharacterLookupData = {
  profile: CharacterProfile;
  latestSnapshot: SnapshotSummary | null;
  syncState: SyncState;
};

export type DashboardData = {
  profile: CharacterProfile;
  latestSnapshot: SnapshotSummary | null;
  syncState: SyncState;
  summary: GrowthSummary;
  chart: GrowthHistory;
  timeline: Timeline;
};

export type RefreshData = {
  profile: CharacterProfile;
  latestSnapshot: SnapshotSummary | null;
  syncState: SyncState;
  snapshotCreated: boolean;
  snapshotUpdated: boolean;
  createdEventCount: number;
};
