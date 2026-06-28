// @vitest-environment happy-dom
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import CampaignDetailView from './CampaignDetailView.vue'
import type { Campaign, TradeLeg, Position, ParsedTrade } from '@/types/index'
import { MOCK_PRICES } from '@/types/index'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1' } }),
  RouterLink: { template: '<a><slot /></a>' },
}))

vi.mock('@/services/campaignService', () => ({
  getCampaigns:   vi.fn(),
  getCampaign:    vi.fn(),
  closeCampaign:  vi.fn(),
}))

vi.mock('@/services/tradeService', () => ({
  getTradesForCampaign: vi.fn(),
  saveTrade:            vi.fn(),
}))

vi.mock('@/services/positionService', () => ({
  getPositionsForCampaign: vi.fn(),
}))

import { getCampaign, closeCampaign } from '@/services/campaignService'
import { getTradesForCampaign, saveTrade } from '@/services/tradeService'
import { getPositionsForCampaign } from '@/services/positionService'

const mockCampaign: Campaign = {
  id: 1, ticker: 'NVDA', label: 'Wheel Strategy', status: 'OPEN',
  notes: 'Running the wheel on NVDA.', openedAt: '2024-01-01',
  netCashFlow: 3830, costBasis: 820.83, sharesHeld: 300, openPositionCount: 2,
}

const mockTrades: TradeLeg[] = [
  { id: 1, tradeEntryId: 1, campaignId: 1, instrumentType: 'OPTION', action: 'STO', ticker: 'NVDA', quantity: 5, price: 2.35, netCashFlow: 1175, optionType: 'PUT', strike: 480, expiry: '2024-12-20', tradedAt: '2024-11-01', strategyTag: 'CSP' },
  { id: 2, tradeEntryId: 2, campaignId: 1, instrumentType: 'OPTION', action: 'STO', ticker: 'NVDA', quantity: 3, price: 1.80, netCashFlow: 540, optionType: 'CALL', strike: 510, expiry: '2024-12-20', tradedAt: '2024-11-08', strategyTag: 'CC' },
]

const mockPositions: Position[] = [
  { id: 1, campaignId: 1, instrumentType: 'OPTION', ticker: 'NVDA', optionType: 'PUT', strike: 480, expiry: '2024-12-20', openAction: 'STO', openQuantity: 5, avgPrice: 2.35, status: 'OPEN', openedAt: '2024-11-01' },
]

const mockParsedTrade: ParsedTrade = {
  action: 'STO', qty: 5, ticker: 'SPY', instrumentType: 'OPTION',
  optionType: 'PUT', strike: 480, expiry: '2024-12-20',
  price: 2.35, cashFlow: 1175, strategy: 'CSP', valid: true,
}

function setupMocks() {
  vi.mocked(getCampaign).mockResolvedValue(mockCampaign)
  vi.mocked(getTradesForCampaign).mockResolvedValue(mockTrades)
  vi.mocked(getPositionsForCampaign).mockResolvedValue(mockPositions)
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

beforeEach(() => vi.clearAllMocks())

// ── Loading & error states ──────────────────────────────────────────────────

describe('loading state', () => {
  it('shows "Loading…" while fetches are in-flight', async () => {
    let resolve!: (v: Campaign) => void
    vi.mocked(getCampaign).mockReturnValue(new Promise(r => { resolve = r }))
    vi.mocked(getTradesForCampaign).mockResolvedValue([])
    vi.mocked(getPositionsForCampaign).mockResolvedValue([])
    const wrapper = mountView()
    await nextTick()
    expect(wrapper.find('.state-msg').text()).toBe('Loading…')
    resolve(mockCampaign)
    await flushPromises()
  })
})

describe('not found state', () => {
  it('shows "Campaign not found" when getCampaign fails', async () => {
    vi.mocked(getCampaign).mockRejectedValue(new Error('Not found'))
    vi.mocked(getTradesForCampaign).mockResolvedValue([])
    vi.mocked(getPositionsForCampaign).mockResolvedValue([])
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.state-msg--error').exists()).toBe(true)
    expect(wrapper.find('.state-msg--error').text()).toBe('Campaign not found')
  })
})

// ── Header content ──────────────────────────────────────────────────────────

describe('header content', () => {
  beforeEach(() => setupMocks())

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
    vi.mocked(getCampaign).mockResolvedValue({ ...mockCampaign, label: undefined })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.campaign-label').exists()).toBe(false)
  })

  it('renders ACTIVE badge for OPEN campaign', async () => {
    const wrapper = mountView()
    await flushPromises()
    const badge = wrapper.find('.badge')
    expect(badge.text()).toBe('ACTIVE')
    expect(badge.classes()).toContain('badge-open')
  })

  it('renders CLOSED badge for CLOSED campaign', async () => {
    vi.mocked(getCampaign).mockResolvedValue({ ...mockCampaign, status: 'CLOSED' as const })
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
    vi.mocked(getCampaign).mockResolvedValue({ ...mockCampaign, notes: undefined })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.notes').exists()).toBe(false)
  })
})

// ── Stat strip ──────────────────────────────────────────────────────────────

describe('stat strip', () => {
  beforeEach(() => setupMocks())

  async function getStatValues(wrapper: ReturnType<typeof mountView>) {
    await flushPromises()
    return wrapper.findAll('.stat-value')
  }

  it('shows cost basis formatted as currency (index 0)', async () => {
    const stats = await getStatValues(mountView())
    expect(stats[0]!.text()).toBe('$820.83')
  })

  it('shows shares as plain integer (index 1)', async () => {
    const stats = await getStatValues(mountView())
    expect(stats[1]!.text()).toBe('300')
  })

  it('shows net cash flow with + prefix when positive (index 2)', async () => {
    const stats = await getStatValues(mountView())
    expect(stats[2]!.text()).toBe('+$3,830.00')
  })

  it('applies profit class to net cash when positive (index 2)', async () => {
    const stats = await getStatValues(mountView())
    expect(stats[2]!.classes()).toContain('profit')
  })

  it('applies loss class to net cash when negative (index 2)', async () => {
    vi.mocked(getCampaign).mockResolvedValue({ ...mockCampaign, netCashFlow: -500 })
    const stats = await getStatValues(mountView())
    expect(stats[2]!.classes()).toContain('loss')
  })

  it('shows unrealized P&L from MOCK_PRICES and costBasis (index 3)', async () => {
    const stats = await getStatValues(mountView())
    const unrlz = 300 * ((MOCK_PRICES['NVDA'] ?? 0) - 820.83)
    const expected = '+' + unrlz.toLocaleString('en-US', { style: 'currency', currency: 'USD' })
    expect(stats[3]!.text()).toBe(expected)
  })

  it('shows current price from MOCK_PRICES (index 4)', async () => {
    const stats = await getStatValues(mountView())
    expect(stats[4]!.text()).toBe('$875.40')
  })

  it('shows — for cost basis when absent (index 0)', async () => {
    vi.mocked(getCampaign).mockResolvedValue({ ...mockCampaign, costBasis: undefined, sharesHeld: undefined })
    const stats = await getStatValues(mountView())
    expect(stats[0]!.text()).toBe('—')
  })
})

// ── Trade entry bar ─────────────────────────────────────────────────────────

describe('trade entry bar', () => {
  it('renders TradeEntryBar when campaign is found', async () => {
    setupMocks()
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.trade-entry').exists()).toBe(true)
  })
})

// ── Open Positions section ──────────────────────────────────────────────────

describe('open positions section', () => {
  beforeEach(() => setupMocks())

  it('renders position rows from store', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.findAll('.pos-row')).toHaveLength(1)
  })

  it('hides positions section when store.positions is empty', async () => {
    vi.mocked(getPositionsForCampaign).mockResolvedValue([])
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.positions-section').exists()).toBe(false)
  })
})

// ── Trade History section ───────────────────────────────────────────────────

describe('trade history section', () => {
  beforeEach(() => setupMocks())

  it('renders trade rows from store', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.findAll('.trade-row')).toHaveLength(2)
  })

  it('renders net cash flow footer', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.ncf-footer').exists()).toBe(true)
  })

  it('net cash flow is sum of store.trades netCashFlow', async () => {
    const wrapper = mountView()
    await flushPromises()
    // 1175 + 540 = 1715
    expect(wrapper.find('.ncf-value').text()).toContain('1,715')
  })
})

// ── ConfirmPanel orchestration ──────────────────────────────────────────────

describe('confirm panel', () => {
  beforeEach(() => setupMocks())

  it('does not render ConfirmPanel before a trade is parsed', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.confirm-panel').exists()).toBe(false)
  })

  it('shows ConfirmPanel when TradeEntryBar emits parsed', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.findComponent({ name: 'TradeEntryBar' }).vm.$emit('parsed', {
      trade: mockParsedTrade, rawInput: 'STO 5 SPY 480C 12/20 @2.35',
    })
    await nextTick()
    expect(wrapper.findComponent({ name: 'ConfirmPanel' }).exists()).toBe(true)
  })

  it('hides ConfirmPanel when panel emits cancel', async () => {
    const wrapper = mountView()
    await flushPromises()
    await wrapper.findComponent({ name: 'TradeEntryBar' }).vm.$emit('parsed', {
      trade: mockParsedTrade, rawInput: 'STO 5 SPY 480C 12/20 @2.35',
    })
    await nextTick()
    await wrapper.findComponent({ name: 'ConfirmPanel' }).vm.$emit('cancel')
    await nextTick()
    expect(wrapper.findComponent({ name: 'ConfirmPanel' }).exists()).toBe(false)
  })

  it('calls saveTrade with correct args when panel emits save', async () => {
    const savedTrade = mockTrades[0]!
    vi.mocked(saveTrade).mockResolvedValue(savedTrade)
    const wrapper = mountView()
    await flushPromises()
    await wrapper.findComponent({ name: 'TradeEntryBar' }).vm.$emit('parsed', {
      trade: mockParsedTrade, rawInput: 'STO 5 SPY 480C 12/20 @2.35',
    })
    await nextTick()
    await wrapper.findComponent({ name: 'ConfirmPanel' }).vm.$emit('save', { strategyTag: 'CSP', notes: 'my note' })
    await flushPromises()
    expect(saveTrade).toHaveBeenCalledWith({
      campaignId: 1,
      rawInput: 'STO 5 SPY 480C 12/20 @2.35',
      strategyTag: 'CSP',
      notes: 'my note',
    })
  })

  it('closes panel after successful save', async () => {
    vi.mocked(saveTrade).mockResolvedValue(mockTrades[0]!)
    const wrapper = mountView()
    await flushPromises()
    await wrapper.findComponent({ name: 'TradeEntryBar' }).vm.$emit('parsed', {
      trade: mockParsedTrade, rawInput: 'STO 5 SPY 480C 12/20 @2.35',
    })
    await nextTick()
    await wrapper.findComponent({ name: 'ConfirmPanel' }).vm.$emit('save', { strategyTag: 'CSP', notes: '' })
    await flushPromises()
    expect(wrapper.findComponent({ name: 'ConfirmPanel' }).exists()).toBe(false)
  })

  it('re-fetches data after successful save', async () => {
    vi.mocked(saveTrade).mockResolvedValue(mockTrades[0]!)
    const wrapper = mountView()
    await flushPromises()
    await wrapper.findComponent({ name: 'TradeEntryBar' }).vm.$emit('parsed', {
      trade: mockParsedTrade, rawInput: 'STO 5 SPY 480C 12/20 @2.35',
    })
    await nextTick()
    await wrapper.findComponent({ name: 'ConfirmPanel' }).vm.$emit('save', { strategyTag: 'CSP', notes: '' })
    await flushPromises()
    expect(getCampaign).toHaveBeenCalledTimes(2)
    expect(getTradesForCampaign).toHaveBeenCalledTimes(2)
    expect(getPositionsForCampaign).toHaveBeenCalledTimes(2)
  })

  it('shows save error when saveTrade rejects', async () => {
    vi.mocked(saveTrade).mockRejectedValue(new Error('Server error'))
    const wrapper = mountView()
    await flushPromises()
    await wrapper.findComponent({ name: 'TradeEntryBar' }).vm.$emit('parsed', {
      trade: mockParsedTrade, rawInput: 'STO 5 SPY 480C 12/20 @2.35',
    })
    await nextTick()
    await wrapper.findComponent({ name: 'ConfirmPanel' }).vm.$emit('save', { strategyTag: 'CSP', notes: '' })
    await flushPromises()
    // Panel should still be visible
    expect(wrapper.findComponent({ name: 'ConfirmPanel' }).exists()).toBe(true)
    // Error prop should be passed — check via the ConfirmPanel's saveError prop
    const panel = wrapper.findComponent({ name: 'ConfirmPanel' })
    expect(panel.props('saveError')).toBe('Server error')
  })
})

// ── Close Campaign ──────────────────────────────────────────────────────────

describe('close campaign button', () => {
  it('shows close campaign button for OPEN campaign', async () => {
    setupMocks()
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.btn-close-campaign').exists()).toBe(true)
  })

  it('hides close campaign button for CLOSED campaign', async () => {
    vi.mocked(getCampaign).mockResolvedValue({ ...mockCampaign, status: 'CLOSED' as const })
    vi.mocked(getTradesForCampaign).mockResolvedValue([])
    vi.mocked(getPositionsForCampaign).mockResolvedValue([])
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.btn-close-campaign').exists()).toBe(false)
  })

  it('calls closeCampaign and re-fetches campaign on click', async () => {
    setupMocks()
    vi.mocked(closeCampaign).mockResolvedValue({ ...mockCampaign, status: 'CLOSED' as const })
    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('.btn-close-campaign').trigger('click')
    await flushPromises()
    expect(closeCampaign).toHaveBeenCalledWith(1)
    expect(getCampaign).toHaveBeenCalledTimes(2)
  })
})

// ── Trade History empty state ────────────────────────────────────────────────

describe('trade history empty state', () => {
  it('shows "No trades recorded yet" when trades list is empty', async () => {
    vi.mocked(getCampaign).mockResolvedValue(mockCampaign)
    vi.mocked(getTradesForCampaign).mockResolvedValue([])
    vi.mocked(getPositionsForCampaign).mockResolvedValue([])
    const wrapper = mountView()
    await flushPromises()
    const cell = wrapper.find('.empty-cell')
    expect(cell.exists()).toBe(true)
    expect(cell.text()).toBe('No trades recorded yet')
  })

  it('does not show empty cell when trades exist', async () => {
    setupMocks()
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.empty-cell').exists()).toBe(false)
  })
})
