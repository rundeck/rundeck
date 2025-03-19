<template>
  <main>
    <InputText
      ref="input"
      v-model="inputValue"
      type="text"
      fluid
      @input="handleInput"
      @keydown="handleKeyDown"
    />
    <p>Auto complete in tooltip, custom</p>
    <Popover ref="popover" class="popover-class">
      <div>
        <ul class="list-none">
          <li
            v-for="(suggestion, index) in filteredSuggestions"
            :key="suggestion"
            :class="{
              'bg-selected': index === highlightedIndex && isPopoverVisible,
            }"
            @click="selectSuggestion(suggestion)"
          >
            <span>{{ suggestion }}</span>
          </li>
        </ul>
      </div>
    </Popover>
  </main>
</template>

<script lang="ts">
import { defineComponent, computed, ref, watch } from "vue";
import InputText from "primevue/inputtext";
import Popover from "primevue/popover";
import { contextVariables } from "./contextVariables";

export default defineComponent({
  name: "PtAutoComplete",
  components: { InputText, Popover },
  props: {
    modelValue: {
      type: String,
      required: true,
    },
  },
  emits: ["update:modelValue"],
  setup(props, { emit }) {
    const inputValue = ref(props.modelValue);
    const highlightedIndex = ref(-1);
    const isPopoverVisible = ref(false);
    const popover = ref(null);
    const suggestions = ref(contextVariables);

    const filteredSuggestions = computed(() => {
      const words = inputValue.value.trim().split(/\s+/);
      const lastWord = words[words.length - 1].toLowerCase();

      return lastWord
        ? suggestions.value.filter((s) => s.toLowerCase().startsWith(lastWord))
        : [];
    });

    watch(
      () => props.modelValue,
      (newVal) => {
        inputValue.value = newVal;
      },
    );

    const handleInput = (event) => {
      emit("update:modelValue", inputValue.value);

      if (filteredSuggestions.value.length) {
        isPopoverVisible.value = true;
        highlightedIndex.value = 0;
        popover.value.show(event);
      } else {
        isPopoverVisible.value = false;
        highlightedIndex.value = -1;
        popover.value.hide();
      }
    };

    const selectSuggestion = (suggestion) => {
      const words = inputValue.value.trim().split(/\s+/);
      words[words.length - 1] = suggestion;
      inputValue.value = words.join(" ");
      emit("update:modelValue", inputValue.value);

      // Inline popover close
      isPopoverVisible.value = false;
      highlightedIndex.value = -1;
      popover.value.hide();
    };

    const handleKeyDown = (event) => {
      if (!isPopoverVisible.value || !filteredSuggestions.value.length) return;

      if (event.key === "ArrowDown") {
        event.preventDefault();
        highlightedIndex.value =
          (highlightedIndex.value + 1) % filteredSuggestions.value.length;
      } else if (event.key === "ArrowUp") {
        event.preventDefault();
        highlightedIndex.value =
          (highlightedIndex.value - 1 + filteredSuggestions.value.length) %
          filteredSuggestions.value.length;
      } else if (event.key === "Enter") {
        event.preventDefault();
        if (highlightedIndex.value >= 0) {
          selectSuggestion(filteredSuggestions.value[highlightedIndex.value]);
        }
      }
    };

    return {
      inputValue,
      filteredSuggestions,
      highlightedIndex,
      isPopoverVisible,
      popover,
      handleInput,
      handleKeyDown,
      selectSuggestion,
    };
  },
});
</script>

<style lang="scss">
.bg-selected {
  background-color: #35353f;
}
ul {
  padding-left: 0;
  margin-bottom: 0;
}
li {
  padding: 5px 10px;
}
.popover-class {
  z-index: 5000 !important;
}
.p-popover-content {
  min-width: 200px;
  padding-left: 0 !important;
  padding-right: 0 !important;
}
</style>
