<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { UserOutlined, CameraOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { useUserStore } from '@/store/user'
import { useAvatar } from '@/composables/useAvatar'
import { updateUser } from '@/services/user'
import { uploadFile } from '@/services/upload'
import type { User, UpdateUserParams } from '@/types/user'
import type { UploadProps } from 'ant-design-vue'

const { t } = useI18n()

const userStore = useUserStore()
const { getAvatarUrl } = useAvatar()

const userInfo = computed(() => userStore.userInfo || {})
const avatarUrl = computed(() => getAvatarUrl(userInfo.value.avatar))
// Estado relacionado ao upload de avatar
const avatarLoading = ref(false)

// Relacionado ao formulário
const formRef = ref<FormInstance>()
const formData = reactive({
  name: userInfo.value.name || '',
  tel: userInfo.value.tel || '',
  email: userInfo.value.email || '',
  password: '',
  confirmPassword: '',
})

// Força da senha
const passwordLevel = ref(0)
const passwordLevelVisible = ref(false)

const levelNames = computed(() => [t('account.passwordLevel.low'), t('account.passwordLevel.low'), t('account.passwordLevel.medium'), t('account.passwordLevel.strong')])
const levelColors = ['#ff0000', '#ff0000', '#ff7e05', '#52c41a']
const passwordLevelName = computed(() => levelNames.value[passwordLevel.value])
const passwordLevelColor = computed(() => levelColors[passwordLevel.value])
const passwordPercent = computed(() => {
  if (passwordLevel.value === 0) return 10
  if (passwordLevel.value === 3) return 100
  return passwordLevel.value * 30
})

// Regras de validação do formulário
const rules: Record<string, Rule[]> = {
  name: [],
  tel: [
    {
      pattern: /^1[3456789]\d{9}$/,
      message: t('validation.phone'),
      trigger: ['blur', 'change']
    }
  ],
  email: [
    {
      type: 'email',
      message: t('validation.email'),
      trigger: ['blur', 'change']
    }
  ],
  password: [
    {
      validator: (_rule, value) => {
        if (!value) {
          passwordLevel.value = 0
          return Promise.resolve()
        }

        let level = 0
        // Número
        if (/[0-9]/.test(value)) level++
        // Letra
        if (/[a-zA-Z]/.test(value)) level++
        // Símbolo especial
        if (/[^0-9a-zA-Z_]/.test(value)) level++
        // Comprimento
        if (value.length < 6) level = 0

        passwordLevel.value = level

        if (level >= 2) {
          return Promise.resolve()
        }
        return Promise.reject(t('account.validation.passwordStrength'))
      },
      trigger: ['blur', 'change']
    }
  ],
  confirmPassword: [
    {
      validator: (_rule, value) => {
        if (value && formData.password && value !== formData.password) {
          return Promise.reject(t('account.validation.passwordMismatch'))
        }
        return Promise.resolve()
      },
      trigger: ['blur', 'change']
    }
  ]
}

// Foco no campo de senha
const handlePasswordFocus = () => {
  passwordLevelVisible.value = true
}

// Desfoco no campo de senha
const handlePasswordBlur = () => {
  setTimeout(() => {
    passwordLevelVisible.value = false
  }, 200)
}

// Enviar formulário
const submitLoading = ref(false)
const handleSubmit = async () => {
  try {
    await formRef.value?.validate()

    submitLoading.value = true

    const updateData: UpdateUserParams = {
      userId: userInfo.value.userId,
      username: userInfo.value.username,
      name: formData.name,
      tel: formData.tel,
      email: formData.email,
    }

    // Só envia o campo de senha se ele foi preenchido
    if (formData.password) {
      updateData.password = formData.password
    }
    
    const res = await updateUser(updateData)
    
    if (res.code === 200) {
      // Atualiza as informações locais do usuário
      const userData = res.data as Partial<User>
      userStore.updateUserInfo({
        ...userData,
        state: userData.state?.toString(),
        isAdmin: userData.isAdmin?.toString()
      })
      message.success(t('account.updateSuccess'))
      
      // Limpa o campo de senha
      formData.password = ''
      formData.confirmPassword = ''
      passwordLevel.value = 0
    } else {
      message.error(res.message || t('account.updateFailed'))
    }
  } catch (error) {
    console.error('Falha na validação do formulário:', error)
  } finally {
    submitLoading.value = false
  }
}

// Verificação antes do upload do avatar
const beforeAvatarUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    message.error(t('common.onlyImageFiles'))
    return false
  }
  if (!isLt2M) {
    message.error(t('common.imageSizeLimit'))
    return false
  }

  avatarLoading.value = true
  uploadFile(file, 'avatar')
    .then(url => {
      updateUserAvatar(url as string)
    })
    .catch(error => {
      message.error(t('common.avatarUploadFailed') + error)
      avatarLoading.value = false
    })

  return false
}

// Atualiza o avatar do usuário
const updateUserAvatar = async (avatarUrl: string) => {
  try {
    // Converte a URL completa em caminho relativo para armazenamento
    const relativePath = getRelativePath(avatarUrl)
    
    const updateData: UpdateUserParams = {
      userId: userInfo.value.userId,
      username: userInfo.value.username,
      avatar: relativePath
    } as UpdateUserParams
    
    const res = await updateUser(updateData)
    
    if (res.code === 200) {
      // Atualiza as informações locais do usuário, armazenando o caminho relativo
      userStore.updateUserInfo({
        ...userInfo.value,
        avatar: relativePath
      })
      message.success(t('common.avatarUploadSuccess'))
    } else {
      message.error(res.message || t('common.avatarUploadFailed'))
    }
  } catch (error) {
    message.error(t('common.avatarUploadFailed') + error)
  } finally {
    avatarLoading.value = false
  }
}

// Converte a URL completa em caminho relativo
const getRelativePath = (fullUrl: string): string => {
  if (!fullUrl) return ''
  
  // Se já for um caminho relativo, retorna diretamente
  if (!fullUrl.startsWith('http://') && !fullUrl.startsWith('https://')) {
    return fullUrl
  }
  
  // Extrai a parte do caminho relativo
  // Exemplo: http://192.168.5.165:8091/uploads/avatar/2025/10/06/xxx.jpg
  // Convertido para: uploads/avatar/2025/10/06/xxx.jpg
  try {
    const url = new URL(fullUrl)
    return url.pathname.startsWith('/') ? url.pathname.substring(1) : url.pathname
  } catch {
    // Se a análise da URL falhar, tenta um processamento simples de string
    const parts = fullUrl.split('/')
    const uploadIndex = parts.findIndex(part => part === 'uploads')
    if (uploadIndex !== -1) {
      return parts.slice(uploadIndex).join('/')
    }
    return fullUrl
  }
}
</script>

<template>
  <div class="account-view">
    <a-card :title="t('common.personalSettings')" :bordered="false">
      <a-row :gutter="24">
        <!-- Esquerda: edição de informações -->
        <a-col :xs="24" :lg="12">
          <a-form
            ref="formRef"
            :model="formData"
            :rules="rules"
            layout="vertical"
            @finish="handleSubmit"
          >
            <a-form-item :label="t('common.name')" name="name">
              <a-input
                v-model:value="formData.name"
                :placeholder="t('account.enterName')"
              />
            </a-form-item>

            <a-form-item :label="t('account.phone')" name="tel">
              <a-input
                v-model:value="formData.tel"
                :placeholder="t('account.enterPhone')"
              />
            </a-form-item>

            <a-form-item :label="t('account.email')" name="email">
              <a-input
                v-model:value="formData.email"
                :placeholder="t('account.enterEmail')"
                allow-clear
              />
            </a-form-item>

            <a-form-item :label="t('account.password')" name="password">
              <a-popover
                v-model:open="passwordLevelVisible"
                placement="right"
                trigger="focus"
              >
                <template #content>
                  <div style="width: 240px">
                    <div :style="{ color: passwordLevelColor, marginBottom: '8px' }">
                      {{ t('account.passwordStrength') }}：<strong>{{ passwordLevelName }}</strong>
                    </div>
                    <a-progress
                      :percent="passwordPercent"
                      :show-info="false"
                      :stroke-color="passwordLevelColor"
                    />
                    <div style="margin-top: 10px; font-size: 12px; color: var(--ant-color-text-secondary)">
                      {{ t('account.passwordTip') }}
                    </div>
                  </div>
                </template>
                <a-input-password
                  v-model:value="formData.password"
                  :placeholder="t('account.passwordPlaceholder')"
                  allow-clear
                  @focus="handlePasswordFocus"
                  @blur="handlePasswordBlur"
                />
              </a-popover>
            </a-form-item>

            <a-form-item :label="t('account.confirmPassword')" name="confirmPassword">
              <a-input-password
                v-model:value="formData.confirmPassword"
                :placeholder="t('account.confirmPasswordPlaceholder')"
                allow-clear
              />
            </a-form-item>

            <a-form-item>
              <a-button
                v-permission="'system:setting:account:update'"
                type="primary"
                html-type="submit"
                :loading="submitLoading"
              >
                {{ t('common.save') }}
              </a-button>
            </a-form-item>
          </a-form>
        </a-col>

        <!-- Direita: pré-visualização do avatar -->
        <a-col :xs="24" :lg="12">
          <div class="avatar-section">
            <a-upload
              v-if="userStore.hasPermission('system:setting:account:update')"
              name="file"
              :show-upload-list="false"
              :before-upload="beforeAvatarUpload"
              accept=".jpg,.jpeg,.png,.gif"
              class="avatar-uploader"
            >
              <div class="avatar-preview">
                <a-avatar :src="avatarUrl" :size="180">
                  <template #icon><UserOutlined /></template>
                </a-avatar>
                <div class="avatar-mask">
                  <a-spin v-if="avatarLoading" size="large" />
                  <CameraOutlined v-else :style="{ fontSize: '32px' }" />
                </div>
              </div>
            </a-upload>
            <div v-else class="avatar-preview avatar-preview--readonly">
              <a-avatar :src="avatarUrl" :size="180">
                <template #icon><UserOutlined /></template>
              </a-avatar>
            </div>
            <div class="avatar-tips">
              <p>{{ userStore.hasPermission('system:setting:account:update') ? t('common.clickToChangeAvatar') : t('common.avatar') }}</p>
              <p style="color: var(--ant-color-text-tertiary); font-size: 12px">{{ t('common.avatarFormatTip') }}</p>
            </div>
          </div>
        </a-col>
      </a-row>
    </a-card>
  </div>
</template>

<style scoped lang="scss">
.account-view {
  padding: 16px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 40px;

  .avatar-uploader {
    cursor: pointer;
  }

  .avatar-preview {
    position: relative;
    transition: all 0.3s;

    &:hover {
      transform: scale(1.05);

      .avatar-mask {
        opacity: 1;
      }
    }

    .avatar-mask {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.4);
      border-radius: 50%;
      opacity: 0;
      transition: opacity 0.3s;
      color: #fff;
    }
  }

  .avatar-preview--readonly:hover {
    transform: none;
  }

  .avatar-tips {
    margin-top: 24px;
    text-align: center;

    p {
      margin: 4px 0;
    }
  }
}

:deep(.ant-form-vertical .ant-form-item) {
  margin-bottom: 24px;
}
</style>
