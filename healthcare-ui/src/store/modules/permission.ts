import { defineStore } from 'pinia'
import { getRouters, type RouterItem } from '@/api/auth'
import type { RouteRecordRaw } from 'vue-router'
import { Layout, fixedPaths } from '@/router/constant'

/**
 * views 下的页面组件。
 * 必须用 import.meta.glob 静态展开：import(`../views/${component}.vue`) 这种变量导入
 * 只能匹配一级目录（views/*.vue），而页面都放在子目录里；
 * 而且相对路径是相对本文件解析的，'../views' 实际指向 src/store/views，并不存在。
 * 两种写法编译后都是一张空表，运行期任何菜单都会报 "Unknown variable dynamic import"。
 * 这里统一用根路径写法，开发、生产构建、SSR 下解析到的都是同一批文件。
 */
const viewModules = import.meta.glob('/src/views/**/*.vue')

/** 页面组件在 viewModules 中的路径前缀 */
const VIEW_PREFIX = '/src/views/'

/**
 * 取菜单路径片段，去掉首尾斜杠。
 * 后端 sys_menu.path 存的是 "index"、"system/user" 这类相对值，
 * 而 vue-router 要求顶层路由路径必须以 "/" 开头。
 */
function toSegment(path?: string): string {
  return (path || '').trim().replace(/^\/+/, '').replace(/\/+$/, '')
}

/**
 * 拼出菜单的绝对访问路径，例如 system + user -> /system/user。
 */
function toFullPath(parentPath: string, path?: string): string {
  const segment = toSegment(path)
  if (!segment) {
    return parentPath || '/'
  }
  return `${parentPath.replace(/\/$/, '')}/${segment}`
}

/**
 * 生成全局唯一的路由名。
 * 后端按单层路径命名（多个模块下都有 index、record），重名时 vue-router 的
 * addRoute 会把先注册的同名路由直接删掉，所以这里统一按完整路径生成。
 */
function toRouteName(prefix: string, fullPath: string): string {
  const key = fullPath.replace(/^\//, '').replace(/\//g, '_')
  return key ? `${prefix}_${key}` : prefix
}

/**
 * 菜单组件 -> 路由组件。后端 "Layout" 表示目录，其余是 views 下的相对路径。
 */
function toRouteComponent(component?: string) {
  if (!component || component === 'Layout') {
    return Layout
  }
  const loader = viewModules[`${VIEW_PREFIX}${component}.vue`]
  if (!loader) {
    console.warn(`菜单对应的页面组件不存在：${component}`)
    return Layout
  }
  return loader as () => Promise<unknown>
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    routes: [] as RouteRecordRaw[],
    raw: [] as RouterItem[],
    loaded: false
  }),
  actions: {
    async loadRoutes() {
      const routers = (await getRouters()) as RouterItem[]
      this.raw = routers.map((item) => this.normalizeMenu(item, 0))
      // 固定路由已经提供的路径不再重复注册
      this.routes = routers
        .filter((item) => !fixedPaths.includes(toFullPath('', item.path)))
        .map((item) => this.toRoute(item, '', true))
      this.loaded = true
      return this.routes
    },

    /**
     * 侧边栏用的菜单数据：顶层路径转成绝对路径，子级保持相对路径，
     * 这样 SidebarItem 拼出来的访问路径与路由表完全一致。
     */
    normalizeMenu(item: RouterItem, level: number): RouterItem {
      const segment = toSegment(item.path)
      return {
        ...item,
        path: level === 0 ? `/${segment}` : segment,
        children: item.children && item.children.length
          ? item.children.map((child) => this.normalizeMenu(child, level + 1))
          : item.children
      }
    },

    /**
     * 后端菜单 -> 前端路由。
     * 顶层菜单统一套一层 Layout，这样"首页"这类叶子菜单也会渲染在左侧菜单/顶栏外壳内，
     * 而不是变成一个裸页面。
     */
    toRoute(item: RouterItem, parentPath = '', isRoot = true): RouteRecordRaw {
      const fullPath = toFullPath(parentPath, item.path)
      const children = item.children || []
      const hasChildren = children.length > 0
      const meta = {
        title: item.meta?.title || item.name,
        icon: item.meta?.icon,
        hidden: item.meta?.hidden,
        keepAlive: item.meta?.keepAlive
      }

      if (hasChildren) {
        const wrapper: any = {
          path: fullPath,
          name: toRouteName('L', fullPath),
          component: Layout,
          meta: isRoot ? { title: meta.title, icon: meta.icon, hidden: meta.hidden } : {},
          children: children.map((child) => this.toRoute(child, fullPath, false))
        }
        // 父级菜单默认跳到第一个可见子菜单，避免点开父级菜单出现空白内容区
        const first = wrapper.children.find((child: any) => !child.meta?.hidden)
        if (first) {
          wrapper.redirect = first.path
        }
        return wrapper as RouteRecordRaw
      }

      if (isRoot) {
        // 叶子菜单：Layout 作为外壳，真正的页面作为空路径子路由
        return {
          path: fullPath,
          name: toRouteName('L', fullPath),
          component: Layout,
          children: [
            {
              path: '',
              name: toRouteName('P', fullPath),
              component: toRouteComponent(item.component),
              meta
            }
          ]
        } as RouteRecordRaw
      }

      return {
        path: fullPath,
        name: toRouteName('P', fullPath),
        component: toRouteComponent(item.component),
        meta
      } as RouteRecordRaw
    },
    reset() {
      this.routes = []
      this.raw = []
      this.loaded = false
    }
  }
})
