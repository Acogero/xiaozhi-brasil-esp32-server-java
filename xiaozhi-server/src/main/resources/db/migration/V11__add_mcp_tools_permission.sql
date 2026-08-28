-- Adiciona as permissões de gerenciamento de ferramentas MCP (ausentes desde a introdução do recurso:
-- McpToolController referenciava essas permissionKeys, mas elas nunca foram inseridas em sys_permission)

-- Permissão de seção: ferramentas MCP do papel (controla a exibição do bloco na tela de Configuração de papéis)
INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT permissionId, 'Ferramentas MCP do papel', 'system:role:mcp-tools', 'button', NULL, NULL, NULL, 10, '0', '1'
FROM `sys_permission` WHERE `permissionKey` = 'system:role';

-- Sub-permissões de API: ferramentas MCP do papel
INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT permissionId, 'Endpoint de listagem de ferramentas desativadas do papel', 'system:role:mcp-tools:api:list', 'api', NULL, NULL, NULL, 1, '0', '1'
FROM `sys_permission` WHERE `permissionKey` = 'system:role:mcp-tools';

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT permissionId, 'Endpoint de atualização de ferramentas MCP do papel', 'system:role:mcp-tools:api:update', 'api', NULL, NULL, NULL, 2, '0', '1'
FROM `sys_permission` WHERE `permissionKey` = 'system:role:mcp-tools';

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT permissionId, 'Endpoint de listagem de ferramentas globais do sistema', 'system:role:mcp-tools:api:system-global', 'api', NULL, NULL, NULL, 3, '0', '1'
FROM `sys_permission` WHERE `permissionKey` = 'system:role:mcp-tools';

-- Sub-permissão de API: atualização de ferramenta MCP global (usada em Configurações)
INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT permissionId, 'Endpoint de atualização de ferramenta MCP global', 'system:config:mcpServer:api:update', 'api', NULL, NULL, NULL, 5, '0', '1'
FROM `sys_permission` WHERE `permissionKey` = 'system:config';

-- Concede automaticamente ao papel de administrador todas as novas permissões de ferramentas MCP
INSERT INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, permissionId FROM `sys_permission`
WHERE `permissionKey` IN (
    'system:role:mcp-tools',
    'system:role:mcp-tools:api:list',
    'system:role:mcp-tools:api:update',
    'system:role:mcp-tools:api:system-global',
    'system:config:mcpServer:api:update'
)
AND permissionId NOT IN (SELECT permissionId FROM `sys_auth_role_permission` WHERE `authRoleId` = 1);
