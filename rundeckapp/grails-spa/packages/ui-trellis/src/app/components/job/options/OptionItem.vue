<template>
  <div class="option-item">
    <template v-if="editable">
      <btn
        size="xs"
        type="plain"
        class="dragHandle"
        :title="$t('option.view.action.drag.title')"
        :disabled="!canMoveUp && !canMoveDown"
      >
        <i class="glyphicon glyphicon-resize-vertical"></i>
      </btn>

      <div class="btn-group">
        <btn
          size="xs"
          :disabled="!canMoveUp"
          @click="$emit('moveUp', option)"
          :title="$t('option.view.action.moveUp.title')"
        >
          <i class="glyphicon glyphicon-arrow-up"></i>
        </btn>

        <btn
          size="xs"
          :disabled="!canMoveDown"
          @click="$emit('moveDown', option)"
          :title="$t('option.view.action.moveDown.title')"
        >
          <i class="glyphicon glyphicon-arrow-down"></i>
        </btn>
      </div>
    </template>
    <span class="option-item-content" @click="$emit('edit', option)">
      <option-view :option="option" :editable="editable" />
    </span>
    <template v-if="editable">
      <span class="btn-group pull-right">
        <btn
          size="xs"
          @click="$emit('edit', option)"
          :title="$t('option.view.action.edit.title')"
        >
          <i class="glyphicon glyphicon-edit"></i>
          {{ $t("edit") }}
        </btn>
        <btn
          size="xs"
          @click="$emit('duplicate', option)"
          :title="$t('option.view.action.duplicate.title')"
        >
          <i class="glyphicon glyphicon-duplicate"></i>
          {{ $t("duplicate") }}
        </btn>
        <btn
          size="xs"
          type="danger"
          :title="$t('option.view.action.delete.title')"
          @click="$emit('delete', option)"
        >
          <i class="glyphicon glyphicon-remove"></i>
        </btn>
      </span>
    </template>
  </div>
</template>
<script lang="ts">
import { JobOption } from "@/library/types/jobs/JobEdit";
import OptionView from "./OptionView.vue";
import { defineComponent, PropType } from "vue";

export default defineComponent({
  name: "OptionItem",
  components: { OptionView },
  emits: ["edit", "duplicate", "moveUp", "moveDown", "delete"],
  props: {
    option: {
      type: Object as () => PropType<JobOption>,
      required: true,
    },
    editable: {
      type: Boolean,
      default: false,
    },
    canMoveUp: {
      type: Boolean,
      default: false,
    },
    canMoveDown: {
      type: Boolean,
      default: false,
    },
  },
});
</script>

<style scoped lang="scss">
.option-item {
  > .btn-group,
  > .btn,
  > .option-item-content {
    margin-right: var(--spacing-2);
  }
}
</style>
