import { afterEach, describe, expect, it, vi } from 'vitest'
import { marketApi } from './api'
import type { ExecutionInput } from './types'

const execution: ExecutionInput = {
  executionId: 'EXEC-API-1',
  accountId: 'ACC-1',
  instrument: 'AAPL',
  assetClass: 'EQUITY',
  side: 'BUY',
  quantity: 10,
  price: 200,
  currency: 'USD',
  executedAt: '2026-07-23T10:00:00Z',
}

afterEach(() => vi.restoreAllMocks())

describe('market API client', () => {
  it('loads account positions', async () => {
    const response = [{ accountId: 'ACC-1', instrument: 'AAPL' }]
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: vi.fn().mockResolvedValue(response),
    }))

    await expect(marketApi.positions('ACC 1')).resolves.toEqual(response)
    expect(fetch).toHaveBeenCalledWith('/api/v1/accounts/ACC%201/positions', undefined)
  })

  it('loads a risk summary', async () => {
    const response = { accountId: 'ACC-1', positionCount: 0 }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: vi.fn().mockResolvedValue(response),
    }))

    await expect(marketApi.risk('ACC-1')).resolves.toEqual(response)
  })

  it('posts a JSON execution', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: vi.fn().mockResolvedValue({ accepted: true }),
    }))

    await marketApi.execution(execution)

    expect(fetch).toHaveBeenCalledWith('/api/v1/executions', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify(execution),
    }))
  })

  it('surfaces a structured API error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      statusText: 'Bad Request',
      json: vi.fn().mockResolvedValue({ message: 'Request validation failed' }),
    }))

    await expect(marketApi.risk('ACC-1')).rejects.toThrow('Request validation failed')
  })

  it('falls back to the status text for a non-JSON error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      statusText: 'Service Unavailable',
      json: vi.fn().mockRejectedValue(new Error('not json')),
    }))

    await expect(marketApi.risk('ACC-1')).rejects.toThrow('Service Unavailable')
  })
})

