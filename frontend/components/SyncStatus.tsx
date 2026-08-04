import { formatTime } from "../lib/format";
import type { SyncState } from "../lib/api/types";

type SyncStatusProps = {
  syncState: SyncState;
  onRefresh: () => void;
  refreshing: boolean;
};

export function SyncStatus({ syncState, onRefresh, refreshing }: SyncStatusProps) {
  return (
    <section className="panel panel--wide sync-status" aria-busy={refreshing}>
      <div>
        <p className="eyebrow">Sync</p>
        <h2>{syncState.state === "fresh" ? "최신 상태" : syncState.state === "failed_with_cache" ? "캐시 유지" : syncState.state === "failed_empty" ? "데이터 없음" : "동기화 상태"}</h2>
        <p className="sync-status__message">{syncState.message}</p>
        <p className="sync-status__meta">
          마지막 성공: {formatTime(syncState.lastSuccessAt)} · 마지막 시도: {formatTime(syncState.lastAttemptAt)}
        </p>
      </div>
      <button type="button" className="refresh-button" onClick={onRefresh} disabled={refreshing}>
        {refreshing ? "새로고침 중..." : "새로고침"}
      </button>
    </section>
  );
}
