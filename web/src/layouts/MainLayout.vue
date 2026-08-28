<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'
import AppFooter from './AppFooter.vue'
import { ROUTES } from '@/router/routes'
import FloatingChat from '@/components/FloatingChat.vue'
import PageSkeleton from '@/components/PageSkeleton.vue'

const router = useRouter()
const userStore = useUserStore()

// Controle de largura da barra lateral
const sidebarWidth = ref(200)
const isCollapsed = ref(false)

// Largura do cliente
const clientWidth = ref(document.body.clientWidth)

// Se é dispositivo móvel
const isMobile = computed(() => clientWidth.value < 768)

// Informações do usuário
const userInfo = computed(() => userStore.userInfo)

/**
 * Trata a mudança de tamanho da janela
 */
function handleResize() {
  clientWidth.value = document.body.clientWidth
  userStore.setMobileType(isMobile.value)
}

/**
 * Trata a mudança de breakpoint (layout responsivo)
 */
function handleBreakpoint(broken: boolean) {
  if (broken) {
    // Tela pequena - recolhe a barra lateral automaticamente
    sidebarWidth.value = 80
    isCollapsed.value = true
  } else {
    // Tela grande - expande a barra lateral
    sidebarWidth.value = 200
    isCollapsed.value = false
  }
}

onMounted(() => {
  // Monitora a mudança de tamanho da janela
  window.addEventListener('resize', handleResize)
  handleResize()
  
  // Verifica o status de login
  if (!userInfo.value) {
    router.push(ROUTES.LOGIN)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="main-layout">
    <a-layout>
      <!-- Sider de espaço reservado - usado para manter a posição da área de conteúdo -->
      <div
        class="sider-placeholder"
        :style="{
          width: `${sidebarWidth}px`,
          flex: `0 0 ${sidebarWidth}px`,
          maxWidth: `${sidebarWidth}px`,
          minWidth: `${sidebarWidth}px`,
        }"
      />

      <!-- Barra lateral fixa -->
      <a-layout-sider
        v-model:collapsed="isCollapsed"
        theme="light"
        breakpoint="lg"
        :collapsed-width="80"
        :width="200"
        @breakpoint="handleBreakpoint"
        collapsible
        class="fixed-sidebar"
      >
        <AppSidebar :collapsed="isCollapsed" />
      </a-layout-sider>

      <!-- Área de conteúdo principal -->
      <a-layout class="main-content-layout">
        <!-- Barra superior -->
        <a-layout-header class="layout-header">
          <AppHeader />
        </a-layout-header>

        <!-- Área de conteúdo -->
        <a-layout-content class="layout-content">
        <router-view v-slot="{ Component }">
          <Suspense>
            <template #default>
              <component :is="Component" :key="$route.fullPath" />
            </template>
            <template #fallback>
              <PageSkeleton />
            </template>
          </Suspense>
        </router-view>
        </a-layout-content>

        <!-- Rodapé -->
        <a-layout-footer class="layout-footer">
          <AppFooter />
        </a-layout-footer>
      </a-layout>
    </a-layout>

    <!-- Componente de chat flutuante -->
    <FloatingChat />
  </div>
</template>

<style scoped lang="scss">
.main-layout {
  width: 100%;
  min-height: 100vh;
}

.sider-placeholder {
  overflow: hidden;
  transition: all 0.2s;
}

.fixed-sidebar {
  height: 100vh;
  z-index: 99;
  position: fixed;
  left: 0;
  overflow: auto;

  :deep(.ant-layout-sider-children) {
    display: flex;
    flex-direction: column;
  }
}

.main-content-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.layout-header {
  padding: 0;
  height: auto;
  line-height: normal;
  background: var(--ant-color-bg-container);
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.03),
      0 1px 6px -1px rgba(0, 0, 0, 0.02),
      0 2px 4px 0 rgba(0, 0, 0, 0.02);
  flex-shrink: 0;
}

.layout-content {
  flex: 1;
  position: relative;
}


.layout-footer {
  padding: 0;
  background: transparent;
  flex-shrink: 0;
}
</style>

