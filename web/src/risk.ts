import type { ExecutionInput, Position, RiskSummary } from './types'

export const NOTIONAL_LIMIT = 1_000_000

export function applyExecution(positions: Position[], execution: ExecutionInput): Position[] {
  const current = positions.find(
    (position) =>
      position.accountId === execution.accountId &&
      position.instrument === execution.instrument,
  )
  const signedDelta = execution.quantity * (execution.side === 'BUY' ? 1 : -1)
  const signedQuantity = (current?.signedQuantity ?? 0) + signedDelta
  const netNotional = signedQuantity * execution.price
  const updated: Position = {
    accountId: execution.accountId,
    instrument: execution.instrument,
    assetClass: execution.assetClass,
    signedQuantity,
    lastPrice: execution.price,
    netNotional,
    limitBreached: Math.abs(netNotional) > NOTIONAL_LIMIT,
    updatedAt: execution.executedAt,
  }
  return [...positions.filter((position) => position !== current), updated].sort((a, b) =>
    a.instrument.localeCompare(b.instrument),
  )
}

export function summarize(accountId: string, positions: Position[]): RiskSummary {
  const scoped = positions.filter((position) => position.accountId === accountId)
  return {
    accountId,
    positionCount: scoped.length,
    breachedPositions: scoped.filter((position) => position.limitBreached).length,
    grossNotional: scoped.reduce(
      (total, position) => total + Math.abs(position.netNotional),
      0,
    ),
    netNotional: scoped.reduce((total, position) => total + position.netNotional, 0),
  }
}

export function formatMoney(value: number, currency = 'USD'): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(value)
}

export function formatQuantity(value: number): string {
  return new Intl.NumberFormat('en-US', { maximumFractionDigits: 4 }).format(value)
}

