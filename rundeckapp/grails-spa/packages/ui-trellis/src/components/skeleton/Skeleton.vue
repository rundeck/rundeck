<script lang="ts">
import Vue, {CreateElement, VNode} from 'vue'

export default Vue.extend({
    props: {
        loading: { default: false},
        type: {default: 'list'}
    },

    render(h): VNode {
        if (this.loading)
            return this.skeleton(h)
        else {
            return this.slot(h)
        }
    },

    methods: {
        /**
         * Use a render function so we can avoid an unecessary wrapper element
         * if the slot is a single VNode.
         */
        slot(h: CreateElement): VNode {
            const slot = this.$slots.default

            if (!slot)
                return h('div')
            else if (Array.isArray(slot))
                if (slot.length == 1)
                    return slot[0]
                else
                    return h('div', slot)
            else
                return slot
        },
        skeleton(h: CreateElement): VNode {
            return h('div', {
                attrs: {
                    'role': 'alert',
                    'aria-busy': true,
                    'aria-live': 'polite',
                },
                class: ['skeleton', `skeleton--${this.type}`]
            })
        }
    }
})
</script>

<style lang="scss">

</style>

<style scoped lang="scss">

:root {
    --skel-color: #dedede;
}

.skeleton--list {
    height: 100%;
    background-repeat: repeat-y;
    background-image:
        linear-gradient( 100deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.5) 50%, rgba(255, 255, 255, 0) 80% ),
        linear-gradient(var(--skel-color) 20px, transparent 0 );

    background-size:
        50px 1.5em, /* highlight */
        100% 30px;

    background-position:
        -50px 0, /* highlight */
        0 0;

    animation: shine 1s infinite;

    @keyframes shine {
        to {
            background-position:
                120% 0, /* move highlight to right */
                0 0;
        }
    }
}
</style>