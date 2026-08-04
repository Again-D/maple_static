import { CharacterDashboardClient } from "../../../components/CharacterDashboardClient";

type CharacterPageProps = {
  params: {
    name: string;
  };
};

export default function CharacterPage({ params }: CharacterPageProps) {
  return <CharacterDashboardClient name={decodeURIComponent(params.name)} />;
}
