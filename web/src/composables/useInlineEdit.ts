import { ref, type Ref } from 'vue'

/**
 * Composable de edição em linha
 * Usado para funcionalidade de edição em linha em tabelas
 */

export interface UseInlineEditOptions<T> {
  /**
   * Obtém o identificador único do item
   * @param item Item de dados
   * @returns Identificador único (geralmente o ID)
   */
  getKey: (item: T) => string | number
  
  /**
   * Função de callback de salvamento
   * @param item Item de dados após edição
   * @returns Indica sucesso; se retornar false, não sai do modo de edição
   */
  onSave?: (item: T) => Promise<boolean | void>
  
  /**
   * Callback de cancelamento de edição
   * @param item Item de dados com edição cancelada
   */
  onCancel?: (item: T) => void
}

export interface EditableItem {
  editable?: boolean
  [key: string]: any
}

export function useInlineEdit<T extends EditableItem>(
  dataSource: Ref<T[]>,
  options: UseInlineEditOptions<T>
) {
  // Key do item atualmente em edição
  const editingKey = ref<string | number>('')
  
  // Dados em cache (usados para restaurar ao cancelar a edição)
  const cacheData = ref<T[]>([])
  
  /**
   * Verifica se um item está sendo editado
   */
  const isEditing = (key: string | number): boolean => {
    return editingKey.value === key
  }
  
  /**
   * Inicia a edição
   * @param key Key do item a ser editado
   */
  const startEdit = (key: string | number) => {
    // Cancela o estado de edição de outras linhas
    dataSource.value.forEach(item => {
      if (item.editable) {
        item.editable = false
      }
    })
    
    // Inicia a edição da linha alvo
    const target = dataSource.value.find(item => options.getKey(item) === key)
    if (target) {
      // Faz backup dos dados atuais
      cacheData.value = dataSource.value.map(item => ({ ...item }))
      
      // Define o estado de edição
      target.editable = true
      editingKey.value = key
    }
  }
  
  /**
   * Cancela a edição
   * @param key Key do item a ter a edição cancelada
   */
  const cancelEdit = (key: string | number) => {
    const target = dataSource.value.find(item => options.getKey(item) === key)
    const cache = cacheData.value.find(item => options.getKey(item as T) === key)
    
    if (target && cache) {
      // Restaura os dados em cache
      Object.assign(target, cache)
      target.editable = false
    }
    
    editingKey.value = ''
    
    // Dispara o callback de cancelamento
    if (target) {
      options.onCancel?.(target)
    }
  }
  
  /**
   * Salva a edição
   * @param item Item de dados após edição
   */
  const saveEdit = async (item: T): Promise<boolean> => {
    if (!options.onSave) {
      item.editable = false
      editingKey.value = ''
      return true
    }
    
    try {
      const result = await options.onSave(item)
      
      // Se retornar false, não sai do modo de edição
      if (result === false) {
        return false
      }
      
      item.editable = false
      editingKey.value = ''
      return true
    } catch (error) {
      console.error('Falha ao salvar:', error)
      return false
    }
  }
  
  /**
   * Atualiza o valor do campo
   * @param key Key do item
   * @param field Nome do campo
   * @param value Novo valor
   */
  const updateField = <K extends keyof T>(
    key: string | number,
    field: K,
    value: T[K]
  ) => {
    const target = dataSource.value.find(item => options.getKey(item) === key)
    if (target) {
      target[field] = value
    }
  }
  
  return {
    // Estado
    editingKey,
    cacheData,
    
    // Métodos
    isEditing,
    startEdit,
    cancelEdit,
    saveEdit,
    updateField
  }
}

