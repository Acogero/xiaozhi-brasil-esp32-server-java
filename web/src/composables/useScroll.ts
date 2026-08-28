import { ref, computed, nextTick, onMounted, onBeforeUnmount, type Ref } from 'vue'

/**
 * Composable de gerenciamento de rolagem
 * Usado para gerenciar o comportamento de rolagem do contêiner
 */

export interface UseScrollOptions {
  /**
   * Referência do contêiner de rolagem
   */
  container?: Ref<HTMLElement | undefined>
  
  /**
   * Limite (em pixels) para considerar que chegou ao final; valores abaixo disso são considerados no final
   */
  bottomThreshold?: number
  
  /**
   * Limite (em pixels) para considerar que chegou ao topo; valores abaixo disso são considerados no topo
   */
  topThreshold?: number
  
  /**
   * Indica se o monitoramento de rolagem está habilitado
   */
  enableScrollListener?: boolean
  
  /**
   * Callback do evento de rolagem
   */
  onScroll?: (scrollInfo: ScrollInfo) => void
  
  /**
   * Callback ao chegar ao final
   */
  onReachBottom?: () => void
  
  /**
   * Callback ao chegar ao topo
   */
  onReachTop?: () => void
}

export interface ScrollInfo {
  scrollTop: number
  scrollLeft: number
  scrollHeight: number
  scrollWidth: number
  clientHeight: number
  clientWidth: number
}

export function useScroll(options: UseScrollOptions = {}) {
  const containerRef = options.container || ref<HTMLElement>()
  const bottomThreshold = options.bottomThreshold || 50
  const topThreshold = options.topThreshold || 10
  
  // Informações de rolagem
  const scrollTop = ref(0)
  const scrollLeft = ref(0)
  const scrollHeight = ref(0)
  const scrollWidth = ref(0)
  const clientHeight = ref(0)
  const clientWidth = ref(0)
  
  // Indica se está no final
  const isAtBottom = computed(() => {
    if (!containerRef.value) return false
    return scrollHeight.value - scrollTop.value - clientHeight.value <= bottomThreshold
  })
  
  // Indica se está no topo
  const isAtTop = computed(() => {
    return scrollTop.value <= topThreshold
  })
  
  // Indica se é possível rolar
  const isScrollable = computed(() => {
    return scrollHeight.value > clientHeight.value
  })
  
  // Percentual de rolagem
  const scrollPercentage = computed(() => {
    if (!isScrollable.value) return 0
    return Math.round((scrollTop.value / (scrollHeight.value - clientHeight.value)) * 100)
  })
  
  /**
   * Atualiza as informações de rolagem
   */
  const updateScrollInfo = () => {
    if (!containerRef.value) return
    
    const el = containerRef.value
    scrollTop.value = el.scrollTop
    scrollLeft.value = el.scrollLeft
    scrollHeight.value = el.scrollHeight
    scrollWidth.value = el.scrollWidth
    clientHeight.value = el.clientHeight
    clientWidth.value = el.clientWidth
    
    // Dispara o callback
    if (options.onScroll) {
      options.onScroll({
        scrollTop: scrollTop.value,
        scrollLeft: scrollLeft.value,
        scrollHeight: scrollHeight.value,
        scrollWidth: scrollWidth.value,
        clientHeight: clientHeight.value,
        clientWidth: clientWidth.value
      })
    }
    
    // Dispara o callback de chegada ao final
    if (isAtBottom.value && options.onReachBottom) {
      options.onReachBottom()
    }
    
    // Dispara o callback de chegada ao topo
    if (isAtTop.value && options.onReachTop) {
      options.onReachTop()
    }
  }
  
  /**
   * Rola até o final
   */
  const scrollToBottom = (smooth = true) => {
    nextTick(() => {
      if (!containerRef.value) return
      
      containerRef.value.scrollTo({
        top: containerRef.value.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto'
      })
      
      updateScrollInfo()
    })
  }
  
  /**
   * Rola até o topo
   */
  const scrollToTop = (smooth = true) => {
    nextTick(() => {
      if (!containerRef.value) return
      
      containerRef.value.scrollTo({
        top: 0,
        behavior: smooth ? 'smooth' : 'auto'
      })
      
      updateScrollInfo()
    })
  }
  
  /**
   * Rola até a posição especificada
   */
  const scrollTo = (options: { top?: number; left?: number; smooth?: boolean }) => {
    nextTick(() => {
      if (!containerRef.value) return
      
      containerRef.value.scrollTo({
        top: options.top,
        left: options.left,
        behavior: options.smooth !== false ? 'smooth' : 'auto'
      })
      
      updateScrollInfo()
    })
  }
  
  /**
   * Rola até o elemento especificado
   */
  const scrollToElement = (
    selector: string | HTMLElement,
    options: { block?: ScrollLogicalPosition; inline?: ScrollLogicalPosition; smooth?: boolean } = {}
  ) => {
    nextTick(() => {
      if (!containerRef.value) return
      
      let element: HTMLElement | null = null
      
      if (typeof selector === 'string') {
        element = containerRef.value.querySelector(selector)
      } else {
        element = selector
      }
      
      if (!element) {
        console.warn('Elemento alvo não encontrado:', selector)
        return
      }
      
      element.scrollIntoView({
        block: options.block || 'start',
        inline: options.inline || 'nearest',
        behavior: options.smooth !== false ? 'smooth' : 'auto'
      })
      
      updateScrollInfo()
    })
  }
  
  /**
   * Rola a distância especificada
   */
  const scrollBy = (options: { top?: number; left?: number; smooth?: boolean }) => {
    nextTick(() => {
      if (!containerRef.value) return
      
      containerRef.value.scrollBy({
        top: options.top || 0,
        left: options.left || 0,
        behavior: options.smooth !== false ? 'smooth' : 'auto'
      })
      
      updateScrollInfo()
    })
  }
  
  // Tratamento do evento de rolagem
  const handleScroll = () => {
    updateScrollInfo()
  }
  
  // Inicializa na montagem
  onMounted(() => {
    if (options.enableScrollListener !== false && containerRef.value) {
      containerRef.value.addEventListener('scroll', handleScroll)
      updateScrollInfo()
    }
  })
  
  // Limpa na desmontagem
  onBeforeUnmount(() => {
    if (containerRef.value) {
      containerRef.value.removeEventListener('scroll', handleScroll)
    }
  })
  
  return {
    // Referência do contêiner
    containerRef,
    
    // Informações de rolagem
    scrollTop,
    scrollLeft,
    scrollHeight,
    scrollWidth,
    clientHeight,
    clientWidth,
    
    // Propriedades computadas
    isAtBottom,
    isAtTop,
    isScrollable,
    scrollPercentage,
    
    // Métodos
    scrollToBottom,
    scrollToTop,
    scrollTo,
    scrollToElement,
    scrollBy,
    updateScrollInfo
  }
}

