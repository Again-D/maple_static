import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { renderToStaticMarkup } from "react-dom/server";

import { SearchPageView } from "../components/SearchPageView";

describe("search page", () => {
  it("disables blank submit and shows guidance", () => {
    const html = renderToStaticMarkup(
      <SearchPageView
        nickname="   "
        submitting={false}
        feedback={null}
        onNicknameChange={() => undefined}
        onSubmit={() => undefined}
      />
    );

    assert.match(html, /검색/);
    assert.match(html, /disabled/);
    assert.match(html, /대시보드는/);
  });

  it("shows a pending submit state", () => {
    const html = renderToStaticMarkup(
      <SearchPageView
        nickname="Aries92"
        submitting={true}
        feedback="검색 중..."
        onNicknameChange={() => undefined}
        onSubmit={() => undefined}
      />
    );

    assert.match(html, /검색 중\.\.\./);
    assert.match(html, /aria-busy="true"/);
  });
});
