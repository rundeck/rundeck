<template>
  <div>

    <div class="help-block">
      Notifications can be triggered by different events during the Job Execution.
    </div>
    <div >

      <div v-if="notifications.length < 1" >
        <p class="text-muted">No Notifications are defined. Click an event below to add a Notification for that Trigger.</p>
      </div>
      <div v-for="(trigger) in notifyTypes" >
          <div  class="list-group" v-if="hasNotificationsForTrigger(trigger)">
            <div class="list-group-item flex-container flex-align-items-baseline flex-justify-start">
              <span class="flex-item " :class="{'text-secondary':(!hasNotificationsForTrigger(trigger))}">
              <i class="fas" :class="triggerIcons[trigger]"></i>
              {{$t('notification.event.'+trigger)}}
              </span>
              <btn type="simple"
                   class=" btn-hover  btn-secondary"
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
                           size="40"/>
                  </div>
                  <extended-description :text="$t('scheduledExecution.property.notifyAvgDurationThreshold.description')"
                                        class="help-block"/>
                </div>
              </div>
            </div>
            <div v-for="(notif,i) in getNotificationsForTrigger(trigger)" v-if="getNotificationsForTrigger(trigger)" class="list-group-item flex-container">
              <div   class="flex-item flex-grow-1" style="margin-left: 20px">
                <plugin-config
                    serviceName="Notification"
                    :provider="notif.type"
                    :config="notif.config"
                    mode="show"
                    :show-title="true"
                    :show-description="true"
                    :key="'g_'+i+'/'+notif.type+':config'"
                />
              </div>


              <dropdown ref="dropdown" menu-right>
                <btn type="simple" class=" btn-hover  btn-secondary dropdown-toggle">
                  <span class="caret"></span>
                </btn>
                <template slot="dropdown">
                  <li @click="doDeleteNotification(notif)">
                    <a role="button">
                      {{$t('Delete')}}
                    </a>
                  </li>
                </template>
              </dropdown>
              <btn type="secondary" size="sm" @click="doEditNotification(notif)">Edit</btn>
            </div>
          </div>
          <div v-else class="list-placeholder">
            <btn type="simple"
                 class=" btn-hover  btn-secondary"
                 size="md" @click="addNotification(trigger)">
              <i class="fas" :class="triggerIcons[trigger]"></i>
              &nbsp;
              {{$t('notification.event.'+trigger)}}
            </btn>
          </div>
        </div>

    </div>



    <modal v-model="editModal" :title="$t(editIndex<0?'Create Notification':'Edit Notification')" size="lg">
      <div>
        <div class="form-group"  >
          <label class="col-sm-2 control-label  " >
            Trigger
          </label>
          <div class="col-sm-10 form-control-static">

            <dropdown ref="dropdown">
              <btn type="simple" class=" btn-hover  btn-secondary dropdown-toggle">
                <span class="caret"></span>
                &nbsp;
                <span v-if="editNotificationTrigger" class="text-primary">
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
              <dropdown ref="dropdown">
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
                    <a role="button" @click="setEditNotificationType(plugin.name)">
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
            :mode="editIndex===-1 ? 'create':'edit'"
            :serviceName="'Notification'"
            v-model="editNotification"
            :key="'edit_config'+editIndex+'/'+editNotification.type"
            :show-title="false"
            :show-description="false"
            :validation="editValidation"
        ></plugin-config>

      </div>

      <div slot="footer">
        <btn @click="cancelEditNotification">{{ $t('Cancel') }}</btn>
        &nbsp;
        <btn @click="saveNotification" type="primary"
             :disabled="!editNotificationTrigger || !editNotification.type"
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


import {
  getRundeckContext,
  RundeckContext
} from "@rundeck/ui-trellis"

import PluginInfo from "@rundeck/ui-trellis/lib/components/plugins/PluginInfo.vue";
import PluginConfig from "@rundeck/ui-trellis/lib/components/plugins/pluginConfig.vue";
import pluginService from "@rundeck/ui-trellis/lib/modules/pluginService";
import ExtendedDescription from "@rundeck/ui-trellis/lib/components/utils/ExtendedDescription.vue";
import Vue from 'vue'

export default {
  name: 'NotificationsEditor',
  props: ['eventBus', 'notificationData'],
  components: {PluginInfo,PluginConfig,ExtendedDescription},
  data () {
    return {
      project: null,
      rdBase: null,
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
    async doDeleteNotification(notif){
      let ndx=this.notifications.findIndex(n=>n===notif)
      if(ndx>=0){
        this.notifications.splice(ndx,1)
      }
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
    async saveNotification(){
      if(!this.editNotificationTrigger){
        this.editError='Choose a Trigger'
        return
      }
      if(!this.editNotification.type){
        this.editError='Choose a Notification Type'
        return
      }
      const validation = await pluginService.validatePluginConfig(
          'Notification',
          this.editNotification.type,
          this.editNotification.config
      )
      if (!validation.valid) {
        this.editValidation = validation
        return
      }
      this.editModal=false
      if(this.editIndex<0){
        this.editNotification.trigger=this.editNotificationTrigger
        this.editIndex=-1
        this.notifications.push(Object.assign({},this.editNotification))
        this.editNotification={}
      }else{
        this.editNotification.trigger=this.editNotificationTrigger
        //nb: use Vue.set to trigger watchers
        Vue.set(this.notifications,this.editIndex,this.editNotification)
        this.editIndex=-1
        this.editNotification={}
      }
    },
    getProviderFor(name){
      return this.pluginProviders.find(p => p.name === name)
    },
    getNotificationsForTrigger(trigger){
      return this.notifications.filter(s=>s.trigger===trigger)
    },
    hasNotificationsForTrigger(trigger){
      return this.notifications.findIndex(s=>s.trigger===trigger)>=0
    }
  },
  watch:{
    notifications(){
      this.$emit('changed',this.notifications)
    }
  },
  async mounted () {
    if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
      this.rdBase = window._rundeck.rdBase
      this.project = window._rundeck.projectName
      this.notifications = [].concat(this.notificationData.notifications || [])
      this.notifyAvgDurationThreshold = this.notificationData.notifyAvgDurationThreshold
    }
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
<style lang="scss">
.list-group-item {
  &.list-group-item-secondary {
    border-width: 0;
  }
}
.list-placeholder{
  margin-bottom: 20px;
}
</style>
