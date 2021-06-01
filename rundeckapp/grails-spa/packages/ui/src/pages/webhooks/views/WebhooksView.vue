<template>
<div style="display: flex;flex-direction: column; height: 100%; overflow: hidden;">
  <div id="wh-title" class="screen-title" style="display: flex;">

    <h3>{{ $t('message.webhookPageTitle') }}</h3>
    <div style="margin-left: auto;">
      <a class="btn btn-primary fr" @click="addNewHook">{{ $t('message.webhookCreateBtn') }}</a>
    </div>
  </div>
  
  <div style="display: flex; height: 100%;overflow: hidden;">
    <div id="wh-list" style="flex-basis: 250px;flex-grow: 0; padding: 10px;overflow-x: hidden;overflow-y: auto;">
      <div style="width:100%; min-height: 500px">
        <div style="margin-top: 0; padding-top: 0;">
          <table class="table table-striped">
            <thead>
            <tr>
              <th scope="col">{{ $t('message.webhookListNameHdr') }}</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="hook in webhooks" :key="hook.id" @click="select(hook)" class="clickable"
                v-bind:class="{selected: curHook === hook}">
              <td>
                <div>{{hook.name}} <i class="far fa-clipboard fr clip" @click="copyUrl(hook.id, $event)" @mouseover="toggleUrl(hook.id,true)" @mouseout="toggleUrl(hook.id,false)" title="Copy Url To Clipboard">
                  <div v-bind:id="'whc-'+hook.id" class="post-url-copy">{{generatePostUrl(hook)}}</div>
                </i>
                </div>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="wh-details" style="flex-grow: 1;overflow-y: auto;overflow-x: hidden; height: 100%">
      <div>
        <div id="wh-edit" v-if="curHook">
          <div id="wh-header">
                <h3>{{curHook.name}}</h3>
                <div style="margin-left: auto;display: flex;align-items: center;">
                  <div><a
                          v-if="curHook.id"
                          @click="handleDelete"
                          class="btn">{{ $t('message.webhookDeleteBtn') }}</a>
                  </div>
                  <div>
                    <a
                            v-if="!curHook.id"
                            @click="handleCancel"
                            class="btn btn-md "
                    >{{ $t('message.cancel') }}</a>
                    <a
                            class="btn btn-cta"
                            style="margin-left: 5px"
                            @click="handleSave"
                    >{{ $t(curHook.isNew?'message.webhookCreateBtn':'message.webhookSaveBtn') }}</a>
                  </div>
                </div>
          </div>

          <Tabs style="height: 200px;" :key="curHook.uuid">
            <Tab :index="0" title="General">
              <div class="wh-edit__body">
                <div  class="form-group">
                  <div class="well well-sm">
                    <label>{{ $t('message.webhookPostUrlLabel') }}</label>
                    <span class="form-control fc-span-adj" style="height: auto;" v-if="!curHook.isNew">{{postUrl}}</span>
                    <span class="form-control fc-span-adj font-italic" style="height: auto;" v-if="curHook.isNew">{{$t('message.webhookPostUrlPlaceholder')}}</span>
                    <div class="help-block">
                      {{$t('message.webhookPostUrlHelp')}}
                    </div>
                  </div>
                </div>
                <div class="form-group"><label>{{ $t('message.webhookNameLabel') }}</label><input v-model="curHook.name" class="form-control"></div>
                <div class="form-group"><label>{{ $t('message.webhookUserLabel') }}</label>
                  <input v-model="curHook.user" class="form-control" v-if="curHook.isNew">
                  <span class="form-control readonly fc-span-adj" v-else>{{curHook.user}}</span>
                  <div class="help-block">
                    {{$t('message.webhookUserHelp')}}
                  </div>
                </div>
                <div class="form-group"><label>{{ $t('message.webhookRolesLabel') }}</label><input v-model="curHook.roles" class="form-control">
                  <div class="help-block">
                    {{$t('message.webhookRolesHelp')}}
                  </div>
                </div>
                <div class="form-group">
                  <div class="checkbox"><input type="checkbox" v-model="curHook.enabled" class="form-control"><label>{{ $t('message.webhookEnabledLabel') }}</label></div>
                </div>
              </div>
            </Tab>
            <Tab :index="1" title="Handler Configuration">
              <div class="wh-edit__body">
                <div class="card" style="padding: 1em;">
                  <div class="form-group">
                    <div class="btn-group">
                      <button class="btn dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" :class="{'btn-info':!curHook.eventPlugin, 'btn-muted':curHook.eventPlugin}"
                        aria-expanded="false" >

                        <plugin-info
                          :detail="getPluginDescription(curHook.eventPlugin)"
                          :show-description="false"
                          v-if="curHook.eventPlugin" />
                        <span v-else>
                          {{$t('message.webhookPluginLabel')}}
                        </span>
                        <span class="caret"></span>
                      </button>
                      <ul class="dropdown-menu ">

                        <li v-for="plugin in webhookPlugins" v-bind:key="plugin.name">
                          <a href="#" @click="setSelectedPlugin(false,plugin.name)">
                            <plugin-info :detail="plugin" />
                          </a>
                        </li>
                      </ul>
                    </div>
                  </div>
                  <div v-if="selectedPlugin && curHook.eventPlugin">
                    <plugin-info
                        :detail="getPluginDescription(curHook.eventPlugin)"
                        :show-description="true"
                        :show-icon="false"
                        :show-title="false" />
                  </div>
                </div>
                <div v-if="selectedPlugin && showPluginConfig" class="new-section">
                  <plugin-config
                    :mode="'edit'"
                    :serviceName="'WebhookEvent'"
                    v-model="selectedPlugin"
                    :provider="curHook.eventPlugin"
                    :key="curHook.name"
                    :show-title="false"
                    :show-description="false"
                    :validation="validation"
                    v-if="!customConfigComponent"/>
                  <component :is="customConfigComponent" v-else :webhook="curHook" :pluginConfig="curHook.config" :errors="errors"></component>
                </div>
              </div>
            </Tab>
          </Tabs>
        </div>
      </div>
    </div>
  </div>
</div>
</template>

<script>
import Vue from 'vue'
import VueI18n from 'vue-i18n'
import i18n from '../i18n'
import axios from 'axios'
import PluginConfig from "@rundeck/ui-trellis/lib/components/plugins/pluginConfig.vue"
import PluginInfo from "@rundeck/ui-trellis/lib/components/plugins/PluginInfo.vue"

import Tabs from '@rundeck/ui-trellis/lib/components/containers/tabs/Tabs'
import Tab from '@rundeck/ui-trellis/lib/components/containers/tabs/Tab'

import {getServiceProviderDescription,
  getPluginProvidersForService} from '@rundeck/ui-trellis/lib/modules/pluginService'

var rdBase = "http://localhost:4440"
var apiVersion = "33"
var curUser = ""
var curUserRoles = ""
if (window._rundeck && window._rundeck.rdBase && window._rundeck.apiVersion) {
  rdBase = window._rundeck.rdBase;
  apiVersion = window._rundeck.apiVersion
}
var proPluginList = window.PRO_WEBHOOK_COMPONENTS ? window.PRO_WEBHOOK_COMPONENTS : []
var projectName = window._rundeck ? window._rundeck.projectName : undefined

var _i18n = i18n
var lang = window._rundeck.language
var i18nInstance = new VueI18n({
  messages: {
    [lang]: {
      ...(_i18n[lang] || i18n.en),
      ...(window.Messages[lang])
    }
  }
})

export default {
  name: "WebhooksView",
  components: {
    PluginConfig,
    PluginInfo,
    Tabs,
    Tab
  },
  data() {
    return {
      webhooks: [],
      webhookPlugins: [],
      curHook: null,
      errors: {},
      validation:{valid:true,errors:{}},
      selectedPlugin: null,
      apiBasePostUrl: `${rdBase}api/${apiVersion}/webhook/`,
      customConfigComponent: null,
      showPluginConfig: false
    }
  },
  computed: {
    postUrl() {
      if(this.curHook.isNew) {
        return "Webhook endpoint url will appear here after saving."
      }
      return this.generatePostUrl(this.curHook)
    }
  },
  methods: {
    toggleUrl(hookId, show) {
      if(show) document.getElementById("whc-" + hookId).style.display = "block";
      else document.getElementById("whc-" + hookId).style.display = "none";
    },
    copyUrl(hookId, evt) {
      let el = document.getElementById("whc-" + hookId);
      let range = document.createRange();
      range.selectNode(el);
      window.getSelection().removeAllRanges();
      window.getSelection().addRange(range);
      document.execCommand("copy")
      evt.stopImmediatePropagation()
      window.getSelection().removeAllRanges();
      this.toggleUrl(hookId, false)
      this.setMessage("Copied url to clipboard")
    },
    generatePostUrl(hook) {
      return `${this.apiBasePostUrl}${hook.authToken}#${encodeURI(hook.name.replace(/ /g, '_'))}`
    },
    setMessage(msg) {
      this.$notify({content: msg, dismissible: false})
    },
    setError(err) {
      this.$notify.error({
        title: "Error",
        icon: "",
        customClass: "dismiss-positioner",
        content: err,
        duration: 0
      })
    },
    closePopup() {
      this.popup.message = null;
      this.popup.error = null;
      this.popup.showing = false;
    },
    getHooks() {
      this.ajax("get", `${rdBase}webhook/admin/editorData/${projectName}`).then(response => {
        curUser = response.data.username
        curUserRoles = response.data.roles
        this.webhooks = response.data.hooks
        if (this.curHook) {
          this.curHook = this.webhooks.find(hk => hk.id === this.curHook.id)
          if(this.curHook) this.setSelectedPlugin(true)
        }
      })
    },
    setValidation(valid,errors={}){
      this.validation = {valid:valid, errors}
      this.errors=errors
    },
    select(selected) {
      this.curHook = selected
      this.setValidation(true)
      this.setSelectedPlugin(true)
    },
    getPluginDescription(type){
      if(!type){
        return null
      }
      return this.webhookPlugins.find(plugin=>plugin.name===type)
    },
    setSelectedPlugin(preserve, type) {
      if(type){
        this.curHook.eventPlugin=type
      }
      if(!preserve) this.curHook.config = {}
      this.selectedPlugin = {type: this.curHook.eventPlugin, config: this.curHook.config}
      if(!preserve){
          this.setValidation(true)
      }
      getServiceProviderDescription("WebhookEvent", this.curHook.eventPlugin).then(data => {
        this.customConfigComponent = data.vueConfigComponent
        this.showPluginConfig = this.customConfigComponent || data.props.length > 0
      })
    },
    handleSave() {
      if(!this.curHook.eventPlugin) {
        this.setError("You must select a Webhook plugin before saving")
        return
      }
      this.curHook.config = this.selectedPlugin.config
      this.ajax("post", `${rdBase}webhook/admin/save`, this.curHook).then(response => {
        if (response.data.err) {
          this.setError("Failed to save! " + response.data.err)

          this.setValidation(false, response.data.errors)
        } else {
          this.setMessage("Saved!")
          this.setValidation(true)
          this.getHooks()
        }
      }).catch(err => {
        if (err.response.data.err) {
          this.setError("Failed to save! " + err.response.data.err)
          this.setValidation(false, err.response.data.errors)
        }
      })
    },
    handleCancel(){
      this.curHook=null
    },
    handleDelete() {
      var self = this
      self.$confirm({
        title:"Confirm",
        content:"Are you sure you want to delete this webhook?"
      }).then(() => {
      this.ajax("delete", `${rdBase}webhook/admin/delete/${this.curHook.id}`).then(response => {
        if (response.data.err) {
          this.setError("Failed to delete! " + response.data.err)
        } else {
          this.curHook = null
          this.selectedPlugin = null
          this.getHooks()
          this.setMessage("Deleted!")
        }
      }).catch(err => {
        if (err.response.data.err) {
          this.setError("Failed to delete! " + err.response.data.err)
        }
      })}).catch(() => {})
    },
    ajax(method, url, payload) {
      var params = {
        method: method,
        headers: {
          "x-rundeck-ajax": true
        },
        url: url,
        withCredentials: true
      }
      if(payload) {
        params.data = JSON.stringify(payload)
        params.headers["Content-Type"] = "application/json"
      }
      return axios(params)
    },
    addNewHook() {
      this.showPluginConfig = false
      this.curHook = {name: "New Hook", user: curUser, roles: curUserRoles, enabled: true, project: projectName, isNew: true, config: {}}
    },
    loadProPlugins() {
      if (window.ProWebhookComponents == undefined)
        return

      for (let [k, comp] of Object.entries(window.ProWebhookComponents)) {
        Vue.component(k, comp)
      }
    }
  },
  mounted() {
    getPluginProvidersForService("WebhookEvent").then(data => {
      if(data.service){
        this.webhookPlugins = data.descriptions
      }
    })
    this.getHooks()
    this.loadProPlugins()
  }
}
</script>

<style scoped>

</style>

<style lang="scss" scoped>
  #wh-title {
    display: flex;
    align-items: center;
    padding: 0 2em 0 2em;
    flex-basis: 70px;
    flex-grow: 0;
    flex-shrink: 0;
    border-color: #d7d7d7;
    border-bottom: 2px solid #d7d7d7;
    h3 {
      margin: 0;
      padding: 0;
      font-weight: 700;
      color: black;
    }
  }

  #wh-list {
    background-color: #f4f5f7;
    border-right: 2px solid #d3dbe5;
    flex-shrink: 0;
  }

  #wh-edit {
    display: flex;
    flex-direction: column;
  }

  #wh-header {
    display: flex;
    align-items: center;
    background-color: #f7f7f7;
    height: 70px;
    padding: 0 2em 0 2em;

    h3 {
      color: black;
      font-weight: 700;
      margin: 0;
    }
  }

  ::v-deep #wh-edit .rdtabs__tabheader {
    background-color: #f7f7f7;
    border-bottom: none;
    // padding-left: 20px;
  }

  .wh-edit__body {
    padding: 0 2em 0 2em;
    margin-top: 20px;
  }

  .add-btn {
    padding: 2px 6px;
    border: 1px solid #ddd;
    cursor: pointer;
    font-size: 1.5em;
    color: green;
  }

  .clickable {
    cursor: pointer;
  }

  .selected {
    background-color: #4684b2 !important;
    color: #fff !important;
  }

  .fr {
    float: right;
  }

  .readonly {
    color: #555;
    background-color: #eee;
    padding: 8px 12px 6px;
  }

  .form-margin {
    margin: 10px 0 12px;
  }

  .fc-span-adj {
    padding: 10px 12px;
  }
  .clip {
    position: relative;
    color: #777;
  }
  .post-url-copy {
    position: absolute;
    top: -45px;
    left: -200px;
    background-color: #fefefe;
    display: none;
    border: 1px solid #999;
    padding: 10px;
    border-radius: 3px;
    color: #636363;
    z-index: 999;
    font-family: 'Muli', Arial, sans-serif;
  }
  .section-separator{
    border-top:1px solid #f0f0f0;
  }
  .new-section{
    padding-top:12px;
    margin-top:12px;
  }
</style>
