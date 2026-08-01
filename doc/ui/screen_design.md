# 🎨 UI Screen Design & UX Architecture (메이플스토리 성장 분석 서비스)

본 문서는 **Maple Growth Tracker** 서비스의 주요 화면 구성, 디자인 시스템(컬러/폰트/글래스모피즘), 컴포넌트 레이아웃 명세를 정의합니다.

---

## 🖼️ 1. 대시보드 비주얼 목업 (Visual UI Mockup)

![Dashboard Preview](file:///e:/projects/Maple/doc/ui/maple_growth_dashboard_mockup.jpg)

---

## 🎨 2. 디자인 시스템 (Design System Tokens)

### 2.1. 컬러 팔레트 (Color Palette)
* **Background Primary**: `#0b0f19` (깊은 우주/다크 네이비)
* **Card Surface (Glassmorphism)**: `rgba(18, 26, 43, 0.75)` + `backdrop-filter: blur(16px)`
* **Primary Accent (Maple Gold)**: `#f59e0b` (단풍/골드 강조색)
* **Secondary Accent (Arcane Purple)**: `#a855f7` (아케인/헥사 보라빛)
* **Info Accent (Maple Cyan)**: `#06b6d4` (경험치/스탯 청록색)
* **Text Primary**: `#f8fafc` (밝은 백색)
* **Text Secondary**: `#94a3b8` (은은한 회색)

### 2.2. 타이포그래피 (Typography)
* **Font Family**: Pretentard, Inter, sans-serif
* **Heading 1**: 32px / Bold / 1.2
* **Heading 2**: 22px / SemiBold / 1.3
* **Body / Stat**: 15px / Regular / 1.5

---

## 📱 3. 페이지별 화면 정의서 (Page Specifications)

### PAGE 1: 메인 검색 페이지 (`/`)

#### 1. 히어로 섹션 (Hero Section)
* **로고 & 타이틀**: "Maple Growth Tracker - 내 캐릭터의 성장을 한눈에"
* **닉네임 검색바**: 
  * 텍스트 입력창 + 검색 아이콘
  * 최근 검색한 캐릭터 칩(Chip) 바로가기
  * 넥슨 API 실시간 닉네임 자동완성 / 검증

#### 2. 실시간 인기/주목받는 성장 캐릭터 카루셀
* 최근 전투력 상승폭이 높은 캐릭터 TOP 5 카드 노출.

---

### PAGE 2: 캐릭터 성장 분석 대시보드 (`/character/[name]`)

#### 1. 캐릭터 프로필 헤더 카드 (Character Profile Header)
* **왼쪽**: 넥슨 CDN 캐릭터 아바타 고화질 이미지
* **중앙**: 닉네임, 월드 뱃지, 직업, 현재 레벨(경험치 %), 현재 전투력 수치
* **오른쪽**: [동기화/새로고침] 버튼 & 최근 수집 일시 (예: "오늘 04:00 수집됨")

#### 2. 요약 지표 그리드 (Growth Summary Grid)
* **최근 7일 성장 요약 카드 4종**:
  1. 📈 **전투력 변동**: `+1,420,500 (+1.8%)` (네온 그린 상승 화살표)
  2. ⬆️ **레벨/경험치**: `Lv.285 (45.2%) → Lv.286 (1.5%)`
  3. ⚔️ **장비 교체/강화**: `2건 감지` (클릭 시 타임라인 이동)
  4. 🔮 **헥사/유니온**: `유니온 +120`, `헥사코어 +3`

#### 3. 성장 추이 인터랙티브 차트 (Interactive Growth Chart)
* **차트 필터**: [전투력] | [레벨/경험치] | [유니온] 탭 전환
* **기간 선택**: [7일] | [30일] | [전체]
* **차트 라이브러리**: Recharts 라인 차트 (호버 시 해당 날짜의 수치 툴팁 제공)

#### 4. 성장 타임라인 피드 (Growth Event Timeline)
* 날짜별 수직 타임라인 피드:
  * 뱃지 태그: `[레벨업]`, `[장비교체]`, `[헥사강화]`
  * **장비 교체 이벤트 카드**: 클릭 시 이전 장비 ↔ 새로 장착한 장비의 옵션 비교(Diff) 팝업/서랍 띄우기.

---

## 📜 4. 문서 파일 위치

본 UI 화면 정의 문서는 프로젝트 내부 [doc/ui/screen_design.md](file:///e:/projects/Maple/doc/ui/screen_design.md)에 저장되었습니다.
