// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import {createApp, h} from 'vue'
import VueCookies from 'vue-cookies'
import App from './App.vue'
import {initI18n} from "../../utilities/i18n";

const els = document.body.getElementsByClassName('dynamic-form-vue')

for (let i = 0; i < els.length; i++) {
  const el = els[i];

  const i18n = initI18n()

  /* eslint-disable no-new */
  const app = createApp({
    render() {
      return h(App, {
            fields: el.attributes.fields.value || "",
            options: el.attributes.options.value || "",
            hasOptions: el.attributes.hasOptions.value || "",
            element: el.attributes.element.value || "",
            name: el.attributes.name.value || "",
      })
    },
  });
  app.use(VueCookies)
  app.use(i18n)
  app.mount(el)
}

