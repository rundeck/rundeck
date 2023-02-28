<template>
  <div class="text-warning" v-if="errors  && errors.length>0">
    <ul>
      <ol v-for="err in errors">{{ err }}</ol>
    </ul>
    <i class="glyphicon glyphicon-warning-sign"></i>
  </div>
</template>
<script lang="ts">

import Vue from 'vue'
import Component from 'vue-class-component'
import {Prop} from 'vue-property-decorator'

@Component
export default class InlineValidationErrors extends Vue {
  @Prop({required: true})
  errors!: Array<string>
}
</script>