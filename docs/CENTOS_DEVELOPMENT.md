# Guia de Implantação no CentOS

## Requisitos do Sistema

| Item | Requisito |
|------|------|
| Sistema | CentOS 7/8 |
| Memória | ≥ 2GB (recomendado 4GB) |
| Disco | ≥ 10GB |
| Portas | 8084, 8091, 8092, 3306 |

## 1. Instalar Dependências

```bash
sudo yum install -y epel-release wget curl git vim unzip
sudo yum install -y java-21-openjdk java-21-openjdk-devel maven
curl -sL https://rpm.nodesource.com/setup_22.x | sudo bash -
sudo yum install -y nodejs
```

## 2. Configurar o Firewall

```bash
sudo firewall-cmd --permanent --add-port={8084,8091,8092,3306}/tcp
sudo firewall-cmd --reload
```

## 3. Instalar o MySQL 8.0

```bash
sudo yum localinstall -y https://dev.mysql.com/get/mysql80-community-release-el7-7.noarch.rpm
sudo yum install -y mysql-community-server
sudo systemctl start mysqld && sudo systemctl enable mysqld
sudo grep 'temporary password' /var/log/mysqld.log   # obter a senha temporária
sudo mysql_secure_installation
```

Criar o banco de dados:

```sql
mysql -u root -p
CREATE DATABASE xiaozhi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'xiaozhi'@'localhost' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON xiaozhi.* TO 'xiaozhi'@'localhost';
FLUSH PRIVILEGES;
```

> Não é necessário importar o SQL manualmente — o projeto integra o Flyway, que cria as tabelas automaticamente na primeira inicialização.

## 4. Baixar Modelos e Bibliotecas Nativas

Se estiver usando serviços de STT/TTS de terceiros, é possível baixar somente as dependências básicas.

```bash
./scripts/download_models.sh            # baixa tudo (modelos + bibliotecas nativas)
./scripts/download_models.sh status     # verifica o status
```

Também é possível baixar individualmente, conforme a necessidade:

```bash
./scripts/download_base.sh              # dependências básicas (modelo VAD + bibliotecas nativas) — obrigatório
./scripts/download_stt.sh               # modelo Vosk STT (pode ser pulado se usar STT de terceiros)
./scripts/download_tts.sh               # modelo TTS (pode ser pulado se usar TTS de terceiros)
```

## 5. Implantação

O projeto adota uma **arquitetura de dois processos**:

| Serviço | Porta | Descrição |
|------|------|------|
| xiaozhi-server | 8091 | API do painel administrativo, gerenciamento de usuários/dispositivos |
| xiaozhi-dialogue | 8092 | Diálogo com dispositivos, IA, WebSocket |

```bash
git clone https://github.com/joey-zhou/xiaozhi-esp32-server-java
cd xiaozhi-esp32-server-java

# Compilar e iniciar com um único comando
bin/all.sh start

# Verificar status
bin/all.sh status

# Parar / reiniciar
bin/all.sh stop
bin/all.sh restart
```

Também é possível gerenciar cada processo separadamente: `bin/server.sh start`, `bin/dialogue.sh start`

Frontend:

```bash
cd web && npm install && npm run build
```

## 6. Proxy Reverso Nginx (opcional)

```nginx
server {
    listen 80;
    server_name your_domain_or_ip;

    location / {
        root /path/to/xiaozhi-esp32-server-java/web/dist;
        try_files $uri $uri/ /index.html;
    }
    location /api {
        proxy_pass http://localhost:8091;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    location /ws {
        proxy_pass http://localhost:8092;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

## 7. Acesso

| Serviço | Endereço |
|------|------|
| Frontend | http://your_server_ip:8084 |
| API do painel administrativo | http://your_server_ip:8091 |
| WebSocket | ws://your_server_ip:8092/ws/xiaozhi/v1/ |

Administrador padrão: admin / 123456

## Manutenção

```bash
bin/all.sh status                          # verificar status
tail -f logs/xiaozhi-server.log            # verificar logs
tail -f logs/xiaozhi-dialogue.log
git pull origin main && bin/all.sh restart # atualizar e reiniciar
mysqldump -u root -p xiaozhi > backup.sql  # backup do banco de dados
```

## Problemas Comuns

| Problema | Solução |
|------|------|
| Falha na inicialização do MySQL | `sudo systemctl restart mysqld` |
| Conflito de portas | `netstat -tulnp \| grep <porta>` para localizar e encerrar (kill) o processo que está usando a porta |
| Memória insuficiente | Adicionar swap: `sudo dd if=/dev/zero of=/swapfile bs=1M count=2048 && sudo mkswap /swapfile && sudo swapon /swapfile` |
| Falha ao carregar modelo | `chmod -R 755 models` |
