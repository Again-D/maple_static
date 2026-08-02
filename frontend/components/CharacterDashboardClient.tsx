"use client";

import { useEffect, useState } from "react";
import { fetchDashboard, refreshCharacter } from "../lib/api/client";
import type { ApiFailure, DashboardData } from "../lib/api/types";
import { CharacterDashboardView } from "./CharacterDashboardView";

type DashboardState =
  | { status: "loading" }
  | { status: "ready"; data: DashboardData; banner: { title: string; message: string } | null }
  | { status: "not_found"; message: string }
  | { status: "error"; message: string };

type CharacterDashboardClientProps = {
  name: string;
};

function errorMessage(error: ApiFailure["error"]) {
  return error.message;
}

export function CharacterDashboardClient({ name }: CharacterDashboardClientProps) {
  const [state, setState] = useState<DashboardState>({ status: "loading" });
  const [refreshing, setRefreshing] = useState(false);

  async function loadDashboard(options?: { banner?: { title: string; message: string } | null; preserveCurrentContent?: boolean }) {
    const banner = options?.banner ?? null;
    const preserveCurrentContent = options?.preserveCurrentContent ?? false;

    if (!preserveCurrentContent) {
      setState({ status: "loading" });
    }

    const response = await fetchDashboard(name);
    if (response.success) {
      setState({ status: "ready", data: response.data, banner });
      return response.data;
    }

    if (response.error.code === "CHARACTER_NOT_FOUND") {
      setState({ status: "not_found", message: response.error.message });
      return null;
    }

    setState({ status: "error", message: response.error.message });
    return null;
  }

  useEffect(() => {
    setState({ status: "loading" });
    void loadDashboard();
  }, [name]);

  async function handleRefresh() {
    if (refreshing || state.status !== "ready") {
      return;
    }
    setRefreshing(true);
    try {
      const refreshResponse = await refreshCharacter(name);
      if (!refreshResponse.success) {
        setState({
          status: "ready",
          data: state.data,
          banner: {
            title: refreshResponse.error.code === "RATE_LIMITED" ? "새로고침이 제한되었습니다." : "새로고침에 실패했습니다.",
            message: refreshResponse.error.message
          }
        });
        return;
      }
      await loadDashboard({
        banner: {
          title: refreshResponse.data.snapshotUpdated ? "새로고침이 완료되었습니다." : "새로운 스냅샷을 저장했습니다.",
          message: `성장 이벤트 ${refreshResponse.data.createdEventCount}개가 반영되었습니다.`
        },
        preserveCurrentContent: true
      });
    } finally {
      setRefreshing(false);
    }
  }

  if (state.status === "loading") {
    return <CharacterDashboardView name={name} status="loading" data={null} errorMessage={null} banner={null} refreshing={false} onRefresh={() => undefined} onRetry={() => void loadDashboard()} />;
  }

  if (state.status === "not_found") {
    return <CharacterDashboardView name={name} status="not_found" data={null} errorMessage={state.message} banner={null} refreshing={false} onRefresh={() => undefined} onRetry={() => void loadDashboard()} />;
  }

  if (state.status === "error") {
    return <CharacterDashboardView name={name} status="error" data={null} errorMessage={state.message} banner={null} refreshing={false} onRefresh={() => undefined} onRetry={() => void loadDashboard()} />;
  }

  return (
    <CharacterDashboardView
      name={name}
      status="ready"
      data={state.data}
      errorMessage={null}
      banner={state.banner}
      refreshing={refreshing}
      onRefresh={() => void handleRefresh()}
      onRetry={() => void loadDashboard()}
    />
  );
}
