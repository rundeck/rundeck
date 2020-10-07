import Vue from 'vue'

import {Card} from 'ant-design-vue'

import '../../theme-next/components/card/style/index.scss'

export default {
    title: 'Atom/Card'
}


export const basic = () => (Vue.extend({
    render(h) {
        return (
            <div style='padding: 10px'>
                <Card headStyle='padding: 0' style='width: 300px; border-radius: 4px 4px 4px 4px; box-shadow: 0px 4px 14px 0 rgba(0, 0, 0, 0.11);'><div slot='title'>Foo</div>Bar</Card>
            </div>
        )
    }
}))