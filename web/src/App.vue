<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { RouterView } from 'vue-router'
import GlobalLoading from './components/GlobalLoading.vue'
import ErrorBoundary from './components/ErrorBoundary.vue'
import { useLocale } from './composables/useLocale'
import { useAntdTheme } from './composables/useAntdTheme'
import { useAppStore } from './store/app'

const { antdLocale } = useLocale()
const { antdTheme } = useAntdTheme()
const appStore = useAppStore()

// Inicializa e observa mudanças no tamanho da janela
onMounted(() => {
  appStore.updateScreenSize()
  window.addEventListener('resize', appStore.updateScreenSize)
})

onUnmounted(() => {
  window.removeEventListener('resize', appStore.updateScreenSize)
})
</script>

<template>
  <a-config-provider :locale="antdLocale" :theme="antdTheme">
    <div id="app">
      <!-- Componente de Loading global -->
      <GlobalLoading />
      
      <!-- Limite de erro -->
      <ErrorBoundary>
        <!-- Visualização de rota -->
        <RouterView />
      </ErrorBoundary>
    </div>
  </a-config-provider>
</template>

<style>
#app {
  width: 100%;
  height: 100%;
}
</style>
