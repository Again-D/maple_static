import type { Timeline } from "../lib/api/types";

type EventTimelineProps = {
  timeline: Timeline;
  hasEnoughSnapshots: boolean;
};

export function EventTimeline({ timeline, hasEnoughSnapshots }: EventTimelineProps) {
  return (
    <section className="panel panel--wide">
      <p className="eyebrow">Timeline</p>
      <h2>성장 이벤트</h2>
      {timeline.items.length === 0 ? (
        <p className="empty-state">
          {hasEnoughSnapshots ? "아직 감지된 성장 이벤트가 없습니다." : "다음 스냅샷이 쌓이면 성장 이벤트가 표시됩니다."}
        </p>
      ) : (
        <ol className="timeline">
          {timeline.items.map((item) => (
            <li key={item.id} className="timeline__item">
              <div>
                <p className="timeline__date">{item.eventDate}</p>
                <h3>{item.title}</h3>
                <p className="timeline__description">{item.description ?? "세부 설명 없음"}</p>
              </div>
              <span className={`timeline__importance timeline__importance--${item.importanceLevel}`}>중요도 {item.importanceLevel}</span>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
