import Vue from 'vue'
import { addons } from '@storybook/addons'
import { CHANGE, array, object, boolean, withKnobs, select } from '@storybook/addon-knobs'


export default {
    title: 'UI/Example'
}


export const Example = () => (Vue.extend({
    render(h) {
        return (
            <div>
                This is an example in UI
            </div>
        )
    }
}))
