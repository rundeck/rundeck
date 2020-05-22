import {withKnobs, object} from '@storybook/addon-knobs'

import MyButton from './plugins/PluginInfo.vue'

export default {
    title: 'PluginInfo',
    decorators: [withKnobs]
}

const pluginData = {
    title: 'Sample Plugin',
    desc: 'A sample plugin'
}

export const withDescription = () => ({
    components: { MyButton },
    template: '<MyButton :detail="detail" />',
    props: {
        detail: {
            default: object('detail', {
                ...pluginData
            })
        }
    }
})