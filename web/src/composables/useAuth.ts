import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { useStorage } from '@vueuse/core'
import { useUserStore } from '@/store/user'
import { useCountdown } from '@/composables/useCountdown'
import { login as loginApi, register as registerApi, resetPassword as resetPasswordApi, telLogin as telLoginApi, sendSmsCaptcha } from '@/services/user'
import { encrypt, decrypt } from '@/utils/jsencrypt'
import { ROUTES } from '@/router/routes'

interface LoginForm {
  username: string
  password: string
  rememberMe?: boolean
}

interface RegisterForm {
  name: string
  username: string
  email: string
  tel?: string
  password: string
  confirmPassword: string
  verifyCode: string
  agreeTerms: boolean
}

interface ForgetPasswordForm {
  email: string
  verificationCode: string
  newPassword: string
  confirmPassword: string
}

interface MobileLoginForm {
  tel: string
  code: string
}

export function useAuth() {
  const router = useRouter()
  const userStore = useUserStore()
  const { t } = useI18n()
  const loading = ref(false)
  const sendCodeLoading = ref(false)

  const rememberedUsername = useStorage('username', '', localStorage)
  const rememberedPassword = useStorage('rememberMe', '', localStorage)

  // Usa o composable de contagem regressiva
  const {
    count: countdown,
    counting,
    countdownText,
    start: startCountdown
  } = useCountdown({
    initialCount: 60
  })

  // Login
  const login = async (form: LoginForm) => {
    loading.value = true
    try {
      const res = await loginApi({
        username: form.username,
        password: form.password,
      })

      if (res.code === 200) {
        // Define as informações do usuário
        userStore.setUserInfo(res.data.user)
        // Define as informações de permissões
        userStore.setPermissions(res.data.permissions)
        // Define as informações de papéis/permissões do backend
        userStore.setAuthRole(res.data.authRole)
        // Define o token
        userStore.setToken(res.data.token)
        userStore.setRefreshToken(res.data.refreshToken)

        if (form.rememberMe) {
          rememberedUsername.value = form.username
          const encryptedPassword = encrypt(form.password)
          if (encryptedPassword) {
            rememberedPassword.value = encryptedPassword
          }
        } else {
          rememberedUsername.value = ''
          rememberedPassword.value = ''
        }

        message.success(t('auth.loginSuccess'))
        
        // Redireciona para páginas diferentes conforme o tipo de usuário
        // Administradores vão para o dashboard, usuários comuns vão para agents
        const isAdmin = res.data.user && res.data.user.isAdmin === '1'
        const defaultRoute = isAdmin ? ROUTES.DASHBOARD : ROUTES.DEVICE
        
        // Obtém o caminho de redirecionamento
        let redirect = router.currentRoute.value.query.redirect as string || defaultRoute
        
        // Verifica se o usuário tem permissão para acessar o caminho de redirecionamento
        if (redirect && redirect !== defaultRoute) {
          const targetRoute = router.resolve(redirect)
          if (targetRoute && targetRoute.meta) {
            // Verifica se é necessária permissão de administrador
            if (targetRoute.meta.isAdmin && !isAdmin) {
              redirect = defaultRoute
            }
            // Verifica uma permissão específica
            else if (targetRoute.meta.permission) {
              const hasPermission = userStore.hasPermission(targetRoute.meta.permission as string)
              if (!hasPermission) {
                redirect = defaultRoute
              }
            }
            // Verifica múltiplas permissões (basta uma)
            else if (targetRoute.meta.permissions && Array.isArray(targetRoute.meta.permissions)) {
              const hasAnyPermission = userStore.hasAnyPermission(targetRoute.meta.permissions as string[])
              if (!hasAnyPermission) {
                redirect = defaultRoute
              }
            }
          }
        }
        
        // Redireciona para a página especificada
        router.push(redirect)
        return true
      } else {
        message.error(res.message || t('auth.loginFailed'))
        return false
      }
    } catch (error) {
      message.error(t('auth.loginFailed'))
      return false
    } finally {
      loading.value = false
    }
  }

  // Registro
  const register = async (form: RegisterForm) => {
    loading.value = true
    try {
      const res = await registerApi({
        name: form.name,
        username: form.username,
        email: form.email,
        tel: form.tel,
        password: form.password,
        verifyCode: form.verifyCode,
      })

      if (res.code === 200) {
        message.success(t('auth.registerSuccess'))
        setTimeout(() => {
          router.push(ROUTES.LOGIN)
        }, 500)
        return true
      } else {
        message.error(res.message || t('common.error'))
        return false
      }
    } catch (error) {
      message.error(t('common.error'))
      return false
    } finally {
      loading.value = false
    }
  }

  // Redefinir senha
  const resetPassword = async (form: ForgetPasswordForm) => {
    loading.value = true
    try {
      const res = await resetPasswordApi({
        email: form.email,
        code: form.verificationCode,
        password: form.newPassword,
      })

      if (res.code === 200) {
        message.success(t('auth.passwordReset'))
        setTimeout(() => {
          router.push(ROUTES.LOGIN)
        }, 500)
        return true
      } else {
        message.error(res.message || t('common.error'))
        return false
      }
    } catch (error) {
      message.error(t('common.error'))
      return false
    } finally {
      loading.value = false
    }
  }

  const getRememberedCredentials = () => {
    const username = rememberedUsername.value
    const encryptedPassword = rememberedPassword.value
    const password = encryptedPassword ? decrypt(encryptedPassword) : ''

    return {
      username,
      password: typeof password === 'string' ? password : '',
      rememberMe: !!encryptedPassword,
    }
  }

  // Login por código de verificação via celular
  const telLogin = async (form: MobileLoginForm) => {
    loading.value = true
    try {
      const res = await telLoginApi({
        tel: form.tel,
        code: form.code,
      })

      if (res.code === 200) {
        // Define as informações do usuário
        userStore.setUserInfo(res.data.user)
        // Define as informações de permissões
        userStore.setPermissions(res.data.permissions)
        // Define as informações de papéis/permissões do backend
        userStore.setAuthRole(res.data.authRole)
        // Define o token
        userStore.setToken(res.data.token)
        userStore.setRefreshToken(res.data.refreshToken)

        message.success(t('auth.loginSuccess'))
        
        // Redireciona para páginas diferentes conforme o tipo de usuário
        const isAdmin = res.data.user && res.data.user.isAdmin === '1'
        const defaultRoute = isAdmin ? ROUTES.DASHBOARD : ROUTES.DEVICE
        
        // Obtém o caminho de redirecionamento
        let redirect = router.currentRoute.value.query.redirect as string || defaultRoute
        
        // Verifica se o usuário tem permissão para acessar o caminho de redirecionamento
        if (redirect && redirect !== defaultRoute) {
          const targetRoute = router.resolve(redirect)
          if (targetRoute && targetRoute.meta) {
            // Verifica se é necessária permissão de administrador
            if (targetRoute.meta.isAdmin && !isAdmin) {
              redirect = defaultRoute
            }
            // Verifica uma permissão específica
            else if (targetRoute.meta.permission) {
              const hasPermission = userStore.hasPermission(targetRoute.meta.permission as string)
              if (!hasPermission) {
                redirect = defaultRoute
              }
            }
            // Verifica múltiplas permissões (basta uma)
            else if (targetRoute.meta.permissions && Array.isArray(targetRoute.meta.permissions)) {
              const hasAnyPermission = userStore.hasAnyPermission(targetRoute.meta.permissions as string[])
              if (!hasAnyPermission) {
                redirect = defaultRoute
              }
            }
          }
        }
        
        router.push(redirect)
        return true
      } else if (res.code === 201) {
        // Número de celular não cadastrado
        message.warning(res.message)
        router.push(ROUTES.REGISTER)
        return false
      } else {
        message.error(res.message || t('auth.loginFailed'))
        return false
      }
    } catch (error: any) {
      if (error && error.code === 201) {
        message.warning(error.message)
        router.push(ROUTES.REGISTER)
      } else {
        message.error(error?.message || t('auth.loginFailed'))
      }
      return false
    } finally {
      loading.value = false
    }
  }

  // Enviar código de verificação por SMS
  const sendVerificationCode = async (tel: string) => {
    if (!tel) {
      message.error(t('auth.enterMobilePhone'))
      return false
    }

    if (sendCodeLoading.value || counting.value) {
      return false
    }

    sendCodeLoading.value = true
    try {
      const res = await sendSmsCaptcha({
        tel,
        type: 'login',
      })

      if (res.code === 200) {
        message.success(t('auth.verificationCodeSent'))
        // Inicia a contagem regressiva (60 segundos)
        startCountdown(60)
        return true
      } else {
        message.error(res.message || t('auth.sendVerificationCodeFailed'))
        return false
      }
    } catch (error) {
      message.error(t('auth.sendVerificationCodeFailed'))
      return false
    } finally {
      sendCodeLoading.value = false
    }
  }

  const logout = () => {
    userStore.clearUserInfo()
    userStore.clearToken()
    router.push(ROUTES.LOGIN)
  }

  return {
    loading,
    sendCodeLoading,
    countdown,
    counting,
    countdownText,
    login,
    telLogin,
    register,
    resetPassword,
    sendVerificationCode,
    getRememberedCredentials,
    logout,
  }
}
