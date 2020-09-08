<template>
  <div>

    <div >
      <div class="form-group form-inline">

        <label class="col-sm-2 control-label">
          Average Duration {{$t('scheduledExecution.property.notifyAvgDurationThreshold.label')}}
        </label>
        <div class="col-sm-10  ">
            <input type='text'
                   name="notifyAvgDurationThreshold"
                   v-model="notifyAvgDurationThreshold"
                   id="schedJobNotifyAvgDurationThreshold"
                   class="form-control"
                   size="40"/>
            <span class="help-block">{{ $t('scheduledExecution.property.notifyAvgDurationThreshold.description') }}</span>
        </div>

      </div>
    </div>
    <div >
      <btn @click="addNotification()">Add Notification</btn>
      <div v-if="notifications.length < 1" style="padding: 50px;">
        <p class="text-muted">No Notifications</p>
      </div>
      <div class="list-group" v-else>
        <div v-for="(notif,i) in notifications" class="list-group-item flex-container">
          <div   class="flex-item flex-grow-1">
            {{$t('notification.event.'+notif.trigger)}}
            <div v-if="notif.type==='email'">

              <plugin-config
                  :config="notif.config"
                  :plugin-config="getBuiltinPluginConfig(notif.type)"
                  mode="show"
                  :show-title="true"
                  :show-description="true"
                  :key="'g_'+i+'/'+notif.type+':config'"
                  />
            </div>
            <div v-else-if="notif.type==='url'">

              <plugin-config
                  :config="notif.config"
                  :plugin-config="getBuiltinPluginConfig(notif.type)"
                  mode="show"
                  :show-title="true"
                  :show-description="true"
                  :key="'g_'+i+'/'+notif.type+':config'"
              />
            </div>
            <plugin-config
                serviceName="Notification"
                :provider="notif.type"
                :config="notif.config"
                mode="show"
                :show-title="true"
                :show-description="true"
                :key="'g_'+i+'/'+notif.type+':config'"
                v-else-if="notif.type"
            />
          </div>
          <btn @click="doEditNotification(i)">Edit</btn>
          <btn @click="doDeleteNotification(i)" type="danger">Delete</btn>
        </div>

      </div>
    </div>



    <modal v-model="editModal" :title="$t(editIndex<0?'Create Notification':'Edit Notification')" size="lg">
      <div>
        <div class="form-group"  >
          <label class="col-sm-2 control-label  " >
            Trigger:
          </label>
          <div class="col-sm-10">

            <dropdown ref="dropdown">
              <btn type="simple" class="btn-simple btn-hover  btn-secondary dropdown-toggle">
                <span class="caret"></span>
                &nbsp;
                <span v-if="editNotificationTrigger" class="text-info">
                  {{ $t('notification.event.' + editNotificationTrigger) }}
                </span>
                <span v-else>
                  Select a Trigger
                </span>
              </btn>
              <template slot="dropdown">
                <li v-for="trigger in notifyTypes"
                    @click="editNotificationTrigger=trigger"
                >
                  <a role="button">{{ $t('notification.event.' + trigger) }}</a>
                </li>
              </template>
            </dropdown>

          </div>

        </div>


        <div v-if="editNotificationTrigger" >

          <div class="form-group">
            <label class="col-sm-2 control-label  " >
              Notification Type:
            </label>
            <div class="col-sm-10">
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
                  <li v-for="plugin in pluginProviders" :key="plugin.name">
                    <a role="button" @click="editNotification.type=plugin.name">
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
<!--          <div class="list-group">-->
<!--            <a-->
<!--                v-for="plugin in pluginProviders"-->
<!--                v-bind:key="plugin.name"-->
<!--                href="#"-->
<!--                class="list-group-item"-->
<!--                @click="editNotification.type=plugin.name"-->
<!--            >-->
<!--              <plugin-info-->
<!--                  :detail="plugin"-->
<!--                  :show-description="true"-->
<!--                  :show-extended="false"-->
<!--                  description-css="help-block"-->
<!--              >-->
<!--              </plugin-info>-->
<!--            </a>-->
<!--          </div>-->
          </div>
        </div>
        </div>
        <div v-if="editNotification.type==='email'">

          <plugin-config

              :mode="editIndex===-1 ? 'create':'edit'"
              :plugin-config="getBuiltinPluginConfig(editNotification.type)"
              v-model="editNotification"
              :key="'edit_config'+editIndex+'/'+editNotification.type"
              :show-title="false"
              :show-description="false"
              :validation="editValidation"
          ></plugin-config>

        </div>
        <div v-else-if="editNotification.type==='url'">

          <plugin-config

              :mode="editIndex===-1 ? 'create':'edit'"
              :plugin-config="getBuiltinPluginConfig(editNotification.type)"
              v-model="editNotification"
              :key="'edit_config'+editIndex+'/'+editNotification.type"
              :show-title="false"
              :show-description="false"
              :validation="editValidation"
          ></plugin-config>
        </div>
        <plugin-config

            :mode="editIndex===-1 ? 'create':'edit'"
            :serviceName="'Notification'"
            v-model="editNotification"
            :key="'edit_config'+editIndex+'/'+editNotification.type"
            :show-title="false"
            :show-description="false"
            :validation="editValidation"
            v-else-if="editNotification.type"
        ></plugin-config>

      </div>

      <div slot="footer">
        <btn @click="cancelEditNotification">Cancel</btn>
        <btn @click="saveNotification" type="primary">Save</btn>
      </div>
    </modal>
  </div>
</template>
<script>


import {
  getRundeckContext,
  RundeckContext
} from "@rundeck/ui-trellis"

import Expandable from "@rundeck/ui-trellis/lib/components/utils/Expandable.vue";
import PluginInfo from "@rundeck/ui-trellis/lib/components/plugins/PluginInfo.vue";
import PluginConfig from "@rundeck/ui-trellis/lib/components/plugins/pluginConfig.vue";
import pluginService from "@rundeck/ui-trellis/lib/modules/pluginService";
import PluginValidation from "@rundeck/ui-trellis/lib/interfaces/PluginValidation";

export default {
  name: 'NotificationsEditor',
  props: ['eventBus', 'notificationData'],
  components: {PluginInfo,PluginConfig},
  data () {
    return {
      project: null,
      rdBase: null,
      notifyAvgDurationThreshold:null,
      pluginProviders: [],
      pluginLabels: {},
      notifyTypes:[
          'onsuccess',
          'onfailure',
          'onstart',
          'onavgduration',
          'onretryablefailure'
      ],
      notifications:[],
      editNotificationTrigger:null,
      editNotification:{},
      editValidation:null,
      editIndex:-1,
      editModal:false
    }
  },
  methods:{

    async addNotification(){
      this.editNotificationTrigger = null
      this.editNotification={type:null,config:{}}
      this.editIndex=-1
      this.editModal=true
    },
    async doEditNotification(ndx){
      this.editIndex=ndx
      this.editNotificationTrigger=this.notifications[ndx].trigger
      this.editNotification=this.notifications[ndx]
      this.editModal=true
    },
    async doDeleteNotification(ndx){
      this.notifications.splice(ndx,1)
    },
    async cancelEditNotification(){
      this.editModal=false
      this.editIndex=-1
      this.editNotification={}
      this.editNotificationTrigger=null
    },
    saveNotification(){
      this.editModal=false
      if(this.editIndex<0){
        this.editNotification.trigger=this.editNotificationTrigger
        this.editIndex=-1
        this.notifications.push(Object.assign({},this.editNotification))
        this.editNotification={}
      }else{
        this.editNotification.trigger=this.editNotificationTrigger
        this.notifications[this.editIndex]=Object.assign({},this.editNotification)
        this.editIndex=-1
        this.editNotification={}
      }
    },
    getBuiltinPluginConfig(name){
      return {
        email:{
          name:"email",
          description:'Send an email to multiple recipients, and customize the subject line.',
          title: 'Send Email',
          providerMetadata:{
            faicon:"envelope"
          },
          props:[
            {
              name:'recipients',
              desc:this.$t('notification.email.description'),
              title:this.$t('to'),
              required:true,
            },
            {
              name:'subject',
              desc: this.$t('notification.email.subject.description')
                    + '\n\n[Documentation]('+
                           (this.$t('notification.email.subject.helpLink'))
                           +')',
              title:this.$t('subject'),
              required:true,
            },
            {
              name:'attachLog',
              desc: '',
              title:this.$t('attach.output.log'),
              type:'Boolean'
            },
            {
              name:'attachType',
              desc: 'Attach as a file, or inline to the message',
              title:'Attachment type',
              type:'Select',
              defaultValue:'file',
              allowed:[
                  'file',
                  'inline'
              ],
              selectLabels:{
                file:'As a File',
                inline:'Inline',
              },
              renderingOptions:{

              }
            },
          ]
        },
        url:{
          description: "Send a HTTP POST request to one ore more URLs.",
          icon:"email",
          name:"url",
          title:"Send Webhook",
          providerMetadata:{
            faicon:"globe"
          },
          props:[
            {
              name:'urls',
              desc:this.$t('notification.webhook.field.description'),
              title:this.$t('notification.webhook.field.title'),
              required:true,
              options:{
                displayType:'MULTI_LINE'
              }
            },
            {
              name:'format',
              desc:'',
              title:this.$t('notify.url.format.label'),
              type:'Select',
              required:true,
              allowed:['xml','json'],
              selectLabels:{
                xml:this.$t('notify.url.format.xml'),
                json:this.$t('notify.url.format.json'),
              }
            }
          ]
        }
      }[name]
    },

    getBuiltinProviderDescs(){
      return [
        this.getBuiltinPluginConfig('email'),
        this.getBuiltinPluginConfig('url'),
      ]
    },
    getProviderFor(name){
      return this.pluginProviders.find(p => p.name === name)
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
            this.pluginProviders = [].concat(this.getBuiltinProviderDescs()).concat(data.descriptions);
            this.pluginLabels = data.labels;
          }
        });
  }
}
</script>
