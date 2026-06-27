import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTradeLogStore } from './tradeLog'
import { getCampaigns, getCampaign } from '@/services/campaignService'
import { getTradesForCampaign } from '@/services/tradeService'
import { getPositionsForCampaign } from '@/services/positionService'
import type { Campaign, TradeLeg, Position } from '@/types/index'

vi.mock('@/services/campaignService', () => ({
  getCampaigns: vi.fn(),
  getCampaign: vi.fn(),
}))

vi.mock('@/services/tradeService', () => ({
  getTradesForCampaign: vi.fn(),
}))

vi.mock('@/services/positionService', () => ({
  getPositionsForCampaign: vi.fn(),
}))

const mockCampaigns: Campaign[] = [
  { id: 1, ticker: 'SPY',  status: 'OPEN',   openedAt: '2026-01-01', netCashFlow:  500, openPositionCount: 1 },
  { id: 2, ticker: 'AAPL', status: 'CLOSED', openedAt: '2025-06-01', netCashFlow:  200, openPositionCount: 0 },
]

beforeEach(() => {
  setActivePinia(createPinia())
  vi.restoreAllMocks()
})

describe('fetchCampaigns', () => {
  it('populates campaigns and clears loading/error on success', async () => {
    vi.mocked(getCampaigns).mockResolvedValue(mockCampaigns)
    const store = useTradeLogStore()
    await store.fetchCampaigns()
    expect(store.campaigns).toEqual(mockCampaigns)
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('sets error string and clears loading on failure', async () => {
    vi.mocked(getCampaigns).mockRejectedValue(new Error('Network error'))
    const store = useTradeLogStore()
    await store.fetchCampaigns()
    expect(store.error).toBe('Network error')
    expect(store.campaigns).toEqual([])
    expect(store.loading).toBe(false)
  })

  it('sets loading true while fetch is in-flight', async () => {
    let resolve!: (v: Campaign[]) => void
    vi.mocked(getCampaigns).mockReturnValue(new Promise(r => { resolve = r }))
    const store = useTradeLogStore()
    const p = store.fetchCampaigns()
    expect(store.loading).toBe(true)
    resolve(mockCampaigns)
    await p
    expect(store.loading).toBe(false)
  })
})

describe('activeCampaigns', () => {
  it('returns only OPEN campaigns', async () => {
    vi.mocked(getCampaigns).mockResolvedValue(mockCampaigns)
    const store = useTradeLogStore()
    await store.fetchCampaigns()
    expect(store.activeCampaigns).toEqual([mockCampaigns[0]])
  })
})

describe('closedCampaigns', () => {
  it('returns only CLOSED campaigns', async () => {
    vi.mocked(getCampaigns).mockResolvedValue(mockCampaigns)
    const store = useTradeLogStore()
    await store.fetchCampaigns()
    expect(store.closedCampaigns).toEqual([mockCampaigns[1]])
  })
})

const mockCampaign1: Campaign = {
  id: 1, ticker: 'SPY', status: 'OPEN', openedAt: '2026-01-01',
  netCashFlow: 500, openPositionCount: 1,
}

const mockTrade: TradeLeg = {
  id: 1, tradeEntryId: 1, campaignId: 1, instrumentType: 'OPTION',
  action: 'STO', ticker: 'SPY', quantity: 5, price: 2.35, netCashFlow: 1175,
  optionType: 'PUT', strike: 480, expiry: '2024-12-20', tradedAt: '2024-11-01',
}

const mockPosition: Position = {
  id: 1, campaignId: 1, instrumentType: 'OPTION', ticker: 'SPY',
  optionType: 'PUT', strike: 480, expiry: '2024-12-20',
  openAction: 'STO', openQuantity: 5, avgPrice: 2.35,
  status: 'OPEN', openedAt: '2024-11-01',
}

describe('fetchCampaign', () => {
  it('sets currentCampaign on success', async () => {
    vi.mocked(getCampaign).mockResolvedValue(mockCampaign1)
    const store = useTradeLogStore()
    await store.fetchCampaign(1)
    expect(store.currentCampaign).toEqual(mockCampaign1)
  })

  it('sets currentCampaign to null and sets error on failure', async () => {
    vi.mocked(getCampaign).mockRejectedValue(new Error('Not found'))
    const store = useTradeLogStore()
    await store.fetchCampaign(999)
    expect(store.currentCampaign).toBeNull()
    expect(store.error).toBe('Not found')
  })

  it('clears stale error when next call succeeds', async () => {
    vi.mocked(getCampaign).mockRejectedValue(new Error('Not found'))
    const store = useTradeLogStore()
    await store.fetchCampaign(999)
    expect(store.error).toBe('Not found')
    vi.mocked(getCampaign).mockResolvedValue(mockCampaign1)
    await store.fetchCampaign(1)
    expect(store.error).toBeNull()
  })
})

describe('fetchTrades', () => {
  it('sets trades on success', async () => {
    vi.mocked(getTradesForCampaign).mockResolvedValue([mockTrade])
    const store = useTradeLogStore()
    await store.fetchTrades(1)
    expect(store.trades).toEqual([mockTrade])
  })

  it('sets error on failure', async () => {
    vi.mocked(getTradesForCampaign).mockRejectedValue(new Error('Network error'))
    const store = useTradeLogStore()
    await store.fetchTrades(1)
    expect(store.error).toBe('Network error')
  })

  it('clears stale error when next call succeeds', async () => {
    vi.mocked(getTradesForCampaign).mockRejectedValue(new Error('Network error'))
    const store = useTradeLogStore()
    await store.fetchTrades(1)
    expect(store.error).toBe('Network error')
    vi.mocked(getTradesForCampaign).mockResolvedValue([mockTrade])
    await store.fetchTrades(1)
    expect(store.error).toBeNull()
  })
})

describe('fetchPositions', () => {
  it('sets positions on success', async () => {
    vi.mocked(getPositionsForCampaign).mockResolvedValue([mockPosition])
    const store = useTradeLogStore()
    await store.fetchPositions(1)
    expect(store.positions).toEqual([mockPosition])
  })

  it('sets error on failure', async () => {
    vi.mocked(getPositionsForCampaign).mockRejectedValue(new Error('Network error'))
    const store = useTradeLogStore()
    await store.fetchPositions(1)
    expect(store.error).toBe('Network error')
  })

  it('clears stale error when next call succeeds', async () => {
    vi.mocked(getPositionsForCampaign).mockRejectedValue(new Error('Network error'))
    const store = useTradeLogStore()
    await store.fetchPositions(1)
    expect(store.error).toBe('Network error')
    vi.mocked(getPositionsForCampaign).mockResolvedValue([mockPosition])
    await store.fetchPositions(1)
    expect(store.error).toBeNull()
  })
})
