export interface Campaign {
  id: number
  ticker: string
  label?: string
  status: 'OPEN' | 'CLOSED'
  notes?: string
  openedAt: string
  closedAt?: string
  realizedPnl?: number
  netCashFlow: number
  costBasis?: number
  sharesHeld?: number
  openPositionCount: number
}

export interface TradeLeg {
  id: number
  tradeEntryId: number
  campaignId: number
  instrumentType: 'STOCK' | 'OPTION'
  action: 'BTO' | 'STO' | 'BTC' | 'STC' | 'ASSIGNED' | 'EXPIRED'
  ticker: string
  quantity: number
  price: number
  netCashFlow: number
  optionType?: 'CALL' | 'PUT'
  strike?: number
  expiry?: string
  tradedAt: string
  strategyTag?: string
  notes?: string
}

export interface Position {
  id: number
  campaignId: number
  instrumentType: 'STOCK' | 'OPTION'
  ticker: string
  optionType?: 'CALL' | 'PUT'
  strike?: number
  expiry?: string
  openAction: 'BTO' | 'STO'
  openQuantity: number
  avgPrice: number
  status: 'OPEN' | 'CLOSED'
  openedAt: string
  closedAt?: string
}

export interface ParsedTrade {
  action: string
  qty: number
  ticker: string
  instrumentType: 'STOCK' | 'OPTION'
  optionType?: 'CALL' | 'PUT'
  strike?: number
  expiry?: string
  price: number
  cashFlow: number
  strategy: string
  valid: boolean
  error?: string
}

export interface CreateCampaignRequest {
  ticker: string
  label?: string
  notes?: string
  openedAt: string
}

export interface SaveTradeRequest {
  campaignId: number
  rawInput: string
  strategyTag?: string
  notes?: string
}

export const MOCK_PRICES: Record<string, number> = {
  NVDA: 875.40,
  SPY:  502.18,
  TSLA: 155.30,
  AAPL: 185.92,
  AMD:  165.44,
}
