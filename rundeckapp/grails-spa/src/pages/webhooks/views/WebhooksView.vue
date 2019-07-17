<template>
  <div>
    <h3>Webhook Management</h3>
    <div class="row">
      <div class="col-xs-12">
        <div class="artifact-grid row row-flex row-flex-wrap">
          <div class="col-sm-4">
            <div class="card" style="width:100%; min-height: 500px">
              <div class="card-header">
                <a class="btn btn-sm btn-success fr" @click="addNewHook">Add</a>
                <h5 style="margin:0;">Webhooks</h5>
              </div>
              <hr>
              <div class="card-content" style="margin-top: 0; padding-top: 0;">
                <table class="table table-striped">
                  <thead>
                  <tr>
                    <th scope="col">Name</th>
                    <th scope="col">Event Handler Plugin</th>
                  </tr>
                  </thead>
                  <tbody>
                  <tr v-for="hook in webhooks" :key="id" @click="select(hook)" class="clickable"
                      v-bind:class="{selected: curHook === hook}">
                    <td>{{hook.name}}</td>
                    <td>{{hook.eventPlugin}}</td>
                  </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
          <div class="col-sm-8 details-output">
            <div
              class="flex-col"
            >
              <div class="card">
                <div class="card-header">
                  <div class="popup-positioner">
                    <div class="popup" v-if="popup.showing">
                      <div class="close-popup"><span @click="closeOverlay" class="popup-close-btn">x</span></div>
                      <div v-if="popup.message" class="popup-msg">{{popup.message}}</div>
                      <div v-if="popup.error" class="popup-error">{{popup.error}}</div>
                    </div>
                  </div>
                  <h5 style="margin:0;">Webhook Detail</h5>
                </div>
                <hr>
                <div class="card-content">
                  <div v-if="curHook">
                    <div><label>Post Url:</label><span v-if="curHook.authToken" class="form-control">{{apiBasePostUrl}}{{curHook.name}}/{{curHook.authToken}}</span>
                    </div>
                    <div><label>Webhook Name:</label><input v-model="curHook.name" class="form-control"></div>
                    <div><label>Webhook User:</label><input v-model="curHook.user" class="form-control"></div>
                    <div><label>Webhook Roles:</label><input v-model="curHook.roles" class="form-control"></div>
                    <div><label>Webhook Event Plugin:</label><select v-model="curHook.eventPlugin"
                                                                     @change="setSelectedPlugin()" class="form-control">
                      <option v-for="plugin in webhookPlugins" v-bind:value="plugin.name">{{plugin.title}}</option>
                    </select></div>
                    <div v-if="selectedPlugin && showPluginConfig">
                      <h5>Plugin Configuration</h5>
                      <hr>
                      <plugin-config
                        :mode="'edit'"
                        :serviceName="'WebhookEvent'"
                        v-model="selectedPlugin"
                        :provider="curHook.eventPlugin"
                        :key="curHook.eventPlugin"
                        :show-title="false"
                        :show-description="false"
                      >
                      </plugin-config>
                    </div>
                    <div class="row" style="margin-top: 10px;">
                      <div class="col-sm-6">
                        <a
                          class="btn btn-md btn-success"
                          @click="handleSave"
                        >Save Config</a>
                      </div>
                      <div class="col-sm-6 text-right"><a
                        v-if="curHook.id"
                        @click="handleDelete"
                        class="btn btn-md btn-danger"
                      >Delete Webhook</a></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="card">
          <div class="card-header">
            <h5 style="margin:0;">Plugin Config</h5>
          </div>
          <hr style="margin-bottom:0;">
          <div class="card-content">
            <div v-if="selectedPlugin && selectedPlugin.type == 'pagerduty-run-job' ">
                <pager-duty-config :selectedPlugin="selectedPlugin" :curHook="selectedPlugin"></pager-duty-config>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import PagerDutyConfig from './PagerDutyConfig'
  import axios from 'axios'
  import PluginConfig from "@rundeck/ui-trellis/src/components/plugins/pluginConfig.vue"
  import {getServiceProviderDescription,
          getPluginProvidersForService} from '@rundeck/ui-trellis/src/modules/pluginService'

  var rdBase = "http://localhost:4440"
  var apiVersion = "31"
  var curUser = ""
  var curUserRoles = ""
  if (window._rundeck && window._rundeck.rdBase && window._rundeck.apiVersion) {
    rdBase = window._rundeck.rdBase;
    apiVersion = window._rundeck.apiVersion
  }
  export default {
    name: "WebhooksView",
    components: {PluginConfig, PagerDutyConfig},
    data() {
      return {
        webhooks: [],
        webhookPlugins: [],
        curHook: null,
        showPluginConfig: false,
        selectedPlugin: null,
        apiBasePostUrl: `${rdBase}api/${apiVersion}/webhook/`,
        popup: {
          error: null,
          showing: false,
          message: null
        }
      }
    },
    methods: {
      setMessage(msg) {
        this.popup.message = msg
        this.popup.showing = true
        setTimeout(() => {
          this.popup.showing = false
          this.popup.message = ""
        }, 2000)
      },
      setError(err) {
        this.popup.error = err
        this.popup.showing = true
      },
      closeOverlay() {
        this.popup.message = null;
        this.popup.error = null;
        this.popup.showing = false;
      },
      getHooks() {
        axios({
          method: "get",
          headers: {
            "x-rundeck-ajax": true
          },
          url: `${rdBase}api/${apiVersion}/webhook-admin/uiData`,
          withCredentials: true
        }).then(response => {
          curUser = response.data.username
          curUserRoles = response.data.roles
          this.webhooks = response.data.hooks
          if (this.curHook) {
            this.curHook = this.webhooks.find(hk => hk.name === this.curHook.name)
          }
        })
      },
      getPlugins() {
        axios({
          method: "get",
          headers: {
            "x-rundeck-ajax": true
          },
          url: `${rdBase}api/${apiVersion}/webhook-admin/listWebhookPlugins`,
          withCredentials: true
        }).then(response => {
          this.webhookPlugins = response.data
        })
      },
      select(selected) {
        this.curHook = selected
        this.setSelectedPlugin()
      },
      setSelectedPlugin() {
        console.log(this.curHook.config)
        this.selectedPlugin = {type:this.curHook.eventPlugin, config:this.curHook.config}
        console.log(this.selectedPlugin)
        getServiceProviderDescription("WebhookEvent",this.curHook.eventPlugin).then(data => {this.showPluginConfig = data.props.length > 0})
      },
      handleSave() {
        if(!this.curHook.eventPlugin) {
          this.setError("You must select a Webhook plugin before saving")
          return
        }
        this.curHook.config = this.selectedPlugin.config
        axios({
          method: "post",
          headers: {
            "x-rundeck-ajax": true,
            "Content-Type": "application/json"
          },
          url: `${rdBase}webhook-admin/save`,
          data: JSON.stringify(this.curHook),
          withCredentials: true
        }).then(response => {
          if (response.data.err) {
            this.setError("Failed to save! " + response.data.err)
          } else {
            this.setMessage("Saved!")
            this.getHooks()
          }
        }).catch(err => {
          if (err.response.data.err) {
            this.setError("Failed to save! " + err.response.data.err)
          }
        })
      },
      handleDelete() {
        axios({
          method: "delete",
          headers: {
            "x-rundeck-ajax": true
          },
          url: `${rdBase}webhook-admin/delete/${this.curHook.id}`,
          withCredentials: true
        }).then(response => {
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
        })
      },
      addNewHook() {
        this.showPluginConfig = false
        this.curHook = {name: "NewHook", user: curUser, roles: curUserRoles, config: {}}
      }
    },
    mounted() {
      getPluginProvidersForService("WebhookEvent").then(data => {
        data.descriptions.forEach(desc => {
          this.webhookPlugins.push({name:desc.name,title:desc.title})
        })
      })
      this.getHooks()
    }
  }
</script>

<style lang="scss" scoped>
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

  .popup-positioner {
    position: relative;
  }

  .popup {
    background-color: #fff;
    border: 1px solid #aaa;
    border-radius: 3px 0 3px 3px;
    padding: 10px;
    position: absolute;
    top: -10px;
    left: 150px;
    width: 550px;
    z-index: 99999;
  }

  .popup-msg {
    border-radius: 3px;
    border: 1px solid #006400;
    background-color: #90ee90;
    color: #006400;
    padding: 5px;
    width: 95%;
  }

  .popup-error {
    border-radius: 3px;
    border: 1px solid #8b0000;
    background-color: #ffefef;
    color: #8b0000;
    padding: 5px;
  }

  .close-popup {
    position: absolute;
    right: -21px;
    top: -1px;
    z-index: 99999;
    border: 1px solid #aaa;
    border-left-color: #fff;
    border-radius: 0 3px 3px 0;
    padding: 0 6px;
  }

  .popup-close-btn {
    color: #aaa;
    cursor: pointer;
  }
</style>