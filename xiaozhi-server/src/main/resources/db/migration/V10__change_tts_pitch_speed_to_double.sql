-- Altera os campos de velocidade/tom da TTS de FLOAT para DOUBLE, mantendo consistência com o tipo Double do lado Java e evitando perda implícita de precisão.

ALTER TABLE `sys_role`
    MODIFY COLUMN `ttsPitch` DOUBLE DEFAULT 1.0 COMMENT 'Tom de voz',
    MODIFY COLUMN `ttsSpeed` DOUBLE DEFAULT 1.0 COMMENT 'Velocidade da fala';
