import type { ParsedTrade } from '@/types/index'

const OPTIONS_RE = /^(STO|BTO|BTC|STC)\s+(\d+)\s+([A-Z]+)\s+(\d+)(C|P)\s+(\d{1,2}\/\d{1,2}(?:\/\d{4})?)\s+@([\d.]+)$/i
const STOCK_RE = /^(BTO|STC|ASGN)\s+(\d+)\s+([A-Z]+)\s+@([\d.]+)$/i

export function getStrategy(action: string, optionType?: string): string {
  const a = action.toUpperCase()
  const o = optionType?.toUpperCase()
  if (a === 'STO' && o === 'PUT') return 'CSP'
  if (a === 'STO' && o === 'CALL') return 'CC'
  if (a === 'STO') return 'STO'
  if (a === 'BTO') return 'Long'
  if (a === 'BTC' || a === 'STC') return 'Close'
  if (a === 'ASGN') return 'Assignment'
  return ''
}

export function parseTrade(input: string): ParsedTrade {
  const raw = input.trim()

  const optMatch = raw.match(OPTIONS_RE)
  if (optMatch) {
    const action   = optMatch[1]!
    const qtyStr   = optMatch[2]!
    const ticker   = optMatch[3]!
    const strikeStr = optMatch[4]!
    const optCode  = optMatch[5]!
    const expiry   = optMatch[6]!
    const priceStr = optMatch[7]!
    const qty = parseInt(qtyStr, 10)
    const price = parseFloat(priceStr)
    const strike = parseInt(strikeStr, 10)
    const optionType = optCode.toUpperCase() === 'C' ? 'CALL' : ('PUT' as const)
    const a = action.toUpperCase() as 'STO' | 'BTO' | 'BTC' | 'STC'
    const isCredit = a === 'STO' || a === 'STC'
    const cashFlow = isCredit ? qty * price * 100 : -(qty * price * 100)
    return {
      action: a,
      qty,
      ticker: ticker.toUpperCase(),
      instrumentType: 'OPTION',
      optionType,
      strike,
      expiry,
      price,
      cashFlow,
      strategy: getStrategy(a, optionType),
      valid: true,
    }
  }

  const stkMatch = raw.match(STOCK_RE)
  if (stkMatch) {
    const action   = stkMatch[1]!
    const qtyStr   = stkMatch[2]!
    const ticker   = stkMatch[3]!
    const priceStr = stkMatch[4]!
    const qty = parseInt(qtyStr, 10)
    const price = parseFloat(priceStr)
    const rawA = action.toUpperCase()
    const normalizedAction = rawA === 'ASGN' ? 'ASSIGNED' : rawA
    const isCredit = rawA === 'STC'
    const cashFlow = isCredit ? qty * price : -(qty * price)
    return {
      action: normalizedAction,
      qty,
      ticker: ticker.toUpperCase(),
      instrumentType: 'STOCK',
      price,
      cashFlow,
      strategy: getStrategy(rawA),
      valid: true,
    }
  }

  return {
    action: '',
    qty: 0,
    ticker: '',
    instrumentType: 'STOCK',
    price: 0,
    cashFlow: 0,
    strategy: '',
    valid: false,
    error: 'Could not parse. Try: STO 5 SPY 480C 12/20 @2.35',
  }
}

export function parseMulti(input: string): ParsedTrade[] {
  return input.split(',').map(segment => {
    const trimmed = segment.trim()
    if (!trimmed) {
      return {
        action: '', qty: 0, ticker: '', instrumentType: 'STOCK' as const,
        price: 0, cashFlow: 0, strategy: '', valid: false, error: 'empty input',
      }
    }
    return parseTrade(trimmed)
  })
}

export function detectMultiLegStrategy(legs: ParsedTrade[]): string {
  if (legs.some(l => !l.valid)) return ''
  if (legs.some(l => l.instrumentType !== 'OPTION')) return ''

  if (legs.length === 2) {
    const [a, b] = legs as [ParsedTrade, ParsedTrade]
    // Bull Put Spread: STO higher put + BTO lower put (credit spread)
    if (a.action === 'STO' && a.optionType === 'PUT' &&
        b.action === 'BTO' && b.optionType === 'PUT' &&
        a.expiry === b.expiry && a.strike! > b.strike!)
      return 'Bull Put Spread'
    // Bear Call Spread: STO lower call + BTO higher call (credit spread)
    if (a.action === 'STO' && a.optionType === 'CALL' &&
        b.action === 'BTO' && b.optionType === 'CALL' &&
        a.expiry === b.expiry && a.strike! < b.strike!)
      return 'Bear Call Spread'
    // Debit Put Spread: BTO higher put + STO lower put (debit spread)
    if (a.action === 'BTO' && a.optionType === 'PUT' &&
        b.action === 'STO' && b.optionType === 'PUT' &&
        a.expiry === b.expiry && a.strike! > b.strike!)
      return 'Debit Put Spread'
    // Debit Call Spread: BTO lower call + STO higher call (debit spread)
    if (a.action === 'BTO' && a.optionType === 'CALL' &&
        b.action === 'STO' && b.optionType === 'CALL' &&
        a.expiry === b.expiry && a.strike! < b.strike!)
      return 'Debit Call Spread'
    // Calendar Spread: same ticker + type + strike, different expiry
    if (a.ticker === b.ticker && a.optionType === b.optionType &&
        a.strike === b.strike && a.expiry !== b.expiry)
      return 'Calendar Spread'
  }

  if (legs.length === 4) {
    // Sort by strike; Iron Condor = BTO put (lowest), STO put, STO call, BTO call (highest)
    const sorted = [...legs].sort((a, b) => (a.strike ?? 0) - (b.strike ?? 0))
    const [l1, l2, l3, l4] = sorted as [ParsedTrade, ParsedTrade, ParsedTrade, ParsedTrade]
    const sameExpiry = l1.expiry === l2.expiry && l2.expiry === l3.expiry && l3.expiry === l4.expiry
    const isIronCondor =
      l1.optionType === 'PUT'  && l1.action === 'BTO' &&
      l2.optionType === 'PUT'  && l2.action === 'STO' &&
      l3.optionType === 'CALL' && l3.action === 'STO' &&
      l4.optionType === 'CALL' && l4.action === 'BTO'
    if (sameExpiry && isIronCondor) return 'Iron Condor'
  }

  return ''
}
