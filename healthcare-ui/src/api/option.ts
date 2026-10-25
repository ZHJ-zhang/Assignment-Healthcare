import request from './request'

/**
 * 业务下拉选项（登录即可访问的轻量只读接口）。
 * 与 /system/*、/drug/page 这类管理接口分开，一线业务角色也能用。
 */
export const optionDepts = () => request.get<any, any[]>('/common/options/depts')

export const optionDoctors = () => request.get<any, any[]>('/common/options/doctors')

export const optionDrugs = (keyword?: string) =>
  request.get<any, any[]>('/common/options/drugs', { params: { keyword } })

export const optionPatients = (keyword?: string) =>
  request.get<any, any[]>('/common/options/patients', { params: { keyword } })
