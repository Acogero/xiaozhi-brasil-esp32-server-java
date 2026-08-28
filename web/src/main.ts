import './assets/main.css'
import './assets/theme.css'
import 'ant-design-vue/dist/reset.css'
import 'nprogress/nprogress.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'

import App from './App.vue'
import router from './router'
import { setupRouterGuards } from './router/guards'
import { setupErrorHandler } from './utils/errorHandler'
import { setupDirectives } from './directives'
import { i18n } from './locales'

const app = createApp(App)

// 1. Configurar tratamento global de erros
setupErrorHandler(app)

// 2. Usar plugins
app.use(createPinia())
app.use(router)
app.use(Antd)
app.use(i18n)

// 2.1 Registrar diretivas personalizadas
setupDirectives(app)

// 3. Configurar guardas de rota (validação de login, título da página, barra de progresso)
setupRouterGuards(router)

app.mount('#app')
