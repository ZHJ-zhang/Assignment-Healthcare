import { reactive, ref } from 'vue'

/**
 * 分页列表通用逻辑：查询、分页、重置。
 *
 * @param loader 分页数据加载函数，接收查询参数
 * @param defaults 查询参数默认值
 */
export function usePage<T = any>(
  loader: (params: any) => Promise<any>,
  defaults: Record<string, any> = {}
) {
  const list = ref<T[]>([]) as any
  const total = ref(0)
  const loading = ref(false)
  const query = reactive<Record<string, any>>({
    pageNum: 1,
    pageSize: 10,
    ...defaults
  })

  async function load() {
    loading.value = true
    try {
      const result = await loader(query)
      const data = result || {}
      list.value = data.list || []
      total.value = Number(data.total || 0)
    } catch (error) {
      list.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  /**
 * 查询：回到第一页后重新加载。
 */
  function search() {
    query.pageNum = 1
    return load()
  }

  /**
 * 重置：清空除分页外的查询条件并重新加载。
 */
  function reset() {
    Object.keys(query).forEach((key) => {
      if (key === 'pageNum' || key === 'pageSize') return
      query[key] = defaults[key] ?? ''
    })
    query.pageNum = 1
    return load()
  }

  return { list, total, loading, query, load, search, reset }
}
