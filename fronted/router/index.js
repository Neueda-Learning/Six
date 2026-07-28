// 该文件用于定义前端模块骨架，后续需完成页面逻辑、接口调用与状态处理。
import { createRouter, createWebHistory } from 'vue-router';
import PaymentList from '../views/PaymentList.vue';
import PaymentCreate from '../views/PaymentCreate.vue';
import PaymentDetail from '../views/PaymentDetail.vue';
import PaymentTrash from '../views/PaymentTrash.vue';

const routes = [
  {
    path: '/',
    name: 'PaymentList',
    component: PaymentList
  },
  {
    path: '/payments/create',
    name: 'PaymentCreate',
    component: PaymentCreate
  },
  {
    path: '/payments/recycle-bin',
    name: 'PaymentTrash',
    component: PaymentTrash
  },
  {
    path: '/payments/:id',
    name: 'PaymentDetail',
    component: PaymentDetail,
    props: true
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

//todo add route guards if auth is introduced in future
export default router;
