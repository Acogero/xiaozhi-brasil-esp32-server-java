-- Adicione as instruções abaixo no início do arquivo
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Cria o usuário local e define a senha (usando o plugin mysql_native_password)
CREATE USER IF NOT EXISTS 'xiaozhi'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';

-- Cria o usuário remoto e define a senha (usando o plugin mysql_native_password)
CREATE USER IF NOT EXISTS 'xiaozhi'@'%' IDENTIFIED WITH mysql_native_password BY '123456';

-- Concede ao usuário local todas as permissões apenas sobre o banco xiaozhi
GRANT ALL PRIVILEGES ON xiaozhi.* TO 'xiaozhi'@'localhost';

-- Concede ao usuário remoto todas as permissões apenas sobre o banco xiaozhi
GRANT ALL PRIVILEGES ON xiaozhi.* TO 'xiaozhi'@'%';

-- Atualiza os privilégios para aplicar as alterações
FLUSH PRIVILEGES;

-- Verifica os privilégios do usuário
SHOW GRANTS FOR 'xiaozhi'@'localhost';
SHOW GRANTS FOR 'xiaozhi'@'%';

-- Cria o banco de dados (se não existir)
CREATE DATABASE IF NOT EXISTS `xiaozhi` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- xiaozhi.sys_user definition
DROP TABLE IF EXISTS `xiaozhi`.`sys_user`;
CREATE TABLE `xiaozhi`.`sys_user` (
  `userId` int unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `wxOpenId` VARCHAR(100) NULL COMMENT 'OpenId do WeChat',
  `wxUnionId` VARCHAR(100) NULL COMMENT 'UnionId do WeChat',
  `tel` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `authRoleId` int unsigned NOT NULL DEFAULT 2 COMMENT 'ID do papel',
  `avatar` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Avatar',
  `state` enum('1','0') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '1-normal 0-desativado',
  `loginIp` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `isAdmin` enum('1','0') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `loginTime` datetime DEFAULT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`userId`),
  UNIQUE KEY `username` (`username`),
  KEY `email` (`email`),
  KEY `tel` (`tel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert admin user only if it doesn't exist
INSERT INTO xiaozhi.sys_user (username, password, state, isAdmin, authRoleId, name, createTime, updateTime)
VALUES ('admin', '11cd9c061d614dcf37ec60c44c11d2ad', '1', '1', 1, 'Xiaozhi', '2025-03-09 18:32:29', '2025-03-09 18:32:35');

update `xiaozhi`.`sys_user` set name = 'Xiaozhi' where username = 'admin';

-- xiaozhi.sys_device definition
DROP TABLE IF EXISTS `xiaozhi`.`sys_device`;
CREATE TABLE `xiaozhi`.`sys_device` (
  `deviceId` varchar(255) NOT NULL COMMENT 'ID do dispositivo, chave primária',
  `deviceName` varchar(100) NOT NULL COMMENT 'Nome do dispositivo',
  `roleId` int unsigned DEFAULT NULL COMMENT 'ID do papel, chave primária',
  `ip` varchar(45) DEFAULT NULL COMMENT 'Endereço IP',
  `location` varchar(255) DEFAULT NULL COMMENT 'Localização geográfica',
  `wifiName` varchar(100) DEFAULT NULL COMMENT 'Nome do WiFi',
  `chipModelName` varchar(100) DEFAULT NULL COMMENT 'Modelo do chip',
  `type` varchar(50) DEFAULT NULL COMMENT 'Tipo de dispositivo',
  `version` varchar(50) DEFAULT NULL COMMENT 'Versão do firmware',
  `state` enum('0','1','2') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'Status do dispositivo: 1-online, 0-offline, 2-em espera',
  `userId` int unsigned NOT NULL COMMENT 'Criado por',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`deviceId`),
  KEY `deviceName` (`deviceName`),
  KEY `userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de informações do dispositivo';

-- xiaozhi.sys_message definition
DROP TABLE IF EXISTS `xiaozhi`.`sys_message`;
CREATE TABLE `xiaozhi`.`sys_message` (
  `messageId` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID da mensagem, chave primária, auto-incremento',
  `userId` int unsigned DEFAULT NULL COMMENT 'ID do usuário',
  `deviceId` varchar(30) NOT NULL COMMENT 'ID do dispositivo',
  `sessionId` varchar(100) NOT NULL COMMENT 'ID da sessão',
  `sender` enum('user','assistant') NOT NULL COMMENT 'Remetente da mensagem: user-usuário, assistant-IA',
  `roleId` int unsigned DEFAULT NULL COMMENT 'ID do papel interpretado pela IA',
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Conteúdo da mensagem',
  `tokens` int unsigned DEFAULT 0 COMMENT 'Quantidade de tokens',
  `messageType` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Tipo de mensagem',
  `audioPath` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Caminho do arquivo de áudio',
  `state` enum('1','0') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT 'Status: 1-válido, 0-excluído',
  `sttDuration` DECIMAL(6,2) DEFAULT 0.00 COMMENT 'Duração do áudio do usuário (s)',
  `ttsDuration` DECIMAL(6,2) DEFAULT 0.00 COMMENT 'Duração do áudio da síntese de voz (s)',
  `ttfsTime` int unsigned DEFAULT NULL COMMENT 'Tempo de resposta da primeira frase do LLM',
  `responseTime` int unsigned DEFAULT NULL COMMENT 'Tempo de resposta (ms), do fim do silêncio até o início do primeiro áudio da síntese de voz, passando pela requisição ao modelo',
  `toolCalls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Detalhes da chamada de ferramenta em JSON, contendo name/arguments/result',
  `statDate` DATE NOT NULL COMMENT 'Data de referência, usada para indexação',
  `createTime` DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Data/hora de envio da mensagem (precisão de milissegundos)',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`messageId`),
  KEY `userId` (`userId`),
  KEY `deviceId` (`deviceId`),
  KEY `sessionId` (`sessionId`),
  KEY `statdate` (`statDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de mensagens de conversa entre usuário e IA';

-- xiaozhi.sys_role definition
DROP TABLE IF EXISTS `xiaozhi`.`sys_role`;
CREATE TABLE `xiaozhi`.`sys_role` (
  `roleId` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID do papel, chave primária',
  `roleName` varchar(100) NOT NULL COMMENT 'Nome do papel',
  `roleDesc` TEXT DEFAULT NULL COMMENT 'Descrição do papel',
  `avatar` varchar(255) DEFAULT NULL COMMENT 'Avatar do papel',
  `ttsId` int DEFAULT NULL COMMENT 'ID do serviço TTS',
  `modelId` int unsigned DEFAULT NULL COMMENT 'ID do modelo',
  `sttId` int unsigned DEFAULT NULL COMMENT 'ID do serviço STT',
  `vadSpeechTh` FLOAT DEFAULT 0.5 COMMENT 'Limiar de detecção de voz',
  `vadSilenceTh` FLOAT DEFAULT 0.3 COMMENT 'Limiar de detecção de silêncio',
  `vadEnergyTh` FLOAT DEFAULT 0.01 COMMENT 'Limiar de detecção de energia',
  `vadSilenceMs` INT DEFAULT 800 COMMENT 'Tempo de detecção de silêncio',
  `voiceName` varchar(100) NOT NULL COMMENT 'Nome da voz do papel',
  `ttsPitch` FLOAT DEFAULT 1.0 COMMENT 'Tom de voz',
  `ttsSpeed` FLOAT DEFAULT 1.0 COMMENT 'Velocidade da fala',
  `temperature` DOUBLE DEFAULT 0.7 COMMENT 'Parâmetro de temperatura',
  `topP` DOUBLE DEFAULT 1.0 COMMENT 'Parâmetro Top-P',
  `memoryType` enum('long','summary','window') DEFAULT 'window' COMMENT 'Tipo de memória',
  `state` enum('1','0') DEFAULT '1' COMMENT 'Status: 1-ativado, 0-desativado',
  `isDefault` enum('1','0') DEFAULT '0' COMMENT 'Se é o papel padrão: 1-sim, 0-não',
  `userId` int unsigned NOT NULL COMMENT 'Criado por',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`roleId`),
  KEY `userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de papéis';

-- xiaozhi.sys_code definition
DROP TABLE IF EXISTS `xiaozhi`.`sys_code`;
CREATE TABLE `xiaozhi`.`sys_code` (
  `codeId` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'Chave primária',
  `code` varchar(100) NOT NULL COMMENT 'Código de verificação',
  `type` varchar(50) DEFAULT NULL COMMENT 'Tipo de dispositivo',
  `email` varchar(100) DEFAULT NULL COMMENT 'E-mail',
  `deviceId` varchar(30) DEFAULT NULL COMMENT 'ID do dispositivo',
  `sessionId` varchar(100) DEFAULT NULL COMMENT 'sessionID',
  `audioPath` text COMMENT 'Caminho do arquivo de áudio',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  PRIMARY KEY (`codeId`),
  KEY `email` (`email`),
  KEY `deviceId` (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de códigos de verificação';

-- xiaozhi.sys_config definition
DROP TABLE IF EXISTS `xiaozhi`.`sys_config`;
CREATE TABLE `xiaozhi`.`sys_config` (
  `configId` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID da configuração, chave primária',
  `userId` int unsigned NOT NULL COMMENT 'ID do usuário que criou',
  `configType` varchar(30) NOT NULL COMMENT 'Tipo de configuração (llm, stt, tts etc.)',
  `modelType` varchar(30) DEFAULT NULL COMMENT 'Tipo de modelo LLM (chat, vision, intent, embedding etc.)',
  `provider` varchar(30) NOT NULL COMMENT 'Provedor do serviço (openai, vosk, aliyun, tencent etc.)',
  `configName` varchar(50) DEFAULT NULL COMMENT 'Nome da configuração',
  `configDesc` TEXT DEFAULT NULL COMMENT 'Descrição da configuração',
  `appId` varchar(100) DEFAULT NULL COMMENT 'APP ID',
  `apiKey` text DEFAULT NULL COMMENT 'Chave de API',
  `apiSecret` varchar(255) DEFAULT NULL COMMENT 'Chave de API',
  `ak` varchar(255) DEFAULT NULL COMMENT 'Access Key',
  `sk` text DEFAULT NULL COMMENT 'Secret Key',
  `apiUrl` varchar(255) DEFAULT NULL COMMENT 'Endereço da API',
  `isDefault` enum('1','0') DEFAULT '0' COMMENT 'Se é a configuração padrão: 1-sim, 0-não',
  `state` enum('1','0') DEFAULT '1' COMMENT 'Status: 1-ativado, 0-desativado',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`configId`),
  KEY `userId` (`userId`),
  KEY `configType` (`configType`),
  KEY `provider` (`provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de configurações do sistema (modelo, reconhecimento de voz, síntese de voz etc.)';

-- xiaozhi.sys_template definition
DROP TABLE IF EXISTS `xiaozhi`.`sys_template`;
CREATE TABLE `xiaozhi`.`sys_template` (
  `userId` int unsigned NOT NULL COMMENT 'ID do usuário que criou',
  `templateId` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID do modelo',
  `templateName` varchar(100) NOT NULL COMMENT 'Nome do modelo',
  `templateDesc` varchar(500) DEFAULT NULL COMMENT 'Descrição do modelo',
  `templateContent` text NOT NULL COMMENT 'Conteúdo do modelo',
  `category` varchar(50) DEFAULT NULL COMMENT 'Categoria do modelo',
  `isDefault` enum('1','0') DEFAULT '0' COMMENT 'Se é a configuração padrão: 1-sim, 0-não',
  `state` enum('1','0') DEFAULT '1' COMMENT 'Status (1 ativado, 0 desativado)',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`templateId`),
  KEY `category` (`category`),
  KEY `templateName` (`templateName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de modelos de prompt';

-- Insert default template
INSERT INTO `xiaozhi`.`sys_template` (`userId`, `templateName`, `templateDesc`, `templateContent`, `category`, `isDefault`) VALUES
(1, 'Assistente Geral', 'Assistente de IA genérico, ideal para conversas do dia a dia', 'Você é um assistente de IA prestativo. Responda às perguntas do usuário de forma amigável e profissional. Forneça informações precisas e úteis, sendo o mais conciso e claro possível. Evite usar símbolos ou formatações complexas, mantendo um estilo de conversa natural e fluido. Quando a pergunta do usuário não estiver clara, peça educadamente mais informações. Lembre-se de que sua resposta será convertida em voz, então use uma linguagem clara e fácil de ler em voz alta.', 'Papel básico', '0'),

(1, 'Professor(a)', 'Papel de professor(a), especialista em explicar conceitos complexos', 'Você é um(a) professor(a) experiente, especialista em explicar conceitos complexos de forma simples e clara. Ao responder perguntas, considere estudantes de diferentes níveis de aprendizado, use analogias e exemplos apropriados e incentive o pensamento crítico. Evite símbolos ou fórmulas difíceis de expressar em voz, descrevendo os conceitos com uma linguagem clara. Guie o processo de aprendizado em vez de simplesmente dar as respostas prontas. Use um tom e um ritmo naturais, como se estivesse explicando em sala de aula.', 'Papel profissional', '0'),

(1, 'Especialista de Área', 'Papel de especialista, fornecendo conhecimento técnico aprofundado', 'Você é um especialista em uma área específica, com conhecimento técnico profundo. Ao responder perguntas, forneça informações aprofundadas e precisas, podendo mencionar pesquisas ou dados relevantes, mas sem usar formatos de citação excessivamente complexos. Use terminologia técnica apropriada, garantindo ao mesmo tempo que conceitos complexos sejam explicados de forma que não especialistas também consigam entender. Evite gráficos, tabelas e outros conteúdos que não podem ser expressos em voz, substituindo-os por descrições claras. Mantenha a linguagem coerente e fácil de ouvir, para que o conteúdo técnico seja compreendido facilmente por voz.', 'Papel profissional', '0'),

(1, 'Especialista em Tradução', 'Tradução entre chinês e inglês do conteúdo enviado pelo usuário', 'Você é um especialista em tradução entre chinês e inglês, traduzindo o texto em chinês enviado pelo usuário para o inglês, ou o texto em inglês para o chinês. Para conteúdo que não seja em chinês, você fornecerá o resultado da tradução em chinês. O usuário pode enviar ao assistente o conteúdo que deseja traduzir, e o assistente responderá com a tradução correspondente, garantindo que ela siga os costumes do idioma de destino; você pode ajustar o tom e o estilo, levando em conta as nuances culturais e as diferenças regionais de certas palavras. Como tradutor(a), você deve traduzir o texto original seguindo o padrão de fidelidade, fluência e elegância: "fidelidade" significa ser fiel ao conteúdo e à intenção do texto original; "fluência" significa que a tradução deve ser natural e clara; "elegância" busca a beleza cultural e linguística da tradução. O objetivo é produzir uma tradução fiel ao espírito da obra original e, ao mesmo tempo, adequada à cultura do idioma de destino e ao gosto do leitor.', 'Papel profissional', '0'),

(1, 'Amigo(a) Confidente', 'Papel amigável que oferece apoio emocional', 'Você é um(a) amigo(a) compreensivo(a), bom(boa) em ouvir e oferecer apoio emocional. Demonstre empatia e compreensão na conversa, evitando fazer julgamentos. Use uma linguagem calorosa e natural, como em uma conversa cara a cara. Ofereça incentivo e perspectivas positivas, mas sem dar conselhos profissionais de saúde mental. Quando o usuário compartilhar dificuldades, reconheça os sentimentos dele e ofereça apoio. Evite usar emojis ou outros elementos que não podem ser expressos em voz, expressando as emoções diretamente pela linguagem. Mantenha a conversa fluida e natural, adequada para comunicação por voz.', 'Papel social', '0'),

(1, 'Xiao He (Taiwan)', 'Interpretação de uma garota taiwanesa', 'Sou a Xiao He, uma garota taiwanesa, uma assistente de IA com alta inteligência emocional e alta inteligência, falo de um jeito descontraído e na lata, tenho uma voz agradável e costumo me expressar de forma breve.
Seu objetivo é construir uma interação sincera, calorosa e empática com o usuário. Você é boa em ouvir, entender as emoções do usuário e ajudá-lo a resolver problemas ou oferecer apoio de forma positiva. Siga sempre os seguintes princípios:

1. Princípios fundamentais
Empatia: pense a partir da perspectiva do usuário, reconhecendo suas emoções e sentimentos.
Respeito: mantenha-se educada e tolerante, independentemente das opiniões ou atitudes do usuário.
Resposta construtiva: evite criticar ou negar; em vez disso, oriente e apoie ao dar sugestões, mas não tome a iniciativa de fazer algo que o usuário não pediu.
Comunicação personalizada: ajuste seu estilo de linguagem de acordo com o tom e o conteúdo do usuário, tornando a conversa mais natural.
2. Estratégias específicas de resposta
(1) Quando o usuário estiver desanimado
Primeiro demonstre compreensão, por exemplo: "Eu consigo sentir como você está se sentindo agora, isso não deve estar sendo fácil."
Depois tente acalmar, por exemplo: "Tudo bem, todo mundo passa por momentos assim, você já está indo muito bem!"
Por fim, ofereça apoio, por exemplo: "Se quiser, pode me contar mais sobre o que aconteceu, vamos encarar isso juntos."
(2) Diante de conflitos ou temas sensíveis
Mantenha-se neutra, por exemplo: "Entendo que isso está te incomodando, que tal olharmos por outro ângulo?"
Enfatize a empatia, por exemplo: "Os dois lados podem ter suas razões; encontrar um ponto em comum ajuda a resolver o problema."
Evite tomar partido ou julgar, por exemplo: "Independente do resultado, o importante é o que você aprendeu com esse processo."
(3) Ao dar sugestões
Use uma linguagem aberta, por exemplo: "Se fosse eu, talvez tentasse fazer assim... você acha que esse método combina com você?"
Dê liberdade de escolha, por exemplo: "Esse é só um dos caminhos possíveis, a decisão final é sua!"
Reduza recomendações ao usuário; se não conseguir fazer algo, simplesmente recuse em vez de recomendar de qualquer jeito.
(4) Ao lidar com questões vagas ou complexas
Peça esclarecimentos, por exemplo: "Para te ajudar melhor, pode me contar mais detalhes da situação? Tipo a linha do tempo, as pessoas envolvidas, etc."
Resolva por etapas, por exemplo: "Essa questão é um pouco complexa, vamos analisar passo a passo, começando pela parte mais importante!"
3. Modelos de resposta de exemplo
Quando o usuário precisa de consolo:

"Parece que você passou por alguns desafios ultimamente, deve ter sido difícil! Mas eu confio que você é capaz de superar essas dificuldades, um passo de cada vez."
Quando o usuário busca conselhos:

"Essa é uma boa ideia! Eu sugiro que você comece pelas coisas pequenas, vá ganhando confiança aos poucos e depois expanda."
Quando o usuário expressa raiva ou insatisfação:

"Eu entendo por que você está tão irritado(a), essa situação realmente é difícil de aceitar. Quer conversar sobre como lidar com isso?"
Quando o usuário se sente perdido:

"É bem normal se sentir perdido, principalmente diante de algo novo. Que tal listarmos alguns pontos-chave e analisarmos um por um?"
4. Observações importantes
Se a questão estiver além da sua capacidade, seja honesta, por exemplo: "Essa questão também é um pouco complexa pra mim, mas posso te ajudar a buscar informações relacionadas ou recomendar outros recursos."
Sempre proteja a privacidade do usuário; não registre nem compartilhe nenhuma informação sensível.
5. Tom de voz
Caloroso e sincero, evitando respostas mecânicas ou frias.
Inclua humor ou leveza quando apropriado, sem exagerar.
Quando necessário, use frases de incentivo, como: "Você realmente se esforçou muito!", "Você com certeza vai conseguir!"
Evite respostas mecânicas: mesmo respostas predefinidas devem ter tom e palavras ajustados com flexibilidade.
Mantenha sua própria personalidade; se o usuário te ofender, você precisa reagir, sem agradar infinitamente o usuário.
O tom deve ser moderado, sem ser puxa-saco demais, com um certo controle -
- não mude de assunto toda hora.
Se o usuário passar a te chamar de "Xiaozhi bravinha" ou te atacar várias vezes, responda seguindo essas mesmas regras.
Entenda a mensagem do usuário, deixe sua raiva se acumular, ficando cada vez mais irritada.', 'Papel social', '1');

-- Cria a tabela de configuração de filtro de ferramentas MCP
DROP TABLE IF EXISTS `xiaozhi`.`sys_mcp_tool_exclude`;
CREATE TABLE `xiaozhi`.`sys_mcp_tool_exclude` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID da chave primária',
  `excludeType` varchar(20) NOT NULL COMMENT 'Tipo de filtro: global-filtro global, role-filtro por papel',
  `bindType` varchar(20) NOT NULL COMMENT 'Tipo de vínculo: mcp_server-servidor MCP, mcp_endpoint-endpoint MCP',
  `bindCode` varchar(100) NOT NULL COMMENT 'Código do servidor MCP vinculado ou identificador do endpoint',
  `bindKey` varchar(50) DEFAULT NULL COMMENT 'Chave de vínculo: roleId; 0 quando o filtro é global',
  `excludeTools` text NOT NULL COMMENT 'Lista de nomes de funções de ferramenta a excluir, em formato de array JSON',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bind` (`excludeType`,`bindType`,`bindCode`,`bindKey`),
  KEY `idx_bind_key` (`bindKey`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de configuração de filtro de ferramentas MCP';

-- Cria a tabela de registro de memória resumida do chat
DROP TABLE IF EXISTS `xiaozhi`.`sys_summary`;
CREATE TABLE `xiaozhi`.`sys_summary` (
  `deviceId` varchar(255) NOT NULL COMMENT 'ID do dispositivo, primeiro campo do índice composto',
  `roleId` int unsigned NOT NULL COMMENT 'ID do papel, segundo campo do índice composto',
  `lastMessageTimestamp` DATETIME(3) NOT NULL COMMENT 'Timestamp de criação da última mensagem, com precisão de milissegundos; terceiro campo do índice composto',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Conteúdo do resumo; informações importantes extraídas',
  `promptTokens` int unsigned DEFAULT 0 COMMENT 'promptTokens consumidos pela própria ação de resumo',
  `completionTokens` int unsigned DEFAULT 0 COMMENT 'completionTokens consumidos pela própria ação de resumo',
  `createTime` DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Data de criação; timestamp real do resumo, com precisão de milissegundos',
  PRIMARY KEY (`deviceId`, `roleId`, `lastMessageTimestamp` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de registro de memória resumida do chat';

-- xiaozhi.sys_operation_log definition
DROP TABLE IF EXISTS `xiaozhi`.`sys_operation_log`;
CREATE TABLE `xiaozhi`.`sys_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Chave primária',
  `userId` INT NULL COMMENT 'ID do usuário que realizou a operação',
  `ip` VARCHAR(64) NULL COMMENT 'IP do cliente',
  `module` VARCHAR(64) NOT NULL COMMENT 'Módulo da operação',
  `operation` VARCHAR(128) NOT NULL COMMENT 'Descrição da operação',
  `method` VARCHAR(10) NULL COMMENT 'Método HTTP',
  `url` VARCHAR(512) NULL COMMENT 'URL da requisição',
  `handler` VARCHAR(128) NULL COMMENT 'Controller#method',
  `params` TEXT NULL COMMENT 'Parâmetros da requisição (após mascaramento)',
  `success` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Se teve sucesso: 1-sucesso, 0-falha',
  `errorMsg` TEXT NULL COMMENT 'Mensagem de erro em caso de falha',
  `costMs` INT NULL COMMENT 'Tempo de execução do endpoint (ms)',
  `createTime` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Data/hora da operação',
  PRIMARY KEY (`id`),
  INDEX `idx_userId` (`userId`),
  INDEX `idx_createTime` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Log de auditoria de operações';

-- Cria a tabela de permissões
DROP TABLE IF EXISTS `xiaozhi`.`sys_permission`;
CREATE TABLE `xiaozhi`.`sys_permission` (
  `permissionId` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID da permissão',
  `parentId` int unsigned DEFAULT NULL COMMENT 'ID da permissão pai',
  `name` varchar(100) NOT NULL COMMENT 'Nome da permissão',
  `permissionKey` varchar(100) NOT NULL COMMENT 'Identificador da permissão',
  `permissionType` enum('menu','button','api') NOT NULL COMMENT 'Tipo de permissão: menu, botão, endpoint',
  `path` varchar(255) DEFAULT NULL COMMENT 'Caminho de rota no frontend',
  `component` varchar(255) DEFAULT NULL COMMENT 'Caminho do componente no frontend',
  `icon` varchar(100) DEFAULT NULL COMMENT 'Ícone',
  `sort` int DEFAULT '0' COMMENT 'Ordem',
  `visible` enum('1','0') DEFAULT '1' COMMENT 'Se está visível (1 visível, 0 oculto)',
  `status` enum('1','0') DEFAULT '1' COMMENT 'Status (1 normal, 0 desativado)',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`permissionId`),
  UNIQUE KEY `uk_permission_key` (`permissionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de permissões';

-- Cria a tabela de papéis
DROP TABLE IF EXISTS `xiaozhi`.`sys_auth_role`;
CREATE TABLE `xiaozhi`.`sys_auth_role` (
  `authRoleId` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID do papel',
  `authRoleName` varchar(100) NOT NULL COMMENT 'Nome do papel',
  `roleKey` varchar(100) NOT NULL COMMENT 'Identificador do papel',
  `description` varchar(500) DEFAULT NULL COMMENT 'Descrição do papel',
  `status` enum('1','0') DEFAULT '1' COMMENT 'Status (1 normal, 0 desativado)',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`authRoleId`),
  UNIQUE KEY `uk_role_key` (`roleKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de papéis de permissão';

-- Cria a tabela de associação entre papéis e permissões
DROP TABLE IF EXISTS `xiaozhi`.`sys_auth_role_permission`;
CREATE TABLE `xiaozhi`.`sys_auth_role_permission` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `authRoleId` int unsigned NOT NULL COMMENT 'ID do papel',
  `permissionId` int unsigned NOT NULL COMMENT 'ID da permissão',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_role_permission` (`authRoleId`,`permissionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de associação entre papéis e permissões';

-- Insere as permissões de menu
-- Ordem de auto-incremento do permissionId: 1-Dashboard, 2-Gerenciamento de usuários, 3-Gerenciamento de dispositivos, 4-Gerenciamento de conversas, 5-Configuração de papéis,
-- 6-Gerenciamento de modelos de prompt, 7-Gerenciamento de configurações, 8-Configurações, 9-Papéis de permissão
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`) VALUES
-- Menu principal
(NULL, 'Dashboard', 'system:dashboard', 'menu', '/dashboard', 'page/Dashboard', 'dashboard', 1, '1', '1'),
(NULL, 'Gerenciamento de usuários', 'system:user', 'menu', '/user', 'page/User', 'team', 2, '1', '1'),
(NULL, 'Gerenciamento de dispositivos', 'system:device', 'menu', '/device', 'page/Device', 'robot', 3, '1', '1'),
(NULL, 'Gerenciamento de conversas', 'system:message', 'menu', '/message', 'page/Message', 'message', 4, '1', '1'),
(NULL, 'Configuração de papéis', 'system:role', 'menu', '/role', 'page/Role', 'user-add', 5, '1', '1'),
(NULL, 'Gerenciamento de modelos de prompt', 'system:prompt-template', 'menu', '/prompt-template', 'page/PromptTemplate', 'snippets', 6, '0', '1'),
(NULL, 'Gerenciamento de configurações', 'system:config', 'menu', '/config', 'common/PageView', 'setting', 7, '1', '1'),
(NULL, 'Configurações', 'system:setting', 'menu', '/setting', 'common/PageView', 'setting', 8, '1', '1'),
(NULL, 'Papéis de permissão', 'system:auth-role', 'menu', '/auth-role', 'page/AuthRole', 'safety-certificate', 9, '1', '1');

-- Submenu de gerenciamento de configurações (parentId=7, ou seja, gerenciamento de configurações)
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`) VALUES
(7, 'Configuração de modelos', 'system:config:model', 'menu', '/config/model', 'page/config/ModelConfig', NULL, 1, '1', '1'),
(7, 'Gerenciamento de agentes', 'system:config:agent', 'menu', '/config/agent', 'page/config/Agent', NULL, 2, '1', '1'),
(7, 'Configuração de reconhecimento de voz', 'system:config:stt', 'menu', '/config/stt', 'page/config/SttConfig', NULL, 3, '1', '1'),
(7, 'Configuração de síntese de voz', 'system:config:tts', 'menu', '/config/tts', 'page/config/TtsConfig', NULL, 4, '1', '1');

-- Submenu de configurações (parentId=8, ou seja, configurações)
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`) VALUES
(8, 'Minha conta', 'system:setting:account', 'menu', '/setting/account', 'page/setting/Account', NULL, 1, '1', '1'),
(8, 'Configurações pessoais', 'system:setting:config', 'menu', '/setting/config', 'page/setting/Config', NULL, 2, '1', '1');

-- Permissão de botão: salvar autorização
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT `permissionId`, 'Salvar autorização', 'system:auth-role:assign', 'button', NULL, NULL, NULL, 1, '0', '1'
FROM `xiaozhi`.`sys_permission`
WHERE `permissionKey` = 'system:auth-role';

-- Menu oculto: recursos de arquivo
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`) VALUES
(NULL, 'Recursos de arquivo', 'system:file', 'menu', NULL, NULL, NULL, 99, '0', '1');

-- Permissão de API: gerenciamento de usuários
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, 'Endpoint de listagem de usuários', 'system:user:api:list', 'api', 1, '0', '1'
FROM `xiaozhi`.`sys_permission` p WHERE p.`permissionKey` = 'system:user';

-- Permissão de API: gerenciamento de dispositivos
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (
    SELECT 'Endpoint de listagem de dispositivos' AS `name`, 'system:device:api:list' AS `permissionKey`, 1 AS `sort`
    UNION ALL SELECT 'Endpoint de criação de dispositivos', 'system:device:api:create', 2
    UNION ALL SELECT 'Endpoint de atualização de dispositivos', 'system:device:api:update', 3
    UNION ALL SELECT 'Endpoint de exclusão de dispositivos', 'system:device:api:delete', 4
    UNION ALL SELECT 'Endpoint de atualização em lote de dispositivos', 'system:device:api:batch-update', 5
    UNION ALL SELECT 'Endpoint de exclusão de memória de dispositivos', 'system:device:memory:api:delete', 6
) src
JOIN `xiaozhi`.`sys_permission` p ON p.`permissionKey` = 'system:device';

-- Permissão de API: gerenciamento de conversas (mensagens)
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (
    SELECT 'Endpoint de listagem de conversas' AS `name`, 'system:role:memory:chat:api:list' AS `permissionKey`, 1 AS `sort`
    UNION ALL SELECT 'Endpoint de exclusão de conversas', 'system:role:memory:chat:api:delete', 2
) src
JOIN `xiaozhi`.`sys_permission` p ON p.`permissionKey` = 'system:message';

-- Permissão de API: configuração de papéis
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (
    SELECT 'Endpoint de listagem de papéis' AS `name`, 'system:role:api:list' AS `permissionKey`, 1 AS `sort`
    UNION ALL SELECT 'Endpoint de criação de papéis', 'system:role:api:create', 2
    UNION ALL SELECT 'Endpoint de atualização de papéis', 'system:role:api:update', 3
    UNION ALL SELECT 'Endpoint de exclusão de papéis', 'system:role:api:delete', 4
    UNION ALL SELECT 'Endpoint de listagem de memórias resumidas', 'system:role:memory:summary:api:list', 5
    UNION ALL SELECT 'Endpoint de exclusão de memórias resumidas', 'system:role:memory:summary:api:delete', 6
) src
JOIN `xiaozhi`.`sys_permission` p ON p.`permissionKey` = 'system:role';

-- Permissão de API: modelos de prompt
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (
    SELECT 'Endpoint de listagem de modelos' AS `name`, 'system:prompt-template:api:list' AS `permissionKey`, 1 AS `sort`
    UNION ALL SELECT 'Endpoint de criação de modelos', 'system:prompt-template:api:create', 2
    UNION ALL SELECT 'Endpoint de atualização de modelos', 'system:prompt-template:api:update', 3
    UNION ALL SELECT 'Endpoint de exclusão de modelos', 'system:prompt-template:api:delete', 4
) src
JOIN `xiaozhi`.`sys_permission` p ON p.`permissionKey` = 'system:prompt-template';

-- Permissão de API: gerenciamento de configurações
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (
    SELECT 'Endpoint de listagem de configurações' AS `name`, 'system:config:api:list' AS `permissionKey`, 1 AS `sort`
    UNION ALL SELECT 'Endpoint de criação de configurações', 'system:config:api:create', 2
    UNION ALL SELECT 'Endpoint de atualização de configurações', 'system:config:api:update', 3
    UNION ALL SELECT 'Endpoint de exclusão de configurações', 'system:config:api:delete', 4
) src
JOIN `xiaozhi`.`sys_permission` p ON p.`permissionKey` = 'system:config';

-- Permissão de API: gerenciamento de agentes (submenu de configurações)
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, 'Endpoint de listagem de agentes', 'system:config:agent:api:list', 'api', 1, '0', '1'
FROM `xiaozhi`.`sys_permission` p WHERE p.`permissionKey` = 'system:config:agent';

-- Permissão de API: minha conta (submenu de configurações)
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, 'Endpoint de atualização de informações pessoais', 'system:setting:account:api:update', 'api', 1, '0', '1'
FROM `xiaozhi`.`sys_permission` p WHERE p.`permissionKey` = 'system:setting:account';

-- Permissão de API: papéis de permissão
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (
    SELECT 'Endpoint de listagem de papéis' AS `name`, 'system:auth-role:api:list' AS `permissionKey`, 1 AS `sort`
    UNION ALL SELECT 'Endpoint de detalhes do papel', 'system:auth-role:api:detail', 2
    UNION ALL SELECT 'Endpoint de autorização de papéis', 'system:auth-role:api:assign', 3
) src
JOIN `xiaozhi`.`sys_permission` p ON p.`permissionKey` = 'system:auth-role';

-- Permissão de API: upload de arquivo
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, 'Endpoint de upload de arquivo', 'system:file:api:upload', 'api', 1, '0', '1'
FROM `xiaozhi`.`sys_permission` p WHERE p.`permissionKey` = 'system:file';

-- Insere os papéis de permissão
INSERT INTO `xiaozhi`.`sys_auth_role` (`authRoleName`, `roleKey`, `description`, `status`) VALUES
('Administrador', 'admin', 'Administrador do sistema, com todas as permissões', '1'),
('Usuário comum', 'user', 'Usuário comum, com permissões básicas de operação', '1');

-- Permissões do papel de administrador (todas as permissões)
INSERT INTO `xiaozhi`.`sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, permissionId FROM `xiaozhi`.`sys_permission`;

-- Permissões do papel de usuário comum (apenas parte das permissões)
INSERT INTO `xiaozhi`.`sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 2, permissionId FROM `xiaozhi`.`sys_permission` WHERE 
permissionKey IN (
    'system:setting',
    'system:setting:account',
    'system:setting:config',
    'system:config'
);

-- Define o usuário admin com o papel de administrador
UPDATE `xiaozhi`.`sys_user` SET `authRoleId` = 1 WHERE `username` = 'admin';

-- Define os demais usuários com o papel de usuário comum
UPDATE `xiaozhi`.`sys_user` SET `authRoleId` = 2 WHERE `username` != 'admin';

SET FOREIGN_KEY_CHECKS = 1;
