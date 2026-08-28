<template>
  <div class="template-container">
    <!-- Área de busca -->
    <a-card class="search-card">
      <a-form layout="inline">
        <a-form-item :label="t('template.templateName')">
          <a-input
            v-model:value="searchForm.templateName"
            :placeholder="t('template.enterTemplateName')"
            allow-clear
            style="width: 200px"
            @pressEnter="handleSearch"
          />
        </a-form-item>
        <a-form-item :label="t('template.category')">
          <a-select
            v-model:value="searchForm.category"
            :placeholder="t('common.all')"
            style="width: 150px"
            @change="handleSearch"
          >
            <a-select-option
              v-for="item in categoryOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- Área da tabela -->
    <a-card :title="t('template.templateList')" :bordered="false" style="margin-top: 16px">
      <template #extra>
        <a-button v-permission="'system:prompt-template:create'" type="primary" @click="handleCreate">
          <template #icon><PlusOutlined /></template>
          {{ t('template.createTemplate') }}
        </a-button>
      </template>

      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 800 }"
        row-key="templateId"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'templateContent'">
            <a-tooltip :title="record.templateContent" placement="leftTop">
              <span>{{ record.templateContent }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.key === 'isDefault'">
            <a-tag v-if="record.isDefault == 1" color="green">{{ t('common.default') }}</a-tag>
            <span v-else>-</span>
          </template>

          <template v-else-if="column.key === 'operation'">
            <TableActionButtons
              :record="record"
              permission-prefix="system:prompt-template"
              show-edit
              show-view
              show-set-default
              show-delete
              :is-default="record.isDefault == 1"
              :delete-title="t('template.confirmDelete')"
              @edit="handleEdit"
              @view="handlePreview"
              @set-default="handleSetDefault"
              @delete="handleDelete"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Voltar ao topo -->
    <a-back-top />

    <!-- Diálogo de criação/edição -->
    <a-modal
      v-model:open="modal.visible.value"
      :title="modal.isEdit.value ? t('template.editTemplate') : t('template.createTemplate')"
      :confirm-loading="modal.submitLoading.value"
      :mask-closable="false"
      width="800px"
      @cancel="modal.close"
    >
      <template #footer>
        <a-button @click="modal.close">{{ t('common.cancel') }}</a-button>
        <a-button
          v-permission="modal.isEdit.value ? 'system:prompt-template:update' : 'system:prompt-template:create'"
          type="primary"
          :loading="modal.submitLoading.value"
          @click="handleSubmit"
        >
          {{ t('common.confirm') }}
        </a-button>
      </template>

      <a-form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
      >
        <a-form-item
          :label="t('template.templateName')"
          name="templateName"
          :rules="[{ required: true, message: t('template.enterTemplateName') }]"
        >
          <a-input v-model:value="formData.templateName" :placeholder="t('template.enterTemplateName')" />
        </a-form-item>

        <a-form-item
          :label="t('template.templateCategory')"
          name="category"
          :rules="[{ required: true, message: t('template.selectCategory') }]"
        >
          <a-select
            v-model:value="formData.category"
            :placeholder="t('template.selectCategory')"
            @change="handleCategoryChange"
          >
            <a-select-option
              v-for="category in categoryOptions"
              :key="category.value"
              :value="category.value"
            >
              {{ category.label }}
            </a-select-option>
            <a-select-option value="custom">{{ t('template.customCategory') }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          v-if="showCustomCategory"
          :label="t('template.customCategory')"
          name="customCategory"
          :rules="[{ required: true, message: t('template.enterCustomCategory') }]"
        >
          <a-input v-model:value="formData.customCategory" :placeholder="t('template.enterCustomCategory')" />
        </a-form-item>

        <a-form-item :label="t('template.templateDesc')" name="templateDesc">
          <a-input
            v-model:value="formData.templateDesc"
            :placeholder="t('template.enterTemplateDesc')"
          />
        </a-form-item>

        <a-form-item v-permission="'system:prompt-template:update'" :label="t('common.isDefault')" name="isDefault">
          <a-switch v-model:checked="formData.isDefault" />
          <span style="margin-left: 8px; color: var(--ant-color-text-tertiary)">{{ t('template.defaultTip') }}</span>
        </a-form-item>

        <a-form-item
          :label="t('template.templateContent')"
          name="templateContent"
          :rules="[{ required: true, message: t('template.enterTemplateContent') }]"
        >
          <a-textarea
            v-model:value="formData.templateContent"
            :rows="12"
            :placeholder="t('template.templateContentPlaceholder')"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Diálogo de pré-visualização -->
    <a-modal
      v-model:open="previewVisible"
      :title="t('template.templatePreview')"
      :footer="null"
      width="800px"
    >
      <a-typography v-if="previewTemplate">
        <a-typography-title :level="3">
          {{ previewTemplate.templateName }}
        </a-typography-title>
        
        <a-typography-paragraph v-if="previewTemplate.templateDesc" type="secondary">
          <template #icon><InfoCircleOutlined /></template>
          {{ previewTemplate.templateDesc }}
        </a-typography-paragraph>
        
        <a-typography-paragraph>
          <a-space>
            <a-tag color="blue">{{ previewTemplate.category }}</a-tag>
            <a-tag v-if="previewTemplate.isDefault == 1" color="green">{{ t('template.defaultTemplate') }}</a-tag>
          </a-space>
        </a-typography-paragraph>
        
        <a-divider />
        
        <a-typography-title :level="5">{{ t('template.templateContent') }}</a-typography-title>
        <a-typography-paragraph>
          <blockquote class="template-preview-content">
            {{ previewTemplate.templateContent }}
          </blockquote>
        </a-typography-paragraph>
        
        <a-typography-paragraph v-if="previewTemplate.createTime" type="secondary" style="margin-top: 16px">
          <small>{{ t('common.createTime') }}：{{ previewTemplate.createTime }}</small>
        </a-typography-paragraph>
      </a-typography>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import type { FormInstance, TableProps } from 'ant-design-vue'
import type { PromptTemplate, TemplateFormData, CategoryOption } from '@/types/template'
import {
  queryTemplates,
  addTemplate,
  updateTemplate,
  deleteTemplate,
  setDefaultTemplate
} from '@/services/template'
import { useTable } from '@/composables/useTable'
import { useModal } from '@/composables/useModal'
import TableActionButtons from '@/components/TableActionButtons.vue'

const { t } = useI18n()

const columns = computed(() => [
  {
    title: t('template.templateName'),
    dataIndex: 'templateName',
    width: 100,
    align: 'center'
  },
  {
    title: t('template.category'),
    dataIndex: 'category',
    width: 120,
    align: 'center'
  },
  {
    title: t('template.templateContent'),
    dataIndex: 'templateContent',
    key: 'templateContent',
    width: 200,
    align: 'center'
  },
  {
    title: t('common.isDefault'),
    dataIndex: 'isDefault',
    key: 'isDefault',
    width: 80,
    align: 'center'
  },
  {
    title: t('common.createTime'),
    dataIndex: 'createTime',
    width: 180,
    align: 'center'
  },
  {
    title: t('table.action'),
    key: 'operation',
    width: 220,
    fixed: 'right',
    align: 'center'
  }
])

// Formulário de busca
const searchForm = reactive({
  templateName: '',
  category: ''
})

// Opções de categoria padrão
const defaultCategoryOptions = computed<CategoryOption[]>(() => [
  { label: t('common.all'), value: '' },
  { label: t('template.categoryBasic'), value: 'Persona Basica' },
  { label: t('template.categoryProfessional'), value: 'Persona Profissional' },
  { label: t('template.categorySocial'), value: 'Persona Social' },
  { label: t('template.categoryEntertainment'), value: 'Persona de Entretenimento' }
])

// Opções de categoria (inclui categorias personalizadas carregadas dinamicamente)
const categoryOptions = ref<CategoryOption[]>([...defaultCategoryOptions.value])

// Usa a função composable de tabela
const {
  data: dataSource,
  loading,
  pagination,
  handleTableChange: onTableChange,
  loadData
} = useTable<PromptTemplate>()

const formRef = ref<FormInstance>()
const modal = useModal({
  formRef,
  onSubmit: async (data, isEdit) => {
    // O Modal já possui submitLoading, não precisa de loading global
    try {
      const category = formData.category === 'custom' && formData.customCategory 
        ? formData.customCategory 
        : formData.category
      
      const requestData: Partial<PromptTemplate> = {
        templateName: formData.templateName,
        templateDesc: formData.templateDesc || '',
        category: category,
        templateContent: formData.templateContent,
        isDefault: formData.isDefault ? 1 : 0,
        state: 1
      }
      
      if (isEdit && modal.editingItem.value) {
        requestData.templateId = (modal.editingItem.value as PromptTemplate).templateId
      }
      
      const res = isEdit
        ? await updateTemplate(requestData)
        : await addTemplate(requestData)
      
      if (res.code === 200) {
        message.success(isEdit ? t('template.updateSuccess') : t('template.createSuccess'))
        await fetchData()
        return true
      } else {
        message.error(res.message || t('template.operationFailed'))
        return false
      }
    } catch (error) {
      message.error(t('template.operationFailed'))
      return false
    }
  },
  onOpen: (item) => {
    if (item) {
      const template = item as PromptTemplate
      const isCustomCategory = !categoryOptions.value.some(c => c.value === template.category)
      showCustomCategory.value = isCustomCategory
      
      Object.assign(formData, {
        templateName: template.templateName,
        category: isCustomCategory ? 'custom' : template.category,
        customCategory: isCustomCategory ? template.category : '',
        templateDesc: template.templateDesc || '',
        templateContent: template.templateContent,
        isDefault: template.isDefault == 1
      })
    } else {
      showCustomCategory.value = false
      resetForm()
      formData.category = 'Persona Basica'
      formData.isDefault = false
    }
  }
})

const showCustomCategory = ref(false)

// Dados do formulário
const formData = reactive<TemplateFormData>({
  templateName: '',
  category: '',
  customCategory: '',
  templateDesc: '',
  templateContent: '',
  isDefault: false
})

// Relacionado à pré-visualização
const previewVisible = ref(false)
const previewTemplate = ref<PromptTemplate | null>(null)

// Busca
const handleSearch = () => {
  fetchData()
}

// Carrega dados
const fetchData = async () => {
  await loadData(async ({ pageNo, pageSize }) => {
    const res = await queryTemplates({
      ...searchForm,
      pageNo,
      pageSize
    })
    
    // Atualiza as opções de categoria
    if (res.data?.list) {
      const categories = new Set<string>()
      res.data.list.forEach((item: PromptTemplate) => {
        if (item.category) {
          categories.add(item.category)
        }
      })
      
      const defaultValues = defaultCategoryOptions.value.map(c => c.value)
      const customCategories = [...categories].filter(c => !defaultValues.includes(c) && c !== '')
      
      if (customCategories.length > 0) {
        categoryOptions.value = [
          ...defaultCategoryOptions.value,
          ...customCategories.map(c => ({ label: c, value: c }))
        ]
      } else {
        categoryOptions.value = [...defaultCategoryOptions.value]
      }
    }
    
    return res
  })
}

// Criar
const handleCreate = () => {
  modal.openCreate()
}

const handleEdit = (record: PromptTemplate) => {
  modal.openEdit(record)
}

// Excluir (ação rápida, usa apenas o loading da tabela)
const handleDelete = async (record: PromptTemplate) => {
  if (!record.templateId) return
  
  loading.value = true
  try {
    const res = await deleteTemplate(record.templateId!)
    if (res.code === 200) {
      message.success(t('template.deleteSuccess'))
      await fetchData()
    } else {
      message.error(res.message || t('template.deleteFailed'))
    }
  } catch (error) {
    message.error(t('template.deleteFailed'))
  } finally {
    loading.value = false
  }
}

// Definir como padrão (ação rápida, usa apenas o loading da tabela)
const handleSetDefault = async (record: PromptTemplate) => {
  if (!record.templateId) return
  
  loading.value = true
  try {
    const res = await setDefaultTemplate(record.templateId)
    if (res.code === 200) {
      message.success(t('template.setDefaultSuccess'))
      await fetchData()
    } else {
      message.error(res.message || t('template.operationFailed'))
    }
  } catch (error) {
    message.error(t('template.operationFailed'))
  } finally {
    loading.value = false
  }
}

// Pré-visualizar
const handlePreview = (record: PromptTemplate) => {
  previewTemplate.value = record
  previewVisible.value = true
}

// Trata a mudança de categoria
const handleCategoryChange = (value: string) => {
  showCustomCategory.value = value === 'custom'
  if (value !== 'custom') {
    formData.customCategory = ''
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    await modal.submit(formData)
  } catch (error) {
    console.error('Falha na validação do formulário:', error)
  }
}

// Reseta o formulário
const resetForm = () => {
  formRef.value?.resetFields()
  formData.templateName = ''
  formData.category = ''
  formData.customCategory = ''
  formData.templateDesc = ''
  formData.templateContent = ''
  formData.isDefault = false
  showCustomCategory.value = false
}

// Trata a mudança da tabela
const handleTableChange: TableProps['onChange'] = (pag) => {
  onTableChange(pag)
}

// Inicialização (carregamento não bloqueante)
fetchData()
</script>

<style scoped>
.template-container {
  padding: 16px;
}

.search-card :deep(.ant-form-item) {
  margin-bottom: 0;
}

.template-preview-content {
  white-space: pre-wrap;
  background: var(--ant-color-fill-tertiary);
  padding: 16px 20px;
  border-left: 4px solid var(--ant-color-primary);
  border-radius: 4px;
  max-height: 500px;
  overflow-y: auto;
  color: var(--ant-color-text);
  line-height: 1.8;
  margin: 0;
}

blockquote.template-preview-content {
  margin: 8px 0;
}
</style>
