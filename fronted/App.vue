<!-- 应用根组件：负责整体页面外壳布局（顶部导航 + 路由出口），不承载具体业务逻辑 -->
<template>
  <el-container class="app-shell">
    <!-- 顶部导航栏：使用 el-menu 的 router 模式，点击菜单项自动触发路由跳转，并根据当前路径高亮 -->
    <el-header class="app-header">
      <div class="brand">
        <el-icon :size="22" class="brand-icon"><Wallet /></el-icon>
        <span class="brand-title">Payments Processing System</span>
      </div>
      <el-menu :default-active="route.path" mode="horizontal" router :ellipsis="false" class="nav-menu">
        <el-menu-item index="/">
          <el-icon><List /></el-icon>
          <span>Payments</span>
        </el-menu-item>
        <el-menu-item index="/payments/create">
          <el-icon><CirclePlus /></el-icon>
          <span>Create</span>
        </el-menu-item>
      </el-menu>
    </el-header>

    <el-main class="app-main">
      <!-- 路由出口：根据当前路径渲染 PaymentList / PaymentCreate / PaymentDetail -->
      <RouterView />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router';
import { Wallet, List, CirclePlus } from '@element-plus/icons-vue';

// 使用当前路由路径驱动顶部菜单高亮项，无需手动维护激活状态变量
const route = useRoute();
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  padding: 0 24px;
  height: 60px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.brand-icon {
  color: #2563eb;
}

.brand-title {
  font-size: 18px;
  font-weight: 600;
}

.nav-menu {
  border-bottom: none;
}

.app-main {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}
</style>
