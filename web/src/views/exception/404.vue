<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ROUTES } from '@/router/routes'

const router = useRouter()

function goBack() {
  router.back()
}

function goHome() {
  router.push(ROUTES.DASHBOARD)
}
</script>

<template>
  <div class="exception-page">
    <a-result
      status="404"
      title="404"
      sub-title="Desculpe, a página que você acessou não existe."
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

