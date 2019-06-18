<!--
- Copyright 2019 Rundeck, Inc. (http://rundeck.com)
-
- Licensed under the Apache License, Version 2.0 (the "License");
- you may not use this file except in compliance with the License.
- You may obtain a copy of the License at
-
-     http://www.apache.org/licenses/LICENSE-2.0
-
- Unless required by applicable law or agreed to in writing, software
- distributed under the License is distributed on an "AS IS" BASIS,
- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
- See the License for the specific language governing permissions and
- limitations under the License.
-->

<template>
<span v-if="count>0" @click="clickAction">
    <slot :count="count">{{count}}</slot>
</span>
</template>

<script lang="ts">

import Vue from 'vue'

export default Vue.extend({
  name: 'ActivityRunningIndicator',
  components: {},
  props: [
    'eventBus',
    'displayMode'
  ],
  data() {
    return {
      count: 0
    }
  },
  methods: {
    updateNowrunning(count: number) {
      this.count = count
    },
    clickAction() {
      this.eventBus.$emit('activity-nowrunning-click-action')
    }
  },
  mounted(): void {
    this.eventBus.$on('activity-nowrunning-count', this.updateNowrunning)
  }
})
</script>
