import { CharacterShell } from "../../../../components/CharacterShell";
import { BossDamageAnalysisView } from "../../../../components/BossDamageAnalysisView";

export default function CharacterAnalyticsPage({ params }: { params: { name: string } }) {
  const name = decodeURIComponent(params.name);
  return <CharacterShell name={name} active="analytics"><main className="shell shell--analytics"><BossDamageAnalysisView name={name} /></main></CharacterShell>;
}
