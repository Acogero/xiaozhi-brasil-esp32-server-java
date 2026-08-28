<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ROUTES } from '@/router/routes'

const router = useRouter()
</script>

<template>
  <div class="about-container">
    <a-card title="Sobre o Sistema de Gerenciamento XiaoZhi ESP32" :bordered="false">
      <a-descriptions :column="1" bordered>
        <a-descriptions-item label="Nome do Sistema">Sistema de Gerenciamento XiaoZhi ESP32</a-descriptions-item>
        <a-descriptions-item label="Versão">Vue 3.0</a-descriptions-item>
        <a-descriptions-item label="Stack Tecnológico">
          Vue 3 + TypeScript + Vite + Ant Design Vue 4 + Pinia + VueUse
        </a-descriptions-item>
        <a-descriptions-item label="Framework de Desenvolvimento">
          <a-tag color="blue">Vue 3 Composition API</a-tag>
          <a-tag color="green">TypeScript</a-tag>
          <a-tag color="orange">Vite</a-tag>
        </a-descriptions-item>
      </a-descriptions>

      <a-divider />

      <a-space direction="vertical" :size="16" style="width: 100%">
        <a-alert
          message="Arquitetura Moderna"
          description="Utiliza os recursos mais recentes do Vue 3, com Composition API e TypeScript para uma melhor experiência de desenvolvimento"
          type="success"
          show-icon
        />
        <a-alert
          message="Integração com VueUse"
          description="Integra a biblioteca de utilitários VueUse, oferecendo armazenamento reativo, debounce, throttle e outras funcionalidades úteis"
          type="info"
          show-icon
        />
      </a-space>

      <div style="margin-top: 24px">
        <a-button type="primary" @click="router.push(ROUTES.DASHBOARD)">Voltar ao Painel</a-button>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.about-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}
</style>