<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { Campaign } from '@/types/index'
import { MOCK_PRICES } from '@/types/index'
import CampaignRow from '@/components/dashboard/CampaignRow.vue'

const router = useRouter()

const mockCampaigns: Campaign[] = [
  {
    id: 1,
    ticker: 'NVDA',
    label: 'Wheel Strategy',
    status: 'OPEN',
    openedAt: '2024-01-15',
    netCashFlow: -246300,
    costBasis: 820.83,
    sharesHeld: 300,
    openPositionCount: 1,
    realizedPnl: 6900,
  },
  {
    id: 2,
    ticker: 'SPY',
    label: 'Calendar Spreads',
    status: 'OPEN',
    openedAt: '2024-02-10',
    netCashFlow: -265,
    openPositionCount: 2,
    realizedPnl: 2200,
  },
  {
    id: 3,
    ticker: 'TSLA',
    label: 'Short Puts',
    status: 'OPEN',
    openedAt: '2024-03-05',
    netCashFlow: 1400,
    openPositionCount: 1,
    realizedPnl: 420,
  },
  {
    id: 4,
    ticker: 'AAPL',
    label: 'Wheel Exit',
    status: 'CLOSED',
    openedAt: '2023-10-12',
    closedAt: '2024-01-10',
    netCashFlow: 850,
    openPositionCount: 0,
    realizedPnl: 850,
  },
  {
    id: 5,
    ticker: 'AMD',
    label: 'Covered Calls',
    status: 'CLOSED',
    openedAt: '2023-08-20',
    closedAt: '2024-02-15',
    netCashFlow: 2800,
    costBasis: 122.10,
    sharesHeld: 200,
    openPositionCount: 0,
    realizedPnl: 1200,
  },
]

const mockKpis = { totalUnrealized: 16250, totalRealized: 11530, premiumCollected: 11955 }
const mockTotalTrades = 24

const activeCampaigns = computed(() => mockCampaigns.filter(c => c.status === 'OPEN'))
const closedCampaigns = computed(() => mockCampaigns.filter(c => c.status === 'CLOSED'))

function unrealizedFor(c: Campaign): number {
  if (!c.sharesHeld || c.costBasis == null) return 0
  const price = MOCK_PRICES[c.ticker] ?? 0
  return Math.round(c.sharesHeld * (price - c.costBasis))
}

function formatKpi(value: number): string {
  const sign = value >= 0 ? '+' : '-'
  return `${sign}$${Math.abs(value).toLocaleString()}`
}

function onSelect(id: number) {
  router.push(`/campaign/${id}`)
}

const TABLE_COLS = [
  'TICKER', 'LABEL', 'COST BASIS', 'SHARES', 'OPEN POS',
  'NET CASH', 'UNRLZ P&L', 'RLZ P&L', 'STATUS', 'STARTED',
]

const kpiStats = [
  { label: 'TOTAL UNREALIZED', value: mockKpis.totalUnrealized },
  { label: 'TOTAL REALIZED', value: mockKpis.totalRealized },
  { label: 'PREMIUM COLLECTED', value: mockKpis.premiumCollected },
]

const footerStats = computed(() => [
  { label: 'ACTIVE CAMPAIGNS', value: activeCampaigns.value.length },
  { label: 'TOTAL CAMPAIGNS', value: mockCampaigns.length },
  { label: 'TOTAL TRADES', value: mockTotalTrades },
])
</script>

<template>
  <div class="dashboard">

    <!-- Header -->
    <div class="header">
      <div class="header-left">
        <div class="screen-label">OVERVIEW</div>
        <div class="screen-title">Dashboard</div>
      </div>
      <div class="kpi-row">
        <div v-for="stat in kpiStats" :key="stat.label" class="kpi">
          <div class="kpi-label">{{ stat.label }}</div>
          <div class="kpi-value" :class="stat.value >= 0 ? 'profit' : 'loss'">
            {{ formatKpi(stat.value) }}
          </div>
        </div>
      </div>
    </div>

    <!-- Active campaigns -->
    <div class="section-head">
      <span class="dot dot-active">●</span>
      <span class="section-label">ACTIVE CAMPAIGNS</span>
      <span class="section-count">({{ activeCampaigns.length }})</span>
    </div>
    <div class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th v-for="col in TABLE_COLS" :key="col" class="th">{{ col }}</th>
          </tr>
        </thead>
        <tbody>
          <CampaignRow
            v-for="c in activeCampaigns"
            :key="c.id"
            :campaign="c"
            :unrealized-pnl="unrealizedFor(c)"
            @select="onSelect"
          />
        </tbody>
      </table>
    </div>

    <!-- Closed campaigns -->
    <div class="section-head section-head--gap">
      <span class="dot dot-closed">●</span>
      <span class="section-label">CLOSED CAMPAIGNS</span>
      <span class="section-count">({{ closedCampaigns.length }})</span>
    </div>
    <div class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th v-for="col in TABLE_COLS" :key="col" class="th">{{ col }}</th>
          </tr>
        </thead>
        <tbody>
          <CampaignRow
            v-for="c in closedCampaigns"
            :key="c.id"
            :campaign="c"
            :unrealized-pnl="unrealizedFor(c)"
            @select="onSelect"
          />
        </tbody>
      </table>
    </div>

    <!-- Footer -->
    <div class="footer">
      <div v-for="stat in footerStats" :key="stat.label" class="footer-stat">
        <div class="footer-label">{{ stat.label }}</div>
        <div class="footer-value">{{ stat.value }}</div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.dashboard {
  min-height: 100%;
}

/* ── Header ── */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 28px 16px;
  border-bottom: 1px solid var(--outline-variant);
}

.screen-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  margin-bottom: 2px;
}

.screen-title {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--on-surface);
  line-height: 28px;
}

.kpi-row {
  display: flex;
  gap: 24px;
}

.kpi {
  text-align: right;
}

.kpi-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  margin-bottom: 3px;
}

.kpi-value {
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 700;
  line-height: 24px;
}

/* ── Section header ── */
.section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 28px 10px;
}

.section-head--gap {
  padding-top: 24px;
}

.dot {
  font-size: 8px;
  line-height: 1;
}

.dot-active { color: var(--secondary); }
.dot-closed { color: var(--outline); }

.section-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
}

.section-count {
  font-size: 11px;
  color: var(--outline-variant);
}

/* ── Table ── */
.table-wrap {
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.th {
  padding: 6px 10px;
  text-align: left;
  background: var(--surface-container-low);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  white-space: nowrap;
  border-bottom: 1px solid var(--outline-variant);
}

/* ── Footer ── */
.footer {
  display: flex;
  gap: 32px;
  margin: 28px 28px 28px;
  padding: 14px 0;
  border-top: 1px solid var(--outline-variant);
}

.footer-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
}

.footer-value {
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 700;
  color: var(--on-surface-variant);
  margin-top: 2px;
}

/* ── P&L colors ── */
.profit { color: var(--color-profit); }
.loss   { color: var(--color-loss); }
</style>
