import { defineStore } from 'pinia'

const COLLAPSE_KEY = 'healthcare_collapsed'

/**
 * 界面偏好：侧边栏折叠状态等。
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    collapsed: localStorage.getItem(COLLAPSE_KEY) === '1'
  }),
  actions: {
    toggleCollapse() {
      this.collapsed = !this.collapsed
      localStorage.setItem(COLLAPSE_KEY, this.collapsed ? '1' : '0')
      window.dispatchEvent(new Event('resize'))
    },
    setCollapsed(value: boolean) {
      this.collapsed = value
      localStorage.setItem(COLLAPSE_KEY, value ? '1' : '0')
      window.dispatchEvent(new Event('resize'))
    }
  }
})
