import { defineStore } from 'pinia'
import { getInfo, login, logout, type LoginResult, type UserInfo } from '@/api/auth'
import { clearLookupCache } from '@/utils/lookup'

const TOKEN_KEY = 'healthcare_token'

/**
 * 读取本地令牌。历史版本登录失败时会把字符串 "undefined" 写进本地存储，这里一并清理。
 */
function readToken(): string {
  const saved = localStorage.getItem(TOKEN_KEY)
  return saved && saved !== 'undefined' ? saved : ''
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: readToken(),
    userInfo: null as UserInfo | null,
    roles: [] as string[],
    permissions: [] as string[]
  }),
  getters: {
    isAdmin: (state) => state.roles.includes('admin'),
    /** 数据范围：1 全部、2 本部门及以下、3 本部门、4 仅本人；缺省按"全部"处理，避免误锁。 */
    dataScope: (state) => state.userInfo?.dataScope ?? 1,
    /**
     * 只能看自己名下的数据（医生）。
     * 页面靠这个标记把科室锁死在自己的科室，与后端 DataScopeHelper 保持一致。
     */
    isSelfScope: (state) => (state.userInfo?.dataScope ?? 1) === 4,
    /** 自己的科室：仅本人范围下用来预置并锁死科室选择。 */
    ownDeptId: (state) => state.userInfo?.deptId,
    displayName: (state) => state.userInfo?.realName || state.userInfo?.username || '未登录'
  },
  actions: {
    async login(payload: { username: string; password: string }) {
      const result = (await login(payload)) as LoginResult
      if (!result || !result.token) {
        throw new Error('登录响应中缺少访问令牌')
      }
      this.token = result.token
      localStorage.setItem(TOKEN_KEY, result.token)
      // 换账号登录：先清下拉缓存，保证选项按新身份的可见范围重新拉取
      clearLookupCache()
      await this.loadProfile()
      return result
    },
    async loadProfile() {
      const info = (await getInfo()) as UserInfo
      this.userInfo = info
      this.roles = info.roles || []
      this.permissions = info.permissions || []
    },
    async logout() {
      try {
        await logout()
      } finally {
        this.reset()
      }
    },
    reset() {
      this.token = ''
      this.userInfo = null
      this.roles = []
      this.permissions = []
      localStorage.removeItem(TOKEN_KEY)
      // 下拉缓存里存着"上一个账号能看到的患者/科室"，换人登录必须清掉，
      // 否则医生会在患者下拉里看到上一个账号（比如管理员）的全院名单。
      clearLookupCache()
    },
    hasPermission(value: string | string[]): boolean {
      if (this.isAdmin) return true
      const list = Array.isArray(value) ? value : [value]
      return list.some((item) => this.permissions.includes(item))
    }
  }
})
