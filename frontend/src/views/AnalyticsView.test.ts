// @vitest-environment happy-dom
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import AnalyticsView from './AnalyticsView.vue'
import { useTradeLogStore } from '@/stores/tradeLog'

function mountView() {
  return mount(AnalyticsView, { global: { renderStubDefaultSlot: true } })
}

beforeEach(() => {
  setActivePinia(createPinia())
  const store = useTradeLogStore()
  vi.spyOn(store, 'fetchCampaigns').mockResolvedValue(undefined)
  vi.spyOn(store, 'fetchAnalytics').mockResolvedValue(undefined)
})

describe('AnalyticsView — header', () => {
  it('renders PERFORMANCE screen label', () => {
    const w = mountView()
    expect(w.find('.screen-label').text()).toBe('PERFORMANCE')
  })

  it('renders Analytics title', () => {
    const w = mountView()
    expect(w.find('.screen-title').text()).toBe('Analytics')
  })
})

describe('AnalyticsView — KPI cards', () => {
  it('renders 5 KPI cards', () => {
    const w = mountView()
    expect(w.findAll('.kpi-card')).toHaveLength(5)
  })

  it('renders all 5 KPI labels', () => {
    const w = mountView()
    const text = w.text()
    expect(text).toContain('TOTAL PREMIUM')
    expect(text).toContain('NET OPTIONS P&L')
    expect(text).toContain('CAMPAIGN WIN RATE')
    expect(text).toContain('TRADE WIN RATE')
    expect(text).toContain('TOTAL TRADES')
  })

  it('renders win rate as percentage', () => {
    const w = mountView()
    expect(w.text()).toMatch(/\d+%/)
  })
})
