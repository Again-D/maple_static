import { formatCompactNumber, formatPercent, formatSignedNumber } from "../lib/format";
import type { GrowthSummary } from "../lib/api/types";

type SummaryCardsProps = {
  summary: GrowthSummary;
};

export function SummaryCards({ summary }: SummaryCardsProps) {
  const hasEnough = summary.hasEnoughSnapshots;

  return (
    <section className="panel panel--wide">
      <p className="eyebrow">Summary</p>
      <div className="summary-grid">
        <article>
          <h3>전투력 변화</h3>
          <strong>{hasEnough ? formatSignedNumber(summary.combatPowerDelta) : "데이터 부족"}</strong>
          <span>{hasEnough ? formatPercent(summary.combatPowerDeltaRate) : "비교 가능한 스냅샷이 더 필요합니다."}</span>
        </article>
        <article>
          <h3>레벨 변화</h3>
          <strong>{hasEnough ? formatSignedNumber((summary.levelTo ?? 0) - (summary.levelFrom ?? 0)) : "데이터 부족"}</strong>
          <span>
            {summary.levelFrom != null && summary.levelTo != null ? `Lv.${formatCompactNumber(summary.levelFrom)} -> Lv.${formatCompactNumber(summary.levelTo)}` : "데이터 부족"}
          </span>
        </article>
        <article>
          <h3>헥사 변화</h3>
          <strong>{hasEnough ? formatSignedNumber(summary.hexaMatrixLevelDelta) : "데이터 부족"}</strong>
          <span>{summary.hexaMatrixLevelDelta === null ? "헥사 수치가 아직 없습니다." : "최근 7일 기준 변화"}</span>
        </article>
        <article>
          <h3>유니온 변화</h3>
          <strong>{hasEnough ? formatSignedNumber(summary.unionLevelDelta) : "데이터 부족"}</strong>
          <span>{summary.unionLevelDelta === null ? "유니온 수치가 아직 없습니다." : "최근 7일 기준 변화"}</span>
        </article>
      </div>
    </section>
  );
}
