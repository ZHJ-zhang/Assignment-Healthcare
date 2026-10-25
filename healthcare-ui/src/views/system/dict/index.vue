<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">字典管理</h2>
        <p class="page-header__desc">维护系统下拉选项与业务枚举值</p>
      </div>
    </div>

    <div class="dict-layout">
      <div class="content-card dict-layout__aside">
        <div class="content-card__header">
          <h3 class="content-card__title">字典类型</h3>
          <el-button v-perm="'system:dict:add'" type="primary" link :icon="Plus" @click="openType()">
            新增
          </el-button>
        </div>
        <div class="dict-type-list">
          <div
            v-for="item in types"
            :key="item.id"
            class="dict-type-item"
            :class="{ 'dict-type-item--active': currentType && currentType.id === item.id }"
            @click="selectType(item)"
          >
            <div class="dict-type-item__main">
              <span class="dict-type-item__name">{{ item.dictName }}</span>
              <span class="dict-type-item__code">{{ item.dictType }}</span>
            </div>
            <el-dropdown trigger="click" @command="(cmd: string) => onTypeCommand(cmd, item)">
              <el-button link :icon="MoreFilled" @click.stop />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">修改</el-dropdown-item>
                  <el-dropdown-item command="remove" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div v-if="!types.length" class="empty-hint">暂无字典类型</div>
        </div>
      </div>

      <div class="content-card dict-layout__main">
        <div class="content-card__header">
          <h3 class="content-card__title">
            字典项
            <span v-if="currentType" class="dict-current">（{{ currentType.dictName }}）</span>
          </h3>
          <el-button
            v-perm="'system:dict:add'"
            type="primary"
            :icon="Plus"
            :disabled="!currentType"
            @click="openItem()"
          >
            新增字典项
          </el-button>
        </div>

        <div class="table-wrap">
          <el-table v-loading="loading" :data="items" border>
            <el-table-column prop="dictLabel" label="字典标签" min-width="140" />
            <el-table-column prop="dictValue" label="字典键值" min-width="140" />
            <el-table-column prop="sort" label="排序" width="80" align="center" />
            <el-table-column label="默认" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isDefault === 1" type="success" size="small">默认</el-tag>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '正常' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button v-perm="'system:dict:edit'" link type="primary" @click="openItem(row)">
                  修改
                </el-button>
                <el-button v-perm="'system:dict:remove'" link type="danger" @click="removeItem(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <el-dialog v-model="typeVisible" :title="typeForm.id ? '修改字典类型' : '新增字典类型'" width="520px">
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="92px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="如 性别" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="typeForm.dictType" :disabled="!!typeForm.id" placeholder="如 sys_gender" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitType">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemVisible" :title="itemForm.id ? '修改字典项' : '新增字典项'" width="520px">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="92px">
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="itemForm.dictLabel" placeholder="显示文本" />
        </el-form-item>
        <el-form-item label="字典键值" prop="dictValue">
          <el-input v-model="itemForm.dictValue" placeholder="存储值" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="是否默认">
          <el-radio-group v-model="itemForm.isDefault">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="itemForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitItem">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { MoreFilled, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  deleteDictItem,
  deleteDictType,
  listDictItems,
  listDictTypes,
  saveDictItem,
  saveDictType,
  updateDictItem,
  updateDictType
} from '@/api/system'

const types = ref<any[]>([])
const items = ref<any[]>([])
const currentType = ref<any>()
const loading = ref(false)
const saving = ref(false)

const typeVisible = ref(false)
const itemVisible = ref(false)
const typeFormRef = ref<FormInstance>()
const itemFormRef = ref<FormInstance>()

const typeForm = reactive<any>({
  id: undefined,
  dictName: '',
  dictType: '',
  status: 1,
  remark: ''
})

const itemForm = reactive<any>({
  id: undefined,
  dictType: '',
  dictLabel: '',
  dictValue: '',
  sort: 0,
  isDefault: 0,
  status: 1,
  remark: ''
})

const typeRules: FormRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }]
}

const itemRules: FormRules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }]
}

async function loadTypes() {
  types.value = ((await listDictTypes()) as any[]) || []
  if (!currentType.value && types.value.length) {
    await selectType(types.value[0])
  }
}

async function selectType(type: any) {
  currentType.value = type
  loading.value = true
  try {
    items.value = ((await listDictItems(type.dictType)) as any[]) || []
  } finally {
    loading.value = false
  }
}

function openType() {
  Object.assign(typeForm, { id: undefined, dictName: '', dictType: '', status: 1, remark: '' })
  typeVisible.value = true
  typeFormRef.value?.clearValidate()
}

async function onTypeCommand(command: string, item: any) {
  if (command === 'edit') {
    Object.assign(typeForm, item)
    typeVisible.value = true
    typeFormRef.value?.clearValidate()
    return
  }
  await ElMessageBox.confirm(`确定删除字典类型「${item.dictName}」吗？`, '提示', { type: 'warning' })
  await deleteDictType(item.id)
  ElMessage.success('删除成功')
  if (currentType.value?.id === item.id) {
    currentType.value = undefined
    items.value = []
  }
  loadTypes()
}

async function submitType() {
  const valid = await typeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (typeForm.id) {
      await updateDictType(typeForm.id, { ...typeForm })
    } else {
      await saveDictType({ ...typeForm })
    }
    ElMessage.success('保存成功')
    typeVisible.value = false
    loadTypes()
  } finally {
    saving.value = false
  }
}

function openItem(row?: any) {
  if (row) {
    Object.assign(itemForm, row)
  } else {
    Object.assign(itemForm, {
      id: undefined,
      dictType: currentType.value?.dictType || '',
      dictLabel: '',
      dictValue: '',
      sort: 0,
      isDefault: 0,
      status: 1,
      remark: ''
    })
  }
  itemVisible.value = true
  itemFormRef.value?.clearValidate()
}

async function submitItem() {
  const valid = await itemFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (itemForm.id) {
      await updateDictItem(itemForm.id, { ...itemForm })
    } else {
      await saveDictItem({ ...itemForm })
    }
    ElMessage.success('保存成功')
    itemVisible.value = false
    if (currentType.value) selectType(currentType.value)
  } finally {
    saving.value = false
  }
}

async function removeItem(row: any) {
  await ElMessageBox.confirm(`确定删除字典项「${row.dictLabel}」吗？`, '提示', { type: 'warning' })
  await deleteDictItem(row.id)
  ElMessage.success('删除成功')
  if (currentType.value) selectType(currentType.value)
}

onMounted(loadTypes)
</script>

<style scoped lang="scss">
.dict-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 14px;
  align-items: start;
}

.dict-layout__aside,
.dict-layout__main {
  margin-top: 0;
}

.dict-type-list {
  max-height: 620px;
  overflow-y: auto;
  padding: 6px;
}

.dict-type-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 9px 10px;
  border-radius: var(--hc-radius-sm);
  cursor: pointer;
}

.dict-type-item:hover {
  background: var(--hc-bg);
}

.dict-type-item--active {
  background: var(--hc-primary-light);
}

.dict-type-item__main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.dict-type-item__name {
  font-size: 14px;
  color: var(--hc-text);
}

.dict-type-item--active .dict-type-item__name {
  color: var(--hc-primary-dark);
  font-weight: 600;
}

.dict-type-item__code {
  font-size: 12px;
  color: var(--hc-text-muted);
}

.dict-current {
  font-size: 13px;
  font-weight: 400;
  color: var(--hc-text-secondary);
}

@media (max-width: 1000px) {
  .dict-layout {
    grid-template-columns: 1fr;
  }
}
</style>
