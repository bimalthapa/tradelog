// @vitest-environment happy-dom
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import BarChart from './BarChart.vue'

function mountChart(items: { label: string; value: number }[]) {
  return mount(BarChart, { props: { items } })
}

describe('BarChart — empty state', () => {
  it('renders empty-label when no items', () => {
    const w = mountChart([])
    expect(w.find('.empty-label').exists()).toBe(true)
  })

  it('renders no bars when no items', () => {
    const w = mountChart([])
    expect(w.findAll('rect')).toHaveLength(0)
  })
})

describe('BarChart — positive values', () => {
  const items = [
    { label: 'NVDA', value: 1000 },
    { label: 'SPY',  value:  500 },
  ]

  it('renders one rect per item', () => {
    const w = mountChart(items)
    expect(w.findAll('rect')).toHaveLength(2)
  })

  it('fills profit-colored bars for positive values', () => {
    const w = mountChart(items)
    const rects = w.findAll('rect')
    rects.forEach(r => {
      expect(r.attributes('fill')).toBe('var(--color-profit)')
    })
  })

  it('renders label text for each item', () => {
    const w = mountChart(items)
    expect(w.text()).toContain('NVDA')
    expect(w.text()).toContain('SPY')
  })
})

describe('BarChart — negative values', () => {
  it('fills loss-colored bar for negative value', () => {
    const w = mountChart([{ label: 'TSLA', value: -300 }])
    const rect = w.find('rect')
    expect(rect.attributes('fill')).toBe('var(--color-loss)')
  })
})

describe('BarChart — mixed values', () => {
  it('renders both profit and loss colored bars', () => {
    const w = mountChart([
      { label: 'A', value:  800 },
      { label: 'B', value: -200 },
    ])
    const fills = w.findAll('rect').map(r => r.attributes('fill'))
    expect(fills).toContain('var(--color-profit)')
    expect(fills).toContain('var(--color-loss)')
  })
})
