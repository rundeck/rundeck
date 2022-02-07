import Vue, {VNode} from 'vue'

export default Vue.extend({
    name: 'rd-tab',
    props: {
        title: String,
        index: Number,
        active: {type: Boolean, default: false},
        keep: {type: Boolean, default: true}
    },
    render(h) {
        let component: VNode
        if (! this.$slots.default?.length)
            component = this.$createElement('div')
        else if (this.$slots.default.length > 1)
            component = this.$createElement('div', this.$slots.default)
        else
            component = this.$slots.default[0]

        return component
    }
})