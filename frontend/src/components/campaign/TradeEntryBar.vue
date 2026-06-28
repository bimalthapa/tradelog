<script setup lang="ts">
import { ref } from 'vue'
import { parseTrade } from '@/composables/useTradeParser'
import type { ParsedTrade } from '@/types/index'

const emit = defineEmits<{ parsed: [{ trade: ParsedTrade; rawInput: string }] }>()

const rawInput  = ref('')
const error     = ref('')
const flashing  = ref(false)

function handleParse() {
  const result = parseTrade(rawInput.value)
  if (!result.valid) {
    error.value = result.error ?? 'Could not parse trade input.'
  } else {
    error.value = ''
    emit('parsed', { trade: result, rawInput: rawInput.value })
  }
}

function clearInput() {
  rawInput.value = ''
  error.value = ''
}

function triggerFlash() {
  flashing.value = true
  setTimeout(() => { flashing.value = false }, 600)
}

defineExpose({ triggerFlash, clearInput })
</script>

<template>
  <div class="trade-entry">
    <form @submit.prevent="handleParse" class="entry-form">
      <div class="entry-wrap" :class="{ 'has-error': error, 'flash': flashing }">
        <span class="prompt">›</span>
        <input
          v-model="rawInput"
          @input="error = ''"
          type="text"
          spellcheck="false"
          autocomplete="off"
          placeholder="Enter trade…"
          class="entry-input"
          aria-label="Trade input"
        />
      </div>
      <button type="submit" class="btn-parse">Parse →</button>
    </form>
    <div class="syntax-strip">
      <div class="syntax-row">
        <span class="syntax-label">OPTIONS</span>
        <span class="syntax-example">STO 5 SPY 480C 12/20 @2.35</span>
      </div>
      <div class="syntax-row">
        <span class="syntax-label">STOCK</span>
        <span class="syntax-example">BTO 100 NVDA @820.00</span>
      </div>
    </div>
    <p v-if="error" class="error-msg">{{ error }}</p>
  </div>
</template>

<style scoped>
.trade-entry {
  padding: 12px 28px;
  border-bottom: 1px solid var(--outline-variant);
}

.entry-form {
  display: flex;
  align-items: center;
  gap: 8px;
}

.entry-wrap {
  position: relative;
  flex: 1;
  border-bottom: 1px solid var(--outline-variant);
  transition: border-color 0.1s, background 0.3s;
}

.entry-wrap:focus-within {
  border-color: var(--primary);
}

.entry-wrap.has-error {
  border-color: var(--color-loss);
}

.entry-wrap.flash {
  background: rgba(74, 225, 118, 0.06);
}

.prompt {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 500;
  color: var(--primary);
  user-select: none;
  pointer-events: none;
}

.entry-input {
  width: 100%;
  background: transparent;
  border: none;
  border-radius: 0;
  outline: none;
  padding: 8px 8px 8px 20px;
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 500;
  line-height: 18px;
  color: var(--on-surface);
}

.entry-input::placeholder {
  color: var(--on-surface-variant);
  opacity: 0.4;
}

.btn-parse {
  padding: 6px 16px;
  background: var(--primary-container);
  color: var(--on-primary-container);
  border: none;
  border-radius: 0;
  font-family: var(--font-ui);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}

.btn-parse:hover {
  filter: brightness(1.1);
}

.error-msg {
  margin: 6px 0 0;
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 400;
  line-height: 16px;
  color: var(--color-loss);
}

.syntax-strip {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 6px 20px 0;
}

.syntax-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.syntax-label {
  font-family: var(--font-ui);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--on-surface-variant);
  min-width: 60px;
  opacity: 0.6;
  user-select: none;
}

.syntax-example {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 400;
  line-height: 16px;
  color: var(--on-surface-variant);
  opacity: 0.5;
  user-select: none;
}
</style>
