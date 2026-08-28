import { ref, type Ref } from 'vue'
import type { FormInstance } from 'ant-design-vue'

/**
 * Composable de gerenciamento de modal
 * Usado para gerenciar de forma unificada o estado e a lógica dos modais de criação/edição
 */

export interface UseModalOptions<T = any> {
  /**
   * Função de callback de envio
   * @param data Dados do formulário
   * @param isEdit Indica se está em modo de edição
   * @returns Indica sucesso; se retornar false, o modal não é fechado
   */
  onSubmit?: (data: T, isEdit: boolean) => Promise<boolean | void>
  
  /**
   * Callback de abertura do modal
   * @param item Item de edição (informado no modo de edição)
   */
  onOpen?: (item?: T) => void | Promise<void>
  
  /**
   * Callback de fechamento do modal
   */
  onClose?: () => void
  
  /**
   * Referência da instância do formulário (usada para resetar o formulário)
   */
  formRef?: Ref<FormInstance | undefined>
}

export function useModal<T = any>(options?: UseModalOptions<T>) {
  // Visibilidade do modal
  const visible = ref(false)
  
  // Indica se está em modo de edição
  const isEdit = ref(false)
  
  // Item atualmente em edição
  const editingItem = ref<T | null>(null)
  
  // Estado de carregamento do envio
  const submitLoading = ref(false)
  
  /**
   * Abre o modal - modo de criação
   */
  const openCreate = async () => {
    isEdit.value = false
    editingItem.value = null
    await options?.onOpen?.()
    visible.value = true
  }
  
  /**
   * Abre o modal - modo de edição
   * @param item Item a ser editado
   */
  const openEdit = async (item: T) => {
    isEdit.value = true
    editingItem.value = item
    await options?.onOpen?.(item)
    visible.value = true
  }
  
  /**
   * Fecha o modal
   */
  const close = () => {
    visible.value = false
    editingItem.value = null
    
    // Reseta o formulário
    if (options?.formRef?.value) {
      options.formRef.value.resetFields()
    }
    
    options?.onClose?.()
  }
  
  /**
   * Envia o formulário
   * @param data Dados do formulário
   */
  const submit = async (data: T): Promise<boolean> => {
    if (!options?.onSubmit) {
      close()
      return true
    }
    
    submitLoading.value = true
    try {
      const result = await options.onSubmit(data, isEdit.value)
      
      // Se retornar false, o modal não é fechado
      if (result === false) {
        return false
      }
      
      close()
      return true
    } catch (error) {
      console.error('Falha ao enviar:', error)
      return false
    } finally {
      submitLoading.value = false
    }
  }
  
  /**
   * Cancela a operação (igual a close, mas com semântica mais clara)
   */
  const cancel = () => {
    close()
  }
  
  return {
    // Estado
    visible,
    isEdit,
    editingItem,
    submitLoading,
    
    // Métodos
    openCreate,
    openEdit,
    close,
    cancel,
    submit
  }
}

