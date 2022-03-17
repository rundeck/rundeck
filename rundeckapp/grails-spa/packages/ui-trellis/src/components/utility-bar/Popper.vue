<template>
    <div ref="wrapper" style="display: none;">
        <div class="popper" ref="popper" @click.stop>
            <slot/>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'

import {createPopper, Instance} from '@popperjs/core'

export default Vue.extend({
    props: {
        open: {
            default: false
        }
    },

    data(){
        return {
            parent: null as HTMLElement | null,
            instance: null as Instance | null
        }
    },

    mounted() {
        const wrapper = this.$refs['wrapper'] as HTMLElement
        this.parent = wrapper.parentElement

        document.addEventListener('click', this.closeListener)

        this.pop()
    },

    beforeDestroy() {
        document.removeEventListener('click', this.closeListener)
        const popper = this.$refs['popper'] as HTMLElement
        document.body.removeChild(popper)
        if (this.instance)
            this.instance.destroy()
    },

    methods: {
        pop() {
            const popper = this.$refs['popper'] as HTMLElement

            popper.parentNode?.removeChild(popper)
            document.body.appendChild(popper)

            this.instance = createPopper(this.parent!, popper, {
                modifiers: [
                    {
                        name: 'offset',
                        options: {
                            offset: [0,10]
                        }
                    },
                    {
                        name: 'preventOverflow',
                        options: {
                            padding: 10
                        }
                    }
                ]
            })
        },
        closeListener(click: MouseEvent) {
            const clickNode = click.target as Node
            const popper = this.$refs['popper'] as HTMLElement

            if (!this.parent?.contains(clickNode) && !popper.contains(clickNode))
                this.$emit('close')
        }
    }
})
</script>

<style scoped lang="scss">
.popper {
    z-index: 1000;
    cursor: auto;
}
</style>
