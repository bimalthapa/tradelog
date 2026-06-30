import { describe, it, expect } from 'vitest'
import { parseTrade, getStrategy, parseMulti, detectMultiLegStrategy } from './useTradeParser'

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

describe('parseMulti', () => {
  it('parses two valid option legs', () => {
    const result = parseMulti('STO 5 SPY 480C 12/20 @2.35, BTO 5 SPY 485C 12/20 @0.85')
    expect(result).toHaveLength(2)
    expect(result[0]!.valid).toBe(true)
    expect(result[0]!.action).toBe('STO')
    expect(result[0]!.cashFlow).toBe(1175)
    expect(result[1]!.valid).toBe(true)
    expect(result[1]!.action).toBe('BTO')
    expect(result[1]!.cashFlow).toBe(-425)
  })

  it('flags an invalid second leg as valid:false', () => {
    const result = parseMulti('STO 5 SPY 480C 12/20 @2.35, garbage')
    expect(result[0]!.valid).toBe(true)
    expect(result[1]!.valid).toBe(false)
    expect(result[1]!.error).toBeTruthy()
  })

  it('returns valid:false with error "empty input" for empty segment', () => {
    const result = parseMulti('STO 5 SPY 480C 12/20 @2.35,')
    expect(result).toHaveLength(2)
    expect(result[1]!.valid).toBe(false)
    expect(result[1]!.error).toBe('empty input')
  })

  it('handles extra whitespace around the comma', () => {
    const result = parseMulti('STO 5 SPY 480C 12/20 @2.35 ,  BTO 5 SPY 485C 12/20 @0.85')
    expect(result[0]!.valid).toBe(true)
    expect(result[1]!.valid).toBe(true)
  })
})

describe('detectMultiLegStrategy', () => {
  it('detects Bull Put Spread: STO higher put + BTO lower put, same expiry', () => {
    const legs = parseMulti('STO 5 SPY 480P 12/20 @2.35, BTO 5 SPY 475P 12/20 @0.85')
    expect(detectMultiLegStrategy(legs)).toBe('Bull Put Spread')
  })

  it('detects Bear Call Spread: STO lower call + BTO higher call, same expiry', () => {
    const legs = parseMulti('STO 5 SPY 480C 12/20 @2.35, BTO 5 SPY 485C 12/20 @0.85')
    expect(detectMultiLegStrategy(legs)).toBe('Bear Call Spread')
  })

  it('detects Debit Put Spread: BTO higher put + STO lower put, same expiry', () => {
    const legs = parseMulti('BTO 5 SPY 480P 12/20 @3.00, STO 5 SPY 475P 12/20 @1.50')
    expect(detectMultiLegStrategy(legs)).toBe('Debit Put Spread')
  })

  it('detects Debit Call Spread: BTO lower call + STO higher call, same expiry', () => {
    const legs = parseMulti('BTO 5 SPY 480C 12/20 @3.00, STO 5 SPY 485C 12/20 @1.50')
    expect(detectMultiLegStrategy(legs)).toBe('Debit Call Spread')
  })

  it('detects Calendar Spread: same ticker + type + strike, different expiry', () => {
    const legs = parseMulti('STO 5 SPY 480C 12/20 @2.35, BTO 5 SPY 480C 1/17 @3.50')
    expect(detectMultiLegStrategy(legs)).toBe('Calendar Spread')
  })

  it('detects Iron Condor from 4 legs regardless of input order', () => {
    // sorted by strike: BTO 460P, STO 465P, STO 475C, BTO 480C
    const legs = parseMulti(
      'BTO 5 SPY 460P 12/20 @0.50, STO 5 SPY 465P 12/20 @1.20, STO 5 SPY 475C 12/20 @1.10, BTO 5 SPY 480C 12/20 @0.45'
    )
    expect(detectMultiLegStrategy(legs)).toBe('Iron Condor')
  })

  it('returns empty string for unrecognized 2-leg pattern (two STOs)', () => {
    const legs = parseMulti('STO 5 SPY 480C 12/20 @2.35, STO 5 SPY 475C 12/20 @1.50')
    expect(detectMultiLegStrategy(legs)).toBe('')
  })

  it('returns empty string when any leg is a stock (non-option)', () => {
    const legs = parseMulti('BTO 100 SPY @500.00, STO 5 SPY 480C 12/20 @2.35')
    expect(detectMultiLegStrategy(legs)).toBe('')
  })
})
