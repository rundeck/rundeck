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
    <AutoComplete
      ref="autoInput"
      :model-value="value"
      :suggestions="items"
      :option-label="optionLabel"
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
        <div class="entity-autocomplete-option-content">
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
    </AutoComplete>
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
import type { EntitySuggestion } from "./PtEntityAutoCompleteTypes";

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
 */
export default defineComponent({
  name: "PtEntityAutoComplete",
  components: { AutoComplete },
  props: {
    modelValue: {
      type: String,
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
      type: String,
      default: "name",
    },
    /** Optional key holding a dimmed secondary line on the option row. */
    optionSecondary: {
      type: String,
      default: "",
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
  emits: ["update:modelValue", "select"],
  data() {
    return {
      value: this.modelValue as string,
      items: [] as EntitySuggestion[],
      /** Sequence of the most recently issued search, to drop stale results. */
      requestSeq: 0,
    };
  },
  computed: {
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
    modelValue(newVal: string) {
      this.value = newVal;
    },
  },
  methods: {
    /** Text shown for a suggestion, per `optionLabel`. */
    labelOf(option: EntitySuggestion): string {
      return String(option?.[this.optionLabel] ?? "");
    },

    /** Secondary text for a suggestion, or empty when not configured. */
    secondaryOf(option: EntitySuggestion): string {
      if (!this.optionSecondary) {
        return "";
      }
      return String(option?.[this.optionSecondary] ?? "");
    },

    /**
     * PrimeVue emits the suggestion object here once one is picked; normalize
     * back to a string so the caller's v-model stays a plain value.
     */
    onValueUpdate(val: string | EntitySuggestion): void {
      const text =
        typeof val === "string" || val == null
          ? ((val as string) ?? "")
          : this.labelOf(val);
      this.value = text;
      this.$emit("update:modelValue", text);
    },

    /** Load suggestions for the typed query, ignoring out-of-order results. */
    async onComplete(event: AutoCompleteCompleteEvent): Promise<void> {
      const query = event?.query ?? "";
      if (this.readOnly || !this.search || query.length < this.minChars) {
        this.items = [];
        return;
      }
      const seq = ++this.requestSeq;
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
      this.$emit("select", event.value);
    },
  },
});
</script>

<style lang="scss">
// Not scoped: PrimeVue teleports the overlay out of this component's subtree,
// so the option rows below cannot be reached by a scoped selector.
@import "../_autocomplete-overlay.scss";

.pt-entity-autocomplete-wrapper {
  display: flex;
  flex-direction: column;
  width: 100%;
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
