<template>
  <div :id="`optvis${elementIdSuffix}`" :ref="`optvis${elementIdSuffix}`" v-show="visible">
    <div class="optitem optctrlholder">
      <span v-if=editMode class="optctrl opteditcontrols controls ">
        <span :class="['btn', 'btn-xs', 'btn-default', canMoveUp||canMoveDown ? 'dragHandle' : 'disabled']" :title="$t('edit.dragReorder')">
          <span class="glyphicon glyphicon-resize-vertical" />
        </span>
      <div class="btn-group">
        <span :class="['btn', 'btn-xs', 'btn-default', canMoveUp ? '' : 'disabled']" @click="legacyOptions('reorder', -1)" :title="$t('edit.moveUp')">
          <span class="glyphicon glyphicon-arrow-up" />
        </span>

        <span :class="['btn', 'btn-xs', 'btn-default', canMoveDown ? '' : 'disabled']" @click="legacyOptions('reorder', 1)" :title="$t('edit.moveDown')">
          <span class="glyphicon glyphicon-arrow-down" />
        </span>
      </div>
      </span>

      <span class="opt item" :id="optionName" >
        <OptView
          :option="option"
          :editMode="editMode"
          :elemIdSuffix="elementIdSuffix"
        />
      </span>

      <div :id="`optdel${elementIdSuffix}`" class="panel panel-danger" v-show="deleteConfirm">
        <div class="panel-heading">
          {{ $t('edit.deleteOption') }}
        </div>

        <div class="panel-body">
          {{ $t('message.deleteOptionConfirm', [optionName]) }}
        </div>
        <div class="panel-footer">
          <span class="btn btn-default btn-xs" @click="deleteConfirm = false">{{ $t('edit.cancel') }}</span>
          <span class="btn btn-danger btn-xs" @click="legacyOptions('remove')">{{ $t('edit.delete') }}</span>
        </div>
      </div>

      <div v-if="editMode">
        <span class="optctrl opteditcontrols controls " :id="`optctrls${elementIdSuffix}`" style="position:absolute; right:0;">
          <span class="btn btn-xs btn-info" @click="legacyOptions('edit')" :title="$t('edit.editOption')">
            <span class="glyphicon glyphicon-edit" />
            {{ $t('edit.edit') }}
          </span>
          <span class="btn btn-xs btn-info" @click="legacyOptions('copy')" :title="$t('edit.duplicateOption')">
            <span class="glyphicon glyphicon-duplicate" />
            {{ $t('edit.duplicate') }}
          </span>
          <span class="btn btn-xs btn-danger" @click="deleteConfirm = true" :title="$t('edit.deleteOption')">
            <span class="glyphicon glyphicon-remove" />
          </span>
        </span>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue';
  import VueI18n from 'vue-i18n';
  import i18n from './i18n';
  import OptionsView from './OptionsView.vue';
  import {
    getRundeckContext,
    RundeckContext
  } from "@rundeck/ui-trellis";

  const w = window as any;
  const winRd = getRundeckContext();
  const jquery = w.jQuery;
  const _i18n = i18n as any;
  const lang = winRd.language || 'en';
  const locale = winRd.locale || 'en_US';

  const messages = {
    [locale]: {
      ...(_i18n[lang] || _i18n.en),
      ...(w.Messages[lang])
    }
  }

  const i18nInstance = new VueI18n({
    silentTranslationWarn: true,
    locale: locale,
    messages
  })

  // @ts-ignore
  Vue.use(VueI18n);

  export default Vue.extend({
    name: 'OptionsListContentItem',
    components: {
      OptionsView
    },
    props: {
      option: Array,
      optionName: String,
      editMode: Boolean,
      optIndex: Number,
      optCount: Number
    },
    computed: {
      canMoveUp: function(): boolean {
        return this.optIndex != 0;
      },
      canMoveDown: function(): boolean {
        return this.optIndex < this.optCount - 1;
      },
      optionName: function(): string {
        return JSON.stringify(this.option.name);
      }
    },
    methods: {
      reorder: function(value: string, pos: number) {
        w._doReorderOption(value, {pos: pos});
      },
      legacyOptions: function(actionType: string, reorderPos: number) {
        const jq = jquery(w);
        const optName = this.optionName;
        const i = this.optIndex;
        if (actionType === 'remove') {
          w._doRemoveOption(optName, jq.select(`#optli_${i}`));
        } else if (actionType === 'copy') {
          w._optcopy(optName)
        } else if (actionType === 'edit') {
          w._optedit(optName, jq.select(`#optli_${i}`));
        } else if (actionType === 'reorder') {
          w._doReorderOption(optName, {pos: reorderPos});
        }
      }
    },
    data() {
      return {
        optionName: "",
        elementIdSuffix: `_${this.optionName}_${this.optIndex}`,
        editMode: this.editMode,
        deleteConfirm: false
      }
    }
  })
</script>
