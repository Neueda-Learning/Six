// 该文件用于封装 Axios 实例，统一处理后端响应信封与全局错误提示。
import axios from 'axios';
import { ElMessage } from 'element-plus';
import i18n from '../i18n';

// 创建 Axios 实例：baseURL 优先读取环境变量，未配置时默认走 /api（配合 Vite 代理转发到后端）
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
});

function pickErrorMessage(payload) {
  if (!payload) {
    return '';
  }

  if (typeof payload === 'string') {
    return payload;
  }

  if (typeof payload.message === 'string' && payload.message.trim()) {
    return payload.message;
  }

  return '';
}

http.interceptors.response.use(
  (response) => {
    // 后端统一响应信封结构为 { success, data, errorCode, message }
    const body = response.data;

    // 业务失败（success === false）：统一弹出错误提示，并以 reject 方式抛出，方便页面 catch 后做后续处理
    // 后端返回的 message 本身不做翻译（由后端决定语言），仅当后端未提供 message 时才使用前端本地化的兜底文案
    if (body && body.success === false) {
      ElMessage.error(body.message || i18n.global.t('http.requestFailed'));
      return Promise.reject(body);
    }

    // 业务成功：直接把信封整体返回给调用方，页面通过 res.data 取出真正的业务数据
    return body;
  },
  (error) => {
    // 网络异常或非 2xx 状态码：尝试从后端错误响应体中取出 message，否则退回到前端本地化的兜底文案
    const backendMessage = pickErrorMessage(error.response && error.response.data);
    ElMessage.error(backendMessage || error.message || i18n.global.t('http.networkError'));
    return Promise.reject(error);
  }
);

export default http;
