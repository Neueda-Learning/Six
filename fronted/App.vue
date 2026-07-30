<!-- 应用根组件：负责整体页面外壳布局（顶部导航 + 路由出口），不承载具体业务逻辑 -->
<template>
  <!-- el-config-provider 负责把当前语言对应的 Element Plus 内置文案（分页、日期等）注入给所有子组件 -->
  <el-config-provider :locale="elLocale">
    <el-container class="app-shell">
      <!-- 顶部导航栏：使用 el-menu 的 router 模式，点击菜单项自动触发路由跳转，并根据当前路径高亮 -->
      <el-header class="app-header">
        <div class="brand">
          <el-icon :size="22" class="brand-icon"><Wallet /></el-icon>
          <span class="brand-title">{{ t('app.title') }}</span>
        </div>
        <el-menu
          :default-active="route.path"
          mode="horizontal"
          router
          :ellipsis="false"
          class="nav-menu"
          background-color="transparent"
          text-color="rgba(255, 255, 255, 0.85)"
          active-text-color="#ffffff"
        >
          <el-menu-item index="/">
            <el-icon><List /></el-icon>
            <span>{{ t('app.navList') }}</span>
          </el-menu-item>
          <el-menu-item index="/payments/create">
            <el-icon><CirclePlus /></el-icon>
            <span>{{ t('app.navCreate') }}</span>
          </el-menu-item>
          <el-menu-item index="/payments/recycle-bin">
            <el-icon><Delete /></el-icon>
            <span>{{ t('app.navTrash') }}</span>
          </el-menu-item>
          <el-menu-item index="/accounts/balances">
            <el-icon><Search /></el-icon>
            <span>{{ t('app.navAccounts') }}</span>
          </el-menu-item>
        </el-menu>

        <!-- 语言切换器：下拉选择 中文/English/Deutsch，选择后立即切换并持久化到 localStorage -->
        <el-dropdown class="lang-switcher" trigger="click" @command="handleLocaleChange">
          <span class="lang-trigger">
            <el-icon :size="16"><Position /></el-icon>
            {{ t(`language.${locale}`) }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="item in localeOptions"
                :key="item"
                :command="item"
                :class="{ 'is-active-lang': item === locale }"
              >
                {{ t(`language.${item}`) }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="app-main">
        <!-- 路由出口：根据当前路径渲染 PaymentList / PaymentCreate / PaymentDetail -->
        <RouterView />
      </el-main>
    </el-container>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { Wallet, List, CirclePlus, Delete, Position, Search } from '@element-plus/icons-vue';
import zhCn from 'element-plus/dist/locale/zh-cn.mjs';
import en from 'element-plus/dist/locale/en.mjs';
import de from 'element-plus/dist/locale/de.mjs';
import { setLocale, SUPPORTED_LOCALES } from './i18n';

// 使用当前路由路径驱动顶部菜单高亮项，无需手动维护激活状态变量
const route = useRoute();

// t 用于翻译文案，locale 为当前语言（响应式），切换后模板自动重新渲染
const { t, locale } = useI18n();

const localeOptions = SUPPORTED_LOCALES;

// Element Plus 内置组件（分页、日期选择器等）的语言包，随当前语言联动切换
const elLocaleMap = { zh: zhCn, en, de };
const elLocale = computed(() => elLocaleMap[locale.value]);

/** 下拉菜单选中某语言时触发：切换 vue-i18n 当前语言并持久化 */
function handleLocaleChange(command) {
  setLocale(command);
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto minmax(140px, 1fr);
  align-items: center;
  gap: 40px;
  /* 渐变色导航栏，替代原本的纯白背景，让整个应用更有辨识度 */
  background: linear-gradient(135deg, #4f6df5 0%, #3d54d1 100%);
  box-shadow: 0 4px 16px rgba(61, 84, 209, 0.25);
  padding: 0 24px;
  height: 64px;
}

.brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  white-space: nowrap;
}

.brand-icon {
  color: #ffffff;
}

.brand-title {
  font-size: 19px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.3px;
}

.nav-menu {
  border-bottom: none;
  justify-self: center;
}

.nav-menu :deep(.el-menu-item) {
  background: transparent;
}

.nav-menu :deep(.el-menu-item:hover),
.nav-menu :deep(.el-menu-item:focus) {
  background-color: rgba(255, 255, 255, 0.14) !important;
}

.nav-menu :deep(.el-menu-item.is-active) {
  background-color: rgba(255, 255, 255, 0.18) !important;
  border-bottom-color: #ffffff !important;
  font-weight: 600;
}

.lang-switcher {
  white-space: nowrap;
  justify-self: end;
}

.lang-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.9);
  white-space: nowrap;
  padding: 6px 10px;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.lang-trigger:hover {
  background-color: rgba(255, 255, 255, 0.14);
  color: #ffffff;
}

.is-active-lang {
  color: var(--color-primary);
  font-weight: 600;
}

.app-main {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

@media (max-width: 960px) {
  .app-header {
    grid-template-columns: 1fr;
    justify-items: center;
    height: auto;
    padding: 16px 20px;
    gap: 12px;
  }

  .nav-menu,
  .lang-switcher {
    justify-self: center;
  }
}
</style>
