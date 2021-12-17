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
                    <span class="header" >Name</span>
                </span>
        <span class="valuesSet">
                    <span class="header">Values</span>
                </span>
        <span class="enforceSet">
                    <span class="header">Restriction</span>
                </span>
      </div>
    </div>
    <div class="clear"></div>
    <ul class="options">
      <optlist-content :options=options :editMode=editMode />
    </ul>
    <div id="optionDropFinal"
        class="dragdropfinal droppableitem" :data-abs-index="(options.length() || 1)" data-is-final="true"
        style="display:none"></div>

    <div v-if="!optionsCheck" :class="['empty', 'note', { error: 'error' }]" id="optempty">
      No Options
    </div>

      <div v-if=editMode id="optnewbutton" style="margin:10px 0; ">
                <span class="btn btn-default btn-sm ready" v-on:click="w._optaddnew()" title="Add a new Option">
                    <b class="glyphicon glyphicon-plus"></b>
                    Add an option
                </span>
      </div>
    </div>

    <div id="optnewitem"></div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import UndoRedo from "../../util/UndoRedo.vue";
import OptlistContent from "./OptlistContent.vue";

const w = window;

export default Vue.extend({
  name: 'DetailsOptions',
  components: {OptlistContent, UndoRedo},
  props: {
    eventBus: Object,
    editMode: Boolean,
    options: Object
  },
  mounted() {
    w._enableOptDragDrop();
  },
  watch: {
    options: function() {
      const optList = this.options.map(option => ({
        name: option.name,
        type: option.optionType,
        multivalued: option.multivalued,
        delimiter: option.delimiter
      }));
      w._optionData(optList);
    }
  },
  methods: {
    optionsCheck() {
      return (this.options && this.options.length > 0);
    }
  },
  data() {
    return {
      optDataList: {}
    }
  }
})
</script>
