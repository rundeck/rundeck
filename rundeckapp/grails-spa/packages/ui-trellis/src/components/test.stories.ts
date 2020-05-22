import Vue from 'vue';
import MyButton from './plugins/PluginInfo.vue'

export default { title: 'PluginInfo' };

const pluginData = {
    desc: 'A sample plugin'
}

export const withDescription = () => ({
    components: { MyButton },
    template: '<MyButton :detail="detail" />',
    props: {
        detail: {
            default: () => ({
                ...pluginData
            })
        }
    }
})