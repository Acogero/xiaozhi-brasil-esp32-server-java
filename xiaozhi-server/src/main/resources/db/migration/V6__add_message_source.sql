-- Adiciona o campo de origem da mensagem, distinguindo Chat Web / conversa via dispositivo / outras origens futuras
-- Os dados existentes (todos eram conversas via dispositivo antes da migração) são tratados automaticamente pelo DEFAULT 'device'
ALTER TABLE `sys_message`
    ADD COLUMN `source` VARCHAR(16) NOT NULL DEFAULT 'device'
    COMMENT 'Origem da mensagem: web|device|... (dados existentes usam device por padrão)'
    AFTER `sessionId`;
