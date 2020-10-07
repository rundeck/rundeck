import Vue from 'vue'

import {Steps} from 'ant-design-vue'


// require('ant-design-vue/es/steps/style/index.less')
require('../../../theme-next/components/steps/style/index.scss')

export default {
    title: 'Steps'
}

export const basic = () => (Vue.extend({
    components: {
        Steps,
        Step: Steps.Step
    },
    render(h) {
        return (
            <Steps current={1}><Steps.Step title="First"/><Steps.Step title="Second"/><Steps.Step title="Third"/></Steps>
        )
    }
}))

export const vertical = () => (Vue.extend({
    template: `
<a-steps direction="vertical" :current="1">
    <a-step title="Finished" description="This is a description." />
    <a-step title="In Progress" description="This is a description." />
    <a-step title="Waiting" description="This is a description." />
</a-steps>
    `,
    components: {
        'a-steps': Steps,
        'a-step': Steps.Step
    }
}))