-- Adiciona o tipo 'tool' ao campo sender da tabela sys_message, usado para armazenar mensagens de resposta de chamadas de ferramenta
ALTER TABLE sys_message
  MODIFY COLUMN sender enum('user','assistant','tool') NOT NULL
  COMMENT 'Remetente da mensagem: user-usuário, assistant-IA, tool-resposta de ferramenta';
