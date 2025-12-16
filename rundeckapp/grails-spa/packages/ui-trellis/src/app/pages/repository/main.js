// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import { createApp } from "vue";
import { createPinia } from "pinia";
import VueCookies from "vue-cookies";
import VueScrollTo from "vue-scrollto";
import * as uiv from "uiv";

import router from "./router";
import App from "./App.vue";
import { initI18n } from "../../utilities/i18n";

const i18n = initI18n();
const pinia = createPinia();

const app = createApp({
  name: "RepositoryApp",
  components: {
    App,
  },
  template: "<App/>",
});
app.use(pinia);
app.use(router);
app.use(VueCookies);
app.use(VueScrollTo);
app.use(uiv);
app.use(i18n);
app.mount("#repository-vue");
