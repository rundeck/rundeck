import Vue from 'vue';
import Router from 'vue-router';

import PluginRepositoryView from "./views/PluginRepositoryView"
import PluginConfigurationView from './views/PluginConfigurationView'
import UploadPluginView from './views/UploadPluginView'

Vue.use(Router)

export default new Router({
  mode: 'history',
  linkExactActiveClass: 'is-active',
  routes: [{
      path: '/artifact/index/repositories',
      name: 'repositories',
      component: PluginRepositoryView
    },
    {
      path: '/artifact/index/configurations',
      name: 'configurations',
      component: PluginConfigurationView
    },
    {
      path: '/artifact/index/upload',
      name: 'upload',
      component: UploadPluginView
    },
    {
      path: '*',
      redirect: '/artifact/index/configurations'
    }
  ]
});
