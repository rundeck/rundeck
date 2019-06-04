import Vue from 'vue';
import Router from 'vue-router';

import WebhooksView from "./views/WebhooksView"

Vue.use(Router)

export default new Router({
  mode: 'history',
  linkExactActiveClass: 'is-active',
  routes: [{
      path: '/webhooks',
      name: 'webhooks',
      component: WebhooksView
    }
  ]
});
