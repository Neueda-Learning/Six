<!-- 应用根组件：负责整体页面外壳布局（顶部导航 + 路由出口），不承载具体业务逻辑 -->
<template>
  <!-- el-config-provider 负责把当前语言对应的 Element Plus 内置文案（分页、日期等）注入给所有子组件 -->
  <el-config-provider :locale="elLocale">
    <el-container class="app-shell">
      <div class="ambient ambient-one"></div>
      <div class="ambient ambient-two"></div>
      <div class="ambient ambient-three"></div>
      <!-- 顶部导航栏：使用 el-menu 的 router 模式，点击菜单项自动触发路由跳转，并根据当前路径高亮 -->
      <el-header class="app-header">
        <div class="brand">
          <span class="brand-mark">
            <el-icon :size="22" class="brand-icon"><Wallet /></el-icon>
          </span>
          <div class="brand-copy">
            <span class="brand-title">{{ t('app.title') }}</span>
            <span class="brand-subtitle">Live transfer console</span>
          </div>
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
        <div class="route-stage">
          <!-- 路由出口：根据当前路径渲染 PaymentList / PaymentCreate / PaymentDetail -->
          <RouterView />
        </div>
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
  position: relative;
  overflow: hidden;
}

.ambient {
  position: absolute;
  border-radius: 999px;
  filter: blur(18px);
  opacity: 0.8;
  pointer-events: none;
}

.ambient-one {
  top: 68px;
  left: -80px;
  width: 260px;
  height: 260px;
  background: radial-gradient(circle, rgba(9, 180, 199, 0.22), transparent 68%);
}

.ambient-two {
  top: 20px;
  right: 8%;
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(255, 156, 66, 0.22), transparent 68%);
}

.ambient-three {
  bottom: 8%;
  right: -60px;
  width: 260px;
  height: 260px;
  background: radial-gradient(circle, rgba(15, 111, 255, 0.18), transparent 70%);
}

.app-header {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto minmax(140px, 1fr);
  align-items: center;
  height: auto;
  gap: 40px;
  position: sticky;
  top: 16px;
  z-index: 20;
  background: linear-gradient(135deg, rgba(6, 29, 76, 0.92) 0%, rgba(11, 76, 160, 0.88) 62%, rgba(7, 154, 191, 0.86) 100%);
  box-shadow: 0 26px 50px rgba(12, 36, 79, 0.24);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(20px);
  padding: 14px 24px;
  margin: 18px 18px 0;
  border-radius: 26px;
  min-height: 78px;
  box-sizing: border-box;
}

.app-header::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, transparent 0%, rgba(255, 255, 255, 0.10) 34%, transparent 58%);
  pointer-events: none;
}

.brand {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  align-self: center;
  min-height: 48px;
  white-space: nowrap;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0.08));
  border: 1px solid rgba(255, 255, 255, 0.22);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.24);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 3px;
}

.brand-icon {
  color: #ffffff;
}

.brand-title {
  font-size: 19px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: 0.08px;
  line-height: 1.05;
}

.brand-subtitle {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.64);
  line-height: 1.05;
}

.nav-menu {
  border-bottom: none;
  justify-self: center;
  align-self: center;
  display: flex;
  align-items: center;
  min-height: 48px;
  margin-top: 2px;
}

.nav-menu :deep(.el-menu--horizontal) {
  display: flex;
  align-items: center;
  border-bottom: none;
  min-height: 48px;
}

.nav-menu :deep(.el-menu-item) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border-radius: 999px;
  margin: 0 4px;
  height: 42px;
  line-height: 1;
}

.nav-menu :deep(.el-menu-item > span) {
  font-weight: 500;
  letter-spacing: 0.01em;
}

.nav-menu :deep(.el-menu-item:hover),
.nav-menu :deep(.el-menu-item:focus) {
  background-color: rgba(255, 255, 255, 0.14) !important;
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.22), rgba(255, 255, 255, 0.14)) !important;
  border-bottom-color: transparent !important;
  font-weight: 500;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.nav-menu :deep(.el-menu-item.is-active > span) {
  font-weight: 550;
}

.lang-switcher {
  display: flex;
  align-items: center;
  align-self: center;
  white-space: nowrap;
  justify-self: end;
  min-height: 48px;
}

.lang-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.9);
  white-space: nowrap;
  font-weight: 500;
  padding: 10px 14px;
  min-height: 42px;
  line-height: 1;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
  transition: background-color 0.2s ease;
}

.lang-trigger:hover {
  background-color: rgba(255, 255, 255, 0.14);
  color: #ffffff;
}

.is-active-lang {
  color: var(--color-primary);
  font-weight: 500;
}

.app-main {
  padding: 24px 12px 18px;
  max-width: 1520px;
  margin: 0 auto;
  width: 100%;
}

.route-stage {
  position: relative;
  animation: route-rise 0.45s ease;
}

@keyframes route-rise {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 960px) {
  .app-header {
    grid-template-columns: 1fr;
    justify-items: center;
    min-height: auto;
    padding: 16px 20px;
    margin: 18px 18px 0;
    gap: 12px;
  }

  .nav-menu,
  .lang-switcher {
    justify-self: center;
  }

  .brand {
    justify-content: center;
  }
}
</style>
