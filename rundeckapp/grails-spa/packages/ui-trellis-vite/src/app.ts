import Vue from "vue";
import App from "./app/App.vue";
import { createApp, h } from "vue-demi";


Vue.config.productionTip = false;
Vue.config.devtools = true;

//**** Miguel  ****/
import './app/components/copybox/main'
import './app/components/central/main'
import './app/components/ko-paginator/main'
import './app/components/motd/main'
import './app/components/navbar/main'
import './app/components/project-picker/main'
import './app/components/first-run/main'
// import './app/components/theme/main'
import './app/components/tour/main'
import './app/components/version-notification/main'

import './app/pages/login/main'
import './app/pages/project-dashboard/main'
import './app/pages/project-activity/main'
import './app/pages/repository/main'
import './app/pages/command/main'
import './app/pages/community-news/main'

//**** Carlos  ****/
import './app/components/version/main'
import './app/components/server-identity/main'
import './app/components/community-news-notification/main'
import './app/pages/project-nodes-config/main'
import './app/pages/execution-show/main'
import './app/pages/webhooks/main'
import './app/pages/menu/main'
import './app/pages/dynamic-form/main'
import './app/pages/job/editor/main'



const app = createApp({
  render: () => h(App),
});

if(document.getElementById("sandbox-app"))
  app.mount("#sandbox-app");
