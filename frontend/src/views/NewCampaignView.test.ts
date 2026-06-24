// @vitest-environment happy-dom
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import NewCampaignView from './NewCampaignView.vue'
import type { Campaign } from '@/types/index'

const mockPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  RouterLink: { template: '<a><slot /></a>' },
}))

vi.mock('@/services/campaignService', () => ({
  createCampaign: vi.fn(),
}))

import { createCampaign } from '@/services/campaignService'

const mockCampaign: Campaign = {
  id: 42, ticker: 'SPY', status: 'OPEN', openedAt: '2026-06-24',
  netCashFlow: 0, openPositionCount: 0,
}

function mountView() {
  return mount(NewCampaignView, {
    global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } },
  })
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('ticker field', () => {
  it('auto-uppercases input', async () => {
    const wrapper = mountView()
    const input = wrapper.find('input.ticker-input')
    await input.setValue('nvda')
    expect((input.element as HTMLInputElement).value).toBe('NVDA')
  })

  it('strips non-letter characters from input', async () => {
    const wrapper = mountView()
    const input = wrapper.find('input.ticker-input')
    await input.setValue('SP1Y')
    expect((input.element as HTMLInputElement).value).toBe('SPY')
  })
})

describe('validation', () => {
  it('shows "Ticker is required." and does not call createCampaign when ticker is empty', async () => {
    const wrapper = mountView()
    await wrapper.find('form').trigger('submit')
    expect(wrapper.find('.ticker-error').text()).toBe('Ticker is required.')
    expect(createCampaign).not.toHaveBeenCalled()
  })

  it('clears ticker error once ticker is filled', async () => {
    const wrapper = mountView()
    await wrapper.find('form').trigger('submit')
    expect(wrapper.find('.ticker-error').exists()).toBe(true)
    await wrapper.find('input.ticker-input').setValue('SPY')
    expect(wrapper.find('.ticker-error').exists()).toBe(false)
  })
})

describe('submit', () => {
  it('calls createCampaign with correct payload and navigates to campaign detail on success', async () => {
    vi.mocked(createCampaign).mockResolvedValue(mockCampaign)
    const wrapper = mountView()
    await wrapper.find('input.ticker-input').setValue('SPY')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(createCampaign).toHaveBeenCalledWith(expect.objectContaining({
      ticker: 'SPY',
      openedAt: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
    }))
    expect(mockPush).toHaveBeenCalledWith('/campaign/42')
  })

  it('shows API error and does not navigate when createCampaign rejects', async () => {
    vi.mocked(createCampaign).mockRejectedValue(new Error('Server error'))
    const wrapper = mountView()
    await wrapper.find('input.ticker-input').setValue('SPY')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(wrapper.find('.api-error').text()).toBe('Server error')
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('disables submit button while in-flight and re-enables on error', async () => {
    let resolve!: (c: Campaign) => void
    vi.mocked(createCampaign).mockReturnValue(new Promise(r => { resolve = r }))
    const wrapper = mountView()
    await wrapper.find('input.ticker-input').setValue('SPY')
    wrapper.find('form').trigger('submit')
    await flushPromises()
    const btn = wrapper.find('button[type="submit"]')
    expect((btn.element as HTMLButtonElement).disabled).toBe(true)
    resolve(mockCampaign)
    await flushPromises()
  })
})

describe('cancel', () => {
  it('navigates to /dashboard', async () => {
    const wrapper = mountView()
    await wrapper.find('button.cancel-btn').trigger('click')
    expect(mockPush).toHaveBeenCalledWith('/dashboard')
  })
})

describe('start date', () => {
  it('defaults to today in YYYY-MM-DD format', () => {
    const wrapper = mountView()
    const today = new Date().toISOString().split('T')[0]
    const dateInput = wrapper.find('input[type="date"]')
    expect((dateInput.element as HTMLInputElement).value).toBe(today)
  })
})
