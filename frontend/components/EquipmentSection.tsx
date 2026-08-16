import Link from "next/link";
import type { EquipmentData } from "../lib/api/types";

const IMAGE_PROXY_VERSION = "1";

function imageUrl(url: string | null) {
  return url ? `/api/character-image?url=${encodeURIComponent(url)}&v=${IMAGE_PROXY_VERSION}` : null;
}

export function EquipmentSection({ name, equipment }: { name: string; equipment?: EquipmentData }) {
  const items = equipment?.items ?? [];
  const candidates = equipment?.upgradeCandidates ?? [];

  return (
    <section className="panel panel--wide" aria-labelledby="equipment-heading">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Equipment</p>
          <h2 id="equipment-heading">현재 장비</h2>
        </div>
        {equipment?.snapshotDate ? <span className="section-heading__meta">기준일 {equipment.snapshotDate}</span> : null}
      </div>
      {items.length === 0 ? (
        <p className="empty-state">현재 장비 데이터를 확인할 수 없습니다.</p>
      ) : (
        <ul className="equipment-list">
          {items.map((item) => {
            const icon = imageUrl(item.iconUrl ?? item.shapeIconUrl);
            return (
              <li key={item.id} className="equipment-list__item">
                <Link className="equipment-list__link" href={`/character/${encodeURIComponent(name)}/equipment/${encodeURIComponent(item.id)}`}>
                  {icon ? <img className="equipment-list__icon" src={icon} alt="" /> : <span className="equipment-list__icon equipment-list__icon--placeholder" aria-hidden="true" />}
                  <span className="equipment-list__copy">
                    <span className="equipment-list__slot">{item.part} · {item.slot}</span>
                    <strong>{item.name}</strong>
                  </span>
                  <span className="equipment-list__arrow" aria-hidden="true">→</span>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
      {candidates.length > 0 ? (
        <div className="equipment-candidates" aria-labelledby="equipment-candidates-heading">
          <h3 id="equipment-candidates-heading">우선 검토 장비</h3>
          <ul>
            {candidates.map((candidate) => (
              <li key={`${candidate.itemId}-${candidate.category}`}>
                <Link href={`/character/${encodeURIComponent(name)}/equipment/${encodeURIComponent(candidate.itemId)}`}>
                  <strong>{candidate.itemName}</strong> · {candidate.category}
                </Link>
                <span>{candidate.reason}</span>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </section>
  );
}
