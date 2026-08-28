import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  timeFix,
  welcome,
  formatDate,
  formatDateTime,
  formatNumber,
  formatDuration,
} from '../time'
import dayjs from 'dayjs'

describe('timeFix', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  const cases: [number, string][] = [
    [0, 'Boa madrugada'],
    [3, 'Boa madrugada'],
    [5, 'Boa madrugada'],
    [6, 'Bom dia'],
    [8, 'Bom dia'],
    [9, 'Bom dia'],
    [11, 'Bom dia'],
    [12, 'Bom almoço'],
    [13, 'Bom almoço'],
    [14, 'Boa tarde'],
    [16, 'Boa tarde'],
    [17, 'Boa tardinha'],
    [18, 'Boa tardinha'],
    [19, 'Boa noite'],
    [21, 'Boa noite'],
    [22, 'Boa noite'],
    [23, 'Boa noite'],
  ]

  it.each(cases)('hour=%i retorna "%s"', (hour, expected) => {
    vi.spyOn(dayjs.prototype, 'hour').mockReturnValue(hour)
    expect(timeFix()).toBe(expected)
  })
})

describe('welcome', () => {
  it('retorna uma string', () => {
    const result = welcome()
    expect(typeof result).toBe('string')
    expect(result.length).toBeGreaterThan(0)
  })

  it('o valor retornado está na lista predefinida', () => {
    const validMessages = [
      'Que você seja feliz todos os dias',
      'Hoje é mais um dia cheio de energia',
      'Que todos os seus desejos se realizem',
      'Mantenha o bom humor',
      'Progrida um pouco a cada dia',
      'Força! Você é o melhor',
    ]
    // Chama múltiplas vezes para garantir que o valor aleatório está dentro do intervalo
    for (let i = 0; i < 20; i++) {
      expect(validMessages).toContain(welcome())
    }
  })
})

describe('formatDate', () => {
  it('formata como YYYY-MM-DD', () => {
    expect(formatDate('2026-03-12T10:30:00')).toBe('2026-03-12')
    expect(formatDate(new Date('2026-01-01T00:00:00'))).toBe('2026-01-01')
  })
})

describe('formatDateTime', () => {
  it('formata como YYYY-MM-DD HH:mm:ss', () => {
    expect(formatDateTime('2026-03-12T10:30:45')).toBe('2026-03-12 10:30:45')
  })
})

describe('formatNumber', () => {
  it('adiciona separador de milhar', () => {
    expect(formatNumber(1234567)).toBe('1,234,567')
    expect(formatNumber(1000)).toBe('1,000')
    expect(formatNumber(100)).toBe('100')
  })

  it('retorna "0" quando o valor é 0 ou falsy', () => {
    expect(formatNumber(0)).toBe('0')
  })
})

describe('formatDuration', () => {
  it('retorna "0s" quando o valor é 0 ou falsy', () => {
    expect(formatDuration(0)).toBe('0s')
  })

  it('formata segundos puros', () => {
    expect(formatDuration(30)).toBe('30.0s')
    expect(formatDuration(0.5)).toBe('0.5s')
  })

  it('formata duração com minutos', () => {
    expect(formatDuration(90)).toBe('1min30.0s')
    expect(formatDuration(125)).toBe('2min5.0s')
    expect(formatDuration(60)).toBe('1min0.0s')
  })
})
