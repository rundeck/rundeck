// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import Vue2Filters from 'vue2-filters'
import VueCookies from 'vue-cookies'
import App from './App.vue'

Vue.config.productionTip = false

Vue.use(Vue2Filters)
Vue.use(VueCookies)
Vue.use(VueCookies)

const els = document.body.getElementsByClassName('dynamic-form-vue')

for (var i = 0; i < els.length; i++) {
  const el = els[i];

  /* eslint-disable no-new */
  new Vue({
    el: el,
    render(h) {
      return h(App, {
        props: {
          fields: this.$el.attributes.fields.value,
          options: this.$el.attributes.options.value,
          hasOptions: this.$el.attributes.hasOptions.value,
          element: this.$el.attributes.element.value,
          name: this.$el.attributes.name.value,
        }
      })
    },
  });
}

