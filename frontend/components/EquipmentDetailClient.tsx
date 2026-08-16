"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { fetchDashboard } from "../lib/api/client";
import { StateMessage } from "./StateMessage";
import type { EquipmentItem } from "../lib/api/types";

const IMAGE_PROXY_VERSION = "1";

const labels: Record<string, string> = {
  str: "힘",
  dex: "민첩",
  int: "지력",
  luk: "운",
  max_hp: "최대 HP",
  max_mp: "최대 MP",
  attack_power: "공격력",
  magic_power: "마력",
  armor: "방어력",
  speed: "이동속도",
  jump: "점프력",
  boss_damage: "보스 몬스터 공격 시 데미지",
  ignore_monster_armor: "몬스터 방어율 무시",
  all_stat: "올스탯",
  damage: "데미지"
};

function labelFor(key: string) {
  return labels[key] ?? key.replaceAll("_", " ");
}

function imageUrl(url: string | null) {
  return url ? `/api/character-image?url=${encodeURIComponent(url)}&v=${IMAGE_PROXY_VERSION}` : null;
}

function OptionGroup({ title, values }: { title: string; values: Record<string, string> }) {
  const entries = Object.entries(values);
  if (entries.length === 0) return null;
  return (
    <section className="equipment-detail__group">
      <h3>{title}</h3>
      <dl className="equipment-detail__stats">
        {entries.map(([key, value]) => <div key={key}><dt>{labelFor(key)}</dt><dd>{value}</dd></div>)}
      </dl>
    </section>
  );
}

function TextGroup({ title, values }: { title: string; values: string[] }) {
  if (values.length === 0) return null;
  return <section className="equipment-detail__group"><h3>{title}</h3><ul className="equipment-detail__text-list">{values.map((value) => <li key={value}>{value}</li>)}</ul></section>;
}

function EquipmentDetail({ name, item, snapshotDate, capturedAt }: { name: string; item: EquipmentItem; snapshotDate: string | null; capturedAt: string | null }) {
  const icon = imageUrl(item.iconUrl ?? item.shapeIconUrl);
  return (
    <main className="shell equipment-detail-page">
      <Link className="back-link" href={`/character/${encodeURIComponent(name)}`}>← 현재 장비 목록</Link>
      <section className="panel panel--wide equipment-detail__hero">
        {icon ? <img className="equipment-detail__icon" src={icon} alt={`${item.name} 아이템 이미지`} /> : <span className="equipment-detail__icon equipment-detail__icon--placeholder" aria-hidden="true" />}
        <div>
          <p className="eyebrow">{item.part} · {item.slot}</p>
          <h1>{item.name}</h1>
          <p className="equipment-detail__meta">현재 스냅샷 기준{snapshotDate ? ` · ${snapshotDate}` : ""}{capturedAt ? ` · 수집 ${capturedAt}` : ""}</p>
        </div>
      </section>
      <section className="panel panel--wide equipment-detail__groups">
        <OptionGroup title="기본 옵션" values={item.baseOptions} />
        <OptionGroup title="추가 옵션" values={item.additionalOptions} />
        <OptionGroup title="기타 옵션" values={item.etcOptions} />
        <OptionGroup title="스타포스 옵션" values={item.starforceOptions} />
        <OptionGroup title="종합 옵션" values={item.totalOptions} />
        <TextGroup title="잠재능력" values={item.potentialOptions} />
        <TextGroup title="에디셔널 잠재능력" values={item.additionalPotentialOptions} />
        {item.description ? <section className="equipment-detail__group"><h3>설명</h3><p>{item.description}</p></section> : null}
      </section>
    </main>
  );
}

export function EquipmentDetailClient({ name, itemId }: { name: string; itemId: string }) {
  const [state, setState] = useState<{ status: "loading" } | { status: "ready"; item: EquipmentItem; snapshotDate: string | null; capturedAt: string | null } | { status: "error"; message: string }>({ status: "loading" });

  useEffect(() => {
    let cancelled = false;
    void fetchDashboard(name).then((response) => {
      if (cancelled) return;
      if (!response.success) {
        setState({ status: "error", message: response.error.message });
        return;
      }
      const equipment = response.data.equipment;
      const item = equipment?.items.find((candidate) => candidate.id === itemId);
      if (!equipment || !item) {
        setState({ status: "error", message: "현재 스냅샷에서 장비를 찾을 수 없습니다." });
        return;
      }
      setState({ status: "ready", item, snapshotDate: equipment.snapshotDate, capturedAt: equipment.capturedAt });
    });
    return () => { cancelled = true; };
  }, [itemId, name]);

  if (state.status === "loading") return <main className="shell"><section className="panel panel--wide panel--skeleton"><div className="skeleton-line" /><div className="skeleton-card" /></section></main>;
  if (state.status === "error") return <main className="shell"><section className="panel panel--wide"><Link className="back-link" href="..">← 현재 장비 목록</Link><StateMessage tone="error" title="장비 상세를 불러오지 못했습니다." message={state.message} actionLabel="현재 장비 목록으로 돌아가기" /></section></main>;
  return <EquipmentDetail name={name} item={state.item} snapshotDate={state.snapshotDate} capturedAt={state.capturedAt} />;
}
