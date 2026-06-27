<script setup lang="ts">
import { computed, onMounted } from 'vue'
import TradeEntryBar from '@/components/campaign/TradeEntryBar.vue'
import PositionRow from '@/components/campaign/PositionRow.vue'
import TradeRow from '@/components/campaign/TradeRow.vue'
import { useRoute } from 'vue-router'
import { useTradeLogStore } from '@/stores/tradeLog'
import { MOCK_PRICES } from '@/types/index'
import type { ParsedTrade, Position, TradeLeg } from '@/types/index'

const route = useRoute()
const store = useTradeLogStore()

onMounted(() => store.fetchCampaigns())

const campaign = computed(() =>
  store.campaigns.find(c => c.id === Number(route.params.id))
)

const mockPrice = computed(() =>
  campaign.value ? (MOCK_PRICES[campaign.value.ticker] ?? null) : null
)

const unrealizedPnl = computed(() => {
  const c = campaign.value
  if (!c || !c.sharesHeld || c.costBasis == null) return 0
  return c.sharesHeld * ((MOCK_PRICES[c.ticker] ?? 0) - c.costBasis)
})

function formatCurrency(value: number | null | undefined): string {
  if (value == null) return '—'
  return value.toLocaleString('en-US', { style: 'currency', currency: 'USD' })
}

function formatSigned(value: number): string {
  const sign = value >= 0 ? '+' : ''
  return sign + formatCurrency(value)
}

function onParsed(_trade: ParsedTrade) {
  // Confirm Panel wired in T13
}

const MOCK_POSITIONS: Position[] = [
  {
    id: 1,
    campaignId: 1,
    instrumentType: 'OPTION',
    ticker: 'SPY',
    optionType: 'PUT',
    strike: 480,
    expiry: '2024-12-20',
    openAction: 'STO',
    openQuantity: 5,
    avgPrice: 2.35,
    status: 'OPEN',
    openedAt: '2024-11-01',
  },
  {
    id: 2,
    campaignId: 1,
    instrumentType: 'OPTION',
    ticker: 'SPY',
    optionType: 'CALL',
    strike: 510,
    expiry: '2024-12-20',
    openAction: 'STO',
    openQuantity: 3,
    avgPrice: 1.80,
    status: 'OPEN',
    openedAt: '2024-11-08',
  },
]

const MOCK_TRADES: TradeLeg[] = [
  {
    id: 1, tradeEntryId: 1, campaignId: 1,
    instrumentType: 'OPTION', action: 'STO', ticker: 'SPY',
    quantity: 5, price: 2.35, netCashFlow: 1175,
    optionType: 'PUT', strike: 480, expiry: '2024-12-20',
    tradedAt: '2024-11-01', strategyTag: 'CSP',
  },
  {
    id: 2, tradeEntryId: 2, campaignId: 1,
    instrumentType: 'OPTION', action: 'STO', ticker: 'SPY',
    quantity: 3, price: 1.80, netCashFlow: 540,
    optionType: 'CALL', strike: 510, expiry: '2024-12-20',
    tradedAt: '2024-11-08', strategyTag: 'CC',
  },
  {
    id: 3, tradeEntryId: 3, campaignId: 1,
    instrumentType: 'OPTION', action: 'BTC', ticker: 'SPY',
    quantity: 2, price: 0.50, netCashFlow: -100,
    optionType: 'PUT', strike: 480, expiry: '2024-12-20',
    tradedAt: '2024-11-15', strategyTag: 'CSP',
  },
  {
    id: 4, tradeEntryId: 4, campaignId: 1,
    instrumentType: 'OPTION', action: 'EXPIRED', ticker: 'SPY',
    quantity: 1, price: 0, netCashFlow: 0,
    optionType: 'CALL', strike: 510, expiry: '2024-12-20',
    tradedAt: '2024-12-20', strategyTag: 'CC',
  },
  {
    id: 5, tradeEntryId: 5, campaignId: 1,
    instrumentType: 'STOCK', action: 'BTO', ticker: 'SPY',
    quantity: 100, price: 498.50, netCashFlow: -49850,
    tradedAt: '2024-12-20',
  },
]

const mockNetCashFlow = MOCK_TRADES.reduce((sum, t) => sum + t.netCashFlow, 0)
</script>

<template>
  <div class="campaign-detail">

    <template v-if="store.loading && !campaign">
      <div class="state-msg">Loading…</div>
    </template>

    <template v-else-if="!campaign">
      <div class="state-msg state-msg--error">Campaign not found</div>
    </template>

    <template v-else>
      <div class="header">

        <!-- Left column -->
        <div class="header-left">
          <div class="breadcrumb">
            <RouterLink to="/dashboard" class="breadcrumb-link">Dashboard</RouterLink>
            <span class="breadcrumb-sep"> › </span>
            <span class="breadcrumb-ticker">{{ campaign.ticker }}</span>
          </div>

          <div class="title-row">
            <span class="ticker">{{ campaign.ticker }}</span>
            <span v-if="campaign.label" class="campaign-label">{{ campaign.label }}</span>
            <span
              class="badge"
              :class="campaign.status === 'OPEN' ? 'badge-open' : 'badge-closed'"
            >
              {{ campaign.status === 'OPEN' ? 'ACTIVE' : 'CLOSED' }}
            </span>
          </div>

          <div v-if="campaign.notes" class="notes">{{ campaign.notes }}</div>
        </div>

        <!-- Stat strip -->
        <div class="stat-strip">
          <div class="stat">
            <div class="stat-label">COST BASIS</div>
            <div class="stat-value">{{ formatCurrency(campaign.costBasis) }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">SHARES</div>
            <div class="stat-value">{{ campaign.sharesHeld ?? '—' }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">NET CASH</div>
            <div
              class="stat-value"
              :class="campaign.netCashFlow >= 0 ? 'profit' : 'loss'"
            >
              {{ formatSigned(campaign.netCashFlow) }}
            </div>
          </div>
          <div class="stat">
            <div class="stat-label">UNRLZ P&amp;L</div>
            <div
              class="stat-value"
              :class="unrealizedPnl >= 0 ? 'profit' : 'loss'"
            >
              {{ formatSigned(unrealizedPnl) }}
            </div>
          </div>
          <div class="stat">
            <div class="stat-label">CURR PRICE</div>
            <div class="stat-value">{{ formatCurrency(mockPrice) }}</div>
          </div>
        </div>

      </div><!-- end .header -->

      <TradeEntryBar @parsed="onParsed" />

      <!-- Open Positions -->
      <div v-if="MOCK_POSITIONS.length > 0" class="section positions-section">
        <div class="section-label">
          <span class="dot">●</span>
          OPEN POSITIONS
          <span class="count">({{ MOCK_POSITIONS.length }})</span>
        </div>
        <table class="table">
          <thead>
            <tr class="thead-row">
              <th class="th">OPENED</th>
              <th class="th">ACTION</th>
              <th class="th">QTY</th>
              <th class="th">INSTRUMENT</th>
              <th class="th">AVG PRICE</th>
              <th class="th">STATUS</th>
            </tr>
          </thead>
          <tbody>
            <PositionRow v-for="pos in MOCK_POSITIONS" :key="pos.id" :position="pos" />
          </tbody>
        </table>
      </div>

      <!-- Trade History -->
      <div class="section history-section">
        <div class="section-label">
          TRADE HISTORY
          <span class="count">({{ MOCK_TRADES.length }})</span>
        </div>
        <table class="table">
          <thead>
            <tr class="thead-row">
              <th class="th">DATE</th>
              <th class="th">ACTION</th>
              <th class="th">QTY</th>
              <th class="th">INSTRUMENT</th>
              <th class="th">PRICE</th>
              <th class="th">CASH FLOW</th>
              <th class="th">STRATEGY</th>
              <th class="th">STATUS</th>
            </tr>
          </thead>
          <tbody>
            <TradeRow v-for="trade in MOCK_TRADES" :key="trade.id" :trade="trade" />
          </tbody>
        </table>
        <div class="ncf-footer">
          <span class="ncf-label">NET CASH FLOW</span>
          <span class="ncf-value" :class="mockNetCashFlow >= 0 ? 'profit' : 'loss'">
            {{ formatSigned(mockNetCashFlow) }}
          </span>
        </div>
      </div>

    </template><!-- end v-else -->

  </div>
</template>

<style scoped>
.campaign-detail {
  min-height: 100%;
}

/* ── Header ── */
.header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 28px 16px;
  border-bottom: 1px solid var(--outline-variant);
}

.header-left {
  flex: 1;
}

/* ── Breadcrumb ── */
.breadcrumb {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  margin-bottom: 8px;
}

.breadcrumb-link {
  color: var(--on-surface-variant);
  text-decoration: none;
}

.breadcrumb-link:hover {
  color: var(--on-surface);
}

.breadcrumb-sep {
  margin: 0 4px;
}

.breadcrumb-ticker {
  color: var(--on-surface);
}

/* ── Title row ── */
.title-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 6px;
}

.ticker {
  font-family: var(--font-mono);
  font-size: 30px;
  font-weight: 700;
  line-height: 36px;
  letter-spacing: -0.02em;
  color: var(--on-surface);
}

.campaign-label {
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  color: var(--on-surface-variant);
}

/* ── Status badge ── */
.badge {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  line-height: 16px;
  padding: 2px 8px;
  border-radius: 0;
  border: 1px solid;
}

.badge-open {
  background: rgba(74, 225, 118, 0.12);
  color: var(--secondary);
  border-color: rgba(74, 225, 118, 0.3);
}

.badge-closed {
  background: rgba(66, 71, 84, 0.25);
  color: var(--outline);
  border-color: var(--outline-variant);
}

/* ── Notes ── */
.notes {
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  color: var(--on-surface-variant);
}

/* ── Stat strip ── */
.stat-strip {
  display: flex;
  gap: 24px;
  flex-shrink: 0;
  align-items: flex-start;
}

.stat {
  text-align: right;
}

.stat-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  margin-bottom: 3px;
  white-space: nowrap;
}

.stat-value {
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 700;
  line-height: 24px;
  color: var(--on-surface);
  white-space: nowrap;
}

/* ── P&L colors ── */
.profit { color: var(--color-profit); }
.loss   { color: var(--color-loss); }

/* ── State messages ── */
.state-msg {
  padding: 40px 28px;
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--on-surface-variant);
}

.state-msg--error {
  color: var(--color-loss);
}

/* ── Sections ── */
.section {
  padding: 16px 28px 0;
}

.history-section {
  padding-bottom: 40px;
}

.section-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--on-surface-variant);
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.dot {
  color: var(--primary);
}

.count {
  color: var(--outline);
  font-weight: 400;
}

/* ── Tables ── */
.table {
  width: 100%;
  border-collapse: collapse;
}

.thead-row {
  background: var(--surface-container-low);
}

.th {
  padding: 0 10px;
  height: 28px;
  text-align: left;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--on-surface-variant);
  white-space: nowrap;
  vertical-align: middle;
}

/* ── Net Cash Flow footer ── */
.ncf-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 24px;
  padding: 8px 10px;
  border-top: 1px solid var(--outline-variant);
}

.ncf-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--on-surface-variant);
}

.ncf-value {
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 700;
}
</style>
