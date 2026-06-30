interface PriceApiResponse {
  ticker: string
  price: number | null
  source: string
  fetchedAt: string
}

export async function fetchPrices(tickers: string[]): Promise<Map<string, number | null>> {
  try {
    const res = await fetch('/api/v1/prices/batch', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tickers }),
    })
    if (!res.ok) return new Map(tickers.map(t => [t, null]))
    const data = await res.json() as Record<string, PriceApiResponse>
    return new Map(Object.entries(data).map(([ticker, r]) => [ticker, r.price]))
  } catch {
    return new Map(tickers.map(t => [t, null]))
  }
}
