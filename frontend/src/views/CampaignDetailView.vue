<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import TradeEntryBar from '@/components/campaign/TradeEntryBar.vue'
import ConfirmPanel from '@/components/campaign/ConfirmPanel.vue'
import PositionRow from '@/components/campaign/PositionRow.vue'
import TradeRow from '@/components/campaign/TradeRow.vue'
import { useTradeLogStore } from '@/stores/tradeLog'
import { usePriceStore } from '@/stores/priceStore'
import { saveTrade } from '@/services/tradeService'
import { closeCampaign } from '@/services/campaignService'
import type { ParsedTrade, Position } from '@/types/index'

const route      = useRoute()
const store      = useTradeLogStore()
const priceStore = usePriceStore()

const loading         = ref(true)
const saving          = ref(false)
const parsedTrade     = ref<ParsedTrade | null>(null)
const closingPosition = ref<Position | null>(null)
const pendingRawInput = ref('')
const panelError      = ref('')
const entryBarRef     = ref<InstanceType<typeof TradeEntryBar> | null>(null)

const campaignId = computed(() => Number(route.params.id))

onMounted(async () => {
  const id = campaignId.value
  await Promise.all([
    store.fetchCampaign(id),
    store.fetchTrades(id),
    store.fetchPositions(id),
  ])
  if (campaign.value) priceStore.ensureTicker(campaign.value.ticker)
  loading.value = false
})

watch(campaignId, async (id) => {
  loading.value = true
  await Promise.all([
    store.fetchCampaign(id),
    store.fetchTrades(id),
    store.fetchPositions(id),
  ])
  if (campaign.value) priceStore.ensureTicker(campaign.value.ticker)
  loading.value = false
})

const campaign = computed(() => store.currentCampaign)

const currentPrice = computed(() =>
  campaign.value ? priceStore.getPrice(campaign.value.ticker) : null
)

const unrealizedPnl = computed(() => {
  const c = campaign.value
  if (!c || !c.sharesHeld || c.costBasis == null) return 0
  const price = currentPrice.value
  if (price == null) return 0
  return c.sharesHeld * (price - c.costBasis)
})

const netCashFlow = computed(() =>
  store.trades.reduce((sum, t) => sum + t.netCashFlow, 0)
)

function formatCurrency(value: number | null | undefined): string {
  if (value == null) return '—'
  return value.toLocaleString('en-US', { style: 'currency', currency: 'USD' })
}

function formatSigned(value: number): string {
  return (value >= 0 ? '+' : '') + formatCurrency(value)
}

function onParsed({ trade, rawInput }: { trade: ParsedTrade; rawInput: string }) {
  parsedTrade.value = trade
  pendingRawInput.value = rawInput
  panelError.value = ''
}

async function onSave({ strategyTag, notes, tradeDate, qty, exitPrice }: {
  strategyTag: string; notes: string; tradeDate: string; qty?: number; exitPrice?: number
}) {
  if (saving.value) return
  saving.value = true
  panelError.value = ''
  const isClose = closingPosition.value != null
  try {
    let rawInput: string
    if (isClose && closingPosition.value && qty != null && exitPrice != null) {
      rawInput = buildCloseRawInput(closingPosition.value, qty, exitPrice)
    } else {
      rawInput = pendingRawInput.value
    }
    await saveTrade({ campaignId: campaignId.value, rawInput, strategyTag, notes, tradedAt: tradeDate })
    parsedTrade.value     = null
    closingPosition.value = null
    if (!isClose) {
      entryBarRef.value?.triggerFlash()
      entryBarRef.value?.clearInput()
    }
    const id = campaignId.value
    await Promise.all([store.fetchCampaign(id), store.fetchTrades(id), store.fetchPositions(id)])
  } catch (e) {
    panelError.value = e instanceof Error ? e.message : 'Failed to save trade'
  } finally {
    saving.value = false
  }
}

function onCancel() {
  const wasClose        = closingPosition.value != null
  parsedTrade.value     = null
  closingPosition.value = null
  panelError.value      = ''
  if (!wasClose) entryBarRef.value?.clearInput()
}

function buildCloseRawInput(position: Position, qty: number, exitPrice: number): string {
  const action = position.openAction === 'STO' ? 'BTC' : 'STC'
  if (position.instrumentType === 'OPTION') {
    const optChar = position.optionType === 'CALL' ? 'C' : 'P'
    const [, month, day] = position.expiry!.split('-')
    const expiry = `${parseInt(month!)}/${day!}`
    return `${action} ${qty} ${position.ticker} ${position.strike}${optChar} ${expiry} @${exitPrice}`
  }
  return `STC ${qty} ${position.ticker} @${exitPrice}`
}

function onClosePosition(position: Position) {
  closingPosition.value = position
  panelError.value = ''
}

async function handleCloseCampaign() {
  if (!campaign.value) return
  try {
    await closeCampaign(campaign.value.id)
    await store.fetchCampaign(campaign.value.id)
  } catch {
    // badge stays unchanged; user can retry
  }
}

const editingAccount   = ref(false)
const pendingAccountId = ref<number | undefined>(undefined)

function startAccountEdit() {
  pendingAccountId.value = store.currentCampaign?.accountId
  editingAccount.value = true
}

async function saveAccount() {
  if (!store.currentCampaign) return
  try {
    await store.assignAccount(
      store.currentCampaign.id,
      pendingAccountId.value ?? null
    )
  } finally {
    editingAccount.value = false
  }
}
</script>

<template>
  <div class="campaign-detail">

    <template v-if="loading">
      <div class="state-msg">Loading…</div>
    </template>

    <template v-else-if="!campaign">
      <div class="state-msg state-msg--error">Campaign not found</div>
    </template>

    <template v-else>
      <div v-if="store.error" class="fetch-error">{{ store.error }}</div>

      <div class="header">

        <div class="header-left">
          <div class="breadcrumb">
            <RouterLink to="/dashboard" class="breadcrumb-link">Dashboard</RouterLink>
            <span class="breadcrumb-sep"> › </span>
            <span class="breadcrumb-ticker">{{ campaign.ticker }}</span>
          </div>

          <div class="title-row">
            <span class="ticker">{{ campaign.ticker }}</span>
            <span v-if="campaign.label" class="campaign-label">{{ campaign.label }}</span>
            <span class="badge" :class="campaign.status === 'OPEN' ? 'badge-open' : 'badge-closed'">
              {{ campaign.status === 'OPEN' ? 'ACTIVE' : 'CLOSED' }}
            </span>
            <button
              v-if="campaign.status === 'OPEN'"
              class="btn-close-campaign"
              @click="handleCloseCampaign"
            >
              Close Campaign
            </button>
          </div>

          <div v-if="campaign.notes" class="notes">{{ campaign.notes }}</div>
        </div>

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
            <div class="stat-value" :class="campaign.netCashFlow >= 0 ? 'profit' : 'loss'">
              {{ formatSigned(campaign.netCashFlow) }}
            </div>
          </div>
          <div class="stat">
            <div class="stat-label">UNRLZ P&amp;L</div>
            <div class="stat-value" :class="unrealizedPnl >= 0 ? 'profit' : 'loss'">
              {{ formatSigned(unrealizedPnl) }}
            </div>
          </div>
          <div class="stat">
            <div class="stat-label">CURR PRICE</div>
            <div class="stat-value">{{ priceStore.loading ? '…' : formatCurrency(currentPrice) }}</div>
          </div>
          <div class="stat">
            <div class="stat-label">ACCOUNT</div>
            <template v-if="editingAccount">
              <select
                v-model="pendingAccountId"
                class="account-select"
                @change="saveAccount"
              >
                <option :value="undefined">Unassigned</option>
                <option
                  v-for="account in store.accounts"
                  :key="account.id"
                  :value="account.id"
                >{{ account.name }}</option>
              </select>
            </template>
            <template v-else>
              <button class="account-value" @click="startAccountEdit">
                {{ campaign.accountName ?? 'Unassigned' }}
                <span class="edit-icon">✎</span>
              </button>
            </template>
          </div>
        </div>

      </div>

      <TradeEntryBar ref="entryBarRef" @parsed="onParsed" />

      <div v-if="store.openPositions.length > 0" class="section positions-section">
        <div class="section-label">
          <span class="dot">●</span>
          OPEN POSITIONS
          <span class="count">({{ store.openPositions.length }})</span>
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
              <th class="th"></th>
            </tr>
          </thead>
          <tbody>
            <PositionRow v-for="pos in store.openPositions" :key="pos.id" :position="pos" @close="onClosePosition" />
          </tbody>
        </table>
      </div>

      <div class="section history-section">
        <div class="section-label">
          TRADE HISTORY
          <span class="count">({{ store.trades.length }})</span>
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
            <template v-if="store.trades.length > 0">
              <TradeRow v-for="trade in store.trades" :key="trade.id" :trade="trade" />
            </template>
            <template v-else>
              <tr>
                <td :colspan="8" class="empty-cell">No trades recorded yet</td>
              </tr>
            </template>
          </tbody>
        </table>
        <div class="ncf-footer">
          <span class="ncf-label">NET CASH FLOW</span>
          <span class="ncf-value" :class="netCashFlow >= 0 ? 'profit' : 'loss'">
            {{ formatSigned(netCashFlow) }}
          </span>
        </div>
      </div>

      <ConfirmPanel
        v-if="parsedTrade"
        mode="parse"
        :trade="parsedTrade"
        :save-error="panelError"
        :saving="saving"
        @save="onSave"
        @cancel="onCancel"
      />
      <ConfirmPanel
        v-else-if="closingPosition"
        mode="close"
        :position="closingPosition"
        :save-error="panelError"
        :saving="saving"
        @save="onSave"
        @cancel="onCancel"
      />

    </template>
  </div>
</template>

<style scoped>
.campaign-detail { min-height: 100%; }

.header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 28px 16px;
  border-bottom: 1px solid var(--outline-variant);
}

.header-left { flex: 1; }

.breadcrumb {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  margin-bottom: 8px;
}

.breadcrumb-link { color: var(--on-surface-variant); text-decoration: none; }
.breadcrumb-link:hover { color: var(--on-surface); }
.breadcrumb-sep { margin: 0 4px; }
.breadcrumb-ticker { color: var(--on-surface); }

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

.btn-close-campaign {
  background: transparent;
  border: 1px solid var(--outline-variant);
  border-radius: 0;
  color: var(--color-loss);
  font-family: var(--font-ui);
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  cursor: pointer;
  align-self: center;
}

.btn-close-campaign:hover { border-color: var(--color-loss); }

.notes {
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  color: var(--on-surface-variant);
}

.stat-strip {
  display: flex;
  gap: 24px;
  flex-shrink: 0;
  align-items: flex-start;
}

.stat { text-align: right; }

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

.profit { color: var(--color-profit); }
.loss   { color: var(--color-loss); }

.state-msg {
  padding: 40px 28px;
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--on-surface-variant);
}

.state-msg--error { color: var(--color-loss); }

.fetch-error {
  padding: 8px 28px;
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--color-loss);
}

.section { padding: 16px 28px 0; }
.history-section { padding-bottom: 40px; }

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

.dot   { color: var(--primary); }
.count { color: var(--outline); font-weight: 400; }

.table { width: 100%; border-collapse: collapse; }

.thead-row { background: var(--surface-container-low); }

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

.empty-cell {
  padding: 12px 10px;
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--on-surface-variant);
  text-align: center;
}

.account-value {
  background: none;
  border: none;
  color: var(--on-surface);
  font-family: var(--font-mono);
  font-size: 13px;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}
.edit-icon {
  color: var(--outline);
  font-size: 11px;
  opacity: 0;
}
.account-value:hover .edit-icon { opacity: 1; }
.account-select {
  background: var(--surface-container-low);
  border: none;
  border-bottom: 1px solid var(--primary);
  color: var(--on-surface);
  font-family: var(--font-mono);
  font-size: 13px;
  outline: none;
  cursor: pointer;
  border-radius: 0;
}
</style>
