import type { Campaign, CreateCampaignRequest } from '@/types/index'

const BASE = '/api/v1/campaigns'

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = init ? await fetch(url, init) : await fetch(url)
  if (!res.ok) throw new Error(await res.text())
  return res.json() as Promise<T>
}

export async function getCampaigns(): Promise<Campaign[]> {
  return request<Campaign[]>(BASE)
}

export async function getCampaign(id: number): Promise<Campaign> {
  return request<Campaign>(`${BASE}/${id}`)
}

export async function createCampaign(req: CreateCampaignRequest): Promise<Campaign> {
  return request<Campaign>(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
}

export async function closeCampaign(id: number): Promise<Campaign> {
  return request<Campaign>(`${BASE}/${id}/close`, { method: 'PATCH' })
}
