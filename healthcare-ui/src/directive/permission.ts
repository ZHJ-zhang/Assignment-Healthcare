import type { App } from 'vue'
import { useUserStore } from '@/store/modules/user'

/**
 * v-perm 指令：当前用户缺少权限时移除元素。
 *
 * 用法：v-perm="'system:user:add'" 或 v-perm="['system:user:add', 'system:user:edit']"
 */
export function setupPermission(app: App) {
  app.directive('perm', {
    mounted(el: HTMLElement, binding: any) {
      const userStore = useUserStore()
      const value = binding.value
      if (!value) return
      const list = Array.isArray(value) ? value : [value]
      if (!userStore.hasPermission(list)) {
        el.parentNode?.removeChild(el)
      }
    }
  })
}
