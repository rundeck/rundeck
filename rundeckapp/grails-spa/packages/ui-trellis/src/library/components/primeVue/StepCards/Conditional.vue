<template>
  <div class="conditional">
    <p class="conditional--if">
      If
      <Tag class="tag-code" value="node.os-family"></Tag>
      <span class="conditional--equality">equals</span>
      <span class="bold">Linux</span>
    </p>
    <div class="conditional--do">
      <div class="conditional--connector"></div>
      <slot></slot>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Tag from "primevue/tag";

export default defineComponent({
  name: "Conditional",
  components: {
    Tag,
  },
  props: {},
});
</script>

<style lang="scss">
.conditional {
  counter-reset: step;
  width: 100%;

  &--if {
    align-items: baseline;
    display: flex;
    gap: 5px;
    margin: 0;
  }

  &--do {
    //padding-left: var(--sizes-1);
    //padding-top: 15px;
    padding-left: 30px;
    position: relative;
  }

  &--connector {
    border-bottom: 1px solid var(--colors-gray-400);
    border-left: 1px solid var(--colors-gray-400);
    border-radius: 0 var(--radii-lg) 0;
    position: absolute;
    top: 3px;
    left: 3px;
    height: 25px;
    //width: 25px;
    width: 23px;
  }

  .bold {
    font-weight: var(--fontWeights-semibold);
  }

  .p-accordionheader {
    background: none;
    border-left: none;
    border-top: none;
    border-right: none;
    border-radius: 0;
    font-weight: var(--fontWeights-normal);
    gap: var(--sizes-2);
    justify-content: flex-start;
    margin-left: var(--sizes-8);
    padding-left: 0;
    z-index: 1;

    &.nested {
      padding-right: 0;

      &:last-child {
        padding-bottom: 0;
        border: none;
      }
    }

    &.inner-child {
      padding-bottom: 0;
      border: none;
    }

    .conditional {
      margin-right: -1rem;
      margin-top: 1px;
    }

    &::before {
      align-items: center;
      background: var(--colors-cardNumber);
      border-radius: 50%;
      counter-increment: step;
      content: counter(step);
      display: flex;
      flex-shrink: 0;
      font-weight: var(--fontWeights-normal) !important;
      height: 1.5rem;
      justify-content: center;
      left: 0;
      position: absolute;
      top: 1.125rem;
      width: 1.5rem;
    }
  }

  .p-accordioncontent {
    margin-left: var(--sizes-8);

    &-content {
      border-radius: 0 0 5px 5px;
    }
  }

  .p-accordion > .p-accordionpanel {
    position: relative;

    &::before {
      display: block;
      width: 1px;
      height: 100%;
      background: var(--colors-gray-400);
      content: "";
      position: absolute;
      bottom: 0;
      left: 10px;
      z-index: 0;
    }

    &:nth-child(1)::before {
      top: 25px;
    }

    &:last-child::before {
      top: 0;
      height: 1.125rem;
    }

    &.align-start {
      &:last-child::before {
        height: 1.125rem;
      }

      .p-accordionheader {
        align-items: flex-start;
      }
    }

    &:only-child::before {
      display: none;
    }
  }
}
</style>
