<template>
  <div v-show="isActive">
    <slot></slot>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';

@Component
export default class Tab extends Vue {
  @Prop({ default: 'Tab' })
  title!: string;

  isActive = true
}
</script>