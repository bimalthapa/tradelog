<script setup lang="ts">
import { computed, getCurrentInstance } from 'vue'

interface Point { month: string; value: number }

const props = withDefaults(defineProps<{ points: Point[] }>(), { points: () => [] })

const uid    = getCurrentInstance()?.uid ?? 0
const gradId = `area-fill-${uid}`

const PAD_L = 56, PAD_R = 20, PAD_T = 16, PAD_B = 36
const SVG_W = 520, SVG_H = 180
const INN_W = SVG_W - PAD_L - PAD_R   // 444
const INN_H = SVG_H - PAD_T - PAD_B   // 128

const geo = computed(() => {
  const pts = props.points
  if (pts.length < 2) return null

  const vals  = pts.map(p => p.value)
  const minV  = Math.min(...vals)
  const maxV  = Math.max(...vals)
  const range = (maxV - minV) || 1
  const n     = pts.length
  const yBase = PAD_T + INN_H

  const xOf = (i: number) => PAD_L + (i / (n - 1)) * INN_W
  const yOf = (v: number) => PAD_T + (1 - (v - minV) / range) * INN_H

  const coords = pts.map((p, i) => ({ x: xOf(i), y: yOf(p.value), month: p.month }))

  const linePath = coords.map((c, i) =>
    `${i === 0 ? 'M' : 'L'}${c.x.toFixed(1)},${c.y.toFixed(1)}`
  ).join(' ')

  const areaPath = [
    `M${coords[0].x.toFixed(1)},${yBase}`,
    ...coords.map(c => `L${c.x.toFixed(1)},${c.y.toFixed(1)}`),
    `L${coords[n - 1].x.toFixed(1)},${yBase}`,
    'Z',
  ].join(' ')

  const yTicks = [minV, (minV + maxV) / 2, maxV].map(v => ({
    y: yOf(v),
    label: '$' + Math.round(v).toLocaleString(),
  }))

  const step    = Math.max(1, Math.ceil(n / 6))
  const xLabels = coords
    .filter((_, i) => i % step === 0 || i === n - 1)
    .map(c => ({ x: c.x, label: fmtMonth(c.month) }))

  return { linePath, areaPath, yTicks, xLabels, yBase }
})

function fmtMonth(ym: string): string {
  const [y, m] = ym.split('-')
  const names  = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']
  return `${names[parseInt(m) - 1]} '${y.slice(2)}`
}
</script>

<template>
  <svg
    :viewBox="`0 0 ${SVG_W} ${SVG_H}`"
    :height="SVG_H"
    width="100%"
    xmlns="http://www.w3.org/2000/svg"
    class="line-chart"
  >
    <defs>
      <linearGradient :id="gradId" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%"   stop-color="var(--color-profit)" stop-opacity="0.18" />
        <stop offset="100%" stop-color="var(--color-profit)" stop-opacity="0"    />
      </linearGradient>
    </defs>

    <template v-if="geo">
      <path :d="geo.areaPath" :fill="`url(#${gradId})`" />
      <path :d="geo.linePath" fill="none" stroke="var(--color-profit)" stroke-width="1.5" />

      <g v-for="tick in geo.yTicks" :key="tick.label">
        <line :x1="PAD_L" :x2="SVG_W - PAD_R" :y1="tick.y" :y2="tick.y"
              stroke="var(--outline-variant)" stroke-width="1" stroke-dasharray="2,3" />
        <text :x="PAD_L - 4" :y="tick.y" text-anchor="end" dominant-baseline="middle" class="tick-label">
          {{ tick.label }}
        </text>
      </g>

      <line :x1="PAD_L" :x2="SVG_W - PAD_R" :y1="geo.yBase" :y2="geo.yBase"
            stroke="var(--outline-variant)" stroke-width="1" />

      <text
        v-for="lbl in geo.xLabels" :key="lbl.x"
        :x="lbl.x" :y="geo.yBase + 18"
        text-anchor="middle" class="x-label"
      >{{ lbl.label }}</text>
    </template>

    <text
      v-else
      :x="SVG_W / 2" :y="SVG_H / 2"
      text-anchor="middle"
      class="empty-label"
    >No data</text>
  </svg>
</template>

<style scoped>
.line-chart  { display: block; }
.tick-label  { font-family: var(--font-mono); font-size: 10px; fill: var(--on-surface-variant); }
.x-label     { font-family: var(--font-mono); font-size: 10px; fill: var(--on-surface-variant); }
.empty-label { font-family: var(--font-mono); font-size: 12px; fill: var(--outline); }
</style>
