import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTradeLogStore } from './tradeLog'
import { getCampaigns } from '@/services/campaignService'
import type { Campaign } from '@/types/index'

vi.mock('@/services/campaignService', () => ({
  getCampaigns: vi.fn(),
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
