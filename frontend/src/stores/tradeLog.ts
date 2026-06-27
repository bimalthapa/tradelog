import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { Campaign, TradeLeg, Position } from '@/types/index'
import { getCampaigns, getCampaign } from '@/services/campaignService'
import { getTradesForCampaign } from '@/services/tradeService'
import { getPositionsForCampaign } from '@/services/positionService'

export const useTradeLogStore = defineStore('tradeLog', () => {
  const campaigns       = ref<Campaign[]>([])
  const currentCampaign = ref<Campaign | null>(null)
  const trades          = ref<TradeLeg[]>([])
  const positions       = ref<Position[]>([])
  const loading         = ref(false)
  const error           = ref<string | null>(null)

  const activeCampaigns = computed(() => campaigns.value.filter(c => c.status === 'OPEN'))
  const closedCampaigns = computed(() => campaigns.value.filter(c => c.status === 'CLOSED'))

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

  return {
    campaigns, currentCampaign, trades, positions,
    loading, error,
    activeCampaigns, closedCampaigns,
    fetchCampaigns, fetchCampaign, fetchTrades, fetchPositions,
  }
})
