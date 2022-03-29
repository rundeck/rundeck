import 'virtual:windi-base.css'
import 'virtual:windi-components.css'
// windicss devtools support (dev only)
import 'virtual:windi-devtools'
// windicss utilities should be the last style import
import 'virtual:windi-utilities.css'
// your custom styles here
import './core/assets/styles/main.css'

import App from './App.vue'
// register vue composition api globally
import { ViteSSG } from 'vite-ssg'
import generatedRoutes from 'virtual:generated-pages'
// windicss layers
import { setupLayouts } from 'virtual:generated-layouts'

const routes = setupLayouts(generatedRoutes)

// https://github.com/antfu/vite-ssg
export const createApp = ViteSSG(App, { routes, base: import.meta.env.BASE_URL }, (ctx) => {
  // install all modules under `**/modules/`
  Object.values(import.meta.globEager('./**/modules/*.ts')).forEach((i) => i.install?.(ctx))
})
