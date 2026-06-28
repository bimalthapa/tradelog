import type { AnalyticsSummary, PnlItem, CumulativeData } from '@/types/index'

const BASE = '/api/v1/analytics'

async function request<T>(url: string): Promise<T> {
  const res = await fetch(url)
  if (!res.ok) throw new Error(await res.text())
  return res.json() as Promise<T>
}

export async function getAnalyticsSummary(): Promise<AnalyticsSummary> {
  return request<AnalyticsSummary>(`${BASE}/summary`)
}

export async function getPnlByStrategy(): Promise<PnlItem[]> {
  return request<PnlItem[]>(`${BASE}/pnl-by-strategy`)
}

export async function getCumulativeData(): Promise<CumulativeData> {
  return request<CumulativeData>(`${BASE}/cumulative`)
}
