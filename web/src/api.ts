import type {
  ExecutionInput,
  ExecutionResponse,
  Position,
  RiskSummary,
} from './types'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init)
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }))
    throw new Error(error.message ?? 'Request failed')
  }
  return response.json() as Promise<T>
}

export const marketApi = {
  positions(accountId: string): Promise<Position[]> {
    return request(`/api/v1/accounts/${encodeURIComponent(accountId)}/positions`)
  },
  risk(accountId: string): Promise<RiskSummary> {
    return request(`/api/v1/accounts/${encodeURIComponent(accountId)}/risk`)
  },
  execution(input: ExecutionInput): Promise<ExecutionResponse> {
    return request('/api/v1/executions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })
  },
}

