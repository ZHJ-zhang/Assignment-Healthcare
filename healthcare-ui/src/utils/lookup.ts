import { reactive } from 'vue'
import { listRoles } from '@/api/system'
import { optionDepts, optionDoctors, optionPatients } from '@/api/option'

export interface LookupOption {
  value: string | number
  label: string
  extra?: any
}

const deptCache = reactive<{ list: LookupOption[]; loaded: boolean }>({
  list: [],
  loaded: false
})

const doctorCache = reactive<{ list: LookupOption[]; loaded: boolean }>({
  list: [],
  loaded: false
})

const roleCache = reactive<{ list: LookupOption[]; loaded: boolean }>({
  list: [],
  loaded: false
})

const patientCache = reactive<{ list: LookupOption[]; loaded: boolean }>({
  list: [],
  loaded: false
})

/**
 * 加载科室选项。
 * 走 /common/options/depts 而不是 /system/dept/list —— 后者要管理员权限，医生和收费员会 403。
 */
export async function loadDeptOptions(force = false): Promise<LookupOption[]> {
  if (deptCache.loaded && !force) return deptCache.list
  const rows: any[] = (await optionDepts()) || []
  deptCache.list = rows.map((item) => ({ value: item.id, label: item.deptName }))
  deptCache.loaded = true
  return deptCache.list
}

/**
 * 加载医生（具备医生角色的用户）选项。
 */
export async function loadDoctorOptions(force = false): Promise<LookupOption[]> {
  if (doctorCache.loaded && !force) return doctorCache.list
  const rows: any[] = (await optionDoctors()) || []
  doctorCache.list = rows.map((item) => ({
    value: item.id,
    label: item.realName || String(item.id),
    extra: item.deptId
  }))
  doctorCache.loaded = true
  return doctorCache.list
}

/**
 * 加载角色选项。
 */
export async function loadRoleOptions(force = false): Promise<LookupOption[]> {
  if (roleCache.loaded && !force) return roleCache.list
  const rows: any[] = (await listRoles()) || []
  roleCache.list = rows.map((item) => ({ value: item.id, label: item.roleName }))
  roleCache.loaded = true
  return roleCache.list
}

/**
 * 根据 id 取出科室名称。
 */
export function deptName(id: unknown): string {
  if (id === null || id === undefined || id === '') return ''
  const hit = deptCache.list.find((item) => String(item.value) === String(id))
  return hit ? hit.label : ''
}

/**
 * 根据 id 取出医生姓名。
 */
export function doctorName(id: unknown): string {
  if (id === null || id === undefined || id === '') return ''
  const hit = doctorCache.list.find((item) => String(item.value) === String(id))
  return hit ? hit.label : ''
}

/**
 * 加载患者选项。
 */
export async function loadPatientOptions(force = false): Promise<LookupOption[]> {
  if (patientCache.loaded && !force) return patientCache.list
  const rows: any[] = (await optionPatients()) || []
  patientCache.list = rows.map((item) => ({
    value: item.id,
    // 患者档案姓名字段是 name（不是 patientName），带上门诊号更易辨认
    label: item.name ? (item.patientNo ? `${item.name}（${item.patientNo}）` : item.name)
      : (item.patientNo || String(item.id)),
    extra: item
  }))
  patientCache.loaded = true
  return patientCache.list
}

/**
 * 根据 id 取出患者姓名。
 */
export function patientName(id: unknown): string {
  if (id === null || id === undefined || id === '') return ''
  const hit = patientCache.list.find((item) => String(item.value) === String(id))
  return hit ? hit.label : ''
}

/**
 * 清空缓存，退出登录时调用。
 */
export function clearLookupCache() {
  deptCache.list = []
  deptCache.loaded = false
  doctorCache.list = []
  doctorCache.loaded = false
  roleCache.list = []
  roleCache.loaded = false
  patientCache.list = []
  patientCache.loaded = false
}
