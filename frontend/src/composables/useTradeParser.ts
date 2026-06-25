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
    const [, action, qtyStr, ticker, strikeStr, optCode, expiry, priceStr] = optMatch
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
    const [, action, qtyStr, ticker, priceStr] = stkMatch
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
