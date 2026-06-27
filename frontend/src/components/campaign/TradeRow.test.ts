// @vitest-environment happy-dom
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import TradeRow from './TradeRow.vue'
import type { TradeLeg } from '@/types/index'

const stoLeg: TradeLeg = {
  id: 1, tradeEntryId: 1, campaignId: 1,
  instrumentType: 'OPTION', action: 'STO', ticker: 'SPY',
  quantity: 5, price: 2.35, netCashFlow: 1175,
  optionType: 'PUT', strike: 480, expiry: '2024-12-20',
  tradedAt: '2024-11-01', strategyTag: 'CSP',
}

const btcLeg: TradeLeg = {
  id: 2, tradeEntryId: 2, campaignId: 1,
  instrumentType: 'OPTION', action: 'BTC', ticker: 'SPY',
  quantity: 2, price: 0.50, netCashFlow: -100,
  optionType: 'PUT', strike: 480, expiry: '2024-12-20',
  tradedAt: '2024-11-15', strategyTag: 'CSP',
}

const expiredLeg: TradeLeg = {
  id: 3, tradeEntryId: 3, campaignId: 1,
  instrumentType: 'OPTION', action: 'EXPIRED', ticker: 'SPY',
  quantity: 1, price: 0, netCashFlow: 0,
  optionType: 'CALL', strike: 510, expiry: '2024-12-20',
  tradedAt: '2024-12-20', strategyTag: 'CC',
}

const assignedLeg: TradeLeg = {
  id: 4, tradeEntryId: 4, campaignId: 1,
  instrumentType: 'STOCK', action: 'ASSIGNED', ticker: 'SPY',
  quantity: 100, price: 480, netCashFlow: -48000,
  tradedAt: '2024-12-20',
}

const stockLeg: TradeLeg = {
  id: 5, tradeEntryId: 5, campaignId: 1,
  instrumentType: 'STOCK', action: 'BTO', ticker: 'SPY',
  quantity: 100, price: 498.50, netCashFlow: -49850,
  tradedAt: '2024-12-20',
}

function mountRow(trade: TradeLeg) {
  return mount(TradeRow, { props: { trade } })
}

describe('TradeRow — rendering', () => {
  it('renders traded date', () => {
    const w = mountRow(stoLeg)
    expect(w.text()).toContain('2024-11-01')
  })

  it('renders quantity', () => {
    const w = mountRow(stoLeg)
    expect(w.text()).toContain('5')
  })

  it('renders price formatted', () => {
    const w = mountRow(stoLeg)
    expect(w.text()).toContain('$2.35')
  })
})

describe('TradeRow — action coloring', () => {
  it('applies credit class to STO', () => {
    const w = mountRow(stoLeg)
    expect(w.find('.action').classes()).toContain('credit')
  })

  it('applies credit class to STC', () => {
    const stcLeg: TradeLeg = { ...stoLeg, action: 'STC' }
    const w = mountRow(stcLeg)
    expect(w.find('.action').classes()).toContain('credit')
  })

  it('applies debit class to BTC', () => {
    const w = mountRow(btcLeg)
    expect(w.find('.action').classes()).toContain('debit')
  })

  it('applies debit class to BTO', () => {
    const w = mountRow(stockLeg)
    expect(w.find('.action').classes()).toContain('debit')
  })

  it('applies warning class to ASSIGNED', () => {
    const w = mountRow(assignedLeg)
    expect(w.find('.action').classes()).toContain('warning')
  })

  it('applies dim class to EXPIRED', () => {
    const w = mountRow(expiredLeg)
    expect(w.find('.action').classes()).toContain('dim')
  })
})

describe('TradeRow — instrument formatting', () => {
  it('formats PUT option instrument', () => {
    const w = mountRow(stoLeg)
    expect(w.find('.instr-ticker').text()).toBe('SPY')
    expect(w.find('.instr-detail').text()).toBe('480P 12/20')
  })

  it('formats CALL option instrument', () => {
    const w = mountRow(expiredLeg)
    expect(w.find('.instr-detail').text()).toBe('510C 12/20')
  })

  it('formats STOCK instrument', () => {
    const w = mountRow(stockLeg)
    expect(w.find('.instr-ticker').text()).toBe('SPY')
    expect(w.find('.instr-detail').text()).toBe('STOCK')
  })
})

describe('TradeRow — cash flow', () => {
  it('shows positive cash flow with + prefix and credit class', () => {
    const w = mountRow(stoLeg)
    const cf = w.find('.cash-flow')
    expect(cf.text()).toContain('+')
    expect(cf.text()).toContain('1,175')
    expect(cf.classes()).toContain('credit')
  })

  it('shows negative cash flow with debit class', () => {
    const w = mountRow(btcLeg)
    const cf = w.find('.cash-flow')
    expect(cf.text()).toContain('-')
    expect(cf.text()).toContain('100')
    expect(cf.classes()).toContain('debit')
  })
})

describe('TradeRow — strategy chip', () => {
  it('renders strategy chip when strategyTag is present', () => {
    const w = mountRow(stoLeg)
    expect(w.find('.strategy-chip').exists()).toBe(true)
    expect(w.find('.strategy-chip').text()).toBe('CSP')
  })

  it('renders — when strategyTag is absent', () => {
    const w = mountRow(stockLeg)
    expect(w.find('.strategy-chip').exists()).toBe(false)
    expect(w.text()).toContain('—')
  })
})

describe('TradeRow — status derivation', () => {
  it('shows "open" status for STO', () => {
    const w = mountRow(stoLeg)
    expect(w.find('.status').text()).toBe('open')
    expect(w.find('.status').classes()).toContain('status-open')
  })

  it('shows "open" status for BTO', () => {
    const w = mountRow(stockLeg)
    expect(w.find('.status').text()).toBe('open')
    expect(w.find('.status').classes()).toContain('status-open')
  })

  it('shows "closed" status for BTC', () => {
    const w = mountRow(btcLeg)
    expect(w.find('.status').text()).toBe('closed')
    expect(w.find('.status').classes()).toContain('status-closed')
  })

  it('shows "expired" status for EXPIRED', () => {
    const w = mountRow(expiredLeg)
    expect(w.find('.status').text()).toBe('expired')
    expect(w.find('.status').classes()).toContain('status-expired')
  })

  it('shows "assigned" status with status-assigned class', () => {
    const w = mountRow(assignedLeg)
    expect(w.find('.status').text()).toBe('assigned')
    expect(w.find('.status').classes()).toContain('status-assigned')
  })
})
