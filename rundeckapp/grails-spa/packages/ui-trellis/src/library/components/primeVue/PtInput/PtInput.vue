<template>
  <div class="pt-input-wrapper">
    <label
      v-if="label"
      :for="inputId"
      class="text-heading--sm pt-input__label"
    >
      {{ label }}
    </label>

    <p v-if="helpText" class="text-body--sm pt-input__help">
      {{ helpText }}
    </p>

    <IconField v-if="leftIcon || rightIcon" class="pt-input__field">
      <InputIcon v-if="leftIcon" :class="leftIcon" />
      <InputText
        :id="inputId"
        v-model="internalValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :invalid="invalid"
        :name="name"
        :readonly="readonly"
        :maxlength="maxlength"
        :autocomplete="autocomplete"
        :type="type"
        :aria-label="ariaLabel"
        :aria-labelledby="ariaLabelledby"
        @focus="onFocus"
        @blur="onBlur"
        @input="onInput"
      />
      <InputIcon v-if="rightIcon" :class="rightIcon" />
    </IconField>

    <InputText
      v-else
      :id="inputId"
      v-model="internalValue"
      class="pt-input__field"
      :placeholder="placeholder"
      :disabled="disabled"
      :invalid="invalid"
      :name="name"
      :readonly="readonly"
      :maxlength="maxlength"
      :autocomplete="autocomplete"
      :type="type"
      :aria-label="ariaLabel"
      :aria-labelledby="ariaLabelledby"
      @focus="onFocus"
      @blur="onBlur"
      @input="onInput"
    />

    <p v-if="invalid && errorText" class="text-body--sm pt-input__error">
      {{ errorText }}
    </p>
  </div>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import InputText from "primevue/inputtext";
import IconField from "primevue/iconfield";
import InputIcon from "primevue/inputicon";

export default defineComponent({
  name: "PtInput",
  components: {
    InputText,
    IconField,
    InputIcon,
  },
  props: {
    modelValue: {
      type: [String, Number] as PropType<string | number | null>,
      default: "",
    },
    placeholder: {
      type: String,
      default: undefined,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    invalid: {
      type: Boolean,
      default: false,
    },
    name: {
      type: String,
      default: undefined,
    },
    label: {
      type: String,
      default: undefined,
    },
    helpText: {
      type: String,
      default: undefined,
    },
    errorText: {
      type: String,
      default: undefined,
    },
    leftIcon: {
      type: String,
      default: undefined,
    },
    rightIcon: {
      type: String,
      default: undefined,
    },
    inputId: {
      type: String,
      default: undefined,
    },
    ariaLabel: {
      type: String,
      default: undefined,
    },
    ariaLabelledby: {
      type: String,
      default: undefined,
    },
    readonly: {
      type: Boolean,
      default: false,
    },
    maxlength: {
      type: Number,
      default: undefined,
    },
    autocomplete: {
      type: String,
      default: undefined,
    },
    type: {
      type: String as PropType<string>,
      default: "text",
    },
  },
  emits: ["update:modelValue", "focus", "blur", "input"],
  computed: {
    internalValue: {
      get(): string | number | null {
        return this.modelValue;
      },
      set(value: string | number | null) {
        this.$emit("update:modelValue", value);
      },
    },
  },
  methods: {
    onFocus(event: FocusEvent) {
      this.$emit("focus", event);
    },
    onBlur(event: FocusEvent) {
      this.$emit("blur", event);
    },
    onInput(event: Event) {
      this.$emit("input", event);
    },
  },
});
</script>

<style lang="scss">
@import "../_form-inputs.scss";

.pt-input-wrapper {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.pt-input__label {
  margin-bottom: 0;
}

.pt-input__help {
  margin-top: 0;
  margin-bottom: var(--space-1);
}

.pt-input__error {
  margin-top: var(--space-1);
  margin-bottom: 0;
  color: var(--colors-red-500);
}

// IconField container styles
.p-iconfield {
  width: 100%;
  display: flex;
  align-items: center;
  position: relative;

  .p-inputtext {
    // When icons present, add padding for icon space (icon 14px + gap 10px + padding 10px = 34px)
    padding-left: calc(14px + 10px + 10px);
    padding-right: calc(14px + 10px + 10px);
  }

  // When only left icon
  &:has(.p-inputicon:first-child):not(:has(.p-inputicon:last-child)) {
    .p-inputtext {
      padding-right: 10px;
    }
  }

  // When only right icon
  &:has(.p-inputicon:last-child):not(:has(.p-inputicon:first-child)) {
    .p-inputtext {
      padding-left: 10px;
    }
  }
}

// InputIcon styles
.p-inputicon {
  position: absolute;
  top: 50%;
  transform: translateY(-25%);
  color: var(--colors-gray-500);
  width: 14px;
  height: 14px;
  font-size: 14px;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;

  // Left icon positioning
  &:first-child {
    left: 10px;
  }

  // Right icon positioning
  &:last-child:not(:first-child) {
    right: 10px;
    left: auto;
  }

  // Only one icon (right position)
  &:only-child {
    right: 10px;
    left: auto;
  }
}

// Disabled icon state
.p-iconfield:has(.p-inputtext:disabled) .p-inputicon {
  color: var(--colors-gray-300);
}

// InputText styles
.p-inputtext {
  width: 100%;
  @include form-input-base;
  padding: 10px;
  font-family: Inter, var(--fonts-body);
  font-size: 14px;
  font-weight: var(--fontWeights-regular);
  line-height: normal;
  color: var(--colors-gray-800);
  background: var(--colors-white);

  @include form-input-placeholder;

  // Hover state
  &:hover:not(:focus):not(:disabled):not(.p-invalid) {
    @include form-input-hover;
  }

  // Focus state
  &:focus {
    @include form-input-focus;
  }

  // Invalid state
  &.p-invalid {
    @include form-input-invalid;
  }

  // Disabled state
  &:disabled {
    @include form-input-disabled;
    background: var(--colors-gray-50);
    color: var(--colors-gray-500);
    cursor: not-allowed;
  }
}
</style>
