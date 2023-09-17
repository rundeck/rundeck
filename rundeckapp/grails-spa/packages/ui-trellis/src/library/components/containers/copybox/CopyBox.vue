<template>
    <div class="rd-copybox" :title="title" @click="handleClick">
        <div ref="content" class="rd-copybox__content" :class="{'rd-copybox__content--active': active}">
            {{content}}
        </div>
        <div class="rd-copybox__spacer" :class="{'rd-copybox__content--active': active}"></div>
        <div class="rd-copybox__icon">
            <i class="fas fa-clipboard"></i>
        </div>
        <span v-if="active" class="rd-copybox__success" :class="{'rd-copybox__success--active': active}">Copied to clipboard!</span>
        <span v-if="hasFailed" class="rd-copybox__success error" :class="{'rd-copybox__success--active': hasFailed}">Copy failed!</span>
    </div>
</template>

<script lang="ts">
import {defineComponent, VNode} from 'vue'

import {CopyToClipboard} from '../../../utilities/Clipboard'

export default defineComponent({
    name: 'rd-copybox',
    props: {
        content: {
          type: String,
          required: true,
        }
    },
    data() { return {
        active: false,
        hasFailed: false,
    }},
    computed: {
        title() {
            return this.content
        }
    },
    methods: {
        async handleClick() {
            const content = (<HTMLElement>this.$refs['content'])

            const range = document.createRange()
            range.selectNode(content)

            const sel = window.getSelection()
            sel?.removeAllRanges()

            try {
              await CopyToClipboard(this.content)
              this.active = true
              setTimeout(() => this.active = false, 400)
              setTimeout(() => {
                sel?.addRange(range)
              }, 700)
            } catch (e) {
              this.hasFailed = true
              setTimeout(() => this.hasFailed = false, 400)
              console.error(e)
            }
        }
    }
})
</script>

<style scoped lang="scss">
.rd-copybox {
    position: relative;
    background-color: var(--background-color-lvl2);
    border-radius: 5px;
    border-style: solid;
    border-width: 0.1em;
    border-color: var(--border-color);
    display: flex;
    cursor: pointer;
}

.rd-copybox__content {
    border-top-left-radius: 5px;
    border-bottom-left-radius: 5px;
    font-weight: 600;
}

.rd-copybox__content, .rd-copybox__spacer {
    padding: 10px;
    overflow: hidden;

    transition: all 200ms ease-in-out;
    transition-property: color, background-color;

    &--active {
        background-color: slategray;
        color: forestgreen;
    }
}

.rd-copybox__spacer {
    flex-grow: 1;
}

.rd-copybox__icon {
    padding: 10px;
    border-left: inherit;
    display: flex;
    align-content: center;
    align-items: center;
}

.rd-copybox__success {
    position: absolute;
    top: 10px;
    left: 10px;

    color: var(--font-fill-color);
    font-weight: 800;
    opacity: 0;

    transition: opacity 200ms ease-in-out;

    &.error {
      color: var(--danger-color);
    }

    &--active {
        opacity: 1;
    }
}

</style>