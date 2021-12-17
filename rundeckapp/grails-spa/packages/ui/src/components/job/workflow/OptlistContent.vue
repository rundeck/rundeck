<template>
  <div id="optionList">
    <li v-for="optionsel in options" id="optli_${index}"
      :key="optionsel+index"  :class="['el-collapse-item', 'optEntry', 'draggableitem', 'droppableitem',
      {highlight: 'dohighlight'}]"
      data-opt-index=index data-opt-name=optionsel.name>
      <optlistitem-content :option="optionsel" :editMode="editMode" :optIndex="index" :optCount="options.length()" />
    </li>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import OptlistitemContent from "./OptlistitemContent.vue";

export default Vue.extend({
  name: 'OptlistContent',
  components: {
    OptlistitemContent
  },
  props: {
    editMode: Boolean,
    options: Object
  },
  methods: {},
  mounted() {
    },
  data() {
    return {
    }
  }
})
</script>

<style lang="scss">
.dohighlight {
  animation: blink 2s ease;
}

@keyframes blink {
  0% {
    background-color: yellow;
  }
  100% {
    background-color: white;
  }
}

#optionList {
  li:nth-child(odd) {
    .info.time {
      color: #808080;
    }

    .data {
      background: #f0f0f0;
    }
  }
}
</style>
