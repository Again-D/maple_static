import { formatCompactNumber, formatPercent } from "../lib/format";
import type { GrowthHistory, RangeOption, MetricOption } from "../lib/api/types";

type CombatPowerChartProps = {
  chart: GrowthHistory;
  hasEnoughSnapshots: boolean;
  selectedRange?: RangeOption;
  selectedMetric?: MetricOption;
  onRangeChange?: (range: RangeOption) => void;
  onMetricChange?: (metric: MetricOption) => void;
  chartLoading?: boolean;
  chartError?: string | null;
};

const ranges: Array<{ value: RangeOption; label: string; testId: string }> = [
  { value: "7d", label: "7일", testId: "growth-range-7d" },
  { value: "30d", label: "30일", testId: "growth-range-30d" },
  { value: "all", label: "전체", testId: "growth-range-all" }
];

const metrics: Array<{ value: MetricOption; label: string; testId: string }> = [
  { value: "combatPower", label: "전투력", testId: "growth-metric-combat-power" },
  { value: "level", label: "레벨", testId: "growth-metric-level" },
  { value: "expRate", label: "경험치율", testId: "growth-metric-exp-rate" },
  { value: "unionLevel", label: "유니온", testId: "growth-metric-union-level" },
  { value: "hexaMatrixLevelSum", label: "헥사 합계", testId: "growth-metric-hexa-sum" }
];

const metricLabels: Record<MetricOption, string> = {
  combatPower: "전투력",
  level: "레벨",
  expRate: "경험치율",
  unionLevel: "유니온 레벨",
  hexaMatrixLevelSum: "헥사 매트릭스 레벨 합계"
};

function metricValue(point: GrowthHistory["points"][number], metric: MetricOption) {
  const value = point[metric];
  return metric === "expRate" ? formatPercent(value) : formatCompactNumber(value);
}

export function CombatPowerChart({ chart, hasEnoughSnapshots, selectedRange, selectedMetric, onRangeChange, onMetricChange, chartLoading = false, chartError = null }: CombatPowerChartProps) {
  const points = chart.points;
  const range = selectedRange ?? chart.range;
  const metric = selectedMetric ?? chart.metric;
  const values = points.map((point) => point[metric]).filter((value): value is number => value !== null && value !== undefined);
  const maxValue = values.reduce((max, value) => Math.max(max, value), 0);
  const title = `${range === "7d" ? "최근 7일" : range === "30d" ? "최근 30일" : "전체 기간"} ${metricLabels[metric]} 성장`;

  return (
    <section className="panel panel--wide">
      <div className="growth-chart__heading">
        <div>
          <p className="eyebrow">Growth Insights</p>
          <h2>{title}</h2>
        </div>
        {chartLoading ? <span className="growth-chart__status" role="status">차트 갱신 중...</span> : null}
      </div>
      <div className="growth-chart__controls" aria-label="성장 차트 선택">
        <div className="growth-chart__control-group" role="group" aria-label="기간 선택">
          <span className="growth-chart__control-label">기간</span>
          <div className="growth-chart__buttons">
            {ranges.map((option) => (
              <button key={option.value} type="button" data-testid={option.testId} aria-pressed={range === option.value} className="growth-chart__button" onClick={() => onRangeChange?.(option.value)}>
                {option.label}
              </button>
            ))}
          </div>
        </div>
        <div className="growth-chart__control-group" role="group" aria-label="지표 선택">
          <span className="growth-chart__control-label">지표</span>
          <div className="growth-chart__buttons growth-chart__buttons--metrics">
            {metrics.map((option) => (
              <button key={option.value} type="button" data-testid={option.testId} aria-pressed={metric === option.value} className="growth-chart__button" onClick={() => onMetricChange?.(option.value)}>
                {option.label}
              </button>
            ))}
          </div>
        </div>
      </div>
      {chartError ? <p className="empty-state growth-chart__error" role="alert">{chartError} 선택을 바꿔 다시 시도해 주세요.</p> : null}
      {!chartError && !hasEnoughSnapshots ? <p className="empty-state">{metricLabels[metric]} 지표를 비교할 스냅샷이 아직 부족합니다. 서로 다른 날짜의 값이 두 개 이상 쌓이면 차트가 나타납니다.</p> : null}
      {hasEnoughSnapshots ? (
        <div className={`chart${chartLoading ? " chart--loading" : ""}`} aria-label={`${title} 차트`} aria-busy={chartLoading}>
          {points.map((point) => (
            <div className="chart__bar" key={point.snapshotDate}>
              <span className="chart__value">{metricValue(point, metric)}</span>
              <span className="chart__fill" style={{ height: maxValue > 0 && point[metric] != null ? `${Math.max(8, (point[metric]! / maxValue) * 100)}%` : "8%" }} />
              <span className="chart__label">{point.snapshotDate}</span>
            </div>
          ))}
        </div>
      ) : null}
    </section>
  );
}
