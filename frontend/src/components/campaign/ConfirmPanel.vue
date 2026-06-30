<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { ParsedTrade, Position, TradeLeg } from '@/types/index'
import { detectMultiLegStrategy } from '@/composables/useTradeParser'

const props = defineProps<{
  mode: 'parse' | 'close' | 'edit' | 'roll'
  trade?: ParsedTrade
  trades?: ParsedTrade[]
  position?: Position
  tradeLeg?: TradeLeg
  rollingPosition?: Position
  saveError?: string
  saving?: boolean
}>()

const emit = defineEmits<{
  save: [{ strategyTag: string; notes: string; tradeDate: string; qty?: number; exitPrice?: number }]
  roll: [{ qty: number; btcPrice: number; newStrike: number; newExpiry: string; stoPrice: number; tradeDate: string; notes: string }]
  cancel: []
}>()

const strategyTag = ref(props.trade?.strategy ?? '')
const notes       = ref('')
const tradeDate   = ref(todayDisplayDate())

// Close mode state (used in Task 3)
const closeQty    = ref(props.position?.openQuantity ?? 0)
const exitPrice   = ref('')

watch(() => props.trade, (t) => {
  if (t) {
    strategyTag.value = t.strategy
    notes.value       = ''
    tradeDate.value   = todayDisplayDate()
  }
})

watch(() => props.trades, (legs) => {
  if (legs && legs.length > 0) {
    strategyTag.value = detectMultiLegStrategy(legs)
    notes.value       = ''
    tradeDate.value   = todayDisplayDate()
  }
})

const netCashFlow = computed(() =>
  (props.trades ?? []).reduce((sum, l) => sum + l.cashFlow, 0)
)

function optionLabel(leg: ParsedTrade): string {
  if (leg.instrumentType !== 'OPTION') return '—'
  return `$${leg.strike}${leg.optionType === 'CALL' ? 'C' : 'P'}`
}

watch(() => props.position, (p) => {
  if (p) {
    closeQty.value  = p.openQuantity
    exitPrice.value = ''
    notes.value     = ''
    tradeDate.value = todayDisplayDate()
  }
})

// Edit mode state
const editQty       = ref(0)
const editPrice     = ref('')
const editTradeDate = ref('')
const editStrategy  = ref('')
const editNotes     = ref('')

// Roll mode state
const rollBtcPrice  = ref('')
const rollQty       = ref(0)
const rollNewStrike = ref('')
const rollNewExpiry = ref('')
const rollStoPrice  = ref('')

function formatDisplayDate(isoDate: string): string {
  const [yyyy, mm, dd] = isoDate.split('-')
  return `${mm}/${dd}/${yyyy}`
}

watch(() => props.tradeLeg, (leg) => {
  if (leg) {
    editQty.value       = leg.quantity
    editPrice.value     = String(leg.price)
    editTradeDate.value = formatDisplayDate(leg.tradedAt)
    editStrategy.value  = leg.strategyTag ?? ''
    editNotes.value     = leg.notes ?? ''
  }
}, { immediate: true })

watch(() => props.rollingPosition, (p) => {
  if (p) {
    rollQty.value       = p.openQuantity
    rollBtcPrice.value  = ''
    rollNewStrike.value = ''
    rollNewExpiry.value = ''
    rollStoPrice.value  = ''
    tradeDate.value     = todayDisplayDate()
    notes.value         = ''
  }
})

const editCashFlow = computed((): number | null => {
  const qty   = Number(editQty.value)
  const price = Number(editPrice.value)
  if (!qty || !price || !props.tradeLeg) return null
  const multiplier = props.tradeLeg.instrumentType === 'OPTION' ? 100 : 1
  const raw = qty * price * multiplier
  const action = props.tradeLeg.action
  if (action === 'BTO' || action === 'BTC' || action === 'ASSIGNED') return -raw
  return raw
})

const closeAction = computed(() => {
  if (!props.position) return ''
  return props.position.openAction === 'STO' ? 'BTC' : 'STC'
})

const closeStrategy = computed(() => {
  if (!props.position) return ''
  const { openAction, optionType } = props.position
  if (openAction === 'STO' && optionType === 'PUT')  return 'CSP'
  if (openAction === 'STO' && optionType === 'CALL') return 'CC'
  if (openAction === 'BTO') return 'Long'
  return ''
})

const closeCashFlow = computed((): number | null => {
  const qty   = Number(closeQty.value)
  const price = Number(exitPrice.value)
  if (!qty || !price || !props.position) return null
  const multiplier = props.position.instrumentType === 'OPTION' ? 100 : 1
  return closeAction.value === 'BTC'
    ? -(qty * price * multiplier)
    :  (qty * price * multiplier)
})

const rollBtcCashFlow = computed((): number | null => {
  const qty   = Number(rollQty.value)
  const price = Number(rollBtcPrice.value)
  if (!qty || !price) return null
  return -(qty * price * 100)
})

const rollStoCashFlow = computed((): number | null => {
  const qty   = Number(rollQty.value)
  const price = Number(rollStoPrice.value)
  if (!qty || !price) return null
  return qty * price * 100
})

function formatExpiry(iso: string): string {
  const [, month, day] = iso.split('-')
  return `${parseInt(month!)}/${parseInt(day!)}`
}

function todayDisplayDate(): string {
  const d  = new Date()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${mm}/${dd}/${d.getFullYear()}`
}

function toIsoDate(display: string): string {
  const [mm, dd, yyyy] = display.split('/')
  return `${yyyy ?? ''}-${(mm ?? '').padStart(2, '0')}-${(dd ?? '').padStart(2, '0')}`
}

function formatCurrency(value: number): string {
  return value.toLocaleString('en-US', { style: 'currency', currency: 'USD' })
}

function handleSave() {
  if (props.mode === 'parse') {
    emit('save', {
      strategyTag: strategyTag.value,
      notes:       notes.value,
      tradeDate:   toIsoDate(tradeDate.value),
    })
  } else if (props.mode === 'close') {
    emit('save', {
      strategyTag: closeStrategy.value,
      notes:       notes.value,
      tradeDate:   toIsoDate(tradeDate.value),
      qty:         Number(closeQty.value),
      exitPrice:   Number(exitPrice.value),
    })
  } else if (props.mode === 'edit') {
    emit('save', {
      strategyTag: editStrategy.value,
      notes:       editNotes.value,
      tradeDate:   toIsoDate(editTradeDate.value),
      qty:         Number(editQty.value),
      exitPrice:   Number(editPrice.value),
    })
  } else if (props.mode === 'roll') {
    emit('roll', {
      qty:       Number(rollQty.value),
      btcPrice:  Number(rollBtcPrice.value),
      newStrike: Number(rollNewStrike.value),
      newExpiry: rollNewExpiry.value,
      stoPrice:  Number(rollStoPrice.value),
      tradeDate: toIsoDate(tradeDate.value),
      notes:     notes.value,
    })
  }
}
</script>

<template>
  <div class="backdrop" @click.self="emit('cancel')">
    <div class="dialog" role="dialog" :aria-label="mode === 'parse' ? 'Confirm trade' : mode === 'close' ? 'Close position' : mode === 'roll' ? 'Roll position' : 'Edit trade'">

      <div class="dialog-header">
        <span class="dialog-title">{{
          mode === 'parse' ? 'CONFIRM TRADE' :
          mode === 'close' ? 'CLOSE POSITION' :
          mode === 'roll'  ? 'ROLL POSITION'  :
          'EDIT TRADE'
        }}</span>
        <button class="btn-x" @click="emit('cancel')" aria-label="Close">✕</button>
      </div>

      <div class="dialog-body">

        <!-- Multi-leg parse mode -->
        <template v-if="mode === 'parse' && trades && trades.length > 0">
          <table class="legs-table">
            <thead>
              <tr class="legs-thead">
                <th class="legs-th">ACTION</th>
                <th class="legs-th">QTY</th>
                <th class="legs-th">TICKER</th>
                <th class="legs-th">STRIKE</th>
                <th class="legs-th">EXPIRY</th>
                <th class="legs-th">PRICE</th>
                <th class="legs-th legs-cash">CASH FLOW</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(leg, i) in trades" :key="i" class="legs-row">
                <td class="legs-td">{{ leg.action }}</td>
                <td class="legs-td">{{ leg.qty }}</td>
                <td class="legs-td">{{ leg.ticker }}</td>
                <td class="legs-td">{{ optionLabel(leg) }}</td>
                <td class="legs-td">{{ leg.expiry ?? '—' }}</td>
                <td class="legs-td">{{ formatCurrency(leg.price) }}</td>
                <td class="legs-td legs-cash" :class="leg.cashFlow >= 0 ? 'profit' : 'loss'">
                  {{ leg.cashFlow >= 0 ? '+' : '' }}{{ formatCurrency(leg.cashFlow) }}
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr class="legs-net-row">
                <td class="legs-td" colspan="5"></td>
                <td class="legs-td legs-net-label">NET</td>
                <td class="legs-td legs-cash" :class="netCashFlow >= 0 ? 'profit' : 'loss'">
                  {{ netCashFlow >= 0 ? '+' : '' }}{{ formatCurrency(netCashFlow) }}
                </td>
              </tr>
            </tfoot>
          </table>

          <div class="divider" />

          <label class="input-label" for="ml-trade-date">TRADE DATE</label>
          <input id="ml-trade-date" v-model="tradeDate" type="text" class="text-input" placeholder="MM/DD/YYYY" />

          <label class="input-label" for="ml-strategy">STRATEGY</label>
          <input id="ml-strategy" v-model="strategyTag" type="text" class="text-input" placeholder="Bull Put Spread, Iron Condor…" />

          <label class="input-label" for="ml-notes">NOTES</label>
          <textarea id="ml-notes" v-model="notes" class="notes-textarea" rows="3" placeholder="Optional notes…" />
        </template>

        <!-- Parse mode -->
        <template v-else-if="mode === 'parse' && trade">
          <div class="field-grid">
            <div class="field-cell">
              <span class="field-label">ACTION</span>
              <span class="field-value">{{ trade.action }}</span>
            </div>
            <div class="field-cell">
              <span class="field-label">QTY</span>
              <span class="field-value">{{ trade.qty }}</span>
            </div>
            <div class="field-cell">
              <span class="field-label">TICKER</span>
              <span class="field-value">{{ trade.ticker }}</span>
            </div>
            <div class="field-cell">
              <span class="field-label">INSTRUMENT</span>
              <span class="field-value">{{ trade.instrumentType }}</span>
            </div>
            <template v-if="trade.optionType">
              <div class="field-cell">
                <span class="field-label">STRIKE</span>
                <span class="field-value">${{ trade.strike }}</span>
              </div>
              <div class="field-cell">
                <span class="field-label">EXPIRY</span>
                <span class="field-value">{{ trade.expiry }}</span>
              </div>
            </template>
            <div class="field-cell">
              <span class="field-label">PRICE</span>
              <span class="field-value">{{ formatCurrency(trade.price) }}</span>
            </div>
            <div class="field-cell">
              <span class="field-label">CASH FLOW</span>
              <span class="field-value" :class="trade.cashFlow >= 0 ? 'profit' : 'loss'">
                {{ trade.cashFlow >= 0 ? '+' : '' }}{{ formatCurrency(trade.cashFlow) }}
              </span>
            </div>
          </div>

          <div class="divider" />

          <label class="input-label" for="trade-date">TRADE DATE</label>
          <input
            id="trade-date"
            v-model="tradeDate"
            type="text"
            class="text-input"
            placeholder="MM/DD/YYYY"
          />

          <label class="input-label" for="strategy">STRATEGY</label>
          <input
            id="strategy"
            v-model="strategyTag"
            type="text"
            class="text-input"
            placeholder="CSP, CC, Long…"
          />

          <label class="input-label" for="notes">NOTES</label>
          <textarea
            id="notes"
            v-model="notes"
            class="notes-textarea"
            rows="3"
            placeholder="Optional notes…"
          />
        </template>

        <!-- Edit mode -->
        <template v-else-if="mode === 'edit' && tradeLeg">
          <div class="field-grid">
            <div class="field-cell">
              <span class="field-label">ACTION</span>
              <span class="field-value">{{ tradeLeg.action }}</span>
            </div>
            <div class="field-cell">
              <span class="field-label">TICKER</span>
              <span class="field-value">{{ tradeLeg.ticker }}</span>
            </div>
            <div class="field-cell">
              <span class="field-label">INSTRUMENT</span>
              <span class="field-value">{{ tradeLeg.instrumentType }}</span>
            </div>
            <template v-if="tradeLeg.instrumentType === 'OPTION'">
              <div class="field-cell">
                <span class="field-label">STRIKE</span>
                <span class="field-value">${{ tradeLeg.strike }}</span>
              </div>
              <div class="field-cell">
                <span class="field-label">EXPIRY</span>
                <span class="field-value">{{ formatExpiry(tradeLeg.expiry!) }}</span>
              </div>
            </template>
          </div>

          <div class="divider" />

          <label class="input-label" for="edit-qty">QTY</label>
          <input id="edit-qty" v-model="editQty" type="number" min="1" class="text-input" />

          <label class="input-label" for="edit-price">PRICE</label>
          <input id="edit-price" v-model="editPrice" type="number" min="0.01" step="0.01" class="text-input" placeholder="0.00" />

          <div v-if="editCashFlow !== null" class="field-cell" style="margin-top: 4px;">
            <span class="field-label">CASH FLOW</span>
            <span class="field-value" :class="editCashFlow >= 0 ? 'profit' : 'loss'">
              {{ editCashFlow >= 0 ? '+' : '' }}{{ formatCurrency(editCashFlow) }}
            </span>
          </div>

          <div class="divider" />

          <label class="input-label" for="edit-trade-date">TRADE DATE</label>
          <input id="edit-trade-date" v-model="editTradeDate" type="text" class="text-input" placeholder="MM/DD/YYYY" />

          <label class="input-label" for="edit-strategy">STRATEGY</label>
          <input id="edit-strategy" v-model="editStrategy" type="text" class="text-input" placeholder="CSP, CC, Long…" />

          <label class="input-label" for="edit-notes">NOTES</label>
          <textarea id="edit-notes" v-model="editNotes" class="notes-textarea" rows="3" placeholder="Optional notes…" />
        </template>

        <!-- Close mode -->
        <template v-else-if="mode === 'close' && position">
          <div class="field-grid">
            <div class="field-cell">
              <span class="field-label">TICKER</span>
              <span class="field-value">{{ position.ticker }}</span>
            </div>
            <div class="field-cell">
              <span class="field-label">INSTRUMENT</span>
              <span class="field-value">{{ position.instrumentType }}</span>
            </div>
            <template v-if="position.instrumentType === 'OPTION'">
              <div class="field-cell">
                <span class="field-label">STRIKE</span>
                <span class="field-value">${{ position.strike }}</span>
              </div>
              <div class="field-cell">
                <span class="field-label">EXPIRY</span>
                <span class="field-value">{{ formatExpiry(position.expiry!) }}</span>
              </div>
            </template>
            <div class="field-cell">
              <span class="field-label">ACTION</span>
              <span class="field-value">{{ closeAction }}</span>
            </div>
            <div class="field-cell">
              <span class="field-label">STRATEGY</span>
              <span class="field-value">{{ closeStrategy || '—' }}</span>
            </div>
          </div>

          <div class="divider" />

          <label class="input-label" for="close-qty">QTY</label>
          <input
            id="close-qty"
            v-model="closeQty"
            type="number"
            min="1"
            class="text-input"
          />

          <label class="input-label" for="exit-price">EXIT PRICE</label>
          <input
            id="exit-price"
            v-model="exitPrice"
            type="number"
            min="0"
            step="0.01"
            class="text-input"
            placeholder="0.00"
          />

          <div v-if="closeCashFlow !== null" class="field-cell" style="margin-top: 4px;">
            <span class="field-label">CASH FLOW</span>
            <span class="field-value" :class="closeCashFlow >= 0 ? 'profit' : 'loss'">
              {{ closeCashFlow >= 0 ? '+' : '' }}{{ formatCurrency(closeCashFlow) }}
            </span>
          </div>

          <div class="divider" />

          <label class="input-label" for="close-trade-date">TRADE DATE</label>
          <input
            id="close-trade-date"
            v-model="tradeDate"
            type="text"
            class="text-input"
            placeholder="MM/DD/YYYY"
          />

          <label class="input-label" for="close-notes">NOTES</label>
          <textarea
            id="close-notes"
            v-model="notes"
            class="notes-textarea"
            rows="3"
            placeholder="Optional notes…"
          />
        </template>

        <!-- Roll mode -->
        <template v-else-if="mode === 'roll' && rollingPosition">
          <div class="field-grid">
            <div class="field-cell">
              <span class="field-label">TICKER</span>
              <span class="field-value">{{ rollingPosition.ticker }}</span>
            </div>
            <div class="field-cell">
              <span class="field-label">OPTION TYPE</span>
              <span class="field-value">{{ rollingPosition.optionType }}</span>
            </div>
            <div class="field-cell">
              <span class="field-label">STRIKE</span>
              <span class="field-value">${{ rollingPosition.strike }}</span>
            </div>
            <div class="field-cell">
              <span class="field-label">EXPIRY</span>
              <span class="field-value">{{ formatExpiry(rollingPosition.expiry!) }}</span>
            </div>
            <div class="field-cell">
              <span class="field-label">ACTION</span>
              <span class="field-value">BTC</span>
            </div>
            <div class="field-cell">
              <span class="field-label">QTY</span>
              <span class="field-value">{{ rollingPosition.openQuantity }}</span>
            </div>
          </div>

          <div class="divider" />

          <label class="input-label" for="roll-btc-price">BTC PRICE</label>
          <input id="roll-btc-price" v-model="rollBtcPrice" type="number" min="0" step="0.01" class="text-input" placeholder="0.00" />

          <div v-if="rollBtcCashFlow !== null" class="field-cell" style="margin-top: 4px;">
            <span class="field-label">CASH FLOW</span>
            <span class="field-value loss">{{ formatCurrency(rollBtcCashFlow) }}</span>
          </div>

          <div class="divider" />
          <span class="section-label">OPENING</span>

          <label class="input-label" for="roll-qty">QTY</label>
          <input id="roll-qty" v-model="rollQty" type="number" min="1" class="text-input" />

          <label class="input-label" for="roll-new-strike">NEW STRIKE</label>
          <input id="roll-new-strike" v-model="rollNewStrike" type="number" min="0" step="0.5" class="text-input" placeholder="0.00" />

          <label class="input-label" for="roll-new-expiry">NEW EXPIRY</label>
          <input id="roll-new-expiry" v-model="rollNewExpiry" type="text" class="text-input" placeholder="MM/DD" />

          <label class="input-label" for="roll-sto-price">STO PRICE</label>
          <input id="roll-sto-price" v-model="rollStoPrice" type="number" min="0" step="0.01" class="text-input" placeholder="0.00" />

          <div v-if="rollStoCashFlow !== null" class="field-cell" style="margin-top: 4px;">
            <span class="field-label">CASH FLOW</span>
            <span class="field-value profit">+{{ formatCurrency(rollStoCashFlow) }}</span>
          </div>

          <div class="divider" />

          <label class="input-label" for="roll-trade-date">TRADE DATE</label>
          <input id="roll-trade-date" v-model="tradeDate" type="text" class="text-input" placeholder="MM/DD/YYYY" />

          <label class="input-label" for="roll-notes">NOTES</label>
          <textarea id="roll-notes" v-model="notes" class="notes-textarea" rows="3" placeholder="Optional notes…" />
        </template>

        <p v-if="saveError" class="save-error">{{ saveError }}</p>
      </div>

      <div class="dialog-footer">
        <button class="btn-cancel" @click="emit('cancel')">Cancel</button>
        <button class="btn-save" :disabled="saving" @click="handleSave">{{ mode === 'edit' ? 'Save Changes' : mode === 'roll' ? 'Save Roll' : 'Save Trade' }}</button>
      </div>

    </div>
  </div>
</template>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fade-in 0.15s ease;
}

@keyframes fade-in {
  from { opacity: 0; }
  to   { opacity: 1; }
}

.dialog {
  width: 480px;
  background: var(--surface-container-high);
  border: 1px solid var(--outline-variant);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  animation: scale-in 0.15s ease;
}

@keyframes scale-in {
  from { transform: scale(0.97); opacity: 0; }
  to   { transform: scale(1);    opacity: 1; }
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--outline-variant);
  flex-shrink: 0;
}

.dialog-title {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
}

.btn-x {
  background: none;
  border: none;
  color: var(--on-surface-variant);
  cursor: pointer;
  font-size: 14px;
  padding: 0;
  line-height: 1;
}

.btn-x:hover { color: var(--on-surface); }

.dialog-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 16px;
}

.field-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.field-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
}

.field-value {
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 500;
  color: var(--on-surface);
}

.profit { color: var(--color-profit); }
.loss   { color: var(--color-loss); }

.divider {
  border-top: 1px solid var(--outline-variant);
  margin: 4px 0;
}

.input-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
}

.section-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  margin-bottom: 4px;
}

.text-input,
.notes-textarea {
  width: 100%;
  background: var(--surface-container);
  border: 1px solid var(--outline-variant);
  border-radius: 0;
  color: var(--on-surface);
  font-family: var(--font-mono);
  font-size: 13px;
  padding: 6px 10px;
  outline: none;
  box-sizing: border-box;
  resize: none;
}

.text-input:focus,
.notes-textarea:focus { border-color: var(--primary); }

.save-error {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--color-loss);
  margin: 0;
}

.dialog-footer {
  display: flex;
  gap: 8px;
  padding: 16px 20px;
  border-top: 1px solid var(--outline-variant);
  flex-shrink: 0;
}

.btn-cancel {
  flex: 1;
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--outline-variant);
  border-radius: 0;
  color: var(--on-surface);
  font-family: var(--font-ui);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-cancel:hover { border-color: var(--outline); }

.btn-save {
  flex: 2;
  padding: 8px 16px;
  background: var(--primary-container);
  border: none;
  border-radius: 0;
  color: var(--on-primary-container);
  font-family: var(--font-ui);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.btn-save:hover:not(:disabled) { filter: brightness(1.1); }
.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }

.legs-table {
  width: 100%;
  border-collapse: collapse;
}

.legs-thead { background: var(--surface-container-low); }

.legs-th {
  padding: 4px 8px;
  text-align: left;
  font-family: var(--font-ui);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  white-space: nowrap;
}

.legs-td {
  padding: 5px 8px;
  border-bottom: 1px solid var(--outline-variant);
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--on-surface);
  white-space: nowrap;
}

.legs-cash { text-align: right; }

.legs-net-row .legs-td {
  border-bottom: none;
  border-top: 1px solid var(--outline-variant);
  font-weight: 700;
}

.legs-net-label {
  font-family: var(--font-ui);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  text-align: right;
}
</style>
