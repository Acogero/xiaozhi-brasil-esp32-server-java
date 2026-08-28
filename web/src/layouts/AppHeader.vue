<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { useAvatar } from '@/composables/useAvatar'
import { useLocale } from '@/composables/useLocale'
import { useAntdTheme } from '@/composables/useAntdTheme'
import { ROUTES } from '@/router/routes'

const { t } = useI18n()
import { 
  UserOutlined, 
  SettingOutlined, 
  LogoutOutlined, 
  GlobalOutlined,
  BgColorsOutlined,
  BulbOutlined,
  DesktopOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const { getAvatarUrl } = useAvatar()
const { currentLocale, localeName, setLocale, availableLocales, localeNames } = useLocale()
const { themeMode, actualTheme, toggleTheme, setTheme } = useAntdTheme()

// Informações do usuário
const user = computed(() => userStore.userInfo || {})

// URL do avatar
const avatarUrl = computed(() => getAvatarUrl(user.value.avatar))
console.log(avatarUrl.value)
// Ícone do tema
const themeIcon = computed(() => {
  switch (actualTheme.value) {
    case 'light':
      return BgColorsOutlined
    case 'dark':
      return BulbOutlined
    default:
      return DesktopOutlined
  }
})

// Texto do tema
const themeText = computed(() => {
  switch (themeMode.value) {
    case 'light':
      return t('component.settings.theme.light')
    case 'dark':
      return t('component.settings.theme.dark')
    case 'auto':
      return t('component.settings.theme.auto')
    default:
      return t('component.settings.theme.title')
  }
})

/**
 * Sair da conta
 */
function handleLogout() {
  // Limpa as informações do usuário
  userStore.clearUserInfo()
  // Limpa o token
  userStore.clearToken()
  // Redireciona para a página de login
  router.push(ROUTES.LOGIN)
}

/**
 * Redireciona para o centro pessoal
 */
function goToAccount() {
  router.push(ROUTES.SETTING_ACCOUNT)
}

/**
 * Redireciona para as configurações pessoais
 */
function goToSettings() {
  router.push(ROUTES.SETTING_CONFIG)
}

/**
 * Alterna o idioma
 */
function handleLocaleChange(locale: string) {
  setLocale(locale as 'zh-CN' | 'en-US' | 'pt-BR')
}

/**
 * Alterna o tema
 */
function handleThemeChange(theme: string) {
  setTheme(theme as 'light' | 'dark' | 'auto')
}
</script>

<template>
  <div class="app-header">
    <div class="header-left">
      <!-- Espaço reservado à esquerda, pode receber breadcrumbs, etc. -->
    </div>
    
    <div class="header-right">
      <!-- Seleção de idioma -->
      <a-dropdown class="locale-dropdown">
        <a-button type="text" class="header-btn">
          <GlobalOutlined />
          <span class="btn-text">{{ localeName }}</span>
        </a-button>
        
        <template #overlay>
          <a-menu @click="({ key }: { key: string }) => handleLocaleChange(key)">
            <a-menu-item 
              v-for="locale in availableLocales" 
              :key="locale"
              :class="{ 'ant-menu-item-selected': currentLocale === locale }"
            >
              {{ localeNames[locale] }}
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>

      <!-- Alternância de tema -->
      <a-dropdown class="theme-dropdown">
        <a-button type="text" class="header-btn">
          <component :is="themeIcon" />
          <span class="btn-text">{{ themeText }}</span>
        </a-button>
        
        <template #overlay>
          <a-menu @click="({ key }: { key: string }) => handleThemeChange(key)">
            <a-menu-item 
              key="light"
              :class="{ 'ant-menu-item-selected': themeMode === 'light' }"
            >
              <BgColorsOutlined />
              <span class="menu-text">{{ t('component.settings.theme.light') }}</span>
            </a-menu-item>
            <a-menu-item 
              key="dark"
              :class="{ 'ant-menu-item-selected': themeMode === 'dark' }"
            >
              <BulbOutlined />
              <span class="menu-text">{{ t('component.settings.theme.dark') }}</span>
            </a-menu-item>
            <a-menu-item 
              key="auto"
              :class="{ 'ant-menu-item-selected': themeMode === 'auto' }"
            >
              <DesktopOutlined />
              <span class="menu-text">{{ t('component.settings.theme.auto') }}</span>
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>

      <!-- Menu suspenso de informações do usuário -->
      <a-dropdown class="user-dropdown">
        <div class="user-info">
          <a-avatar
            :src="avatarUrl"
            size="small"
            class="user-avatar"
          >
            <template #icon>
              <UserOutlined />
            </template>
          </a-avatar>
          <span class="user-name">{{ user?.name || t('common.user') }}</span>
        </div>
        
        <template #overlay>
          <a-menu>
            <a-menu-item @click="() => goToAccount()">
              <UserOutlined />
              <span class="menu-text">{{ t('common.personalCenter') }}</span>
            </a-menu-item>
            <!-- Configurações pessoais (temporariamente desativado)
            <a-menu-item @click="() => goToSettings()">
              <SettingOutlined />
              <span class="menu-text">{{ t('common.personalSettings') }}</span>
            </a-menu-item>
            -->
            <a-menu-divider />
            <a-menu-item @click="() => handleLogout()">
              <LogoutOutlined />
              <span class="menu-text">{{ t('common.logout') }}</span>
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </div>
  </div>
</template>

<style scoped lang="scss">
.app-header {
  box-shadow: 0 2px 8px var(--ant-color-border-secondary);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 10;
  height: 48px;
  padding: 0 40px;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  height: 100%;
  gap: 8px;
  
  .header-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    height: 32px;
    padding: 0 8px;
    border-radius: 6px;
    transition: all 0.2s;
    
    &:hover {
      background-color: var(--ant-color-fill-tertiary);
    }
    
    .btn-text {
      font-size: 14px;
      color: var(--ant-color-text);
    }
  }
  
  .locale-dropdown,
  .theme-dropdown {
    .header-btn {
      min-width: 80px;
    }
  }
  
  .user-dropdown {
    cursor: pointer;
    padding: 0 12px;
    gap: 10px;
    display: flex;
    
    .user-info {
      display: flex;
      align-items: center;
      
      .user-avatar {
        margin-right: 8px;
      }
      
      .user-name {
        color: var(--ant-color-text);
      }
    }
  }
}

.menu-text {
  margin-left: 8px;
  color: var(--ant-color-text-secondary);
}
</style>

