<template>
<div  style="padding:30px">
    <vue-good-table
      theme="rd-table"
      @on-selected-rows-change="selectionChanged"
      :columns="columns"
      :pagination-options="{
        enabled: true,
        position: 'top',
        mode: 'pfilters'
      }"
      :search-options="{ enabled: true }"
      :select-options="{ 
        enabled: true,
        selectOnCheckboxOnly: true,
      }"
      :rows="rows"
      >
      <div slot="selected-row-actions">
        <button>Action 1</button>
      </div>
      <template slot="table-row" slot-scope="props">
        <span v-if="props.column.field == 'filter'">
          <input type="text" :value="props.row.filter"/>
        </span>
        <span v-else-if="props.column.field == 'enabled'">
          <rd-switch v-model="props.row.enabled" contrast />
        </span>
        <span v-else>
          {{props.formattedRow[props.column.field]}}
        </span>
      </template>
    </vue-good-table>
</div>
</template>
<script>
import { VueGoodTable } from 'vue-good-table';
import RdSwitch from '../inputs/Switch.vue'
export default {
  components: {
    RdSwitch,
    VueGoodTable
  },
  data () {
    return {
      columns: [
        {
          label: 'Project Name',
          field: 'name',
        },
        {
          label: 'Filter',
          field: 'filter',
          type: 'number',
        },
        {
          label: 'Enabled',
          field: 'enabled',
          type: 'boolean',
        },
      ],
      rows: [
        { id:1, name:"Zingbats Logistics Runbook", filter: ".*",enabled: true },
        { id:2, name:"Aws East SQS", filter: ".*", enabled: true },
        { id:3, name:"Aws West SQS", filter: ".*", enabled: true },
        { id:4, name:"Datadog", filter: ".*", enabled: true },
        { id:5, name:"PagerDuty", filter: ".*", enabled: true }
      ],
    }
  },
  methods: {
    onRowClick(params) {
      params.event.preventDefault();
    },
    selectionChanged(x){
      console.log(x)
    }
  },
}
</script>
<style lang="scss" scoped>
@import '../../../theme/scss/paper/_variables.scss';
// @import '../../../node_modules/vue-good-table/src/styles/style.scss';
// @import '../../../node_modules/vue-good-table/dist/vue-good-table.css';


$cell-padding: 1rem;
.rd-trow{
  border-top:1px solid $grey-400;
  border-left:1px solid $grey-400;
  border-right:1px solid $grey-400;
  background-color: $white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  &:last-child{
    border-bottom:1px solid $grey-400;
  }
}
.cell{
  display: flex;
  padding: $cell-padding;
  flex-grow: 1;
  &--100{
    width: 100%
  }
  &--none{
    flex: none;
  }
}
.checkbox label{
  padding: 0;
}
.justify-end{
  justify-self: end;
}
input[type="text"]{
  width: 100%;
}
</style>