// 该文件用于定义前端模块骨架，后续需完成页面逻辑、接口调用与状态处理。
import axios from 'axios';

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
});

http.interceptors.response.use(
  (response) => {
    //todo normalize backend response envelope here
    return response;
  },
  (error) => {
    //todo handle global api error feedback
    return Promise.reject(error);
  }
);

export default http;
