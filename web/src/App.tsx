import { type FormEvent, useEffect, useMemo, useState } from 'react'
import { marketApi } from './api'
import { DEMO_ACCOUNT, DEMO_POSITIONS } from './demo'
import { applyExecution, formatMoney, formatQuantity, summarize } from './risk'
import type { AssetClass, ExecutionInput, Position, Side } from './types'
import './styles.css'

const isDemo =
  location.hostname.endsWith('github.io') ||
  new URLSearchParams(location.search).get('demo') === 'true'

function defaultExecution(accountId: string): ExecutionInput {
  return {
    executionId: `EXEC-${Date.now()}`,
    accountId,
    instrument: 'NVDA',
    assetClass: 'EQUITY',
    side: 'BUY',
    quantity: 250,
    price: 171.5,
    currency: 'USD',
    executedAt: new Date().toISOString(),
  }
}

interface AppProps {
  demoMode?: boolean
}

export default function App({ demoMode = isDemo }: AppProps) {
  const [accountId, setAccountId] = useState(DEMO_ACCOUNT)
  const [positions, setPositions] = useState<Position[]>(demoMode ? DEMO_POSITIONS : [])
  const [execution, setExecution] = useState<ExecutionInput>(() => defaultExecution(DEMO_ACCOUNT))
  const [status, setStatus] = useState(demoMode ? 'Synthetic browser demo' : 'Connecting to API')
  const [busy, setBusy] = useState(false)
  const risk = useMemo(() => summarize(accountId, positions), [accountId, positions])
  const visiblePositions = useMemo(
    () => positions.filter((position) => position.accountId === accountId),
    [accountId, positions],
  )

  useEffect(() => {
    if (demoMode) return
    Promise.all([marketApi.positions(accountId), marketApi.risk(accountId)])
      .then(([nextPositions]) => {
        setPositions(nextPositions)
        setStatus('Live API connected')
      })
      .catch(() => setStatus('API unavailable - run the backend or add ?demo=true'))
  }, [accountId, demoMode])

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    const input = { ...execution, accountId, executedAt: new Date().toISOString() }
    try {
      if (demoMode) {
        setPositions((current) => applyExecution(current, input))
        setStatus(`Accepted ${input.executionId} in demo state`)
      } else {
        const result = await marketApi.execution(input)
        setPositions(await marketApi.positions(accountId))
        setStatus(result.duplicate ? 'Replay detected; position unchanged' : 'Execution accepted')
      }
      setExecution(defaultExecution(accountId))
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'Execution failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main>
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark">N</span>
          <div>
            <strong>Northstar</strong>
            <span>Position & Risk</span>
          </div>
        </div>
        <div className="status"><i />{status}</div>
      </header>

      <section className="hero">
        <div>
          <p className="eyebrow">SECURITIES OPERATIONS / SYNTHETIC DATA</p>
          <h1>Intraday exposure, without the blind spots.</h1>
          <p className="lede">
            Replay-safe execution intake, deterministic positions, and explicit limit
            breaches for equity and futures workflows.
          </p>
        </div>
        <label className="account">
          Account
          <input
            value={accountId}
            onChange={(event) => {
              setAccountId(event.target.value)
              setExecution((current) => ({ ...current, accountId: event.target.value }))
            }}
          />
        </label>
      </section>

      <section className="metrics" aria-label="Risk summary">
        <article><span>Gross notional</span><strong>{formatMoney(risk.grossNotional)}</strong></article>
        <article><span>Net notional</span><strong>{formatMoney(risk.netNotional)}</strong></article>
        <article><span>Open positions</span><strong>{risk.positionCount}</strong></article>
        <article className={risk.breachedPositions ? 'warning' : ''}>
          <span>Limit breaches</span><strong>{risk.breachedPositions}</strong>
        </article>
      </section>

      <section className="workspace">
        <article className="panel positions-panel">
          <div className="panel-heading">
            <div><p className="eyebrow">POSITION LEDGER</p><h2>Current exposure</h2></div>
            <span>{visiblePositions.length} instruments</span>
          </div>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Instrument</th><th>Class</th><th>Quantity</th><th>Last</th><th>Net notional</th><th>Risk</th></tr></thead>
              <tbody>
                {visiblePositions.map((position) => (
                  <tr key={position.instrument}>
                    <td><strong>{position.instrument}</strong></td>
                    <td>{position.assetClass}</td>
                    <td className={position.signedQuantity < 0 ? 'negative' : ''}>
                      {formatQuantity(position.signedQuantity)}
                    </td>
                    <td>{formatMoney(position.lastPrice)}</td>
                    <td>{formatMoney(position.netNotional)}</td>
                    <td><span className={`risk-pill ${position.limitBreached ? 'breach' : 'clear'}`}>{position.limitBreached ? 'BREACH' : 'CLEAR'}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </article>

        <form className="panel execution-panel" onSubmit={submit}>
          <div className="panel-heading"><div><p className="eyebrow">EXECUTION INTAKE</p><h2>Book synthetic trade</h2></div></div>
          <label>Execution ID<input value={execution.executionId} onChange={(event) => setExecution({ ...execution, executionId: event.target.value })} required /></label>
          <div className="field-row">
            <label>Instrument<input value={execution.instrument} onChange={(event) => setExecution({ ...execution, instrument: event.target.value.toUpperCase() })} required /></label>
            <label>Asset class<select value={execution.assetClass} onChange={(event) => setExecution({ ...execution, assetClass: event.target.value as AssetClass })}><option>EQUITY</option><option>FUTURE</option></select></label>
          </div>
          <div className="field-row">
            <label>Side<select value={execution.side} onChange={(event) => setExecution({ ...execution, side: event.target.value as Side })}><option>BUY</option><option>SELL</option></select></label>
            <label>Quantity<input type="number" min="0.000001" step="any" value={execution.quantity} onChange={(event) => setExecution({ ...execution, quantity: Number(event.target.value) })} required /></label>
          </div>
          <label>Price (USD)<input type="number" min="0.000001" step="any" value={execution.price} onChange={(event) => setExecution({ ...execution, price: Number(event.target.value) })} required /></label>
          <button disabled={busy}>{busy ? 'Booking...' : 'Book execution'}</button>
          <p className="form-note">Duplicate execution IDs are accepted as replays and do not change positions.</p>
        </form>
      </section>

      <section className="architecture">
        <p className="eyebrow">RELIABILITY MODEL</p>
        <div>
          <article><b>01</b><h3>Consistent write</h3><p>Execution, position update, and outbox record commit in one database transaction.</p></article>
          <article><b>02</b><h3>Replay safe</h3><p>Execution IDs provide an idempotency boundary before position state changes.</p></article>
          <article><b>03</b><h3>Observable delivery</h3><p>Kafka publication retries and Prometheus metrics expose pending and failed work.</p></article>
        </div>
      </section>
      <footer>Independent engineering portfolio · No live market, client, or Clear Street data</footer>
    </main>
  )
}
