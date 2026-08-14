"use client";

import { useEffect, useRef, useState } from "react";
import { fetchDashboard, fetchGrowthHistory, refreshCharacter } from "../lib/api/client";
import type { ApiResponse, DashboardData, MetricOption, RangeOption, RefreshData } from "../lib/api/types";
import { CharacterDashboardView } from "./CharacterDashboardView";

type DashboardState =
  | { status: "loading" }
  | { status: "ready"; data: DashboardData; banner: { title: string; message: string } | null }
  | { status: "not_found"; message: string }
  | { status: "error"; message: string };

type CharacterDashboardClientProps = {
  name: string;
};

type Banner = { title: string; message: string };

type ChartSubstate =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "error"; message: string };

export type DashboardClientApi = {
  fetchDashboard: (name: string) => Promise<ApiResponse<DashboardData>>;
  fetchGrowthHistory: (name: string, range: RangeOption, metric: MetricOption) => ReturnType<typeof fetchGrowthHistory>;
  refreshCharacter: (name: string) => Promise<ApiResponse<RefreshData>>;
};

const DEFAULT_RANGE: RangeOption = "7d";
const DEFAULT_METRIC: MetricOption = "combatPower";

const defaultApi: DashboardClientApi = {
  fetchDashboard,
  fetchGrowthHistory,
  refreshCharacter
};

function mergeSelectedChart(
  data: DashboardData,
  selectedRange: RangeOption,
  selectedMetric: MetricOption,
  historyResponse: Awaited<ReturnType<typeof fetchGrowthHistory>>
) {
  if (!historyResponse.success) {
    return {
      data: {
        ...data,
        chart: {
          ...data.chart,
          range: selectedRange,
          metric: selectedMetric
        }
      },
      chartSubstate: {
        status: "error",
        message: historyResponse.error.message
      } as ChartSubstate
    };
  }

  return {
    data: {
      ...data,
      chart: {
        range: selectedRange,
        metric: selectedMetric,
        hasEnoughSnapshots: historyResponse.data.hasEnoughSnapshots,
        points: historyResponse.data.points
      }
    },
    chartSubstate: { status: "idle" } as ChartSubstate
  };
}

export async function fetchSelectedChartHistory(params: {
  api: DashboardClientApi;
  name: string;
  data: DashboardData;
  selectedRange: RangeOption;
  selectedMetric: MetricOption;
}) {
  const historyResponse = await params.api.fetchGrowthHistory(params.name, params.selectedRange, params.selectedMetric);
  return mergeSelectedChart(params.data, params.selectedRange, params.selectedMetric, historyResponse);
}

export async function refreshDashboardWithSelectedHistory(params: {
  api: DashboardClientApi;
  name: string;
  currentData: DashboardData;
  selectedRange: RangeOption;
  selectedMetric: MetricOption;
}) {
  const refreshResponse = await params.api.refreshCharacter(params.name);
  if (!refreshResponse.success) {
    return {
      status: "refresh_failed" as const,
      data: params.currentData,
      banner: {
        title: refreshResponse.error.code === "RATE_LIMITED" ? "새로고침이 제한되었습니다." : "새로고침에 실패했습니다.",
        message: refreshResponse.error.message
      } as Banner
    };
  }

  const dashboardResponse = await params.api.fetchDashboard(params.name);
  if (!dashboardResponse.success) {
    if (dashboardResponse.error.code === "CHARACTER_NOT_FOUND") {
      return {
        status: "not_found" as const,
        message: dashboardResponse.error.message
      };
    }

    return {
      status: "error" as const,
      message: dashboardResponse.error.message
    };
  }

  const historyResult = await fetchSelectedChartHistory({
    api: params.api,
    name: params.name,
    data: dashboardResponse.data,
    selectedRange: params.selectedRange,
    selectedMetric: params.selectedMetric
  });

  return {
    status: "ready" as const,
    data: historyResult.data,
    chartSubstate: historyResult.chartSubstate,
    banner: {
      title: refreshResponse.data.snapshotUpdated ? "새로고침이 완료되었습니다." : "새로운 스냅샷을 저장했습니다.",
      message: `성장 이벤트 ${refreshResponse.data.createdEventCount}개가 반영되었습니다.`
    } as Banner
  };
}

export function CharacterDashboardClient({ name }: CharacterDashboardClientProps) {
  const [state, setState] = useState<DashboardState>({ status: "loading" });
  const [refreshing, setRefreshing] = useState(false);
  const [selectedRange, setSelectedRange] = useState<RangeOption>(DEFAULT_RANGE);
  const [selectedMetric, setSelectedMetric] = useState<MetricOption>(DEFAULT_METRIC);
  const [chartSubstate, setChartSubstate] = useState<ChartSubstate>({ status: "idle" });
  const selectorRequestIdRef = useRef(0);

  async function loadDashboard(options?: { banner?: Banner | null; preserveCurrentContent?: boolean }) {
    const banner = options?.banner ?? null;
    const preserveCurrentContent = options?.preserveCurrentContent ?? false;

    if (!preserveCurrentContent) {
      setState({ status: "loading" });
    }

    const response = await defaultApi.fetchDashboard(name);
    if (response.success) {
      setChartSubstate({ status: "idle" });
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
    setSelectedRange(DEFAULT_RANGE);
    setSelectedMetric(DEFAULT_METRIC);
    setChartSubstate({ status: "idle" });
    setState({ status: "loading" });
    void loadDashboard();
  }, [name]);

  useEffect(() => {
    if (state.status !== "ready") {
      return;
    }
    if (state.data.chart.range === selectedRange && state.data.chart.metric === selectedMetric) {
      return;
    }

    let cancelled = false;
    const requestId = selectorRequestIdRef.current + 1;
    selectorRequestIdRef.current = requestId;
    setChartSubstate({ status: "loading" });

    void (async () => {
      const historyResult = await fetchSelectedChartHistory({
        api: defaultApi,
        name,
        data: state.data,
        selectedRange,
        selectedMetric
      });
      if (cancelled || selectorRequestIdRef.current !== requestId) {
        return;
      }

      setState((current) => {
        if (current.status !== "ready") {
          return current;
        }
        return {
          status: "ready",
          data: historyResult.data,
          banner: current.banner
        };
      });
      setChartSubstate(historyResult.chartSubstate);
    })();

    return () => {
      cancelled = true;
    };
  }, [name, selectedMetric, selectedRange, state]);

  async function handleRefresh() {
    if (refreshing || state.status !== "ready") {
      return;
    }

    const currentData = state.data;
    setRefreshing(true);
    try {
      const refreshResult = await refreshDashboardWithSelectedHistory({
        api: defaultApi,
        name,
        currentData,
        selectedRange,
        selectedMetric
      });

      if (refreshResult.status === "refresh_failed") {
        setState({
          status: "ready",
          data: currentData,
          banner: refreshResult.banner
        });
        return;
      }

      if (refreshResult.status === "not_found") {
        setState({ status: "not_found", message: refreshResult.message });
        return;
      }

      if (refreshResult.status === "error") {
        setState({ status: "error", message: refreshResult.message });
        return;
      }

      setState({
        status: "ready",
        data: refreshResult.data,
        banner: refreshResult.banner
      });
      setChartSubstate(refreshResult.chartSubstate);
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

  const chartError = chartSubstate.status === "error" ? chartSubstate.message : null;

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
      selectedRange={selectedRange}
      selectedMetric={selectedMetric}
      onRangeChange={setSelectedRange}
      onMetricChange={setSelectedMetric}
      chartLoading={chartSubstate.status === "loading"}
      chartError={chartError}
    />
  );
}
