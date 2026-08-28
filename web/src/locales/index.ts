/**
 * Configuração de internacionalização
 * Nota: é necessário instalar o vue-i18n
 * npm install vue-i18n@9
 */
import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'
import ptBR from './pt-BR'

// Idioma padrão
const defaultLocale = localStorage.getItem('locale') || 'pt-BR'

// Cria a instância do i18n
export const i18n = createI18n({
  legacy: false, // Usa o modo Composition API
  locale: defaultLocale,
  fallbackLocale: 'pt-BR',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
    'pt-BR': ptBR,
  },
})

// Exporta a função t, para facilitar o uso em JS/TS
export const { t } = i18n.global

export default i18n
