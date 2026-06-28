// @vitest-environment happy-dom
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import LineChart from './LineChart.vue'

type Point = { month: string; value: number }

function mountChart(points: Point[]) {
  return mount(LineChart, { props: { points } })
}

const SIX_MONTHS: Point[] = [
  { month: '2026-01', value:  450 },
  { month: '2026-02', value:  920 },
  { month: '2026-03', value: 1340 },
  { month: '2026-04', value: 1890 },
  { month: '2026-05', value: 2450 },
  { month: '2026-06', value: 3240 },
]

describe('LineChart — empty state', () => {
  it('shows empty-label with no points', () => {
    const w = mountChart([])
    expect(w.find('.empty-label').exists()).toBe(true)
  })

  it('shows empty-label with a single point', () => {
    const w = mountChart([{ month: '2026-01', value: 100 }])
    expect(w.find('.empty-label').exists()).toBe(true)
  })

  it('renders no path when no points', () => {
    const w = mountChart([])
    expect(w.findAll('path')).toHaveLength(0)
  })
})

describe('LineChart — with data', () => {
  it('renders a line path when 2+ points given', () => {
    const w = mountChart(SIX_MONTHS)
    const paths = w.findAll('path')
    expect(paths.length).toBeGreaterThanOrEqual(1)
  })

  it('renders an area fill path', () => {
    const w = mountChart(SIX_MONTHS)
    const areaPaths = w.findAll('path').filter(p =>
      p.attributes('fill') && p.attributes('fill') !== 'none'
    )
    expect(areaPaths.length).toBeGreaterThanOrEqual(1)
  })

  it('renders x-axis month labels', () => {
    const w = mountChart(SIX_MONTHS)
    expect(w.text()).toContain("Jan '26")
  })

  it('does not show empty-label when data present', () => {
    const w = mountChart(SIX_MONTHS)
    expect(w.find('.empty-label').exists()).toBe(false)
  })
})
