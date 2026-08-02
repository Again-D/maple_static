"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { buildCharacterRoute, canSubmitSearch, normalizeCharacterName } from "../lib/api/client";
import { SearchPageView } from "./SearchPageView";

export function SearchPageClient() {
  const router = useRouter();
  const [nickname, setNickname] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);

  function handleSubmit() {
    if (!canSubmitSearch(nickname, submitting)) {
      setFeedback("캐릭터 닉네임을 입력해 주세요.");
      return;
    }
    setFeedback(null);
    setSubmitting(true);
    router.push(buildCharacterRoute(normalizeCharacterName(nickname)));
  }

  return (
    <SearchPageView
      nickname={nickname}
      submitting={submitting}
      feedback={feedback}
      onNicknameChange={(value) => {
        setNickname(value);
        if (feedback) {
          setFeedback(null);
        }
      }}
      onSubmit={handleSubmit}
    />
  );
}
