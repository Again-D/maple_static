export function formatSignedNumber(value: number | null | undefined, unit = "") {
  if (value === null || value === undefined) {
    return "데이터 부족";
  }
  const sign = value > 0 ? "+" : value < 0 ? "-" : "";
  const absolute = Math.abs(value).toLocaleString("ko-KR");
  return `${sign}${absolute}${unit}`;
}

export function formatCompactNumber(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return "데이터 부족";
  }
  return value.toLocaleString("ko-KR");
}

export function formatPercent(value: number | null | undefined) {
  if (value === null || value === undefined) {
    return "데이터 부족";
  }
  return `${value > 0 ? "+" : ""}${value.toFixed(2)}%`;
}

export function formatDate(value: string | null | undefined) {
  if (!value) {
    return "데이터 부족";
  }
  return value;
}

export function formatTime(value: string | null | undefined) {
  if (!value) {
    return "기록 없음";
  }
  return value;
}
