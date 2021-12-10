import Vue from "vue";
import App from "@/App.vue";
import { createApp, h } from "vue-demi";

import "windi.css";
import router from "@/routes";

Vue.config.productionTip = false;
Vue.config.devtools = true;

import "@/entries/nav";

const app = createApp({
  router,
  render: () => h(App),
});

app.mount("#app");
