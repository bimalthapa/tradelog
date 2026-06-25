// @vitest-environment happy-dom
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import TradeEntryBar from './TradeEntryBar.vue'

function mountBar() {
  return mount(TradeEntryBar)
}

describe('TradeEntryBar rendering', () => {
  it('renders the › prompt glyph', () => {
    const wrapper = mountBar()
    expect(wrapper.find('.prompt').text()).toBe('›')
  })

  it('renders the Parse → button', () => {
    const wrapper = mountBar()
    expect(wrapper.find('.btn-parse').text()).toBe('Parse →')
  })

  it('shows no error message initially', () => {
    const wrapper = mountBar()
    expect(wrapper.find('.error-msg').exists()).toBe(false)
  })
})

describe('TradeEntryBar — valid parse', () => {
  it('emits parsed event with ParsedTrade payload on valid options input', async () => {
    const wrapper = mountBar()
    await wrapper.find('.entry-input').setValue('STO 5 SPY 480C 12/20 @2.35')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('parsed')).toHaveLength(1)
    const payload = wrapper.emitted('parsed')![0]![0] as { valid: boolean; action: string; cashFlow: number }
    expect(payload.valid).toBe(true)
    expect(payload.action).toBe('STO')
    expect(payload.cashFlow).toBe(1175)
  })

  it('does not show error on valid input', async () => {
    const wrapper = mountBar()
    await wrapper.find('.entry-input').setValue('STO 5 SPY 480C 12/20 @2.35')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.find('.error-msg').exists()).toBe(false)
  })
})

describe('TradeEntryBar — invalid parse', () => {
  it('shows error message below bar on invalid input', async () => {
    const wrapper = mountBar()
    await wrapper.find('.entry-input').setValue('gibberish input')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.find('.error-msg').exists()).toBe(true)
    expect(wrapper.find('.error-msg').text()).toContain('STO 5 SPY 480C 12/20 @2.35')
  })

  it('does not emit parsed on invalid input', async () => {
    const wrapper = mountBar()
    await wrapper.find('.entry-input').setValue('gibberish')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('parsed')).toBeFalsy()
  })

  it('applies has-error class to entry-wrap on error', async () => {
    const wrapper = mountBar()
    await wrapper.find('.entry-input').setValue('bad')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.find('.entry-wrap').classes()).toContain('has-error')
  })

  it('clears error message when user types after an error', async () => {
    const wrapper = mountBar()
    await wrapper.find('.entry-input').setValue('bad input')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.find('.error-msg').exists()).toBe(true)
    await wrapper.find('.entry-input').trigger('input')
    expect(wrapper.find('.error-msg').exists()).toBe(false)
  })

  it('does not emit parsed on empty input', async () => {
    const wrapper = mountBar()
    await wrapper.find('form').trigger('submit')
    expect(wrapper.emitted('parsed')).toBeFalsy()
  })
})
