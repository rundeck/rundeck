<template>
  <div ref="wrapper" data-testid="popper-wrapper" style="display: none">
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
.popper {
  // Must stay above the fixed layout sections used in Next UI
  // (#section-navbar/#section-utility/#section-header, which use
  // $zindex-navbar-fixed [1030] and $zindex-navbar-fixed + 2 [1032]
  // in _view.scss), or the left nav renders on top of this popup.
  //
  // Can't reference $zindex-navbar-fixed directly here: _variables.scss
  // depends on other partials (e.g. $cyan-500 from a colors file) that are
  // only in scope when the whole theme is assembled via app.scss - importing
  // it standalone into this scoped SFC style block fails to compile
  // ("Undefined variable $cyan-500"). Keep this a hardcoded literal.
  z-index: 1035;
  cursor: auto;
}
</style>
