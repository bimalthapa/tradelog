import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { computeDte } from './dte'

describe('computeDte', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('returns positive DTE for a future expiry', () => {
    vi.setSystemTime(new Date('2026-06-30'))
    expect(computeDte('2026-07-21')).toBe(21)
  })

  it('returns 0 on expiration day', () => {
    vi.setSystemTime(new Date('2026-07-18'))
    expect(computeDte('2026-07-18')).toBe(0)
  })

  it('returns negative DTE for a past expiry', () => {
    vi.setSystemTime(new Date('2026-07-20'))
    expect(computeDte('2026-07-18')).toBe(-2)
  })

  it('returns 7 for exactly one week out (red zone boundary)', () => {
    vi.setSystemTime(new Date('2026-07-11'))
    expect(computeDte('2026-07-18')).toBe(7)
  })

  it('returns 8 for 8 days out (amber zone boundary)', () => {
    vi.setSystemTime(new Date('2026-07-10'))
    expect(computeDte('2026-07-18')).toBe(8)
  })

  it('returns 22 for 22 days out (green zone)', () => {
    vi.setSystemTime(new Date('2026-06-26'))
    expect(computeDte('2026-07-18')).toBe(22)
  })
})
