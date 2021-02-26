<template>
    <div v-if="open" ref="popper" style="display: fixed;">
        <slot/>
    </div>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'

import {createPopper, Instance} from '@popperjs/core'

export default Vue.extend({
    props: {
        open: {
            default: false
        }
    },

    data: {
        instance: null as Instance | null
    },

    mounted() {
        const popper = this.$refs['popper'] as HTMLElement
        const referenceElm = this.$el.parentElement
        
        // popper.parentNode?.removeChild(popper)
        // document.body.appendChild(popper)

        const instance = createPopper(referenceElm as HTMLElement, this.$refs['popper'] as HTMLElement, {
            modifiers: [
                {
                    name: 'offset',
                    options: {
                        offset: [0,8]
                    }
                }
            ]
        })
    },

    beforeDestroy() {
        console.log('Before destroy')
        if (this.instance)
            this.instance.destroy()
    }
})
</script>