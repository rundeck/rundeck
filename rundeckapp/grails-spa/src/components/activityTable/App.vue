<template>
  <div>
    <h1>hello world</h1>
    <activity-table></activity-table>
    <vue-bootstrap-table
            :columns="columns"
            :values="values"
            :show-filter="true"
            :show-column-picker="true"
            :sortable="true"
            :paginated="true"
            :multi-column-sortable="true"
            :filter-case-sensitive="false">
    </vue-bootstrap-table>
  </div>
</template>

<script>
import activityTable from '@/components/activityTable/activityTable'
import VueBootstrapTable from 'vue2-bootstrap-table2'
import axios from 'axios'

export default {
  name: 'App',
  components: {
    activityTable,
    VueBootstrapTable
  },
  data () {
    return {
      columns: [
        {
          title: ''
        }
      ],
      values: []
    }
  },
  mounted () {
    if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
      axios({
        method: 'get',
        headers: {'x-rundeck-ajax': true},
        url: `${window._rundeck.rdBase}/project/${window._rundeck.projectName}/events/eventsAjax`,
        withCredentials: true,
        params: {
          // jobFilter: 'f6b8aec9-16b1-4b4d-a9fb-b62ef5d92d00'
        }
      }).then((response) => {
        console.log('eventsAjax', response)
      })
    }
  }
}
</script>

<style>
</style>
