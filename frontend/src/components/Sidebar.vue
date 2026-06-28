<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useTradeLogStore } from '@/stores/tradeLog'
import type { Campaign } from '@/types/index'
import AccountSelector from './sidebar/AccountSelector.vue'

const route = useRoute()
const store = useTradeLogStore()

const isCurrentCampaign = (id: number) => route.params.id === String(id)

function filterByAccount(campaigns: Campaign[]) {
  const sel = store.selectedAccountId
  if (sel === 'all') return campaigns
  if (sel === null) return campaigns.filter(c => !c.accountId)
  return campaigns.filter(c => c.accountId === sel)
}

const activeCampaigns = computed(() => filterByAccount(store.activeCampaigns))
const closedCampaigns = computed(() => filterByAccount(store.closedCampaigns))
</script>

<template>
  <aside class="sidebar">
    <div class="logo-area">
      <div class="logo-icon">▲</div>
      <span class="logo-text">TradeLog</span>
    </div>

    <nav class="nav">
      <div class="nav-item">
        <RouterLink to="/dashboard">Dashboard</RouterLink>
      </div>
      <div class="nav-item">
        <RouterLink to="/analytics">Analytics</RouterLink>
      </div>
    </nav>

    <AccountSelector />

    <div class="campaigns-area">
      <div v-if="activeCampaigns.length > 0" class="campaign-section">
        <div class="section-label">ACTIVE</div>
        <RouterLink
          v-for="c in activeCampaigns"
          :key="c.id"
          :to="`/campaign/${c.id}`"
          class="campaign-item"
          :class="{ 'campaign-item--active': isCurrentCampaign(c.id) }"
        >
          <span class="status-dot active"></span>
          <span class="campaign-ticker">{{ c.ticker }}</span>
          <span v-if="c.label" class="campaign-item-label">{{ c.label }}</span>
        </RouterLink>
      </div>

      <div v-if="closedCampaigns.length > 0" class="campaign-section">
        <div class="section-label">CLOSED</div>
        <RouterLink
          v-for="c in closedCampaigns"
          :key="c.id"
          :to="`/campaign/${c.id}`"
          class="campaign-item"
          :class="{ 'campaign-item--active': isCurrentCampaign(c.id) }"
        >
          <span class="status-dot closed"></span>
          <span class="campaign-ticker">{{ c.ticker }}</span>
          <span v-if="c.label" class="campaign-item-label">{{ c.label }}</span>
        </RouterLink>
      </div>
    </div>

    <div class="new-campaign-wrapper">
      <RouterLink to="/campaigns/new" class="new-campaign-btn">
        + New Campaign
      </RouterLink>
    </div>

    <div class="sidebar-footer">v0.1 · Jun 2026</div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 196px;
  min-width: 196px;
  height: 100vh;
  background: var(--surface-container-lowest);
  border-right: 1px solid var(--outline-variant);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 16px 12px;
  flex-shrink: 0;
}

.logo-icon {
  width: 24px;
  height: 24px;
  background: var(--primary-container);
  color: var(--on-primary-container);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  flex-shrink: 0;
}

.logo-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--on-surface);
  letter-spacing: -0.01em;
}

.nav {
  padding: 4px 0;
  flex-shrink: 0;
}

.nav-item {
  border-left: 2px solid transparent;
}

.nav-item:has(.router-link-active) {
  border-left-color: var(--primary);
}

.nav-item a {
  display: block;
  padding: 8px 14px;
  color: var(--on-surface-variant);
  text-decoration: none;
  font-size: 14px;
}

.nav-item:has(.router-link-active) a {
  color: var(--on-surface);
}

.nav-item a:hover {
  color: var(--on-surface);
}

/* Campaigns area scrolls independently */
.campaigns-area {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.campaign-section {
  padding-bottom: 8px;
  margin-top: 16px;
}

.section-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  padding: 4px 16px;
}

.campaign-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 16px 4px 14px;
  cursor: pointer;
  text-decoration: none;
  color: inherit;
  border-left: 2px solid transparent;
}

.campaign-item:hover {
  background: var(--surface-container-low);
}

.campaign-item--active {
  border-left-color: var(--primary);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.active { background: var(--secondary); }
.status-dot.closed { background: var(--outline); }

.campaign-ticker {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--on-surface);
  flex-shrink: 0;
}

.campaign-item-label {
  font-size: 11px;
  color: var(--on-surface-variant);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.new-campaign-wrapper {
  padding: 12px;
  flex-shrink: 0;
}

.new-campaign-btn {
  display: block;
  border: 1px dashed var(--outline-variant);
  background: transparent;
  color: var(--on-surface-variant);
  padding: 8px 12px;
  text-align: center;
  text-decoration: none;
  font-size: 13px;
}

.new-campaign-btn:hover {
  border-color: var(--outline);
  color: var(--on-surface);
}

.sidebar-footer {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--on-surface-variant);
  padding: 8px 16px 12px;
  flex-shrink: 0;
}
</style>
