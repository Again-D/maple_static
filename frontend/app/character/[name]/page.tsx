type CharacterPageProps = {
  params: { name: string };
};

export default async function CharacterPage({ params }: CharacterPageProps) {
  const { name } = params;

  return (
    <main className="shell">
      <section className="hero-card">
        <p className="eyebrow">Character Dashboard</p>
        <h1>{decodeURIComponent(name)}</h1>
        <p className="lede">
          이 경로는 다음 단계에서 대시보드 상태, 차트, 이벤트 타임라인과 연결됩니다.
        </p>
      </section>
    </main>
  );
}
