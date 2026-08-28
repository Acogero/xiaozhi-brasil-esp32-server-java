# Compilar o Firmware do ESP32

1. Baixe o `xiaozhi-esp32`
   projeto e configure o ambiente seguindo este tutorial [Configurando o ambiente de desenvolvimento ESP IDF 5.3.2 no Windows e compilando o Xiaozhi](https://icnynnzcwou8.feishu.cn/wiki/JEYDwTTALi5s2zkGlFGcDiRknXf)

# Firmware de versões anteriores à 1.6.2

2. Abra o arquivo `xiaozhi-esp32/main/Kconfig.projbuild`, encontre o conteúdo de `default` da variável `WEBSOCKET_URL` e substitua `wss://api.tenclass.net`
   pelo seu próprio endereço. Por exemplo, se o endereço da minha API é `ws://192.168.1.25:8091`, altere o conteúdo para esse valor.

Antes da alteração:

```
config WEBSOCKET_URL
    depends on CONNECTION_TYPE_WEBSOCKET
    string "Websocket URL"
    default "wss://api.tenclass.net/xiaozhi/v1/"
    help
        Communication with the server through websocket after wake up.
```

Depois da alteração (exemplo):

```
config WEBSOCKET_URL
    depends on CONNECTION_TYPE_WEBSOCKET
    string "Websocket URL"
    default "ws://192.168.5.167:8091/ws/xiaozhi/v1/"
    help
        Communication with the server through websocket after wake up.
```

Atenção: seu endereço começa com `ws://`, não com `wss://` — não erre isso.

Atenção: seu endereço começa com `ws://`, não com `wss://` — não erre isso.

Atenção: seu endereço começa com `ws://`, não com `wss://` — não erre isso.

# Firmware a partir da versão 1.6.2

Encontre o conteúdo de `default` da variável `OTA_URL` e substitua `https://api.tenclass.net/xiaozhi/ota/`
   pelo seu próprio endereço. Por exemplo, se o endereço da minha API é `http://192.168.5.165:8091/api/device/ota/`, altere o conteúdo para esse valor.

Antes da alteração:
```
config OTA_VERSION_URL
    string "OTA Version URL"
    default "https://api.tenclass.net/xiaozhi/ota/"
    help
        The application will access this URL to check for updates.
```

Depois da alteração (exemplo):
```
config OTA_VERSION_URL
    string "OTA Version URL"
    default "http://192.168.5.167:8091/api/device/ota"
    help
        The application will access this URL to check for updates.
```

Atenção: seu endereço começa com `http://`, não com `https://` — não erre isso.

Atenção: seu endereço começa com `http://`, não com `https://` — não erre isso.

Atenção: seu endereço começa com `http://`, não com `https://` — não erre isso.


3. Configurar os parâmetros de compilação

```
# No terminal, acesse o diretório raiz do xiaozhi-esp32
cd xiaozhi-esp32
# Por exemplo, como estou usando a placa esp32s3, defino o alvo de compilação como esp32s3; se sua placa for outro modelo, substitua pelo modelo correspondente
idf.py set-target esp32s3
# Acessar o menu de configuração
idf.py menuconfig
```

Após acessar o menu de configuração, entre em `Xiaozhi Assistant` e defina `BOARD_TYPE` com o modelo específico da sua placa
Salve e saia, retornando ao terminal.

4. Compilar o firmware

```
idf.py build
```

Se você instalou o IDF pelo VSCode, pode usar `F1` ou `ctrl+shift+p`, digitar idf e selecionar diretamente a opção de compilação

Também é possível gravar o firmware diretamente, sem precisar seguir os próximos passos

<img src="./images/vscode_idf.png" width="500px"/>

5. Empacotar o firmware bin

```
cd scripts
python release.py
```

Após a compilação bem-sucedida, o arquivo de firmware `merged-binary.bin` será gerado no diretório `build`, na raiz do projeto.
Esse `merged-binary.bin` é o arquivo de firmware que deve ser gravado no hardware.

Atenção: se, após executar o segundo comando, ocorrer um erro relacionado a "zip", ignore esse erro — desde que o arquivo de firmware `merged-binary.bin` tenha sido gerado no diretório `build`
, isso não afeta significativamente o processo; continue normalmente.

6. Gravar o firmware
   Conecte o dispositivo ESP32 ao computador, use o navegador Chrome e abra o seguinte endereço

```
https://espressif.github.io/esp-launchpad/
```

Abra este tutorial, [Ferramenta Flash/Gravação de firmware pela Web (sem ambiente de desenvolvimento IDF)](https://ccnphfhqs21z.feishu.cn/wiki/Zpz4wXBtdimBrLk25WdcXzxcnNS).
Vá até: `Método 2: Gravação via navegador com ESP-Launchpad`, comece em `3. Gravar firmware/Baixar para a placa` e siga as instruções do tutorial.

Após a gravação e a conexão à rede serem bem-sucedidas, desperte o Xiaozhi usando a palavra de ativação e observe as informações exibidas no console do servidor.
