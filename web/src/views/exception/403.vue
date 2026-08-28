<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ROUTES } from '@/router/routes'

const router = useRouter()
const userStore = useUserStore()

function goBack() {
  router.back()
}

function goHome() {
  // Limpar informações do usuário e token, para evitar redirecionamento em loop por permissão insuficiente
  userStore.clearUserInfo()
  userStore.clearToken()
  router.push(ROUTES.LOGIN)
}
</script>

<template>
  <div class="exception-page">
    <a-result
      status="403"
      title="403"
      sub-title="Desculpe, você não tem permissão para acessar esta página."
    >
      <template #extra>
        <a-space>
          <a-button type="primary" @click="() => goHome()">Voltar à Página Inicial</a-button>
          <a-button @click="() => goBack()">Voltar à Página Anterior</a-button>
        </a-space>
      </template>
    </a-result>
  </div>
</template>

<style scoped lang="scss">
.exception-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f0f2f5;
}
</style>

