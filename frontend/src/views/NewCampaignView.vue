<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createCampaign } from '@/services/campaignService'
import { useTradeLogStore } from '@/stores/tradeLog'

const router = useRouter()
const store = useTradeLogStore()

const ticker      = ref('')
const label       = ref('')
const startDate   = ref(new Date().toISOString().split('T')[0] ?? '')
const notes       = ref('')
const tickerError = ref('')
const apiError    = ref('')
const submitting  = ref(false)
const selectedAccountId = ref<number | undefined>(
  typeof store.selectedAccountId === 'number' ? store.selectedAccountId : undefined
)

function onTickerInput(e: Event) {
  ticker.value = (e.target as HTMLInputElement).value.toUpperCase().replace(/[^A-Z]/g, '')
  tickerError.value = ''
}

function onTickerKeypress(e: KeyboardEvent) {
  if (e.key.length === 1 && !/[a-zA-Z]/.test(e.key)) e.preventDefault()
}

async function onSubmit() {
  if (!ticker.value) {
    tickerError.value = 'Ticker is required.'
    return
  }
  submitting.value = true
  apiError.value = ''
  try {
    const campaign = await createCampaign({
      ticker: ticker.value,
      label: label.value.trim() || undefined,
      notes: notes.value.trim() || undefined,
      openedAt: startDate.value,
      accountId: selectedAccountId.value,
    })
    router.push(`/campaign/${campaign.id}`)
  } catch (e) {
    apiError.value = e instanceof Error ? e.message : 'Request failed'
  } finally {
    submitting.value = false
  }
}

function onCancel() {
  router.push('/dashboard')
}

const SYNTAX_EXAMPLES = [
  { trade: 'STO 3 NVDA 920C 8/16 @6.10',  desc: 'Sell to open 3 NVDA 920 calls' },
  { trade: 'BTO 5 SPY 480P 12/20 @2.35',  desc: 'Buy to open 5 SPY 480 puts' },
  { trade: 'BTC 2 AAPL 175C 11/17 @0.05', desc: 'Buy to close 2 AAPL calls' },
  { trade: 'BTO 100 NVDA @875.40',         desc: 'Buy 100 shares at market' },
]
</script>

<template>
  <div class="new-campaign">

    <!-- Header -->
    <div class="header">
      <div class="breadcrumb">
        <RouterLink to="/dashboard" class="breadcrumb-link">DASHBOARD</RouterLink>
        <span class="breadcrumb-sep"> › </span>
        <span>NEW CAMPAIGN</span>
      </div>
      <div class="screen-title">New Campaign</div>
    </div>

    <!-- Form -->
    <div class="form-wrap">
      <form @submit.prevent="onSubmit">

        <!-- Ticker -->
        <div class="field">
          <label class="field-label">TICKER <span class="required">*</span></label>
          <input
            class="input ticker-input"
            :value="ticker"
            @input="onTickerInput"
            @keypress="onTickerKeypress"
            placeholder="NVDA"
            maxlength="5"
            autocomplete="off"
            autofocus
          />
          <div v-if="tickerError" class="ticker-error">{{ tickerError }}</div>
        </div>

        <!-- Label -->
        <div class="field">
          <label class="field-label">LABEL <span class="optional">(optional)</span></label>
          <input
            class="input input-full"
            v-model="label"
            placeholder="e.g. Wheel Strategy, Short Puts"
            autocomplete="off"
          />
        </div>

        <!-- Account -->
        <div class="field">
          <label class="field-label">ACCOUNT <span class="optional">(optional)</span></label>
          <select v-model="selectedAccountId" class="input form-select">
            <option :value="undefined">Unassigned</option>
            <option v-for="account in store.accounts" :key="account.id" :value="account.id">
              {{ account.name }}
            </option>
          </select>
        </div>

        <!-- Start Date -->
        <div class="field">
          <label class="field-label">START DATE</label>
          <input
            class="input input-date"
            type="date"
            v-model="startDate"
          />
        </div>

        <!-- Notes -->
        <div class="field">
          <label class="field-label">NOTES <span class="optional">(optional)</span></label>
          <textarea
            class="input input-full textarea"
            v-model="notes"
            rows="3"
            placeholder="Strategy notes, targets, thesis…"
          />
        </div>

        <!-- API error -->
        <div v-if="apiError" class="api-error">{{ apiError }}</div>

        <!-- Buttons -->
        <div class="btn-row">
          <button type="button" class="btn btn-ghost cancel-btn" @click="onCancel">Cancel</button>
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            {{ submitting ? 'Creating…' : 'Create Campaign →' }}
          </button>
        </div>

      </form>

      <!-- Syntax reference card -->
      <div class="syntax-card">
        <div class="syntax-header">QUICK ENTRY SYNTAX</div>
        <div v-for="ex in SYNTAX_EXAMPLES" :key="ex.trade" class="syntax-row">
          <span class="syntax-trade">{{ ex.trade }}</span>
          <span class="syntax-desc">{{ ex.desc }}</span>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.new-campaign {
  min-height: 100%;
}

/* ── Header ── */
.header {
  padding: 20px 28px 16px;
  border-bottom: 1px solid var(--outline-variant);
}

.breadcrumb {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  margin-bottom: 4px;
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

.screen-title {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--on-surface);
  line-height: 28px;
}

/* ── Form ── */
.form-wrap {
  padding: 28px;
  max-width: 480px;
}

.field {
  margin-bottom: 18px;
}

.field-label {
  display: block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  margin-bottom: 6px;
}

.required {
  color: var(--color-loss);
}

.optional {
  font-weight: 400;
  letter-spacing: 0;
  text-transform: none;
  color: var(--outline);
}

.input {
  background: var(--surface-container-lowest);
  border: 1px solid var(--outline-variant);
  color: var(--on-surface);
  padding: 8px 10px;
  font-size: 14px;
  font-family: var(--font-ui);
  border-radius: 0;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
}

.input:focus {
  border-color: var(--primary);
}

.ticker-input {
  width: 120px;
  font-family: var(--font-mono);
  font-size: 16px;
  font-weight: 700;
}

.input-full {
  width: 100%;
}

.input-date {
  width: 180px;
  color-scheme: dark;
}

.textarea {
  resize: vertical;
}

.form-select {
  appearance: none;
  -webkit-appearance: none;
  cursor: pointer;
  width: 200px;
}

/* ── Errors ── */
.ticker-error,
.api-error {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--color-loss);
  margin-top: 6px;
}

.api-error {
  margin-bottom: 14px;
}

/* ── Buttons ── */
.btn-row {
  display: flex;
  gap: 10px;
}

.btn {
  padding: 8px 20px;
  font-size: 13px;
  font-family: var(--font-ui);
  font-weight: 700;
  border-radius: 0;
  cursor: pointer;
  border: none;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: var(--primary-container);
  color: var(--on-primary-container);
}

.btn-ghost {
  background: transparent;
  border: 1px solid var(--outline-variant);
  color: var(--on-surface-variant);
  font-weight: 400;
}

/* ── Syntax card ── */
.syntax-card {
  margin-top: 32px;
  padding: 14px;
  background: var(--surface-container-lowest);
  border: 1px solid var(--outline-variant);
}

.syntax-header {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  margin-bottom: 8px;
}

.syntax-row {
  display: flex;
  gap: 12px;
  margin-bottom: 5px;
}

.syntax-row:last-child {
  margin-bottom: 0;
}

.syntax-trade {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--primary);
  white-space: nowrap;
}

.syntax-desc {
  font-size: 11px;
  color: var(--on-surface-variant);
}
</style>
