<template>
  <Button
    :label="label"
    :icon="icon"
    :icon-pos="iconPos"
    :loading="loading"
    :link="link"
    :severity="severity"
    :outlined="outlined"
  ></Button>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Button from "primevue/button";

export default defineComponent({
  name: "PtButton",
  // eslint-disable-next-line vue/no-reserved-component-names
  components: { Button },
  props: {
    label: {
      type: String,
      default: "",
    },
    icon: {
      type: String,
      default: undefined,
    },
    iconPos: {
      type: String,
      default: undefined,
      validator: (val: string) =>
        ["left", "top", "bottom", "right"].includes(val),
    },
    loading: {
      type: Boolean,
      default: false,
    },
    link: {
      type: Boolean,
      default: false,
    },
    severity: {
      type: String,
      default: null,
      validator: (val: string) =>
        ["success", "secondary", "info", "warning", "danger"].includes(val),
    },
    outlined: {
      type: Boolean,
      default: false,
    },
  },
});
</script>

<style lang="scss">
.p-button {
  background: var(--colors-primaryButtonOnLight);
  color: var(--colors-white);

  &:hover {
    background: var(--colors-primaryButtonHoverOnLight);
  }

  &:active {
    background: var(--colors-primaryButtonActiveOnLight);
  }
}

.p-button-danger {
  background: var(--colors-dangerButtonOnLight);
  color: var(--white);

  &:hover {
    background: var(--colors-dangerButtonHoverOnLight);
  }

  &:active {
    background: var(--colors-dangerButtonOnLight);
  }
}
</style>
