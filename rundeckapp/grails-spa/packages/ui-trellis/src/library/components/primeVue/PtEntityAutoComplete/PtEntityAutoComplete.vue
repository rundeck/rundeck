<template>
  <div class="pt-entity-autocomplete-wrapper">
    <label
      v-if="label"
      :for="inputId"
      class="text-heading--sm pt-form-label"
      data-testid="pt-entity-autocomplete-label"
    >
      {{ label }}
    </label>
    <p
      v-if="helpText || $slots.help"
      class="text-body--sm pt-entity-autocomplete__help"
      data-testid="pt-entity-autocomplete-help"
    >
      <slot name="help">{{ helpText }}</slot>
    </p>
    <div
      class="pt-entity-autocomplete__field"
      :class="{ 'pt-entity-autocomplete__field--icon': leftIcon }"
    >
      <i
        v-if="leftIcon"
        :class="leftIcon"
        aria-hidden="true"
        data-testid="pt-entity-autocomplete-icon"
      ></i>
      <AutoComplete
        ref="autoInput"
        :model-value="inputValue"
        :suggestions="visibleSuggestions"
        :option-label="optionLabel"
        :option-disabled="optionDisabled"
        :name="name"
        :input-id="inputId"
        :placeholder="placeholder"
        :invalid="invalid"
        :delay="delay"
        :auto-option-focus="true"
        :complete-on-focus="minChars === 0 && !readOnly"
        :show-empty-message="false"
        :pt="inputPassThrough"
        @update:model-value="onValueUpdate"
        @complete="onComplete"
        @option-select="onOptionSelect"
        @keydown.enter.prevent
      >
        <template #option="slotProps">
          <slot
            v-if="$slots.option"
            name="option"
            v-bind="slotProps"
            :query="inputValue"
            :selected="isSelected(slotProps.option)"
            :toggle="() => toggleOption(slotProps.option)"
          />
          <div v-else class="entity-autocomplete-option-content">
            <span class="entity-autocomplete-option-name">
              {{ labelOf(slotProps.option) }}
            </span>
            <span
              v-if="secondaryOf(slotProps.option)"
              class="entity-autocomplete-option-secondary"
            >
              {{ secondaryOf(slotProps.option) }}
            </span>
          </div>
        </template>
        <template v-if="$slots.header" #header>
          <slot name="header" />
        </template>
        <template v-if="$slots.footer" #footer>
          <slot name="footer" />
        </template>
        <template v-if="$slots.empty" #empty>
          <slot name="empty" />
        </template>
        <template v-if="$slots.loader" #loader="slotProps">
          <slot name="loader" v-bind="slotProps" />
        </template>
        <template v-if="$slots.content" #content="slotProps">
          <slot name="content" v-bind="slotProps" />
        </template>
        <template v-if="$slots.optiongroup" #optiongroup="slotProps">
          <slot name="optiongroup" v-bind="slotProps" />
        </template>
        <template v-if="$slots.dropdownicon" #dropdownicon>
          <slot name="dropdownicon" />
        </template>
        <template v-if="$slots.dropdown" #dropdown="slotProps">
          <slot name="dropdown" v-bind="slotProps" />
        </template>
        <template v-if="$slots.clearicon" #clearicon="slotProps">
          <slot name="clearicon" v-bind="slotProps" />
        </template>
      </AutoComplete>
    </div>
    <slot
      v-if="multiple && selectedValues.length > 0"
      name="selected-items"
      :items="selectedValues"
      :label="labelOf"
      :remove="removeOption"
    />
    <p
      v-if="invalid && errorText"
      class="text-body--sm pt-entity-autocomplete__error"
      data-testid="pt-entity-autocomplete-error"
    >
      {{ errorText }}
    </p>
  </div>
</template>

<script lang="ts">
import { defineComponent, type PropType } from "vue";
import AutoComplete, {
  AutoCompleteCompleteEvent,
  AutoCompleteOptionSelectEvent,
} from "primevue/autocomplete";
import type {
  EntityFieldResolver,
  EntitySuggestion,
} from "./PtEntityAutoCompleteTypes";

/**
 * Autocomplete over a list of entities (as opposed to context variables, which
 * `PtAutoComplete` handles).
 *
 * The difference that matters: this component keeps the whole suggestion object
 * and hands it back on `select`, so a caller can populate several fields from
 * one pick. It also matches against the entire input rather than the last
 * whitespace-delimited word, so entity names containing spaces work.
 *
 * Filtering is expected to happen server side: the `search` callback is invoked
 * with the current query (debounced by `delay`) and its result becomes the
 * suggestion list. Responses that arrive out of order are discarded, so only
 * the newest query is ever shown.
 *
 * Multiple selection is opt-in. In that mode the model contains selected
 * entity objects while the typed query remains internal, and consumers can
 * render both checkbox rows and selected items through scoped slots.
 */
export default defineComponent({
  name: "PtEntityAutoComplete",
  components: { AutoComplete },
  props: {
    modelValue: {
      type: [String, Array] as PropType<string | EntitySuggestion[]>,
      required: true,
    },
    /** Called with the current query to load suggestions. */
    search: {
      type: Function as PropType<
        (query: string) => Promise<EntitySuggestion[]>
      >,
      default: null,
    },
    /** Key holding the text shown in the input and the option row. */
    optionLabel: {
      type: [String, Function] as PropType<EntityFieldResolver<string>>,
      default: "name",
    },
    /** Optional key holding a dimmed secondary line on the option row. */
    optionSecondary: {
      type: [String, Function] as PropType<EntityFieldResolver<string>>,
      default: "",
    },
    /** Property or resolver identifying an option across refreshed results. */
    dataKey: {
      type: String,
      default: "",
    },
    /** Property or resolver indicating that an option cannot be selected. */
    optionDisabled: {
      type: [String, Function] as PropType<EntityFieldResolver<boolean>>,
      default: "disabled",
    },
    /** Enables an object-array model and reusable checkbox-style result rows. */
    multiple: {
      type: Boolean,
      default: false,
    },
    /** Reopens the suggestions after native keyboard or row selection. */
    keepOpenOnSelect: {
      type: Boolean,
      default: false,
    },
    /** Minimum characters before suggestions load; 0 also opens on focus. */
    minChars: {
      type: Number,
      default: 0,
    },
    /** Debounce applied before `search` runs, in milliseconds. */
    delay: {
      type: Number,
      default: 500,
    },
    name: {
      type: String,
      default: "",
    },
    inputId: {
      type: String,
      default: undefined,
    },
    /** `data-testid` applied to the inner input element. */
    inputTestid: {
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
    leftIcon: {
      type: String,
      default: undefined,
    },
    placeholder: {
      type: String,
      default: "",
    },
    invalid: {
      type: Boolean,
      default: false,
    },
    errorText: {
      type: String,
      default: undefined,
    },
    readOnly: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["update:modelValue", "select", "unselect"],
  data() {
    return {
      value: (typeof this.modelValue === "string"
        ? this.modelValue
        : "") as string,
      queryValue: "",
      items: [] as EntitySuggestion[],
      /** Sequence of the most recently issued search, to drop stale results. */
      requestSeq: 0,
      reopenTimer: null as ReturnType<typeof setTimeout> | null,
    };
  },
  computed: {
    /** Text model handed to PrimeVue; selections remain a separate array. */
    inputValue(): string {
      return this.multiple ? this.queryValue : this.value;
    },
    /** Selected entity objects when multiple mode is enabled. */
    selectedValues(): EntitySuggestion[] {
      return this.multiple && Array.isArray(this.modelValue)
        ? this.modelValue
        : [];
    },
    /**
     * Suggestions handed to PrimeVue, or `undefined` when there are none.
     *
     * PrimeVue treats an empty array as a completed search and keeps
     * re-opening the overlay, which makes it flicker while editing a value with
     * no matches; `undefined` keeps it closed. Same approach as `PtAutoComplete`.
     */
    visibleSuggestions(): EntitySuggestion[] | undefined {
      return this.items.length > 0 ? this.items : undefined;
    },
    /**
     * Attributes forwarded to the inner input element.
     *
     * PrimeVue exposes no `inputProps`, so `readonly` and the test id are
     * applied through its PassThrough API. `readonly` is used rather than
     * `disabled` so the value stays visible and selectable, matching the field
     * this component replaced; suggestions are suppressed separately in
     * `onComplete`.
     */
    inputPassThrough(): Record<string, unknown> {
      return {
        pcInputText: {
          root: {
            readonly: this.readOnly || undefined,
            "data-testid": this.inputTestid,
          },
        },
      };
    },
  },
  watch: {
    modelValue(newVal: string | EntitySuggestion[]) {
      if (!this.multiple && typeof newVal === "string") {
        this.value = newVal;
      }
    },
  },
  beforeUnmount() {
    if (this.reopenTimer) {
      clearTimeout(this.reopenTimer);
    }
  },
  methods: {
    /** Resolves a string or boolean field from an entity suggestion. */
    resolve<T>(
      option: EntitySuggestion,
      field: EntityFieldResolver<T>,
    ): T | undefined {
      if (typeof field === "function") {
        return field(option);
      }
      return option?.[field] as T | undefined;
    },
    /** Text shown for a suggestion, per `optionLabel`. */
    labelOf(option: EntitySuggestion): string {
      return String(this.resolve(option, this.optionLabel) ?? "");
    },

    /** Secondary text for a suggestion, or empty when not configured. */
    secondaryOf(option: EntitySuggestion): string {
      if (!this.optionSecondary) {
        return "";
      }
      return String(this.resolve(option, this.optionSecondary) ?? "");
    },

    /**
     * PrimeVue emits the suggestion object here once one is picked; normalize
     * back to a string so the caller's v-model stays a plain value.
     */
    onValueUpdate(val: string | EntitySuggestion): void {
      if (this.multiple) {
        if (typeof val === "string" || val == null) {
          this.queryValue = (val as string) ?? "";
        } else {
          this.restoreQueryValue();
        }
        return;
      }
      const text =
        typeof val === "string" || val == null
          ? ((val as string) ?? "")
          : this.labelOf(val);
      this.value = text;
      this.$emit("update:modelValue", text);
    },

    /** Restores the typed query inside PrimeVue after a native option selection. */
    restoreQueryValue(): void {
      (
        this.$refs.autoInput as
          { writeValue?: (value: string) => void } | undefined
      )?.writeValue?.(this.queryValue);
    },

    /** Load suggestions for the typed query, ignoring out-of-order results. */
    async onComplete(event: AutoCompleteCompleteEvent): Promise<void> {
      const query = event?.query ?? "";
      // Bump the sequence even for queries that are not sent, so a request
      // already in flight cannot repopulate the list after it was cleared.
      const seq = ++this.requestSeq;
      if (this.readOnly || !this.search || query.length < this.minChars) {
        this.items = [];
        return;
      }
      try {
        const results = await this.search(query);
        if (seq === this.requestSeq) {
          this.items = results || [];
        }
      } catch (e) {
        if (seq === this.requestSeq) {
          this.items = [];
        }
        console.error("Error loading autocomplete suggestions:", e);
      }
    },

    /** Re-emit the picked entity so the caller can read every field of it. */
    onOptionSelect(event: AutoCompleteOptionSelectEvent): void {
      const option = event.value as EntitySuggestion;
      if (!this.multiple) {
        this.$emit("select", option);
        return;
      }
      this.toggleOption(option);
      if (this.keepOpenOnSelect) {
        this.scheduleReopen();
      }
    },

    /** Whether an option is selected, using dataKey when supplied. */
    isSelected(option: EntitySuggestion): boolean {
      return this.selectedValues.some(
        (selected) => this.identityOf(selected) === this.identityOf(option),
      );
    },

    /** Adds or removes one option from the multiple-selection model. */
    toggleOption(option: EntitySuggestion): void {
      if (this.readOnly || !this.multiple || this.isOptionDisabled(option)) {
        return;
      }
      if (this.isSelected(option)) {
        this.removeOption(option);
        return;
      }
      this.$emit("update:modelValue", [...this.selectedValues, option]);
      this.$emit("select", option);
    },

    /** Removes an option from the multiple-selection model. */
    removeOption(option: EntitySuggestion): void {
      if (this.readOnly) {
        return;
      }
      const identity = this.identityOf(option);
      const selected = this.selectedValues.find(
        (item) => this.identityOf(item) === identity,
      );
      if (!selected) {
        return;
      }
      this.$emit(
        "update:modelValue",
        this.selectedValues.filter(
          (item) => this.identityOf(item) !== identity,
        ),
      );
      this.$emit("unselect", selected);
    },

    /** Stable identity for selection across asynchronous result refreshes. */
    identityOf(option: EntitySuggestion): unknown {
      return this.dataKey ? option?.[this.dataKey] : option;
    },

    /** Whether the configured disabled field marks an option unavailable. */
    isOptionDisabled(option: EntitySuggestion): boolean {
      return Boolean(this.resolve(option, this.optionDisabled));
    },

    /** Reopens the PrimeVue panel after its native select handler closes it. */
    scheduleReopen(): void {
      if (this.reopenTimer) {
        clearTimeout(this.reopenTimer);
      }
      void this.$nextTick(() => {
        this.reopenTimer = setTimeout(() => {
          this.reopenTimer = null;
          (this.$refs.autoInput as { show?: () => void } | undefined)?.show?.();
        }, 0);
      });
    },
  },
});
</script>

<!-- eslint-disable-next-line vue/enforce-style-attribute -- styles PrimeVue internals globally -->
<style lang="scss">
// Not scoped: PrimeVue teleports the overlay out of this component's subtree,
// so the option rows below cannot be reached by a scoped selector.
@import "../_autocomplete-overlay.scss";

.pt-entity-autocomplete-wrapper {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.pt-entity-autocomplete__help {
  margin: 0 0 var(--sizes-1);
  color: var(--colors-gray-800);
}

.pt-entity-autocomplete__field {
  position: relative;
  width: 100%;

  > .p-autocomplete {
    width: 100%;
  }

  > i {
    position: absolute;
    z-index: 1;
    top: 50%;
    left: var(--sizes-3);
    color: var(--colors-gray-600);
    transform: translateY(-50%);
    pointer-events: none;
  }

  &--icon .p-autocomplete-input {
    padding-left: var(--sizes-8);
  }
}

.pt-entity-autocomplete__error {
  margin-top: var(--space-1);
  margin-bottom: 0;
  color: var(--colors-red-500);
}

.p-autocomplete-option .entity-autocomplete-option-content {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: var(--sizes-2);
  width: 100%;
  min-width: 0;
}

.p-autocomplete-option .entity-autocomplete-option-name {
  font-weight: var(--fontWeights-regular);
  color: var(--colors-gray-800);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.p-autocomplete-option .entity-autocomplete-option-secondary {
  font-weight: var(--fontWeights-regular);
  color: var(--colors-gray-600);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
