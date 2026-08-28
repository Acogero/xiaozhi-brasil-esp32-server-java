/**
 * Configuração global do Vitest
 * Executado antes de todos os testes
 */

// Mock do componente message do ant-design-vue (evita chamadas reais de notificação DOM nos testes)
vi.mock('ant-design-vue', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
    loading: vi.fn(),
  },
}))

// Mock vue-i18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, unknown>) => {
      // Retorna a própria key, para facilitar as asserções
      if (params) {
        return `${key}:${JSON.stringify(params)}`
      }
      return key
    },
    locale: { value: 'zh-CN' },
  }),
  createI18n: vi.fn(),
}))
