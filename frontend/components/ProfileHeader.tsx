import { formatCompactNumber, formatPercent } from "../lib/format";
import type { CharacterProfile, SnapshotSummary } from "../lib/api/types";

const CHARACTER_IMAGE_PROXY_VERSION = "1";

type ProfileHeaderProps = {
  profile: CharacterProfile;
  latestSnapshot: SnapshotSummary | null;
};

export function ProfileHeader({ profile, latestSnapshot }: ProfileHeaderProps) {
  return (
    <section className="panel panel--wide">
      <div className="profile-header">
        <div className="profile-header__identity">
          {profile.imageUrl ? <img className="profile-header__image" src={`/api/character-image?url=${encodeURIComponent(profile.imageUrl)}&v=${CHARACTER_IMAGE_PROXY_VERSION}`} alt={`${profile.name} 캐릭터 이미지`} /> : <div className="profile-header__placeholder" aria-hidden="true" />}
          <div>
            <p className="eyebrow">Profile</p>
            <h2>{profile.name}</h2>
            <p className="profile-header__meta">
              {profile.worldName} · {profile.jobName}
            </p>
          </div>
        </div>

        <dl className="profile-header__facts">
          <div>
            <dt>레벨</dt>
            <dd>{latestSnapshot ? formatCompactNumber(latestSnapshot.level) : "데이터 부족"}</dd>
          </div>
          <div>
            <dt>경험치율</dt>
            <dd>{latestSnapshot ? formatPercent(latestSnapshot.expRate) : "데이터 부족"}</dd>
          </div>
          <div>
            <dt>전투력</dt>
            <dd>{latestSnapshot ? formatCompactNumber(latestSnapshot.combatPower) : "데이터 부족"}</dd>
          </div>
          <div>
            <dt>자동 추적</dt>
            <dd>{profile.isAutoTrack ? "활성" : "비활성"}</dd>
          </div>
        </dl>
      </div>
    </section>
  );
}
