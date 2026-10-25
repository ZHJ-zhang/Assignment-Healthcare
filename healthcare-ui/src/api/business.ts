import request from './request'
import type { PageResult } from './system'

/* 药品与库存 */
export const pageDrugs = (params: any) => request.get<any, PageResult<any>>('/drug/page', { params })
export const getDrug = (id: number) => request.get(`/drug/${id}`)
export const createDrug = (data: any) => request.post('/drug', data)
export const updateDrug = (id: number, data: any) => request.put(`/drug/${id}`, data)
export const deleteDrug = (id: number) => request.delete(`/drug/${id}`)
export const pageStock = (params: any) => request.get<any, PageResult<any>>('/drug/stock/page', { params })
export const stockIn = (data: any) => request.post('/drug/stock/in', data)
export const stockOut = (data: any) => request.post('/drug/stock/out', data)
export const stockWarnings = () => request.get('/drug/stock/warn')
export const stockExpiring = (warnDays = 90) =>
  request.get('/drug/stock/expire', { params: { warnDays } })
export const pageStockRecords = (params: any) =>
  request.get<any, PageResult<any>>('/drug/stock/record/page', { params })
export const listWarehouses = () => request.get('/drug/warehouse/list')
export const saveWarehouse = (data: any) => request.post('/drug/warehouse', data)
export const dispense = (data: any) => request.post('/pharmacy/dispense', data)
export const returnDrug = (data: any) => request.post('/pharmacy/return', data)

/* 检查检验 */
export const pageExamApplies = (params: any) =>
  request.get<any, PageResult<any>>('/exam/apply/page', { params })
export const createExamApply = (data: any) => request.post('/exam/apply', data)
export const collectSample = (id: number) => request.post('/exam/sample', null, { params: { id } })
export const confirmExamPay = (id: number) => request.put(`/exam/apply/${id}/pay`)
export const pageExamReports = (params: any) =>
  request.get<any, PageResult<any>>('/exam/report/page', { params })
export const getExamReport = (id: number) => request.get(`/exam/report/${id}`)
export const saveExamReport = (data: any) => request.post('/exam/report', data)
export const auditExamReport = (id: number) => request.put(`/exam/report/${id}/audit`)
export const publishExamReport = (id: number) => request.put(`/exam/report/${id}/publish`)
export const pageCriticalValues = (params: any) =>
  request.get<any, PageResult<any>>('/exam/critical/page', { params })
export const handleCriticalValue = (params: any) =>
  request.post('/exam/critical/handle', null, { params })

/* 收费财务 */
export const createCharge = (data: any) => request.post('/charge', data)
export const pageCharges = (params: any) => request.get<any, PageResult<any>>('/charge/page', { params })
export const chargeDetail = (id: number) => request.get(`/charge/${id}/detail`)
export const applyRefund = (id: number, params: any) =>
  request.post(`/charge/${id}/refund`, null, { params })
export const pageRefunds = (params: any) => request.get<any, PageResult<any>>('/charge/refund/page', { params })
export const auditRefund = (id: number, params: any) =>
  request.put(`/charge/refund/${id}/audit`, null, { params })
export const executeRefund = (id: number) => request.put(`/charge/refund/${id}/execute`)
export const printInvoice = (params: any) => request.post('/invoice/print', null, { params })
export const pageInvoices = (params: any) => request.get<any, PageResult<any>>('/invoice/page', { params })
export const voidInvoice = (id: number, reason?: string) =>
  request.put(`/invoice/${id}/void`, null, { params: { reason } })
export const financeDaily = (params: any) => request.get('/finance/daily', { params })
export const financeMonthly = (params: any) => request.get('/finance/monthly', { params })
export const insuranceSettle = (params: any) => request.post('/insurance/settle', null, { params })
export const pageFeeItems = (params: any) =>
  request.get<any, PageResult<any>>('/fee-item/page', { params })
export const listFeeItems = (params?: any) => request.get('/fee-item/list', { params })
export const createFeeItem = (data: any) => request.post('/fee-item', data)
export const updateFeeItem = (id: number, data: any) => request.put(`/fee-item/${id}`, data)

/* 统计报表 */
export const reportOutpatient = (params: any) => request.get('/report/outpatient', { params })
export const reportInpatient = (params: any) => request.get('/report/inpatient', { params })
export const reportIncome = (params: any) => request.get('/report/income', { params })
export const reportDrug = (params: any) => request.get('/report/drug', { params })
export const reportPatient = (params: any) => request.get('/report/patient', { params })
export const listCustomReports = () => request.get('/report/custom/list')
export const saveCustomReport = (data: any) => request.post('/report/custom', data)
export const executeCustomReport = (id: number, params: any) =>
  request.post(`/report/custom/${id}/execute`, null, { params })
export const reportExportUrl = (params: any) => {
  const query = new URLSearchParams(params).toString()
  return `${request.defaults.baseURL}/report/export?${query}`
}
/**
 * 导出报表文件：后端以二进制返回，异常时仍可能返回 JSON 错误体。
 */
export const exportReportFile = async (params: any) => {
  const blob: any = await request.get('/report/export', { params, responseType: 'blob' })
  return unwrapBlob(blob, '导出报表失败')
}

/* 消息通知 */
export const pageMessages = (params: any) =>
  request.get<any, PageResult<any>>('/message/page', { params })
export const unreadCount = () => request.get<any, number>('/message/unread')
export const readMessage = (id: number) => request.put(`/message/${id}/read`)
export const readAllMessages = () => request.put('/message/read-all')
export const pageNotices = (params: any) => request.get<any, PageResult<any>>('/notice/page', { params })
export const publishNotice = (data: any) => request.post('/notice', data)
export const deleteNotice = (id: number) => request.delete(`/notice/${id}`)
export const pageSmsRecords = (params: any) => request.get<any, PageResult<any>>('/sms/page', { params })
export const sendSms = (params: any) => request.post('/sms/send', null, { params })
export const warningList = (limit = 50) => request.get('/warning/list', { params: { limit } })

/* 数据安全 */
export const pageAuditOperationLogs = (params: any) =>
  request.get<any, PageResult<any>>('/audit/operation/page', { params })
export const pageAuditLoginLogs = (params: any) =>
  request.get<any, PageResult<any>>('/audit/login/page', { params })
export const exportLoginLogs = async (params: any) => {
  const blob: any = await request.get('/audit/login/export', { params, responseType: 'blob' })
  return unwrapBlob(blob, '导出登录日志失败')
}

/**
 * 导出接口返回 JSON 说明是业务错误，转为异常抛出。
 */
async function unwrapBlob(blob: any, fallback: string) {
  if (blob instanceof Blob && blob.type && blob.type.includes('application/json')) {
    const text = await blob.text()
    try {
      const payload = JSON.parse(text)
      throw new Error(payload.msg || fallback)
    } catch (error: any) {
      if (error instanceof Error && error.message !== 'Unexpected end of JSON input') {
        throw error
      }
      throw new Error(fallback)
    }
  }
  return blob
}
export const pageBackups = (params: any) => request.get<any, PageResult<any>>('/backup/page', { params })
export const createBackup = (data: any) => request.post('/backup', data)
export const restoreBackup = (id: number) => request.post(`/backup/${id}/restore`)
export const deleteBackup = (id: number) => request.delete(`/backup/${id}`)
export const getSecurityConfig = () => request.get('/security/config')
export const saveSecurityConfig = (data: any) => request.put('/security/config', data)

/* 首页看板 */
export const dashboardSummary = () => request.get('/dashboard/summary')
export const dashboardTrend = () => request.get('/dashboard/trend')
