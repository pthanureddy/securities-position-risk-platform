import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('operations dashboard', () => {
  it('renders the risk workspace and reliability model', () => {
    render(<App demoMode />)
    expect(screen.getByRole('heading', { name: /intraday exposure/i })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /current exposure/i })).toBeInTheDocument()
    expect(screen.getByText(/consistent write/i)).toBeInTheDocument()
    expect(screen.getByText('$1,794,500')).toBeInTheDocument()
  })

  it('labels all portfolio data as synthetic', () => {
    render(<App demoMode />)
    expect(screen.getByText(/securities operations \/ synthetic data/i)).toBeInTheDocument()
    expect(screen.getByText(/no live market, client, or clear street data/i)).toBeInTheDocument()
  })

  it('books a demo execution and refreshes position state', async () => {
    const user = userEvent.setup()
    render(<App demoMode />)

    await user.clear(screen.getByLabelText(/execution id/i))
    await user.type(screen.getByLabelText(/execution id/i), 'EXEC-UI-1')
    await user.click(screen.getByRole('button', { name: /book execution/i }))

    expect(await screen.findByText('NVDA')).toBeInTheDocument()
    expect(screen.getByText(/accepted EXEC-UI-1 in demo state/i)).toBeInTheDocument()
    expect(screen.getByText('$1,837,375')).toBeInTheDocument()
  })

  it('updates account state from the account control', async () => {
    const user = userEvent.setup()
    render(<App demoMode />)

    await user.clear(screen.getByLabelText(/^account$/i))
    await user.type(screen.getByLabelText(/^account$/i), 'OTHER')

    expect(screen.getAllByText('$0')).toHaveLength(2)
    expect(screen.getByText('0 instruments')).toBeInTheDocument()
  })
})

