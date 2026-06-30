# TradeLog — Product Manager Feature Recommendations

> Product perspective on what to build next, after all T01–T20 build tasks are complete.

---

## Already Shipped

- **Real-time stock price feed** — Yahoo Finance proxy, live prices on Campaign Detail `[SHIPPED]`
- **Multi-account support** — Multiple brokerage accounts tracked in parallel `[SHIPPED]`
- **Edit trades from history** — Position rebuild on edit `[SHIPPED]`
- **Multi-leg trade entry** — Comma-separated spread entry (vertical, iron condor, calendar) with compact confirm table and auto-detected strategy tags `[SHIPPED]`
- **Roll tracking** — Roll button on open option positions, Roll modal, atomic BTC+STO backend, `closes_leg_id` wired, roll badge (↻) with tooltips `[SHIPPED]`

---

## Tier 1 — Core Workflow Gaps (highest user impact)

1. **Multi-leg trade entry** `[SHIPPED]` — Comma-separated spread entry via the trade entry bar. Saved all legs atomically under one TradeEntry with hybrid strategy auto-detection (Bull Put Spread, Bear Call Spread, Iron Condor, etc.).

2. **Broker CSV/JSON import** — Manual entry is the #1 friction point. Supporting imports from IBKR, Schwab, Robinhood, TastyTrade, and TWS would dramatically reduce the barrier to getting trade data into the system. Even a generic CSV mapper would be transformative.

3. **Roll tracking** `[SHIPPED]` — Roll button on open option positions, Roll modal (BTC + new STO entry), atomic `POST /api/v1/trades/roll` backend, `closes_leg_id` wired on BTC leg, roll badge (↻) with tooltips on Trade History and Open Positions.

4. **Export (CSV + summary PDF)** — Users need to get data *out* for tax filing, sharing with their CPA, or importing into other tools. A campaign-level CSV export of trade legs and a summary report (realized P&L by ticker, short/long-term gain breakdown) would add significant utility.

## Tier 2 — Strategic Depth

5. **Wheel strategy dashboard** — A dedicated view that flips the perspective from "campaigns per ticker" to "wheel cycles": CSP opened → assigned → CC opened → assigned → P&L per complete cycle. This is the killer feature that differentiates TradeLog from a generic trade journal.

6. **Calendar / expiration radar** — A view showing all open options positions mapped to their expiration dates. Traders need to see what's expiring this week, next week, this month — at a glance. Could live under Analytics or as a new route.

7. **Trade journaling (structured notes)** — Beyond the free-text notes field, structured fields like "entry thesis," "exit reason," "lesson learned," and pre-defined tags (earnings, technical breakout, dividend capture, hedge). Turns the app from a transaction log into a learning tool.

8. **Advanced metrics** — Win/loss streaks, average hold time per strategy, max drawdown per campaign, Sharpe/MAR ratio, P&L by month calendar heatmap. The current Analytics screen is relatively basic — this is where the Bloomberg-terminal feel could really shine.

9. **DTE (Days to Expiration) counter on open positions** — Show DTE prominently on each open option position row, computed from `expiry - today`. Color-coded: green (>21 DTE), amber (8–21), red (<7). One of the most-referenced numbers when managing an options book, and trivially computed from existing data.

10. **Annualized return on capital (ROC) per trade** — For each closed option position: `(premium_collected / (strike × qty × 100)) × (365 / days_held)`. The primary metric wheel strategy traders use to compare trades across different strikes, expirations, and capital requirements. Should appear on the Trade History row and in Analytics alongside raw P&L.

11. **Breakeven price display on open option positions** — For each open short option position, show the breakeven price (e.g. CSP at 480P with $2.35 premium → breakeven $477.65). Core reference number — traders check this against the current stock price constantly. Computed entirely from existing position data.

## Tier 3 — Quality of Life

12. **Batch trade entry** — Paste a block of trade strings (e.g., from a broker statement or the user's own notes) and have them parsed and queued for batch confirmation.

13. **Dividend tracking** — Track dividends received on stock positions as a separate income line. Total return = P&L + dividends.

14. **Activity log / undo** — Every trade save, edit, and close logged internally so the user can see what changed. Pairs nicely with "edit trade" which already exists.

15. **Custom date range filter on Analytics** — Filter all analytics (charts and KPI cards) by a date range: This Month, This Year, Last Quarter, or a custom date picker. The current analytics screen has no time filtering at all — essential once trade history grows beyond a few months.

16. **Global trade search** — A search bar (or filter row) across all campaigns: filter by ticker, strategy tag, action type, or date range. Lets a user instantly find "all CSPs I've sold on NVDA" without navigating campaign by campaign.

17. **Total capital at risk summary** — A Dashboard KPI showing total capital currently at risk: sum of (stock position market value) + (option obligation = strike × qty × 100 for short puts). Gives a portfolio-level exposure snapshot without opening each campaign.

18. **Monthly premium income goal tracker** — Let the user set a monthly premium income target (stored in local config). The Dashboard shows `Month: $1,840 / $3,000` as a progress stat. Simple and motivating — directly aligned with the income-generation thesis of wheel trading.

## Tier 4 — Nice-to-Have

19. Option chain browser (inline, read-only) — requires live data source; significant complexity for a local-only tool
20. Position sizing calculator
21. Template-based quick entry for recurring trade structures
22. **Command palette (Cmd+K)** — Keyboard-driven quick launcher: navigate to any campaign, open the trade entry bar, jump to Analytics. Deeply aligned with the Bloomberg terminal aesthetic and rewards power users.
23. **One-click database backup** — A Settings page (or sidebar menu item) that copies `~/tradelog/tradelog.db` to a user-chosen path. Critical for a local-only app where a single file holds all trading history.
24. **Optional Greeks capture at trade entry** — Optional fields on the Confirm Panel: Delta, Theta, IV at time of entry. Not required — fields can be blank — but allows traders who track these to review them later. Could be stored in `trade_entry.notes` as JSON or a dedicated column.
25. **Upcoming expiration alerts** — Visual indicator (banner or sidebar badge) when any open option position expires within 7 days. No push notifications needed — a UI flag that appears on app load. Prevents the worst-case scenario: forgetting an open short position until it's too late.

---

## Recommended Priority Order

1. Multi-leg trade entry
2. Broker CSV import
3. Roll tracking
4. DTE counter on open positions
5. Breakeven price on open positions
6. Annualized ROC per trade
7. Wheel strategy dashboard
8. Export (CSV + summary report)
9. Calendar / expiration radar
10. Custom date range filter on Analytics
11. Trade journaling + advanced metrics
12. Global trade search
13. Total capital at risk summary
14. Monthly income goal tracker
15. Everything else
