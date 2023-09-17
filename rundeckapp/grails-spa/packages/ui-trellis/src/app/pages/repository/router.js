import {createRouter, createWebHistory} from 'vue-router';

import PluginRepositoryView from "./views/PluginRepositoryView.vue"
import PluginConfigurationView from './views/PluginConfigurationView.vue'
import UploadPluginView from './views/UploadPluginView.vue'

let ctx = window._rundeck.context;
if(ctx !== "/") ctx += "/";

const router = createRouter({
  history: createWebHistory(),
  linkExactActiveClass: 'is-active',
  routes: [{
      path: ctx+'artifact/index/repositories',
      name: 'repositories',
      component: PluginRepositoryView
    },
    {
      path: ctx+'artifact/index/configurations',
      name: 'configurations',
      component: PluginConfigurationView
    },
    {
      path: ctx+'artifact/index/upload',
      name: 'upload',
      component: UploadPluginView
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: ctx+'artifact/index/configurations'
    }
  ]
});
export default router