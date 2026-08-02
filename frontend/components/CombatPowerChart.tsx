import { formatCompactNumber } from "../lib/format";
import type { GrowthHistory } from "../lib/api/types";

type CombatPowerChartProps = {
  chart: GrowthHistory;
  hasEnoughSnapshots: boolean;
};

export function CombatPowerChart({ chart, hasEnoughSnapshots }: CombatPowerChartProps) {
  const points = chart.points;
  const maxCombat = points.reduce((max, point) => Math.max(max, point.combatPower ?? 0), 0);

  return (
    <section className="panel panel--wide">
      <p className="eyebrow">Combat Power</p>
      <h2>최근 7일 전투력</h2>
      {!hasEnoughSnapshots ? <p className="empty-state">비교할 스냅샷이 아직 부족합니다. 다음 일일 수집이 쌓이면 차트가 나타납니다.</p> : null}
      {hasEnoughSnapshots ? (
        <div className="chart" aria-label="전투력 차트">
          {points.map((point) => (
            <div className="chart__bar" key={point.snapshotDate}>
              <span className="chart__value">{formatCompactNumber(point.combatPower)}</span>
              <span className="chart__fill" style={{ height: maxCombat > 0 && point.combatPower != null ? `${Math.max(8, (point.combatPower / maxCombat) * 100)}%` : "8%" }} />
              <span className="chart__label">{point.snapshotDate}</span>
            </div>
          ))}
        </div>
      ) : null}
    </section>
  );
}
