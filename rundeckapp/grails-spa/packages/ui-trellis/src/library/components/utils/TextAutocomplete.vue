<template>
  <Dropdown
    ref="dropdown"
    v-model="open"
    tag="section"
    :position-element="inputEl"
  >
    <template #dropdown>
      <slot
        name="item"
        :items="items"
        :select="selectItem"
        :highlight="highlight"
      >
        <li v-for="(item, index) in items" :key="index">
          <a href="#" @click.prevent="selectItem(item)">
            <span v-html="highlight(item)"></span>
          </a>
        </li>
      </slot>
      <slot v-if="!items || items.length === 0" name="empty" />
    </template>
  </Dropdown>
</template>

<script lang="ts">
import { Dropdown } from "uiv";

import { defineComponent, PropType } from "vue";

type AutocompleteItem = string | Record<string, unknown>;
type AutocompleteTarget = string | Element | { $el: Element };

export default defineComponent({
  name: "TextAutocomplete",
  extends: Dropdown,
  props: {
    modelValue: {
      type: String,
      required: true,
    },
    data: Array as PropType<AutocompleteItem[]>,
    itemKey: String,
    limit: {
      type: Number,
      default: 10,
    },
    target: {
      type: [String, Object] as PropType<AutocompleteTarget>,
      required: true,
    },
    autocompleteKey: {
      type: String,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      inputEl: null as HTMLInputElement | null,
      items: [] as AutocompleteItem[],
      open: false,
      selectionStart: 0,
      currentSelection: 0,
      selectedValue: "",
    };
  },
  computed: {
    value: {
      get() {
        return this.modelValue;
      },
      set(val: string) {
        this.$emit("update:modelValue", val);
      },
    },
  },
  mounted() {
    this.initInputElByTarget(this.target);
    this.initListeners();
  },
  methods: {
    selectItem(item: AutocompleteItem) {
      const value = this.inputEl.value.toString();
      const preItem = value.substr(0, this.selectionStart);
      const postItem = value.substr(this.currentSelection + 1, value.length);
      const finalValue = preItem + item + postItem;
      this.value = finalValue;
      this.cleanItem();
    },
    cleanItem() {
      this.open = false;
      this.selectionStart = 0;
      this.currentSelection = 0;
      this.selectedValue = "";
    },
    highlight(item: AutocompleteItem) {
      const _value =
        this.itemKey && typeof item === "object"
          ? String(item[this.itemKey])
          : String(item);
      const inputValue =
        "\\" +
        this.autocompleteKey +
        this.selectedValue.substr(1, this.selectedValue.length);
      const regex = new RegExp(`${inputValue}`);
      return _value.replace(regex, "<b>$&</b>");
    },
    getItems(data: AutocompleteItem[]) {
      this.items = [];
      for (let i = 0, l = data.length; i < l; i++) {
        const item = data[i];
        const key = String(
          this.itemKey && typeof item === "object" ? item[this.itemKey] : item,
        );
        if (key.startsWith(this.selectedValue)) {
          this.items.push(item);
        }
      }
    },
    initListeners() {
      if (this.inputEl) {
        this.inputEl.addEventListener("blur", this.inputBlured);
        this.inputEl.addEventListener("keydown", this.inputKeyPressed);
        this.inputEl.addEventListener("paste", this.inputPaste);
      }
    },
    inputPaste(event: ClipboardEvent) {
      this.cleanItem();
      const clipboardData = event.clipboardData!;
      const pastedData = clipboardData.getData("Text");
      this.selectedValue = pastedData;

      const target = event.target as HTMLInputElement;
      this.selectionStart = target.selectionStart || 0;
      this.currentSelection = this.selectionStart + pastedData.length;

      this.getItems(this.data);
      this.open = true;
    },

    inputBlured() {
      if (!this.open) {
        this.cleanItem();
      }
    },
    inputKeyPressed(event: KeyboardEvent) {
      event.stopPropagation();
      const key = event.key;
      const keyCode = event.keyCode;

      if (keyCode >= 8 && keyCode <= 46) {
        return;
      }
      if (key == this.autocompleteKey) {
        if (this.selectionStart != 0) {
          this.cleanItem();
        }
        this.selectionStart =
          (event.target as HTMLInputElement).selectionStart || 0;
        this.open = true;
      }
      this.currentSelection =
        (event.target as HTMLInputElement).selectionStart || 0;

      if (this.open) {
        const originalInputEl = this.inputEl.value.toString();
        const newInputEl =
          originalInputEl.substr(0, this.currentSelection) +
          key +
          originalInputEl.substr(
            this.currentSelection + 1,
            originalInputEl.length,
          );

        this.selectedValue = newInputEl.substr(
          this.selectionStart,
          this.currentSelection - this.selectionStart + 1,
        );
        this.getItems(this.data);
      }
    },
    initInputElByTarget(target: AutocompleteTarget) {
      if (!target) {
        return;
      }
      if (this.isString(target)) {
        // is selector
        this.inputEl = document.querySelector<HTMLInputElement>(target);
      } else if (this.isElement(target)) {
        // is element
        this.inputEl = target as HTMLInputElement;
      } else if (this.isElement(target.$el)) {
        // is component
        this.inputEl = target.$el as HTMLInputElement;
      }
    },
    isString(obj: unknown): obj is string {
      return typeof obj === "string";
    },
    isElement(el: unknown): el is Element {
      return el instanceof Element;
    },
  },
});
</script>

<style scoped></style>
