<template>
  <div>
    <modal id="scheduleUploadModal" v-model="showUploadModal" :title="$t('Persist Schedules')" size="lg" :footer=false @hide="close(false)">
      <div class="alert alert-danger" v-if="persistErrors">
        <ul>
          <li v-for="error in persistErrors">
            <span>{{error}}</span>
          </li>
        </ul>
      </div>
      <div class="base-filters">
        <div class="row">
          <label for="scheduleUploadSelect">Select a Schedule definition file.</label>
        </div>
        <div class="row">
          <div class="panel panel-default"  >
            <div class="panel-body">
              <div class="container">
                <div class="row">
                  <div class="form-group">
                    <input type="file"
                           name="scheduleUploadSelect"
                           id="scheduleUploadSelect"
                           ref="scheduleUploadSelect"
                           class="form-control"/>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="row">
          <div class="col-xs-12 col-sm-4">
            <div class="form-group">
              <button type="button" class="btn btn-default" @click="close(false)">Close</button>
              <button type="button" class="btn btn-default" @click="save">Save</button>
            </div>
          </div>
        </div>
      </div>
    </modal>
  </div>
</template>

<script>

    import Vue from "vue"
    import {getRundeckContext} from "@rundeck/ui-trellis"
    import axios from 'axios'

    export default Vue.extend({
        name: "ScheduleUpload",
        props: ['eventBus'],
        data : function() {
            return {
                showUploadModal: true,
                persistErrors: null
            }
        },
        methods: {
             close(refresh){
                 this.eventBus.$emit('closeUploadDefinitionModal', {'reload': refresh})
            },
            save(){
                let formData = new FormData();
                let files = this.$refs.scheduleUploadSelect.files
                for (var i = 0; i < files.length; i++) {
                    let file = files[i];
                    formData.append("scheduleUploadSelect", file);
                }

                const rundeckContext = getRundeckContext()
                axios({
                    method: "post",
                    headers: {
                        "x-rundeck-ajax": true,
                        "Content-Type": "multipart/form-data"
                    },
                    data: formData,
                    params: { project: rundeckContext.projectName},
                    url: `${window._rundeck.rdBase}projectSchedules/uploadFileDefinition`,
                    withCredentials: true
                }).then(response => {
                    if(!response.data.success){
                        this.persistErrors = response.data.errors
                    }else{
                        this.close(true)
                    }
                })
            }
        }
    })

</script>

<style scoped>

</style>
