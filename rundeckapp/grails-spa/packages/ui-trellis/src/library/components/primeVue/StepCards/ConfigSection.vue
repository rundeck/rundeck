<template>
  <div class="config-section" :class="{ 'has-chips': modelValue.length > 0, 'edit-view': isEditView }">

    <!-- Edit view: title + description grouped, then chips, then add button -->
    <template v-if="isEditView">
      <div class="section-header">
        <p class="section-title" data-testid="config-section-title">{{ title }}</p>
        <p v-if="tooltip" class="section-description" data-testid="config-section-description">{{ tooltip }}</p>
      </div>
      <slot name="extra">
        <div v-if="modelValue.length > 0" class="chips-row" data-testid="config-section-chips-row">
          <transition-group name="chip-list" tag="div" class="chips-container">
            <Chip
              v-for="(element, index) in modelValue"
              :key="element.name || element.type || index"
              :label="getElementLabel(element)"
              :icon="hideIcon ? undefined : getElementIcon(element)"
              removable
              @remove="handleRemove(index)"
              @click="handleEdit(index)"
            />
          </transition-group>
        </div>
      </slot>
      <button
        v-if="modelValue.length === 0 || !hideWhenSingle"
        @click.prevent="handleAdd"
        class="edit-view-add-btn"
        type="button"
        data-testid="config-section-add-btn"
      >
        <i class="pi pi-plus" />
        Add
      </button>
      <slot name="content" />
    </template>

    <!-- Default chip view: title inline with tooltip icon and add button -->
    <template v-else>
      <div class="header-row">
        <p data-testid="config-section-title">
          {{ title }}
          <i class="pi pi-info-circle" v-tooltip="{ value: tooltip }"></i> :
        </p>
        <slot name="header">
          <transition name="inline-button-fade">
            <button
              v-if="modelValue.length === 0"
              @click.prevent="handleAdd"
              class="inline-button link-button"
              type="button"
              data-testid="config-section-add-btn"
            >
              + Add
            </button>
          </transition>
        </slot>
      </div>

      <transition name="chips-slide" mode="out-in">
        <slot name="extra">
          <div v-if="modelValue.length > 0" class="chips-row" data-testid="config-section-chips-row">
            <transition-group name="chip-list" tag="div" class="chips-container">
              <Chip
                v-for="(element, index) in modelValue"
                :key="element.name || element.type || index"
                :label="getElementLabel(element)"
                :icon="hideIcon ? undefined : getElementIcon(element)"
                removable
                @remove="handleRemove(index)"
                @click="handleEdit(index)"
              />
            </transition-group>
            <button
              v-if="!hideWhenSingle"
              v-show="!hideWhenSingle || modelValue.length !== 1"
              @click.prevent="handleAdd"
              class="link-button"
              data-testid="config-section-add-more-btn"
              type="button"
            >
              + Add
            </button>
          </div>
        </slot>
      </transition>
      <slot name="content" />
    </template>

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
      required: false,
      default: "",
    },
    modelValue: {
      type: Array as PropType<object[]>,
      default: () => [],
    },
    hideWhenSingle: {
      type: Boolean,
      default: false,
    },
    hideIcon: {
      type: Boolean,
      default: false,
    },
    isEditView: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["addElement", "removeElement", "editElement", "update:modelValue"],
  methods: {
    handleAdd() {
      this.$emit("addElement");
    },
    handleRemove(index: number) {
      const updatedArray = [...this.modelValue];
      updatedArray.splice(index, 1);
      this.$emit("update:modelValue", updatedArray);
      this.$emit("removeElement", index);
    },
    handleEdit(index: number) {
      if (!this.modelValue[index]) {
        return;
      }
      const element = this.modelValue[index];
      this.$emit("editElement", element, index);
    },
    getElementLabel(element: any): string {
      return element.title || element.type || "Unknown";
    },
    getElementIcon(element: any): string {
      const icon = element.providerMetadata?.faicon || "filter";
      return `fa fa-${icon}`;
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
      font-family: Inter, var(--fonts-body2) !important;

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
    cursor: pointer;

    &:hover {
      opacity: 0.8;
    }
  }

  &.has-chips .header-row {
    margin-bottom: 0;
  }

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }

  &.edit-view {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .section-header {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .section-title {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      line-height: 21px;
      color: var(--colors-gray-800, #1a202c);
    }

    .section-description {
      margin: 0;
      font-size: 14px;
      font-weight: 400;
      color: var(--colors-gray-600, #4a5568);
    }

    .edit-view-add-btn {
      all: unset;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      background: #f5f9ff;
      color: #0052cc;
      font-size: 12px;
      font-weight: 500;
      padding: 5px 9px;
      border-radius: 6px;
      cursor: pointer;

      .pi {
        font-size: 10px;
      }

      &:hover {
        opacity: 0.85;
      }
    }
  }

  .link-button {
    all: unset;
    color: #0052CC;
    cursor: pointer;
    font-weight: 400;
    font-size: 12px;

    &:hover {
      text-decoration: underline;
    }
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
.p-chip-icon.fa {
  width: auto !important;
  height: auto !important;
}
</style>
