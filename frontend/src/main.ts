import { createApp } from 'vue';
import { createPinia } from 'pinia';
import Antd from 'ant-design-vue';
import App from './App.vue';
import router from './router';
import 'ant-design-vue/dist/reset.css';

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(Antd);

// 挂载应用（初始化逻辑移到 App.vue 中）
app.mount('#app');
