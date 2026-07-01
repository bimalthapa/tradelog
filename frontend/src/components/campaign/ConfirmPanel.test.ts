// @vitest-environment happy-dom
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ConfirmPanel from './ConfirmPanel.vue'
import type { ParsedTrade, Position } from '@/types/index'

const mockTrade: ParsedTrade = {
  action: 'STO', qty: 5, ticker: 'SPY',
  instrumentType: 'OPTION', optionType: 'PUT',
  strike: 480, expiry: '2024-12-20',
  price: 2.35, cashFlow: 1175,
  strategy: 'CSP', valid: true,
}

function mountPanel(props: { trade: ParsedTrade; saveError?: string } = { trade: mockTrade }) {
  return mount(ConfirmPanel, { props: { mode: 'parse', ...props } })
}

describe('ConfirmPanel — rendering', () => {
  it('renders CONFIRM TRADE header', () => {
    expect(mountPanel().find('.dialog-title').text()).toBe('CONFIRM TRADE')
  })

  it('displays action, qty, ticker in field rows', () => {
    const texts = mountPanel().findAll('.field-value').map(v => v.text())
    expect(texts).toContain('STO')
    expect(texts).toContain('5')
    expect(texts).toContain('SPY')
  })

  it('pre-fills strategy input with trade.strategy', () => {
    const input = mountPanel().find('input#strategy').element as HTMLInputElement
    expect(input.value).toBe('CSP')
  })

  it('shows strike and expiry for options', () => {
    const texts = mountPanel().findAll('.field-value').map(v => v.text())
    expect(texts.some(t => t.includes('480'))).toBe(true)
    expect(texts.some(t => t.includes('2024-12-20'))).toBe(true)
  })

  it('hides strike and expiry for stock trades', () => {
    const stockTrade: ParsedTrade = {
      action: 'BTO', qty: 100, ticker: 'NVDA',
      instrumentType: 'STOCK', price: 820, cashFlow: -82000,
      strategy: 'Long', valid: true,
    }
    const texts = mountPanel({ trade: stockTrade }).findAll('.field-value').map(v => v.text())
    expect(texts.some(t => t.includes('480'))).toBe(false)
  })

  it('applies profit class to cash flow when positive', () => {
    const cells = mountPanel().findAll('.field-cell')
    const cashCell = cells.find(c => c.text().includes('CASH FLOW'))
    expect(cashCell?.find('.profit').exists()).toBe(true)
  })

  it('applies loss class to cash flow when negative', () => {
    const trade = { ...mockTrade, cashFlow: -500, action: 'BTO' }
    const cells = mountPanel({ trade }).findAll('.field-cell')
    const cashCell = cells.find(c => c.text().includes('CASH FLOW'))
    expect(cashCell?.find('.loss').exists()).toBe(true)
  })

  it('hides save error when saveError prop is absent', () => {
    expect(mountPanel().find('.save-error').exists()).toBe(false)
  })

  it('shows save error when saveError prop is provided', () => {
    const wrapper = mountPanel({ trade: mockTrade, saveError: 'Network failure' })
    expect(wrapper.find('.save-error').text()).toBe('Network failure')
  })
})

describe('ConfirmPanel — emits', () => {
  it('emits cancel when ✕ button clicked', async () => {
    const wrapper = mountPanel()
    await wrapper.find('.btn-x').trigger('click')
    expect(wrapper.emitted('cancel')).toHaveLength(1)
  })

  it('emits cancel when Cancel button clicked', async () => {
    const wrapper = mountPanel()
    await wrapper.find('.btn-cancel').trigger('click')
    expect(wrapper.emitted('cancel')).toHaveLength(1)
  })

  it('emits save with strategyTag and notes when Save Trade clicked', async () => {
    const wrapper = mountPanel()
    await wrapper.find('input#strategy').setValue('MyStrategy')
    await wrapper.find('.notes-textarea').setValue('some notes')
    await wrapper.find('.btn-save').trigger('click')
    const payload = wrapper.emitted('save')![0]![0] as { strategyTag: string; notes: string }
    expect(payload.strategyTag).toBe('MyStrategy')
    expect(payload.notes).toBe('some notes')
  })

  it('emits save with empty notes if none entered', async () => {
    const wrapper = mountPanel()
    await wrapper.find('.btn-save').trigger('click')
    const payload = wrapper.emitted('save')![0]![0] as { strategyTag: string; notes: string }
    expect(payload.notes).toBe('')
  })
})

describe('ConfirmPanel — roll mode', () => {
  const shortPosition: Position = {
    id: 1, campaignId: 1, instrumentType: 'OPTION', ticker: 'SPY',
    optionType: 'PUT', strike: 480, expiry: '2026-12-20',
    openAction: 'STO', openQuantity: 5, avgPrice: 2.35,
    status: 'OPEN', openedAt: '2026-01-01', openingLegId: 5,
  }
  const longPosition: Position = { ...shortPosition, openAction: 'BTO' }

  function mountRoll(position: Position) {
    return mount(ConfirmPanel, { props: { mode: 'roll', rollingPosition: position } })
  }

  it('shows BTC as the close action when rolling a short (STO) position', () => {
    const texts = mountRoll(shortPosition).findAll('.field-value').map(v => v.text())
    expect(texts).toContain('BTC')
    expect(texts).not.toContain('STC')
  })

  it('shows STC as the close action when rolling a long (BTO) position', () => {
    const texts = mountRoll(longPosition).findAll('.field-value').map(v => v.text())
    expect(texts).toContain('STC')
    expect(texts).not.toContain('BTC')
  })

  it('emits roll with STC/BTO-consistent values when rolling a long position', async () => {
    const wrapper = mountRoll(longPosition)
    await wrapper.find('#roll-btc-price').setValue('1.80')
    await wrapper.find('#roll-new-strike').setValue('470')
    await wrapper.find('#roll-new-expiry').setValue('1/17')
    await wrapper.find('#roll-sto-price').setValue('2.10')
    await wrapper.find('.btn-save').trigger('click')
    const payload = wrapper.emitted('roll')![0]![0] as { qty: number; btcPrice: number; stoPrice: number }
    expect(payload.btcPrice).toBe(1.8)
    expect(payload.stoPrice).toBe(2.1)
  })
})
