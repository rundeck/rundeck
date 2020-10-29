let theme = process.env.STORYBOOK_THEME || 'theme'

if (theme == 'theme')
    require('../theme/scss/app')
if (theme == 'theme-next')
    require('../theme-next/scss/app')