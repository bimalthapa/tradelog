import type { Position } from '@/types/index'

const BASE = '/api/v1/positions'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = init ? await fetch(url, init) : await fetch(url)
  if (!res.ok) throw new Error(await res.text())
  return res.json() as Promise<T>
}

export async function getPositionsForCampaign(campaignId: number): Promise<Position[]> {
  return request<Position[]>(`${BASE}?campaignId=${campaignId}`)
}
