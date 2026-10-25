<template>
  <div class="layout-root" :class="{ 'layout-root--collapsed': collapsed }">
    <AppSidebar />
    <div class="layout-body">
      <AppNavbar />
      <AppMain />
    </div>
  </div>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import AppSidebar from './components/Sidebar.vue'
import AppNavbar from './components/Navbar.vue'
import AppMain from './components/AppMain.vue'
import { useAppStore } from '@/store/modules/app'

const appStore = useAppStore()
const { collapsed } = storeToRefs(appStore)
</script>

<style scoped lang="scss">
.layout-root {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.layout-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}
</style>
