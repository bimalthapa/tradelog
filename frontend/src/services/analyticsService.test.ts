import { describe, it, expect, vi, afterEach } from 'vitest'
import { getAnalyticsSummary, getPnlByStrategy, getCumulativeData } from './analyticsService'
import type { AnalyticsSummary, PnlItem, CumulativeData } from '@/types/index'

const mockSummary: AnalyticsSummary = {
  totalPremium: 3240, netOptionsPnl: 2180,
  campaignWinRate: 0.67, tradeWinRate: 0.80, totalTrades: 15,
}

const mockStrategy: PnlItem[] = [
  { label: 'CSP', value: 1540 },
  { label: 'CC',  value:  700 },
]

const mockCumulative: CumulativeData = {
  premium:    [{ month: '2026-01', value: 450 }],
  optionsPnl: [{ month: '2026-01', value: 320 }],
}

function mockFetch(body: unknown, ok = true) {
  const fn = vi.fn().mockResolvedValue({
    ok,
    json:  () => Promise.resolve(body),
    text:  () => Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)),
  })
  vi.stubGlobal('fetch', fn)
  return fn
}

afterEach(() => vi.restoreAllMocks())

describe('getAnalyticsSummary', () => {
  it('calls GET /api/v1/analytics/summary', async () => {
    const fn = mockFetch(mockSummary)
    const result = await getAnalyticsSummary()
    expect(fn).toHaveBeenCalledWith('/api/v1/analytics/summary')
    expect(result).toEqual(mockSummary)
  })

  it('throws on non-2xx', async () => {
    mockFetch('error', false)
    await expect(getAnalyticsSummary()).rejects.toThrow('error')
  })
})

describe('getPnlByStrategy', () => {
  it('calls GET /api/v1/analytics/pnl-by-strategy', async () => {
    const fn = mockFetch(mockStrategy)
    const result = await getPnlByStrategy()
    expect(fn).toHaveBeenCalledWith('/api/v1/analytics/pnl-by-strategy')
    expect(result).toEqual(mockStrategy)
  })
})

describe('getCumulativeData', () => {
  it('calls GET /api/v1/analytics/cumulative', async () => {
    const fn = mockFetch(mockCumulative)
    const result = await getCumulativeData()
    expect(fn).toHaveBeenCalledWith('/api/v1/analytics/cumulative')
    expect(result.premium).toHaveLength(1)
    expect(result.optionsPnl).toHaveLength(1)
  })
})
