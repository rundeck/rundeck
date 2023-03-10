<template>
  <div class="alert alert-warning" v-if="this.errorMsg !== null">
    <span> {{this.errorMsg}}</span>
  </div>
  <div class="row text-info ">
    <div class="form-group col-sm-12" data-bind="css: {{invalid}}?'has-error':'' ">
      <div class="input-group">
        <div class="input-group-addon rs-tooltip bottom" v-if='staticRoot'>
        <span class="text-ellipsis" style="
          max-width: 300px;
          display: inline-block;">
          {{getRootBasePath()}}
        </span>
          <div class="rs-tooltip--message">{{getRootBasePath()}}</div>
        </div>
        <input type="text" class="form-control" style="padding-left:18px" v-model="inputPath" v-bind:disabled="loading" v-on:keyup.enter="browseToInputPath()" placeholder="Enter a path" />

        <div class="input-group-btn">
          <button type="button" class="btn btn-default dropdown-toggle"
                  data-toggle="dropdown" aria-haspopup="true"
                  aria-expanded="false">
            <span>{{linksTitle}}</span>
            <span class="caret"></span>
          </button>
          <ul class="dropdown-menu dropdown-menu-right">
            <li v-for="jumpLink in jumpLinks">
              <a href="#" v-on:click="loadDir(jumpLink.path)" v-text="jumpLink.name"></a>
            </li>
          </ul>
        </div>

      </div>
    </div>
  </div>
  <div class="row">
    <div class="col-sm-12">
      <div style="margin-bottom:1em;">
        <button type="button" class="btn btn-sm btn-default" v-on:click="loadDir(upPath())" v-bind:class="{disabled: !upPath() || invalid}">
          <i class="glyphicon glyphicon-folder-open"></i>
          <i class="glyphicon glyphicon-arrow-up"></i>
          <span v-text="upPath() ? dirName(upPath()) : ''"></span>
        </button>
        <div class="btn-group" v-if="browseMode === 'browse'">
          <button type="button" class="btn btn-sm dropdown-toggle" :class="{ disabled: !selectedPath }" data-toggle="dropdown">
            {{ $t('Action') }}
            <span class="caret"></span>
          </button>
          <ul class="dropdown-menu" role="menu">
            <li>
              <a href="#storageconfirmdelete" data-toggle="modal">
                <i class="glyphicon glyphicon-remove"></i>
                {{ $t('Delete Selected Item') }}
              </a>
            </li>

            <li v-if="selectedIsDownloadable">
              <a href="#" @click="download()">
                <i class="glyphicon glyphicon-download"></i>
                {{ $t('Download Contents') }}</a>
            </li>
          </ul>
        </div>

        <div v-if="allowUpload" class="btn-group">
          <a href="#storageuploadkey" v-on:click="actionUpload()" class="btn btn-sm btn-primary">
            <i class="glyphicon glyphicon-plus"></i>
            {{ $t('Add or Upload a Key') }}
          </a>
        </div>
        <div class="btn-group" v-if="allowUpload && selectedPath">
          <a href="#storageuploadkey" @click="actionUploadModify()" class="btn btn-sm btn-info ">
            <i class="glyphicon glyphicon-pencil"></i>
            {{ $t('Overwrite Key') }}
          </a>
        </div>
      </div>
      <div class="loading-area text-info " v-if="loading" style="width: 100%; height: 200px; padding: 50px; background-color: #eee;">
        <i class="glyphicon glyphicon-time"></i>
        {{ $t('Loading') }}
      </div>
      <table class="table table-hover table-condensed" data-bind="if: !invalid() && !loading()">
        <tbody v-if="!notFound">
        <tr>
          <td colspan="2" class="text-strong">
            <span v-if="filteredFiles.length < 1">
              {{ $t('No Keys') }}
            </span>
            <span v-if="filteredFiles.length > 0">
              <span>{{filteredFiles.length}}</span>
              {{ $t('keys') }}
            </span>
          </td>
        </tr>
        </tbody>
        <tbody>
        <tr v-for="file in filteredFiles" :key="file.path" @click="selectFile(file)" :class="{ 'success': selectedPath === file.path }" class="action">
          <td>
            <i :class="{ 'glyphicon-ok': selectedPath === file.path, 'glyphicon-unchecked': selectedPath !== file.path }"></i>

            <span v-if="file.isPrivateKey()" title="This path contains a private key that can be used for remote node execution.">
          <i class="glyphicon glyphicon-lock"></i>
        </span>
            <span v-if="file.isPublicKey()">
          <i class="glyphicon glyphicon-eye-open"></i>
        </span>
            <span v-if="file.isPassword()" title="This path contains a password that can be used for remote node execution.">
          <i class="glyphicon glyphicon-lock"></i>
        </span>

            <span>{{ file.name }}</span>
          </td>
          <td class="text-strong">
        <span class="pull-right">
          <span v-if="file.isPrivateKey()" title="${g.enc(code: 'storage.private.key.description')}">
            {{ $t('Private Key') }}
          </span>
          <span v-if="file.isPublicKey()">
            {{ $t('Public Key') }}
          </span>
          <span v-if="file.isPassword()" title="${g.enc(code: 'storage.password.description')}">
            {{ $t('Password') }}
          </span>
        </span>
          </td>
        </tr>
        </tbody>

        <tbody v-if="!notFound">
        <tr v-for="directory in directories" :key="directory.name">
          <td class="action" @click="loadDir(directory)" colspan="2">
            <i class="glyphicon glyphicon-arrow-down"></i>
            <i class="glyphicon glyphicon-folder-close"></i>
            <span>{{ dirName(directory) }}</span>
          </td>
        </tr>
        </tbody>
        <tbody v-else>
        <tr>
          <td colspan="2">
      <span class="text-strong">{{ $t('Nothing found at this path') }}
        <span v-if="allowUpload">{{ $t('Select "Add or Upload a Key" if you would like to create a new key.') }}</span>
      </span>
          </td>
        </tr>
        </tbody>

      </table>

    </div>
  </div>
  <div v-if="selectedPath" class="row">
    <div class="col-sm-12">
      <div class="well">
        <div>
          {{ $t('Storage path\:') }}
          <code class="text-success">{{ selectedPath }}</code>
          <a :href="selectedPathUrl">
            <i class="glyphicon glyphicon-link"></i>
          </a>
        </div>

        <div v-if="selectedResource && selectedResource.createdTime">
          <div>
            {{ $t('created\:') }}
            <span class="timeabs text-strong" :title="selectedResource.meta['Rundeck-content-creation-time']">{{ selectedResource.createdTime }}</span>
            <span v-if="selectedResource.createdUsername">
        {{ $t('by\:') }}

        <span class="text-strong">{{ selectedResource.createdUsername }}</span>
      </span>

          </div>
        </div>
        <div v-if="selectedResource && selectedResource.wasModified()">
          <div>
            {{ $t('Modified\:') }}
            <span class="timeago text-strong" :title="selectedResource.meta['Rundeck-content-modify-time']">{{ selectedResource.modifiedTimeAgo('ago') }}</span>

            <span v-if="selectedResource.modifiedUsername">
        {{ $t('by\:') }}
        <span class="text-strong">{{ selectedResource.modifiedUsername }}</span>
      </span>
          </div>
        </div>

        <div v-if="selectedResource && selectedResource.isPublicKey() && selectedIsDownloadable">
          <button @click="$root.actionLoadContents('publicKeyContents',$element)" v-if="!selectedResource.wasDownloaded()" class="btn btn-sm btn-default">
            {{ $t('View Public Key Contents') }}
            (<span>{{ selectedResource.contentSize }}</span>
            {{ $t('Bytes') }})
          </button>

          <div class="pre-scrollable" v-if="selectedResource.downloadError">
            <span class="text-danger">{{ selectedResource.downloadError }}</span>
          </div>
          <pre id="publicKeyContents" class="pre-scrollable" v-if="selectedResource.wasDownloaded"></pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {getRundeckContext} from "../../../library";
import {RundeckBrowser} from "@rundeck/client";
import Vue from "vue";

const client: RundeckBrowser = getRundeckContext().rundeckClient
const rdBase = getRundeckContext().rdBase
const context = getRundeckContext()
interface StorageData {
  resourcePath: string;
  project: string;
  downloadEnabled: boolean;
}


export default Vue.extend({
  name: "KeyStorageBrowser.vue",
  data(){
    return{
      storageData: {} as StorageData,
      rootPath: "keys",
      baseUrl: "",
      basePath: "",
      jumpLinks: [],
      linksTitle: '',
      staticRoot: false,
      errorMsg: "",
      path: '',
      inputPath: '',
      selectedPath: '',
      selectedResource: '',
      selectedIsDownloadable: false,
      fileFilter: '',
      fieldTarget: '',
      resources: [],
      loading: false,
      invalid: false,
      browseMode: 'browse',
      allowUpload: false,
      allowSelection: true,
      allowNotFound: false,
      notFound: false,
      downloadenabled: true
    }
    },
    computed: {
      filteredFiles() {
        return this.files.filter(res => {
          if (this.fileFilter) {
            const filt = this.fileFilter.split("=");
            if (filt.length > 1) {
              const key = filt[0];
              const value = filt[1];
              return res.metaValue(key) == value;
            }
          }
          return true;
        });
      },
      directories: function() {
        return this.resources.filter(function(res) {
          return res.type === 'directory';
        }).sort(function(a,b){
          return a.path === b.path ? 0 : (a.path < b.path ? -1 : 1);
        });
      },
      selectedPathUrl() {
        return this._genUrl("/storage/download/keys" + '/' + this.selectedPath);
      }
    },
    methods: {
      getRootBasePath(){
        return this.rootPath + (this.basePath ? '/' + this.basePath : '')
      },
      browseToInputPath() {
        this.path = this.absolutePath(this.inputBasePath);
      },
      loadDir(dir) {
        const path = typeof dir === 'string' ? dir : dir.path;
        if (dir !== this.path) {
          this.selectedPath = null;
        }
        this.path = path;
      },
      upPath() {
        if (this.path !== this.rootBasePath && this.path !== `${this.rootBasePath}/`) {
          if (this.path.indexOf('/') >= 0) {
            return this.path.substring(0, this.path.lastIndexOf('/'));
          } else {
            return this.rootBasePath;
          }
        }
        return null;
      },
      dirName(elem) {
        if (typeof elem === 'string') {
          return this.dirNameString(elem);
        }
        if (elem.type === 'directory') {
          return this.dirNameString(elem.path);
        } else {
          return elem.name;
        }
      },
      download() {
        if (this.selectedPath) {
          //What to replace w applinks
          window.location.href = this._genUrl("/storage/download/keys", {relativePath: this.relativePath(this.selectedPath)});
        }
      },
      selectFile(file) {
        this.selectedPath = file.path;
      },
      actionUpload() {
        this.upload.modifyMode = false;
        this.upload.fileName = '';
        // jQuery("#storageuploadkey").modal({backdrop: false});
        //jQuery("#storageuploadkey").modal('show');
      },
      actionUploadModify() {
        if (this.selectedResource) {
          this.upload.fileName = this.selectedResource.name;
          this.upload.keyType = this.selectedResource.isPrivateKey ? 'private' : this.selectedResource.isPublicKey ? 'public' : 'password';
          this.upload.modifyMode = true;
          // jQuery("#storageuploadkey").modal({backdrop:false});
          // jQuery("#storageuploadkey").modal('show');
        }
      }
    },
  async mounted() {
    this.project = window._rundeck.projectName;
    this.storageData = window._rundeck.data.storageData as StorageData
  }


});
</script>

<style scoped>

</style>