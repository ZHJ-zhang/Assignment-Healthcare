<template>
  <el-sub-menu v-if="visibleChildren.length" :index="resolvedPath">
    <template #title>
      <el-icon v-if="item.meta?.icon">
        <component :is="item.meta.icon" />
      </el-icon>
      <span>{{ item.meta?.title }}</span>
    </template>
    <SidebarItem
      v-for="child in visibleChildren"
      :key="child.path"
      :item="child"
      :base-path="resolvedPath"
    />
  </el-sub-menu>

  <el-menu-item v-else :index="resolvedPath">
    <el-icon v-if="item.meta?.icon">
      <component :is="item.meta.icon" />
    </el-icon>
    <template #title>{{ item.meta?.title }}</template>
  </el-menu-item>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RouterItem } from '@/api/auth'

const props = defineProps<{
  item: RouterItem
  basePath: string
}>()

/**
 * 拼接菜单项的完整访问路径。
 */
const resolvedPath = computed(() => {
  const path = props.item.path || ''
  if (!props.basePath) return path
  if (!path) return props.basePath
  if (path.startsWith('/')) return path
  return `${props.basePath.replace(/\/$/, '')}/${path}`
})

/**
 * 需要展示的子菜单，隐藏项不参与渲染。
 */
const visibleChildren = computed(() => {
  const children = props.item.children || []
  return children.filter((child) => !child.meta?.hidden)
})
</script>
