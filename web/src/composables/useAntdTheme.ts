import { useStorage, usePreferredDark } from '@vueuse/core'
import { computed, watch } from 'vue'
import { theme } from 'ant-design-vue'
import type { ThemeConfig } from 'ant-design-vue/es/config-provider/context'

export type ThemeMode = 'light' | 'dark' | 'auto'

// Configuração de tema escuro do Ant Design Vue
const darkTheme: ThemeConfig = {
  algorithm: theme.darkAlgorithm,
  token: {
    colorPrimary: '#1890ff',
    colorError: '#ff4d4f',
    colorSuccess: '#52c41a',
    colorWarning: '#faad14',
    colorBgBase: '#141414',
    colorBgContainer: '#1f1f1f',
    colorBgElevated: '#262626',
    colorBorder: '#434343',
    colorText: '#ffffff',
    colorTextSecondary: '#a6a6a6',
    colorTextTertiary: '#8c8c8c',
    colorTextQuaternary: '#595959',
    colorFillQuaternary: '#262626',
    colorFillTertiary: '#1f1f1f',
    colorErrorHover: '#ff7875',
  },
}

// Configuração de tema claro do Ant Design Vue
const lightTheme: ThemeConfig = {
  algorithm: theme.defaultAlgorithm,
  token: {
    colorPrimary: '#1890ff',
    colorError: '#ff4d4f',
    colorSuccess: '#52c41a',
    colorWarning: '#faad14',
    colorBgBase: '#ffffff',
    colorBgContainer: '#ffffff',
    colorBgElevated: '#ffffff',
    colorBorder: '#d9d9d9',
    colorText: '#000000',
    colorTextSecondary: '#666666',
    colorTextTertiary: '#999999',
    colorTextQuaternary: '#cccccc',
    colorFillQuaternary: '#fafafa',
    colorFillTertiary: '#f5f5f5',
    colorErrorHover: '#ff7875',
  },
}

/**
 * Injeta os tokens do tema nas variáveis CSS
 * Isso é para manter compatibilidade com o estilo var(--ant-xxx) usado no projeto
 * Observação: diferente da versão React, o Ant Design Vue não gera variáveis CSS automaticamente, sendo necessário injetá-las manualmente
 */
function injectCssVariables(isDark: boolean) {
  const root = document.documentElement
  const tokens = isDark ? darkTheme.token : lightTheme.token
  
  if (!tokens) return
  
  // Injeta todos os tokens como variáveis CSS
  Object.entries(tokens).forEach(([key, value]) => {
    // Converte camelCase para kebab-case
    const cssVarName = key.replace(/([A-Z])/g, '-$1').toLowerCase()
    root.style.setProperty(`--ant-${cssVarName}`, String(value))
  })
  
  // Adiciona algumas variáveis derivadas comumente usadas
  root.style.setProperty('--ant-color-primary-hover', isDark ? '#40a9ff' : '#40a9ff')
  root.style.setProperty('--ant-color-primary-bg', isDark ? '#111d2c' : '#e6f7ff')
  root.style.setProperty('--ant-color-success-bg', isDark ? '#162312' : '#f6ffed')
  root.style.setProperty('--ant-color-error-bg', isDark ? '#2c1618' : '#fff1f0')
  root.style.setProperty('--ant-color-warning-bg', isDark ? '#2b2111' : '#fffbe6')
  root.style.setProperty('--ant-color-text-inverse', '#ffffff')
  root.style.setProperty('--ant-color-white', '#ffffff')
  root.style.setProperty('--ant-color-text-placeholder', isDark ? '#595959' : '#bfbfbf')
  root.style.setProperty('--ant-box-shadow', isDark 
    ? '0 3px 6px -4px rgba(0, 0, 0, 0.48), 0 6px 16px 0 rgba(0, 0, 0, 0.32), 0 9px 28px 8px rgba(0, 0, 0, 0.20)'
    : '0 3px 6px -4px rgba(0, 0, 0, 0.12), 0 6px 16px 0 rgba(0, 0, 0, 0.08), 0 9px 28px 8px rgba(0, 0, 0, 0.05)'
  )
  root.style.setProperty('--ant-color-border-secondary', isDark ? '#303030' : '#f0f0f0')
}

export function useAntdTheme() {
  const themeMode = useStorage<ThemeMode>('theme-mode', 'auto')
  const prefersDark = usePreferredDark()

  // Calcula o tema efetivamente aplicado
  const actualTheme = computed<'light' | 'dark'>(() => {
    if (themeMode.value === 'auto') {
      return prefersDark.value ? 'dark' : 'light'
    }
    return themeMode.value
  })

  // Obtém a configuração de tema do Ant Design Vue
  const antdTheme = computed<ThemeConfig>(() => {
    return actualTheme.value === 'dark' ? darkTheme : lightTheme
  })

  // Observa mudanças de tema e injeta as variáveis CSS
  watch(actualTheme, (theme) => {
    injectCssVariables(theme === 'dark')
  }, { immediate: true })

  // Alterna o tema (ciclo: light -> dark -> auto)
  const toggleTheme = () => {
    if (themeMode.value === 'light') {
      themeMode.value = 'dark'
    } else if (themeMode.value === 'dark') {
      themeMode.value = 'auto'
    } else {
      themeMode.value = 'light'
    }
  }

  // Define um tema específico
  const setTheme = (theme: ThemeMode) => {
    themeMode.value = theme
  }

  // Obtém o ícone do tema
  const themeIcon = computed(() => {
    switch (themeMode.value) {
      case 'light':
        return '☀️'
      case 'dark':
        return '🌙'
      case 'auto':
        return '🔄'
      default:
        return '☀️'
    }
  })

  // Obtém o nome de exibição do tema
  const themeName = computed(() => {
    switch (themeMode.value) {
      case 'light':
        return 'Modo claro'
      case 'dark':
        return 'Modo escuro'
      case 'auto':
        return 'Seguir sistema'
      default:
        return 'Modo claro'
    }
  })

  return {
    themeMode,
    actualTheme,
    antdTheme,
    toggleTheme,
    setTheme,
    themeIcon,
    themeName,
  }
}

// Exemplo de uso:
// const { themeMode, actualTheme, antdTheme, toggleTheme } = useAntdTheme()
// 
// <a-config-provider :theme="antdTheme">
//   <button @click="toggleTheme">
//     {{ themeIcon }} {{ themeName }}
//   </button>
// </a-config-provider>
