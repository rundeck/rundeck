<template>
  <div>
    <div class="optslist" id="optionContent">
    <div class="col-sm-2 control-label text-form-label">
      <span id="optsload"></span>Options
    </div>
    <div class="col-sm-10">
      <div id="editoptssect" class="rounded">
    <div id="optundoredo" class="undoRedo undoredocontrols" style="margin-top:1em" v-if=editMode>
      <UndoRedo key='opts' :eventBus="eventBus" />
    </div>

    <div v-if="optionsCheck" id="optheader">
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
        :data-abs-index="(options.length || 1)"
        data-is-final="true"
        style="display:none"></div>

    <div v-if="!optionsCheck" :class="['empty', 'note', { error: 'error' }]" id="optempty">
      {{ $t('label.noOptions') }}
    </div>

      <div v-if=editMode id="optnewbutton" style="margin:10px 0; ">
        <span
          class="btn btn-default btn-sm ready"
          :title="$t('label.addNewOption')"
          @click="jQueryAddNew"
        >
          <span class="glyphicon glyphicon-plus" />
            {{ $t('label.addOption') }}
        </span>
      </div>
    </div>

    <div id="optnewitem"></div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue';
  import 'vue-i18n';
  import { OptionData, OptionDataShort } from "@/components/job/workflow/Workflow";
  import UndoRedo from '../../util/UndoRedo.vue';
  import OptionsListContent from './OptionsListContent.vue';

  const w = window as any;

  export default Vue.extend({
    name: 'Options',
    components: {
      OptionsListContent,
      UndoRedo
    },
    props: {
      options: Array,
      eventBus: Object,
      editMode: Boolean
    },
    mounted() {
      w._enableOptDragDrop();
    },
    computed: {
      optionsCheck: function(): boolean {
        return (this.options != null && this.options.length > 0);
      }
    },
    watch: {
      options: function() {
        this.optDataList = this.options.map((option: any) => ({
          name: option.name,
          type: option.optionType,
          multivalued: option.multivalued,
          delimiter: option.delimiter
        }));
        w._optionData(this.optDataList);
      }
    },
    methods: {
      jQueryAddNew() {
        w._optaddnew();
      }
    },
    data() {
      return {
        optDataList: [] as OptionDataShort[]
      }
    }
  })
</script>

<style lang="scss">
.undoRedo {
  margin-top: 1em;
  margin-bottom: 10px;
}
</style>
