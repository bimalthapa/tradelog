// @vitest-environment happy-dom
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePriceStore } from '@/stores/priceStore'

vi.mock('@/services/priceService', () => ({
  fetchPrices: vi.fn(),
}))

import { fetchPrices } from '@/services/priceService'

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
})

// ── ensureTicker ────────────────────────────────────────────────────────────

describe('ensureTicker', () => {
  it('fetches prices when a new ticker is added', async () => {
    vi.mocked(fetchPrices).mockResolvedValue(new Map([['AAPL', 185.92]]))
    const store = usePriceStore()

    await store.ensureTicker('AAPL')

    expect(fetchPrices).toHaveBeenCalledWith(['AAPL'])
    expect(store.getPrice('AAPL')).toBe(185.92)
  })

  it('does not fetch again when ticker is already tracked', async () => {
    vi.mocked(fetchPrices).mockResolvedValue(new Map([['AAPL', 185.92]]))
    const store = usePriceStore()

    await store.ensureTicker('AAPL')
    await store.ensureTicker('AAPL')

    expect(fetchPrices).toHaveBeenCalledTimes(1)
  })

  it('batch includes all tracked tickers when a second one is added', async () => {
    vi.mocked(fetchPrices)
      .mockResolvedValueOnce(new Map([['AAPL', 185.92]]))
      .mockResolvedValueOnce(new Map([['AAPL', 185.92], ['NVDA', 875.40]]))
    const store = usePriceStore()

    await store.ensureTicker('AAPL')
    await store.ensureTicker('NVDA')

    expect(fetchPrices).toHaveBeenLastCalledWith(expect.arrayContaining(['AAPL', 'NVDA']))
    expect(store.getPrice('NVDA')).toBe(875.40)
  })
})

// ── fetchAll ────────────────────────────────────────────────────────────────

describe('fetchAll', () => {
  it('merges returned prices into store state', async () => {
    vi.mocked(fetchPrices)
      .mockResolvedValueOnce(new Map([['AAPL', 185.92]]))
      .mockResolvedValueOnce(new Map([['AAPL', 190.00]]))
    const store = usePriceStore()
    await store.ensureTicker('AAPL')

    await store.fetchAll()

    expect(store.getPrice('AAPL')).toBe(190.00)
  })

  it('stores null for unavailable prices', async () => {
    vi.mocked(fetchPrices).mockResolvedValue(new Map([['UNKN', null]]))
    const store = usePriceStore()
    await store.ensureTicker('UNKN')

    expect(store.getPrice('UNKN')).toBeNull()
  })

  it('does nothing when no tickers are tracked', async () => {
    const store = usePriceStore()

    await store.fetchAll()

    expect(fetchPrices).not.toHaveBeenCalled()
  })
})

// ── getPrice ────────────────────────────────────────────────────────────────

describe('getPrice', () => {
  it('returns null for untracked ticker', () => {
    const store = usePriceStore()
    expect(store.getPrice('ZZZY')).toBeNull()
  })
})

// ── polling ─────────────────────────────────────────────────────────────────

describe('polling', () => {
  afterEach(() => vi.useRealTimers())

  it('calls fetchAll after 5-minute interval', async () => {
    vi.useFakeTimers()
    vi.mocked(fetchPrices)
      .mockResolvedValueOnce(new Map([['AAPL', 185.92]]))
      .mockResolvedValueOnce(new Map([['AAPL', 186.00]]))
    const store = usePriceStore()
    await store.ensureTicker('AAPL')

    store.startPolling()
    await vi.advanceTimersByTimeAsync(5 * 60 * 1000)

    expect(fetchPrices).toHaveBeenCalledTimes(2)
    store.stopPolling()
  })

  it('stopPolling prevents further fetches', async () => {
    vi.useFakeTimers()
    vi.mocked(fetchPrices).mockResolvedValue(new Map([['AAPL', 185.92]]))
    const store = usePriceStore()
    await store.ensureTicker('AAPL')
    const callsAfterEnsure = vi.mocked(fetchPrices).mock.calls.length

    store.startPolling()
    store.stopPolling()
    await vi.advanceTimersByTimeAsync(10 * 60 * 1000)

    expect(fetchPrices).toHaveBeenCalledTimes(callsAfterEnsure)
  })

  it('double startPolling does not leak a second interval', async () => {
    vi.useFakeTimers()
    vi.mocked(fetchPrices)
      .mockResolvedValueOnce(new Map([['AAPL', 185.92]]))
      .mockResolvedValueOnce(new Map([['AAPL', 186.00]]))
    const store = usePriceStore()
    await store.ensureTicker('AAPL')
    const callsAfterEnsure = vi.mocked(fetchPrices).mock.calls.length

    store.startPolling()
    store.startPolling()  // second call must not add a second interval
    await vi.advanceTimersByTimeAsync(5 * 60 * 1000)

    // only one interval should have fired (not two)
    expect(fetchPrices).toHaveBeenCalledTimes(callsAfterEnsure + 1)
    store.stopPolling()
  })
})
