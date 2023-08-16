<template>
  <div class="container-flex">
    <aside>
      <div class="flex--none bg-grey-100">
        <span class="title"><i class="fas fa-plug"></i> {{ $t('message_webhookPageTitle') }}</span>
      </div>
      <div class="flex--grow navs">
        <div id="wh-list" class="px-3">
          <WebhookSelect :selected="curHook ? curHook.uuid : ''" :project="projectName" @item:selected="(item) => handleSelect(item)"/>
        </div>
      </div>
      <div class="flex--none bg-grey-100">
        <button
          type="button"
          class="btn btn-primary btn-full"
          :class="{'btn-primary': isProjectLoaded && noWebhooksForProject && !curHook}"
          @click="handleAddNew">
            <i class="fas fa-plus-circle"/> {{ $t('message_webhookCreateBtn') }}
        </button>
      </div>
    </aside>
    <main id="mainconfig">
      <div>
        <div id="wh-edit" v-if="curHook">
          <div id="wh-header">
                <WebhookTitle :webhook="curHook"/>
                <div style="margin-left: auto;display: flex;align-items: center;">
                  <div><a
                          style="font-weight: 800;"
                          v-if="curHook.id"
                          @click="handleDelete"
                          class="btn btn-danger">{{ $t('message_webhookDeleteBtn') }}</a>
                  </div>
                  <div>
                    <a
                            v-if="!curHook.id"
                            @click="handleCancel"
                            class="btn btn-md btn-default"
                    >{{ $t('message_cancel') }}</a>
                    <btn
                            :disabled="!(dirty || curHook.new)"
                            type="cta"
                            style="margin-left: 5px;font-weight: 800;"
                            @click="handleSave"
                    >{{ $t('message_webhookSaveBtn') }}</btn>
                  </div>
                </div>
          </div>

          <Tabs data-tabkey="webhook-header" style="height: 200px;" :key="curHook.new ? curHook.uuid : ''">
            <Tab :index="0" title="General">
              <div class="wh-edit__body">
                <div  class="form-group">
                  <div class="card card-accent">
                  <div class="card-content">
                    <label>{{ $t('message_webhookPostUrlLabel') }}</label>
                    <div class="help-block">
                      {{$t('message_webhookPostUrlHelp')}}
                    </div>
                    <CopyBox style="max-width: 800px" v-if="!curHook.new" :content="postUrl()"/>
                    <span class="form-control fc-span-adj font-italic" style="height: auto;" v-if="curHook.new">{{$t('message_webhookPostUrlPlaceholder')}}</span>
                  </div>
                  <div class="card-content">
                    <label>{{ $t('message_webhookAuthLabel') }}</label>
                    <div class="help-block"> {{$t('message_webhookGenerateSecretCheckboxHelp')}}</div>
                    <div class="checkbox"><input type="checkbox" v-model="curHook.useAuth" @click="confirmAuthToggle" class="form-control" id="wh-authtoggle"><label for="wh-authtoggle">{{ $t('message_webhookGenerateSecurityLabel') }}</label></div>
                    <div v-if="showRegenButton">
                      <button class="btn btn-sm btn-default" @click="setRegenerate">Regenerate</button>
                      <span v-if="curHook.regenAuth" style="color: var(--color-red); margin-left: 5px"> {{$t('message_webhookRegenClicked')}}</span>
                    </div>
                    <div v-if="hkSecret" >
                      <label style="color: var(--color-red); margin-top: 5px">{{$t('message_webhookSecretMessageHelp')}}</label>
                      <CopyBox style="max-width: 800px" :content="hkSecret"/>
                    </div>
                  </div>
                  </div>
                </div>
                <div class="card">
                <div class="card-content">
                  <div class="form-group"><label>{{ $t('message_webhookNameLabel') }}</label><input v-model="curHook.name" class="form-control" @change="input"></div>
                  <div class="form-group"><label>{{ $t('message_webhookUserLabel') }}</label>
                    <input v-model="curHook.user" class="form-control" v-if="curHook.new">
                    <span class="form-control readonly fc-span-adj" v-else>{{curHook.user}}</span>
                    <div class="help-block">
                      {{$t('message_webhookUserHelp')}}
                    </div>
                  </div>
                  <div class="form-group"><label>{{ $t('message_webhookRolesLabel') }}</label><input v-model="curHook.roles" class="form-control" @change="input">
                    <div class="help-block">
                      {{$t('message_webhookRolesHelp')}}
                    </div>
                  </div>
                  <div class="form-group">
                    <div class="checkbox"><input type="checkbox" v-model="curHook.enabled" class="form-control" id="wh-enabled" @change="input"><label for="wh-enabled">{{ $t('message_webhookEnabledLabel') }}</label></div>
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
                      <button class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" :class="{'btn-default':!curHook.eventPlugin, 'btn-muted':curHook.eventPlugin}"
                        aria-expanded="false" >

                        <plugin-info
                          :detail="curHook.eventPlugin"
                          :show-description="false"
                          v-if="curHook.eventPlugin" />
                        <span v-else>
                          {{$t('message_webhookPluginLabel')}}
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

    </main>
  </div>
</template>

<script>
import {defineComponent} from 'vue'
import axios from 'axios'

import PluginConfig from "../../../../library/components/plugins/pluginConfig.vue"
import PluginInfo from "../../../../library/components/plugins/PluginInfo.vue"

import CopyBox from '../../../../library/components/containers/copybox/CopyBox.vue'
import Tabs from '../../../../library/components/containers/tabs/Tabs.vue'
import Tab from '../../../../library/components/containers/tabs/Tab.vue'
import WebhookSelect from '../../../../library/components/widgets/webhook-select/WebhookSelect.vue'
import KeyStorageSelector from '../../../../library/components/plugins/KeyStorageSelector.vue'

import { getServiceProviderDescription } from '../../../../library/modules/pluginService'

import WebhookTitle from '../components/WebhookTitle.vue'

export default defineComponent({
  name: "WebhooksView",
  components: {
    CopyBox,
    PluginConfig,
    PluginInfo,
    Tabs,
    Tab,
    WebhookSelect,
    WebhookTitle,
    KeyStorageSelector
  },
  inject: ["rootStore", 'registerComponent'],
  data() {
    const projectName = window._rundeck ? window._rundeck.projectName : undefined
    let rdBase = "http://localhost:4440"
    let apiVersion = "33"
    const curUser = ""
    const curUserRoles = ""
    if (window._rundeck && window._rundeck.rdBase && window._rundeck.apiVersion) {
      rdBase = window._rundeck.rdBase;
      apiVersion = window._rundeck.apiVersion
    }
    return {
      webhooks: [],
      webhookPlugins: [],
      curHook: null,
      hkSecret: null,
      origUseAuthVal: false,
      config: null,
      errors: {},
      validation:{valid:true,errors:{}},
      selectedPlugin: null,
      apiBasePostUrl: `${rdBase}api/${apiVersion}/webhook/`,
      customConfigComponent: null,
      showPluginConfig: false,
      projectName,
      dirty: false,
      webhookStore: window._rundeck.rootStore.webhooks,
      curUser,
      curUserRoles,
      rdBase,
    }
  },
  computed: {
    showRegenButton() {
      return !this.hkSecret && this.curHook.useAuth && !this.curHook.new && this.origUseAuthVal
    },
    isProjectLoaded() {
        return this.webhookStore.loaded[this.projectName]
    },
    noWebhooksForProject() {
        return this.webhookStore.webhooksForProject(this.projectName).length === 0
    },
  },
  methods: {
    confirmAuthToggle() {
      this.input()
      if(this.curHook.useAuth) {
        this.$confirm({
          title:"Confirm",
          content:"Are you sure you want to remove this webhook authorization string?"
        }).then(() => {
          //do nothing
        }).catch(() => {
          this.curHook.useAuth = true
        })
      }

    },
    setRegenerate() {
      this.curHook.regenAuth = true
      this.input()
    },
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
      this.ajax("get", `${this.rdBase}webhook/admin/editorData/${this.projectName}`).then(response => {
        this.curUser = response.data.username
        this.curUserRoles = response.data.roles
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
    select(selected, showAuth) {
      if (!this.curHook || !showAuth) {
          this.hkSecret = null
      }
      this.curHook = this.webhookStore.clone(selected)
      this.origUseAuthVal = this.curHook.useAuth

      this.dirty = false
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
      if(this.curHook.useAuth && this.origUseAuthVal === false) {
        this.curHook.regenAuth = true
      }
      if(!webhook.eventPlugin) {
        this.setError("You must select a Webhook plugin before saving")
        return
      }
      webhook.config = this.selectedPlugin.config
      let showAuthString = false
      let resp
      if (webhook.new) {
          showAuthString = webhook.useAuth
          resp = await this.webhookStore.create(webhook)
      } else {
          showAuthString = webhook.regenAuth
          resp = await this.webhookStore.save(webhook)
      }

      const data = resp.parsedBody

      if (!data || data.err) {
        this.setError("Failed to save!\n" + data.err)
        this.setValidation(false, data.errors)
      } else {
        this.setMessage("Saved!")
        if(data.generatedSecurityString) {
          this.hkSecret = data.generatedSecurityString
        }
        this.setValidation(true)
        this.dirty = false
        await this.webhookStore.refresh(this.projectName)

        this.select(this.webhookStore.webhooksByUuid.get(data.uuid), showAuthString)
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
      this.$confirm({
        title:"Confirm",
        content:"Are you sure you want to delete this webhook?",
        okType:"danger"
      }).then(() => {

        this.webhookStore.delete(this.curHook).then(response => {
        const data = response.parsedBody
        if (data.err) {
          this.setError("Failed to delete! " + data.err)
        } else if (response.status !== 200) {
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
      const params = {
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
      this.hkSecret = null
      this.showPluginConfig = false
      this.origUseAuthVal = false
      this.curHook = this.webhookStore.newFromApi({name: "New Hook", user: this.curUser, roles: this.curUserRoles, useAuth: false, enabled: true, project: this.projectName, new: true, config: {}})
      this.config = this.curHook.config
      this.dirty = true
    },
    loadProPlugins() {
      if (window.ProWebhookComponents === undefined)
        return

      for (let [k, comp] of Object.entries(window.ProWebhookComponents)) {
        this.registerComponent(k, comp)
      }
    }
  },
  async mounted() {
    this.loadProPlugins()
    await this.rootStore.plugins.load('WebhookEvent')
    this.webhookPlugins = this.rootStore.plugins.getServicePlugins('WebhookEvent')
  }
})
</script>


<style lang="scss" scoped>
// Layout Classes
.font-heading{
  font-family: Jost, Helvetica, Arial, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}
.container-flex{
  position:relative;
  margin:-2rem;
  display: flex;
  justify-content: space-between;
  height: calc(100vh - 69px);
}
aside{
  height:100%;
  background-color: var(--background-color-lvl3);
  width: 300px;
  flex: none;
  overflow: none;
  border-right:1.5px solid var(--grey-400);
  display: flex;
  flex-direction: column;
  > div{
    border-bottom:1.5px solid var(--grey-400);
    padding: 1.6rem;
    &:last-child{
      border-bottom:0 solid;
    }
  }
  > .navs{
    padding: 12px 0;
    overflow: scroll;
  }
}
main{
  flex-grow: 1;
  background-color: var(--background-color);
  overflow: scroll;
  padding-bottom: 3rem;
}

.bg-grey-100{
  background-color: var(--background-color);
}
.title{
  @extend .font-heading;
  font-weight: 600;
  font-size: 2.2rem;
}


////////////////////////////////////////////////
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
    background-color: var(--background-color-lvl2);
    border-color: var(--background-color);
    border-bottom: 0.1em solid var(--background-color);
  }

  #wh-details {
    background-color: var(--background-color-lvl2);
  }



  #wh-edit {
    display: flex;
    flex-direction: column;
  }

  #wh-header {
    display: flex;
    align-items: center;
    background-color: var(--background-color-accent-lvl2);
    height: 70px;
    padding: 0 2em 0 2em;

    h3 {
      color: var(--font-color);
      font-weight: 700;
      margin: 0;
    }
  }

  :deep([data-tabkey="webhook-header"]) > .rdtabs__tabheader {
    background-color: var(--background-color-accent-lvl2);
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
    color: var(--input-disabled-color);
    background-color: var(--input-disabled-bg-color);
    padding: 8px 12px 6px;
    cursor: not-allowed;
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
    background-color: var(--input-bg-color);
    display: none;
    border: 1px solid var(--input-bg-color);
    padding: 10px;
    border-radius: 3px;
    color: var(--input-color);
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
