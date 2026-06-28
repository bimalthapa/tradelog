// @vitest-environment happy-dom
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import DashboardView from './DashboardView.vue'
import { useTradeLogStore } from '@/stores/tradeLog'
import type { Campaign } from '@/types/index'

vi.mock('vue-router', () => ({
  useRouter:  () => ({ push: vi.fn() }),
  RouterLink: { template: '<a><slot /></a>' },
}))

vi.mock('@/services/campaignService',  () => ({ getCampaigns: vi.fn(), getCampaign: vi.fn() }))
vi.mock('@/services/tradeService',     () => ({ getTradesForCampaign: vi.fn() }))
vi.mock('@/services/positionService',  () => ({ getPositionsForCampaign: vi.fn() }))
vi.mock('@/services/analyticsService', () => ({
  getAnalyticsSummary: vi.fn(), getPnlByStrategy: vi.fn(), getCumulativeData: vi.fn(),
}))

const mockActive: Campaign[] = [
  { id: 1, ticker: 'SPY', status: 'OPEN', openedAt: '2026-01-01', netCashFlow: 500, openPositionCount: 1 },
]
const mockClosed: Campaign[] = [
  { id: 2, ticker: 'AAPL', status: 'CLOSED', openedAt: '2025-06-01', netCashFlow: 200, openPositionCount: 0 },
]

function mountView(campaigns: Campaign[] = []) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useTradeLogStore()
  vi.spyOn(store, 'fetchCampaigns').mockResolvedValue(undefined)
  store.campaigns = campaigns
  return mount(DashboardView, {
    global: { plugins: [pinia], stubs: { CampaignRow: true } },
  })
}

beforeEach(() => vi.clearAllMocks())

describe('DashboardView — empty states', () => {
  it('shows "No active campaigns" when active list is empty', () => {
    const wrapper = mountView(mockClosed)
    const cells = wrapper.findAll('.empty-cell')
    expect(cells.some(c => c.text() === 'No active campaigns')).toBe(true)
  })

  it('shows "No closed campaigns" when closed list is empty', () => {
    const wrapper = mountView(mockActive)
    const cells = wrapper.findAll('.empty-cell')
    expect(cells.some(c => c.text() === 'No closed campaigns')).toBe(true)
  })

  it('shows both empty messages when no campaigns at all', () => {
    const wrapper = mountView([])
    const cells = wrapper.findAll('.empty-cell')
    const texts = cells.map(c => c.text())
    expect(texts).toContain('No active campaigns')
    expect(texts).toContain('No closed campaigns')
  })

  it('does not show empty-cell when active list has campaigns', () => {
    const wrapper = mountView([...mockActive, ...mockClosed])
    const cells = wrapper.findAll('.empty-cell')
    expect(cells.some(c => c.text() === 'No active campaigns')).toBe(false)
  })

  it('does not show empty-cell when closed list has campaigns', () => {
    const wrapper = mountView([...mockActive, ...mockClosed])
    const cells = wrapper.findAll('.empty-cell')
    expect(cells.some(c => c.text() === 'No closed campaigns')).toBe(false)
  })
})
