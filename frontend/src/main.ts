import { createApp } from 'vue';
import { createPinia } from 'pinia';
import Antd from 'ant-design-vue';
import App from './App.vue';
import router from './router';
import { useUserStore } from './stores/user';
import 'ant-design-vue/dist/reset.css';

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(Antd);

// 初始化用户状态（验证 token）
const userStore = useUserStore();

// 等待 router 准备好并验证 token
router.isReady().then(() => {
  userStore.initialize().then(() => {
    // 如果当前不在登录/注册页，且未登录，则跳转到登录页
    const currentPath = window.location.pathname;
    const isAuthPage = currentPath === '/login' || currentPath === '/register';

    if (!isAuthPage && !userStore.isLoggedIn) {
      router.push('/login');
    }
  });
});

app.mount('#app');
