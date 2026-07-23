import { describe, expect, it } from 'vitest'
import { DEMO_ACCOUNT, DEMO_POSITIONS } from './demo'
import { applyExecution, formatMoney, formatQuantity, summarize } from './risk'
import type { ExecutionInput } from './types'

const baseExecution: ExecutionInput = {
  executionId: 'EXEC-1',
  accountId: DEMO_ACCOUNT,
  instrument: 'AAPL',
  assetClass: 'EQUITY',
  side: 'BUY',
  quantity: 100,
  price: 200,
  currency: 'USD',
  executedAt: '2026-07-23T10:00:00Z',
}

describe('position and risk calculations', () => {
  it('applies a buy to an existing position', () => {
    const result = applyExecution(DEMO_POSITIONS, baseExecution)
    expect(result.find((item) => item.instrument === 'AAPL')?.signedQuantity).toBe(1350)
  })

  it('applies a sell as a negative delta', () => {
    const result = applyExecution(DEMO_POSITIONS, { ...baseExecution, side: 'SELL', quantity: 2000 })
    expect(result.find((item) => item.instrument === 'AAPL')?.signedQuantity).toBe(-750)
  })

  it('adds and sorts a new instrument', () => {
    const result = applyExecution(DEMO_POSITIONS, { ...baseExecution, instrument: 'AMD' })
    expect(result.map((item) => item.instrument)).toEqual(['AAPL', 'AMD', 'ESU6', 'MSFT'])
  })

  it('flags absolute exposure above the limit', () => {
    const result = applyExecution([], { ...baseExecution, quantity: 6000 })
    expect(result[0].limitBreached).toBe(true)
  })

  it('does not flag exposure at the exact limit', () => {
    const result = applyExecution([], { ...baseExecution, quantity: 5000 })
    expect(result[0].limitBreached).toBe(false)
  })

  it('summarizes gross, net, count and breaches', () => {
    const result = summarize(DEMO_ACCOUNT, DEMO_POSITIONS)
    expect(result.positionCount).toBe(3)
    expect(result.breachedPositions).toBe(1)
    expect(result.grossNotional).toBe(1_794_500)
    expect(result.netNotional).toBe(-349_250)
  })

  it('excludes positions from other accounts', () => {
    expect(summarize('OTHER', DEMO_POSITIONS)).toEqual({
      accountId: 'OTHER',
      positionCount: 0,
      breachedPositions: 0,
      grossNotional: 0,
      netNotional: 0,
    })
  })

  it('formats money without decimal noise', () => {
    expect(formatMoney(1250)).toBe('$1,250')
  })

  it('formats signed quantities', () => {
    expect(formatQuantity(-1250.25)).toBe('-1,250.25')
  })
})

