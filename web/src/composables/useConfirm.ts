import { Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import type { ModalFuncProps } from 'ant-design-vue'

/**
 * Composable de diálogo de confirmação
 * Gerencia de forma unificada diversas operações de confirmação
 */

export interface ConfirmOptions {
  /**
   * Título
   */
  title?: string
  
  /**
   * Conteúdo
   */
  content?: string
  
  /**
   * Texto do botão de confirmação
   */
  okText?: string
  
  /**
   * Texto do botão de cancelamento
   */
  cancelText?: string
  
  /**
   * Tipo do botão de confirmação
   */
  okType?: 'primary' | 'danger' | 'default' | 'dashed' | 'link' | 'text'
  
  /**
   * Estado de carregamento do botão de confirmação
   */
  okButtonProps?: {
    loading?: boolean
    disabled?: boolean
  }
  
  /**
   * Ícone
   */
  icon?: any
  
  /**
   * Largura
   */
  width?: string | number
  
  /**
   * Se deve exibir o botão de cancelar
   */
  showCancel?: boolean
}

export function useConfirm() {
  const { t } = useI18n()
  
  /**
   * Diálogo de confirmação genérico
   */
  const confirm = (
    onOk: () => void | Promise<void>,
    options: ConfirmOptions = {}
  ) => {
    return Modal.confirm({
      title: options.title || t('common.confirm'),
      content: options.content || t('common.confirmOperation'),
      okText: options.okText || t('common.confirm'),
      cancelText: options.cancelText || t('common.cancel'),
      okType: options.okType || 'primary',
      icon: options.icon,
      width: options.width,
      okButtonProps: options.okButtonProps,
      class: options.showCancel === false ? 'hide-cancel-button' : undefined,
      onOk: async () => {
        await onOk()
      }
    })
  }
  
  /**
   * Confirmação de exclusão
   */
  const confirmDelete = (
    onConfirm: () => void | Promise<void>,
    options: Partial<ConfirmOptions> = {}
  ) => {
    return Modal.confirm({
      title: options.title || t('common.confirmDelete'),
      content: options.content || t('common.confirmDeleteMessage'),
      okText: options.okText || t('common.delete'),
      cancelText: options.cancelText || t('common.cancel'),
      okType: 'danger',
      icon: options.icon,
      width: options.width,
      onOk: async () => {
        await onConfirm()
      }
    })
  }
  
  /**
   * Confirmação de aviso
   */
  const confirmWarning = (
    onConfirm: () => void | Promise<void>,
    options: Partial<ConfirmOptions> = {}
  ) => {
    return Modal.warning({
      title: options.title || t('common.warning'),
      content: options.content || t('common.warningMessage'),
      okText: options.okText || t('common.confirm'),
      okType: 'primary',
      icon: options.icon,
      width: options.width,
      onOk: async () => {
        await onConfirm()
      }
    } as ModalFuncProps)
  }
  
  /**
   * Confirmação informativa
   */
  const confirmInfo = (
    onConfirm: () => void | Promise<void>,
    options: Partial<ConfirmOptions> = {}
  ) => {
    return Modal.info({
      title: options.title || t('common.info'),
      content: options.content || t('common.infoMessage'),
      okText: options.okText || t('common.confirm'),
      icon: options.icon,
      width: options.width,
      onOk: async () => {
        await onConfirm()
      }
    } as ModalFuncProps)
  }
  
  /**
   * Confirmação de sucesso
   */
  const confirmSuccess = (
    onConfirm: () => void | Promise<void>,
    options: Partial<ConfirmOptions> = {}
  ) => {
    return Modal.success({
      title: options.title || t('common.success'),
      content: options.content || t('common.successMessage'),
      okText: options.okText || t('common.confirm'),
      icon: options.icon,
      width: options.width,
      onOk: async () => {
        await onConfirm()
      }
    } as ModalFuncProps)
  }
  
  /**
   * Confirmação de erro
   */
  const confirmError = (
    onConfirm: () => void | Promise<void>,
    options: Partial<ConfirmOptions> = {}
  ) => {
    return Modal.error({
      title: options.title || t('common.error'),
      content: options.content || t('common.errorMessage'),
      okText: options.okText || t('common.confirm'),
      icon: options.icon,
      width: options.width,
      onOk: async () => {
        await onConfirm()
      }
    } as ModalFuncProps)
  }
  
  /**
   * Confirmação de salvamento
   */
  const confirmSave = (
    onConfirm: () => void | Promise<void>,
    options: Partial<ConfirmOptions> = {}
  ) => {
    return Modal.confirm({
      title: options.title || t('common.confirmSave'),
      content: options.content || t('common.confirmSaveMessage'),
      okText: options.okText || t('common.save'),
      cancelText: options.cancelText || t('common.cancel'),
      okType: 'primary',
      icon: options.icon,
      width: options.width,
      onOk: async () => {
        await onConfirm()
      }
    })
  }
  
  /**
   * Confirmação de cancelamento
   */
  const confirmCancel = (
    onConfirm: () => void | Promise<void>,
    options: Partial<ConfirmOptions> = {}
  ) => {
    return Modal.confirm({
      title: options.title || t('common.confirmCancel'),
      content: options.content || t('common.confirmCancelMessage'),
      okText: options.okText || t('common.confirm'),
      cancelText: options.cancelText || t('common.cancel'),
      okType: 'danger',
      icon: options.icon,
      width: options.width,
      onOk: async () => {
        await onConfirm()
      }
    })
  }
  
  /**
   * Confirmação de envio
   */
  const confirmSubmit = (
    onConfirm: () => void | Promise<void>,
    options: Partial<ConfirmOptions> = {}
  ) => {
    return Modal.confirm({
      title: options.title || t('common.confirmSubmit'),
      content: options.content || t('common.confirmSubmitMessage'),
      okText: options.okText || t('common.submit'),
      cancelText: options.cancelText || t('common.cancel'),
      okType: 'primary',
      icon: options.icon,
      width: options.width,
      onOk: async () => {
        await onConfirm()
      }
    })
  }
  
  return {
    confirm,
    confirmDelete,
    confirmWarning,
    confirmInfo,
    confirmSuccess,
    confirmError,
    confirmSave,
    confirmCancel,
    confirmSubmit
  }
}

