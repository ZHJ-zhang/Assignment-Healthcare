import request from './request'

export interface PageResult<T> {
  total: number
  pageNum: number
  pageSize: number
  list: T[]
}

export interface PageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  startTime?: string
  endTime?: string
}

/* 用户 */
export const pageUsers = (params: any) => request.get<any, PageResult<any>>('/system/user/page', { params })
export const getUser = (id: number) => request.get(`/system/user/${id}`)
export const createUser = (data: any) => request.post('/system/user', data)
export const updateUser = (id: number, data: any) => request.put(`/system/user/${id}`, data)
export const deleteUser = (id: number) => request.delete(`/system/user/${id}`)
export const updateUserStatus = (id: number, status: number) =>
  request.put(`/system/user/${id}/status`, null, { params: { status } })
export const resetUserPassword = (id: number, password: string) =>
  request.put(`/system/user/${id}/password`, null, { params: { password } })
export const changePassword = (data: any) => request.put('/system/user/password', data)

/* 角色 */
export const listRoles = () => request.get('/system/role/list')
export const pageRoles = (params: any) => request.get('/system/role/page', { params })
export const getRole = (id: number) => request.get(`/system/role/${id}`)
export const createRole = (data: any) => request.post('/system/role', data)
export const updateRole = (id: number, data: any) => request.put(`/system/role/${id}`, data)
export const deleteRole = (id: number) => request.delete(`/system/role/${id}`)
export const assignRolePermissions = (id: number, ids: number[]) =>
  request.put(`/system/role/${id}/permission`, { ids })

/* 科室 */
export const listDepts = () => request.get('/system/dept/list')
export const treeDepts = () => request.get('/system/dept/tree')
export const getDept = (id: number) => request.get(`/system/dept/${id}`)
export const createDept = (data: any) => request.post('/system/dept', data)
export const updateDept = (id: number, data: any) => request.put(`/system/dept/${id}`, data)
export const deleteDept = (id: number) => request.delete(`/system/dept/${id}`)

/* 菜单 */
export const treeMenus = () => request.get('/system/menu/tree')
export const getMenu = (id: number) => request.get(`/system/menu/${id}`)
export const createMenu = (data: any) => request.post('/system/menu', data)
export const updateMenu = (id: number, data: any) => request.put(`/system/menu/${id}`, data)
export const deleteMenu = (id: number) => request.delete(`/system/menu/${id}`)

/* 字典 */
export const listDictTypes = () => request.get('/system/dict/type/list')
export const listDictItems = (dictType: string) =>
  request.get('/system/dict/item/list', { params: { dictType } })
export const dictData = (dictType: string) => request.get(`/system/dict/data/${dictType}`)
export const saveDictType = (data: any) => request.post('/system/dict/type', data)
export const updateDictType = (id: number, data: any) => request.put(`/system/dict/type/${id}`, data)
export const deleteDictType = (id: number) => request.delete(`/system/dict/type/${id}`)
export const saveDictItem = (data: any) => request.post('/system/dict/item', data)
export const updateDictItem = (id: number, data: any) => request.put(`/system/dict/item/${id}`, data)
export const deleteDictItem = (id: number) => request.delete(`/system/dict/item/${id}`)

/* 参数 */
export const pageConfigs = (params: any) => request.get('/system/config/page', { params })
export const listConfigs = () => request.get('/system/config/list')
export const saveConfig = (data: any) => request.post('/system/config', data)
export const updateConfig = (id: number, data: any) => request.put(`/system/config/${id}`, data)
export const deleteConfig = (id: number) => request.delete(`/system/config/${id}`)

/* 日志 */
export const pageOperationLogs = (params: any) =>
  request.get('/system/log/operation/page', { params })
export const pageLoginLogs = (params: any) => request.get('/system/log/login/page', { params })
