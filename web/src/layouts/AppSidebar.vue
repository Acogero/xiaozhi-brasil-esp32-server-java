<script setup lang="ts">
import {useI18n} from 'vue-i18n'
import {useMenu} from '@/composables/useMenu'
import * as Icons from '@ant-design/icons-vue'

const { t } = useI18n()

const props = defineProps<{
  collapsed?: boolean
}>()

const { openKeys, selectedKeys, menuItems, handleOpenChange, handleMenuClick } = useMenu()

// Obtém o componente de ícone
function getIcon(iconName?: string) {
  if (!iconName) return null
  return (Icons as Record<string, unknown>)[iconName]
}

// Obtém o título do menu (com suporte a múltiplos idiomas)
function getMenuTitle(title?: string) {
  if (!title) return ''
  // Se for uma chave de múltiplos idiomas, traduz; caso contrário, retorna diretamente
  return title.startsWith('router.') ? t(title) : title
}
</script>

<template>
  <div class="sidebar-container">
    <!-- Logo -->
    <div class="sidebar-logo">
      <router-link to="/dashboard" class="logo-link">
        <img src="/logo.png" alt="Logo" class="logo-image" />
      </router-link>
    </div>

    <!-- Menu -->
    <a-menu
      :open-keys="openKeys"
      v-model:selected-keys="selectedKeys"
      mode="inline"
      class="sidebar-menu"
      @open-change="handleOpenChange"
    >
      <template v-for="item in menuItems" :key="item.path">
        <!-- Item sem submenu -->
        <a-menu-item v-if="!item.children || item.children.length === 0" :key="item.path" @click="() => handleMenuClick(item.path)">
          <template #icon>
            <component :is="getIcon(item.meta.icon)" v-if="item.meta.icon" />
          </template>
          <span>{{ getMenuTitle(item.meta.title) }}</span>
        </a-menu-item>

        <!-- Item com submenu -->
        <a-sub-menu v-else :key="`${item.path}`">
          <template #icon>
            <component :is="getIcon(item.meta.icon)" v-if="item.meta.icon" />
          </template>
          <template #title>{{ getMenuTitle(item.meta.title) }}</template>
          
          <a-menu-item
            v-for="child in item.children"
            :key="child.path"
            @click="() => handleMenuClick(child.path)"
          >
            <template #icon>
              <component :is="getIcon(child.meta.icon)" v-if="child.meta.icon" />
            </template>
            <span>{{ getMenuTitle(child.meta.title) }}</span>
          </a-menu-item>
        </a-sub-menu>
      </template>
    </a-menu>
  </div>
</template>

<style scoped lang="scss">
.sidebar-container {
  height: 100%;
  overflow: hidden auto;
}

.sidebar-logo {
  padding: 8px 16px;
  
  .logo-link {
    display: block;
    height: 48px;
    line-height: 48px;
    text-decoration: none;
    overflow: hidden;
    white-space: nowrap;
    
    .logo-image {
      height: 32px;
      margin: 8px 10px;
      vertical-align: middle;
    }
  }
}

.sidebar-menu {
  border-right-color: transparent;
  border-inline-end: none !important;
}
</style>

