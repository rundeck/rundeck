<template>
  <div>

    <div class="help-block">
      Notifications can be triggered by different events during the Job Execution.
    </div>
    <div >
      <undo-redo :event-bus="this"/>

      <div v-if="notifications.length < 1" >
        <p class="text-muted">No Notifications are defined. Click an event below to add a Notification for that Trigger.</p>
      </div>
      <div class="main-section">
      <div v-for="(trigger) in notifyTypes"  >
          <div  class="list-group" :id="'job-notifications-'+trigger">
            <div class="list-group-item flex-container flex-align-items-baseline flex-justify-space-between">
              <span class="flex-item " :class="{'text-secondary':(!hasNotificationsForTrigger(trigger))}">
              <i class="fas" :class="triggerIcons[trigger]"></i>
              {{$t('notification.event.'+trigger)}}
              </span>
              <btn type="default"
                   class="  flex-item"
                   size="sm" @click="addNotification(trigger)">
                <i class="fas fa-plus"></i>
                Add Notification
              </btn>
            </div>
            <div class="list-group-item form-inline" v-if="trigger==='onavgduration'">
              <div class="form-group">
                <div class="col-sm-12">
                  <div class="input-group">
                    <label class=" input-group-addon" for="schedJobNotifyAvgDurationThreshold">
                      {{ $t('scheduledExecution.property.notifyAvgDurationThreshold.label') }}
                    </label>
                    <input type='text'
                           name="notifyAvgDurationThreshold"
                           v-model="notifyAvgDurationThreshold"
                           id="schedJobNotifyAvgDurationThreshold"
                           class="form-control"
                           :placeholder="$t('jobAverageDurationPlaceholder')"
                           size="40"/>
                    <span class="input-group-addon btn btn-info btn-md"  id="jobAvgInfoBtn">
                        <i class="glyphicon glyphicon-question-sign "></i>
                    </span>
                  </div>

                  <popover :title="$t('scheduledExecution.property.notifyAvgDurationThreshold.label')" target="#jobAvgInfoBtn">
                    <template slot="popover">
                      <markdown-it-vue class="markdown-body" :content="$t('scheduledExecution.property.notifyAvgDurationThreshold.description')"/>
                    </template>
                  </popover>

                </div>
              </div>
            </div>
            <div v-for="(notif,i) in getNotificationsForTrigger(trigger)" v-if="getNotificationsForTrigger(trigger)" class="list-group-item flex-container flex-justify-start">
              <div style="margin-right:10px;">
                <dropdown ref="dropdown" append-to-body>
                  <btn type="simple" class=" btn-hover  btn-secondary dropdown-toggle">
                    <span class="caret"></span>
                  </btn>
                  <template slot="dropdown">
                    <li @click="doCopyNotification(notif)">
                      <a role="button">
                        {{$t('Duplicate...')}}
                      </a>
                    </li>
                    <li role="separator" class="divider"></li>
                    <li @click="doDeleteNotification(notif)">
                      <a role="button">
                        {{$t('Delete')}}
                      </a>
                    </li>
                  </template>
                </dropdown>
              </div>
              <div class="flex-item flex-grow-1" >

                <plugin-config
                    serviceName="Notification"
                    :provider="notif.type"
                    :config="notif.config"
                    mode="show"
                    :show-title="true"
                    :show-description="true"
                    :key="'g_'+i+'/'+notif.type+':config'"
                    scope="Instance"
                    default-scope="Instance"
                />
              </div>

              <btn type="default" size="sm" @click="doEditNotification(notif)">Edit</btn>

            </div>
          </div>

        </div>
      </div>
    </div>



    <modal v-model="editModal" :title="$t(editIndex<0?'Create Notification':'Edit Notification')" size="lg" id="job-notifications-edit-modal" append-to-body>
      <div>
        <div class="form-group"  >
          <label class="col-sm-2 control-label  " >
            Trigger
          </label>
          <div class="col-sm-10 form-control-static">

            <dropdown ref="dropdown" id="notification-edit-trigger-dropdown">
              <btn type="simple" class=" btn-hover  btn-secondary dropdown-toggle">
                <span class="caret"></span>
                &nbsp;
                <span v-if="editNotificationTrigger" class="text-strong">
                  <i class="fas" :class="triggerIcons[editNotificationTrigger]"></i>
                  {{ $t('notification.event.' + editNotificationTrigger) }}
                </span>
                <span v-else>
                  Select a Trigger
                </span>
              </btn>
              <template slot="dropdown">
                <li v-for="trigger in notifyTypes"
                    @click="setEditNotificationTrigger(trigger)"
                    :data-trigger="trigger"
                >
                  <a role="button">
                    <i class="fas" :class="triggerIcons[trigger]"></i>
                    {{ $t('notification.event.' + trigger) }}
                  </a>
                </li>
              </template>
            </dropdown>

          </div>

        </div>


        <div v-if="editNotificationTrigger" >

          <div class="form-group">
            <label class="col-sm-2 control-label  " >
              Notification Type
            </label>
            <div class="col-sm-10  form-control-static">
              <dropdown ref="dropdown" id="notification-edit-type-dropdown">
                <btn type="simple"  class="btn-simple btn-hover  btn-secondary dropdown-toggle">
                  <span class="caret"></span>
                  &nbsp;
                  <span v-if="editNotification.type && getProviderFor(editNotification.type)">
                    <plugin-info
                        :detail="getProviderFor(editNotification.type)"
                        :show-description="false"
                        :show-extended="false"
                        description-css="help-block"
                    >
                      </plugin-info>
                  </span>
                  <span v-else>
                  Select a Notification
                  </span>
                </btn>
                <template slot="dropdown">
                  <li v-for="plugin in sortedProviders" :key="plugin.name">
                    <a role="button"
                       @click="setEditNotificationType(plugin.name)"
                       :data-plugin-type="plugin.name">
                      <plugin-info
                          :detail="plugin"
                          :show-description="true"
                          :show-extended="false"
                          description-css="help-block"
                      >
                      </plugin-info>
                    </a>
                  </li>
                </template>
              </dropdown>
              <div v-if="editNotification.type && getProviderFor(editNotification.type)">
                    <plugin-info
                        :detail="getProviderFor(editNotification.type)"
                        :show-description="true"
                        :show-extended="false"
                        :show-title="false"
                        :show-icon="false"
                        description-css="help-block"
                    >
                      </plugin-info>
                  </div>

          </div>
        </div>
        </div>

        <plugin-config
            id="notification-edit-config"
            :mode="editIndex===-1 ? 'create':'edit'"
            :serviceName="'Notification'"
            v-model="editNotification"
            :key="'edit_config'+editIndex+'/'+editNotification.type"
            :show-title="false"
            :show-description="false"
            :context-autocomplete="true"
            @handleAutocomplete="handleAutoComplete"
            :validation="editValidation"
            scope="Instance"
            default-scope="Instance"
        ></plugin-config>

      </div>

      <div slot="footer">
        <btn @click="cancelEditNotification" id="job-notifications-edit-modal-btn-cancel">{{ $t('Cancel') }}</btn>
        &nbsp;
        <btn @click="saveNotification"
             type="primary"
             :disabled="!editNotificationTrigger || !editNotification.type"
             id="job-notifications-edit-modal-btn-save"
        >{{ $t('Save') }}</btn>
        <span v-if="editValidation && !editValidation.valid" class="text-warning">
          Please correct the highlighted errors.
        </span>
        <span v-if="editError" class="text-warning">
          {{ editError }}
        </span>
      </div>
    </modal>
  </div>
</template>
<script>


import PluginInfo from "@/library/components/plugins/PluginInfo.vue";
import PluginConfig from "@/library/components/plugins/pluginConfig.vue";
import pluginService from "@/library/modules/pluginService";
import ExtendedDescription from "@/library/components/utils/ExtendedDescription.vue";
import UndoRedo from "../../util/UndoRedo.vue"

export default {
  name: 'NotificationsEditor',
  props: ['eventBus', 'notificationData'],
  components: {PluginInfo,PluginConfig,ExtendedDescription,UndoRedo},
  data () {
    return {
      notifyAvgDurationThreshold:null,
      pluginProviders: [],
      pluginLabels: {},
      notifyTypes:[
        'onstart',
          'onsuccess',
          'onfailure',
          'onretryablefailure',
          'onavgduration',
      ],
      triggerIcons: {
        'onsuccess': 'fa-check-square text-success',
        'onfailure': 'fa-times-circle text-danger',
        'onstart': 'fa-play text-info',
        'onavgduration': 'fa-clock text-secondary',
        'onretryablefailure': 'fa-redo text-warning'
      },
      notifications:[],
      editNotificationTrigger:null,
      editNotification:{},
      editValidation:null,
      editError:null,
      editIndex:-1,
      editModal:false
    }
  },
  computed:{
    sortedProviders(){
      let prov=[this.getProviderFor('email'),this.getProviderFor('url')]
      let other=this.pluginProviders.filter(x=>x.name!=='email' && x.name!=='url')
      return prov.concat(other)
    },
    groupedNotifications(){
      let grouped = {}
      this.notifyTypes.forEach(trigger=>{
        let found=this.notifications.filter(s=>s.trigger===trigger)
        if(found && found.length>0){
          grouped[trigger]=found
        }
      })
      return grouped;
    }
  },
  methods:{
    async addNotification(trigger){
      this.editNotificationTrigger = trigger
      this.editNotification={type:null,config:{}}
      this.editIndex=-1
      this.editValidation=null
      this.editError=null
      this.editModal=true
    },
    async doEditNotification(notif){
      this.editIndex=this.notifications.findIndex(n=>n===notif)
      if(this.editIndex<0){
        return
      }
      this.editNotificationTrigger=this.notifications[this.editIndex].trigger
      this.editNotification=this.notifications[this.editIndex]
      this.editValidation=null
      this.editError=null
      this.editModal=true
    },
    async operationDelete(index){
      return this.notifications.splice(index,1)
    },
    async operationCreate(value){
      this.notifications.push(value)
    },
    async operationInsert(index,value){
      this.notifications.splice(index, 0, value)
    },
    async operationModify(index,value){
      this.notifications.splice(index,1,value)
    },
    async doDelete(index){
      let oldval = this.doClone(this.notifications[index])
      await this.operationDelete(index)
      this.$emit(
          "change",
          {
            index: index,
            value: oldval,
            operation: 'delete',
            undo: 'insert',
          }
      )
    },
    async doCreate(value){
      await this.operationCreate(value)
      let value1 = this.doClone(value)
      let index = this.notifications.length - 1
      this.$emit(
          "change",
          {
            index: index,
            value: value1,
            operation: 'insert',
            undo: 'delete',
          }
      )
    },
    async doModify(index,value){
      let oldval = this.doClone(this.notifications[index])
      await this.operationModify(index,value)
      let clone = this.doClone(value)
      this.$emit("change",{
        index: index,
        value: clone,
        orig: oldval,
        operation: 'modify',
        undo: 'modify'
      })
    },
    async doDeleteNotification(notif){
      let ndx=this.notifications.findIndex(n=>n===notif)
      if(ndx>=0){
        return this.doDelete(ndx)
      }
    },
    async doCopyNotification(notif){
      this.editNotificationTrigger=notif.trigger
      this.editNotification= {type:notif.type,config:Object.assign({},notif.config)}
      this.editIndex=-1
      this.editValidation=null
      this.editError=null
      this.editModal=true
    },
    async cancelEditNotification(){
      this.editModal=false
      this.editIndex=-1
      this.editNotification={type:null,config:{}}
      this.editNotificationTrigger=null
      this.editValidation=null
      this.editError=null
    },
    async setEditNotificationTrigger(name){
      this.editError=null
      this.editNotificationTrigger=name
    },
    async setEditNotificationType(name){
      this.editValidation=null
      this.editError=null
      this.editNotification.type=name
    },
    doClone(notif){
      return {
        type:notif.type,
        trigger:notif.trigger,
        config:Object.assign({},notif.config)
      }
    },
    async saveNotification(){
      if(!this.editNotificationTrigger){
        this.editError='Choose a Trigger'
        return
      }
      if(!this.editNotification.type){
        this.editError='Choose a Notification Type'
        return
      }

      if(this.editNotification.config.recipients != null){
        let recipientsStr = this.editNotification.config.recipients
        this.editNotification.config.recipients = recipientsStr.split(",").map( mail => mail.trim()).join(",")
      }

      const validation = await pluginService.validatePluginConfig(
          'Notification',
          this.editNotification.type,
          this.editNotification.config,
          'Project'
      )
      if (!validation.valid) {
        this.editValidation = validation
        return
      }
      this.editModal=false
      this.editNotification.trigger=this.editNotificationTrigger
      if(this.editIndex<0){
        await this.doCreate(this.editNotification)
      }else{
        await this.doModify(this.editIndex,this.editNotification)
      }
      this.editIndex=-1
      this.editNotification={}
    },
    getProviderFor(name){
      return this.pluginProviders.find(p => p.name === name)
    },
    getNotificationsForTrigger(trigger){
      return this.notifications.filter(s=>s.trigger===trigger)
    },
    hasNotificationsForTrigger(trigger){
      return this.notifications.findIndex(s=>s.trigger===trigger)>=0
    },
    doUndo(change){
      this.perform(change.undo, {index:change.index,value:change.orig||change.value})
    },
    doRedo(change){
      this.perform(change.operation,change)
    },
    handleAutoComplete(){
      window.setupNotificationAutocomplete("notification-edit-config")
    },
    async perform(operation,change){
      if(operation==='create'){
        return this.operationCreate(change.value)
      }if(operation==='insert'){
        return this.operationInsert(change.index, change.value)
      }else if(operation==='modify'){
        return this.operationModify(change.index, change.value)
      }else if(operation==='delete'){
        return this.operationDelete(change.index)
      }
    },
  },
  watch:{
    notifications(){
      this.$emit('changed',this.notifications)
    }
  },
  async mounted () {
    this.notifications = [].concat(this.notificationData.notifications || [])
    this.notifyAvgDurationThreshold = this.notificationData.notifyAvgDurationThreshold
    this.$on("undo",this.doUndo)
    this.$on("redo",this.doRedo)
    pluginService
        .getPluginProvidersForService('Notification')
        .then(data => {
          if (data.service) {
            this.pluginProviders = data.descriptions;
            this.pluginLabels = data.labels;
          }
        });
  }
}
</script>
<style lang="scss" scoped>
.list-group-item {
  &.list-group-item-secondary {
    border-width: 0;
  }
}
.list-placeholder{
  margin-bottom: 20px;
}
.main-section{
  margin-top: 20px;
}
</style>
