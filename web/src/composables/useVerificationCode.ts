import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useIntervalFn } from '@vueuse/core'
import { useI18n } from 'vue-i18n'
import { checkUser, sendEmailCaptcha, checkCaptcha } from '@/services/user'

type VerificationType = 'register' | 'forget'

export function useVerificationCode() {
  const { t } = useI18n()
  const sendCodeLoading = ref(false)
  const countdown = ref(0)

  const { pause, resume, isActive } = useIntervalFn(
    () => {
      countdown.value--
      if (countdown.value <= 0) {
        pause()
      }
    },
    1000,
    { immediate: false }
  )

  const canSendCode = computed(() => !sendCodeLoading.value && countdown.value === 0)

  const buttonText = computed(() => {
    if (sendCodeLoading.value) return t('auth.sending')
    if (countdown.value > 0) return t('auth.resendAfter', { seconds: countdown.value })
    return t('auth.sendVerificationCode')
  })

  const startCountdown = (seconds = 60) => {
    countdown.value = seconds
    resume()
  }

  // Valida o formato do e-mail
  const validateEmail = (email: string): boolean => {
    if (!email) {
      message.error(t('auth.enterEmailFirst'))
      return false
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(email)) {
      message.error(t('auth.enterValidEmail'))
      return false
    }

    return true
  }

  // Envia o código de verificação (cenário de registro - requer verificar antes o nome de usuário e o e-mail)
  const sendRegisterCode = async (email: string, username?: string) => {
    if (!validateEmail(email)) return false
    if (!canSendCode.value) return false

    sendCodeLoading.value = true

    try {
      // Verifica primeiro se o nome de usuário e o e-mail já existem
      if (username) {
        const checkRes = await checkUser({
          username,
          email,
        })

        if (checkRes.code !== 200) {
          message.error(checkRes.message)
          return false
        }
      }

      // Envia o código de verificação
      const res = await sendEmailCaptcha({
        email,
        type: 'register',
      })

      if (res.code === 200) {
        message.success(t('auth.verificationCodeSent'))
        startCountdown()
        return true
      } else {
        message.error(res.message || t('auth.sendVerificationCodeFailed'))
        return false
      }
    } catch (error) {
      message.error(t('auth.sendVerificationCodeRetry'))
      return false
    } finally {
      sendCodeLoading.value = false
    }
  }

  // Envia o código de verificação (cenário de senha esquecida)
  const sendForgetCode = async (email: string) => {
    if (!validateEmail(email)) return false
    if (!canSendCode.value) return false

    sendCodeLoading.value = true

    try {
      const res = await sendEmailCaptcha({
        email,
        type: 'forget',
      })

      if (res.code === 200) {
        message.success(t('auth.verificationCodeSent'))
        startCountdown()
        return true
      } else {
        message.error(res.message || t('auth.sendVerificationCodeFailed'))
        return false
      }
    } catch (error) {
      message.error(t('auth.sendVerificationCodeRetry'))
      return false
    } finally {
      sendCodeLoading.value = false
    }
  }

  // Valida o código de verificação
  const verifyCode = async (email: string, code: string, type: VerificationType) => {
    try {
      const res = await checkCaptcha({
        email,
        code,
        type,
      })

      if (res.code === 200) {
        message.success(t('auth.verificationSuccess'))
        return true
      } else {
        message.error(res.message || t('auth.verificationCodeError'))
        return false
      }
    } catch (error) {
      message.error('Falha na verificação, tente novamente mais tarde')
      return false
    }
  }

  return {
    sendCodeLoading,
    countdown,
    canSendCode,
    buttonText,
    isActive,
    sendRegisterCode,
    sendForgetCode,
    verifyCode,
    validateEmail,
    pause,
    resume
  }
}
