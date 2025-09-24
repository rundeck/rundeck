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

      <btn-group>
        <btn
          size="xs"
          :disabled="!canMoveUp"
          :title="$t('option.view.action.moveUp.title')"
          @click="$emit('moveUp', option)"
        >
          <i class="glyphicon glyphicon-arrow-up"></i>
        </btn>

        <btn
          size="xs"
          :disabled="!canMoveDown"
          :title="$t('option.view.action.moveDown.title')"
          @click="$emit('moveDown', option)"
        >
          <i class="glyphicon glyphicon-arrow-down"></i>
        </btn>
      </btn-group>
    </template>
    <span class="option-item-content" @click="$emit('edit', option)">
      <option-view :option="option" :editable="editable" />
    </span>
    <template v-if="editable">
      <btn-group class="pull-right">
        <btn
          size="xs"
          :title="$t('option.view.action.edit.title')"
          @click="$emit('edit', option)"
        >
          <i class="glyphicon glyphicon-edit"></i>
          {{ $t("edit") }}
        </btn>
        <btn
          size="xs"
          :title="$t('option.view.action.duplicate.title')"
          @click="$emit('duplicate', option)"
        >
          <i class="glyphicon glyphicon-duplicate"></i>
        </btn>
        <btn
          size="xs"
          type="danger"
          :title="$t('option.view.action.delete.title')"
          @click="$emit('delete', option)"
        >
          <i class="glyphicon glyphicon-remove"></i>
        </btn>
      </btn-group>
    </template>
  </div>
</template>
<script lang="ts">
import { JobOption } from "@/library/types/jobs/JobEdit";
import OptionView from "./OptionView.vue";
import { defineComponent, PropType } from "vue";
import { Btn, BtnGroup } from "uiv";

export default defineComponent({
  name: "OptionItem",
  components: { OptionView, Btn, BtnGroup },
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
  emits: ["edit", "duplicate", "moveUp", "moveDown", "delete"],
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
