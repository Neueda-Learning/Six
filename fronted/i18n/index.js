// 该文件用于创建并导出全局 i18n 实例，供 main.js 挂载以及 http.js 等非组件模块调用。
import { createI18n } from 'vue-i18n';
import en from './locales/en';
import zh from './locales/zh';
import de from './locales/de';

// 支持的语言列表：语言切换下拉框的选项来源，也用于校验非法语言代码
export const SUPPORTED_LOCALES = ['zh', 'en', 'de'];

// localStorage 中保存当前语言的键名，刷新页面后靠它恢复用户上次的语言选择
const STORAGE_KEY = 'payments-locale';

/**
 * 读取上次保存的语言，用于应用启动时决定初始显示哪种语言。
 * 若 localStorage 中没有记录（首次访问）或记录的值不在支持列表内（比如旧版本存过其他语言代码），
 * 一律回退到中文，保证不会出现空白语言或报错。
 */
function resolveInitialLocale() {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved && SUPPORTED_LOCALES.includes(saved)) {
    return saved;
  }
  return 'zh';
}

// 创建全局唯一的 i18n 实例：
// - legacy: false      使用 Composition API 模式，这样组件里才能用 useI18n() 拿到响应式的 t()/locale
// - locale             应用启动时的初始语言，来自上面读取的持久化结果
// - fallbackLocale      当某个语言包缺失某个 key 的翻译时，回退去这个语言里找，避免页面出现 undefined
// - messages            三种语言的文案字典，key 与各 locales/*.js 文件的结构一一对应
const i18n = createI18n({
  legacy: false,
  locale: resolveInitialLocale(),
  fallbackLocale: 'en',
  messages: { en, zh, de }
});

/**
 * 语言切换的核心函数：由 App.vue 顶部语言下拉菜单的点击事件调用。
 * 切换动作分三步，缺一不可：
 * 1. 修改 i18n.global.locale.value —— 这是一个 Vue 响应式 ref，赋值后会通知所有用到 t()/locale 的组件重新渲染，
 *    界面文字因此立即变成新语言，不需要刷新页面。
 * 2. 写入 localStorage —— 让这次选择被"记住"，下次打开网站时 resolveInitialLocale() 能读到并恢复。
 * 3. 同步 document.documentElement.lang —— 更新 <html lang="..."> 属性，这不是给用户看的，
 *    而是给屏幕阅读器、浏览器翻译工具等辅助功能使用，保证无障碍体验和语言标注保持一致。
 */
export function setLocale(locale) {
  if (!SUPPORTED_LOCALES.includes(locale)) {
    // 传入了不在支持列表内的语言代码时直接忽略，防止切换到一个没有对应语言包的状态
    return;
  }
  i18n.global.locale.value = locale;
  localStorage.setItem(STORAGE_KEY, locale);
  document.documentElement.lang = locale;
}

// 应用首次加载时（还没有用户点击切换），也需要把 html lang 设置成初始语言，
// 否则在用户手动切换一次之前，<html lang> 会一直停留在 index.html 里写死的默认值。
document.documentElement.lang = i18n.global.locale.value;

export default i18n;

