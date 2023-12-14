<template>
  <div>
    <div>
      <template v-if="editable">
        <span
          class="dragHandle btn btn-xs"
          :title="$t('drag.to.reorder')"
          v-if="canMoveUp || canMoveDown"
        >
          <i class="glyphicon glyphicon-resize-vertical"></i>
        </span>
        <span class="btn btn-xs btn-default disabled" v-else
          ><i class="glyphicon glyphicon-resize-vertical"></i
        ></span>
        <div class="btn-group">
          <span
            class="btn btn-xs btn-default"
            v-if="canMoveUp"
            @click="$emit('moveUp', option)"
            :title="$t('move.up')"
          >
            <i class="glyphicon glyphicon-arrow-up"></i>
          </span>
          <span class="btn btn-xs btn-default disabled" v-else>
            <i class="glyphicon glyphicon-arrow-up"></i>
          </span>

          <span
            class="btn btn-xs btn-default"
            v-if="canMoveDown"
            @click="$emit('moveDown', option)"
            :title="$t('move.down')"
          >
            <i class="glyphicon glyphicon-arrow-down"></i>
          </span>
          <span class="btn btn-xs btn-default disabled" v-else>
            <i class="glyphicon glyphicon-arrow-down"></i>
          </span>
        </div>
      </template>
      <span class="opt item" @click="$emit('edit',option)">
        <option-view :option="option" />
      </span>
      <template v-if="editable">
        <span
          class="btn-group pull-right"
        >
          <span
            class="btn btn-xs btn-default"
            @click="$emit('edit', option)"
            title="$t('edit.this.option')"
          >
            <i class="glyphicon glyphicon-edit"></i>
            {{ $t("edit") }}
          </span>
          <span
            class="btn btn-xs btn-default"
            @click="$emit('duplicate', option)"
            title="$t('duplicate.this.option')"
          >
            <i class="glyphicon glyphicon-duplicate"></i>
            {{ $t("duplicate") }}
          </span>
          <span
            class="btn btn-xs btn-danger"
            :title="$t('delete.this.option')"
            @click="$emit('delete', option)"
          >
            <i class="glyphicon glyphicon-remove"></i>
          </span>
        </span>
      </template>
    </div>
  </div>
</template>
<script lang="ts">
import OptionView from "../OptionView.vue";
import { defineComponent } from "vue";

export default defineComponent({
  name: "OptionItem",
  components: { OptionView },
  emits: ["edit", "duplicate", "moveUp", "moveDown", "delete"],
  props: {
    option: {
      type: Object,
      required: true,
    },
    optIndex: {
      type: Number,
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

<style scoped lang="scss"></style>