<script setup lang="ts">
import type { Campaign } from '@/types/index'
import StatusChip from '@/components/StatusChip.vue'

defineProps<{
  campaign: Campaign
}>()

const emit = defineEmits<{
  select: [id: number]
}>()

function formatPnl(value: number): string {
  const abs = Math.abs(value)
  const sign = value > 0 ? '+' : '-'
  if (abs >= 1000) return `${sign}$${(abs / 1000).toFixed(1)}k`
  return `${sign}$${abs.toFixed(0)}`
}

function pnlClass(value: number): string {
  if (value > 0) return 'profit'
  if (value < 0) return 'loss'
  return 'neutral'
}
</script>

<template>
  <tr class="row" @click="emit('select', campaign.id)">
    <td class="cell">
      <span class="ticker">{{ campaign.ticker }}</span>
    </td>
    <td class="cell label-col">{{ campaign.label ?? '' }}</td>
    <td class="cell mono">
      <span v-if="campaign.costBasis != null">${{ campaign.costBasis.toFixed(2) }}</span>
      <span v-else class="dim">—</span>
    </td>
    <td class="cell mono">
      <span v-if="campaign.sharesHeld">{{ campaign.sharesHeld }}</span>
      <span v-else class="dim">—</span>
    </td>
    <td class="cell mono">{{ campaign.openPositionCount }}</td>
    <td class="cell mono" :class="pnlClass(campaign.netCashFlow)">
      {{ formatPnl(campaign.netCashFlow) }}
    </td>
    <td class="cell mono" :class="campaign.realizedPnl != null ? pnlClass(campaign.realizedPnl) : 'neutral'">
      {{ campaign.realizedPnl != null ? formatPnl(campaign.realizedPnl) : '—' }}
    </td>
    <td class="cell">
      <StatusChip :status="campaign.status" />
    </td>
    <td class="cell mono dim">{{ campaign.openedAt }}</td>
  </tr>
</template>

<style scoped>
.row {
  border-bottom: 1px solid var(--outline-variant);
  cursor: pointer;
}

.row:hover {
  background: rgba(255, 255, 255, 0.025);
}

.cell {
  padding: 0 10px;
  height: 32px;
  vertical-align: middle;
  white-space: nowrap;
}

.ticker {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 700;
  color: var(--on-surface);
}

.label-col {
  font-size: 14px;
  color: var(--on-surface-variant);
  max-width: 160px;
}

.mono {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--on-surface-variant);
}

.dim { color: var(--outline); }
.profit { color: var(--color-profit); }
.loss { color: var(--color-loss); }
.neutral { color: var(--outline); }
</style>
