/**
 * Definições de tipos relacionados a ferramentas MCP
 */

/**
 * Resumo global de ferramentas do sistema
 */
export interface SystemGlobalToolSummary {
  name: string
  description: string
}

/**
 * Item de ferramenta MCP
 */
export interface McpToolItem {
  name: string
  description: string
  inputSchema: string
  inputSchemaData: McpToolSchemaProperty[]
  enabled: boolean
  source: string
}

/**
 * Propriedade de parâmetro de ferramenta MCP
 */
export interface McpToolSchemaProperty {
  name: string
  type: string
  description: string
  required: boolean
}
