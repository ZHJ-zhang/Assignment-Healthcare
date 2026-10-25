import request from './request'

export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
  roles: string[]
  userInfo: UserInfo
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  avatar: string
  gender: number
  deptId: number
  deptName: string
  /** 数据范围：1 全部、2 本部门及以下、3 本部门、4 仅本人 */
  dataScope?: number
  roles: string[]
  permissions: string[]
}

export interface RouterMeta {
  title: string
  icon: string
  hidden: boolean
  keepAlive: boolean
}

export interface RouterItem {
  name: string
  path: string
  component: string
  redirect: string
  meta: RouterMeta
  children?: RouterItem[]
}

export function login(data: { username: string; password: string }) {
  return request.post<any, LoginResult>('/system/user/login', data)
}

export function logout() {
  return request.post('/system/user/logout')
}

export function getInfo() {
  return request.get<any, UserInfo>('/system/user/getInfo')
}

export function getRouters() {
  return request.get<any, RouterItem[]>('/system/user/getRouters')
}
