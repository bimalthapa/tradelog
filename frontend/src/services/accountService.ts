import type { Account } from '@/types/index'

const BASE = '/api/v1/accounts'

export async function getAccounts(): Promise<Account[]> {
  const res = await fetch(BASE)
  if (!res.ok) throw new Error('Failed to fetch accounts')
  return res.json()
}

export async function createAccount(name: string): Promise<Account> {
  const res = await fetch(BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
  if (!res.ok) throw new Error('Failed to create account')
  return res.json()
}

export async function renameAccount(id: number, name: string): Promise<Account> {
  const res = await fetch(`${BASE}/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
  if (!res.ok) throw new Error('Failed to rename account')
  return res.json()
}

export async function deleteAccount(id: number): Promise<void> {
  const res = await fetch(`${BASE}/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error('Failed to delete account')
}

export async function assignAccountToCampaign(
  campaignId: number,
  accountId: number | null
): Promise<void> {
  const body = accountId === null ? { clearAccount: true } : { accountId }
  const res = await fetch(`/api/v1/campaigns/${campaignId}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error('Failed to assign account')
}
