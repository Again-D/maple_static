type StateMessageProps = {
  tone: "info" | "warning" | "error" | "success";
  title: string;
  message: string;
  actionLabel?: string;
};

export function StateMessage({ tone, title, message, actionLabel }: StateMessageProps) {
  return (
    <section className={`state-message state-message--${tone}`} aria-label={title}>
      <strong>{title}</strong>
      <p>{message}</p>
      {actionLabel ? <span className="state-message__action">{actionLabel}</span> : null}
    </section>
  );
}
