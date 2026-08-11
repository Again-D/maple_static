const MAX_IMAGE_BYTES = 1_000_000;
const RATE_LIMIT_WINDOW_MS = 60_000;
const MAX_REQUESTS_PER_WINDOW = 30;
const MAX_TRACKED_CLIENTS = 10_000;

type RateLimitEntry = {
  count: number;
  resetAt: number;
};

const rateLimitEntries = new Map<string, RateLimitEntry>();

export function exceedsImageSizeLimit(size: number) {
  return size > MAX_IMAGE_BYTES;
}

export function isImageRequestRateLimited(forwardedFor: string | null) {
  const now = Date.now();
  const key = forwardedFor?.split(",", 1)[0]?.trim().slice(0, 64) || "unknown";
  const entry = rateLimitEntries.get(key);

  if (!entry && rateLimitEntries.size >= MAX_TRACKED_CLIENTS) {
    return true;
  }
  if (!entry || entry.resetAt <= now) {
    rateLimitEntries.set(key, { count: 1, resetAt: now + RATE_LIMIT_WINDOW_MS });
    return false;
  }

  entry.count += 1;
  return entry.count > MAX_REQUESTS_PER_WINDOW;
}
