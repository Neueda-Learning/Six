<!-- 该文件用于定义页面组件骨架，后续需完成模板结构、交互逻辑与接口联调。 -->
<template>
  <div class="app-shell">
    <header class="app-header">
      <h1>Payments Processing System</h1>
      <!-- //todo replace with polished navigation UI -->
      <nav>
        <RouterLink to="/">Payments</RouterLink>
        <RouterLink to="/payments/create">Create</RouterLink>
      </nav>
    </header>
    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
//todo add layout-level loading/error handling
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #e5e7eb;
}

.app-header nav {
  display: flex;
  gap: 16px;
}

.app-main {
  padding: 24px;
}
</style>
