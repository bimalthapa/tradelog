// @vitest-environment happy-dom
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import Sidebar from './Sidebar.vue'
import { useTradeLogStore } from '@/stores/tradeLog'
import type { Campaign } from '@/types/index'

const routeParams: Record<string, string | undefined> = { id: '1' }

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: routeParams }),
  RouterLink: { template: '<a :href="to"><slot /></a>', props: ['to'] },
}))

vi.mock('@/services/campaignService',  () => ({ getCampaigns: vi.fn(), getCampaign: vi.fn() }))
vi.mock('@/services/tradeService',     () => ({ getTradesForCampaign: vi.fn() }))
vi.mock('@/services/positionService',  () => ({ getPositionsForCampaign: vi.fn() }))
vi.mock('@/services/analyticsService', () => ({
  getAnalyticsSummary: vi.fn(), getPnlByStrategy: vi.fn(), getCumulativeData: vi.fn(),
}))

const mockCampaigns: Campaign[] = [
  { id: 1, ticker: 'SPY',  label: 'Wheel',   status: 'OPEN',   openedAt: '2026-01-01', netCashFlow: 500, openPositionCount: 1 },
  { id: 2, ticker: 'NVDA', label: undefined,  status: 'OPEN',   openedAt: '2026-02-01', netCashFlow: 200, openPositionCount: 0 },
  { id: 3, ticker: 'AAPL', label: 'Finished', status: 'CLOSED', openedAt: '2025-06-01', netCashFlow: 100, openPositionCount: 0 },
]

const routerLinkStub = { template: '<a :href="to"><slot /></a>', props: ['to'] }

function mountSidebar() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useTradeLogStore()
  store.campaigns = mockCampaigns
  return { wrapper: mount(Sidebar, { global: { plugins: [pinia], stubs: { RouterLink: routerLinkStub } } }), store }
}

beforeEach(() => {
  routeParams.id = '1'
  vi.clearAllMocks()
})

describe('active campaigns section', () => {
  it('renders one item per active campaign', () => {
    const { wrapper } = mountSidebar()
    expect(wrapper.findAll('.campaign-item')).toHaveLength(3) // 2 active + 1 closed
    const active = wrapper.findAll('.campaign-item').filter(i => i.text().includes('SPY') || i.text().includes('NVDA'))
    expect(active).toHaveLength(2)
  })

  it('hides ACTIVE section when no active campaigns', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const store = useTradeLogStore()
    store.campaigns = mockCampaigns.filter(c => c.status === 'CLOSED')
    const wrapper = mount(Sidebar, { global: { plugins: [pinia], stubs: { RouterLink: routerLinkStub } } })
    expect(wrapper.text()).not.toContain('ACTIVE')
  })

  it('renders campaign ticker', () => {
    const { wrapper } = mountSidebar()
    expect(wrapper.text()).toContain('SPY')
    expect(wrapper.text()).toContain('NVDA')
  })

  it('renders label when present', () => {
    const { wrapper } = mountSidebar()
    expect(wrapper.text()).toContain('Wheel')
  })

  it('does not render label element when absent', () => {
    const { wrapper } = mountSidebar()
    const items = wrapper.findAll('.campaign-item')
    const nvdaItem = items.find(i => i.text().includes('NVDA'))
    expect(nvdaItem?.find('.campaign-item-label').exists()).toBe(false)
  })

  it('each item links to /campaign/:id', () => {
    const { wrapper } = mountSidebar()
    const links = wrapper.findAll('a')
    const campaignLinks = links.filter(l => l.attributes('href')?.startsWith('/campaign/'))
    expect(campaignLinks.length).toBeGreaterThanOrEqual(2)
    const hrefs = campaignLinks.map(l => l.attributes('href'))
    expect(hrefs).toContain('/campaign/1')
    expect(hrefs).toContain('/campaign/2')
  })
})

describe('closed campaigns section', () => {
  it('renders one item per closed campaign', () => {
    const { wrapper } = mountSidebar()
    expect(wrapper.text()).toContain('AAPL')
  })

  it('hides CLOSED section when no closed campaigns', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const store = useTradeLogStore()
    store.campaigns = mockCampaigns.filter(c => c.status === 'OPEN')
    const wrapper = mount(Sidebar, { global: { plugins: [pinia], stubs: { RouterLink: routerLinkStub } } })
    expect(wrapper.text()).not.toContain('CLOSED')
  })
})

describe('active highlight', () => {
  it('applies campaign-item--active to current campaign', () => {
    routeParams.id = '1'
    const { wrapper } = mountSidebar()
    const spyItem = wrapper.findAll('.campaign-item').find(i => i.text().includes('SPY'))
    expect(spyItem?.classes()).toContain('campaign-item--active')
  })

  it('does not apply campaign-item--active to other campaigns', () => {
    routeParams.id = '1'
    const { wrapper } = mountSidebar()
    const nvdaItem = wrapper.findAll('.campaign-item').find(i => i.text().includes('NVDA'))
    expect(nvdaItem?.classes()).not.toContain('campaign-item--active')
  })

  it('no campaign highlighted when route has no id', () => {
    routeParams.id = undefined
    const { wrapper } = mountSidebar()
    expect(wrapper.findAll('.campaign-item--active')).toHaveLength(0)
  })
})
