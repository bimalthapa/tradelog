import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { Campaign, TradeLeg, Position, AnalyticsSummary, PnlItem, CumulativeData, Account } from '@/types/index'
import { getCampaigns, getCampaign } from '@/services/campaignService'
import { getTradesForCampaign } from '@/services/tradeService'
import { getPositionsForCampaign } from '@/services/positionService'
import { getAnalyticsSummary, getPnlByStrategy, getCumulativeData } from '@/services/analyticsService'
import { getAccounts, createAccount, renameAccount, deleteAccount, assignAccountToCampaign } from '@/services/accountService'

export const useTradeLogStore = defineStore('tradeLog', () => {
  const campaigns       = ref<Campaign[]>([])
  const currentCampaign = ref<Campaign | null>(null)
  const trades          = ref<TradeLeg[]>([])
  const positions       = ref<Position[]>([])
  const analytics       = ref<AnalyticsSummary | null>(null)
  const pnlByStrategy   = ref<PnlItem[]>([])
  const cumulativeData  = ref<CumulativeData | null>(null)
  const loading         = ref(false)
  const error           = ref<string | null>(null)
  const accounts          = ref<Account[]>([])
  const selectedAccountId = ref<number | null | 'all'>('all')

  const activeCampaigns = computed(() => campaigns.value.filter(c => c.status === 'OPEN'))
  const closedCampaigns = computed(() => campaigns.value.filter(c => c.status === 'CLOSED'))
  const openPositions   = computed(() => positions.value.filter(p => p.status === 'OPEN'))

  async function fetchCampaigns(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      campaigns.value = await getCampaigns()
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load campaigns'
    } finally {
      loading.value = false
    }
  }

  async function fetchCampaign(id: number): Promise<void> {
    error.value = null
    try {
      currentCampaign.value = await getCampaign(id)
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load campaign'
      currentCampaign.value = null
    }
  }

  async function fetchTrades(campaignId: number): Promise<void> {
    error.value = null
    try {
      trades.value = await getTradesForCampaign(campaignId)
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load trades'
    }
  }

  async function fetchPositions(campaignId: number): Promise<void> {
    error.value = null
    try {
      positions.value = await getPositionsForCampaign(campaignId)
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load positions'
    }
  }

  async function fetchAnalytics(): Promise<void> {
    error.value = null
    try {
      const sel = selectedAccountId.value
      const [summary, strategy, cumulative] = await Promise.all([
        getAnalyticsSummary(sel),
        getPnlByStrategy(sel),
        getCumulativeData(sel),
      ])
      analytics.value      = summary
      pnlByStrategy.value  = strategy
      cumulativeData.value = cumulative
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load analytics'
    }
  }

  async function fetchAccounts(): Promise<void> {
    try {
      accounts.value = await getAccounts()
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to load accounts'
    }
  }

  function setSelectedAccount(id: number | null | 'all'): void {
    selectedAccountId.value = id
  }

  async function createAccountAction(name: string): Promise<void> {
    const account = await createAccount(name)
    accounts.value = [...accounts.value, account].sort((a, b) => a.name.localeCompare(b.name))
  }

  async function renameAccountAction(id: number, name: string): Promise<void> {
    const updated = await renameAccount(id, name)
    const idx = accounts.value.findIndex(a => a.id === id)
    if (idx !== -1) {
      accounts.value = accounts.value.map((a, i) => i === idx ? updated : a)
        .sort((a, b) => a.name.localeCompare(b.name))
    }
  }

  async function deleteAccountAction(id: number): Promise<void> {
    await deleteAccount(id)
    accounts.value = accounts.value.filter(a => a.id !== id)
    if (selectedAccountId.value === id) selectedAccountId.value = 'all'
  }

  async function assignAccount(campaignId: number, accountId: number | null): Promise<void> {
    await assignAccountToCampaign(campaignId, accountId)
    await fetchCampaigns()
    if (currentCampaign.value?.id === campaignId) {
      await fetchCampaign(campaignId)
    }
  }

  return {
    campaigns, currentCampaign, trades, positions,
    analytics, pnlByStrategy, cumulativeData,
    loading, error,
    activeCampaigns, closedCampaigns, openPositions,
    accounts, selectedAccountId,
    fetchCampaigns, fetchCampaign, fetchTrades, fetchPositions, fetchAnalytics,
    fetchAccounts, setSelectedAccount,
    createAccountAction, renameAccountAction, deleteAccountAction, assignAccount,
  }
})
