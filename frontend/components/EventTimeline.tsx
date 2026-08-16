import type { Timeline } from "../lib/api/types";

type EquipmentComparison = {
  slot: string;
  previousItemName: string;
  currentItemName: string;
  previous: Record<string, unknown>;
  current: Record<string, unknown>;
};

type EquipmentReplacementDetail = {
  changes?: EquipmentComparison[];
  combatPower?: {
    status?: "estimated" | "accompanied" | "unchanged" | "unavailable";
    message?: string;
    delta?: number;
    estimatedEquipmentContribution?: number;
  };
};

function replacementDetail(detail: Record<string, unknown> | null) {
  return detail as EquipmentReplacementDetail | null;
}

function formatDelta(value: number | undefined) {
  if (value === undefined) return null;
  return `${value > 0 ? "+" : ""}${value.toLocaleString("ko-KR")}`;
}

type EventTimelineProps = {
  timeline: Timeline;
  hasEnoughSnapshots: boolean;
};

export function EventTimeline({ timeline, hasEnoughSnapshots }: EventTimelineProps) {
  return (
    <section className="panel panel--wide">
      <p className="eyebrow">Timeline</p>
      <h2>성장 이벤트</h2>
      {timeline.events.length === 0 ? (
        <p className="empty-state">
          {hasEnoughSnapshots ? "아직 감지된 성장 이벤트가 없습니다." : "다음 스냅샷이 쌓이면 성장 이벤트가 표시됩니다."}
        </p>
      ) : (
        <ol className="timeline">
          {timeline.events.map((item) => (
            <li key={item.id} className="timeline__item">
              <div>
                <p className="timeline__date">{item.eventDate}</p>
                <h3>{item.title}</h3>
                <p className="timeline__description">{item.description ?? "세부 설명 없음"}</p>
                {item.eventType === "ITEM_REPLACED" && item.detail && Array.isArray(item.detail.changes) ? (() => {
                  const detail = replacementDetail(item.detail);
                  const combatPower = detail?.combatPower;
                  return <div data-testid="event-item-replaced-details" className="timeline__item-replaced-details">
                    {combatPower ? <p className="timeline__equipment-context" data-testid="equipment-combat-power-context">
                      {combatPower.message}{combatPower.delta !== undefined && combatPower.status !== "unchanged" ? ` (${formatDelta(combatPower.delta)})` : ""}
                      {combatPower.estimatedEquipmentContribution !== undefined ? ` · 추정 기여 ${formatDelta(combatPower.estimatedEquipmentContribution)}` : ""}
                    </p> : null}
                    {detail?.changes?.map((change) => (
                      <details key={`${change.slot}-${change.previousItemName}-${change.currentItemName}`} className="timeline__item-replaced-row">
                        <summary>{change.slot}: {change.previousItemName} → {change.currentItemName}</summary>
                        <div className="timeline__comparison-grid">
                          <ComparisonColumn title="변경 전" values={change.previous ?? {}} />
                          <ComparisonColumn title="변경 후" values={change.current ?? {}} />
                        </div>
                      </details>
                    ))}
                  </div>;
                })() : null}
              </div>
              <span className={`timeline__importance timeline__importance--${item.importanceLevel}`}>중요도 {item.importanceLevel}</span>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

function ComparisonColumn({ title, values }: { title: string; values: Record<string, unknown> }) {
  const entries = Object.entries(values).filter(([key, value]) => key !== "available" && value !== undefined && value !== null);
  if (entries.length === 0) return <div><h4>{title}</h4><p>비교 가능한 값이 없습니다.</p></div>;
  return <div><h4>{title}</h4><dl>{entries.map(([key, value]) => <div key={key}><dt>{key}</dt><dd>{typeof value === "object" ? JSON.stringify(value) : String(value)}</dd></div>)}</dl></div>;
}
