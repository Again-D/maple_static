import { formatCompactNumber } from "../lib/format";
import type { SnapshotSummary } from "../lib/api/types";

export function QuickOverview({ snapshot }: { snapshot: SnapshotSummary | null }) {
  const cards = [
    ["Level", snapshot?.level == null ? "-" : `Lv. ${snapshot.level}`],
    ["Combat Power", snapshot?.combatPower == null ? "-" : formatCompactNumber(snapshot.combatPower)],
    ["Union Level", snapshot?.unionLevel == null ? "-" : formatCompactNumber(snapshot.unionLevel)],
    ["Hexa Matrix", snapshot?.hexaMatrixLevelSum == null ? "-" : formatCompactNumber(snapshot.hexaMatrixLevelSum)]
  ];
  return <section className="quick-overview" aria-label="주요 지표">{cards.map(([label, value]) => <article className="quick-overview__card" key={label}><span>{label}</span><strong>{value}</strong></article>)}</section>;
}
