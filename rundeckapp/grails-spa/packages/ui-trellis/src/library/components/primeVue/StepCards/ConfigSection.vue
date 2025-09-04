<template>
  <div class="config-section" :class="{ 'has-chips': modelValue.length > 0 }">
    <div class="header-row">
      <p>
        {{ title }}
        <i class="fa fa-info-circle" v-tooltip="{ value: tooltip }"></i>:
      </p>
      <button
        v-if="modelValue.length === 0 && !hideWhenSingle"
        v-show="!hideWhenSingle"
        @click="handleAdd"
        class="inline-button"
      >
        + add
      </button>
    </div>
    <slot>
      <transition name="chips-slide" mode="out-in">
        <div v-if="modelValue.length > 0" class="chips-row">
          <transition-group name="chip-list" tag="div" class="chips-container">
            <Chip
                v-for="(element, index) in modelValue"
                :key="element.name || index"
                :label="element.title"
                removable
                @remove="handleRemove(index)"
            />
          </transition-group>
          <button
              v-if="!hideWhenSingle"
              v-show="!hideWhenSingle || modelValue.length !== 1"
              @click="handleAdd"
          >
            + add
          </button>
        </div>
      </transition>
    </slot>
  </div>
  <slot name="extra"></slot>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import Chip from 'primevue/chip';


export default defineComponent({
  name: "ConfigSection",
  components: {Chip},
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
  border-bottom: 1px solid red;
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
</style>