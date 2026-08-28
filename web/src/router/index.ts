import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'

// Estende o tipo RouteMeta
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    requiresAuth?: boolean
    isAdmin?: boolean
    parent?: string
    hideInMenu?: boolean
    permission?: string // Permissão única
    permissions?: string[] // Múltiplas permissões (basta ter uma)
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: {
      title: 'router.title.login',
      requiresAuth: false,
    },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: {
      title: 'router.title.register',
      requiresAuth: false,
    },
  },
  {
    path: '/forget',
    name: 'forget',
    component: () => import('../views/ForgetView.vue'),
    meta: {
      title: 'router.title.forget',
      requiresAuth: false,
    },
  },

  // Rotas do aplicativo principal
  {
    path: '/',
    component: MainLayout,
    // redirect: '/dashboard', // Tratado dinamicamente na guarda de rota
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/DashboardView.vue'),
        meta: {
          title: 'router.title.dashboard',
          icon: 'DashboardOutlined',
          requiresAuth: true,
          permission: 'system:dashboard',
        },
      },
      {
        path: 'user',
        name: 'user',
        component: () => import('../views/UserView.vue'),
        meta: {
          title: 'router.title.user',
          icon: 'TeamOutlined',
          requiresAuth: true,
          permission: 'system:user',
        },
      },
      {
        path: 'device',
        name: 'device',
        component: () => import('../views/DeviceView.vue'),
        meta: {
          title: 'router.title.device',
          icon: 'RobotOutlined',
          requiresAuth: true,
          permission: 'system:device',
        },
      },
      {
        path: 'role',
        name: 'role',
        component: () => import('../views/RoleView.vue'),
        meta: {
          title: 'router.title.role',
          icon: 'UserAddOutlined',
          requiresAuth: true,
          permission: 'system:role',
        },
      },
      {
        path: 'template',
        name: 'template',
        component: () => import('../views/TemplateView.vue'),
        meta: {
          title: 'router.title.template',
          icon: 'SnippetsOutlined',
          parent: 'router.parent.roleManagement',
          requiresAuth: true,
          permission: 'system:prompt-template',
          hideInMenu: true
        },
      },
      {
        path: 'memory/chat',
        name: 'memory-chat',
        component: () => import('../views/MemoryManagementView.vue'),
        meta: {
          title: 'router.title.shortTermMemory',
          parent: 'router.parent.memoryManagement',
          requiresAuth: true,
          permission: 'system:role',
        },
      },
      {
        path: 'memory/summary',
        name: 'memory-summary',
        component: () => import('../views/MemoryManagementView.vue'),
        meta: {
          title: 'router.title.summaryMemory',
          parent: 'router.parent.memoryManagement',
          requiresAuth: true,
          permission: 'system:role',
        },
      },
      // Gerenciamento de configuração
      {
        path: 'config/model',
        name: 'config-model',
        component: () => import('../views/config/ModelConfigView.vue'),
        meta: {
          title: 'router.title.modelConfig',
          parent: 'router.parent.configManagement',
          requiresAuth: true,
          permission: 'system:config',
        },
      },
      {
        path: 'config/agent',
        name: 'config-agent',
        component: () => import('../views/config/AgentView.vue'),
        meta: {
          title: 'router.title.agent',
          parent: 'router.parent.configManagement',
          requiresAuth: true,
          permission: 'system:config:agent',
        },
      },
      {
        path: 'config/stt',
        name: 'config-stt',
        component: () => import('../views/config/SttConfigView.vue'),
        meta: {
          title: 'router.title.sttConfig',
          parent: 'router.parent.configManagement',
          requiresAuth: true,
          permission: 'system:config',
        },
      },
      {
        path: 'config/tts',
        name: 'config-tts',
        component: () => import('../views/config/TtsConfigView.vue'),
        meta: {
          title: 'router.title.ttsConfig',
          parent: 'router.parent.configManagement',
          requiresAuth: true,
          permission: 'system:config',
        },
      },
      {
        path: 'config/oss',
        name: 'config-oss',
        component: () => import('../views/config/OssConfigView.vue'),
        meta: {
          title: 'router.title.ossConfig',
          parent: 'router.parent.configManagement',
          requiresAuth: true,
          permission: 'system:config',
        },
      },
      // Chat Web
      {
        path: 'chat',
        name: 'chat',
        component: () => import('../views/ChatView.vue'),
        meta: {
          title: 'router.title.chat',
          icon: 'MessageOutlined',
          requiresAuth: true,
          permission: 'system:chat',
        },
      },
      {
        path: 'auth-role',
        name: 'auth-role',
        component: () => import('../views/AuthRoleView.vue'),
        meta: {
          title: 'router.title.authRole',
          icon: 'SafetyCertificateOutlined',
          requiresAuth: true,
          permission: 'system:auth-role',
        },
      },
      // Central pessoal
      {
        path: 'setting/account',
        name: 'setting-account',
        component: () => import('../views/setting/AccountView.vue'),
        meta: {
          title: 'router.title.account',
          parent: 'router.parent.settings',
          requiresAuth: true,
          permission: 'system:setting',
        },
      },
      // Configurações pessoais (temporariamente desativado)
      // {
      //   path: 'setting/config',
      //   name: 'setting-config',
      //   component: () => import('../views/setting/ConfigView.vue'),
      //   meta: {
      //     title: 'router.title.personalConfig',
      //     parent: 'router.parent.settings',
      //     requiresAuth: true,
      //     permission: 'system:setting',
      //   },
      // },
    ],
  },

  // Páginas de exceção
  {
    path: '/403',
    name: '403',
    component: () => import('../views/exception/403.vue'),
    meta: {
      title: 'router.title.error403',
      requiresAuth: false,
    },
  },
  {
    path: '/404',
    name: '404',
    component: () => import('../views/exception/404.vue'),
    meta: {
      title: 'router.title.error404',
      requiresAuth: false,
    },
  },

  // Captura todas as rotas não correspondidas
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
