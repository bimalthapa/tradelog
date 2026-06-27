<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ParsedTrade } from '@/types/index'

const props = defineProps<{ trade: ParsedTrade; saveError?: string; saving?: boolean }>()
const emit = defineEmits<{
  save:   [{ strategyTag: string; notes: string }]
  cancel: []
}>()

const strategyTag = ref(props.trade.strategy)
const notes       = ref('')

watch(() => props.trade, (t) => {
  strategyTag.value = t.strategy
  notes.value = ''
})

function formatCurrency(value: number): string {
  return value.toLocaleString('en-US', { style: 'currency', currency: 'USD' })
}
</script>

<template>
  <div class="confirm-panel">
    <div class="panel-header">
      <span class="panel-title">CONFIRM TRADE</span>
      <button class="btn-close" @click="emit('cancel')" aria-label="Close">✕</button>
    </div>

    <div class="panel-body">
      <div class="field-row"><span class="field-label">ACTION</span><span class="field-value">{{ trade.action }}</span></div>
      <div class="field-row"><span class="field-label">QTY</span><span class="field-value">{{ trade.qty }}</span></div>
      <div class="field-row"><span class="field-label">TICKER</span><span class="field-value">{{ trade.ticker }}</span></div>
      <div class="field-row"><span class="field-label">INSTRUMENT</span><span class="field-value">{{ trade.instrumentType }}</span></div>
      <div v-if="trade.optionType" class="field-row"><span class="field-label">TYPE</span><span class="field-value">{{ trade.optionType }}</span></div>
      <div v-if="trade.strike != null" class="field-row"><span class="field-label">STRIKE</span><span class="field-value">${{ trade.strike }}</span></div>
      <div v-if="trade.expiry" class="field-row"><span class="field-label">EXPIRY</span><span class="field-value">{{ trade.expiry }}</span></div>
      <div class="field-row"><span class="field-label">PRICE</span><span class="field-value">{{ formatCurrency(trade.price) }}</span></div>
      <div class="field-row">
        <span class="field-label">CASH FLOW</span>
        <span class="field-value" :class="trade.cashFlow >= 0 ? 'profit' : 'loss'">
          {{ trade.cashFlow >= 0 ? '+' : '' }}{{ formatCurrency(trade.cashFlow) }}
        </span>
      </div>

      <div class="divider" />

      <label class="input-label" for="strategy">STRATEGY</label>
      <input
        id="strategy"
        v-model="strategyTag"
        type="text"
        class="strategy-input"
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

      <p v-if="saveError" class="save-error">{{ saveError }}</p>
    </div>

    <div class="panel-footer">
      <button class="btn-cancel" @click="emit('cancel')">Cancel</button>
      <button class="btn-save" :disabled="saving" @click="emit('save', { strategyTag: strategyTag, notes: notes })">Save Trade</button>
    </div>
  </div>
</template>

<style scoped>
.confirm-panel {
  position: fixed;
  top: 0;
  right: 0;
  width: 340px;
  height: 100vh;
  background: var(--surface-container-high);
  border-left: 1px solid var(--outline-variant);
  box-shadow: -8px 0 32px rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  z-index: 100;
  animation: slide-in 0.15s ease;
}

@keyframes slide-in {
  from { transform: translateX(30px); opacity: 0; }
  to   { transform: translateX(0);    opacity: 1; }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--outline-variant);
  flex-shrink: 0;
}

.panel-title {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
}

.btn-close {
  background: none;
  border: none;
  color: var(--on-surface-variant);
  cursor: pointer;
  font-size: 14px;
  padding: 0;
  line-height: 1;
}

.btn-close:hover { color: var(--on-surface); }

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.strategy-input,
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

.strategy-input:focus,
.notes-textarea:focus { border-color: var(--primary); }

.save-error {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--color-loss);
  margin: 0;
}

.panel-footer {
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
</style>
