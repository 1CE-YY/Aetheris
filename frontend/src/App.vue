<template>
  <div id="app">
    <!-- 初始化中，显示 loading -->
    <div v-if="initializing" class="initializing">
      <div class="loading-content">
        <div class="title">Aetheris RAG System</div>
        <div class="subtitle">正在加载...</div>
      </div>
    </div>
    <!-- 初始化完成，显示路由 -->
    <router-view v-else />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const initializing = ref(true)

onMounted(async () => {
  // 初始化用户状态（从 localStorage 恢复 token 并验证）
  await userStore.initialize()
  initializing.value = false
})
</script>

<style scoped>
.initializing {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f5;
}

.loading-content {
  text-align: center;
}

.title {
  font-size: 24px;
  margin-bottom: 16px;
  color: #1890ff;
}

.subtitle {
  font-size: 14px;
  color: #666;
}
</style>

<style>
/* 全局样式 */
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',
    'Noto Color Emoji';
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
}
</style>
