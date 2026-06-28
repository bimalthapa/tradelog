<script setup lang="ts">
import { computed } from 'vue'

interface Item { label: string; value: number }

const props = withDefaults(defineProps<{ items: Item[] }>(), { items: () => [] })

const LABEL_W = 120
const GAP     = 8
const R_PAD   = 72
const TOP_PAD = 4
const ROW_H   = 32
const BAR_H   = 18
const SVG_W   = 520
const CHART_W = SVG_W - LABEL_W - GAP - R_PAD   // 320

const svgHeight = computed(() => Math.max(props.items.length * ROW_H + TOP_PAD * 2, 40))

const geometry = computed(() => {
  if (!props.items.length) return []
  const vals   = props.items.map(i => i.value)
  const minVal = Math.min(0, ...vals)
  const maxVal = Math.max(0, ...vals)
  const range  = (maxVal - minVal) || 1
  const zeroX  = LABEL_W + GAP + (Math.abs(minVal) / range) * CHART_W

  return props.items.map((item, i) => {
    const barW      = (Math.abs(item.value) / range) * CHART_W
    const barX      = item.value >= 0 ? zeroX : zeroX - barW
    const barY      = TOP_PAD + i * ROW_H + (ROW_H - BAR_H) / 2
    const midY      = TOP_PAD + i * ROW_H + ROW_H / 2
    const valX      = item.value >= 0 ? barX + barW + 6 : barX - 6
    const valAnchor = item.value >= 0 ? 'start' : 'end'
    const profit    = item.value >= 0
    return { label: item.label, value: item.value, barX, barY, barW, midY, valX, valAnchor, zeroX, profit }
  })
})

function fmt(v: number): string {
  const abs = Math.abs(v).toLocaleString('en-US', { maximumFractionDigits: 0 })
  return (v >= 0 ? '+$' : '-$') + abs
}
</script>

<template>
  <svg
    :viewBox="`0 0 ${SVG_W} ${svgHeight}`"
    :height="svgHeight"
    width="100%"
    xmlns="http://www.w3.org/2000/svg"
    class="bar-chart"
  >
    <!-- Zero line -->
    <line
      v-if="geometry.length"
      :x1="geometry[0].zeroX" :x2="geometry[0].zeroX"
      :y1="TOP_PAD" :y2="svgHeight - TOP_PAD"
      stroke="var(--outline-variant)" stroke-width="1"
    />

    <g v-for="row in geometry" :key="row.label">
      <text
        :x="LABEL_W" :y="row.midY"
        text-anchor="end" dominant-baseline="middle"
        class="row-label"
      >{{ row.label }}</text>

      <rect
        :x="row.barX" :y="row.barY"
        :width="Math.max(row.barW, 2)" :height="BAR_H"
        :fill="row.profit ? 'var(--color-profit)' : 'var(--color-loss)'"
        opacity="0.85"
      />

      <text
        :x="row.valX" :y="row.midY"
        :text-anchor="row.valAnchor"
        dominant-baseline="middle"
        class="val-label"
        :fill="row.profit ? 'var(--color-profit)' : 'var(--color-loss)'"
      >{{ fmt(row.value) }}</text>
    </g>

    <text
      v-if="!geometry.length"
      :x="SVG_W / 2" y="24"
      text-anchor="middle"
      class="empty-label"
    >No data</text>
  </svg>
</template>

<style scoped>
.bar-chart  { display: block; }
.row-label  { font-family: var(--font-mono); font-size: 12px; fill: var(--on-surface-variant); }
.val-label  { font-family: var(--font-mono); font-size: 11px; font-weight: 700; }
.empty-label { font-family: var(--font-mono); font-size: 12px; fill: var(--outline); }
</style>
