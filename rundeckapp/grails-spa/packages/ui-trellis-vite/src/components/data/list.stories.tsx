import Vue from 'vue'
import { addons } from '@storybook/addons'


export default {
    title: 'Data/List'
}

export const ListHorizontal = () => (Vue.extend({
    render(h) {
        return (
            <div class="rs-container">
                <div class="list-horizontal">
                    <div class="list-horizontal--item">
                       <div>Key</div>
                        <div class="text-right">Value</div>
                    </div>
                    <div class="list-horizontal--item">
                        <div>Key</div>
                        <div class="text-right">Value</div>
                    </div>
                    <div class="list-horizontal--item">
                        <div>Key</div>
                        <div class="text-right">Value</div>
                    </div>
                    <div class="list-horizontal--item">
                        <div>Key</div>
                        <div class="text-right">Value</div>
                    </div>
                </div>
            </div>
        )
    }
}))
