import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'

/**
 * Composable de exportação de dados
 * Suporta exportação nos formatos CSV, JSON, Excel etc.
 */

export interface ExportColumn<T = unknown> {
  /**
   * Nome da chave da coluna
   */
  key: string
  
  /**
   * Título da coluna
   */
  title: string
  
  /**
   * Função de formatação personalizada
   */
  format?: (value: any, record: T) => string | number
}

export interface ExportOptions<T = unknown> {
  /**
   * Nome do arquivo (sem extensão)
   */
  filename?: string
  
  /**
   * Configuração das colunas (se não especificado, exporta todos os campos)
   */
  columns?: ExportColumn<T>[]
  
  /**
   * Se deve exibir a mensagem de carregamento
   */
  showLoading?: boolean
}

export function useExport() {
  const { t } = useI18n()
  
  // Estado de exportação
  const exporting = ref(false)
  
  /**
   * Converte para o formato CSV
   */
  const convertToCSV = <T>(data: T[], columns?: ExportColumn<T>[]): string => {
    if (data.length === 0) return ''
    
    // Se as colunas não forem especificadas, usa todas as chaves da primeira linha
    const firstItem = data[0] as object
    const cols: ExportColumn<T>[] = columns || Object.keys(firstItem).map(key => ({
      key,
      title: key,
    }))
    
    // Cabeçalho do CSV
    const headers = cols.map(col => `"${col.title}"`).join(',')
    
    // Linhas de dados do CSV
    const rows = data.map(record => {
      const recordObj = record as { [key: string]: unknown }
      return cols.map(col => {
        let value: unknown = recordObj[col.key]
        
        // Usa a formatação personalizada
        if (col.format) {
          value = col.format(value, record)
        }
        
        // Trata caracteres especiais
        if (value === null || value === undefined) {
          return '""'
        }
        
        // Converte para string e escapa as aspas
        const strValue = String(value).replace(/"/g, '""')
        return `"${strValue}"`
      }).join(',')
    })
    
    return [headers, ...rows].join('\n')
  }
  
  /**
   * Faz o download do arquivo
   */
  const downloadFile = (content: string, filename: string, mimeType: string) => {
    // Adiciona o BOM para que o Excel reconheça corretamente o UTF-8
    const BOM = '\uFEFF'
    const blob = new Blob([BOM + content], { type: `${mimeType};charset=utf-8;` })
    const url = URL.createObjectURL(blob)
    
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.style.display = 'none'
    
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    // Libera o objeto URL
    setTimeout(() => URL.revokeObjectURL(url), 100)
  }
  
  /**
   * Exporta como CSV
   */
  const exportToCSV = async <T>(
    data: T[],
    options: ExportOptions<T> = {}
  ): Promise<boolean> => {
    if (data.length === 0) {
      message.warning(t('export.noData'))
      return false
    }
    
    exporting.value = true
    
    try {
      if (options.showLoading !== false) {
        message.loading(t('export.exporting'))
      }
      
      const csv = convertToCSV(data, options.columns)
      const filename = `${options.filename || 'export'}.csv`
      
      downloadFile(csv, filename, 'text/csv')
      
      // Exibe a mensagem de sucesso apenas se a notificação interna estiver habilitada
      if (options.showLoading !== false) {
        message.success(t('export.success'))
      }
      return true
    } catch (error) {
      console.error('Falha na exportação do CSV:', error)
      // Exibe a mensagem de erro apenas se a notificação interna estiver habilitada
      if (options.showLoading !== false) {
        message.error(t('export.failed'))
      }
      return false
    } finally {
      exporting.value = false
    }
  }
  
  /**
   * Exporta como JSON
   */
  const exportToJSON = async <T>(
    data: T[],
    options: ExportOptions<T> = {}
  ): Promise<boolean> => {
    if (data.length === 0) {
      message.warning(t('export.noData'))
      return false
    }
    
    exporting.value = true
    
    try {
      if (options.showLoading !== false) {
        message.loading(t('export.exporting'))
      }
      
      // Se as colunas forem especificadas, exporta apenas os campos indicados
      let exportData = data
      if (options.columns && options.columns.length > 0) {
        exportData = data.map(record => {
          const recordObj = record as { [key: string]: unknown }
          const newRecord: { [key: string]: unknown } = {}
          options.columns?.forEach(col => {
            let value = recordObj[col.key]
            if (col.format) {
              value = col.format(value, record)
            }
            newRecord[col.title || col.key] = value
          })
          return newRecord as T
        })
      }
      
      const json = JSON.stringify(exportData, null, 2)
      const filename = `${options.filename || 'export'}.json`
      
      downloadFile(json, filename, 'application/json')
      
      // Exibe a mensagem de sucesso apenas se a notificação interna estiver habilitada
      if (options.showLoading !== false) {
        message.success(t('export.success'))
      }
      return true
    } catch (error) {
      console.error('Falha na exportação do JSON:', error)
      // Exibe a mensagem de erro apenas se a notificação interna estiver habilitada
      if (options.showLoading !== false) {
        message.error(t('export.failed'))
      }
      return false
    } finally {
      exporting.value = false
    }
  }
  
  /**
   * Exporta como Excel (usa o formato CSV, que o Excel consegue abrir)
   */
  const exportToExcel = async <T>(
    data: T[],
    options: ExportOptions<T> = {}
  ): Promise<boolean> => {
    if (data.length === 0) {
      message.warning(t('export.noData'))
      return false
    }
    
    exporting.value = true
    
    try {
      if (options.showLoading !== false) {
        message.loading(t('export.exporting'))
      }
      
      const csv = convertToCSV(data, options.columns)
      const filename = `${options.filename || 'export'}.xlsx`
      
      // Usa a codificação UTF-16LE e um formato especial para que o Excel reconheça
      const BOM = '\ufeff'
      const csvWithBOM = BOM + csv
      
      // Cria um CSV compatível com o Excel
      const blob = new Blob([csvWithBOM], { 
        type: 'application/vnd.ms-excel;charset=utf-8;' 
      })
      const url = URL.createObjectURL(blob)
      
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      link.style.display = 'none'
      
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
      setTimeout(() => URL.revokeObjectURL(url), 100)
      
      // Exibe a mensagem de sucesso apenas se a notificação interna estiver habilitada
      if (options.showLoading !== false) {
        message.success(t('export.success'))
      }
      return true
    } catch (error) {
      console.error('Falha na exportação do Excel:', error)
      // Exibe a mensagem de erro apenas se a notificação interna estiver habilitada
      if (options.showLoading !== false) {
        message.error(t('export.failed'))
      }
      return false
    } finally {
      exporting.value = false
    }
  }
  
  /**
   * Seleciona automaticamente o formato de exportação
   */
  const exportData = async <T>(
    data: T[],
    format: 'csv' | 'json' | 'excel',
    options: ExportOptions<T> = {}
  ): Promise<boolean> => {
    switch (format) {
      case 'csv':
        return await exportToCSV(data, options)
      case 'json':
        return await exportToJSON(data, options)
      case 'excel':
        return await exportToExcel(data, options)
      default:
        message.error(t('export.unsupportedFormat'))
        return false
    }
  }
  
  /**
   * Converte texto CSV em tabela HTML (usado para pré-visualização)
   */
  const parseCSVToTable = (csvText: string): string => {
    if (!csvText.trim()) {
      return ''
    }
    
    const lines = csvText.split(/\r?\n/).filter(line => line.trim())
    if (lines.length === 0) {
      return ''
    }
    
    const headerRow = lines[0]
    const dataRows = lines.slice(1)
    
    // Análise simples de CSV (trata aspas)
    const parseCSVRow = (row: string): string[] => {
      const values: string[] = []
      let current = ''
      let inQuotes = false
      
      for (let i = 0; i < row.length; i++) {
        const char = row[i]
        const nextChar = row[i + 1]
        
        if (char === '"') {
          if (inQuotes && nextChar === '"') {
            current += '"'
            i++
          } else {
            inQuotes = !inQuotes
          }
        } else if (char === ',' && !inQuotes) {
          values.push(current.trim())
          current = ''
        } else {
          current += char
        }
      }
      
      values.push(current)
      return values
    }
    
    const headers = parseCSVRow(headerRow || '')
    const headerHTML = `<tr>${headers.map(h => `<th>${h}</th>`).join('')}</tr>`
    
    const rowsHTML = dataRows.map(row => {
      const cells = parseCSVRow(row)
      return `<tr>${cells.map(c => `<td>${c}</td>`).join('')}</tr>`
    }).join('')
    
    return `<thead>${headerHTML}</thead><tbody>${rowsHTML}</tbody>`
  }
  
  /**
   * Importa dados CSV da área de transferência
   */
  const importFromClipboard = async <T = { [key: string]: string }>(): Promise<T[]> => {
    try {
      const text = await navigator.clipboard.readText()
      
      if (!text.trim()) {
        message.warning(t('export.noData'))
        return []
      }
      
      // Análise simples de CSV (assume separação por tabulação ou vírgula)
      const lines = text.split(/\r?\n/).filter(line => line.trim())
      const headers = lines[0]?.split(/\t|,/).map(h => h.trim()) || []
      
      const data: T[] = []
      for (let i = 1; i < lines.length; i++) {
        const values = lines[i]?.split(/\t|,/).map(v => v.trim()) || []
        const row: { [key: string]: string } = {}
        headers.forEach((header, index) => {
          row[header] = values[index] || ''
        })
        data.push(row as T)
      }
      
      message.success(t('export.importSuccess'))
      return data
    } catch (error) {
      console.error('Falha na importação:', error)
      message.error(t('export.importFailed'))
      return []
    }
  }
  
  return {
    // Estado
    exporting,
    
    // Métodos
    exportToCSV,
    exportToJSON,
    exportToExcel,
    exportData,
    parseCSVToTable,
    importFromClipboard,
    convertToCSV,
    downloadFile
  }
}
