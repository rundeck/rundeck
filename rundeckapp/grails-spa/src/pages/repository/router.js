import Vue from 'vue';
import Router from 'vue-router';

import PluginRepositoryView from "./views/PluginRepositoryView"
import PluginConfigurationView from './views/PluginConfigurationView'

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
      path: '*',
      redirect: '/artifact/index/repositories'
    }
  ]
});
