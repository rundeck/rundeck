<template>
  <div>
    <div id="optundoredo" class="undoredocontrols" style="margin-top:1em" v-if=editMode>
      <UndoRedo key='opts'/>
    </div>

    <div class="optslist" id="optionContent">
    <!--
    header
    -->

    <div v-if=optionsCheck() id="optheader">
      <div class="optheader optctrlholder">
        <span class="optdetail">
          <span class="header" >{{ $t('label.name') }}</span>
        </span>
        <span class="valuesSet">
          <span class="header">{{ $t('label.values') }}</span>
        </span>
        <span class="enforceSet">
          <span class="header">{{ $t('label.restriction') }}</span>
        </span>
      </div>
    </div>
    <div class="clear"></div>
    <ul class="options">
      <OptionsListContent
        :options="options"
        :editMode="editMode"
      />
    </ul>
    <div id="optionDropFinal"
        class="dragdropfinal droppableitem"
        :data-abs-index="(options.length() || 1)"
        data-is-final="true"
        style="display:none"></div>

    <div v-if="!optionsCheck" :class="['empty', 'note', { error: 'error' }]" id="optempty">
      {{ $t('label.noOptions') }}
    </div>

      <div v-if=editMode id="optnewbutton" style="margin:10px 0; ">
        <span
          class="btn btn-default btn-sm ready"
          :title="$t('label.addNewOption')"
          :click="w._optaddnew()"
        >
          <span class="glyphicon glyphicon-plus" />
            {{ $t('label.addOption') }}
        </span>
      </div>
    </div>

    <div id="optnewitem"></div>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue';
  import VueI18n from 'vue-i18n';
  import i18n from './i18n';
  import UndoRedo from '../../util/UndoRedo.vue';
  import OptionsListContent from './OptionsListContent.vue';
  Vue.use(VueI18n)

  const w = window as any;
  const jqery = w.jQuery as any;
  const _i18n = i18n as any;
  const lang = w._rundeck.language || 'en';
  const locale = w._rundeck.locale || 'en_US';

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

export default Vue.extend({
  name: 'DetailsOptions',
  components: {
    OptionsListContent,
    UndoRedo
  },
  props: {
    eventBus: Object,
    editMode: Boolean,
    options: Object
  },
  mounted() {
    w._enableOptDragDrop();
  },
  computed: {
    optionsCheck() {
      return (this.options && this.options.length > 0);
    }
  },
  watch: {
    options: function() {
      const optList = this.options.map(option => ({
        name: option.name,
        type: option.optionType,
        multivalued: option.multivalued,
        delimiter: option.delimiter
      }));
      this.optDataList = optList;
      w._optionData(this.optDataList);
    }
  },
  data() {
    return {
      optDataList: {}
    }
  }
})
</script>
