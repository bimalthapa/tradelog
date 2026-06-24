import { describe, it, expect, vi, afterEach } from 'vitest'
import { getCampaigns, getCampaign, createCampaign, closeCampaign } from './campaignService'
import type { Campaign } from '@/types/index'

const mockCampaign: Campaign = {
  id: 1, ticker: 'SPY', status: 'OPEN', openedAt: '2026-01-01',
  netCashFlow: 500, openPositionCount: 1,
}

function mockFetch(body: unknown, ok = true): ReturnType<typeof vi.fn> {
  const fn = vi.fn().mockResolvedValue({
    ok,
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)),
  })
  vi.stubGlobal('fetch', fn)
  return fn
}

afterEach(() => vi.restoreAllMocks())

describe('getCampaigns', () => {
  it('calls GET /api/v1/campaigns and returns Campaign[]', async () => {
    const fn = mockFetch([mockCampaign])
    const result = await getCampaigns()
    expect(fn).toHaveBeenCalledWith('/api/v1/campaigns')
    expect(result).toEqual([mockCampaign])
  })

  it('throws on non-2xx response', async () => {
    mockFetch('Server error', false)
    await expect(getCampaigns()).rejects.toThrow('Server error')
  })
})

describe('getCampaign', () => {
  it('calls GET /api/v1/campaigns/:id', async () => {
    const fn = mockFetch(mockCampaign)
    const result = await getCampaign(1)
    expect(fn).toHaveBeenCalledWith('/api/v1/campaigns/1')
    expect(result).toEqual(mockCampaign)
  })
})

describe('createCampaign', () => {
  it('calls POST /api/v1/campaigns with JSON body', async () => {
    const fn = mockFetch({ ...mockCampaign, ticker: 'AAPL' })
    const req = { ticker: 'AAPL', openedAt: '2026-06-24' }
    const result = await createCampaign(req)
    expect(fn).toHaveBeenCalledWith('/api/v1/campaigns', expect.objectContaining({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    }))
    expect(result.ticker).toBe('AAPL')
  })
})

describe('closeCampaign', () => {
  it('calls PATCH /api/v1/campaigns/:id/close', async () => {
    const fn = mockFetch({ ...mockCampaign, status: 'CLOSED' })
    const result = await closeCampaign(1)
    expect(fn).toHaveBeenCalledWith('/api/v1/campaigns/1/close', expect.objectContaining({
      method: 'PATCH',
    }))
    expect(result.status).toBe('CLOSED')
  })
})
