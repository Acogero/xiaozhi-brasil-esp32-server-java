-- Adiciona o campo de lista de ferramentas MCP do dispositivo, usado para salvar os nomes das MCP tools reportadas na conexão do dispositivo (separados por vírgula)
-- Corresponde à persistência de DeviceDO.mcpList / DeviceMcpService.persistMcpList
ALTER TABLE `sys_device`
    ADD COLUMN `mcpList` TEXT NULL
    COMMENT 'Lista de ferramentas MCP do dispositivo, nomes separados por vírgula'
    AFTER `deviceName`;
