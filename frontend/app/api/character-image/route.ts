import { NextRequest } from "next/server";
import { get } from "node:https";

import { exceedsImageSizeLimit, isImageRequestRateLimited } from "../../../lib/character-image-security";

const NEXON_IMAGE_HOST = "open.api.nexon.com";
const NEXON_IMAGE_PATH_PREFIX = "/static/maplestory/character/look/";
const NEXON_IMAGE_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36";
const IMAGE_PROXY_VERSION = "1";

function isAllowedImageUrl(url: URL) {
  return url.protocol === "https:"
    && url.hostname === NEXON_IMAGE_HOST
    && url.pathname.startsWith(NEXON_IMAGE_PATH_PREFIX);
}

function fetchNexonImage(imageUrl: URL) {
  return new Promise<{ body: Buffer; contentType: string }>((resolve, reject) => {
    const request = get(imageUrl, {
      headers: { "User-Agent": NEXON_IMAGE_USER_AGENT }
    }, response => {
      const contentType = response.headers["content-type"];
      if (response.statusCode !== 200 || contentType?.split(";", 1)[0] !== "image/png") {
        response.resume();
        reject(new Error("Nexon image request failed"));
        return;
      }

      const contentLength = Number(response.headers["content-length"]);
      if (Number.isFinite(contentLength) && exceedsImageSizeLimit(contentLength)) {
        response.resume();
        request.destroy(new Error("Nexon image is too large"));
        reject(new Error("Nexon image is too large"));
        return;
      }

      const chunks: Buffer[] = [];
      let receivedBytes = 0;
      response.on("data", chunk => {
        receivedBytes += chunk.length;
        if (exceedsImageSizeLimit(receivedBytes)) {
          request.destroy(new Error("Nexon image is too large"));
          return;
        }
        chunks.push(chunk);
      });
      response.on("end", () => resolve({ body: Buffer.concat(chunks), contentType }));
      response.on("error", reject);
    });
    request.setTimeout(10_000, () => request.destroy(new Error("Nexon image request timed out")));
    request.on("error", reject);
  });
}

export async function GET(request: NextRequest) {
  const requestedUrl = request.nextUrl.searchParams.get("url");
  const version = request.nextUrl.searchParams.get("v");
  const hasUnexpectedQuery = [...request.nextUrl.searchParams.keys()].some(key => key !== "url" && key !== "v");
  if (!requestedUrl || version !== IMAGE_PROXY_VERSION || hasUnexpectedQuery) {
    return new Response("Missing image URL", { status: 400 });
  }

  let imageUrl: URL;
  try {
    imageUrl = new URL(requestedUrl);
  } catch {
    return new Response("Invalid image URL", { status: 400 });
  }

  if (!isAllowedImageUrl(imageUrl)) {
    return new Response("Unsupported image URL", { status: 400 });
  }
  if (isImageRequestRateLimited(request.headers.get("x-forwarded-for"))) {
    return new Response("Too many image requests", { status: 429, headers: { "Retry-After": "60" } });
  }

  try {
    const image = await fetchNexonImage(imageUrl);

    return new Response(image.body, {
      headers: {
        "Content-Type": image.contentType,
        "Cache-Control": "public, max-age=86400, s-maxage=86400"
      }
    });
  } catch {
    return new Response("Character image unavailable", { status: 502 });
  }
}
