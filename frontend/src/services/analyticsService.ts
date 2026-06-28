import type { AnalyticsSummary, PnlItem, CumulativeData } from '@/types/index'

const BASE = '/api/v1/analytics'

function accountParams(selectedId: number | null | 'all'): string {
  if (selectedId === 'all') return ''
  if (selectedId === null) return '?unassigned=true'
  return `?accountId=${selectedId}`
}

async function request<T>(url: string): Promise<T> {
  const res = await fetch(url)
  if (!res.ok) throw new Error(await res.text())
  return res.json() as Promise<T>
}

export async function getAnalyticsSummary(
  selectedId: number | null | 'all' = 'all'
): Promise<AnalyticsSummary> {
  return request<AnalyticsSummary>(`${BASE}/summary${accountParams(selectedId)}`)
}

export async function getPnlByStrategy(
  selectedId: number | null | 'all' = 'all'
): Promise<PnlItem[]> {
  return request<PnlItem[]>(`${BASE}/pnl-by-strategy${accountParams(selectedId)}`)
}

export async function getCumulativeData(
  selectedId: number | null | 'all' = 'all'
): Promise<CumulativeData> {
  return request<CumulativeData>(`${BASE}/cumulative${accountParams(selectedId)}`)
}
