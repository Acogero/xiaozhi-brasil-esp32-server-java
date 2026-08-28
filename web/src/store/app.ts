import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useStorage } from '@vueuse/core'

/**
 * Tipo de idioma
 */
export type Locale = 'zh-CN' | 'en-US'

/**
 * Store de estado global da aplicação
 * Gerencia idioma, layout e outras configurações globais
 */
export const useAppStore = defineStore('app', () => {

  // ========== Gerenciamento de layout ==========
  // Estado de recolhimento da barra lateral
  const sidebarCollapsed = ref(false)
  
  // Se é dispositivo móvel
  const isMobile = ref(false)

  // Estilo de navegação
  const navigationStyle = useStorage<'tabs' | 'sidebar'>('navigation-style', 'sidebar')

  // Tamanho da tela
  const screenWidth = ref(window.innerWidth)
  const screenHeight = ref(window.innerHeight)

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  const setSidebarCollapsed = (collapsed: boolean) => {
    sidebarCollapsed.value = collapsed
  }

  const setMobile = (mobile: boolean) => {
    isMobile.value = mobile
  }

  const setNavigationStyle = (style: 'tabs' | 'sidebar') => {
    navigationStyle.value = style
  }

  const updateScreenSize = () => {
    screenWidth.value = window.innerWidth
    screenHeight.value = window.innerHeight
    isMobile.value = window.innerWidth < 768
  }

  // ========== Configurações de página ==========
  const pageTitle = ref<string>('')

  const setPageTitle = (title: string) => {
    pageTitle.value = title
    const baseTitle = import.meta.env.VITE_APP_TITLE || 'Connect Ai'
    document.title = title ? `${title} - ${baseTitle}` : baseTitle
  }

  return {
    // Layout
    sidebarCollapsed,
    isMobile,
    navigationStyle,
    screenWidth,
    screenHeight,
    toggleSidebar,
    setSidebarCollapsed,
    setMobile,
    setNavigationStyle,
    updateScreenSize,
    
    // Página
    pageTitle,
    setPageTitle,
  }
})

