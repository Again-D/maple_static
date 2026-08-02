"use client";

import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";

export default function HomePage() {
  const router = useRouter();
  const [nickname, setNickname] = useState("");
  const [message, setMessage] = useState<string | null>(null);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmed = nickname.trim();
    if (!trimmed) {
      setMessage("캐릭터 닉네임을 입력해 주세요.");
      return;
    }
    setMessage(null);
    router.push(`/character/${encodeURIComponent(trimmed)}`);
  }

  return (
    <main className="shell">
      <section className="hero-card">
        <p className="eyebrow">Maple Growth Tracker</p>
        <h1>닉네임으로 성장 흐름을 바로 확인하세요.</h1>
        <p className="lede">
          익명으로 검색하고, 캐릭터의 현재 상태와 7일 성장 흐름을 한 화면에서 확인하는 MVP입니다.
        </p>

        <form className="search-form" onSubmit={handleSubmit}>
          <label className="sr-only" htmlFor="nickname">
            캐릭터 닉네임
          </label>
          <input
            id="nickname"
            name="nickname"
            type="text"
            placeholder="Aries92"
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
            autoComplete="off"
            spellCheck={false}
          />
          <button type="submit">검색</button>
        </form>

        {message ? <p className="feedback">{message}</p> : <p className="hint">대시보드는 `/character/[name]` 경로로 열립니다.</p>}
      </section>

      <section className="feature-grid" aria-label="기능 안내">
        <article>
          <h2>현재 상태</h2>
          <p>닉네임, 월드, 직업, 레벨, 경험치율, 이미지 정보를 보여줍니다.</p>
        </article>
        <article>
          <h2>최근 7일</h2>
          <p>전투력 추이와 성장 이벤트를 대표 스냅샷 기준으로 계산합니다.</p>
        </article>
        <article>
          <h2>새로고침</h2>
          <p>수동 새로고침은 기존 대시보드를 유지한 채 최신 수집 결과를 반영합니다.</p>
        </article>
      </section>
    </main>
  );
}
