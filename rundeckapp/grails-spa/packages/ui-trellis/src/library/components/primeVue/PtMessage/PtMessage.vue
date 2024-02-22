<template>
  <Message :severity="variant" :pt="passThroughClasses()">
    <slot />
  </Message>
</template>

<script lang="ts">
import {defineComponent} from 'vue'
import Message from 'primevue/message';


export default defineComponent({
  name: "PtMessage",
  components: {
    Message
  },
  props: {
    variant: {
      type: String,
      default: 'success'
    }
  },
  methods: {
    passThroughClasses() {
      return {
        root: () => ({
          class: `pt-message variant-${this.variant}`
        }),
        wrapper: {
          class: `pt-message_wrapper`,
        },
        icon: {
          class: `pt-message_icon`
        },
        button: {
          class: `pt-message_button`
        }
      }
    }
  }

})
</script>

<style lang="scss">
  .p-message-text {
    flex-grow: 1;
    color: var(--colors-secondaryBackgroundOnDark);
  }

  .variant-success {
    color: var(--colors-green-500);
    background-color: var(--colors-green-100);
    border-color:  var(--colors-green-500);
  }

  .variant-info {
    color: var(--colors-blue-500);
    background-color: var(--colors-blue-100);
    border-color: var(--colors-blue-500);
  }

  .variant-warn {
    color: var(--colors-yellow-500);
    background-color: var(--colors-yellow-100);
    border-color: var(--colors-yellow-500);
  }

  .variant-error {
    color: var(--colors-red-500);
    background-color: var(--colors-red-100);
    border-color: var(--colors-red-500);
  }

</style>