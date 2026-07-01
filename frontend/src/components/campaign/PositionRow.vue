<script setup lang="ts">
import { computed } from 'vue'
import type { Position } from '@/types/index'
import { computeDte } from '@/utils/dte'

const props = defineProps<{ position: Position; rollTooltip?: string }>()
const emit  = defineEmits<{ close: [Position]; roll: [Position] }>()

function formatExpiry(isoDate: string): string {
  const [, month, day] = isoDate.split('-')
  return `${parseInt(month!)}/${parseInt(day!)}`
}

const dte = computed<number | null>(() =>
  props.position.instrumentType === 'OPTION' && props.position.expiry
    ? computeDte(props.position.expiry)
    : null
)

function dteCssClass(): string {
  if (dte.value === null) return 'dte-gray'
  if (dte.value < 0)     return 'dte-gray'
  if (dte.value <= 7)    return 'dte-red'
  if (dte.value <= 21)   return 'dte-amber'
  return 'dte-green'
}

function dteLabel(): string {
  if (dte.value === null) return '—'
  if (dte.value < 0)     return 'EXP'
  return `${dte.value} DTE`
}
</script>

<template>
  <tr class="pos-row">
    <td class="cell dim mono">{{ position.openedAt }}</td>
    <td class="cell">
      <span class="action mono" :class="position.openAction === 'STO' ? 'credit' : 'debit'">
        {{ position.openAction }}
      </span>
    </td>
    <td class="cell mono">{{ position.openQuantity }}</td>
    <td class="cell">
      <span class="instr-ticker mono">{{ position.ticker }}</span>
      <span v-if="position.instrumentType === 'OPTION'" class="instr-detail mono">
        {{ position.strike }}{{ position.optionType === 'CALL' ? 'C' : 'P' }} {{ formatExpiry(position.expiry!) }}
      </span>
      <span v-else class="instr-detail mono">STOCK</span>
      <span v-if="props.rollTooltip" class="roll-badge" :title="props.rollTooltip">↻</span>
    </td>
    <td class="cell mono">${{ position.avgPrice.toFixed(2) }}</td>
    <td class="cell">
      <span class="status" :class="position.status === 'OPEN' ? 'status-open' : 'status-closed'">
        {{ position.status }}
      </span>
    </td>
    <td class="cell" :class="dteCssClass()">
      <span class="mono">{{ dteLabel() }}</span>
    </td>
    <td class="cell">
      <button
        v-if="position.status === 'OPEN'"
        class="btn-close-pos"
        @click="emit('close', position)"
      >
        Close
      </button>
      <button
        v-if="position.status === 'OPEN' && position.instrumentType === 'OPTION'"
        class="btn-roll-pos"
        @click="emit('roll', position)"
      >
        Roll
      </button>
    </td>
  </tr>
</template>

<style scoped>
.pos-row {
  border-bottom: 1px solid var(--outline-variant);
}

.pos-row:hover {
  background: rgba(255, 255, 255, 0.025);
}

.cell {
  padding: 0 10px;
  height: 32px;
  vertical-align: middle;
  white-space: nowrap;
}

.mono {
  font-family: var(--font-mono);
  font-size: 12px;
}

.instr-ticker {
  font-weight: 700;
  color: var(--on-surface);
  margin-right: 6px;
}

.instr-detail {
  font-weight: 400;
  color: var(--outline);
}

.action {
  font-weight: 700;
  border-radius: 0;
}

.credit { color: var(--color-profit); }
.debit  { color: var(--color-loss); }
.dim    { color: var(--outline); }

.status {
  font-family: var(--font-mono);
  font-size: 12px;
  border-radius: 0;
}

.status-open   { color: var(--primary); }
.status-closed { color: var(--outline); }

.btn-close-pos {
  background: transparent;
  border: 1px solid var(--outline-variant);
  border-radius: 0;
  color: var(--color-loss);
  font-family: var(--font-ui);
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  cursor: pointer;
  white-space: nowrap;
}

.btn-close-pos:hover { border-color: var(--color-loss); }

.roll-badge {
  font-size: 11px;
  color: var(--primary);
  margin-left: 6px;
  cursor: default;
  opacity: 0.7;
}

.btn-roll-pos {
  background: transparent;
  border: 1px solid var(--outline-variant);
  border-radius: 0;
  color: var(--primary);
  font-family: var(--font-ui);
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  cursor: pointer;
  white-space: nowrap;
  margin-left: 4px;
}

.btn-roll-pos:hover { border-color: var(--primary); }

.dte-green { color: var(--color-profit); }
.dte-amber { color: var(--color-warning); }
.dte-red   { color: var(--color-loss); }
.dte-gray  { color: var(--outline); }
</style>
