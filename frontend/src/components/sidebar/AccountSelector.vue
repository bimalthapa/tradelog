<script setup lang="ts">
import { ref, computed } from 'vue'
import { useTradeLogStore } from '@/stores/tradeLog'

const store = useTradeLogStore()

const open        = ref(false)
const addingNew   = ref(false)
const newName     = ref('')
const renamingId  = ref<number | null>(null)
const renameValue = ref('')
const addError    = ref('')
const renameError = ref('')

const selectedLabel = computed(() => {
  if (store.selectedAccountId === 'all') return 'All Accounts'
  if (store.selectedAccountId === null) return 'Unassigned'
  return store.accounts.find(a => a.id === store.selectedAccountId)?.name ?? 'All Accounts'
})

function select(id: number | null | 'all') {
  store.setSelectedAccount(id)
  open.value = false
  addingNew.value = false
  renamingId.value = null
}

function startAdd() {
  addingNew.value = true
  newName.value = ''
}

async function confirmAdd() {
  const name = newName.value.trim()
  if (!name) { addingNew.value = false; return }
  addError.value = ''
  try {
    await store.createAccountAction(name)
    addingNew.value = false
    newName.value = ''
  } catch {
    addError.value = 'Failed to create account'
  }
}

function startRename(id: number, currentName: string, e: Event) {
  e.stopPropagation()
  renamingId.value = id
  renameValue.value = currentName
}

async function confirmRename(id: number) {
  const name = renameValue.value.trim()
  if (!name) { renamingId.value = null; return }
  renameError.value = ''
  try {
    await store.renameAccountAction(id, name)
    renamingId.value = null
  } catch {
    renameError.value = 'Failed to rename account'
  }
}
</script>

<template>
  <div class="account-selector">
    <button class="selector-trigger" @click="open = !open">
      <span class="selector-label">{{ selectedLabel }}</span>
      <span class="selector-arrow" :class="{ rotated: open }">▾</span>
    </button>

    <div v-if="open" class="selector-dropdown">
      <button
        class="dropdown-item"
        :class="{ active: store.selectedAccountId === 'all' }"
        @click="select('all')"
      >All Accounts</button>

      <button
        class="dropdown-item"
        :class="{ active: store.selectedAccountId === null }"
        @click="select(null)"
      >Unassigned</button>

      <template v-if="store.accounts.length">
        <div class="dropdown-divider" />
        <div v-for="account in store.accounts" :key="account.id" class="dropdown-item-row">
          <template v-if="renamingId === account.id">
            <input
              class="inline-input"
              v-model="renameValue"
              @keydown.enter="confirmRename(account.id)"
              @blur="confirmRename(account.id)"
              @keydown.esc="renamingId = null"
              @click.stop
              autofocus
            />
            <div v-if="renameError" class="inline-error">{{ renameError }}</div>
          </template>
          <template v-else>
            <button
              class="dropdown-item"
              :class="{ active: store.selectedAccountId === account.id }"
              @click="select(account.id)"
            >{{ account.name }}</button>
            <button
              class="rename-btn"
              @click="startRename(account.id, account.name, $event)"
              title="Rename"
            >✎</button>
          </template>
        </div>
      </template>

      <div class="dropdown-divider" />

      <template v-if="addingNew">
        <input
          class="inline-input"
          v-model="newName"
          placeholder="Account name…"
          @keydown.enter="confirmAdd"
          @blur="confirmAdd"
          @keydown.esc="addingNew = false"
          autofocus
        />
        <div v-if="addError" class="inline-error">{{ addError }}</div>
      </template>
      <button v-else class="dropdown-item add-item" @click="startAdd">+ Add Account</button>
    </div>
  </div>
</template>

<style scoped>
.account-selector {
  position: relative;
  padding: 0 12px;
  margin-bottom: 8px;
}

.selector-trigger {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--surface-container-low);
  border: 1px solid var(--outline-variant);
  color: var(--on-surface-variant);
  font-family: var(--font-ui);
  font-size: 12px;
  font-weight: 500;
  padding: 6px 8px;
  cursor: pointer;
  text-align: left;
}
.selector-trigger:hover { background: var(--surface-container); color: var(--on-surface); }

.selector-arrow { font-size: 10px; transition: transform 0.15s; }
.selector-arrow.rotated { transform: rotate(180deg); }

.selector-dropdown {
  position: absolute;
  top: calc(100% + 2px);
  left: 12px;
  right: 12px;
  background: var(--surface-container-high);
  border: 1px solid var(--outline-variant);
  z-index: 200;
}

.dropdown-item {
  width: 100%;
  background: none;
  border: none;
  color: var(--on-surface-variant);
  font-family: var(--font-ui);
  font-size: 12px;
  padding: 6px 8px;
  text-align: left;
  cursor: pointer;
  display: block;
}
.dropdown-item:hover { background: var(--surface-container-highest); color: var(--on-surface); }
.dropdown-item.active { color: var(--primary); }

.dropdown-item-row { display: flex; align-items: center; }
.dropdown-item-row .dropdown-item { flex: 1; }

.rename-btn {
  background: none;
  border: none;
  color: var(--outline);
  cursor: pointer;
  padding: 0 8px;
  font-size: 13px;
  opacity: 0;
}
.dropdown-item-row:hover .rename-btn { opacity: 1; }

.inline-input {
  width: 100%;
  background: var(--surface-container-low);
  border: none;
  border-bottom: 1px solid var(--primary);
  color: var(--on-surface);
  font-family: var(--font-ui);
  font-size: 12px;
  padding: 6px 8px;
  outline: none;
}

.dropdown-divider { height: 1px; background: var(--outline-variant); margin: 2px 0; }
.add-item { color: var(--primary); }

.inline-error {
  font-family: var(--font-ui);
  font-size: 11px;
  color: var(--color-loss);
  padding: 2px 8px;
}
</style>
