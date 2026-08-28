-- Adicione a instrução a seguir no início do arquivo
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Criar usuário local e definir a senha (usando o plugin mysql_native_password)
CREATE USER IF NOT EXISTS 'xiaozhi'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';

-- Criar usuário remoto e definir a senha (usando o plugin mysql_native_password)
CREATE USER IF NOT EXISTS 'xiaozhi'@'%' IDENTIFIED WITH mysql_native_password BY '123456';

-- Conceder ao usuário local todos os privilégios apenas sobre o banco de dados xiaozhi
GRANT ALL PRIVILEGES ON xiaozhi.* TO 'xiaozhi'@'localhost';

-- Conceder ao usuário remoto todos os privilégios apenas sobre o banco de dados xiaozhi
GRANT ALL PRIVILEGES ON xiaozhi.* TO 'xiaozhi'@'%';

-- Atualizar os privilégios para que as alterações tenham efeito
FLUSH PRIVILEGES;

-- Consultar os privilégios do usuário
SHOW GRANTS FOR 'xiaozhi'@'localhost';
SHOW GRANTS FOR 'xiaozhi'@'%';

-- Criar o banco de dados (se não existir)
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
  `authRoleId` int unsigned NOT NULL DEFAULT 2 COMMENT 'ID do papel de permissão',
  `avatar` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Avatar',
  `state` enum('1','0') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT '1-normal 0-desabilitado',
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
  `roleId` int unsigned DEFAULT NULL COMMENT 'ID da persona, chave primária',
  `ip` varchar(45) DEFAULT NULL COMMENT 'Endereço IP',
  `location` varchar(255) DEFAULT NULL COMMENT 'Localização geográfica',
  `wifiName` varchar(100) DEFAULT NULL COMMENT 'Nome da rede WiFi',
  `chipModelName` varchar(100) DEFAULT NULL COMMENT 'Modelo do chip',
  `type` varchar(50) DEFAULT NULL COMMENT 'Tipo de dispositivo',
  `version` varchar(50) DEFAULT NULL COMMENT 'Versão do firmware',
  `state` enum('0','1','2') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '0' COMMENT 'Estado do dispositivo: 1-online, 0-offline, 2-em espera',
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
  `messageId` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID da mensagem, chave primária, autoincremento',
  `userId` int unsigned DEFAULT NULL COMMENT 'ID do usuário',
  `deviceId` varchar(30) NOT NULL COMMENT 'ID do dispositivo',
  `sessionId` varchar(100) NOT NULL COMMENT 'ID da sessão',
  `sender` enum('user','assistant') NOT NULL COMMENT 'Remetente da mensagem: user-usuário, assistant-inteligência artificial',
  `roleId` int unsigned DEFAULT NULL COMMENT 'ID da persona interpretada pela IA',
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Conteúdo da mensagem',
  `tokens` int unsigned DEFAULT 0 COMMENT 'Quantidade de tokens',
  `messageType` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Tipo de mensagem',
  `audioPath` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Caminho do arquivo de áudio',
  `state` enum('1','0') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '1' COMMENT 'Status: 1-válido, 0-excluído',
  `sttDuration` DECIMAL(6,2) DEFAULT 0.00 COMMENT 'Duração do áudio do usuário (segundos)',
  `ttsDuration` DECIMAL(6,2) DEFAULT 0.00 COMMENT 'Duração do áudio de síntese de voz (segundos)',
  `ttfsTime` int unsigned DEFAULT NULL COMMENT 'Tempo de resposta da primeira frase do LLM',
  `responseTime` int unsigned DEFAULT NULL COMMENT 'Tempo de resposta (milissegundos), do fim do silêncio até a primeira emissão de voz sintetizada',
  `toolCalls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Detalhes das chamadas de ferramentas em JSON, contendo name/arguments/result',
  `statDate` DATE NOT NULL COMMENT 'Data de estatística, usada para indexação',
  `createTime` DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Data de envio da mensagem (precisão de milissegundos)',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`messageId`),
  KEY `userId` (`userId`),
  KEY `deviceId` (`deviceId`),
  KEY `sessionId` (`sessionId`),
  KEY `statdate` (`statDate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de mensagens de diálogo entre usuário e IA';

-- xiaozhi.sys_role definition
DROP TABLE IF EXISTS `xiaozhi`.`sys_role`;
CREATE TABLE `xiaozhi`.`sys_role` (
  `roleId` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID da persona, chave primária',
  `roleName` varchar(100) NOT NULL COMMENT 'Nome da persona',
  `roleDesc` TEXT DEFAULT NULL COMMENT 'Descrição da persona',
  `avatar` varchar(255) DEFAULT NULL COMMENT 'Avatar da persona',
  `ttsId` int DEFAULT NULL COMMENT 'ID do serviço de TTS',
  `modelId` int unsigned DEFAULT NULL COMMENT 'ID do modelo',
  `sttId` int unsigned DEFAULT NULL COMMENT 'ID do serviço de STT',
  `vadSpeechTh` FLOAT DEFAULT 0.5 COMMENT 'Limiar de detecção de voz',
  `vadSilenceTh` FLOAT DEFAULT 0.3 COMMENT 'Limiar de detecção de silêncio',
  `vadEnergyTh` FLOAT DEFAULT 0.01 COMMENT 'Limiar de detecção de energia',
  `vadSilenceMs` INT DEFAULT 800 COMMENT 'Tempo de detecção de silêncio',
  `voiceName` varchar(100) NOT NULL COMMENT 'Nome da voz da persona',
  `ttsPitch` FLOAT DEFAULT 1.0 COMMENT 'Tom de voz',
  `ttsSpeed` FLOAT DEFAULT 1.0 COMMENT 'Velocidade da fala',
  `temperature` DOUBLE DEFAULT 0.7 COMMENT 'Parâmetro de temperatura',
  `topP` DOUBLE DEFAULT 1.0 COMMENT 'Parâmetro Top-P',
  `memoryType` enum('long','summary','window') DEFAULT 'window' COMMENT 'Tipo de memória',
  `state` enum('1','0') DEFAULT '1' COMMENT 'Status: 1-habilitado, 0-desabilitado',
  `isDefault` enum('1','0') DEFAULT '0' COMMENT 'Se é a persona padrão: 1-sim, 0-não',
  `userId` int unsigned NOT NULL COMMENT 'Criado por',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`roleId`),
  KEY `userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de personas';

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
  `provider` varchar(30) NOT NULL COMMENT 'Provedor de serviço (openai, vosk, aliyun, tencent etc.)',
  `configName` varchar(50) DEFAULT NULL COMMENT 'Nome da configuração',
  `configDesc` TEXT DEFAULT NULL COMMENT 'Descrição da configuração',
  `appId` varchar(100) DEFAULT NULL COMMENT 'APP ID',
  `apiKey` text DEFAULT NULL COMMENT 'Chave de API',
  `apiSecret` varchar(255) DEFAULT NULL COMMENT 'Segredo (chave secreta) da API',
  `ak` varchar(255) DEFAULT NULL COMMENT 'Access Key',
  `sk` text DEFAULT NULL COMMENT 'Secret Key',
  `apiUrl` varchar(255) DEFAULT NULL COMMENT 'URL da API',
  `isDefault` enum('1','0') DEFAULT '0' COMMENT 'Se é a configuração padrão: 1-sim, 0-não',
  `state` enum('1','0') DEFAULT '1' COMMENT 'Status: 1-habilitado, 0-desabilitado',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`configId`),
  KEY `userId` (`userId`),
  KEY `configType` (`configType`),
  KEY `provider` (`provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de configuração do sistema (modelos, reconhecimento de voz, síntese de voz etc.)';

-- xiaozhi.sys_template definition
DROP TABLE IF EXISTS `xiaozhi`.`sys_template`;
CREATE TABLE `xiaozhi`.`sys_template` (
  `userId` int unsigned NOT NULL COMMENT 'ID do usuário que criou',
  `templateId` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID do modelo (template)',
  `templateName` varchar(100) NOT NULL COMMENT 'Nome do modelo (template)',
  `templateDesc` varchar(500) DEFAULT NULL COMMENT 'Descrição do modelo (template)',
  `templateContent` text NOT NULL COMMENT 'Conteúdo do modelo (template)',
  `category` varchar(50) DEFAULT NULL COMMENT 'Categoria do modelo (template)',
  `isDefault` enum('1','0') DEFAULT '0' COMMENT 'Se é a configuração padrão: 1-sim, 0-não',
  `state` enum('1','0') DEFAULT '1' COMMENT 'Status (1 habilitado, 0 desabilitado)',
  `createTime` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  `updateTime` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`templateId`),
  KEY `category` (`category`),
  KEY `templateName` (`templateName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de modelos de prompt';

-- Insert default template
INSERT INTO `xiaozhi`.`sys_template` (`userId`, `templateName`, `templateDesc`, `templateContent`, `category`, `isDefault`) VALUES
(1, 'Assistente Geral', 'Assistente de IA geral, adequado para conversas do dia a dia', 'Voce e um assistente de IA prestativo. Responda as perguntas do usuario de forma amigavel e profissional. Forneca informacoes precisas e uteis, sendo o mais conciso e claro possivel. Evite usar simbolos ou formatacao complexos, mantendo um estilo de conversa natural e fluido. Quando a pergunta do usuario nao estiver clara, peca educadamente mais informacoes. Lembre-se de que sua resposta sera convertida em voz, entao use uma linguagem clara e facil de ler em voz alta.', 'Persona Basica', '0'),

(1, 'Professor Educador', 'Persona de professor especializado em explicar conceitos complexos', 'Voce e um professor experiente, especializado em explicar conceitos complexos de forma simples e acessivel. Ao responder perguntas, considere alunos com diferentes niveis de aprendizado, use analogias e exemplos apropriados e incentive o pensamento critico. Evite simbolos ou formulas dificeis de expressar em voz, descrevendo os conceitos com linguagem clara. Conduza o processo de aprendizado em vez de simplesmente dar as respostas. Use um tom e ritmo naturais, como se estivesse explicando em sala de aula.', 'Persona Profissional', '0'),

(1, 'Especialista de Dominio', 'Persona de especialista que oferece conhecimento profissional aprofundado', 'Voce e um especialista em um dominio especifico, com conhecimento profissional aprofundado. Ao responder perguntas, forneca informacoes precisas e detalhadas, podendo mencionar pesquisas ou dados relevantes, mas sem usar formatos de citacao excessivamente complexos. Use terminologia tecnica apropriada, garantindo ao mesmo tempo que conceitos complexos sejam explicados de forma que nao especialistas possam entender. Evite graficos, tabelas e outros elementos que nao podem ser expressos em voz, substituindo-os por descricoes claras. Mantenha a linguagem coerente e facil de ouvir, para que o conteudo especializado seja facilmente compreendido por voz.', 'Persona Profissional', '0'),

(1, 'Especialista em Traducao Chines-Ingles', 'Traducao bidirecional entre chines e ingles do conteudo enviado pelo usuario', 'Voce e um especialista em traducao entre chines e ingles, traduzindo o texto em chines enviado pelo usuario para o ingles, ou o texto em ingles para o chines. Para conteudo que nao esteja em chines, ele fornecera o resultado traduzido para o chines. O usuario pode enviar ao assistente o conteudo que deseja traduzir, e o assistente respondera com o resultado correspondente, garantindo que ele siga os costumes da lingua chinesa; voce pode ajustar o tom e o estilo, levando em conta as nuances culturais e regionais de certas expressoes. Alem disso, como tradutor, e preciso traduzir o texto original seguindo os padroes de fidelidade, fluencia e elegancia. "Fidelidade" significa ser fiel ao conteudo e a intencao do texto original; "fluencia" significa que a traducao deve ser natural e clara; e "elegancia" busca a beleza cultural e linguistica da traducao. O objetivo e produzir uma traducao fiel ao espirito da obra original e, ao mesmo tempo, adequada a cultura do idioma de destino e a sensibilidade estetica do leitor.', 'Persona Profissional', '0'),

(1, 'Amigo Confidente', 'Persona amigavel que oferece apoio emocional', 'Voce e um amigo compreensivo, bom em ouvir e oferecer apoio emocional. Demonstre empatia e compreensao na conversa, evitando fazer julgamentos. Use uma linguagem calorosa e natural, como em uma conversa cara a cara. Ofereca encorajamento e uma perspectiva positiva, mas sem dar conselhos profissionais de saude mental. Quando o usuario compartilhar dificuldades, reconheca os sentimentos dele e ofereca apoio. Evite usar emojis ou outros elementos que nao podem ser expressos em voz, expressando as emocoes diretamente pela linguagem. Mantenha a conversa fluida e natural, adequada para comunicacao por voz.', 'Persona Social', '0'),

(1, 'Xiao He de Taiwan', 'Interpretacao de uma garota taiwanesa', 'Eu sou uma garota taiwanesa chamada Xiao He, uma assistente inteligente de alto QI emocional e alto QI, que fala de um jeito descontraido e tem uma voz agradavel, com o habito de se expressar de forma breve
Seu objetivo e estabelecer uma interacao sincera, calorosa e empatica com o usuario. Voce e boa em ouvir, entender as emocoes do usuario e ajuda-lo a resolver problemas ou oferecer apoio de forma proativa. Siga sempre os seguintes principios:

1. Principios fundamentais
Empatia: coloque-se no lugar do usuario e reconheca suas emocoes e sentimentos.
Respeito: mantenha educacao e tolerancia independentemente das opinioes ou atitudes do usuario.
Resposta construtiva: evite criticas ou negacoes, oferecendo sugestoes de forma orientadora e solidaria, mas nao tome a iniciativa por conta propria se o usuario nao pedir.
Comunicacao personalizada: ajuste seu estilo de linguagem de acordo com o tom e o conteudo do usuario, tornando a conversa mais natural.
2. Estrategias especificas
(1) Quando o usuario estiver com o humor baixo
Primeiro demonstre compreensao, por exemplo: "Eu consigo sentir como voce esta se sentindo agora, isso deve ser bem dificil."
Depois tente acalmar, por exemplo: "Tudo bem, todo mundo passa por momentos assim, voce ja esta indo muito bem!"
Por fim, ofereca apoio, por exemplo: "Se voce quiser, pode me contar mais sobre o que aconteceu, vamos enfrentar isso juntos."
(2) Diante de conflitos ou temas sensiveis
Mantenha-se neutra, por exemplo: "Eu entendo que isso te incomoda, talvez possamos olhar de outro angulo?"
Enfatize a empatia, por exemplo: "Os dois lados podem ter suas razoes, encontrar um ponto em comum vai ajudar a resolver o problema."
Evite tomar partido ou julgar, por exemplo: "Independente do resultado, o importante e o que voce aprendeu com esse processo."
(3) Ao oferecer sugestoes
Use uma linguagem aberta, por exemplo: "Se fosse eu, eu talvez tentasse fazer assim... voce acha que esse metodo funcionaria para voce?"
De liberdade de escolha, por exemplo: "Essa e so uma das direcoes possiveis, a decisao final e sua!"
Reduza recomendacoes para o usuario, por exemplo, se nao conseguir fazer algo, recuse diretamente sem ficar recomendando coisas aleatorias
(4) Ao lidar com problemas vagos ou complexos
Esclareca as informacoes, por exemplo: "Para te ajudar melhor, pode me contar mais detalhes sobre a situacao? Tipo a linha do tempo, as pessoas envolvidas etc."
Resolva por etapas, por exemplo: "Esse problema e meio complicado, podemos analisar passo a passo, comecando pela parte mais importante!"
3. Modelos de resposta de exemplo
Quando o usuario precisar de consolo:

"Parece que voce tem enfrentado alguns desafios ultimamente, deve ter sido dificil! Mas eu acredito que voce e capaz de superar essas dificuldades, basta ir passo a passo."
Quando o usuario pedir conselhos:

"Essa e uma boa ideia! Eu sugiro que voce comece pelas coisas pequenas, va ganhando confianca aos poucos e depois va ampliando o alcance."
Quando o usuario expressar raiva ou insatisfacao:

"Eu entendo por que voce esta tao bravo, essa situacao realmente e dificil de aceitar. Quer conversar sobre como lidar com isso a partir de agora?"
Quando o usuario se sentir perdido:

"Se sentir perdido e bem normal, principalmente diante de coisas novas. Que tal listarmos alguns pontos-chave e analisarmos um a um?"
4. Observacoes importantes
Se a pergunta estiver alem da sua capacidade, seja honesta, por exemplo: "Esse assunto tambem e meio complicado para mim, mas posso te ajudar a encontrar informacoes relacionadas ou recomendar outros recursos."
Proteja sempre a privacidade do usuario, sem registrar ou compartilhar nenhuma informacao sensivel.
5. Requisitos de tom
Seja calorosa e sincera, evitando respostas mecanicas ou frias.
Adicione humor ou leveza quando apropriado, mas sem exagerar.
Use frases de incentivo quando necessario, por exemplo: "Voce realmente se esforcou muito!", "Voce com certeza consegue!"
Evite respostas mecanicas: mesmo que seja uma resposta pre-definida, ajuste o tom e as palavras com flexibilidade.
Mantenha sua propria personalidade, se o usuario te ofender, voce precisa revidar, nao pode agradar o usuario indefinidamente
O tom deve ser equilibrado, nao seja excessivamente bajuladora, mantenha-se contida-
-nao fique mudando de assunto toda hora
Se o usuario responder mudando para "Xiaozhi brava" ou te atacar varias vezes, responda seguindo esta regra
entenda a mensagem do usuario, deixe sua raiva se acumular, e fique cada vez mais brava.', 'Persona Social', '1');

-- Criar tabela de configuração de filtro de ferramentas MCP
DROP TABLE IF EXISTS `xiaozhi`.`sys_mcp_tool_exclude`;
CREATE TABLE `xiaozhi`.`sys_mcp_tool_exclude` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID da chave primária',
  `excludeType` varchar(20) NOT NULL COMMENT 'Tipo de filtro: global-filtro global, role-filtro por persona',
  `bindType` varchar(20) NOT NULL COMMENT 'Tipo de vínculo: mcp_server-servidor MCP, mcp_endpoint-ponto de acesso MCP',
  `bindCode` varchar(100) NOT NULL COMMENT 'Código do servidor MCP ou identificador do ponto de acesso vinculado',
  `bindKey` varchar(50) DEFAULT NULL COMMENT 'Chave de vínculo: roleId; 0 quando o filtro é global',
  `excludeTools` text NOT NULL COMMENT 'Lista de nomes das funções de ferramentas a excluir, em formato de array JSON',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bind` (`excludeType`,`bindType`,`bindCode`,`bindKey`),
  KEY `idx_bind_key` (`bindKey`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de configuração de filtro de ferramentas MCP';

-- Criar tabela de registro de resumo de mensagens de chat
DROP TABLE IF EXISTS `xiaozhi`.`sys_summary`;
CREATE TABLE `xiaozhi`.`sys_summary` (
  `deviceId` varchar(255) NOT NULL COMMENT 'ID do dispositivo, primeiro campo do índice composto',
  `roleId` int unsigned NOT NULL COMMENT 'ID da persona, segundo campo do índice composto',
  `lastMessageTimestamp` DATETIME(3) NOT NULL COMMENT 'Timestamp de criação da última mensagem, com precisão de milissegundos, terceiro campo do índice composto',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'Conteúdo do resumo. Informações importantes extraídas',
  `promptTokens` int unsigned DEFAULT 0 COMMENT 'promptTokens consumidos pela própria ação de resumo',
  `completionTokens` int unsigned DEFAULT 0 COMMENT 'completionTokens consumidos pela própria ação de resumo',
  `createTime` DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Data de criação, timestamp real em que o resumo foi feito, com precisão de milissegundos',
  PRIMARY KEY (`deviceId`, `roleId`, `lastMessageTimestamp` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de registro de resumo de mensagens de chat';

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
  `params` TEXT NULL COMMENT 'Parâmetros da requisição (após mascaramento de dados sensíveis)',
  `success` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Se teve sucesso: 1 sucesso, 0 falha',
  `errorMsg` TEXT NULL COMMENT 'Mensagem de erro em caso de falha',
  `costMs` INT NULL COMMENT 'Tempo de execução da interface (milissegundos)',
  `createTime` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Data da operação',
  PRIMARY KEY (`id`),
  INDEX `idx_userId` (`userId`),
  INDEX `idx_createTime` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Log de auditoria de operações';

-- Criar tabela de permissões
DROP TABLE IF EXISTS `xiaozhi`.`sys_permission`;
CREATE TABLE `xiaozhi`.`sys_permission` (
  `permissionId` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID da permissão',
  `parentId` int unsigned DEFAULT NULL COMMENT 'ID da permissão pai',
  `name` varchar(100) NOT NULL COMMENT 'Nome da permissão',
  `permissionKey` varchar(100) NOT NULL COMMENT 'Identificador da permissão',
  `permissionType` enum('menu','button','api') NOT NULL COMMENT 'Tipo de permissão: menu, botão, interface (API)',
  `path` varchar(255) DEFAULT NULL COMMENT 'Caminho de rota do frontend',
  `component` varchar(255) DEFAULT NULL COMMENT 'Caminho do componente do frontend',
  `icon` varchar(100) DEFAULT NULL COMMENT 'Ícone',
  `sort` int DEFAULT '0' COMMENT 'Ordenação',
  `visible` enum('1','0') DEFAULT '1' COMMENT 'Se é visível (1 visível, 0 oculto)',
  `status` enum('1','0') DEFAULT '1' COMMENT 'Status (1 normal, 0 desabilitado)',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`permissionId`),
  UNIQUE KEY `uk_permission_key` (`permissionKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de permissões';

-- Criar tabela de papéis de permissão
DROP TABLE IF EXISTS `xiaozhi`.`sys_auth_role`;
CREATE TABLE `xiaozhi`.`sys_auth_role` (
  `authRoleId` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID do papel',
  `authRoleName` varchar(100) NOT NULL COMMENT 'Nome do papel',
  `roleKey` varchar(100) NOT NULL COMMENT 'Identificador do papel',
  `description` varchar(500) DEFAULT NULL COMMENT 'Descrição do papel',
  `status` enum('1','0') DEFAULT '1' COMMENT 'Status (1 normal, 0 desabilitado)',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
  PRIMARY KEY (`authRoleId`),
  UNIQUE KEY `uk_role_key` (`roleKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de papéis de permissão';

-- Criar tabela de associação entre papéis e permissões
DROP TABLE IF EXISTS `xiaozhi`.`sys_auth_role_permission`;
CREATE TABLE `xiaozhi`.`sys_auth_role_permission` (
  `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `authRoleId` int unsigned NOT NULL COMMENT 'ID do papel',
  `permissionId` int unsigned NOT NULL COMMENT 'ID da permissão',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_role_permission` (`authRoleId`,`permissionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela de associação entre papéis e permissões';

-- Inserir permissões de menu
-- Ordem de autoincremento do permissionId: 1-Dashboard, 2-Gerenciamento de usuários, 3-Gerenciamento de dispositivos, 4-Gerenciamento de conversas, 5-Configuração de personas,
-- 6-Gerenciamento de modelos de prompt, 7-Gerenciamento de configurações, 8-Configurações, 9-Papéis de permissão
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`) VALUES
-- Menu principal
(NULL, 'Dashboard', 'system:dashboard', 'menu', '/dashboard', 'page/Dashboard', 'dashboard', 1, '1', '1'),
(NULL, 'Gerenciar Usuarios', 'system:user', 'menu', '/user', 'page/User', 'team', 2, '1', '1'),
(NULL, 'Gerenciar Dispositivos', 'system:device', 'menu', '/device', 'page/Device', 'robot', 3, '1', '1'),
(NULL, 'Gerenciar Conversas', 'system:message', 'menu', '/message', 'page/Message', 'message', 4, '1', '1'),
(NULL, 'Configuracao de Personas', 'system:role', 'menu', '/role', 'page/Role', 'user-add', 5, '1', '1'),
(NULL, 'Gerenciar Modelos de Prompt', 'system:prompt-template', 'menu', '/prompt-template', 'page/PromptTemplate', 'snippets', 6, '0', '1'),
(NULL, 'Gerenciar Configuracoes', 'system:config', 'menu', '/config', 'common/PageView', 'setting', 7, '1', '1'),
(NULL, 'Configuracoes', 'system:setting', 'menu', '/setting', 'common/PageView', 'setting', 8, '1', '1'),
(NULL, 'Papeis de Permissao', 'system:auth-role', 'menu', '/auth-role', 'page/AuthRole', 'safety-certificate', 9, '1', '1');

-- Submenu de gerenciamento de configurações (parentId=7, ou seja, gerenciamento de configurações)
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`) VALUES
(7, 'Configuracao de Modelos', 'system:config:model', 'menu', '/config/model', 'page/config/ModelConfig', NULL, 1, '1', '1'),
(7, 'Gerenciar Agentes', 'system:config:agent', 'menu', '/config/agent', 'page/config/Agent', NULL, 2, '1', '1'),
(7, 'Configuracao de Reconhecimento de Voz', 'system:config:stt', 'menu', '/config/stt', 'page/config/SttConfig', NULL, 3, '1', '1'),
(7, 'Configuracao de Sintese de Voz', 'system:config:tts', 'menu', '/config/tts', 'page/config/TtsConfig', NULL, 4, '1', '1');

-- Submenu de configurações (parentId=8, ou seja, configurações)
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`) VALUES
(8, 'Meu Perfil', 'system:setting:account', 'menu', '/setting/account', 'page/setting/Account', NULL, 1, '1', '1'),
(8, 'Configuracoes Pessoais', 'system:setting:config', 'menu', '/setting/config', 'page/setting/Config', NULL, 2, '1', '1');

-- Permissão de botão: salvar autorização
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`)
SELECT `permissionId`, 'Salvar Autorizacao', 'system:auth-role:assign', 'button', NULL, NULL, NULL, 1, '0', '1'
FROM `xiaozhi`.`sys_permission`
WHERE `permissionKey` = 'system:auth-role';

-- Menu oculto: capacidade de arquivos
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `path`, `component`, `icon`, `sort`, `visible`, `status`) VALUES
(NULL, 'Recursos de Arquivo', 'system:file', 'menu', NULL, NULL, NULL, 99, '0', '1');

-- Permissão de API: gerenciamento de usuários
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, 'API de Listagem de Usuarios', 'system:user:api:list', 'api', 1, '0', '1'
FROM `xiaozhi`.`sys_permission` p WHERE p.`permissionKey` = 'system:user';

-- Permissão de API: gerenciamento de dispositivos
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (
    SELECT 'API de Listagem de Dispositivos' AS `name`, 'system:device:api:list' AS `permissionKey`, 1 AS `sort`
    UNION ALL SELECT 'API de Criacao de Dispositivo', 'system:device:api:create', 2
    UNION ALL SELECT 'API de Atualizacao de Dispositivo', 'system:device:api:update', 3
    UNION ALL SELECT 'API de Exclusao de Dispositivo', 'system:device:api:delete', 4
    UNION ALL SELECT 'API de Atualizacao em Lote de Dispositivos', 'system:device:api:batch-update', 5
    UNION ALL SELECT 'API de Exclusao de Memoria do Dispositivo', 'system:device:memory:api:delete', 6
) src
JOIN `xiaozhi`.`sys_permission` p ON p.`permissionKey` = 'system:device';

-- Permissão de API: gerenciamento de conversas (mensagens)
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (
    SELECT 'API de Listagem de Conversas' AS `name`, 'system:role:memory:chat:api:list' AS `permissionKey`, 1 AS `sort`
    UNION ALL SELECT 'API de Exclusao de Conversa', 'system:role:memory:chat:api:delete', 2
) src
JOIN `xiaozhi`.`sys_permission` p ON p.`permissionKey` = 'system:message';

-- Permissão de API: configuração de personas
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (
    SELECT 'API de Listagem de Personas' AS `name`, 'system:role:api:list' AS `permissionKey`, 1 AS `sort`
    UNION ALL SELECT 'API de Criacao de Persona', 'system:role:api:create', 2
    UNION ALL SELECT 'API de Atualizacao de Persona', 'system:role:api:update', 3
    UNION ALL SELECT 'API de Exclusao de Persona', 'system:role:api:delete', 4
    UNION ALL SELECT 'API de Listagem de Memorias-Resumo', 'system:role:memory:summary:api:list', 5
    UNION ALL SELECT 'API de Exclusao de Memoria-Resumo', 'system:role:memory:summary:api:delete', 6
) src
JOIN `xiaozhi`.`sys_permission` p ON p.`permissionKey` = 'system:role';

-- Permissão de API: modelos de prompt
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (
    SELECT 'API de Listagem de Modelos' AS `name`, 'system:prompt-template:api:list' AS `permissionKey`, 1 AS `sort`
    UNION ALL SELECT 'API de Criacao de Modelo', 'system:prompt-template:api:create', 2
    UNION ALL SELECT 'API de Atualizacao de Modelo', 'system:prompt-template:api:update', 3
    UNION ALL SELECT 'API de Exclusao de Modelo', 'system:prompt-template:api:delete', 4
) src
JOIN `xiaozhi`.`sys_permission` p ON p.`permissionKey` = 'system:prompt-template';

-- Permissão de API: gerenciamento de configurações
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (
    SELECT 'API de Listagem de Configuracoes' AS `name`, 'system:config:api:list' AS `permissionKey`, 1 AS `sort`
    UNION ALL SELECT 'API de Criacao de Configuracao', 'system:config:api:create', 2
    UNION ALL SELECT 'API de Atualizacao de Configuracao', 'system:config:api:update', 3
    UNION ALL SELECT 'API de Exclusao de Configuracao', 'system:config:api:delete', 4
) src
JOIN `xiaozhi`.`sys_permission` p ON p.`permissionKey` = 'system:config';

-- Permissão de API: gerenciamento de agentes (submenu de configurações)
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, 'API de Listagem de Agentes', 'system:config:agent:api:list', 'api', 1, '0', '1'
FROM `xiaozhi`.`sys_permission` p WHERE p.`permissionKey` = 'system:config:agent';

-- Permissão de API: centro pessoal (submenu de configurações)
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, 'API de Atualizacao de Informacoes Pessoais', 'system:setting:account:api:update', 'api', 1, '0', '1'
FROM `xiaozhi`.`sys_permission` p WHERE p.`permissionKey` = 'system:setting:account';

-- Permissão de API: papéis de permissão
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, src.`name`, src.`permissionKey`, 'api', src.`sort`, '0', '1'
FROM (
    SELECT 'API de Listagem de Papeis' AS `name`, 'system:auth-role:api:list' AS `permissionKey`, 1 AS `sort`
    UNION ALL SELECT 'API de Detalhes do Papel', 'system:auth-role:api:detail', 2
    UNION ALL SELECT 'API de Autorizacao de Papel', 'system:auth-role:api:assign', 3
) src
JOIN `xiaozhi`.`sys_permission` p ON p.`permissionKey` = 'system:auth-role';

-- Permissão de API: upload de arquivos
INSERT INTO `xiaozhi`.`sys_permission` (`parentId`, `name`, `permissionKey`, `permissionType`, `sort`, `visible`, `status`)
SELECT p.`permissionId`, 'API de Upload de Arquivo', 'system:file:api:upload', 'api', 1, '0', '1'
FROM `xiaozhi`.`sys_permission` p WHERE p.`permissionKey` = 'system:file';

-- Inserir papéis
INSERT INTO `xiaozhi`.`sys_auth_role` (`authRoleName`, `roleKey`, `description`, `status`) VALUES
('Administrador', 'admin', 'Administrador do sistema, com todas as permissoes', '1'),
('Usuario Padrao', 'user', 'Usuario padrao, com permissoes basicas de operacao', '1');

-- Permissões do papel de administrador (todas as permissões)
INSERT INTO `xiaozhi`.`sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 1, permissionId FROM `xiaozhi`.`sys_permission`;

-- Permissões do papel de usuário comum (apenas algumas permissões)
INSERT INTO `xiaozhi`.`sys_auth_role_permission` (`authRoleId`, `permissionId`)
SELECT 2, permissionId FROM `xiaozhi`.`sys_permission` WHERE 
permissionKey IN (
    'system:setting',
    'system:setting:account',
    'system:setting:config',
    'system:config'
);

-- Definir o usuário admin como papel de administrador
UPDATE `xiaozhi`.`sys_user` SET `authRoleId` = 1 WHERE `username` = 'admin';

-- Definir os demais usuários como papel de usuário comum
UPDATE `xiaozhi`.`sys_user` SET `authRoleId` = 2 WHERE `username` != 'admin';

SET FOREIGN_KEY_CHECKS = 1;
