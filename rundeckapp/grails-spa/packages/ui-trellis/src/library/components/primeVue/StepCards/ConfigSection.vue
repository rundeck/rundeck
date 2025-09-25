<template>
  <div class="config-section" :class="{ 'has-chips': modelValue.length > 0 }">
    <div class="header-row">
      <p>
        {{ title }}
        <i class="pi pi-info-circle" v-tooltip="{ value: tooltip }"></i> :
      </p>
      <slot name="header">
        <transition name="inline-button-fade">
          <button
            v-if="modelValue.length === 0"
            @click="handleAdd"
            class="inline-button link-button"
          >
            + Add
          </button>
        </transition>
      </slot>
    </div>

    <transition name="chips-slide" mode="out-in">
      <slot name="extra">
        <div v-if="modelValue.length > 0" class="chips-row">
          <transition-group name="chip-list" tag="div" class="chips-container">
            <Chip
              v-for="(element, index) in modelValue"
              :key="element.name || index"
              :label="element.title"
              :icon="`fa fa-${element.providerMetadata.faicon}`"
              removable
              @remove="handleRemove(index)"
            />
          </transition-group>
          <button
            v-if="!hideWhenSingle"
            v-show="!hideWhenSingle || modelValue.length !== 1"
            @click="handleAdd"
            class="link-button"
          >
            + Add
          </button>
        </div>
      </slot>
    </transition>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import Chip from "primevue/chip";

export default defineComponent({
  name: "ConfigSection",
  components: { Chip },
  props: {
    title: {
      type: String,
      required: true,
    },
    tooltip: {
      type: String,
      required: true,
    },
    modelValue: {
      type: Array as PropType<object[]>,
      default: () => [],
    },
    hideWhenSingle: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["addElement", "removeElement", "update:modelValue"],
  methods: {
    handleAdd() {
      this.$emit("addElement");
    },
    handleRemove(index: number) {
      const updatedArray = [...this.modelValue];
      updatedArray.splice(index, 1);
      this.$emit("update:modelValue", updatedArray);
    },
  },
});
</script>

<style lang="scss" scoped>
.config-section {
  border-bottom: 1px solid var(--p-accordion-panel-border-color);
  padding: var(--sizes-4) 0;

  .header-row {
    display: flex;
    align-items: center;
    margin-bottom: 0;

    p {
      margin: 0;
      font-family: Inter, var(--fonts-body) !important;

      i {
        width: auto;
      }
    }

    .inline-button {
      margin-left: var(--sizes-2);
    }
  }

  .chips-row {
    display: flex;
    align-items: center;
    gap: var(--sizes-1);
    margin-top: var(--sizes-2);
    flex-wrap: wrap;
  }

  .chips-container {
    display: flex;
    gap: var(--sizes-1);
    flex-wrap: wrap;
  }

  .p-chip {
    background-color: var(--colors-surface-200);
    font-size: 12px;
    height: 26px;
    padding-top: 6px;
    padding-bottom: 6px;
  }

  &.has-chips .header-row {
    margin-bottom: 0;
  }

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }
}

// Transition animations
.chips-slide-enter-active,
.chips-slide-leave-active {
  transition: all 0.25s ease-out;
}

.chips-slide-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}

.chips-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.chip-list-enter-active,
.chip-list-leave-active {
  transition: all 0.2s ease-out;
}

.chip-list-enter-from,
.chip-list-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

.chip-list-move {
  transition: transform 0.2s ease-out;
}

.inline-button-fade-enter-active,
.inline-button-fade-leave-active {
  transition: all 0.25s ease-out;
}

.inline-button-fade-enter-from,
.inline-button-fade-leave-to {
  opacity: 0;
}
</style>

<style>
.p-chip-icon.fa.fa-volume-off {
  width: auto !important;
  height: auto !important;
}
</style>
