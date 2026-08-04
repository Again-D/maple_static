type StateMessageProps = {
  tone: "info" | "warning" | "error" | "success";
  title: string;
  message: string;
  actionLabel?: string;
};

export function StateMessage({ tone, title, message, actionLabel }: StateMessageProps) {
  const role = tone === "error" ? "alert" : "status";
  const live = tone === "error" ? "assertive" : "polite";

  return (
    <section className={`state-message state-message--${tone}`} aria-label={title} role={role} aria-live={live}>
      <strong>{title}</strong>
      <p>{message}</p>
      {actionLabel ? <span className="state-message__action">{actionLabel}</span> : null}
    </section>
  );
}
