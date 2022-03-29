import path from 'path'
import { startDevServer } from '@cypress/vite-dev-server'

export default (on: Cypress.PluginEvents, config: Cypress.PluginConfigOptions) => {
  on('dev-server:start', async (options) =>
    startDevServer({
      options,
      viteConfig: {
        configFile: path.resolve(__dirname, '..', '..', '..', '..', 'vite.config.ts'),
      },
    })
  )

  return config
}
