<template>
    <details :class="detailsCss">
      <summary  :class="linkCss">
        <slot name="link" :open="open">
          <slot name="label">More...</slot>
          <span class="more-indicator-verbiage more-info-icon"><slot name="more"><i class="glyphicon glyphicon-chevron-right"></i></slot></span>
          <span class="less-indicator-verbiage more-info-icon"><slot name="less"><i class="glyphicon glyphicon-chevron-down"></i></slot></span>
        </slot>
      </summary>

      <slot></slot>

    </details>
</template>
<script lang="ts">
import { defineComponent} from 'vue'
export default defineComponent({
    name:"Expandable",
    props: {
        options: {
            type: Object as () => { open: boolean, iconOpen: string, iconClosed: string, linkCss: string, css: string },
            default: {
                open: false,
                linkCss: '',
                css: ''
            }
        }
    },
    data() {
        return {
            open: this.options.open,
            linkCss: this.options.linkCss,
            css: this.options.css
        }
    },
    computed: {
        detailsCss() {
            return ['more-info','details-reset',this.css]
        }
    }
})

</script>
