<script setup lang="ts">
defineProps<{ status: 'OPEN' | 'CLOSED' }>()
</script>

<template>
  <span class="chip" :class="status === 'OPEN' ? 'chip-active' : 'chip-closed'">
    {{ status === 'OPEN' ? 'ACTIVE' : 'CLOSED' }}
  </span>
</template>

<style scoped>
.chip {
  display: inline-block;
  font-family: var(--font-ui);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  padding: 2px 6px;
  border-radius: 0;
}

.chip-active {
  background: rgba(74, 225, 118, 0.12);
  color: var(--secondary);
}

.chip-closed {
  background: rgba(66, 71, 84, 0.25);
  color: var(--outline);
}
</style>
