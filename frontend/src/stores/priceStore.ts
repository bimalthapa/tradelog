import { ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchPrices } from '@/services/priceService'

export const usePriceStore = defineStore('price', () => {
  const prices         = ref<Map<string, number | null>>(new Map())
  const loading        = ref(false)
  const trackedTickers = ref<Set<string>>(new Set())
  let   pollingHandle: ReturnType<typeof setInterval> | null = null

  async function fetchAll(): Promise<void> {
    if (trackedTickers.value.size === 0) return
    loading.value = true
    try {
      const result = await fetchPrices([...trackedTickers.value])
      result.forEach((price, ticker) => prices.value.set(ticker, price))
    } finally {
      loading.value = false
    }
  }

  async function ensureTicker(ticker: string): Promise<void> {
    if (trackedTickers.value.has(ticker)) return
    trackedTickers.value.add(ticker)
    await fetchAll()
  }

  function getPrice(ticker: string): number | null {
    return prices.value.get(ticker) ?? null
  }

  function startPolling(): void {
    stopPolling()
    pollingHandle = setInterval(fetchAll, 5 * 60 * 1000)
  }

  function stopPolling(): void {
    if (pollingHandle !== null) {
      clearInterval(pollingHandle)
      pollingHandle = null
    }
  }

  return { prices, loading, trackedTickers, ensureTicker, fetchAll, getPrice, startPolling, stopPolling }
})
