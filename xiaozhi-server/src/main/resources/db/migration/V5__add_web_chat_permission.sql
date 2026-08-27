-- Permissão de menu do Chat Web
INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
VALUES (NULL, 'Chat Web', 'system:chat', 'menu', '/chat', 'page/Chat', 'message', 12, '1', '1');

-- Sub-permissões de API do Chat Web (associadas a system:chat)
INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT permissionId, 'Abrir sessão', 'system:chat:api:open', 'api', NULL, NULL, NULL, 1, '0', '1'
FROM `sys_permission` WHERE `permissionKey` = 'system:chat';

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT permissionId, 'Chat em streaming', 'system:chat:api:stream', 'api', NULL, NULL, NULL, 2, '0', '1'
FROM `sys_permission` WHERE `permissionKey` = 'system:chat';

INSERT INTO `sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT permissionId, 'Fechar sessão', 'system:chat:api:close', 'api', NULL, NULL, NULL, 3, '0', '1'
FROM `sys_permission` WHERE `permissionKey` = 'system:chat';

-- Concede automaticamente ao papel de administrador todas as permissões do Chat Web
INSERT INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, permissionId FROM `sys_permission`
WHERE `permissionKey` IN ('system:chat', 'system:chat:api:open', 'system:chat:api:stream', 'system:chat:api:close')
AND permissionId NOT IN (SELECT permissionId FROM `sys_auth_role_permission` WHERE `authRoleId` = 1);

-- Concede também ao papel de usuário comum as permissões do Chat Web (chat é uma funcionalidade básica)
INSERT INTO `sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 2, permissionId FROM `sys_permission`
WHERE `permissionKey` IN ('system:chat', 'system:chat:api:open', 'system:chat:api:stream', 'system:chat:api:close')
AND permissionId NOT IN (SELECT permissionId FROM `sys_auth_role_permission` WHERE `authRoleId` = 2);
