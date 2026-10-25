import { reactive } from 'vue'
import { dictData } from '@/api/system'

export interface DictItem {
  id: number
  dictType: string
  dictLabel: string
  dictValue: string
  sort: number
  cssClass: string | null
  isDefault: number
  status: number
  remark: string | null
}

/**
 * 字典缓存：同一字典类型只向后端请求一次。
 */
const cache = reactive<Record<string, DictItem[]>>({})
const pending = new Map<string, Promise<DictItem[]>>()

/**
 * 加载字典数据，返回该字典下的字典项。
 */
export async function loadDict(dictType: string): Promise<DictItem[]> {
  if (cache[dictType]) return cache[dictType]
  const exists = pending.get(dictType)
  if (exists) return exists

  const request = dictData(dictType)
    .then((items: any) => {
      const list = (items || []) as DictItem[]
      cache[dictType] = list
      pending.delete(dictType)
      return list
    })
    .catch((error) => {
      pending.delete(dictType)
      throw error
    })
  pending.set(dictType, request)
  return request
}

/**
 * 读取已缓存的字典数据，不触发请求。
 */
export function getDict(dictType: string): DictItem[] {
  return cache[dictType] || []
}

/**
 * 根据字典值取显示文本，未命中时回退为原值。
 */
export function dictLabel(dictType: string, value: unknown): string {
  if (value === null || value === undefined || value === '') return ''
  const hit = getDict(dictType).find((item) => item.dictValue === String(value))
  return hit ? hit.dictLabel : String(value)
}

/**
 * 清空字典缓存，供退出登录时调用。
 */
export function clearDictCache() {
  Object.keys(cache).forEach((key) => delete cache[key])
  pending.clear()
}

/**
 * 在组件中加载若干字典，返回字典项数组的映射表。
 */
export async function useDicts(dictTypes: string[]) {
  await Promise.all(dictTypes.map((type) => loadDict(type).catch(() => [])))
  const map: Record<string, DictItem[]> = {}
  dictTypes.forEach((type) => {
    map[type] = getDict(type)
  })
  return map
}
