<template>
  <span>
    <details class="more-info" :class="extendedCss" v-if="extraDescription">
          <summary>
              <span :class="descriptionCss" >{{shortDescription}}</span>
              <span class="more-indicator-verbiage btn-link btn-xs">More &hellip; </span>
              <span class="less-indicator-verbiage btn-link btn-xs">Less </span>
          </summary>
          <div class="more-info-content">
            <VMarkdownView class="markdown-body" mode="light" :content="extraDescription"/>
          </div>
      </details>
      <span :class="basicCss" v-else>{{text}}</span>
  </span>
</template>
<script lang="ts">

import {VMarkdownView} from 'vue3-markdown'
import {defineComponent} from 'vue'

export default defineComponent({
  name: 'ExtendedDescription',
  components: {VMarkdownView},
  computed:{
    shortDescription() :string{
      const desc = this.text
      if (desc && desc.indexOf("\n") > 0) {
        return desc.substring(0, desc.indexOf("\n"));
      }
      return desc;
    },
    extraDescription() :string|null{
      const desc = this.text
      if (desc && desc.indexOf("\n") > 0) {
        return desc.substring(desc.indexOf("\n") + 1);
      }
      return null;
    }
  },
  props: {
    'text': {
      type: String,
      required: false,
      default: ''
    },
    'extendedCss': {
      type: String,
      required: false,
      default: ''
    },
    'descriptionCss': {
      type: String,
      required: false,
      default: ''
    },
    'basicCss': {
      type: String,
      required: false,
      default: ''
    }
  }
})

</script>
<style>
@import '~vue3-markdown/dist/style.css';
</style>
