import type { Rule } from 'ant-design-vue/es/form'
import type { Ref } from 'vue'
import { useI18n } from 'vue-i18n'

// Função de fábrica para criar regras de validação
export function createValidationRules() {
  const { t } = useI18n()

  return {
    // Regra de validação de e-mail
    emailRules: [
      { required: true, message: t('validation.enterEmail'), trigger: 'blur' },
      { type: 'email', message: t('validation.enterValidEmail'), trigger: 'blur' },
    ] as Rule[],

    // Regra de validação de nome de usuário
    usernameRules: [
      { required: true, message: t('validation.enterUsername'), trigger: 'blur' },
      { min: 3, max: 20, message: t('validation.usernameLength', { min: 3, max: 20 }), trigger: 'blur' },
      {
        pattern: /^[a-zA-Z0-9_]+$/,
        message: t('validation.username'),
        trigger: 'blur',
      },
    ] as Rule[],

    // Regra de validação de senha
    passwordRules: [
      { required: true, message: t('validation.enterPassword'), trigger: 'blur' },
      { min: 6, max: 20, message: t('validation.passwordLength', { min: 6, max: 20 }), trigger: 'blur' },
    ] as Rule[],

    // Regra de validação de confirmação de senha (versão reativa)
    confirmPasswordRules: (passwordRef: Ref<string>): Rule[] => [
      {
        validator: (_rule: Rule, value: string) => {
          if (!value) {
            return Promise.reject(t('validation.enterConfirmPassword'))
          }
          if (value !== passwordRef.value) {
            return Promise.reject(t('validation.confirmPassword'))
          }
          return Promise.resolve()
        },
        trigger: 'blur',
      },
    ],

    // Regra do código de verificação
    verificationCodeRules: [
      { required: true, message: t('validation.enterVerificationCode'), trigger: 'blur' },
      { len: 6, message: t('validation.verificationCodeLength', { length: 6 }), trigger: 'blur' },
    ] as Rule[],

    // Regra de número de telefone (opcional)
    telRules: [
      {
        pattern: /^1[3-9]\d{9}$/,
        message: t('validation.enterValidPhone'),
        trigger: 'blur',
      },
    ] as Rule[],

    // Regra de nome
    nameRules: [
      { required: true, message: t('validation.enterName'), trigger: 'blur' },
      { min: 2, max: 20, message: t('validation.nameLength', { min: 2, max: 20 }), trigger: 'blur' },
    ] as Rule[],
  }
}

// Função composable principal
export function useFormValidation() {
  return createValidationRules()
}

// Para compatibilidade retroativa, fornece regras de inicialização tardia
// Essas funções precisam ser chamadas no contexto do setup
export function useEmailRules() {
  return createValidationRules().emailRules
}

export function useUsernameRules() {
  return createValidationRules().usernameRules
}

export function usePasswordRules() {
  return createValidationRules().passwordRules
}

export function useVerificationCodeRules() {
  return createValidationRules().verificationCodeRules
}

export function useTelRules() {
  return createValidationRules().telRules
}

export function useNameRules() {
  return createValidationRules().nameRules
}

export function useConfirmPasswordRules(passwordRef: Ref<string>) {
  return createValidationRules().confirmPasswordRules(passwordRef)
}