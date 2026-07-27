// 该文件用于定义前端模块骨架，后续需完成页面逻辑、接口调用与状态处理。
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        //todo replace target if backend runs on different host/port
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});
