import { EquipmentDetailClient } from "../../../../../components/EquipmentDetailClient";

type EquipmentDetailPageProps = {
  params: { name: string; id: string };
};

export default function EquipmentDetailPage({ params }: EquipmentDetailPageProps) {
  return <EquipmentDetailClient name={decodeURIComponent(params.name)} itemId={decodeURIComponent(params.id)} />;
}
