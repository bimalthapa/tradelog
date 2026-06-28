# TradeLog — Technical Reference

> This document is the single source of truth for the TradeLog project.
> It is written for Claude Code sessions — read this at the start of every conversation before touching any code.

---

## 1. What Is This App

TradeLog is a **local-only, single-user desktop tool** for tracking stock and options trades organized into **campaigns**. A campaign is tied to one ticker and spans its full lifecycle (CSPs, covered calls, assignments, stock positions) until the user explicitly closes it. Trading the same ticker again later starts a new campaign.

There is no cloud, no auth, no multi-tenancy. It runs on the user's machine as a single process.

---

## 2. Tech Stack

### Backend
| Layer | Choice | Notes |
|---|---|---|
| Runtime | Java 25 (LTS) | Current LTS as of Sept 2025; Java 21 enters end-of-free-updates Sept 2026 |
| Framework | Spring Boot 4.1 | Latest release; supports Java 17–26; serves Vue frontend as static files |
| Database | SQLite (Xerial JDBC) | File at `~/tradelog/tradelog.db`, outside project dir |
| ORM | Spring Data JPA / Hibernate | `ddl-auto: validate` only — never create/update |
| Migrations | Flyway | Owns all schema. Never edit existing migration files |
| Dialect | `hibernate-community-dialects` SQLiteDialect | |

### Frontend
| Layer | Choice | Notes |
|---|---|---|
| Framework | **Vue 3** | Composition API, `<script setup lang="ts">` |
| Build | Vite | Default Vue 3 tooling |
| Language | TypeScript | Strict mode |
| Router | Vue Router 4 | |
| State | Pinia | Global campaign/trade store |
| HTTP | `fetch` (native) | No Axios, no extra dependency |
| Styling | Plain CSS | CSS custom properties for design tokens. No SCSS, no Tailwind |
| Charts | Pure inline SVG | No D3, no Chart.js, no external chart libraries |
| Component libs | None | Fully custom — design system is hand-rolled |

### Why these choices (decided, not up for revisiting)
- **H2 rejected** in favor of SQLite: continuous file growth, 1TB ceiling, corruption risk. SQLite handles years of trade data fine.
- **Flyway over Hibernate DDL**: SQLite dialect in Hibernate 6+ is not first-class; explicit SQL migrations sidestep edge cases.
- **Spring Boot serves Vue build**: Single JAR, single port, no CORS, no proxy. `mvn clean install` → `java -jar`.
- **Vue 3 over React/Angular**: Composition API elegance for data-heavy UIs; JSX prototype files (Dashboard.jsx etc.) are adaptable reference material, not production code.
- **No lot tracking**: Partial closes use blended average cost only. No FIFO/LIFO/specific-lot. This is a firm product decision.
- **No charting libraries**: Pure SVG computed from data in Vue templates.

---

## 3. Design System

**Source of truth: `DESIGN.md`** (located at `/Users/bimalth/Downloads/DESIGN.md`)
The HTML prototype files in `/Users/bimalth/Downloads/design_handoff_tradelog/` are references for **layout and interactions only**. Their colors and fonts are ignored.

### 3.1 Brand Personality
Clinical, efficient, technical. Bloomberg Terminal × modern dark developer tool. Dense information layout, no decorative elements, tables over cards. Desktop-first; mobile not a priority.

### 3.2 CSS Custom Properties (from DESIGN.md)

```css
:root {
  /* Surfaces */
  --surface:                   #10131a;
  --surface-dim:               #10131a;
  --surface-bright:            #363941;
  --surface-container-lowest:  #0b0e15;
  --surface-container-low:     #191b23;
  --surface-container:         #1d2027;
  --surface-container-high:    #272a31;
  --surface-container-highest: #32353c;
  --background:                #10131a;

  /* Text on surfaces */
  --on-surface:         #e1e2ec;
  --on-surface-variant: #c2c6d6;
  --inverse-surface:    #e1e2ec;
  --inverse-on-surface: #2e3038;

  /* Borders */
  --outline:         #8c909f;
  --outline-variant: #424754;

  /* Primary (accent blue) */
  --primary:               #adc6ff;
  --on-primary:            #002e6a;
  --primary-container:     #4d8eff;
  --on-primary-container:  #00285d;
  --inverse-primary:       #005ac2;
  --surface-tint:          #adc6ff;

  /* Secondary (profit / credit / active — green) */
  --secondary:              #4ae176;
  --on-secondary:           #003915;
  --secondary-container:    #00b954;
  --on-secondary-container: #004119;

  /* Tertiary (loss / debit — red/pink) */
  --tertiary:              #ffb3b0;
  --on-tertiary:           #670211;
  --tertiary-container:    #ea6767;
  --on-tertiary-container: #5b000d;

  /* Error */
  --error:              #ffb4ab;
  --on-error:           #690005;
  --error-container:    #93000a;
  --on-error-container: #ffdad6;

  /* Semantic shortcuts */
  --color-profit:  var(--secondary);          /* positive P&L, credit cash flow */
  --color-loss:    var(--tertiary);           /* negative P&L, debit cash flow */
  --color-accent:  var(--primary);            /* links, active states, CTA buttons */
  --color-warning: #f59e0b;                   /* assignment / amber alerts */
}
```

### 3.3 Typography

```css
:root {
  --font-ui:   'Space Grotesk', sans-serif;  /* all UI: labels, copy, nav, buttons */
  --font-mono: 'Space Mono', monospace;       /* all data: numbers, tickers, trade strings, prices */
}
```

**Google Fonts import (in `index.html`):**
```html
<link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=Space+Mono:wght@400;700&display=swap" rel="stylesheet">
```

**Type scale (from DESIGN.md):**
| Token | Family | Size | Weight | Line Height | Letter Spacing |
|---|---|---|---|---|---|
| `display-lg` | Space Grotesk | 30px | 700 | 36px | -0.02em |
| `display-sm` | Space Grotesk | 20px | 600 | 28px | -0.01em |
| `body-md` | Space Grotesk | 14px | 400 | 20px | — |
| `label-caps` | Space Grotesk | 11px | 700 | 16px | 0.05em |
| `data-lg` | Space Mono | 18px | 700 | 24px | — |
| `data-md` | Space Mono | 13px | 500 | 18px | — |
| `data-sm` | Space Mono | 12px | 400 | 16px | — |

### 3.4 Layout & Spacing
- **Sidebar**: Fixed 196px left
- **Main viewport**: Fluid
- **Right panel**: Fixed 340px slide-in
- **Grid unit**: 4px base. Key values: 4, 8, 12, 16, 20, 24, 28px
- **Table row height**: 32px (dense, maximize visible data)
- **Gutter**: 12px

### 3.5 Shape
**Sharp corners everywhere.** 0px border-radius on buttons, inputs, containers, table cells, cards. This is the terminal/brutalist aesthetic.

Exceptions:
- Status dots: `50%`
- Scrollbar thumb: `3px`

### 3.6 Elevation
No shadows or blur. Use **tonal layering** (different surface levels) + 1px solid borders (`--outline-variant`) to define containers. Every edge is crisp.

Exception: right-side Confirm Panel gets `box-shadow: -8px 0 32px rgba(0,0,0,0.6)`.

### 3.7 Components Reference (from DESIGN.md)

**Tables** — core of the system
- Header: `--surface-container-low` bg, `label-caps` style, `--on-surface-variant` color
- Rows: 32px height, 1px bottom border (`--outline-variant`). Hover: subtle bg shift
- Data cells: `--font-mono`. Profit = `--color-profit`, Loss = `--color-loss`

**Trade Entry Input** (terminal style)
- No background, 1px bottom border only
- Green `›` prefix prompt character
- Font: `data-md`

**Buttons**
- Primary: `--primary-container` bg, `--on-primary-container` text, bold, 0px radius
- Ghost: transparent, 1px `--outline-variant` border
- Danger/Tertiary: text-only, `--color-loss`

**Sidebar nav active state**
- 2px left vertical bar in `--primary`. No background fill.

**Status chips** — Rectangular (0px radius), subtle bg tint + border + high-contrast text
- Active: `rgba(74,225,118,0.12)` bg, `--secondary` text
- Closed: `rgba(66,71,84,0.25)` bg, `--outline` text

**KPI Cards**
- `label-caps` on top, `data-lg` value below. 1px border, no shadow.

### 3.8 Scrollbars (global)
```css
::-webkit-scrollbar       { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: var(--surface-container-lowest); }
::-webkit-scrollbar-thumb { background: var(--outline-variant); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: var(--outline); }
```

### 3.9 Optional Scanline Overlay
```css
body::after {
  content: '';
  position: fixed;
  inset: 0;
  background: repeating-linear-gradient(
    0deg,
    transparent, transparent 2px,
    rgba(0,0,0,0.04) 2px, rgba(0,0,0,0.04) 4px
  );
  pointer-events: none;
  z-index: 9999;
}
```

---

## 4. Database Schema

```sql
CREATE TABLE campaign (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  ticker        TEXT NOT NULL,
  label         TEXT,
  status        TEXT NOT NULL DEFAULT 'OPEN',  -- OPEN | CLOSED
  notes         TEXT,
  opened_at     DATE NOT NULL,
  closed_at     DATE,
  realized_pnl  REAL
);

CREATE TABLE trade_entry (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  campaign_id  INTEGER NOT NULL REFERENCES campaign(id),
  entered_at   DATETIME NOT NULL,
  raw_input    TEXT,
  notes        TEXT,
  strategy_tag TEXT
);

CREATE TABLE trade_leg (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  trade_entry_id  INTEGER NOT NULL REFERENCES trade_entry(id),
  campaign_id     INTEGER NOT NULL REFERENCES campaign(id),
  instrument_type TEXT NOT NULL,   -- STOCK | OPTION
  action          TEXT NOT NULL,   -- BTO | STO | BTC | STC | ASSIGNED | EXPIRED
  ticker          TEXT NOT NULL,
  quantity        INTEGER NOT NULL,
  price           REAL NOT NULL,
  net_cash_flow   REAL NOT NULL,   -- negative = debit, positive = credit; fees folded in
  option_type     TEXT,            -- CALL | PUT, null for stock
  strike          REAL,
  expiry          DATE,
  closes_leg_id   INTEGER REFERENCES trade_leg(id),
  traded_at       DATE NOT NULL
);

CREATE TABLE position (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  campaign_id     INTEGER NOT NULL REFERENCES campaign(id),
  opening_leg_id  INTEGER REFERENCES trade_leg(id),
  instrument_type TEXT NOT NULL,
  ticker          TEXT NOT NULL,
  option_type     TEXT,
  strike          REAL,
  expiry          DATE,
  open_action     TEXT NOT NULL,   -- BTO | STO
  open_quantity   INTEGER NOT NULL,
  avg_price       REAL NOT NULL,
  status          TEXT NOT NULL DEFAULT 'OPEN',  -- OPEN | CLOSED
  opened_at       DATE NOT NULL,
  closed_at       DATE
);

CREATE INDEX idx_trade_leg_campaign ON trade_leg(campaign_id);
CREATE INDEX idx_trade_leg_closes   ON trade_leg(closes_leg_id);
CREATE INDEX idx_position_campaign  ON position(campaign_id);
CREATE INDEX idx_position_status    ON position(status);
CREATE INDEX idx_campaign_ticker    ON campaign(ticker);
CREATE INDEX idx_campaign_status    ON campaign(status);
```

**Schema notes:**
- `position` is a materialized projection over `trade_leg`, updated on every write. `trade_leg` is the source of truth.
- `trade_entry` groups one or more `trade_leg` rows opened together (e.g. a spread).
- `closes_leg_id` links a closing leg back to its opening leg. No FIFO/lot matching needed.
- Campaign P&L = `SUM(net_cash_flow)` on `trade_leg` filtered by `campaign_id` — no separate ledger table needed.

**Flyway migration files:**
```
V1__create_campaign.sql
V2__create_trade_entry.sql
V3__create_trade_leg.sql
V4__create_position.sql
V5__create_indexes.sql
```
Never edit existing migration files. New schema changes → new `Vn__` file.

---

## 5. Backend Structure

Root package: `com.tradelog`

```
com.tradelog
├── TradeLogApplication.java
├── campaign/
│   ├── Campaign.java
│   ├── CampaignRepository.java
│   ├── CampaignService.java
│   ├── CampaignController.java
│   └── dto/
│       ├── CreateCampaignRequest.java
│       └── CampaignResponse.java
├── trade/
│   ├── TradeEntry.java
│   ├── TradeLeg.java
│   ├── TradeEntryRepository.java
│   ├── TradeLegRepository.java
│   ├── TradeEntryService.java
│   ├── TradeController.java
│   ├── parser/
│   │   ├── TradeInputParser.java
│   │   └── ParsedTradeInput.java
│   └── dto/
├── position/
│   ├── Position.java
│   ├── PositionRepository.java
│   ├── PositionService.java
│   ├── PositionController.java
│   └── dto/
└── common/
    ├── exception/
    │   ├── ResourceNotFoundException.java
    │   └── GlobalExceptionHandler.java
    └── config/
        └── WebConfig.java
```

### application.yml
```yaml
spring:
  datasource:
    url: jdbc:sqlite:${user.home}/tradelog/tradelog.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
server:
  port: 8080
```

### API Endpoints

**Campaigns** (`/api/v1/campaigns`)
```
GET    /campaigns              list all, ordered opened_at DESC
GET    /campaigns/{id}         single campaign
POST   /campaigns              create
PATCH  /campaigns/{id}/close   set CLOSED + closed_at
```

`CampaignResponse` computed fields (JPQL, no native SQL):
- `netCashFlow` — SUM of net_cash_flow across all legs
- `costBasis` — net debit / shares held (null if no shares)
- `sharesHeld` — net STOCK legs (BTO+ASSIGNED add, STC subtracts)
- `openPositionCount` — count of OPEN positions

`CreateCampaignRequest` fields: `ticker` (required, 1–5 uppercase chars), `label` (optional), `notes` (optional), `openedAt` (required, ISO date).

**Trades** (`/api/v1/trades`)
```
GET  /trades?campaignId={id}   all legs for a campaign, ordered traded_at ASC
POST /trades/parse              parse raw string → parsed fields, no DB write
POST /trades                    save confirmed trade
```

`POST /trades/parse` accepts `{ rawInput, campaignId }`, returns parsed fields + `valid` flag + `error`.

`POST /trades` (`SaveTradeRequest`): `campaignId`, `rawInput`, `strategyTag`, `notes`, plus all parsed fields. Single `@Transactional`: creates `TradeEntry` + `TradeLeg`(s) + updates/creates `Position`.

**Positions** (`/api/v1/positions`)
```
GET /positions?campaignId={id}
```

### PositionService logic (invoked by TradeEntryService on every trade save)
- **Opening** (BTO/STO/ASSIGNED): find existing OPEN position by instrument key → update qty + recalculate avg price; else create new.
  - `avg_price = ((prev_qty × prev_avg) + (new_qty × new_price)) / (prev_qty + new_qty)`
- **Closing** (BTC/STC): reduce `open_quantity`; if reaches 0 → `CLOSED`.
- **EXPIRED**: set matching position `CLOSED`.
- Position matching key: `campaign_id + ticker + instrument_type + option_type + strike + expiry`

### Error handling
- `ResourceNotFoundException` → 404
- `MethodArgumentNotValidException` → 400 with field errors
- Generic → 500

---

## 6. Trade String Parser

Implemented identically in two places — **must stay in sync**:
- **Vue**: `useTradeParser` composable (client-side instant preview)
- **Java**: `TradeInputParser` (authoritative, used at save time)

**Options format:** `ACTION QTY TICKER STRIKE[C|P] EXPIRY @PRICE`
- Example: `STO 5 SPY 480C 12/20 @2.35`
- Regex: `/^(STO|BTO|BTC|STC)\s+(\d+)\s+([A-Z]+)\s+(\d+)(C|P)\s+(\d{1,2}\/\d{2,4})\s+@([\d.]+)$/i`

**Stock format:** `ACTION QTY TICKER @PRICE`
- Example: `BTO 100 NVDA @820.00`
- Regex: `/^(BTO|STC|ASGN)\s+(\d+)\s+([A-Z]+)\s+@([\d.]+)$/i`

**Cash flow calculation:**
- Options: `qty × price × 100`; positive for STO/STC, negative for BTO/BTC
- Stock: `qty × price`; positive for STC, negative for BTO/ASGN

**Strategy auto-tagging:**
- STO + PUT → CSP
- STO + CALL → CC
- BTO/BTC → Long / Close
- ASGN → Assignment

---

## 7. Frontend Structure

### Monorepo layout
```
tradelog/
├── TECHNICAL.md
├── TASKS.md
├── backend/                          # Spring Boot (Maven project root)
│   ├── pom.xml
│   └── src/
└── frontend/                         # Vue 3 + Vite
    ├── index.html
    ├── vite.config.ts                # build outDir: ../backend/src/main/resources/static
    └── src/
        ├── main.ts
        ├── App.vue                   # RouterView + Sidebar
        ├── router/
        │   └── index.ts
        ├── stores/
        │   └── tradeLog.ts           # Pinia store: campaigns, trades, positions
        ├── composables/
        │   └── useTradeParser.ts
        ├── services/
        │   ├── campaignService.ts
        │   ├── tradeService.ts
        │   └── positionService.ts
        ├── types/
        │   └── index.ts              # Campaign, TradeLeg, Position, ParsedTrade interfaces
        ├── assets/
        │   └── main.css              # global CSS: custom properties, reset, typography, scrollbars
        ├── views/
        │   ├── DashboardView.vue
        │   ├── CampaignDetailView.vue
        │   ├── AnalyticsView.vue
        │   └── NewCampaignView.vue
        └── components/
            ├── Sidebar.vue
            ├── dashboard/
            │   └── CampaignRow.vue
            ├── campaign/
            │   ├── TradeEntryBar.vue
            │   ├── TradeRow.vue
            │   ├── PositionRow.vue
            │   └── ConfirmPanel.vue
            └── analytics/
                ├── BarChart.vue
                └── LineChart.vue
```

### Build integration
`vite.config.ts` sets `build.outDir` to `../backend/src/main/resources/static` so `npm run build` (run from `frontend/`) drops the production bundle directly into the Spring Boot classpath. Spring Boot's `WebConfig.java` then serves it as static files with SPA fallback to `index.html`.

During development, run Vite dev server (`npm run dev` from `frontend/`) and the Spring Boot backend separately. Vite proxies `/api` calls to `localhost:8080`.

### Routing
```typescript
const routes = [
  { path: '/',              redirect: '/dashboard' },
  { path: '/dashboard',     component: DashboardView },
  { path: '/campaign/:id',  component: CampaignDetailView },
  { path: '/analytics',     component: AnalyticsView },
  { path: '/campaigns/new', component: NewCampaignView },
]
```

### TypeScript interfaces (match backend response shape exactly)
```typescript
interface Campaign {
  id: number
  ticker: string
  label?: string
  status: 'OPEN' | 'CLOSED'
  notes?: string
  openedAt: string
  closedAt?: string
  realizedPnl?: number
  netCashFlow: number
  costBasis?: number
  sharesHeld?: number
  openPositionCount: number
}

interface TradeLeg {
  id: number
  tradeEntryId: number
  campaignId: number
  instrumentType: 'STOCK' | 'OPTION'
  action: 'BTO' | 'STO' | 'BTC' | 'STC' | 'ASSIGNED' | 'EXPIRED'
  ticker: string
  quantity: number
  price: number
  netCashFlow: number
  optionType?: 'CALL' | 'PUT'
  strike?: number
  expiry?: string
  tradedAt: string
  strategyTag?: string
  notes?: string
}

interface Position {
  id: number
  campaignId: number
  instrumentType: 'STOCK' | 'OPTION'
  ticker: string
  optionType?: 'CALL' | 'PUT'
  strike?: number
  expiry?: string
  openAction: 'BTO' | 'STO'
  openQuantity: number
  avgPrice: number
  status: 'OPEN' | 'CLOSED'
  openedAt: string
  closedAt?: string
}

interface ParsedTrade {
  action: string
  qty: number
  ticker: string
  instrumentType: 'STOCK' | 'OPTION'
  optionType?: 'CALL' | 'PUT'
  strike?: number
  expiry?: string
  price: number
  cashFlow: number
  strategy: string
  valid: boolean
  error?: string
}
```

### Mock prices (stub until real feed)
```typescript
const MOCK_PRICES: Record<string, number> = {
  NVDA: 875.40,
  SPY:  502.18,
  TSLA: 155.30,
  AAPL: 185.92,
  AMD:  165.44,
}
```

---

## 8. Screen Specifications

Reference screenshots: `/Users/bimalth/Downloads/design_handoff_tradelog/screenshots/`
Reference prototypes: `/Users/bimalth/Downloads/design_handoff_tradelog/` (layout/interaction only, ignore colors/fonts)

### Screen 1: Dashboard (`/dashboard`)
- Header: screen label "OVERVIEW" + title "Dashboard" (left) + 3 KPI stats (right): Total Unrealized, Total Realized, Premium Collected
- Active Campaigns table + Closed Campaigns table, each preceded by a section label with colored status dot
- Table columns: Ticker | Label | Cost Basis | Shares | Open Pos | Net Cash | Unrlz P&L | Rlz P&L | Status | Started
- Clicking a row navigates to `/campaign/:id`
- Footer row: aggregate counts (Active Campaigns, Total Campaigns, Total Trades)

### Screen 2: Campaign Detail (`/campaign/:id`)
- Breadcrumb: "Dashboard › TICKER"
- Header: ticker (Space Mono, large) + label + status badge + notes line; right side 5 stat pills (Cost Basis, Shares, Net Cash, Unrlz P&L, Curr Price)
- **Trade Entry Bar**: full-width monospace input with `›` prefix, "Parse →" button. Most important UI element.
- Open Positions table (hidden if none)
- Trade History table
- Confirm Panel (slide-in overlay, 340px)

### Screen 3: Confirm Dialog (overlay on Campaign Detail)
- Centered modal dialog, 480px wide; full-screen backdrop `rgba(0,0,0,0.5)`
- Animation: `scale(0.97) → scale(1)` + `opacity 0 → 1`, 0.15s ease; clicking backdrop dismisses
- Two modes: `parse` (from trade entry bar) and `close` (from position row close button)
- **Parse mode** — Header: "CONFIRM TRADE" + ✕ close; 2-column grid of read-only parsed fields (ACTION/QTY, TICKER/INSTRUMENT, STRIKE/EXPIRY, PRICE/CASH FLOW); trade date input (plain text, defaults today); strategy input + notes textarea; Footer: Cancel (ghost) + "Save Trade" (primary)
- **Close mode** — Header: "CLOSE POSITION" + ✕ close; 2-column grid of read-only position fields; editable QTY (pre-filled with open quantity); editable EXIT PRICE; live computed CASH FLOW; trade date + notes; same footer
- On save (parse mode): green flash `rgba(74,225,118,0.06)` on entry bar background
- `SaveTradeRequest` includes optional `tradedAt: LocalDate`; if null, backend defaults to `LocalDate.now()`

### Screen 4: New Campaign (`/campaigns/new`)
- Max width 480px
- Fields: Ticker (required, uppercase, 1–5 chars), Label (optional), Start Date (defaults today), Notes (optional)
- Syntax reference card below form with 4 example trade strings
- Buttons: Cancel (ghost) + "Create Campaign →" (primary)

### Screen 5: Analytics (`/analytics`)
- Header: "PERFORMANCE" label + "Analytics" title
- 4 KPI cards: Total Premium, Net Options P&L, Win Rate, Closed Trades
- 2×2 SVG chart grid:
  - P&L by Campaign (horizontal bar)
  - P&L by Strategy (horizontal bar)
  - Cumulative Premium Collected (line + area)
  - Cumulative Options P&L (line + area)

### Sidebar (persistent across all screens)
- 196px fixed, full height, `--surface-container-lowest` bg, 1px right border
- Logo: 24×24px blue square with mini chart icon + "TradeLog" wordmark
- Nav: Dashboard, Analytics (active = 2px left bar, no bg fill)
- "ACTIVE" section: list of open campaigns with green status dot + ticker + label
- "CLOSED" section: list of closed campaigns with gray dot
- "+ New Campaign" dashed button at bottom
- Footer: `v0.1 · Mon YYYY` in `data-sm` style

---

## 9. Conventions

- **No lot tracking** — ever. Blended average cost only.
- **No native SQL** in repositories — JPQL `@Query` for all aggregates.
- **Flyway owns schema.** New Vn__ file for every change, never edit existing ones.
- **Parser logic must stay identical** between `useTradeParser.ts` (Vue) and `TradeInputParser.java`.
- **Frontend interfaces mirror backend DTOs exactly.** No mapping layer.
- **Design tokens only** — no hardcoded hex values in component styles, reference CSS custom properties.
- **Pure SVG for charts** — no libraries.
- **Local only.** Do not introduce auth, multi-tenancy, or cloud concerns.
- **CSS, not SCSS.** All styles in plain `.css` files or Vue `<style>` blocks.

---

## 10. Open Questions / Not Yet Decided

- **Real-time price feed**: Mock prices currently hardcoded. No decision made on a real feed (Alpha Vantage, Polygon, etc). Leave stub in place.
- **Multi-leg trade entry**: Schema supports it (`trade_entry` → multiple `trade_leg`), but the parser and UI currently handle one leg per entry. Deferred.
- **Roll tracking**: `closes_leg_id` column exists but nothing sets it at save time. Deferred.
- **Unrealized P&L on Campaign Detail**: Unclear if this will come from backend or computed frontend with mock prices. Deferred.

---

## 11. Reference Files

| File | Purpose |
|---|---|
| `/Users/bimalth/Downloads/HANDOFF.md` | Full engineering handoff from prior Claude web session |
| `/Users/bimalth/Downloads/DESIGN.md` | **Authoritative design system** — colors, typography, spacing, components |
| `/Users/bimalth/Downloads/design_handoff_tradelog/TradeLog.html` | Interactive prototype — layout/interaction reference only |
| `/Users/bimalth/Downloads/design_handoff_tradelog/screenshots/` | Screen-by-screen visual reference |
| `/Users/bimalth/Downloads/design_handoff_tradelog/*.jsx` | Component layout reference (React JSX, adapt to Vue) |
