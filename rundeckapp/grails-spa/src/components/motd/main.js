// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import VueCookies from 'vue-cookies'
import { EventBus } from '../../utilities/vueEventBus.js'
import Motd from '@/components/motd/motd'
import MotdIndicator from '@/components/motd/motdIndicator'
import * as uiv from 'uiv'

Vue.config.productionTip = false

Vue.use(VueCookies)
Vue.use(uiv)

/* eslint-disable no-new */

const els = document.body.getElementsByClassName('vue-project-motd')

for (var i = 0; i < els.length; i++) {
  const e = els[i]

  new Vue({
    el: e,
    components: {
      Motd,
      MotdIndicator
    },
    data() {
      return {
        EventBus
      }
    }
  })
}
