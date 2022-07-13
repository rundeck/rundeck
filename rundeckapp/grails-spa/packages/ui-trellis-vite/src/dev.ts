import Vue from "vue";
import App from "./App.vue";
import { createApp, h } from "vue-demi";


Vue.config.productionTip = false;
Vue.config.devtools = true;


const app = createApp({
  render: () => h(App),
});

app.mount("#app");