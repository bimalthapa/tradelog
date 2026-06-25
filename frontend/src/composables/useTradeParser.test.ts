import { describe, it, expect } from 'vitest'
import { parseTrade, getStrategy } from './useTradeParser'

describe('getStrategy', () => {
  it('returns CSP for STO + PUT', () => {
    expect(getStrategy('STO', 'PUT')).toBe('CSP')
  })
  it('returns CC for STO + CALL', () => {
    expect(getStrategy('STO', 'CALL')).toBe('CC')
  })
  it('returns STO for STO without optionType', () => {
    expect(getStrategy('STO')).toBe('STO')
  })
  it('returns Long for BTO', () => {
    expect(getStrategy('BTO')).toBe('Long')
  })
  it('returns Close for BTC', () => {
    expect(getStrategy('BTC')).toBe('Close')
  })
  it('returns Close for STC', () => {
    expect(getStrategy('STC')).toBe('Close')
  })
  it('returns Assignment for ASGN', () => {
    expect(getStrategy('ASGN')).toBe('Assignment')
  })
})

describe('parseTrade — options', () => {
  it('parses STO CALL with correct fields and cash flow', () => {
    const r = parseTrade('STO 5 SPY 480C 12/20 @2.35')
    expect(r.valid).toBe(true)
    expect(r.action).toBe('STO')
    expect(r.qty).toBe(5)
    expect(r.ticker).toBe('SPY')
    expect(r.instrumentType).toBe('OPTION')
    expect(r.optionType).toBe('CALL')
    expect(r.strike).toBe(480)
    expect(r.expiry).toBe('12/20')
    expect(r.price).toBe(2.35)
    expect(r.cashFlow).toBe(1175)
    expect(r.strategy).toBe('CC')
  })

  it('parses STO PUT as CSP with positive cash flow', () => {
    const r = parseTrade('STO 2 SPY 480P 03/21 @1.50')
    expect(r.valid).toBe(true)
    expect(r.optionType).toBe('PUT')
    expect(r.cashFlow).toBe(300)
    expect(r.strategy).toBe('CSP')
  })

  it('parses BTC with negative cash flow and Close strategy', () => {
    const r = parseTrade('BTC 5 SPY 480C 12/20 @1.00')
    expect(r.valid).toBe(true)
    expect(r.cashFlow).toBe(-500)
    expect(r.strategy).toBe('Close')
  })

  it('parses STC with positive cash flow and Close strategy', () => {
    const r = parseTrade('STC 3 AAPL 200C 01/25 @4.00')
    expect(r.valid).toBe(true)
    expect(r.cashFlow).toBe(1200)
    expect(r.strategy).toBe('Close')
  })

  it('is case-insensitive', () => {
    const r = parseTrade('sto 5 spy 480c 12/20 @2.35')
    expect(r.valid).toBe(true)
    expect(r.action).toBe('STO')
    expect(r.ticker).toBe('SPY')
  })

  it('trims leading/trailing whitespace', () => {
    const r = parseTrade('  STO 5 SPY 480C 12/20 @2.35  ')
    expect(r.valid).toBe(true)
  })

  it('parses STO with single-digit expiry day', () => {
    const r = parseTrade('STO 5 SPY 480C 1/5 @2.35')
    expect(r.valid).toBe(true)
    expect(r.expiry).toBe('1/5')
  })
})

describe('parseTrade — stock', () => {
  it('parses BTO stock with negative cash flow and Long strategy', () => {
    const r = parseTrade('BTO 100 NVDA @820.00')
    expect(r.valid).toBe(true)
    expect(r.action).toBe('BTO')
    expect(r.qty).toBe(100)
    expect(r.ticker).toBe('NVDA')
    expect(r.instrumentType).toBe('STOCK')
    expect(r.price).toBe(820)
    expect(r.cashFlow).toBe(-82000)
    expect(r.strategy).toBe('Long')
  })

  it('parses STC stock with positive cash flow', () => {
    const r = parseTrade('STC 100 AAPL @200.00')
    expect(r.valid).toBe(true)
    expect(r.cashFlow).toBe(20000)
    expect(r.strategy).toBe('Close')
  })

  it('parses ASGN stock with negative cash flow and Assignment strategy', () => {
    const r = parseTrade('ASGN 100 NVDA @850.00')
    expect(r.valid).toBe(true)
    expect(r.action).toBe('ASSIGNED')
    expect(r.cashFlow).toBe(-85000)
    expect(r.strategy).toBe('Assignment')
  })
})

describe('parseTrade — invalid', () => {
  it('returns valid: false for gibberish', () => {
    const r = parseTrade('hello world')
    expect(r.valid).toBe(false)
    expect(r.error).toBeTruthy()
  })

  it('returns valid: false for empty string', () => {
    const r = parseTrade('')
    expect(r.valid).toBe(false)
  })

  it('error message includes the example trade string', () => {
    const r = parseTrade('not a trade')
    expect(r.error).toContain('STO 5 SPY 480C 12/20 @2.35')
  })
})
