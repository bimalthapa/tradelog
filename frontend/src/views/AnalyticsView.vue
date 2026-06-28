<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useTradeLogStore } from '@/stores/tradeLog'
import BarChart  from '@/components/analytics/BarChart.vue'
import LineChart from '@/components/analytics/LineChart.vue'

const store = useTradeLogStore()

onMounted(() => Promise.all([store.fetchCampaigns(), store.fetchAnalytics()]))

watch(() => store.selectedAccountId, () => {
  store.fetchAnalytics()
})

const summary = computed(() => store.analytics ?? {
  totalPremium: 0, netOptionsPnl: 0,
  campaignWinRate: 0, tradeWinRate: 0, totalTrades: 0,
})

const pnlByCampaign = computed(() =>
  [...store.campaigns]
    .map(c => ({
      label: c.label ? `${c.ticker}·${c.label}` : `${c.ticker}#${c.id}`,
      value: c.netCashFlow,
    }))
    .sort((a, b) => b.value - a.value)
)

const pnlByStrategy  = computed(() => store.pnlByStrategy)
const cumulativeData = computed(() => store.cumulativeData ?? { premium: [], optionsPnl: [] })

function fmtMoney(v: number): string {
  const abs = Math.abs(v).toLocaleString('en-US', { maximumFractionDigits: 0 })
  return (v >= 0 ? '+$' : '-$') + abs
}

function fmtPct(v: number): string {
  return Math.round(v * 100) + '%'
}
</script>

<template>
  <div class="analytics">

    <!-- Header -->
    <div class="header">
      <div class="screen-label">PERFORMANCE</div>
      <div class="screen-title">Analytics</div>
    </div>

    <template v-if="store.loading">
      <div class="state-msg">Loading...</div>
    </template>
    <template v-else-if="store.error">
      <div class="state-msg state-msg--error">{{ store.error }}</div>
    </template>
    <template v-else>
      <!-- KPI row -->
      <div class="kpi-row">
        <div class="kpi-card">
          <div class="kpi-label">TOTAL PREMIUM</div>
          <div class="kpi-value profit">{{ fmtMoney(summary.totalPremium) }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">NET OPTIONS P&amp;L</div>
          <div class="kpi-value" :class="summary.netOptionsPnl >= 0 ? 'profit' : 'loss'">
            {{ fmtMoney(summary.netOptionsPnl) }}
          </div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">CAMPAIGN WIN RATE</div>
          <div class="kpi-value">{{ fmtPct(summary.campaignWinRate) }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">TRADE WIN RATE</div>
          <div class="kpi-value">{{ fmtPct(summary.tradeWinRate) }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">TOTAL TRADES</div>
          <div class="kpi-value">{{ summary.totalTrades }}</div>
        </div>
      </div>

      <!-- Chart grid -->
      <div class="chart-grid">
        <div class="chart-cell">
          <div class="chart-title">P&amp;L BY CAMPAIGN</div>
          <BarChart :items="pnlByCampaign" />
        </div>
        <div class="chart-cell">
          <div class="chart-title">CUMULATIVE PREMIUM</div>
          <LineChart :points="cumulativeData.premium" />
        </div>
        <div class="chart-cell">
          <div class="chart-title">P&amp;L BY STRATEGY</div>
          <BarChart :items="pnlByStrategy" />
        </div>
        <div class="chart-cell">
          <div class="chart-title">CUMULATIVE OPTIONS P&amp;L</div>
          <LineChart :points="cumulativeData.optionsPnl" />
        </div>
      </div>
    </template>

  </div>
</template>

<style scoped>
.analytics { min-height: 100%; }

.header {
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
  gap: 0;
  border-bottom: 1px solid var(--outline-variant);
}

.kpi-card {
  flex: 1;
  padding: 16px 20px;
  border-right: 1px solid var(--outline-variant);
}
.kpi-card:last-child { border-right: none; }

.kpi-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  margin-bottom: 6px;
}

.kpi-value {
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 700;
  line-height: 24px;
  color: var(--on-surface);
}

.profit { color: var(--color-profit); }
.loss   { color: var(--color-loss); }

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  background: var(--outline-variant);
  margin: 20px 28px;
  border: 1px solid var(--outline-variant);
}

.chart-cell {
  background: var(--surface-container-low);
  padding: 16px 20px;
}

.chart-title {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  margin-bottom: 12px;
}

.state-msg { padding: 40px 28px; font-family: var(--font-mono); font-size: 13px; color: var(--on-surface-variant); }
.state-msg--error { color: var(--color-loss); }
</style>
