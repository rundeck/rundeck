<template>
  <Button
    :label="label"
    :icon="icon"
    :icon-pos="iconPos"
    :loading="loading"
    :link="link"
    :severity="severity"
    :outlined="outlined"
    :text="text"
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
      default: "left",
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
    text: {
      type: Boolean,
      default: false,
    },
  },
});
</script>

<style lang="scss">
.p-button {
  background: var(--colors-primaryButtonOnLight);
  border-color: var(--colors-primaryButtonOnLight);
  color: var(--colors-white);

  &:hover {
    background: var(--colors-blue-600);
    border-color: var(--colors-blue-600);
  }

  &:focus-visible,
  &:focus {
    box-shadow: none;
    outline: 1px solid var(--colors-blue-600);
    outline-offset: 2px;
  }

  &:active {
    background: var(--colors-blue-700);
    border-color: var(--colors-blue-700);

    &:focus-visible,
    &:focus {
      outline: 1px solid var(--colors-blue-700);
    }
  }
}

.p-button-label {
  font-weight: var(--fontWeights-medium);
}

.p-button-danger {
  background: var(--colors-dangerButtonOnLight);
  border-color: var(--colors-dangerButtonOnLight);
  color: var(--colors-white);

  &:hover {
    background: var(--colors-dangerButtonHoverOnLight);
    border-color: var(--colors-dangerButtonHoverOnLight);
  }

  &:focus-visible,
  &:focus {
    outline: 1px solid var(--colors-dangerButtonOnLight);
  }

  &:active {
    background: var(--colors-dangerButtonActiveOnLight);
    border-color: var(--colors-dangerButtonActiveOnLight);

    &:focus-visible,
    &:focus {
      outline: 1px solid var(--colors-dangerButtonActiveOnLight);
    }
  }
}

.p-button-secondary {
  background-color: var(--colors-white);
  border-color: var(--colors-gray-800);
  color: var(--colors-gray-800);

  &:hover {
    background-color: var(--colors-gray-100);
    border-color: var(--colors-gray-800);
  }

  &:focus-visible,
  &:focus {
    outline: 1px solid var(--colors-gray-800);
  }

  &:active {
    background: var(--colors-gray-200);
    border-color: var(--colors-gray-800);

    &:focus-visible,
    &:focus {
      outline: 1px solid var(--colors-gray-800);
    }
  }
}

.p-button-text {
  background: transparent;
  border-color: transparent;
  color: var(--colors-blue-500);

  &:hover {
    background-color: var(--colors-gray-100);
    border-color: transparent;
    color: var(--colors-blue-600);
  }

  &:focus-visible,
  &:focus {
    outline: 1px solid var(--colors-gray-800);
  }

  &:active {
    background: var(--colors-gray-200);
    border-color: transparent;
    color: var(--colors-blue-700);

    &:focus-visible,
    &:focus {
      outline: 1px solid var(--colors-gray-800);
    }
  }

  &.p-button-danger {
    color: var(--colors-dangerButtonOnLight);

    &:hover {
      color: var(--colors-dangerButtonHoverOnLight);
    }

    &:active {
      color: var(--colors-dangerButtonActiveOnLight);
    }
  }

  &.p-button-secondary {
    color: var(--colors-gray-800);

    &:hover {
      color: var(--colors-gray-800);
    }

    &:active {
      color: var(--colors-gray-800);
    }
  }
}
</style>
