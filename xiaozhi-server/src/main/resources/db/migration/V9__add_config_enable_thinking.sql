-- Adiciona o campo enableThinking em sys_config, usado para controlar se o modo de raciocínio (thinking) do modelo está habilitado
ALTER TABLE `sys_config`
    ADD COLUMN `enableThinking` TINYINT(1) DEFAULT NULL
    COMMENT 'Se o modo de raciocínio (thinking) está habilitado'
    AFTER `isDefault`;
