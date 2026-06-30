import type { TradeLeg, SaveTradeRequest, UpdateTradeRequest } from '@/types/index'

const BASE = '/api/v1/trades'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = init ? await fetch(url, init) : await fetch(url)
  if (!res.ok) throw new Error(await res.text())
  return res.json() as Promise<T>
}

export async function getTradesForCampaign(campaignId: number): Promise<TradeLeg[]> {
  return request<TradeLeg[]>(`${BASE}?campaignId=${campaignId}`)
}

export async function saveTrade(req: SaveTradeRequest): Promise<TradeLeg> {
  return request<TradeLeg>(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
}

export async function updateTrade(id: number, req: UpdateTradeRequest): Promise<TradeLeg> {
  return request<TradeLeg>(`${BASE}/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
}
