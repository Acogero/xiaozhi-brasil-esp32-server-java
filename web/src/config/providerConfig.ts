/**
 * Configuração dos diversos provedores de serviço do sistema
 * Gerencia de forma centralizada as informações dos provedores de cada tipo de serviço, facilitando a manutenção e a expansão
 */

import type { ConfigTypeInfo } from '@/types/config'

// Mapeamento de informações dos tipos de configuração
export const configTypeMap: Record<string, ConfigTypeInfo> = {
  llm: {
    label: 'config.llm',
    permissionPrefix: 'system:config',
    // Definição dos campos de parâmetros correspondentes a cada categoria
    typeFields: {
      // Série OpenAI
      'OpenAI': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'sk-...',
          span: 12,
          help: 'Obtenha em https://platform.openai.com/api-keys'
        }
      ],
      // Série Alibaba Cloud
      'Tongyi-Qianwen': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://bailian.console.aliyun.com/?apiKey=1#/api-key'
        }
      ],
      // iFlytek Spark
      'XunFei Spark': [
        {
          name: 'appId',
          label: 'App Id',
          required: true,
          inputType: 'text',
          placeholder: 'your-app-id',
          span: 12,
          help: 'Obtenha o AppID da plataforma aberta iFlytek em https://console.xfyun.cn/'
        },
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'API Key da plataforma aberta iFlytek'
        },
        {
          name: 'apiSecret',
          label: 'API Secret',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-secret',
          span: 12,
          help: 'API Secret da plataforma aberta iFlytek'
        }
      ],
      // Zhipu AI
      'ZHIPU-AI': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://bigmodel.cn/usercenter/proj-mgmt/apikeys'
        }
      ],
      // DeepSeek
      'DeepSeek': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://platform.deepseek.com/'
        }
      ],
      // Volcano Engine
      'VolcEngine': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://ark.cn-beijing.volces.com/api/v3',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API do modelo Doubao da Volcano Engine'
        }
      ],
      // MiniMax
      'MiniMax': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://platform.minimaxi.com/'
        }
      ],
      // Tencent Hunyuan
      'Tencent Hunyuan': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha a API Key do Hunyuan em https://console.cloud.tencent.com/hunyuan/start'
        }
      ],
      // Baidu Wenxin
      'BaiChuan': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha na plataforma aberta de IA da Baidu'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.baichuan-ai.com/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API da Baichuan Intelligent'
        }
      ],
      // Moonshot (Dark Side of the Moon)
      'Moonshot': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://platform.moonshot.cn/console/api-keys'
        }
      ],
      // SiliconFlow
      'SILICONFLOW': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://cloud.siliconflow.cn/account/ak'
        }
      ],
      // Baidu ERNIE Bot
      'BaiduYiyan': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha a API Key da plataforma Qianfan em https://console.bce.baidu.com/qianfan/ais/console/applicationConsole/application'
        },
        {
          name: 'apiSecret',
          label: 'Secret Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-secret-key',
          span: 12,
          help: 'Secret Key da plataforma Qianfan'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://aip.baidubce.com/rpc/2.0/ai_custom/v1',
          span: 12,
          suffix: '/wenxinworkshop/chat/completions',
          help: 'Endereço da API da plataforma Qianfan da Baidu'
        }
      ],
      // Outros serviços locais
      'Ollama': [
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'http://localhost:11434/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço do serviço local Ollama; é necessário instalar e iniciar o Ollama antes'
        }
      ],
      'LM-Studio': [
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'http://localhost:1234/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço do serviço local LM Studio'
        }
      ],
      'Azure-OpenAI': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha no portal do Azure'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://your-resource-name.openai.azure.com',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço do serviço Azure OpenAI'
        }
      ],
      // xAI
      'xAI': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://x.ai/api-keys'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.x.ai/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API do xAI'
        }
      ],
      // Mistral
      'Mistral': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://console.mistral.ai/'
        }
      ],
      // Google Gemini
      'Gemini': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://aistudio.google.com/apikey'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://generativelanguage.googleapis.com',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API do Google Gemini'
        }
      ],
      // Groq
      'Groq': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://console.groq.com/'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.groq.com/openai/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API do Groq'
        }
      ],
      // OpenRouter
      'OpenRouter': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://openrouter.ai/'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://openrouter.ai/api/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API do OpenRouter'
        }
      ],
      // StepFun
      'StepFun': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha na plataforma StepFun'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.stepfun.com/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API do StepFun'
        }
      ],
      // NVIDIA
      'NVIDIA': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha na NVIDIA AI Foundation'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://integrate.api.nvidia.com/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API da NVIDIA'
        }
      ],
      // 01.AI
      '01.AI': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://platform.01.ai/'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.01.ai/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API da 01.AI'
        }
      ],
      // Anthropic
      'Anthropic': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://console.anthropic.com/'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.anthropic.com/v1',
          span: 12,
          suffix: '/messages',
          help: 'Endereço da API da Anthropic'
        }
      ],
      // Voyage AI
      'Voyage AI': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://dash.voyageai.com/'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.voyageai.com/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API da Voyage AI'
        }
      ],
      // GiteeAI
      'GiteeAI': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha na plataforma https://ai.gitee.com/'
        }
      ],
      // DeepInfra
      'DeepInfra': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://deepinfra.com/'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.deepinfra.com/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API da DeepInfra'
        }
      ],
      'LocalAI': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: false,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Chave do serviço local LocalAI (opcional)'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'http://localhost:8080/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço do serviço local LocalAI'
        }
      ],
      'VLLM': [
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'http://localhost:8000/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço do serviço local VLLM'
        }
      ],
      'Xinference': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: false,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Chave do serviço local Xinference (opcional)'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'http://localhost:9997/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço do serviço local Xinference'
        }
      ],
      // HuggingFace
      'HuggingFace': [
        {
          name: 'apiKey',
          label: 'API Token',
          required: true,
          inputType: 'password',
          placeholder: 'hf_...',
          span: 12,
          help: 'Obtenha em https://huggingface.co/settings/tokens'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api-inference.huggingface.co/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API HuggingFace Inference'
        }
      ],
      // Cohere
      'Cohere': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://dashboard.cohere.com/api-keys'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.cohere.ai/v1',
          span: 12,
          suffix: '/chat',
          help: 'Endereço da API da Cohere'
        }
      ],
      // TogetherAI
      'TogetherAI': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://api.together.xyz/settings/api-keys'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.together.xyz/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API da Together AI'
        }
      ],
      // Replicate
      'Replicate': [
        {
          name: 'apiKey',
          label: 'API Token',
          required: true,
          inputType: 'password',
          placeholder: 'r8_...',
          span: 12,
          help: 'Obtenha em https://replicate.com/account/api-tokens'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.replicate.com/v1',
          span: 12,
          suffix: '/predictions',
          help: 'Endereço da API da Replicate'
        }
      ],
      // 302.AI
      '302.AI': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha na plataforma https://302.ai/'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.302.ai/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API da 302.AI'
        }
      ],
      // Fish Audio
      'Fish Audio': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha na plataforma https://fish.audio/'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.fish.audio/v1',
          span: 12,
          suffix: '/tts',
          help: 'Endereço da API da Fish Audio'
        }
      ],
      // PPIO
      'PPIO': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha na plataforma https://www.ppio.cloud/'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.ppio.cloud/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API da PPIO'
        }
      ],
      // NovitaAI
      'NovitaAI': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://novita.ai/settings'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.novita.ai/v3',
          span: 12,
          suffix: '/openai/chat/completions',
          help: 'Endereço da API da NovitaAI'
        }
      ],
      // GPUStack
      'GPUStack': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: false,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'API Key do GPUStack implantado localmente (opcional)'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'http://localhost:80/v1-openai',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço do serviço GPUStack'
        }
      ],
      // Upstage
      'Upstage': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://console.upstage.ai/api-keys'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.upstage.ai/v1/solar',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API da Upstage'
        }
      ],
      // LeptonAI
      'LeptonAI': [
        {
          name: 'apiKey',
          label: 'API Token',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-token',
          span: 12,
          help: 'Obtenha em https://dashboard.lepton.ai/'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.lepton.ai/api/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API da Lepton AI'
        }
      ],
      // PerfXCloud
      'PerfXCloud': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha na plataforma https://cloud.perfxlab.cn/'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://cloud.perfxlab.cn/api/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API da PerfXCloud'
        }
      ],
      // Google Cloud
      'Google Cloud': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha em https://console.cloud.google.com/apis/credentials'
        },
        {
          name: 'projectId',
          label: 'Project ID',
          required: true,
          inputType: 'text',
          placeholder: 'your-project-id',
          span: 12,
          help: 'ID do projeto do Google Cloud'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://generativelanguage.googleapis.com/v1',
          span: 12,
          suffix: '/models',
          help: 'Endereço da API do Google Cloud Vertex AI'
        }
      ],
      // Bedrock (AWS)
      'Bedrock': [
        {
          name: 'apiKey',
          label: 'Access Key ID',
          required: true,
          inputType: 'password',
          placeholder: 'your-access-key-id',
          span: 12,
          help: 'Obtenha a AWS Access Key em https://console.aws.amazon.com/iam/'
        },
        {
          name: 'apiSecret',
          label: 'Secret Access Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-secret-access-key',
          span: 12,
          help: 'AWS Secret Access Key'
        },
        {
          name: 'region',
          label: 'AWS Region',
          required: true,
          inputType: 'text',
          placeholder: 'us-east-1',
          span: 12,
          help: 'Região da AWS, como us-east-1'
        }
      ],
      // CometAPI
      'CometAPI': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha na plataforma https://api.comet.com/'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.comet.com/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API da Comet'
        }
      ],
      // DeerAPI
      'DeerAPI': [
        {
          name: 'apiKey',
          label: 'API Key',
          required: true,
          inputType: 'password',
          placeholder: 'your-api-key',
          span: 12,
          help: 'Obtenha na plataforma https://api.deerapi.com/'
        },
        {
          name: 'apiUrl',
          label: 'API URL',
          required: true,
          inputType: 'text',
          placeholder: 'https://api.deerapi.com/v1',
          span: 12,
          suffix: '/chat/completions',
          help: 'Endereço da API da DeerAPI'
        }
      ]
    }
  },
  stt: {
    label: 'config.stt',
    permissionPrefix: 'system:config',
    typeOptions: [
      { label: 'Tencent', value: 'tencent', key: '0' },
      {
        label: 'Aliyun（DashScope）',
        value: 'aliyun',
        key: '1',
        configNameOptions: [
          'paraformer-realtime-8k-v2',
          'paraformer-realtime-8k-v1',
          'paraformer-realtime-v2',
          'paraformer-realtime-v1',
          'fun-asr-realtime',
          'fun-asr-realtime-2025-11-07',
          'fun-asr-realtime-2025-09-15',
          'fun-asr-flash-8k-realtime',
          'fun-asr-flash-8k-realtime-2026-01-28',
          'gummy-realtime-v1',
          'gummy-chat-v1',
          'qwen3-asr-flash-realtime',
        ]
      },
      { label: 'Aliyun (versão padrão NLS)', value: 'aliyun-nls', key: '2' },
      { label: 'Xfyun', value: 'xfyun', key: '3' },
      { label: 'FunASR', value: 'funasr', key: '4' },
      { label: 'Volcengine（doubao）', value: 'volcengine', key: '5' }
    ],
    typeFields: {
      tencent: [
        { 
          name: 'appId', 
          label: 'App Id', 
          required: true, 
          span: 12,
          help: 'Obtenha em https://console.cloud.tencent.com/cam/capi',
          placeholder: 'your-app-id'
        },
        { 
          name: 'apiKey', 
          label: 'Secret Id', 
          required: true, 
          span: 12,
          help: 'ID da chave de API da Tencent Cloud',
          placeholder: 'your-secret-id'
        },
        { 
          name: 'apiSecret', 
          label: 'Secret Key', 
          required: true, 
          span: 12,
          help: 'Key da chave de API da Tencent Cloud',
          placeholder: 'your-secret-key'
        },
      ],
      aliyun: [
        { 
          name: 'apiKey', 
          label: 'App Key', 
          required: true, 
          span: 12,
          help: 'Obtenha em https://bailian.console.aliyun.com/?apiKey=1#/api-key',
          placeholder: 'your-app-key'
        }
      ],
      'aliyun-nls': [
        {
          name: 'ak',
          label: 'Access Key',
          required: true,
          span: 12,
          help: 'Access Key da Alibaba Cloud, obtenha em https://ram.console.aliyun.com/profile/access-keys',
          placeholder: 'your-access-key'
        },
        {
          name: 'sk',
          label: 'Secret Key',
          required: true,
          inputType: 'password',
          span: 12,
          help: 'Secret Key da Alibaba Cloud, chave correspondente ao Access Key',
          placeholder: 'your-secret-key'
        },
        {
          name: 'apiKey',
          label: 'App Key',
          required: true,
          span: 12,
          help: 'App Key da interação de voz inteligente da Alibaba Cloud, obtenha em https://nls-portal.console.aliyun.com/applist',
          placeholder: 'your-app-key'
        }
      ],
      xfyun: [
        { 
          name: 'appId', 
          label: 'App Id', 
          required: true, 
          span: 12,
          help: 'Obtenha o AppID da plataforma aberta iFlytek em https://console.xfyun.cn/',
          placeholder: 'your-app-id'
        },
        { 
          name: 'apiSecret', 
          label: 'Api Secret', 
          required: true, 
          span: 12,
          help: 'API Secret da plataforma aberta iFlytek',
          placeholder: 'your-api-secret'
        },
        { 
          name: 'apiKey', 
          label: 'Api Key', 
          required: true, 
          span: 12,
          help: 'API Key da plataforma aberta iFlytek',
          placeholder: 'your-api-key'
        }
      ],
      funasr: [
        { 
          name: 'apiUrl', 
          label: 'Websocket URL', 
          required: true, 
          span: 12, 
          defaultUrl: "ws://127.0.0.1:10095",
          help: 'Endereço WebSocket do serviço local FunASR; é necessário implantar o serviço FunASR antes',
          placeholder: 'ws://127.0.0.1:10095'
        }
      ],
      volcengine: [
        { 
          name: 'appId', 
          label: 'App ID', 
          required: true, 
          span: 12,
          help: 'Obtenha o ID do aplicativo em https://console.volcengine.com/speech/app',
          placeholder: 'your-app-id'
        },
        { 
          name: 'apiKey', 
          label: 'Access Token', 
          required: true, 
          inputType: 'password',
          span: 12,
          help: 'Token de acesso do serviço de reconhecimento de voz da Volcano Engine',
          placeholder: 'your-access-token'
        }
      ]
    }
  },
  tts: {
    label: 'config.tts',
    permissionPrefix: 'system:config',
    typeOptions: [
      { label: 'Tencent', value: 'tencent', key: '0' },
      { label: 'Aliyun', value: 'aliyun', key: '1' },
      { label: 'Aliyun NLS', value: 'aliyun-nls', key: '2' },
      { label: 'Volcengine(doubao)', value: 'volcengine', key: '3' },
      { label: 'Xfyun', value: 'xfyun', key: '4' },
      { label: 'Minimax', value: 'minimax', key: '5' },
      { label: 'Sherpa-ONNX (local)', value: 'sherpa-onnx', key: '6' }
    ],
    typeFields: {
      tencent: [
        {
          name: 'appId',
          label: 'App Id',
          required: true,
          span: 12,
          help: 'Obtenha em https://console.cloud.tencent.com/cam/capi',
          placeholder: 'your-app-id'
        },
        {
          name: 'apiKey',
          label: 'Secret Id',
          required: true,
          span: 12,
          help: 'ID da chave de API da Tencent Cloud',
          placeholder: 'your-secret-id'
        },
        {
          name: 'apiSecret',
          label: 'Secret Key',
          required: true,
          span: 12,
          help: 'Key da chave de API da Tencent Cloud',
          placeholder: 'your-secret-key'
        },
      ],
      aliyun: [
        { 
          name: 'apiKey', 
          label: 'API Key', 
          required: true, 
          span: 12,
          help: 'Obtenha em https://bailian.console.aliyun.com/?apiKey=1#/api-key',
          placeholder: 'your-api-key'
        }
      ],
      'aliyun-nls': [
        {
          name: 'ak',
          label: 'Access Key',
          required: true,
          span: 12,
          help: 'Access Key da Alibaba Cloud, obtenha em https://ram.console.aliyun.com/profile/access-keys',
          placeholder: 'your-access-key'
        },
        {
          name: 'sk',
          label: 'Secret Key',
          required: true,
          inputType: 'password',
          span: 12,
          help: 'Secret Key da Alibaba Cloud, chave correspondente ao Access Key',
          placeholder: 'your-secret-key'
        },
        {
          name: 'apiKey',
          label: 'App Key',
          required: true,
          span: 12,
          help: 'App Key da interação de voz inteligente da Alibaba Cloud, obtenha em https://nls-portal.console.aliyun.com/applist',
          placeholder: 'your-app-key'
        }
      ],
      volcengine: [
        { 
          name: 'appId', 
          label: 'App Id', 
          required: true, 
          span: 12,
          help: 'Obtenha em https://console.volcengine.com/speech/app',
          placeholder: 'your-app-id'
        },
        { 
          name: 'apiKey', 
          label: 'Access Token', 
          required: true, 
          span: 12,
          help: 'Token de acesso do serviço de síntese de voz da Volcano Engine',
          placeholder: 'your-access-token'
        }
      ],
      xfyun: [
        { 
          name: 'appId', 
          label: 'App Id', 
          required: true, 
          span: 12,
          help: 'Obtenha o AppID da plataforma aberta iFlytek em https://console.xfyun.cn/',
          placeholder: 'your-app-id'
        },
        { 
          name: 'apiSecret', 
          label: 'Api Secret', 
          required: true, 
          span: 12,
          help: 'API Secret da plataforma aberta iFlytek',
          placeholder: 'your-api-secret'
        },
        { 
          name: 'apiKey', 
          label: 'Api Key', 
          required: true, 
          span: 12,
          help: 'API Key da plataforma aberta iFlytek',
          placeholder: 'your-api-key'
        }
      ],
      minimax: [
        { 
          name: 'appId', 
          label: 'Group Id', 
          required: true, 
          span: 12,
          help: 'Obtenha em https://platform.minimaxi.com/user-center/basic-information',
          placeholder: 'your-group-id'
        },
        { 
          name: 'apiKey', 
          label: 'API Key', 
          required: true, 
          span: 12,
          help: 'Obtenha em https://platform.minimaxi.com/user-center/basic-information/interface-key',
          placeholder: 'your-api-key'
        }
      ],
      'sherpa-onnx': [],
    }
  },
  oss: {
    label: 'config.oss',
    permissionPrefix: 'system:config',
    typeOptions: [
      { label: 'Armazenamento local', value: 'local', key: '0' },
      { label: 'Tencent Cloud COS', value: 'tencent', key: '1' },
      { label: 'Alibaba Cloud OSS', value: 'aliyun', key: '2' }
    ],
    typeFields: {
      local: [],
      tencent: [
        {
          name: 'apiKey',
          label: 'SecretId',
          required: true,
          inputType: 'password',
          span: 12,
          help: 'Obtenha em https://console.cloud.tencent.com/cam/capi',
          placeholder: 'your-secret-id'
        },
        {
          name: 'apiSecret',
          label: 'SecretKey',
          required: true,
          inputType: 'password',
          span: 12,
          help: 'Key da chave de API da Tencent Cloud',
          placeholder: 'your-secret-key'
        },
        {
          name: 'appId',
          label: 'Region',
          required: true,
          inputType: 'text',
          span: 12,
          help: 'Região onde o bucket está localizado',
          placeholder: 'ap-guangzhou'
        },
        {
          name: 'configName',
          label: 'Bucket',
          required: true,
          inputType: 'text',
          span: 12,
          help: 'Nome do bucket',
          placeholder: 'my-bucket-1250000000'
        },
        {
          name: 'apiUrl',
          label: 'Prefixo do caminho',
          required: false,
          inputType: 'text',
          span: 12,
          help: 'Prefixo do caminho no COS (opcional)',
          placeholder: 'uploads/'
        }
      ],
      aliyun: [
        {
          name: 'ak',
          label: 'AccessKey ID',
          required: true,
          inputType: 'password',
          span: 12,
          help: 'Obtenha em https://ram.console.aliyun.com/profile/access-keys',
          placeholder: 'your-access-key-id'
        },
        {
          name: 'sk',
          label: 'AccessKey Secret',
          required: true,
          inputType: 'password',
          span: 12,
          help: 'Chave correspondente ao AccessKey ID',
          placeholder: 'your-access-key-secret'
        },
        {
          name: 'apiUrl',
          label: 'Endpoint',
          required: true,
          inputType: 'text',
          span: 12,
          help: 'Domínio de acesso do OSS',
          placeholder: 'oss-cn-hangzhou.aliyuncs.com'
        },
        {
          name: 'configName',
          label: 'Bucket',
          required: true,
          inputType: 'text',
          span: 12,
          help: 'Nome do bucket',
          placeholder: 'my-bucket'
        }
      ]
    }
  }
};
