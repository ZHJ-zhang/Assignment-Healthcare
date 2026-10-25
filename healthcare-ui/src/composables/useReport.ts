import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { exportReportFile } from '@/api/business'
import { saveBlob } from '@/utils/download'

/**
 * 统计报表通用逻辑：日期区间查询、重置与导出。
 *
 * @param loader 报表查询函数
 * @param type 报表类型，对应后端 /report/export 的 type 参数
 * @param label 报表名称，用于导出文件名
 */
export function useReport(
  loader: (params: any) => Promise<any>,
  type: string,
  label: string
) {
  const report = reactive<any>({})
  const range = ref<string[]>([])
  const query = reactive<any>({ startDate: '', endDate: '' })

  async function load() {
    query.startDate = range.value?.[0] || ''
    query.endDate = range.value?.[1] || ''
    const result: any = await loader(query)
    Object.assign(report, result || {})
    return report
  }

  function reset() {
    range.value = []
    return load()
  }

  async function exportReport(format: string) {
    try {
      const blob: any = await exportReportFile({
        type,
        startDate: query.startDate,
        endDate: query.endDate,
        format
      })
      saveBlob(blob, `${label}-${Date.now()}.${format === 'pdf' ? 'pdf' : 'xlsx'}`)
    } catch (error: any) {
      ElMessage.error(error?.message || '导出失败')
    }
  }

  return { report, range, query, load, reset, exportReport }
}
