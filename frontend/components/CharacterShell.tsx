import Link from "next/link";
import type { ReactNode } from "react";

type CharacterShellProps = { name: string; active: "dashboard" | "analytics"; children: ReactNode };

export function CharacterShell({ name, active, children }: CharacterShellProps) {
  const encodedName = encodeURIComponent(name);
  return (
    <div className="app-shell">
      <aside className="side-nav" aria-label="캐릭터 메뉴">
        <Link className="brand-mark" href={`/character/${encodedName}`} aria-label="대시보드 홈">✦</Link>
        <nav>
          <Link aria-label="Dashboard" className={active === "dashboard" ? "side-nav__link is-active" : "side-nav__link"} href={`/character/${encodedName}`}>▦ <span>Dashboard</span></Link>
          <Link aria-label="Analytics" className={active === "analytics" ? "side-nav__link is-active" : "side-nav__link"} href={`/character/${encodedName}/analytics`}>◌ <span>Analytics</span></Link>
          <span aria-label="Skills (준비 중)" className="side-nav__link side-nav__link--disabled" aria-disabled="true">◇ <span>Skills</span></span>
          <Link aria-label="Equipment" className="side-nav__link" href={`/character/${encodedName}#equipment`}>◈ <span>Equipment</span></Link>
          <span aria-label="Events (준비 중)" className="side-nav__link side-nav__link--disabled" aria-disabled="true">♧ <span>Events</span></span>
          <span aria-label="Settings (준비 중)" className="side-nav__link side-nav__link--disabled" aria-disabled="true">⚙ <span>Settings</span></span>
        </nav>
      </aside>
      <div className="app-shell__content">
        <header className="dashboard-topbar">
          <div><p className="eyebrow">MapleStats Analytics</p><p className="dashboard-topbar__character">{name}</p></div>
          <div className="dashboard-topbar__status"><span className="status-dot" /> KST · live cache</div>
        </header>
        {children}
      </div>
    </div>
  );
}
