import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import type { Campaign } from '@/types/index'
import { getCampaigns } from '@/services/campaignService'

export const useTradeLogStore = defineStore('tradeLog', () => {
  const campaigns = ref<Campaign[]>([])
  const loading   = ref(false)
  const error     = ref<string | null>(null)

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

  return { campaigns, loading, error, activeCampaigns, closedCampaigns, fetchCampaigns }
})
