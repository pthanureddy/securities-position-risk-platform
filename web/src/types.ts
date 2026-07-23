export type AssetClass = 'EQUITY' | 'FUTURE'
export type Side = 'BUY' | 'SELL'

export interface Position {
  accountId: string
  instrument: string
  assetClass: AssetClass
  signedQuantity: number
  lastPrice: number
  netNotional: number
  limitBreached: boolean
  updatedAt: string
}

export interface RiskSummary {
  accountId: string
  positionCount: number
  breachedPositions: number
  grossNotional: number
  netNotional: number
}

export interface ExecutionInput {
  executionId: string
  accountId: string
  instrument: string
  assetClass: AssetClass
  side: Side
  quantity: number
  price: number
  currency: string
  executedAt: string
}

export interface ExecutionResponse {
  executionId: string
  accepted: boolean
  duplicate: boolean
  accountId: string
  instrument: string
  signedQuantity: number
  netNotional: number
  limitBreached: boolean
}

