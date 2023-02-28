import Vue from 'vue';
import Router from 'vue-router';

import PluginRepositoryView from "./views/PluginRepositoryView"
import PluginConfigurationView from './views/PluginConfigurationView'
import UploadPluginView from './views/UploadPluginView'

Vue.use(Router)

var ctx = window._rundeck.context;
if(ctx !== "/") ctx += "/";

export default new Router({
  mode: 'history',
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
      path: '*',
      redirect: ctx+'artifact/index/configurations'
    }
  ]
});
