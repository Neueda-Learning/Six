// 该文件用于创建并导出全局 i18n 实例，供 main.js 挂载以及 http.js 等非组件模块调用。
import { createI18n } from 'vue-i18n';
import en from './locales/en';
import zh from './locales/zh';
import de from './locales/de';

export const SUPPORTED_LOCALES = ['zh', 'en', 'de'];
const STORAGE_KEY = 'payments-locale';

/** 读取上次保存的语言，若无记录或不受支持则回退到中文 */
function resolveInitialLocale() {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved && SUPPORTED_LOCALES.includes(saved)) {
    return saved;
  }
  return 'zh';
}

const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式，便于在 <script setup> 中通过 useI18n() 调用
  locale: resolveInitialLocale(),
  fallbackLocale: 'en',
  messages: { en, zh, de }
});

/** 切换语言并持久化到 localStorage，同时同步 html lang 属性 */
export function setLocale(locale) {
  if (!SUPPORTED_LOCALES.includes(locale)) {
    return;
  }
  i18n.global.locale.value = locale;
  localStorage.setItem(STORAGE_KEY, locale);
  document.documentElement.lang = locale;
}

// 初始化时同步一次 html lang 属性
document.documentElement.lang = i18n.global.locale.value;

export default i18n;
