<template>
  <div class>
    <div class v-if="title">
      <h3 class>
        {{title}}
        <a class="btn btn-primary btn-sm" @click="mode='edit'" v-if="mode!=='edit'">
          <i class="glyphicon glyphicon-pencil"></i>
          {{editButtonText || $t('Edit')}}
        </a>
      </h3>
    </div>

    <div class>
      <div v-if="errors.length>0" class="alert alert-danger">
        <ul>
          <li v-for="(error,index) in errors" :key="'error_'+index">{{error}}</li>
        </ul>
      </div>
      <div>
        <a class="btn btn-primary btn-sm" @click="mode='edit'" v-if="mode!=='edit'">
          <i class="glyphicon glyphicon-pencil"></i>
          {{editButtonText || $t('Edit')}}
        </a>
        <div class="help-block" v-if="help">{{help}}</div>

        <div class="list-group">
          <div
            class="list-group-item"
            v-for="(plugin,index) in pluginConfigs"
            :key="'pluginStorageAccessplugin_'+index"
          >
            <plugin-config
              :mode="'title'"
              :serviceName="serviceName"
              :provider="plugin.entry.type"
              :key="plugin.entry.type+'title/'+index"
              :show-description="true"
              @hasKeyStorageAccess="hasKeyStorageAccess"
            >
              <span slot="titlePrefix">{{index+1}}.</span>
            </plugin-config>

            <div v-if="additionalProps && additionalProps.props.length>0">
              <plugin-config
                :mode="editFocus===(index) ? plugin.create?'create':'edit':'show'"
                v-model="plugin.extra"
                :config="plugin.extra.config"
                :key="'additional_config_'+index+'/'+((editFocus===index)?'true':'false' )"
                :show-title="false"
                :show-description="false"
                :plugin-config="additionalProps"
              ></plugin-config>
            </div>
            <plugin-config
              :mode="editFocus===(index) ? plugin.create?'create':'edit':'show'"
              :serviceName="serviceName"
              v-model="plugin.entry"
              :provider="plugin.entry.type"
              :config="plugin.entry.config"
              :key="plugin.entry.type+'_config/'+index"
              :show-title="false"
              :show-description="false"
              :validation="plugin.validation"
              :validation-warning-text="$t('Validation errors')"
              @input="configUpdated"
            >
              <div slot="extra" class="row" v-if="mode==='edit'">
                <div class="col-xs-12 col-sm-12">
                  <span v-if="editFocus===-1">
                    <a
                      class="btn btn-default btn-xs"
                      @click="editFocus=index"
                      :key="'edit'"
                    >{{$t('Edit')}}</a>
                  </span>
                  <span v-if="editFocus===index">
                    <a
                      class="btn btn-cta btn-xs"
                      @click="savePlugin(plugin,index)"
                      :key="'save'"
                    >{{$t('Save')}}</a>
                    <a class="btn btn-default btn-xs" @click="editFocus=-1">{{$t('Cancel')}}</a>
                  </span>
                  <span class="small text-info" v-if="plugin.modified">
                    <i class="fas fa-pen-square"></i>
                    Modified
                  </span>
                  <div class="btn-group pull-right" v-if="(editFocus===-1||editFocus===index)">
                    <btn
                      class="btn-xs btn-danger"
                      @click="removePlugin(plugin,index)"
                      :disabled="editFocus!==-1&&editFocus!==(index)"
                    >
                      {{$t('Delete')}}
                      <i class="fas fa-minus"></i>
                    </btn>
                    <btn
                      class="btn-xs"
                      @click="movePlugin(index,plugin,-1)"
                      :disabled="editFocus!==-1 || index==0"
                    >
                      <i class="fas fa-arrow-up"></i>
                    </btn>
                    <btn
                      class="btn-xs"
                      @click="movePlugin(index,plugin,1)"
                      :disabled="editFocus!==-1 || index>=pluginConfigs.length-1"
                    >
                      <i class="fas fa-arrow-down"></i>
                    </btn>
                  </div>
                </div>
              </div>
            </plugin-config>
            <slot name="item-extra" :plugin="plugin" :editFocus="editFocus===index" :mode="mode"></slot>
          </div>
          <div class="list-group-item" v-if="pluginConfigs.length<1 && showEmpty">
            <slot name="empty-message">
              <span  class="text-muted">
                {{$t('empty.message.default',[pluginLabels && pluginLabels.addButton || serviceName])}}
              </span>
            </slot>
          </div>
        </div>

        <div class="card-footer" v-if="mode==='edit' && editFocus===-1">
          <btn type="primary" @click="modalAddOpen=true">
            {{pluginLabels && pluginLabels.addButton || serviceName}}
            <i class="fas fa-plus"></i>
          </btn>

          <modal
            v-model="modalAddOpen"
            :title="pluginLabels && pluginLabels.addButton || serviceName"
            ref="modal"
            size="lg"
            id="add-new-modal"
            append-to-body
          >
            <div class="list-group">
              <a
                v-for="plugin in pluginProviders"
                v-bind:key="plugin.name"
                href="#"
                @click="addPlugin(plugin.name)"
                class="list-group-item"
              >
                <plugin-info
                  :detail="plugin"
                  :show-description="true"
                  :show-extended="false"
                  description-css="help-block"
                >
                  <i class="glyphicon glyphicon-plus text-muted"></i>
                </plugin-info>
              </a>
            </div>
            <div slot="footer">
              <btn @click="modalAddOpen=false">{{$t('Cancel')}}</btn>
            </div>
          </modal>
        </div>
        <div class="card-footer" v-if="mode==='edit' && editFocus===-1">
          <btn type="default" @click="cancelAction" v-if="modeToggle">{{$t('Cancel')}}</btn>
          <btn type="default" @click="cancelAction" v-else-if="modified">{{$t('Revert')}}</btn>
          <a class="btn btn-cta" @click="savePlugins" v-if="modified" href="#">{{$t('Save')}}</a>
          <span class="text-warning" v-if="modified">Changes have not been saved.</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import axios from "axios";
import Vue from "vue";
import { Notification } from "uiv";
import { getRundeckContext, RundeckContext } from "@/library";
import Expandable from "@/library/components/utils/Expandable.vue";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";
import PluginConfig from "@/library/components/plugins/pluginConfig.vue";
import pluginService from "@/library/modules/pluginService";
import PluginValidation from "@/library/interfaces/PluginValidation";

interface PluginConf {
  readonly type: string;
  config: any;
}
interface ProjectPluginConfigEntry {
  entry: PluginConf;
  extra: PluginConf;
  validation: PluginValidation;
  create?: boolean;
  origIndex?: number;
  modified: boolean;
}
export default Vue.extend({
  name: "App",
  components: {
    PluginInfo,
    PluginConfig,
    Expandable
  },
  data() {
    return {
      loaded: false,
      modified: false,
      mode: this.editMode ? "edit" : "show",
      project: "",
      rdBase: "",
      cancelUrl: "",
      pluginConfigs: [] as ProjectPluginConfigEntry[],
      configOrig: [] as any[],
      rundeckContext: {} as RundeckContext,
      modalAddOpen: false,
      pluginProviders: [],
      pluginLabels: {},
      editFocus: -1,
      errors: [] as string[],
      pluginStorageAccess: [] as any[]
    };
  },
  props: {
    editMode: {
      type: Boolean,
      default: false
    },
    modeToggle: {
      type: Boolean,
      default: true
    },
    showEmpty: {
      type: Boolean,
      default: true
    },
    serviceName: String,
    title: {
      required: false,
      type: String
    },
    configPrefix: String,
    additionalProps: { required: false, type: Object },
    help: { required: false, type: String },
    editButtonText: { required: false, type: String },
  },
  methods: {
    notifyError(msg: string, args: any[]) {
      Notification.notify({
        type: "danger",
        title: "An Error Occurred",
        content: msg,
        duration: 0
      });
    },

    notifySuccess(title: string, msg: string) {
      Notification.notify({
        type: "success",
        title: title,
        content: msg,
        duration: 5000
      });
    },
    createConfigEntry(entry: any, origIndex: number): ProjectPluginConfigEntry {
      return {
        extra: { config: Object.assign({}, entry.extra) },
        entry: { type: entry.type, config: Object.assign({}, entry.config) },
        origIndex: origIndex
      } as ProjectPluginConfigEntry;
    },

    serializeConfigEntry(entry: ProjectPluginConfigEntry): any {
      return {
        extra: Object.assign({}, entry.extra.config),
        type: entry.entry.type,
        config: Object.assign({}, entry.entry.config),
        origIndex: entry.origIndex
      };
    },
    addPlugin(provider: string) {
      this.modalAddOpen = false;
      this.pluginConfigs.push({
        entry: { type: provider, config: {} },
        extra: { config: {} },
        create: true
      } as ProjectPluginConfigEntry);

      this.setFocus(this.pluginConfigs.length - 1);
    },
    setFocus(focus: number) {
      this.editFocus = focus;
    },
    async savePlugin(plugin: ProjectPluginConfigEntry, index: number) {
      //validate
      const validation: PluginValidation = await pluginService.validatePluginConfig(
        this.serviceName,
        plugin.entry.type,
        plugin.entry.config
      );
      if (!validation.valid) {
        Vue.set(plugin, "validation", validation);
        return;
      }
      Vue.delete(plugin, "validation");
      plugin.create = false;
      plugin.modified = true;
      this.setPluginConfigsModified();
      this.setFocus(-1);
    },
    removePlugin(plugin: ProjectPluginConfigEntry, index: string) {
      const found = this.pluginConfigs.indexOf(plugin);
      this.pluginConfigs.splice(found, 1);
      if (!plugin.create) {
        this.setPluginConfigsModified();
      }

      this.cleanStorageAccess(plugin);
      this.setFocus(-1);
    },

    movePlugin(index: number, plugin: ProjectPluginConfigEntry, shift: number) {
      const found = this.pluginConfigs.indexOf(plugin);
      const item = this.pluginConfigs.splice(found, 1)[0];
      const newindex = found + shift;
      this.pluginConfigs.splice(newindex, 0, item);
      this.setPluginConfigsModified();
      this.editFocus = -1;
    },
    didSave(success: boolean) {
      if (this.modeToggle) {
        this.mode = "show";
      }
    },
    async savePlugins() {
      try {
        const result = await this.saveProjectPluginConfig(
          this.project,
          this.configPrefix,
          this.serviceName,
          this.pluginConfigs
        );
        if (result.success) {
          this.didSave(true);
          this.notifySuccess("Success", "Configuration Saved");
          this.configOrig = result.data.plugins;
          //copy
          this.pluginConfigs = this.configOrig.map(this.createConfigEntry);
          this.pluginConfigsModifiedReset();
          this.$emit("saved", result);
        }
      } catch (error) {
        //@ts-ignore
        this.notifyError(error.message, []);
      }
    },
    async loadProjectPluginConfig(
      project: string,
      configPrefix: string,
      serviceName: string
    ) {
      const response = await axios({
        method: "get",
        headers: { "x-rundeck-ajax": true },
        url: `${this.rdBase}framework/projectPluginsAjax`,
        params: {
          project: `${window._rundeck.projectName}`,
          configPrefix: this.configPrefix,
          serviceName: this.serviceName,
          format: "json"
        },
        withCredentials: true
      });
      if (!response || response.status < 200 || response.status >= 300) {
        throw new Error(
          `Error reading project configuration for ${serviceName}: Response status: ${
            response ? response.status : "None"
          }`
        );
      }
      if (response.data && response.data.plugins) {
        return response.data.plugins;
      }
    },
    async saveProjectPluginConfig(
      project: string,
      configPrefix: string,
      serviceName: string,
      data: ProjectPluginConfigEntry[]
    ) {
      const serializedData = data.map(this.serializeConfigEntry);

      const resp = await this.rundeckContext.rundeckClient.sendRequest({
        pathTemplate: `/framework/saveProjectPluginsAjax`,
        baseUrl: this.rdBase,
        method: "POST",
        queryParameters: {
          project: `${window._rundeck.projectName}`,
          configPrefix: this.configPrefix,
          serviceName: this.serviceName
        },
        body: { plugins: serializedData }
      });

      if (resp && resp.status >= 200 && resp.status < 300) {
        return { success: true, data: resp.parsedBody };
      }
      if (resp && resp.status == 422) {
        //look for validation
        if (resp.parsedBody && resp.parsedBody.errors instanceof Array) {
          this.errors = resp.parsedBody.errors;
          if (resp.parsedBody.reports) {
            const reports = resp.parsedBody.reports as { [key: string]: any };
            //console.log("reports ",resp.parsedBody.reports)
            this.pluginConfigs.forEach((plugin, index) => {
              if (reports[`${index}`] !== undefined) {
                plugin.validation = {
                  valid: false,
                  errors: reports[`${index}`]
                };
              }
            });
          }
          return { success: false };
        }
      }
      throw new Error(
        `Error saving project configuration for ${serviceName}: Response status: ${
          resp ? resp.status : "None"
        }`
      );
    },
    cancelAction() {
      this.pluginConfigs = this.configOrig.map(this.createConfigEntry);
      this.pluginConfigsModifiedReset();
      this.didSave(false);
    },
    setPluginConfigsModified() {
      this.modified = true;
      this.$emit("modified");
      this.notifyPluginConfigs();
    },
    pluginConfigsModified() {
      if (this.loaded) {
        this.setPluginConfigsModified();
      }
    },
    pluginConfigsModifiedReset() {
      this.modified = false;
      this.$emit("reset");
      this.notifyPluginConfigs();
    },
    notifyPluginConfigs(){
      this.$emit("plugin-configs-data",this.pluginConfigs);
    },
    configUpdated() {
      this.pluginConfigsModified();
    },
    hasKeyStorageAccess(provider: any){
      this.pluginStorageAccess.push(provider)
      this.$emit('plugin-storage-access',[provider])
    },
    cleanStorageAccess(plugin: any){
      const pluginStorageAccess = [] as any[];
      this.pluginStorageAccess.forEach((pluginAccess:any)=>{
        if(pluginAccess !== plugin.entry.type){
          pluginStorageAccess.push(pluginAccess);
        }
      });

      this.pluginStorageAccess = pluginStorageAccess;
    },
  },
  mounted() {
    this.rundeckContext = getRundeckContext();
    const self = this;
    this.notifyPluginConfigs();
    if (
      window._rundeck &&
      window._rundeck.rdBase &&
      window._rundeck.projectName
    ) {
      this.rdBase = window._rundeck.rdBase;
      this.project = window._rundeck.projectName;

      this.loadProjectPluginConfig(
        window._rundeck.projectName,
        this.configPrefix,
        this.serviceName
      ).then(pluginConfigs => {
        this.configOrig = pluginConfigs;
        //copy
        this.pluginConfigs = pluginConfigs.map((val: any, index: number) =>
          this.createConfigEntry(val, index)
        );
        this.loaded = true;
        this.notifyPluginConfigs();
      }).catch(error => console.error(error));

      pluginService
        .getPluginProvidersForService(this.serviceName)
        .then(data => {
          if (data.service) {
            this.pluginProviders = data.descriptions;
            this.pluginLabels = data.labels;
          }
        }).catch(error => console.error(error));
    }
  }
});
</script>

<style>
</style>
