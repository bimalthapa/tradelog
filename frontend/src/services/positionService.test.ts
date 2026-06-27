import { describe, it, expect, vi, afterEach } from 'vitest'
import { getPositionsForCampaign } from './positionService'
import type { Position } from '@/types/index'

const mockPosition: Position = {
  id: 1, campaignId: 1, instrumentType: 'OPTION', ticker: 'SPY',
  optionType: 'PUT', strike: 480, expiry: '2024-12-20',
  openAction: 'STO', openQuantity: 5, avgPrice: 2.35,
  status: 'OPEN', openedAt: '2024-11-01',
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

describe('getPositionsForCampaign', () => {
  it('calls GET /api/v1/positions?campaignId=1 and returns Position[]', async () => {
    const fn = mockFetch([mockPosition])
    const result = await getPositionsForCampaign(1)
    expect(fn).toHaveBeenCalledWith('/api/v1/positions?campaignId=1')
    expect(result).toEqual([mockPosition])
  })

  it('throws on non-2xx response', async () => {
    mockFetch('Server error', false)
    await expect(getPositionsForCampaign(1)).rejects.toThrow('Server error')
  })
})
