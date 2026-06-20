# TradeLog — Build Tasks

> Tasks are ordered chronologically. Each task produces something **visually testable** in a browser before moving on.
> Read `TECHNICAL.md` before starting any task.
> Complete tasks in order — later tasks depend on earlier ones.

---

## Phase 0 — Backend Foundation

### T01 · Scaffold Spring Boot project
**Goal:** Runnable Spring Boot app with SQLite wired up.
- Init project: Java 25, Spring Boot 4.1, Spring Data JPA, Flyway, Xerial SQLite JDBC, `hibernate-community-dialects`
- `application.yml` configured (DB at `~/tradelog/tradelog.db`, port 8080, `ddl-auto: validate`)
- Write Flyway migrations V1–V5 (campaign, trade_entry, trade_leg, position, indexes)
- `TradeLogApplication` starts without error; Flyway creates schema on first run

**Test:** `java -jar` → app starts, no errors, `~/tradelog/tradelog.db` exists with correct schema.

---

### T02 · Campaign CRUD
**Goal:** Create, list, and close campaigns via REST.
- `Campaign` entity, `CampaignRepository`, `CampaignService`, `CampaignController`
- DTOs: `CreateCampaignRequest`, `CampaignResponse` (with computed fields: netCashFlow, costBasis, sharesHeld, openPositionCount)
- Endpoints: `GET /api/v1/campaigns`, `GET /api/v1/campaigns/{id}`, `POST /api/v1/campaigns`, `PATCH /api/v1/campaigns/{id}/close`
- `GlobalExceptionHandler` for 404/400/500

**Test:** `curl` or Postman — create a campaign, list it, close it. Verify response shape.

---

### T03 · Trade parser + save
**Goal:** Parse trade strings and persist trades with position materialization.
- `TradeInputParser` with options + stock regex (see TECHNICAL.md §6)
- `TradeEntry`, `TradeLeg` entities + repositories
- `TradeController`: `POST /api/v1/trades/parse` (no DB write), `POST /api/v1/trades` (transactional save)
- `Position` entity + `PositionService` (opening/closing/expired logic, avg price recalc)
- `GET /api/v1/positions?campaignId={id}`

**Test:** Parse `STO 5 SPY 480C 12/20 @2.35` → correct fields returned. Save it → position appears. Save a closing trade → position quantity reduces.

---

### T04 · Spring Boot serves Vue build
**Goal:** Spring Boot serves static files from Vue's `dist/` folder with SPA fallback.
- `WebConfig.java` — serves `dist/` as static resources, fallback `/index.html` for unknown paths
- Placeholder `index.html` in `src/main/resources/static/` to confirm wiring works

**Test:** `http://localhost:8080` serves the placeholder HTML. `http://localhost:8080/anything` also returns it (SPA fallback works).

---

## Phase 1 — Frontend Shell

### T05 · Scaffold Vue 3 project
**Goal:** Vue 3 + Vite + TypeScript + Vue Router + Pinia running.
- `npm create vue@latest` with TypeScript, Vue Router, Pinia selected
- Delete all boilerplate placeholder content
- Add Google Fonts `<link>` for Space Grotesk + Space Mono to `index.html`
- Create `src/assets/main.css` with all CSS custom properties from TECHNICAL.md §3.2, global reset, body font defaults, scrollbar styles

**Test:** `npm run dev` → blank dark page (`--background` color), Space Grotesk visible in browser devtools.

---

### T06 · Sidebar component + routing skeleton
**Goal:** Sidebar visible, all 5 routes navigable to empty views.
- `Sidebar.vue` — static, no data yet. Logo, nav links (Dashboard, Analytics), "+ New Campaign" button, footer
- 5 empty view components: `DashboardView`, `CampaignDetailView`, `AnalyticsView`, `NewCampaignView`
- `App.vue`: flex layout — `Sidebar` fixed left (196px) + `<RouterView>` fluid right
- Active nav state: 2px left bar in `--primary`, no background

**Test:** App loads with sidebar. Clicking Dashboard, Analytics navigates. Routes render (empty). Active link highlights correctly.

---

## Phase 2 — Dashboard

### T07 · Dashboard layout with mock data
**Goal:** Full dashboard screen visible with hardcoded data.
- Header bar: "OVERVIEW" label + "Dashboard" title (left) + 3 KPI stats (right)
- Active Campaigns section label + table
- Closed Campaigns section label + table
- `CampaignRow.vue` component (reusable table row)
- All columns from TECHNICAL.md §8 Screen 1
- P&L values colored: profit = `--color-profit`, loss = `--color-loss`
- Status chip component (Active/Closed)
- Footer aggregate row

**Test:** Dashboard renders pixel-close to `screenshots/01-dashboard.png`. Colors, fonts, spacing match DESIGN.md. Table rows hover state works.

---

### T08 · Wire Dashboard to backend
**Goal:** Real campaign data from the Spring Boot API.
- `src/types/index.ts` — `Campaign`, `TradeLeg`, `Position`, `ParsedTrade` interfaces (from TECHNICAL.md §7)
- `src/services/campaignService.ts` — `getCampaigns()`, `getCampaign(id)`, `createCampaign(req)`, `closeCampaign(id)` using `fetch`
- Pinia store: `tradeLogStore` with `campaigns` state, `fetchCampaigns()` action
- `DashboardView` calls `fetchCampaigns()` on mount; renders real data

**Test:** With backend running, dashboard shows real campaigns. Create a campaign via curl → refresh → it appears.

---

## Phase 3 — New Campaign

### T09 · New Campaign form
**Goal:** New Campaign screen with validation, wired to backend.
- Form fields: Ticker (uppercase, 1–5 chars, required), Label (optional), Start Date (defaults today), Notes (optional)
- Validation: ticker format, required fields
- Syntax reference card below form with 4 example trade strings
- Cancel (ghost) → navigate back to dashboard
- Submit → `POST /api/v1/campaigns` → on success navigate to new campaign's detail page
- Error handling: show API error if request fails

**Test:** Fill form → submit → new campaign appears on dashboard. Invalid ticker (e.g. "nvda1") → validation error shown. Matches `screenshots/05-new-campaign.png`.

---

## Phase 4 — Campaign Detail

### T10 · Campaign Detail header + stats strip (mock)
**Goal:** Detail screen header visible with static/mock data.
- Breadcrumb: "Dashboard › TICKER" with router link back
- Title row: ticker (large, Space Mono) + label + status badge
- Notes line
- Right side 5 stat pills: Cost Basis, Shares, Net Cash, Unrlz P&L, Curr Price (mock price from `MOCK_PRICES`)

**Test:** Navigate to any campaign → header renders. Matches top portion of `screenshots/02-campaign-detail.png`.

---

### T11 · Trade Entry Bar
**Goal:** Trade input with client-side parse preview.
- `TradeEntryBar.vue` — full-width monospace input with `›` prefix glyph
- `useTradeParser.ts` composable — implements parser logic from TECHNICAL.md §6 (identical to Java parser)
- As user types: parse on Enter or "Parse →" button click
- Invalid input: error message below bar in `--color-loss`, `data-sm`
- Valid input: emit parsed result (don't show Confirm Panel yet)
- Focus border: `--primary`. Error border: `--color-loss`.

**Test:** Type `STO 5 SPY 480C 12/20 @2.35` → Parse → no error, parsed fields logged to console. Type gibberish → error message appears.

---

### T12 · Open Positions + Trade History tables (mock)
**Goal:** Both tables visible with hardcoded data.
- `PositionRow.vue` — table row for open positions
- `TradeRow.vue` — table row for trade history
- Action coloring: STO=`--color-profit`, BTO/BTC=`--color-loss`, ASGN=`--color-warning`
- Strategy tag chip style
- Status text coloring (open/expired/closed/assigned)
- Net Cash Flow footer row on trade history table
- Open Positions table hidden if no open positions

**Test:** Both tables render with mock data. Matches `screenshots/02-campaign-detail.png` bottom half.

---

### T13 · Confirm Panel
**Goal:** Full trade entry flow works end-to-end.
- `ConfirmPanel.vue` — 340px fixed right panel
- Slide-in animation: `translateX(30px) → 0` + `opacity 0 → 1`, 0.15s ease
- Parsed fields displayed as key-value rows
- Strategy dropdown + Notes textarea
- Cancel / ✕ → close panel, restore input
- "Save Trade" → `POST /api/v1/trades` → close panel → green flash on entry bar → reload trades

**Test:** Type valid trade → Parse → Confirm Panel slides in. Fields correct. Save → trade appears in history table. Matches `screenshots/03-confirm-panel.png`.

---

### T14 · Wire Campaign Detail to backend
**Goal:** All campaign detail data from real API.
- `tradeService.ts` — `getTradesForCampaign(id)`, `parseTrade(rawInput, campaignId)`, `saveTrade(req)`
- `positionService.ts` — `getPositionsForCampaign(id)`
- Store: `fetchCampaign(id)`, `fetchTrades(campaignId)`, `fetchPositions(campaignId)` actions
- `CampaignDetailView` fetches all three on mount; computed stats (cost basis, net cash, shares) from real data
- "Close Campaign" button → `PATCH /api/v1/campaigns/{id}/close` → status badge updates

**Test:** Full trade entry flow with real data. Enter a CSP → save → appears in positions + trade history. Campaign net cash flow updates.

---

## Phase 5 — Analytics

### T15 · Analytics KPI cards (mock)
**Goal:** Analytics screen header and KPI row visible.
- Header: "PERFORMANCE" label + "Analytics" title
- 4 KPI cards: Total Premium, Net Options P&L, Win Rate, Closed Trades
- Card structure: `label-caps` label + `data-lg` value

**Test:** Analytics route renders 4 cards with mock values. Matches top of `screenshots/04-analytics.png`.

---

### T16 · SVG Bar Charts
**Goal:** P&L by Campaign and P&L by Strategy bar charts rendered.
- `BarChart.vue` — reusable horizontal bar chart, pure SVG
- Inputs: `items: { label: string, value: number }[]`
- Bars: `--color-profit` for positive, `--color-loss` for negative
- Value labels on bars
- No D3, no Chart.js — computed geometry in `<script setup>`

**Test:** Two bar charts visible with mock data. Positive and negative bars colored correctly. Matches left column of chart grid in `screenshots/04-analytics.png`.

---

### T17 · SVG Line Charts
**Goal:** Cumulative Premium and Cumulative Options P&L line charts rendered.
- `LineChart.vue` — reusable line + area chart, pure SVG
- Inputs: `points: { date: string, value: number }[]`
- Area fill: green gradient with low opacity
- X-axis date labels

**Test:** Two line charts visible with mock data. Matches right column of chart grid in `screenshots/04-analytics.png`.

---

### T18 · Wire Analytics to backend
**Goal:** All analytics data from real trades.
- Backend: aggregation queries for premium collected, P&L by strategy, cumulative time series
- Frontend: `analyticsService.ts` + store actions
- Wire all 4 charts and KPI cards to real data

**Test:** With real trade data, analytics charts reflect actual trade history. Adding a new trade updates analytics.

---

## Phase 6 — Polish

### T19 · Sidebar campaign list (live)
**Goal:** Sidebar shows real active/closed campaigns from store.
- Campaign list sections populated from Pinia store
- Clicking a campaign in sidebar navigates to its detail page
- Current campaign highlighted

**Test:** Create campaigns → they appear in sidebar under correct section. Click sidebar item → navigates to detail.

---

### T20 · Scanline overlay + final polish
**Goal:** Terminal aesthetic complete.
- `body::after` scanline overlay (see TECHNICAL.md §3.9)
- Global scrollbar styles applied
- Review all screens against screenshots for spacing/color discrepancies
- Empty states: no campaigns on dashboard, no trades on campaign detail

**Test:** Subtle scanline visible. Scrollbars styled. Empty states render gracefully.

---

## Deferred (not in scope for initial build)

- Multi-leg trade entry (calendar spread, PMCC opened in one action)
- Roll tracking (`closes_leg_id` set at save time)
- Campaign label editable after creation
- Real-time price feed (replace `MOCK_PRICES`)
- Campaign history view for multiple campaigns per ticker
- `EXPIRED` action end-to-end verification
- Partial close end-to-end verification
