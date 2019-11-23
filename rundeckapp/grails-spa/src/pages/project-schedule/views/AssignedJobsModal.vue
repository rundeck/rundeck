<template>
  <div>
    <modal id="assignedJobsModal" v-model="showAssignedJobsModal" :title="$t('Assigned Jobs To ' + schedule.name)" size="lg" :footer=false @hide="close()">
      <assigned-jobs-data
        v-bind:event-bus="eventBus"
        v-bind:schedule="schedule"
      ></assigned-jobs-data>
    </modal>
  </div>
</template>

<script>

    import AssignedJobsData from "./components/AssignedJobsData";
    export default {
        name: "AssignedJobs",
        props: ['eventBus', 'schedule'],
        components: {
            AssignedJobsData
        },
        data : function() {
            return {
                showAssignedJobsModal: true,
            }
        },
        methods: {
            close() {
                this.eventBus.$emit('closeAssignedJobsModal', {})
            }
        }
    }
</script>

<style scoped>

</style>
