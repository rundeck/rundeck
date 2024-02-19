<template>
  <div class="input-group">
    <input
      v-model="search"
      data-test="searchInput"
      type="search"
      name="search"
      :placeholder="placeholder"
      class="form-control input-sm"
      @focus="handleFocus"
      @keyup.enter="handleEnter"
      @blur="handleBlur"
    />
    <div class="input-group-addon">
      <i class="glyphicon glyphicon-search"></i>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "HomeSearchBar",
  props: {
    placeholder: {
      type: String,
      required: true,
    },
    name: {
      type: String,
      default: "search",
    },
    modelValue: {
      type: [String, Number],
      default: "",
    },
  },
  emits: ["update:modelValue", "onEnter", "onBlur", "onFocus"],
  data() {
    return {
      search: this.modelValue,
    };
  },
  watch: {
    modelValue(newVal) {
      if (newVal !== this.search) {
        this.search = newVal;
      }
    },
    search(newVal) {
      this.$emit("update:modelValue", newVal);
    },
  },
  methods: {
    handleEnter() {
      this.$emit("onEnter");
    },
    handleBlur() {
      this.$emit("onBlur");
    },
    handleFocus() {
      this.$emit("onFocus");
    },
  },
});
</script>
