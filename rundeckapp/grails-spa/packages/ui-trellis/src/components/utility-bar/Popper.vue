<template>
    <div ref="wrapper" style="display: none;">
        <div class="card popper" ref="popper" @click.stop>
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

    data: {
        parent: null as HTMLElement | null,
        instance: null as Instance | null
    },

    mounted() {
        const wrapper = this.$refs['wrapper'] as HTMLElement
        this.parent = wrapper.parentElement

        this.pop()
    },

    beforeDestroy() {
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

            console.log(popper.parentElement)

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
        }
    }
})
</script>

<style scoped lang="scss">
.popper {
    // padding: 5px;
    cursor: auto;
}
</style>
