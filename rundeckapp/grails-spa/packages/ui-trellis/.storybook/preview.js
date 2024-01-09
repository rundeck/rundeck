import { app } from '@storybook/vue3'
import * as uiv from 'uiv'

import '../src/library/theme/scss/app.scss'

let theme = process.env.STORYBOOK_THEME || 'theme'

app.use(uiv)

export const parameters = {
    actions: { argTypesRegex: "^on[A-Z].*" },
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/,
      },
    },
};
