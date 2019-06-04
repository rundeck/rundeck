<template>
  <div>
  <h3>Webhook Management</h3>
  <div class="row">
    <div class="col-xs-12">
      <div class="artifact-grid row row-flex row-flex-wrap">
  <div class="col-sm-4 details-checkbox-column">
    <div
      style="max-height: 80vh;overflow-y: scroll; overflow-x: hidden; display:inline-block;"
      class="flex-col"
    >
      <div>
        <div class="row" style="line-height: 2.25em;">
          <div class="col-sm-6">
          <div style="font-size: 1.5em; ">Webhooks</div>
          </div>
          <div class="col-sm-5 text-right">
            <a
              class="btn btn-md btn-success"
              @click="addNewHook"
            >Add</a>
          </div>
        </div>
      <div style="background-color: #fff;">
        <table class="table table-striped">
          <thead>
          <tr>
            <th scope="col">Name</th>
            <th scope="col">Event Handler Plugin</th>
          </tr>
          </thead>
          <tbody>
        <tr v-for="hook in webhooks" :key="id" @click="select(hook)">
          <td>{{hook.name}}</td>
          <td>{{hook.eventPlugin}}</td>
        </tr>
          </tbody>
        </table>
      </div>
<!--          <div class="row">-->
<!--            <div class="col-sm-3 border">-->
<!--              <h5>Name</h5>-->
<!--            </div>-->
<!--            <div class="col-sm-5 border">-->
<!--              <h5>Event Handler Plugin</h5>-->
<!--            </div>-->
<!--          </div>-->
<!--          <div class="row" v-for="hook in webhooks" :key="id" @click="select(hook)">-->
<!--            <div class="col-sm-4">-->
<!--                {{hook.name}}-->
<!--            </div>-->
<!--            <div class="col-sm-4">-->
<!--                {{hook.eventPlugin}}-->
<!--            </div>-->
<!--          </div>-->
      </div>
    </div>
  </div>
  <div class="col-sm-8 details-output">
    <div
      style="display: inline-block;max-height: 80vh;overflow-y: scroll;"
      class="flex-col"
    >

        <div class="card">
          <div class="card-header">
            <h5 style="margin:0;">Webhook Detail</h5>
          </div>
          <hr>
          <div class="card-content">
            <div v-if="curHook">
              <div><label>Post Url:</label><span>{{basePostUrl}}{{curHook.name}}</span></div>
              <div><label>Webhook Name:</label><input v-model="curHook.name"></div>
              <div><label>Webhook Event Plugin:</label><select v-model="curHook.eventPlugin" @change="setSelectedPlugin()">
                <option v-for="plugin in webhookPlugins" v-bind:value="plugin.name">{{plugin.name}}</option>
              </select></div>
              <div v-if="selectedPlugin">
                <h5>Plugin Configuration</h5>
                <hr>
                <div v-for="(prop,index) in selectedPlugin.configProps" :key="index">
                  <label>{{prop}}</label><input type="text" v-model="curHook.config[prop]">
                </div>
              </div>
              <div class="row">
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
  </div>
  </div>
  </div>
</template>

<script>
  import axios from 'axios'
  var rdBase = "http://localhost:4440"
  var apiVersion = "31"
  if (window._rundeck && window._rundeck.rdBase && window._rundeck.apiVersion) {
    rdBase = window._rundeck.rdBase;
    apiVersion = window._rundeck.apiVersion
  }
    export default {
        name: "WebhooksView",
        data() {
          return {
            webhooks: [],
            webhookPlugins: [],
            curHook: null,
            selectedPlugin: null,
            basePostUrl: `${rdBase}api/${apiVersion}/webhook/`
          }
        },
        methods: {
          getHooks() {
            axios({
              method: "get",
              headers: {
                "x-rundeck-ajax": true
              },
              url: `${rdBase}api/${apiVersion}/webhook-admin/list`,
              withCredentials: true
            }).then(response => {
              this.webhooks = response.data
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
           this.selectedPlugin = this.webhookPlugins.find(p => p.name === this.curHook.eventPlugin)
          },
          handleSave() {
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
              if(response.data.err) {
                window.alert("Failed to save! " + response.data.err)
              } else {
                window.alert("Saved!")
                this.getHooks()
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
              if(response.data.err) {
                window.alert("Failed to delete! " + response.data.err)
              } else {
                this.curHook = null
                this.selectedPlugin = null
                this.getHooks()
                window.alert("Deleted!")
              }
            }).catch(response => {
              if(response.data.err) {
                window.alert("Failed to delete! " + response.data.err)
              }
            })
          },
          addNewHook() {
            this.curHook = {name: "NewHook",config:{}}
          }
        },
        mounted() {
          this.getPlugins()
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
</style>
