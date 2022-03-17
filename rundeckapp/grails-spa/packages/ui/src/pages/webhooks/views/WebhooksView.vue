<template>
<div id="wh-view" style="display: flex;flex-direction: column; height: 100%; overflow: hidden;">
  <div id="wh-title" class="screen-title" style="display: flex;">
    <span class="text-h3"><i class="fas fa-plug"></i> {{ $t('message.webhookPageTitle') }}</span>
    <div style="margin-left: auto;">
      <a class="btn"
        :class="{'btn-cta': this.rootStore.webhooks.loaded.get(projectName) && this.rootStore.webhooks.webhooksForProject(projectName).length == 0 && !this.curHook}"
        style="font-weight: 800"
        @click="handleAddNew"><i class="fas fa-plus-circle"/> {{ $t('message.webhookCreateBtn') }}</a>
    </div>
  </div>
  
  <div style="display: flex; height: 100%;overflow: hidden;">
    <div id="wh-list" style="flex-basis: 250px;flex-grow: 0; padding: 20px;overflow-x: hidden;overflow-y: auto;">
      <WebhookPicker :selected="curHook ? curHook.uuid : ''" :project="projectName" @item:selected="(item) => handleSelect(item)"/>
    </div>

    <div class="wh-details" style="flex-grow: 1;overflow-y: auto;overflow-x: hidden; height: 100%">
      <div>
        <div id="wh-edit" v-if="curHook">
          <div id="wh-header">
                <WebhookTitle :webhook="this.curHook"/>
                <div style="margin-left: auto;display: flex;align-items: center;">
                  <div><a
                          style="font-weight: 800;"
                          v-if="curHook.id"
                          @click="handleDelete"
                          class="btn btn-transparent">{{ $t('message.webhookDeleteBtn') }}</a>
                  </div>
                  <div>
                    <a
                            v-if="!curHook.id"
                            @click="handleCancel"
                            class="btn btn-md "
                    >{{ $t('message.cancel') }}</a>
                    <btn
                            :disabled="!(dirty || curHook.new)"
                            type="cta"
                            style="margin-left: 5px;font-weight: 800;"
                            @click="handleSave"
                    >{{ $t('message.webhookSaveBtn') }}</btn>
                  </div>
                </div>
          </div>

          <Tabs data-tabkey="webhook-header" style="height: 200px;" :key="curHook.new ? curHook.uuid : ''">
            <Tab :index="0" title="General">
              <div class="wh-edit__body">
                <div  class="form-group">
                  <div class="card wh-url-card">
                  <div class="card-content">
                    <label>{{ $t('message.webhookPostUrlLabel') }}</label>
                    <div class="help-block">
                      {{$t('message.webhookPostUrlHelp')}}
                    </div>
                    <CopyBox style="max-width: 800px" v-if="!curHook.new" :content="postUrl()"/>
                    <span class="form-control fc-span-adj font-italic" style="height: auto;" v-if="curHook.new">{{$t('message.webhookPostUrlPlaceholder')}}</span>
                  </div>
                  </div>
                </div>
                <div class="card">
                <div class="card-content">
                  <div class="form-group"><label>{{ $t('message.webhookNameLabel') }}</label><input v-model="curHook.name" class="form-control"></div>
                  <div class="form-group"><label>{{ $t('message.webhookUserLabel') }}</label>
                    <input v-model="curHook.user" class="form-control" v-if="curHook.new">
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
                          :detail="curHook.eventPlugin"
                          :show-description="false"
                          v-if="curHook.eventPlugin" />
                        <span v-else>
                          {{$t('message.webhookPluginLabel')}}
                        </span>
                        <span class="caret"></span>
                      </button>
                      <ul class="dropdown-menu ">

                        <li v-for="plugin in webhookPlugins" v-bind:key="plugin.id">
                          <a href="#" @click="setSelectedPlugin(false,plugin)">
                            <plugin-info :detail="plugin" />
                          </a>
                        </li>
                      </ul>
                    </div>
                  </div>
                  <div v-if="curHook.eventPlugin">
                    <span>{{curHook.eventPlugin.description}}</span>
                  </div>
                </div>
                <div v-if="selectedPlugin && showPluginConfig" class="new-section">
                  <div class="card" style="padding: 20px;" v-if="!customConfigComponent">
                    <plugin-config
                      @change="input"
                      :mode="'edit'"
                      :serviceName="'WebhookEvent'"
                      v-model="selectedPlugin"
                      :provider="curHook.eventPlugin.name"
                      :key="curHook.name"
                      :show-title="false"
                      :show-description="false"
                      :validation="validation"/>
                  </div>
                  <component v-else
                    :is="customConfigComponent"
                    :webhook="curHook"
                    :pluginConfig="curHook.config"
                    :errors="errors"
                    @change="input"></component>
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

import {observable, autorun, reaction} from 'mobx'
import {deepObserve} from 'mobx-utils'
import {observer} from 'mobx-vue'

import PluginConfig from "@rundeck/ui-trellis/lib/components/plugins/pluginConfig.vue"
import PluginInfo from "@rundeck/ui-trellis/lib/components/plugins/PluginInfo.vue"

import CopyBox from '@rundeck/ui-trellis/lib/components/containers/copybox/CopyBox.vue'
import Tabs from '@rundeck/ui-trellis/lib/components/containers/tabs/Tabs'
import Tab from '@rundeck/ui-trellis/lib/components/containers/tabs/Tab'
import WebhookPicker from '@rundeck/ui-trellis/lib/components/widgets/webhook-select/WebhookSelect.vue'

import {getServiceProviderDescription,
  getPluginProvidersForService} from '@rundeck/ui-trellis/lib/modules/pluginService'

import {ServiceType} from '@rundeck/ui-trellis/lib/stores/Plugins'


import WebhookTitle from '../components/WebhookTitle.vue'

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

export default observer(Vue.extend({
  name: "WebhooksView",
  components: {
    CopyBox,
    PluginConfig,
    PluginInfo,
    Tabs,
    Tab,
    WebhookPicker,
    WebhookTitle
  },
  inject: ["rootStore"],
  data() {
    return {
      webhooks: [],
      webhookPlugins: [],
      curHook: null,
      config: null,
      errors: {},
      validation:{valid:true,errors:{}},
      selectedPlugin: null,
      apiBasePostUrl: `${rdBase}api/${apiVersion}/webhook/`,
      customConfigComponent: null,
      showPluginConfig: false,
      projectName: projectName,
      dirty: false
    }
  },
  methods: {
    input() {
      this.dirty = true
    },
    toggleUrl(hookId, show) {
      if(show) document.getElementById("whc-" + hookId).style.display = "block";
      else document.getElementById("whc-" + hookId).style.display = "none";
    },
    postUrl() {
      if(this.curHook.new) {
        return "Webhook endpoint url will appear here after saving."
      }
      return this.generatePostUrl(this.curHook)
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
    handleSelect(selected) {
      this.cleanAction(() => this.select(selected))
    },
    select(selected) {
      this.curHook = this.rootStore.webhooks.clone(selected)

      this.dirty = false

      reaction(() => {
        return JSON.stringify(this.curHook.toApi())
      }, (data) => {
        this.dirty = true
      })
      
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
      getServiceProviderDescription("WebhookEvent", this.curHook.eventPlugin.artifactName).then(data => {
        this.customConfigComponent = data.vueConfigComponent
        this.showPluginConfig = this.customConfigComponent || data.props.length > 0
      })
    },
    async handleSave() {
      const webhook = this.curHook

      if(!webhook.eventPlugin) {
        this.setError("You must select a Webhook plugin before saving")
        return
      }
      webhook.config = this.selectedPlugin.config

      let resp
      if (webhook.new)
        resp = await this.rootStore.webhooks.create(webhook)
      else
        resp = await this.rootStore.webhooks.save(webhook)

      const data = resp.parsedBody

      if (data?.err) {
        this.setError("Failed to save!" + data.err)
        this.setValidation(false, data.errors)
      } else {
        this.setMessage("Saved!")
        this.setValidation(true)
        this.dirty = false
        await this.rootStore.webhooks.refresh(this.projectName)
        this.select(this.rootStore.webhooks.webhooksByUuid.get(webhook.uuid))
      }
    },
    handleCancel() {
      this.cleanAction(this.cancel)
    },
    cancel() {
      this.curHook = null
      this.config = null
      this.dirty = false
    },
    handleDelete() {
      var self = this
      self.$confirm({
        title:"Confirm",
        content:"Are you sure you want to delete this webhook?"
      }).then(() => {

        this.rootStore.webhooks.delete(this.curHook).then(response => {
        const data = response.parsedBody
        if (data.err) {
          this.setError("Failed to delete! " + data.err)
        } else if (response.status != 200) {
          this.setMessage('Failed')
        } else {
          this.curHook = null
          this.selectedPlugin = null
          this.setMessage("Deleted!")
        }
      }).catch(err => {
        this.setError("Failed to delete! " + err)
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
    cleanAction(callback) {
      if (this.dirty) {
        this.$confirm({
          title:"Unsaved Changes",
          content:"You have unsaved changes, do you wish to continue?"
        }).then(() => {
          callback()
        })
      } else {
        callback()
      }
    },
    handleAddNew() {
      this.cleanAction(() => this.addNewHook())
    },
    addNewHook() {
      this.showPluginConfig = false
      this.curHook = this.rootStore.webhooks.newFromApi({name: "New Hook", user: curUser, roles: curUserRoles, enabled: true, project: projectName, new: true, config: {}})
      this.config = this.curHook.config
      this.dirty = true
    },
    loadProPlugins() {
      if (window.ProWebhookComponents == undefined)
        return

      for (let [k, comp] of Object.entries(window.ProWebhookComponents)) {
        Vue.component(k, comp)
      }
    }
  },
  async mounted() {
    this.loadProPlugins()
    await this.rootStore.plugins.load('WebhookEvent')
    this.webhookPlugins = this.rootStore.plugins.getServicePlugins('WebhookEvent')
  }
}))
</script>

<style scoped>

</style>

<style lang="scss" scoped>
  #wh-view {
    box-shadow: 0px 4px 14px rgba(0, 0, 0, 0.11);
  }

  #wh-title {
    display: flex;
    align-items: center;
    padding: 0 2em 0 2em;
    flex-basis: 70px;
    flex-grow: 0;
    flex-shrink: 0;
    border-color: #d7d7d7;
    border-bottom: 0.1em solid #d7d7d7;
    h3 {
      margin: 0;
      padding: 0;
      font-weight: 700;
      color: black;
    }
  }

  #wh-list {
    background-color: #f4f5f7;
    border-right: 0.1em solid #d3dbe5;
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

  ::v-deep [data-tabkey="webhook-header"] > .rdtabs__tabheader {
    background-color: #f7f7f7;
    border: none;
    padding: 0 2em;
  }

  .wh-edit__body {
    padding: 0 2em 0 2em;
    margin-top: 20px;
  }

  .wh-card {
    padding: 1em;
  }

  .wh-url-card {
    background: #D8F1EE;
    border: 0.1em solid #9DDCD4;
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
