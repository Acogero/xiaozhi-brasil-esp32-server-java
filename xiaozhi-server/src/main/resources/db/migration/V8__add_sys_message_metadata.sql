-- Adiciona a coluna JSON metadata em sys_message, para armazenar os metadados adicionais do UserMessage
-- (campos estruturados como speaker/emotion/emotionScore/emotionDegree etc.)
-- Esses campos originalmente eram concatenados como prefixo de texto [speaker:X][neutral] na coluna message,
-- e agora passam a ser armazenados de forma estruturada e independente; a coluna message mantém apenas o texto puro do usuário.
-- Ao enviar ao LLM, a camada Conversation reconstrói o prefixo em tempo de execução, via projeção.

ALTER TABLE sys_message
    ADD COLUMN metadata JSON NULL COMMENT 'Metadados adicionais do UserMessage (speaker/emotion etc.), em formato JSON' AFTER message;
