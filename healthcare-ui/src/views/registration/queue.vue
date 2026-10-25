<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-header__title">叫号大屏</h2>
        <p class="page-header__desc">
          展示候诊队列并执行叫号，每 30 秒自动刷新
          <span v-if="selfScope" class="page-header__scope">
            当前只显示挂在你名下的号
          </span>
        </p>
      </div>
      <div class="toolbar__right">
        <el-select
          v-model="doctorId"
          placeholder="选择医生（可全部）"
          clearable
          :disabled="selfScope"
          style="width: 200px"
          @change="loadQueue"
        >
          <el-option v-for="item in doctorOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="deptId" placeholder="全部科室" clearable style="width: 200px" @change="loadQueue">
          <el-option v-for="item in deptOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button :icon="Refresh" @click="loadQueue">刷新</el-button>
      </div>
    </div>

    <div class="queue-board">
      <div class="queue-current">
        <p class="queue-current__label">当前叫号</p>
        <p class="queue-current__no">{{ current?.queueNo ?? '--' }}</p>
        <p class="queue-current__name">{{ currentPatientName }}</p>
        <p class="queue-current__meta">
          {{ currentDeptName }} · {{ currentDoctorName }}
        </p>
        <p class="queue-current__time">{{ currentTime }}</p>
      </div>

      <div class="content-card queue-list">
        <div class="content-card__header">
          <h3 class="content-card__title">候诊队列</h3>
          <span class="text-muted">共 {{ queue.length }} 人等待</span>
        </div>
        <div class="content-card__body content-card__body--flush">
          <el-table v-loading="loading" :data="queue" size="large" :row-class-name="rowClass">
            <el-table-column prop="queueNo" label="排队号" width="110" align="center">
              <template #default="{ row }">
                <span class="queue-no">{{ row.queueNo ?? '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="患者" min-width="120">
              <template #default="{ row }">{{ patientName(row.patientId) }}</template>
            </el-table-column>
            <el-table-column label="号别" width="110" align="center">
              <template #default="{ row }">{{ dictLabel('reg_type', row.regType) || '-' }}</template>
            </el-table-column>
            <el-table-column label="医生" width="110">
              <template #default="{ row }">{{ doctorName(row.doctorId) || '-' }}</template>
            </el-table-column>
            <el-table-column prop="regTime" label="挂号时间" min-width="170" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'warning' : 'info'" size="small">
                  {{ row.status === 1 ? '就诊中' : '待就诊' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status !== 1"
                  v-perm="'registration:call'"
                  type="primary"
                  link
                  @click="call(row)"
                >
                  叫号
                </el-button>
                <template v-else>
                  <el-button v-perm="'registration:call'" type="warning" link @click="pass(row)">
                    过号
                  </el-button>
                  <el-button v-perm="'registration:call'" type="success" link @click="finish(row)">
                    完成
                  </el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!queue.length" class="empty-hint">当前范围内暂无候诊患者</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { callNumber, finishCall, pagePatients, passCall, registrationQueue } from '@/api/medical'
import { useUserStore } from '@/store/modules/user'
import { dictLabel, useDicts } from '@/utils/dict'
import { doctorName, loadDeptOptions, loadDoctorOptions, type LookupOption } from '@/utils/lookup'

const userStore = useUserStore()
/** 医生是"仅本人"范围：大屏上只给自己科室、自己名下的号，避免叫到别的医生的患者。 */
const selfScope = computed(() => userStore.isSelfScope)

const deptOptions = ref<LookupOption[]>([])
const doctorOptions = ref<LookupOption[]>([])
const patientOptions = ref<any[]>([])
const deptId = ref<number>()
const doctorId = ref<number>()
const queue = ref<any[]>([])
const loading = ref(false)
const current = ref<any>()
const currentTime = ref('')
let timer: number | undefined

const currentPatientName = computed(() => patientName(current.value?.patientId))
const currentDeptName = computed(() => {
  const hit = deptOptions.value.find((item) => String(item.value) === String(current.value?.deptId))
  return hit ? hit.label : ''
})
const currentDoctorName = computed(() => doctorName(current.value?.doctorId))

function patientName(id: number) {
  // 后端把 Long 序列化成字符串，这里一律按字符串比，别用 === 比 number
  const hit = patientOptions.value.find((item) => String(item.id) === String(id))
  return hit ? hit.name : '-'
}

function rowClass({ row }: any) {
  return current.value && row.id === current.value.id ? 'queue-row--called' : ''
}

async function loadQueue() {
  loading.value = true
  try {
    const rows: any[] = ((await registrationQueue(deptId.value, doctorId.value)) as any[]) || []
    queue.value = rows
    // 1 = 就诊中，即当前正在叫号的患者；没有就诊中的号就不要硬凑一个出来
    const called = rows.find((row) => row.status === 1)
    current.value = called
    if (called) {
      currentTime.value = new Date().toLocaleTimeString('zh-CN')
    }
  } finally {
    loading.value = false
  }
}

async function call(row: any) {
  await callNumber(row.id)
  ElMessage.success(`已叫号：${row.queueNo ?? '-'} 号 ${patientName(row.patientId)}`)
  current.value = row
  currentTime.value = new Date().toLocaleTimeString('zh-CN')
  loadQueue()
}

async function pass(row: any) {
  await ElMessageBox.confirm(
    `确认 ${row.queueNo ?? '-'} 号 ${patientName(row.patientId)} 过号吗？过号后该号退回候诊队列，可再次叫号。`,
    '过号处理',
    { type: 'warning' }
  )
  await passCall(row.id)
  ElMessage.success('已过号，号序已退回队列')
  loadQueue()
}

async function finish(row: any) {
  await ElMessageBox.confirm(
    `确认 ${row.queueNo ?? '-'} 号 ${patientName(row.patientId)} 完成就诊吗？完成后该号移出队列。`,
    '完成就诊',
    { type: 'warning' }
  )
  await finishCall(row.id)
  ElMessage.success('已完成就诊')
  current.value = undefined
  loadQueue()
}

onMounted(async () => {
  // 下拉任一失败都不能拦住队列：以前是串行 await，前置接口一报错后面就全跳过，
  // 页面表现成"大屏没有号"。患者姓名只影响显示，失败也不该拖垮叫号。
  await Promise.allSettled([
    loadDeptOptions().then((rows) => {
      deptOptions.value = rows
    }),
    loadDoctorOptions().then((rows) => {
      doctorOptions.value = rows
    }),
    pagePatients({ pageNum: 1, pageSize: 500 }).then((result: any) => {
      patientOptions.value = result?.list || []
    }),
    useDicts(['reg_type'])
  ])
  if (selfScope.value) {
    // 医生：锁自己的科室与自己的号；科室可能还没加载完，直接用自己的 deptId 也行
    deptId.value = userStore.ownDeptId
    doctorId.value = userStore.userInfo?.id
  }
  await loadQueue()
  timer = window.setInterval(loadQueue, 30000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped lang="scss">
.page-header__scope {
  margin-left: 8px;
  padding: 1px 8px;
  font-size: 12px;
  color: #0f8a7e;
  background: #e6f5f3;
  border-radius: 10px;
}

.queue-board {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 14px;
  align-items: start;
}

.queue-current {
  padding: 28px 20px;
  color: #eaf2f3;
  text-align: center;
  background: var(--hc-bg-dark);
  border-radius: var(--hc-radius);
  box-shadow: var(--hc-shadow);
}

.queue-current__label {
  margin: 0 0 14px;
  font-size: 15px;
  letter-spacing: 4px;
  color: #9fb4b8;
}

.queue-current__no {
  margin: 0;
  font-size: 76px;
  font-weight: 700;
  line-height: 1;
  color: var(--hc-primary);
}

.queue-current__name {
  margin: 18px 0 6px;
  font-size: 26px;
  font-weight: 600;
}

.queue-current__meta {
  margin: 0 0 18px;
  font-size: 14px;
  color: #9fb4b8;
}

.queue-current__time {
  margin: 0;
  font-size: 30px;
  color: #d98324;
}

.queue-list {
  margin-top: 0;
}

.queue-no {
  font-size: 18px;
  font-weight: 700;
  color: var(--hc-primary);
}

:deep(.queue-row--called) {
  background: var(--hc-primary-light);
}

@media (max-width: 960px) {
  .queue-board {
    grid-template-columns: 1fr;
  }
}
</style>
