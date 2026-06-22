<script setup lang="ts">
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

    <div class="campaign-section">
      <div class="section-label">ACTIVE</div>
      <div class="campaign-item">
        <span class="status-dot active"></span>
        <span class="campaign-ticker">NVDA</span>
      </div>
      <div class="campaign-item">
        <span class="status-dot active"></span>
        <span class="campaign-ticker">SPY</span>
      </div>
    </div>

    <div class="campaign-section">
      <div class="section-label">CLOSED</div>
      <div class="campaign-item">
        <span class="status-dot closed"></span>
        <span class="campaign-ticker">AAPL</span>
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

.campaign-section {
  margin-top: 16px;
  padding-bottom: 8px;
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
  padding: 4px 16px;
  cursor: pointer;
}

.campaign-item:hover {
  background: var(--surface-container-low);
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
}

.new-campaign-wrapper {
  margin-top: auto;
  padding: 12px;
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
}
</style>
