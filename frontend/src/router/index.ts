import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '@/views/DashboardView.vue'
import CampaignDetailView from '@/views/CampaignDetailView.vue'
import AnalyticsView from '@/views/AnalyticsView.vue'
import NewCampaignView from '@/views/NewCampaignView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/dashboard', component: DashboardView },
    { path: '/campaign/:id', component: CampaignDetailView },
    { path: '/analytics', component: AnalyticsView },
    { path: '/campaigns/new', component: NewCampaignView },
  ],
})

export default router
