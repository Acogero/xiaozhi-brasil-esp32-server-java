import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { formatDate, formatDateTime, getRelativeTime } from '../date'

describe('formatDate', () => {
  it('retorna o valor padrão "-" quando a entrada está vazia', () => {
    expect(formatDate()).toBe('-')
    expect(formatDate(undefined)).toBe('-')
    expect(formatDate('')).toBe('-')
  })

  it('valor padrão customizado', () => {
    expect(formatDate(undefined, 'N/A')).toBe('N/A')
    expect(formatDate('', 'Não disponível')).toBe('Não disponível')
  })

  it('formata uma string de data válida', () => {
    const result = formatDate('2026-03-12')
    // A saída de toLocaleDateString varia conforme o ambiente; apenas verifica que o retorno não é o valor padrão
    expect(result).not.toBe('-')
    expect(typeof result).toBe('string')
  })
})

describe('formatDateTime', () => {
  it('retorna o valor padrão "-" quando a entrada está vazia', () => {
    expect(formatDateTime()).toBe('-')
    expect(formatDateTime(undefined)).toBe('-')
    expect(formatDateTime('')).toBe('-')
  })

  it('valor padrão customizado', () => {
    expect(formatDateTime(undefined, 'N/A')).toBe('N/A')
  })

  it('formata uma string de data e hora válida', () => {
    const result = formatDateTime('2026-03-12T10:30:00')
    expect(result).not.toBe('-')
    expect(typeof result).toBe('string')
  })
})

describe('getRelativeTime', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-12T12:00:00'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('retorna "-" quando a entrada está vazia', () => {
    expect(getRelativeTime()).toBe('-')
    expect(getRelativeTime(undefined)).toBe('-')
    expect(getRelativeTime('')).toBe('-')
  })

  it('retorna "agora mesmo" quando a diferença é menor que 60 segundos', () => {
    const now = new Date('2026-03-12T11:59:30')
    expect(getRelativeTime(now.toISOString())).toBe('agora mesmo')
  })

  it('retorna "há N minutos" quando a diferença é menor que 60 minutos', () => {
    const fiveMinAgo = new Date('2026-03-12T11:55:00')
    expect(getRelativeTime(fiveMinAgo.toISOString())).toBe('há 5 minutos')

    const thirtyMinAgo = new Date('2026-03-12T11:30:00')
    expect(getRelativeTime(thirtyMinAgo.toISOString())).toBe('há 30 minutos')
  })

  it('retorna "há N horas" quando a diferença é menor que 24 horas', () => {
    const twoHoursAgo = new Date('2026-03-12T10:00:00')
    expect(getRelativeTime(twoHoursAgo.toISOString())).toBe('há 2 horas')
  })

  it('retorna "há N dias" quando a diferença é menor que 7 dias', () => {
    const threeDaysAgo = new Date('2026-03-09T12:00:00')
    expect(getRelativeTime(threeDaysAgo.toISOString())).toBe('há 3 dias')
  })

  it('retorna data formatada quando a diferença é >= 7 dias', () => {
    const tenDaysAgo = new Date('2026-03-02T12:00:00')
    const result = getRelativeTime(tenDaysAgo.toISOString())
    // Acima de 7 dias deve recair para formatDate, não no formato "há X dias"
    expect(result).not.toContain('dias')
    expect(result).not.toBe('-')
  })
})
