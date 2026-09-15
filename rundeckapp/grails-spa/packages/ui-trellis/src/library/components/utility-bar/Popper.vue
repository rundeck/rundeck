<template>
  <div ref="wrapper" class="popper-wrapper">
    <div ref="popper" class="popper" data-testid="popper-content" @click.stop>
      <slot />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

import { createPopper, Instance } from "@popperjs/core";

export default defineComponent({
  props: {
    open: {
      default: false,
    },
  },
  emits: ["close"],
  data() {
    return {
      parent: null as HTMLElement | null,
      instance: null as Instance | null,
    };
  },

  mounted() {
    const wrapper = this.$refs["wrapper"] as HTMLElement;
    this.parent = wrapper.parentElement;

    document.addEventListener("click", this.closeListener);

    this.pop();
  },

  beforeUnmount() {
    document.removeEventListener("click", this.closeListener);
    const popper = this.$refs["popper"] as HTMLElement;
    document.body.removeChild(popper);
    if (this.instance) this.instance.destroy();
  },

  methods: {
    pop() {
      const popper = this.$refs["popper"] as HTMLElement;

      popper.parentNode?.removeChild(popper);
      document.body.appendChild(popper);

      this.instance = createPopper(this.parent!, popper, {
        modifiers: [
          {
            name: "offset",
            options: {
              offset: [0, 10],
            },
          },
          {
            name: "preventOverflow",
            options: {
              padding: 10,
            },
          },
        ],
      });
    },
    closeListener(click: MouseEvent) {
      const clickNode = click.target as Node;
      const popper = this.$refs["popper"] as HTMLElement;

      if (!this.parent?.contains(clickNode) && !popper.contains(clickNode))
        this.$emit("close");
    },
  },
});
</script>

<style scoped lang="scss">
.popper-wrapper {
  display: none;
}

.popper {
  // Above Next UI's fixed sections (z-index 1032), below the modal backdrop (1040).
  z-index: 1035;
  cursor: auto;
}
</style>
