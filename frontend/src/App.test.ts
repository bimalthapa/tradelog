// @vitest-environment happy-dom
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import App from './App.vue'
import { useTradeLogStore } from '@/stores/tradeLog'

vi.mock('@/components/Sidebar.vue', () => ({ default: { template: '<div />' } }))
vi.mock('vue-router', () => ({
  RouterView:  { template: '<div />' },
  RouterLink:  { template: '<a><slot /></a>' },
  useRoute:    () => ({ params: {} }),
}))
vi.mock('@/services/campaignService',  () => ({ getCampaigns: vi.fn(), getCampaign: vi.fn() }))
vi.mock('@/services/tradeService',     () => ({ getTradesForCampaign: vi.fn() }))
vi.mock('@/services/positionService',  () => ({ getPositionsForCampaign: vi.fn() }))
vi.mock('@/services/analyticsService', () => ({
  getAnalyticsSummary: vi.fn(), getPnlByStrategy: vi.fn(), getCumulativeData: vi.fn(),
}))

beforeEach(() => vi.clearAllMocks())

describe('App', () => {
  it('calls fetchCampaigns on mount', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const store = useTradeLogStore()
    vi.spyOn(store, 'fetchCampaigns').mockResolvedValue(undefined)
    mount(App, { global: { plugins: [pinia] } })
    await flushPromises()
    expect(store.fetchCampaigns).toHaveBeenCalledOnce()
  })
})
