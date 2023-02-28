<template>
  <span>
    <details class="more-info" :class="extendedCss" v-if="extraDescription">
          <summary>
              <span :class="descriptionCss" >{{shortDescription}}</span>
              <span class="more-indicator-verbiage btn-link btn-xs">More &hellip; </span>
              <span class="less-indicator-verbiage btn-link btn-xs">Less </span>
          </summary>
          <div class="more-info-content">
            <markdown-it-vue class="markdown-body" :content="extraDescription"/>
          </div>
      </details>
      <span :class="basicCss" v-else>{{text}}</span>
  </span>
</template>
<script lang="ts">

import MarkdownItVue from 'markdown-it-vue'
import Vue from 'vue'

export default Vue.extend({
  name: 'ExtendedDescription',
  components: {MarkdownItVue},
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
