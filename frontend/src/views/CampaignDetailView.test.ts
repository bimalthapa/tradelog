// @vitest-environment happy-dom
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import CampaignDetailView from './CampaignDetailView.vue'
import type { Campaign } from '@/types/index'
import { MOCK_PRICES } from '@/types/index'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1' } }),
  RouterLink: { template: '<a><slot /></a>' },
}))

vi.mock('@/services/campaignService', () => ({
  getCampaigns: vi.fn(),
}))

import { getCampaigns } from '@/services/campaignService'

const mockCampaign: Campaign = {
  id: 1,
  ticker: 'NVDA',
  label: 'Wheel Strategy',
  status: 'OPEN',
  notes: 'Running the wheel on NVDA.',
  openedAt: '2024-01-01',
  netCashFlow: 3830,
  costBasis: 820.83,
  sharesHeld: 300,
  openPositionCount: 2,
}

function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(CampaignDetailView, {
    global: {
      plugins: [pinia],
      stubs: { RouterLink: { template: '<a><slot /></a>' } },
    },
  })
}

beforeEach(() => {
  vi.clearAllMocks()
})

// ── Loading & error states ──────────────────────────────────────────────────

describe('loading state', () => {
  it('shows "Loading…" while fetch is in-flight', async () => {
    let resolve!: (v: Campaign[]) => void
    vi.mocked(getCampaigns).mockReturnValue(new Promise(r => { resolve = r }))
    const wrapper = mountView()
    await nextTick()
    expect(wrapper.find('.state-msg').text()).toBe('Loading…')
    resolve([mockCampaign])
    await flushPromises()
  })
})

describe('not found state', () => {
  it('shows "Campaign not found" when id does not match any campaign', async () => {
    vi.mocked(getCampaigns).mockResolvedValue([{ ...mockCampaign, id: 99 }])
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.state-msg--error').exists()).toBe(true)
    expect(wrapper.find('.state-msg--error').text()).toBe('Campaign not found')
  })
})

// ── Header content ──────────────────────────────────────────────────────────

describe('header content', () => {
  beforeEach(() => {
    vi.mocked(getCampaigns).mockResolvedValue([mockCampaign])
  })

  it('renders breadcrumb with campaign ticker', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.breadcrumb-ticker').text()).toBe('NVDA')
  })

  it('renders large ticker in title row', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.ticker').text()).toBe('NVDA')
  })

  it('renders label when present', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.campaign-label').text()).toBe('Wheel Strategy')
  })

  it('hides label when absent', async () => {
    vi.mocked(getCampaigns).mockResolvedValue([{ ...mockCampaign, label: undefined }])
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.campaign-label').exists()).toBe(false)
  })

  it('renders ACTIVE badge with badge-open class for OPEN campaign', async () => {
    const wrapper = mountView()
    await flushPromises()
    const badge = wrapper.find('.badge')
    expect(badge.text()).toBe('ACTIVE')
    expect(badge.classes()).toContain('badge-open')
  })

  it('renders CLOSED badge with badge-closed class for CLOSED campaign', async () => {
    vi.mocked(getCampaigns).mockResolvedValue([{ ...mockCampaign, status: 'CLOSED' as const }])
    const wrapper = mountView()
    await flushPromises()
    const badge = wrapper.find('.badge')
    expect(badge.text()).toBe('CLOSED')
    expect(badge.classes()).toContain('badge-closed')
  })

  it('renders notes when present', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.notes').text()).toBe('Running the wheel on NVDA.')
  })

  it('hides notes when absent', async () => {
    vi.mocked(getCampaigns).mockResolvedValue([{ ...mockCampaign, notes: undefined }])
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.notes').exists()).toBe(false)
  })
})

// ── Stat strip ──────────────────────────────────────────────────────────────

describe('stat strip', () => {
  async function getStatValues(wrapper: ReturnType<typeof mountView>) {
    await flushPromises()
    return wrapper.findAll('.stat-value')
  }

  beforeEach(() => {
    vi.mocked(getCampaigns).mockResolvedValue([mockCampaign])
  })

  it('shows cost basis formatted as currency (index 0)', async () => {
    const wrapper = mountView()
    const stats = await getStatValues(wrapper)
    expect(stats[0]!.text()).toBe('$820.83')
  })

  it('shows shares as plain integer (index 1)', async () => {
    const wrapper = mountView()
    const stats = await getStatValues(wrapper)
    expect(stats[1]!.text()).toBe('300')
  })

  it('shows net cash flow with + prefix when positive (index 2)', async () => {
    const wrapper = mountView()
    const stats = await getStatValues(wrapper)
    expect(stats[2]!.text()).toBe('+$3,830.00')
  })

  it('applies profit class to net cash when positive (index 2)', async () => {
    const wrapper = mountView()
    const stats = await getStatValues(wrapper)
    expect(stats[2]!.classes()).toContain('profit')
  })

  it('applies loss class to net cash when negative (index 2)', async () => {
    vi.mocked(getCampaigns).mockResolvedValue([{ ...mockCampaign, netCashFlow: -500 }])
    const wrapper = mountView()
    const stats = await getStatValues(wrapper)
    expect(stats[2]!.classes()).toContain('loss')
  })

  it('shows unrealized P&L computed from MOCK_PRICES and costBasis (index 3)', async () => {
    const wrapper = mountView()
    const stats = await getStatValues(wrapper)
    // mirrors the component formula exactly to avoid floating-point divergence
    const unrlz = 300 * ((MOCK_PRICES['NVDA'] ?? 0) - 820.83)
    const expected = '+' + unrlz.toLocaleString('en-US', { style: 'currency', currency: 'USD' })
    expect(stats[3]!.text()).toBe(expected)
  })

  it('shows current price from MOCK_PRICES (index 4)', async () => {
    const wrapper = mountView()
    const stats = await getStatValues(wrapper)
    expect(stats[4]!.text()).toBe('$875.40')
  })

  it('shows — for cost basis when absent (index 0)', async () => {
    vi.mocked(getCampaigns).mockResolvedValue([
      { ...mockCampaign, costBasis: undefined, sharesHeld: undefined },
    ])
    const wrapper = mountView()
    const stats = await getStatValues(wrapper)
    expect(stats[0]!.text()).toBe('—')
  })

  it('shows — for curr price when ticker not in MOCK_PRICES (index 4)', async () => {
    vi.mocked(getCampaigns).mockResolvedValue([{ ...mockCampaign, ticker: 'UNKNOWN' }])
    const wrapper = mountView()
    const stats = await getStatValues(wrapper)
    expect(stats[4]!.text()).toBe('—')
  })
})
