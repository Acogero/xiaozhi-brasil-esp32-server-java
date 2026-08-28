import { useStorage } from '@vueuse/core'
import { computed, watch } from 'vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import enUS from 'ant-design-vue/es/locale/en_US'
import ptBR from 'ant-design-vue/es/locale/pt_BR'
import type { Locale } from 'ant-design-vue/es/locale'
import { i18n } from '@/locales'

export type LocaleType = 'zh-CN' | 'en-US' | 'pt-BR'

// Mapeamento de configuração de idiomas
const localeMap: Record<LocaleType, Locale> = {
  'zh-CN': zhCN,
  'en-US': enUS,
  'pt-BR': ptBR,
}

// Nomes de exibição dos idiomas (cada um em seu próprio idioma)
const localeNames: Record<LocaleType, string> = {
  'zh-CN': '简体中文',
  'en-US': 'English',
  'pt-BR': 'Português (Brasil)',
}

export function useLocale() {
  const currentLocale = useStorage<LocaleType>('locale', 'pt-BR')

  // Obtém o objeto locale do Ant Design Vue
  const antdLocale = computed(() => localeMap[currentLocale.value])

  // Obtém o nome de exibição do idioma atual
  const localeName = computed(() => localeNames[currentLocale.value])

  // Alterna entre os idiomas disponíveis, em sequência
  const toggleLocale = () => {
    const locales = Object.keys(localeMap) as LocaleType[]
    const currentIndex = locales.indexOf(currentLocale.value)
    currentLocale.value = locales[(currentIndex + 1) % locales.length]
  }

  // Define um idioma específico
  const setLocale = (locale: LocaleType) => {
    currentLocale.value = locale
  }

  // Observa mudanças no idioma e sincroniza com a instância do i18n
  watch(
    currentLocale,
    (newLocale) => {
      // Sincroniza a instância do i18n
      if (i18n && i18n.global) {
        i18n.global.locale.value = newLocale
      }
    },
    { immediate: true }
  )

  // Obtém todos os idiomas disponíveis
  const availableLocales = Object.keys(localeMap) as LocaleType[]

  return {
    currentLocale,
    antdLocale,
    localeName,
    toggleLocale,
    setLocale,
    availableLocales,
    localeNames,
  }
}
