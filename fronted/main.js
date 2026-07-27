// 该文件用于定义前端模块骨架，后续需完成页面逻辑、接口调用与状态处理。
import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import App from './App.vue';
import router from './router';
import './styles/base.css';

const app = createApp(App);

app.use(router);
app.use(ElementPlus);

//todo register global directives/filters when needed
app.mount('#app');
