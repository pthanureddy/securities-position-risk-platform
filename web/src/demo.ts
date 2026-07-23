import type { Position } from './types'

export const DEMO_ACCOUNT = 'NORTHSTAR-01'

export const DEMO_POSITIONS: Position[] = [
  {
    accountId: DEMO_ACCOUNT,
    instrument: 'AAPL',
    assetClass: 'EQUITY',
    signedQuantity: 1250,
    lastPrice: 214.5,
    netNotional: 268125,
    limitBreached: false,
    updatedAt: '2026-07-23T09:42:18Z',
  },
  {
    accountId: DEMO_ACCOUNT,
    instrument: 'ESU6',
    assetClass: 'FUTURE',
    signedQuantity: -175,
    lastPrice: 6125,
    netNotional: -1071875,
    limitBreached: true,
    updatedAt: '2026-07-23T09:43:04Z',
  },
  {
    accountId: DEMO_ACCOUNT,
    instrument: 'MSFT',
    assetClass: 'EQUITY',
    signedQuantity: 900,
    lastPrice: 505,
    netNotional: 454500,
    limitBreached: false,
    updatedAt: '2026-07-23T09:44:27Z',
  },
]

