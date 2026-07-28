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
 * 优先级：localStorage 中的历史选择 > 浏览器/系统语言 > 中文兜底。
 * - 若 localStorage 中有记录且在支持列表内，直接使用（尊重用户上次的主动选择）。
 * - 否则读取 navigator.language（如 'de-DE'、'en-US'），取前两位语言代码（'de'/'en'）匹配支持列表，
 *   让首次访问的用户能直接看到自己系统语言对应的界面，而不是每次都先看到中文再手动切换。
 * - 都不匹配时回退到中文，保证不会出现空白语言或报错。
 */
function resolveInitialLocale() {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved && SUPPORTED_LOCALES.includes(saved)) {
    return saved;
  }

  const browserLang = (navigator.language || '').slice(0, 2).toLowerCase();
  if (SUPPORTED_LOCALES.includes(browserLang)) {
    return browserLang;
  }

  return 'zh';
}

// 数字格式化配置（供 useI18n() 的 n() 函数使用）：
// - currency 格式用于金额展示，具体货币符号在调用处通过 { key: 'currency', currency: row.currency } 动态覆盖，
//   这里的 currency 字段只是必填的默认值占位，实际展示以每笔支付自己的币种为准；
//   真正随语言变化的是千分位分隔符、小数点符号的展示习惯（如英文 1,234.56 / 德文 1.234,56）。
const numberFormats = {
  zh: { currency: { style: 'currency', currency: 'CNY', currencyDisplay: 'symbol' } },
  en: { currency: { style: 'currency', currency: 'USD', currencyDisplay: 'symbol' } },
  de: { currency: { style: 'currency', currency: 'EUR', currencyDisplay: 'symbol' } }
};

// 日期时间格式化配置（供 useI18n() 的 d() 函数使用）：不同语言习惯的日期顺序、是否 12/24 小时制不同，
// 用 'short' 这个格式名统一在各页面调用，具体展示规则按当前 locale 自动切换。
const datetimeFormats = {
  zh: {
    short: { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }
  },
  en: {
    short: { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: true }
  },
  de: {
    short: { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }
  }
};

// 创建全局唯一的 i18n 实例：
// - legacy: false      使用 Composition API 模式，这样组件里才能用 useI18n() 拿到响应式的 t()/locale
// - locale             应用启动时的初始语言，来自上面读取的持久化结果
// - fallbackLocale      当某个语言包缺失某个 key 的翻译时，回退去这个语言里找，避免页面出现 undefined
// - messages            三种语言的文案字典，key 与各 locales/*.js 文件的结构一一对应
const i18n = createI18n({
  legacy: false,
  locale: resolveInitialLocale(),
  fallbackLocale: 'en',
  messages: { en, zh, de },
  numberFormats,
  datetimeFormats
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

