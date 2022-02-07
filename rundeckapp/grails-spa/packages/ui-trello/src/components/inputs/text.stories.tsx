import Vue from 'vue'
import { addons } from '@storybook/addons'
import { CHANGE, array, object, boolean, withKnobs, select } from '@storybook/addon-knobs'


export default {
    title: 'Inputs/Text',
    decorators: [withKnobs({ disableDebounce: true })]
}

export const InputTextHorizontal = () => (Vue.extend({
    render(h) {
        return (
            <div class="input-text--horizontal">
                <div>
                    <label>Name</label>
                    <p class="description">Give the runner an easily identifiable name</p>
                </div>
                <div class="col-span-2 flex-v-center form-group">
                    <input type="text" class="form-control"/>
                </div>
            </div>
        )
    }
}))
