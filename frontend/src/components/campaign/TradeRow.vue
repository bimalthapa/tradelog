<script setup lang="ts">
import type { TradeLeg } from '@/types/index'

defineProps<{ trade: TradeLeg }>()

function formatExpiry(isoDate: string): string {
  const [, month, day] = isoDate.split('-')
  return `${parseInt(month!)}/${parseInt(day!)}`
}

function actionClass(action: TradeLeg['action']): string {
  if (action === 'STO' || action === 'STC') return 'credit'
  if (action === 'BTO' || action === 'BTC') return 'debit'
  if (action === 'ASSIGNED') return 'warning'
  return 'dim'
}

function formatCashFlow(value: number): string {
  const abs = Math.abs(value).toLocaleString('en-US', { style: 'currency', currency: 'USD' })
  return (value >= 0 ? '+' : '-') + abs
}

function legStatus(action: TradeLeg['action']): string {
  if (action === 'BTO' || action === 'STO') return 'open'
  if (action === 'BTC' || action === 'STC') return 'closed'
  if (action === 'EXPIRED') return 'expired'
  return 'assigned'
}

function legStatusClass(action: TradeLeg['action']): string {
  if (action === 'BTO' || action === 'STO') return 'status-open'
  if (action === 'BTC' || action === 'STC') return 'status-closed'
  if (action === 'EXPIRED') return 'status-expired'
  return 'status-assigned'
}
</script>

<template>
  <tr class="trade-row">
    <td class="cell dim mono">{{ trade.tradedAt }}</td>
    <td class="cell">
      <span class="action mono" :class="actionClass(trade.action)">{{ trade.action }}</span>
    </td>
    <td class="cell mono">{{ trade.quantity }}</td>
    <td class="cell">
      <span class="instr-ticker mono">{{ trade.ticker }}</span>
      <span v-if="trade.instrumentType === 'OPTION'" class="instr-detail mono">
        {{ trade.strike }}{{ trade.optionType === 'CALL' ? 'C' : 'P' }} {{ formatExpiry(trade.expiry!) }}
      </span>
      <span v-else class="instr-detail mono">STOCK</span>
    </td>
    <td class="cell mono">${{ trade.price.toFixed(2) }}</td>
    <td class="cell">
      <span class="cash-flow mono" :class="trade.netCashFlow >= 0 ? 'credit' : 'debit'">
        {{ formatCashFlow(trade.netCashFlow) }}
      </span>
    </td>
    <td class="cell">
      <span v-if="trade.strategyTag" class="strategy-chip">{{ trade.strategyTag }}</span>
      <span v-else class="dim mono">—</span>
    </td>
    <td class="cell">
      <span class="status mono" :class="legStatusClass(trade.action)">{{ legStatus(trade.action) }}</span>
    </td>
  </tr>
</template>

<style scoped>
.trade-row {
  border-bottom: 1px solid var(--outline-variant);
}

.trade-row:hover {
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

.credit  { color: var(--color-profit); }
.debit   { color: var(--color-loss); }
.warning { color: var(--color-warning); }
.dim     { color: var(--outline); }

.cash-flow { font-weight: 700; }

.strategy-chip {
  font-size: 10px;
  font-family: var(--font-ui);
  color: var(--outline);
  background: rgba(66, 71, 84, 0.25);
  border: 1px solid var(--outline-variant);
  border-radius: 0;
  padding: 2px 6px;
}

.status { font-weight: 400; border-radius: 0; }
.status-open     { color: var(--primary); }
.status-closed   { color: var(--outline); }
.status-expired  { color: var(--outline); }
.status-assigned { color: var(--color-warning); }
</style>
