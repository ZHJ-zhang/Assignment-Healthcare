<template>
  <aside class="app-sidebar">
    <div class="app-sidebar__brand">
      <span class="app-sidebar__mark">HC</span>
      <span v-show="!collapsed" class="app-sidebar__title">医疗后台管理系统</span>
    </div>

    <el-scrollbar class="app-sidebar__scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        unique-opened
        router
      >
        <SidebarItem
          v-for="item in menus"
          :key="item.path"
          :item="item"
          base-path=""
        />
      </el-menu>
    </el-scrollbar>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import { usePermissionStore } from '@/store/modules/permission'
import { useAppStore } from '@/store/modules/app'
import SidebarItem from './SidebarItem.vue'

const route = useRoute()
const permissionStore = usePermissionStore()
const appStore = useAppStore()
const { collapsed } = storeToRefs(appStore)

const menus = computed(() =>
  (permissionStore.raw || []).filter((item) => !item.meta?.hidden)
)

/**
 * 当前高亮的菜单项，需要与 el-menu-item 的 index 一致。
 */
const activeMenu = computed(() => route.path)
</script>

<style scoped lang="scss">
.app-sidebar {
  display: flex;
  flex-direction: column;
  flex: 0 0 218px;
  width: 218px;
  overflow: hidden;
  background: var(--hc-bg-dark);
  transition: width 0.2s ease, flex-basis 0.2s ease;
}

:global(.layout-root--collapsed) .app-sidebar {
  flex-basis: 64px;
  width: 64px;
}

.app-sidebar__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  white-space: nowrap;
}

.app-sidebar__mark {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  border-radius: var(--hc-radius-sm);
  background: var(--hc-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.app-sidebar__title {
  overflow: hidden;
  color: #e7eef0;
  font-size: 15px;
  font-weight: 600;
  text-overflow: ellipsis;
}

.app-sidebar__scroll {
  flex: 1;
  min-height: 0;
}

:deep(.el-menu) {
  border-right: none;
  background: transparent;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  color: #b6c5c9;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background: var(--hc-bg-dark-soft);
  color: #fff;
}

:deep(.el-menu-item.is-active) {
  background: var(--hc-primary);
  color: #fff;
}

:deep(.el-menu--collapse .el-menu-item),
:deep(.el-menu--collapse .el-sub-menu__title) {
  height: 48px;
  line-height: 48px;
}

:deep(.el-menu--collapse > .el-menu-item .el-icon),
:deep(.el-menu--collapse > .el-sub-menu > .el-sub-menu__title .el-icon) {
  margin-right: 0;
}

:deep(.el-sub-menu .el-menu) {
  background: rgba(0, 0, 0, 0.16);
}
</style>
