<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { Campaign } from '@/types/index'
import { useTradeLogStore } from '@/stores/tradeLog'
import { usePriceStore } from '@/stores/priceStore'
import CampaignRow from '@/components/dashboard/CampaignRow.vue'

const router     = useRouter()
const store      = useTradeLogStore()
const priceStore = usePriceStore()

onMounted(() => {
  store.campaigns.forEach(c => priceStore.ensureTicker(c.ticker))
})

watch(() => store.campaigns, (campaigns) => {
  campaigns.forEach(c => priceStore.ensureTicker(c.ticker))
})

function unrealizedFor(c: Campaign): number {
  if (!c.sharesHeld || c.costBasis == null) return 0
  const price = priceStore.getPrice(c.ticker)
  if (price == null) return 0
  return Math.round(c.sharesHeld * (price - c.costBasis))
}

function filterByAccount(items: Campaign[]): Campaign[] {
  const sel = store.selectedAccountId
  if (sel === 'all') return items
  if (sel === null) return items.filter(c => !c.accountId)
  return items.filter(c => c.accountId === sel)
}

const allFiltered    = computed(() => filterByAccount(store.campaigns))
const filteredActive = computed(() => filterByAccount(store.activeCampaigns))
const filteredClosed = computed(() => filterByAccount(store.closedCampaigns))

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

const totalUnrealized = computed(() =>
  allFiltered.value.reduce((sum, c) => sum + unrealizedFor(c), 0)
)
const totalRealized = computed(() =>
  allFiltered.value.reduce((sum, c) => sum + (c.realizedPnl ?? 0), 0)
)
const totalNetCashFlow = computed(() =>
  allFiltered.value.reduce((sum, c) => sum + c.netCashFlow, 0)
)

const kpiStats = computed(() => [
  { label: 'TOTAL UNREALIZED', value: totalUnrealized.value },
  { label: 'TOTAL REALIZED',   value: totalRealized.value },
  { label: 'NET CASH FLOW',    value: totalNetCashFlow.value },
])

const footerStats = computed(() => [
  { label: 'ACTIVE CAMPAIGNS', value: filteredActive.value.length },
  { label: 'TOTAL CAMPAIGNS',  value: allFiltered.value.length },
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

    <!-- Loading / error -->
    <template v-if="store.loading">
      <div class="state-msg">Loading...</div>
    </template>
    <template v-else-if="store.error">
      <div class="state-msg state-msg--error">{{ store.error }}</div>
    </template>
    <template v-else>

      <!-- Active campaigns -->
      <div class="section-head">
        <span class="dot dot-active">●</span>
        <span class="section-label">ACTIVE CAMPAIGNS</span>
        <span class="section-count">({{ filteredActive.length }})</span>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th v-for="col in TABLE_COLS" :key="col" class="th">{{ col }}</th>
            </tr>
          </thead>
          <tbody>
            <template v-if="filteredActive.length > 0">
              <CampaignRow
                v-for="c in filteredActive"
                :key="c.id"
                :campaign="c"
                :unrealized-pnl="unrealizedFor(c)"
                @select="onSelect"
              />
            </template>
            <template v-else>
              <tr>
                <td :colspan="TABLE_COLS.length" class="empty-cell">No active campaigns</td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <!-- Closed campaigns -->
      <div class="section-head section-head--gap">
        <span class="dot dot-closed">●</span>
        <span class="section-label">CLOSED CAMPAIGNS</span>
        <span class="section-count">({{ filteredClosed.length }})</span>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th v-for="col in TABLE_COLS" :key="col" class="th">{{ col }}</th>
            </tr>
          </thead>
          <tbody>
            <template v-if="filteredClosed.length > 0">
              <CampaignRow
                v-for="c in filteredClosed"
                :key="c.id"
                :campaign="c"
                :unrealized-pnl="unrealizedFor(c)"
                @select="onSelect"
              />
            </template>
            <template v-else>
              <tr>
                <td :colspan="TABLE_COLS.length" class="empty-cell">No closed campaigns</td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

    </template>

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

/* ── Loading / error ── */
.state-msg {
  padding: 40px 28px;
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--on-surface-variant);
}

.state-msg--error {
  color: var(--color-loss);
}

.empty-cell {
  padding: 12px 10px;
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--on-surface-variant);
  text-align: center;
}
</style>
