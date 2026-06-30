import { describe, it, expect, vi, afterEach } from 'vitest'
import { getTradesForCampaign, saveTrade, saveBatchTrade } from './tradeService'
import type { TradeLeg } from '@/types/index'

const mockTrade: TradeLeg = {
  id: 1, tradeEntryId: 1, campaignId: 1,
  instrumentType: 'OPTION', action: 'STO', ticker: 'SPY',
  quantity: 5, price: 2.35, netCashFlow: 1175,
  optionType: 'PUT', strike: 480, expiry: '2024-12-20',
  tradedAt: '2024-11-01', strategyTag: 'CSP',
}

function mockFetch(body: unknown, ok = true) {
  const fn = vi.fn().mockResolvedValue({
    ok,
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)),
  })
  vi.stubGlobal('fetch', fn)
  return fn
}

afterEach(() => vi.restoreAllMocks())

describe('getTradesForCampaign', () => {
  it('calls GET /api/v1/trades?campaignId=1 and returns TradeLeg[]', async () => {
    const fn = mockFetch([mockTrade])
    const result = await getTradesForCampaign(1)
    expect(fn).toHaveBeenCalledWith('/api/v1/trades?campaignId=1')
    expect(result).toEqual([mockTrade])
  })

  it('throws on non-2xx response', async () => {
    mockFetch('Not found', false)
    await expect(getTradesForCampaign(1)).rejects.toThrow('Not found')
  })
})

describe('saveTrade', () => {
  it('calls POST /api/v1/trades with JSON body and returns TradeLeg', async () => {
    const fn = mockFetch(mockTrade)
    const req = { campaignId: 1, rawInput: 'STO 5 SPY 480C 12/20 @2.35', strategyTag: 'CSP', notes: '' }
    const result = await saveTrade(req)
    expect(fn).toHaveBeenCalledWith('/api/v1/trades', expect.objectContaining({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    }))
    expect(result).toEqual(mockTrade)
  })

  it('throws on non-2xx response', async () => {
    mockFetch('Bad request', false)
    await expect(saveTrade({ campaignId: 1, rawInput: 'bad' })).rejects.toThrow('Bad request')
  })
})

describe('saveBatchTrade', () => {
  const mockTrade2: TradeLeg = {
    id: 2, tradeEntryId: 1, campaignId: 1,
    instrumentType: 'OPTION', action: 'BTO', ticker: 'SPY',
    quantity: 5, price: 0.85, netCashFlow: -425,
    optionType: 'CALL', strike: 485, expiry: '2024-12-20',
    tradedAt: '2024-11-01', strategyTag: 'Bear Call Spread',
  }

  it('calls POST /api/v1/trades/batch with JSON body and returns TradeLeg[]', async () => {
    const fn = mockFetch([mockTrade, mockTrade2])
    const req = {
      campaignId: 1,
      rawInputs: ['STO 5 SPY 480C 12/20 @2.35', 'BTO 5 SPY 485C 12/20 @0.85'],
      strategyTag: 'Bear Call Spread',
      notes: '',
      tradedAt: '2024-11-01',
    }
    const result = await saveBatchTrade(req)
    expect(fn).toHaveBeenCalledWith('/api/v1/trades/batch', expect.objectContaining({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    }))
    expect(result).toEqual([mockTrade, mockTrade2])
  })

  it('throws on non-2xx response', async () => {
    mockFetch('Invalid leg: garbage', false)
    await expect(
      saveBatchTrade({ campaignId: 1, rawInputs: ['garbage'], strategyTag: '', notes: '', tradedAt: '' })
    ).rejects.toThrow('Invalid leg: garbage')
  })
})
