import { describe, it, expect } from 'vitest'
import {
  formatCompact,
  formatDecimal,
  formatMilliseconds,
  formatPercentage,
  formatBytes,
} from '../format'

describe('formatCompact', () => {
  it('retorna "0" quando o valor é undefined', () => {
    expect(formatCompact(undefined)).toBe('0')
  })

  it('retorna "0" quando o valor é null', () => {
    expect(formatCompact(null as unknown as undefined)).toBe('0')
  })

  it('retorna "0" quando o valor é 0', () => {
    expect(formatCompact(0)).toBe('0')
  })

  it('formata valores na casa dos milhões como M', () => {
    expect(formatCompact(1000000)).toBe('1.0M')
    expect(formatCompact(2500000)).toBe('2.5M')
    expect(formatCompact(10000000)).toBe('10.0M')
  })

  it('formata valores na casa dos milhares como K', () => {
    expect(formatCompact(1000)).toBe('1.0K')
    expect(formatCompact(1500)).toBe('1.5K')
    expect(formatCompact(999999)).toBe('1000.0K')
  })

  it('valores menores que 1000 são exibidos como inteiro', () => {
    expect(formatCompact(1)).toBe('1')
    expect(formatCompact(999)).toBe('999')
    expect(formatCompact(42)).toBe('42')
  })

  it('trata números negativos', () => {
    expect(formatCompact(-1500)).toBe('-1.5K')
    expect(formatCompact(-2500000)).toBe('-2.5M')
    expect(formatCompact(-42)).toBe('-42')
  })
})

describe('formatDecimal', () => {
  it('retorna "--" quando o valor é undefined', () => {
    expect(formatDecimal(undefined)).toBe('--')
  })

  it('retorna "--" quando o valor é null', () => {
    expect(formatDecimal(null as unknown as undefined)).toBe('--')
  })

  it('retorna "--" quando o valor é NaN', () => {
    expect(formatDecimal(NaN)).toBe('--')
  })

  it('mantém 1 casa decimal por padrão', () => {
    expect(formatDecimal(3.14159)).toBe('3.1')
    expect(formatDecimal(0)).toBe('0.0')
  })

  it('permite especificar o número de casas decimais', () => {
    expect(formatDecimal(3.14159, 2)).toBe('3.14')
    expect(formatDecimal(3.14159, 0)).toBe('3')
    expect(formatDecimal(3.14159, 4)).toBe('3.1416')
  })
})

describe('formatMilliseconds', () => {
  it('retorna "--" quando o valor é undefined', () => {
    expect(formatMilliseconds(undefined)).toBe('--')
  })

  it('retorna "--" quando o valor é NaN', () => {
    expect(formatMilliseconds(NaN)).toBe('--')
  })

  it('sem casas decimais por padrão', () => {
    expect(formatMilliseconds(123.456)).toBe('123 ms')
    expect(formatMilliseconds(0)).toBe('0 ms')
  })

  it('permite especificar o número de casas decimais', () => {
    expect(formatMilliseconds(123.456, 2)).toBe('123.46 ms')
    expect(formatMilliseconds(123.456, 1)).toBe('123.5 ms')
  })
})

describe('formatPercentage', () => {
  it('retorna "--" quando o valor é undefined', () => {
    expect(formatPercentage(undefined)).toBe('--')
  })

  it('retorna "--" quando o valor é NaN', () => {
    expect(formatPercentage(NaN)).toBe('--')
  })

  it('converte um decimal em porcentagem', () => {
    expect(formatPercentage(0.5)).toBe('50.0%')
    expect(formatPercentage(1)).toBe('100.0%')
    expect(formatPercentage(0)).toBe('0.0%')
    expect(formatPercentage(0.123)).toBe('12.3%')
  })

  it('permite especificar o número de casas decimais', () => {
    expect(formatPercentage(0.12345, 2)).toBe('12.35%')
    expect(formatPercentage(0.12345, 0)).toBe('12%')
  })
})

describe('formatBytes', () => {
  it('retorna "0 B" quando o valor é 0 ou falsy', () => {
    expect(formatBytes(0)).toBe('0 B')
    expect(formatBytes(undefined)).toBe('0 B')
  })

  it('formata bytes', () => {
    expect(formatBytes(512)).toBe('512 B')
    expect(formatBytes(1)).toBe('1 B')
  })

  it('formata KB', () => {
    expect(formatBytes(1024)).toBe('1.0 KB')
    expect(formatBytes(1536)).toBe('1.5 KB')
  })

  it('formata MB', () => {
    expect(formatBytes(1048576)).toBe('1.0 MB')
    expect(formatBytes(1572864)).toBe('1.5 MB')
  })

  it('formata GB', () => {
    expect(formatBytes(1073741824)).toBe('1.00 GB')
    expect(formatBytes(2684354560)).toBe('2.50 GB')
  })

  it('trata números negativos (usa valor absoluto)', () => {
    expect(formatBytes(-1024)).toBe('1.0 KB')
  })
})
