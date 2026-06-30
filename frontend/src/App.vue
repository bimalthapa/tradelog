<script setup lang="ts">
import { onMounted } from 'vue'
import Sidebar from '@/components/Sidebar.vue'
import { useTradeLogStore } from '@/stores/tradeLog'
import { usePriceStore } from '@/stores/priceStore'

const store      = useTradeLogStore()
const priceStore = usePriceStore()

onMounted(() => {
  store.fetchCampaigns()
  store.fetchAccounts()
  priceStore.startPolling()
})
</script>

<template>
  <div class="app-layout">
    <Sidebar />
    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>

<style>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  background: var(--background);
}
</style>
