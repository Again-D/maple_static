import { CombatPowerChart } from "./CombatPowerChart";
import { EventTimeline } from "./EventTimeline";
import { ProfileHeader } from "./ProfileHeader";
import { StateMessage } from "./StateMessage";
import { SummaryCards } from "./SummaryCards";
import { SyncStatus } from "./SyncStatus";
import type { DashboardData } from "../lib/api/types";

type CharacterDashboardViewProps = {
  name: string;
  status: "loading" | "ready" | "not_found" | "error";
  data: DashboardData | null;
  errorMessage: string | null;
  banner: { title: string; message: string } | null;
  refreshing: boolean;
  onRefresh: () => void;
  onRetry: () => void;
};

function LoadingSkeleton({ name }: { name: string }) {
  return (
    <main className="shell">
      <section className="hero-card">
        <p className="eyebrow">Character Dashboard</p>
        <h1>{name}</h1>
        <p className="lede">대시보드를 불러오는 중입니다.</p>
      </section>
      <section className="panel panel--wide panel--skeleton">
        <div className="skeleton-line" />
        <div className="skeleton-grid">
          <div className="skeleton-card" />
          <div className="skeleton-card" />
          <div className="skeleton-card" />
          <div className="skeleton-card" />
        </div>
      </section>
    </main>
  );
}

export function CharacterDashboardView({ name, status, data, errorMessage, banner, refreshing, onRefresh, onRetry }: CharacterDashboardViewProps) {
  if (status === "loading") {
    return <LoadingSkeleton name={name} />;
  }

  if (status === "not_found") {
    return (
      <main className="shell">
        <section className="hero-card">
          <p className="eyebrow">Character Dashboard</p>
          <h1>{name}</h1>
          <StateMessage tone="warning" title="캐릭터를 찾을 수 없습니다." message={errorMessage ?? "닉네임을 다시 확인한 뒤 검색해 보세요."} actionLabel="다시 검색" />
        </section>
      </main>
    );
  }

  if (status === "error" || !data) {
    return (
      <main className="shell">
        <section className="hero-card">
          <p className="eyebrow">Character Dashboard</p>
          <h1>{name}</h1>
          <StateMessage tone="error" title="대시보드를 불러오지 못했습니다." message={errorMessage ?? "잠시 후 다시 시도해 주세요."} actionLabel="재시도" />
          <button type="button" className="refresh-button refresh-button--inline" onClick={onRetry}>
            다시 시도
          </button>
        </section>
      </main>
    );
  }

  const hasEnoughSnapshots = data.summary.hasEnoughSnapshots;

  return (
    <main className="shell shell--dashboard">
      <section className="hero-card hero-card--compact">
        <p className="eyebrow">Character Dashboard</p>
        <h1>{data.profile.name}</h1>
        <p className="lede">캐릭터의 현재 상태와 최근 7일 성장 흐름을 확인합니다.</p>
      </section>

      {banner ? <StateMessage tone="warning" title={banner.title} message={banner.message} actionLabel="기존 데이터 유지" /> : null}

      <ProfileHeader profile={data.profile} latestSnapshot={data.latestSnapshot} />
      <SyncStatus syncState={data.syncState} onRefresh={onRefresh} refreshing={refreshing} />
      <SummaryCards summary={data.summary} />
      <CombatPowerChart chart={data.chart} hasEnoughSnapshots={hasEnoughSnapshots} />
      <EventTimeline timeline={data.timeline} hasEnoughSnapshots={hasEnoughSnapshots} />
    </main>
  );
}
