"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { fetchBossDamageAnalysis } from "../lib/api/client";
import type { ApiErrorCode, BossDamageAnalysis } from "../lib/api/types";
import { StateMessage } from "./StateMessage";

type AnalysisState = {
  loading: boolean;
  data: BossDamageAnalysis | null;
  errorCode: ApiErrorCode | null;
  errorMessage: string | null;
};

const initialState: AnalysisState = { loading: true, data: null, errorCode: null, errorMessage: null };

export function BossDamageAnalysisView({ name }: { name: string }) {
  const [state, setState] = useState<AnalysisState>(initialState);
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setState(initialState);
    void fetchBossDamageAnalysis(name).then((response) => {
      if (cancelled) return;
      if (response.success) {
        setState({ loading: false, data: response.data, errorCode: null, errorMessage: null });
      } else {
        setState({ loading: false, data: null, errorCode: response.error.code, errorMessage: response.error.message });
      }
    });
    return () => {
      cancelled = true;
    };
  }, [name, reloadToken]);

  if (state.loading) {
    return <section className="panel"><p className="eyebrow">Boss Analytics</p><h1>보스별 데미지 배율</h1><p>최신 스냅샷을 분석하는 중입니다.</p></section>;
  }

  if (state.errorCode === "CHARACTER_NOT_FOUND") {
    return <section className="panel"><StateMessage tone="warning" title="캐릭터를 찾을 수 없습니다." message={state.errorMessage ?? "닉네임을 다시 확인해 주세요."} /><Link className="refresh-button refresh-button--inline" href="/">다시 검색</Link></section>;
  }

  if (state.errorMessage || !state.data) {
    return <section className="panel"><StateMessage tone="error" title="분석을 불러오지 못했습니다." message={state.errorMessage ?? "잠시 후 다시 시도해 주세요."} /><button type="button" className="refresh-button refresh-button--inline" onClick={() => setReloadToken((current) => current + 1)}>다시 시도</button></section>;
  }

  const data = state.data;
  return <section className="panel boss-analysis"><div className="boss-analysis__heading"><div><p className="eyebrow">Boss Analytics</p><h1>보스별 데미지 배율</h1></div><span className="catalog-badge">Catalog {data.catalogVersion}<br />검토일 {data.catalogReviewedAt}</span></div><div className="boss-analysis__inputs"><span>보스 데미지 <strong>{data.bossDamagePercent == null ? "-" : `${data.bossDamagePercent}%`}</strong></span><span>방어율 무시 <strong>{data.ignoreDefensePercent == null ? "-" : `${data.ignoreDefensePercent}%`}</strong></span></div>{!data.available ? <StateMessage tone="warning" title="현재 스탯으로 계산할 수 없습니다." message="보스 데미지와 방어율 무시가 포함된 최신 스냅샷이 필요합니다." /> : <div className="boss-table" role="table" aria-label="보스별 데미지 배율"><div className="boss-table__row boss-table__header" role="row"><span>보스</span><span>방어율</span><span>예상 배율</span></div>{data.bosses.map((boss) => <div className="boss-table__row" role="row" key={boss.bossId}><span><strong>{boss.bossName}</strong><small>{boss.difficulty}</small></span><span>{boss.defenseRate}%</span><strong>{boss.effectiveDamageMultiplier == null ? "-" : `${boss.effectiveDamageMultiplier.toFixed(3)}x`}</strong></div>)}</div>}<p className="panel__note">{data.limitations}</p><p className="panel__note">카탈로그 출처: {data.catalogSource}</p></section>;
}
