<!-- 应用根组件：负责整体页面外壳布局（顶部导航 + 路由出口），不承载具体业务逻辑 -->
<template>
  <div class="app-shell">
    <header class="app-header">
      <h1>Payments Processing System</h1>
      <!-- 顶部导航：Payments 对应列表页，Create 对应创建支付页 -->
      <nav>
        <RouterLink to="/">Payments</RouterLink>
        <RouterLink to="/payments/create">Create</RouterLink>
      </nav>
    </header>
    <main class="app-main">
      <!-- 路由出口：根据当前路径渲染 PaymentList / PaymentCreate / PaymentDetail -->
      <RouterView />
    </main>
  </div>
</template>

<script setup>
// 当前页面为纯布局壳组件，具体的加载态与错误提示已下沉到各页面自行处理
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
