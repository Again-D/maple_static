import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { NextRequest } from "next/server";

import { exceedsImageSizeLimit, isImageRequestRateLimited } from "../lib/character-image-security";
import { GET } from "../app/api/character-image/route";

describe("character image proxy", () => {
  it("rejects malformed, unsupported, and cache-busting requests before fetching", async () => {
    const missingUrl = await GET(new NextRequest("http://localhost/api/character-image?v=1"));
    const unsupportedHost = await GET(new NextRequest("http://localhost/api/character-image?url=https%3A%2F%2Fexample.com%2Fimage.png&v=1"));
    const unexpectedQuery = await GET(new NextRequest("http://localhost/api/character-image?url=https%3A%2F%2Fopen.api.nexon.com%2Fstatic%2Fmaplestory%2Fcharacter%2Flook%2Fimage.png&v=1&cache-bust=2"));

    assert.equal(missingUrl.status, 400);
    assert.equal(unsupportedHost.status, 400);
    assert.equal(unexpectedQuery.status, 400);
  });

  it("limits client request bursts and oversized image responses", () => {
    for (let index = 0; index < 30; index += 1) {
      assert.equal(isImageRequestRateLimited("test-rate-limit"), false);
    }
    assert.equal(isImageRequestRateLimited("test-rate-limit"), true);
    assert.equal(exceedsImageSizeLimit(1_000_000), false);
    assert.equal(exceedsImageSizeLimit(1_000_001), true);
  });
});
