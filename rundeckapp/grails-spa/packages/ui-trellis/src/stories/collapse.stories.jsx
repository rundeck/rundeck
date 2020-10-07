import Vue from 'vue'

import Card from '../components/card'
import {Collapse} from 'ant-design-vue'

const {Panel} = Collapse

import '../../theme-next/components/card/style/index.scss'
import '../../theme-next/components/collapse/style/index.scss'

export default {
    title: 'Atom/Collapse'
}


export const basic = () => (Vue.extend({
    render(h) {
        return (
            <Card bodyStyle='padding: 0' headStyle='padding: 0' style='width: 300px; border-radius: 4px 4px 4px 4px; box-shadow: 0px 4px 14px 0 rgba(0, 0, 0, 0.11);'>
                <Collapse style='margin: 4px'>
                    <Panel>
                        Foo
                    </Panel>
                </Collapse>
            </Card>
        )
    }
}))