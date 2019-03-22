import Vue from 'vue'
import vSelect from 'vue-select'
import InstantSearch from 'vue-instantsearch';
import VueMoment from 'vue-moment';
// import "bulma/css/bulma.css.min.css"
import App from './App.vue'

Vue.config.productionTip = false


Vue.use(InstantSearch);
Vue.use(VueMoment);

// register globally
Vue.component('v-select', vSelect)

new Vue({
  render: h => h(App)
}).$mount('#plugin-search-vue')
