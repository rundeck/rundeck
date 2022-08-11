import Vue from "vue";
import App from "./app/App.vue";
import { createApp, h } from "vue-demi";


Vue.config.productionTip = false;
Vue.config.devtools = true;

// import './app/pages/command/main'

const app = createApp({
  render: () => h(App),
});

app.mount("#app");