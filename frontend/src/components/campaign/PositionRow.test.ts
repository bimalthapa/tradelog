// @vitest-environment happy-dom
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PositionRow from './PositionRow.vue'
import type { Position } from '@/types/index'

const optionPosition: Position = {
  id: 1,
  campaignId: 1,
  instrumentType: 'OPTION',
  ticker: 'SPY',
  optionType: 'PUT',
  strike: 480,
  expiry: '2024-12-20',
  openAction: 'STO',
  openQuantity: 5,
  avgPrice: 2.35,
  status: 'OPEN',
  openedAt: '2024-11-01',
}

const stockPosition: Position = {
  id: 2,
  campaignId: 1,
  instrumentType: 'STOCK',
  ticker: 'SPY',
  openAction: 'BTO',
  openQuantity: 100,
  avgPrice: 498.50,
  status: 'CLOSED',
  openedAt: '2024-12-20',
  closedAt: '2025-01-10',
}

function mountRow(position: Position) {
  return mount(PositionRow, {
    props: { position },
    global: { renderStubDefaultSlot: true },
  })
}

describe('PositionRow — rendering', () => {
  it('renders opened date', () => {
    const w = mountRow(optionPosition)
    expect(w.text()).toContain('2024-11-01')
  })

  it('renders quantity', () => {
    const w = mountRow(optionPosition)
    expect(w.text()).toContain('5')
  })

  it('renders avg price formatted', () => {
    const w = mountRow(optionPosition)
    expect(w.text()).toContain('$2.35')
  })
})

describe('PositionRow — action coloring', () => {
  it('applies credit class to STO action', () => {
    const w = mountRow(optionPosition)
    expect(w.find('.action').classes()).toContain('credit')
  })

  it('applies debit class to BTO action', () => {
    const w = mountRow(stockPosition)
    expect(w.find('.action').classes()).toContain('debit')
  })
})

describe('PositionRow — instrument formatting', () => {
  it('formats OPTION instrument as TICKER STRIKEp/C MM/DD', () => {
    const w = mountRow(optionPosition)
    expect(w.find('.instr-ticker').text()).toBe('SPY')
    expect(w.find('.instr-detail').text()).toBe('480P 12/20')
  })

  it('formats CALL option type correctly', () => {
    const callPos: Position = { ...optionPosition, optionType: 'CALL', strike: 510 }
    const w = mountRow(callPos)
    expect(w.find('.instr-detail').text()).toBe('510C 12/20')
  })

  it('formats STOCK instrument as STOCK label', () => {
    const w = mountRow(stockPosition)
    expect(w.find('.instr-ticker').text()).toBe('SPY')
    expect(w.find('.instr-detail').text()).toBe('STOCK')
  })
})

describe('PositionRow — status', () => {
  it('applies status-open class for OPEN status', () => {
    const w = mountRow(optionPosition)
    expect(w.find('.status').classes()).toContain('status-open')
    expect(w.find('.status').text()).toBe('OPEN')
  })

  it('applies status-closed class for CLOSED status', () => {
    const w = mountRow(stockPosition)
    expect(w.find('.status').classes()).toContain('status-closed')
    expect(w.find('.status').text()).toBe('CLOSED')
  })
})
