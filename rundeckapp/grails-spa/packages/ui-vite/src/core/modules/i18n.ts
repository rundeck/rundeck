import type { UserModule } from '~/types'
import { createI18n } from 'vue-i18n'

// Import i18n resources
// https://vitejs.dev/guide/features.html#glob-import
//
// Don't need this? Try vitesse-lite: https://github.com/antfu/vitesse-lite
const messages = Object.fromEntries(
  Object.entries(import.meta.globEager('../../../locales/*.y(a)?ml')).map(([key, value]) => {
    const yaml = key.endsWith('.yaml')
    return [key.slice(17, yaml ? -5 : -4), value.default]
  })
)

export const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages,
})

export const install: UserModule = ({ app }) => {
  app.use(i18n)
}
