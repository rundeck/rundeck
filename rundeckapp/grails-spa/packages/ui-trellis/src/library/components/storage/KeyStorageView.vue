<template>
  <div>
    <div class="alert alert-warning" v-if="errorMsg!==''">
      <span>{{errorMsg}}</span>
    </div>

    <div class="card">
      <div class="card-content">
    <div class="row text-info ">
      <div class="form-group col-sm-12" :class="[invalid===true ? 'has-error' : '']">
        <div class="input-group">
          <div class="input-group-addon bg-3" v-if="staticRoot">
            <span>{{rootPath}}</span>
          </div>
          <input type="text" class="form-control bg-2" style="padding-left:18px;"
                v-model="inputPath" @keyup.enter="loadDirInputPath()"
                placeholder="Enter a path"/>
          <div v-if="!this.isProject && !readOnly" class="input-group-btn" :class="isDropdownOpen ? 'open input-group-btn' : 'input-group-btn'">
            <button
                type="button"
                class="btn btn-default dropdown-toggle"
                @click="toggleDropdown"
                :aria-expanded="isDropdownOpen"
            >
              <span>{{ linksTitle }}</span>
              <span class="caret"></span>
            </button>
            <ul class="dropdown-menu dropdown-menu-right" v-if="isDropdownOpen">
              <li v-for="link in jumpLinks" :key="link.path">
                <a href="#" @click="loadDir(link.path)">{{ link.name }}</a>
              </li>
            </ul>
          </div>
          <div v-if="isRunner" class="input-group-btn">
            <button
                type="button"
                class="btn btn-default"
                @click="loadDir()"
            >
              <span>{{ "Reload" }}</span>
            </button>

          </div>
        </div>
      </div>
    </div>
    <div class="row keySelector">
      <div class="col-sm-12">
        <div class="keySelector-button-group" style="margin-bottom:1em;">
          <button type="button" class="btn btn-sm btn-default"
                  @click="loadDir(upPath)" :disabled="upPath===''">
            <i class="glyphicon glyphicon-folder-open"></i>
            <i class="glyphicon glyphicon-arrow-up"></i>
            <span>{{showUpPath()}}</span>
          </button>
          <button v-if="!readOnly || this.allowUpload===true"  @click="actionUpload()" class="btn btn-sm btn-cta">
            <i class="glyphicon glyphicon-plus"></i>
            Add or Upload a Key
          </button>

          <button @click="actionUploadModify()" class="btn btn-sm btn-warning"
                  v-if="this.allowUpload===true && this.isSelectedKey===true && !readOnly">
            <i class="glyphicon glyphicon-pencil"></i>
            Overwrite Key
          </button>
          <button class="btn btn-sm btn-danger" @click="deleteKey" v-if="this.selectedKey && this.selectedKey.path && isSelectedKey && !readOnly">
                  <i class="glyphicon glyphicon-trash"></i>
                  {{"Delete"}}</button>
        </div>


        <div class="loading-area text-info " v-if="loading" style="width: 100%; height: 200px; padding: 50px; background-color: #eee;">
          <i class="glyphicon glyphicon-time">{{ "Loading..." }}</i>
          <div v-if="isRunner">
            <span v-if="countDownLimit > 0">
              Reload from the remote Runner in {{ this.countDownLimit }} seconds
            </span>
            <span v-if="countDownLimit === 0">
              Reload
            </span>
          </div>
        </div>
        <table class="table table-hover table-condensed" v-else>
          <tbody>
          <tr>
            <td colspan="2" class="text-strong">
              <span v-if="files.length<1">No keys</span>
              <span v-if="files.length>0">
                <span>{{files.length}}</span>
                keys
              </span>
            </td>
          </tr>
          </tbody>
          <tbody>
          <tr v-for="key in files" :class="[selectedKey && key.path=== selectedKey.path ? selectedClass : '','action']"
              :key="key.name" @click="selectKey(key)">
            <td>
              <i :class="[key.path=== selectedKey.path ? 'glyphicon glyphicon-ok' :'glyphicon glyphicon-unchecked']"></i>

              <span v-if="isPrivateKey(key)"
                    title="This path contains a private key that can be used for remote node execution.">
                                    <i class="glyphicon glyphicon-lock"></i>
                                  </span>
              <span v-if="isPublicKey(key)">
                                    <i class="glyphicon glyphicon-eye-open"></i>
                                  </span>
              <span v-if="isPassword(key)"
                    title="This path contains a password that can be used for remote node execution.">
                                    <i class="glyphicon glyphicon-lock"></i>
                                  </span>
              <span>{{key.name}}</span><span v-if="key.expired">{{"[CACHE EXPIRED]"}}</span>
            </td>
            <td class="text-strong">
              <span class="pull-right">
                <span v-if="isPrivateKey(key)"
                      title="This path contains a private key that can be used for remote node execution.">
                  Private Key
                </span>
                <span v-if="isPublicKey(key)">
                  Public Key
                </span>
                <span v-if="isPassword(key)"
                      title="This path contains a password that can be used for remote node execution.">
                  Password
                </span>
              </span>
            </td>
          </tr>
          </tbody>

          <tbody v-if="notFound()===true">
          <tr>
            <td colspan="2">
                <span class="text-strong">Nothing found at this path.
                </span>
            </td>
          </tr>
          </tbody>
          <tbody>
          <tr v-for="directory in directories" :key="directory.name">
            <td class="action" @click="loadDir(directory.path)" colspan="2">
              <i class="glyphicon glyphicon-arrow-down"></i>
              <i class="glyphicon glyphicon-folder-close"></i>
              <span>{{dirNameString(directory.path)}}</span>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>
    <modal v-model="isConfirmingDeletion" title="Delete Selected Key" id="storagedeletekey" ref="modalDelete" auto-focus append-to-body :footer="false">
      <div class="modal-body">
        <p>{{"Really delete the selected key at this path?"}} </p>

        <p>
          <strong class="text-info"> {{this.selectedKey.path}}</strong>
        </p>
      </div>
      <div class="modal-footer">
        <button type="button" @click="confirmDeleteKey" class="btn btn-sm btn-danger obs-storagedelete-select"> {{"Delete"}}</button>
        <button type="button" @click="cancelDeleteKey" class="pull-right btn btn-sm btn-default">{{"Cancel"}}</button>
      </div>
    </modal>
    <div class="row" v-if="isSelectedKey && selectedKey.type === 'file'">
      <div class="col-sm-12">
        <div class="well">
          <div>
            Storage path:
            <code class="text-success">{{selectedKey.path}}</code>
            <a href="#" data-bind="attr: { href: selectedPathUrl() }">
              <i class="glyphicon glyphicon-link"></i>
            </a>
          </div>
          <div v-if="createdTime()!==''">
            <div>
              Created:
              <span class="timeabs text-strong">
                                      {{createdTime() | moment("dddd, MMMM Do YYYY, h:mm:ss a") }}
                                  </span>

              <span v-if="createdUsername()!==''">
                                      by:
                                      <span class="text-strong">{{createdUsername()}}</span>
                                  </span>

            </div>
          </div>
          <div v-if="wasModified()!==''">
            <div>
              Modified:
              <span class="timeago text-strong">
                                      {{modifiedTimeAgoText()| duration('humanize') }} ago
                                  </span>

              <span v-if="modifiedUsername()!==''">
                                  by:
                                  <span class="text-strong">{{modifiedUsername()}}</span>
                                </span>
            </div>
          </div>
          <div v-if="this.selectedKey && isPublicKey(this.selectedKey)" class="pull-right">
            <span>
              <a :href="downloadUrl()">
                    <i class="glyphicon glyphicon-download"></i>
                    {{"Download"}}</a>
            </span>
          </div>
          <div v-if="this.selectedKey && this.selectedKey.expired" class="pull-right">
            <span>
              <a :href="'javascript:void(0)'" @click="loadKeys(selectedKey)">
                    <i class="glyphicon glyphicon-download"></i>
                    {{"Refresh Expired Key"}}</a>
            </span>
          </div>
        </div>
      </div>
    </div>
      </div>
      <div class="card-footer">
        <hr>
        <span class="text-info">
            {{ $t('Key Storage provides a global directory-like structure to save Public and Private Keys and Passwords, for use with Node Execution authentication.') }}
        </span>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import InputType from "./InputType"
import KeyType from "./KeyType";
import moment from 'moment'
import {getRundeckContext} from "../../index"
import Vue from "vue"
 

export default Vue.extend({
  name: "KeyStorageView",
  props: {
    readOnly: Boolean,
    allowUpload: Boolean,
    value: String,
    storageFilter: String,
    rootPath: String,
    createdKey: {},
    runnerId: String
  } ,
  data() {
    return {
      errorMsg: '',
      isDropdownOpen: false,
      modalOpen: false,
      invalid: false,
      project: '',
      staticRoot: true,
      inputPath: '',
      upPath: '',
      path: '',
      isConfirmingDeletion: false,
      modalEdit: false,
      upload: {} as any,
      selectedKey: {} as any,
      isSelectedKey: false,
      files: [] as any,
      selectedClass: 'success',
      directories: [] as any,
      uploadErrors: {} as any,
      selectedIsDownloadable: true,
      loading: true,
      linksTitle: '',
      projectList: [],
      jumpLinks: [] as Array<{ name: string | undefined; path: string }>,
      countDownLimit: 0
    }
  },
  mounted() {
    this.loadKeys()
    this.loadProjectNames()
  },
  watch : {
    createdKey : function (newValue, oldValue) {
      if(newValue !== null){
        this.selectKey(newValue)
      }
    },
    rootPath: function(newValue: string) {
      // Reset current path when rootPath changed. 
      this.path = ''
      this.inputPath = ''
      this.selectedKey = {}
      this.loadKeys()
    }
  },
  computed: {
    isProject(): boolean {
      return this.rootPath.startsWith("keys/project");
    },
    isRunner(): boolean {
      return this.rootPath.startsWith('keys/runner')
    }
  },
  methods: {
    downloadUrl(): string {
      const downloadBaseUrl = 'storage/download/keys'
      const rundeckContext = getRundeckContext()

      if(!this.selectedKey || !this.selectedKey.path) return '#'

      return `${rundeckContext.rdBase}/${downloadBaseUrl}?resourcePath=${encodeURIComponent(this.selectedKey.path)}`
    },
    async loadProjectNames() {
      try {
        const response = await getRundeckContext().rundeckClient.projectList();

        this.linksTitle = 'Projects';
        this.jumpLinks = response.map((v:any) => {
          return { name: v.name, path: 'keys/project/' + v.name };
        });
      } catch (error) {
        console.error('Error loading project names:', error);
      }
    },
    toggleDropdown() {
      this.isDropdownOpen = !this.isDropdownOpen;
    },
    deleteKey(){
      this.isConfirmingDeletion=true
    },
    async confirmDeleteKey(){
      const rundeckContext = getRundeckContext();
      this.isConfirmingDeletion=false

      const resp = await rundeckContext.rundeckClient.storageKeyDelete(this.selectedKey.path.slice(5))
      if(resp._response.status >= 400){
        this.errorMsg = resp.error
        return
      }
      this.selectedKey = {}
      this.isSelectedKey = false

      this.loadKeys()
      
    },
    cancelDeleteKey() {
      this.isConfirmingDeletion=false
    },
    calcBrowsePath(path: string){
      let browse=path

      if(this.isRunner) {
      return path
      }

      if (this.rootPath != 'keys/' && this.rootPath != 'keys') {
        browse = (this.rootPath) + "/" + path
        browse = browse.substring(5)
      }
      return browse
    },
    countDown(selectedKey?: any) {
      if(this.countDownLimit > 0) return
      this.countDownLimit = 5
      // @ts-ignore
      const countDownTimer = setInterval(() => {
        this.countDownLimit--;
        
        if(this.countDownLimit <= 0) {
          // @ts-ignore
          clearInterval(countDownTimer);
          const delayExec = setTimeout(() => { 
            this.loadKeys(selectedKey) 
            clearTimeout(delayExec)
          }, 600) // Delay 600ms to execute to give better user experience.
        }
            
      },1000);
    },
    loadKeys(selectedKey?: any) {
      if(selectedKey) {
        this.selectedKey = selectedKey
      }
      this.loading=true

      const rundeckContext = getRundeckContext();
      const getPath = this.calcBrowsePath(this.path)
      
      rundeckContext.rundeckClient.storageKeyGetMetadata(getPath).then((result: any) => {
        this.directories = [];
        this.files = [];

        if(result.cacheStatus === 'LOADING') {
          this.loading = true
          this.countDown(selectedKey)
          return
        } 

        if (result.resources != null) {
          result.resources.forEach((resource: any) => {
            if (resource.type === 'directory') {
              this.directories.push(resource);
              if(this.directories.length > 1){
                this.directories.sort((obj1: any, obj2: any) => {
                  if (obj1.path > obj2.path) {
                    return 1;
                  }

                  if (obj1.path < obj2.path) {
                    return -1;
                  }
                  return 0;
                });
              }
            }

            if (resource.type === 'file') {
              if (this.storageFilter != null && this.storageFilter !== '') {
                if (this.allowedResource(resource.meta)) {
                  this.files.push(resource);
                }
              } else {
                this.files.push(resource);
              }

              this.files.sort((obj1: any, obj2: any) => {
                if (obj1.path > obj2.path) {
                  return 1;
                }

                if (obj1.path < obj2.path) {
                  return -1;
                }
                return 0;
              });
            }
          });
        }
        this.loading=false
      }).catch((err: Error) => {
        this.errorMsg = err.message
        this.loading=false
      });
    },
    allowedResource(meta: any) {
      const filterArray = this.storageFilter.split('=');
      const key = filterArray[0];
      const value = filterArray[1];
      if (key == 'Rundeck-key-type') {
        if (value === meta['rundeckKeyType']) {
          return true;
        }
      } else {
        if (key == 'Rundeck-data-type') {
          if (value === meta['Rundeck-data-type']) {
            return true;
          }
        }
      }
      return false;
    },
    duration(start: any) {
      return moment().diff(moment(start));
    },
    modifiedUsername() {
      var value = '';
      if (this.selectedKey != null &&
          this.selectedKey.meta != null &&
          this.selectedKey.meta['Rundeck-auth-modified-username']) {
        return this.selectedKey.meta['Rundeck-auth-modified-username'];
      }
      return value;
    },
    modifiedTimeAgoText() {
      var value = 0;
      if (this.selectedKey != null &&
          this.selectedKey.meta != null &&
          this.selectedKey.meta['Rundeck-content-modify-time'] != null) {
        var time = this.selectedKey.meta['Rundeck-content-modify-time'];
        value = this.duration(time);
      }
      return value;
    },
    wasModified() {
      if (this.selectedKey != null &&
          this.selectedKey.meta != null &&
          this.selectedKey.meta['Rundeck-content-creation-time'] != null &&
          this.selectedKey.meta['Rundeck-content-modify-time'] != null) {
        return this.selectedKey.meta['Rundeck-content-creation-time'] != this.selectedKey.meta['Rundeck-content-modify-time'];
      }
      return false;
    },
    createdUsername() {
      let value = '';
      if (this.selectedKey != null &&
          this.selectedKey.meta != null &&
          this.selectedKey.meta['Rundeck-auth-created-username'] != null) {
        return this.selectedKey.meta['Rundeck-auth-created-username'];
      }
      return value;
    },
    createdTime() {
      var value = '';
      if (this.selectedKey != null &&
          this.selectedKey.meta != null &&
          this.selectedKey.meta['Rundeck-content-creation-time'] != null) {
        value = this.selectedKey.meta['Rundeck-content-creation-time'];
      }
      return value;
    },
    dirNameString(path: any) {
      if (path.lastIndexOf('/') >= 0) {
        return path.substring(path.lastIndexOf('/') + 1);
      } else {
        return path;
      }
    },
    isPrivateKey(key: any) {
      return key && key.meta && key.meta['rundeckKeyType'] && key.meta["rundeckKeyType"] === 'private';

    },
    isPublicKey(key: any) {
      return key && key.meta && key.meta['rundeckKeyType'] && key.meta["rundeckKeyType"] === 'public';

    },
    isPassword(key: any) {
      return key && key.meta && key.meta["Rundeck-data-type"] === 'password';
    },
    notFound() {
      return false;
    },
    selectKey(key: any) {
      if (this.selectedKey.path === key.path && this.isSelectedKey==false) {
        this.isSelectedKey = true
      }
      else if (this.selectedKey.path === key.path) {
        this.selectedKey = {};
        this.isSelectedKey = false;
      }
      else {
        this.selectedKey = key;
        this.isSelectedKey = true;
      }

      this.$emit('input', this.selectedKey ? this.selectedKey.path : '');
    },
    parentDirString(path: any) {
      if (null != path && path.lastIndexOf('/') >= 0) {
        return path.substring(0, path.lastIndexOf('/'));
      } else {
        return '';
      }
    },
    actionUploadModify() {
      // Default to KeyType.Password
      let type = KeyType.Password;
      if (this.isPrivateKey(this.selectedKey)) {
        type = KeyType.Private;
      }
      if (this.isPublicKey(this.selectedKey)) {
        type = KeyType.Public;
      }

      const inputPath = this.relativePath(this.parentDirString(this.selectedKey.path));

      let inputType = InputType.Text;

      const upload = {
        modifyMode: true,
        keyType: type,
        inputPath: inputPath,
        inputType: inputType,
        fileName: this.selectedKey.name,
        file: '',
        fileContent: '',
        textArea: '',
        password: '',
        status: 'update',
        errorMsg: null as any,
      };
      
      this.$emit("openEditor", upload)
    },
    clean() {
      this.directories = [];
      this.files = [];
      this.selectedKey = {};
      this.isSelectedKey = false;
      this.inputPath = '';
      this.invalid = false;
      this.errorMsg = '';
      this.uploadErrors = {} as any;
    },
    loadDirInputPath() {
      if(this.isRunner) {
        this.loadDir(this.inputPath)
      } else {
        this.loadDir(this.rootPath + "/" + this.inputPath)
      }
    },
    defaultSelectKey(path: any) {
      const rundeckContext = getRundeckContext();

      rundeckContext.rundeckClient.storageKeyGetMetadata(this.calcBrowsePath(path)).then((result: any) => {
        if (result.resources != null) {
          result.resources.forEach((resource: any) => {
            if (resource.type === 'file') {
              if (resource.path === path) {
                this.selectedKey = resource;
                this.isSelectedKey = true;
              }
            }
          });
        }
      });

    },
    loadUpPath() {
      let upPath = '';
      if(this.isRunner) {
        // upPath is the path without trailing part
        if(!this.path) 
          this.upPath = ""
        else {
          const lastIndexOfSlash = this.path.lastIndexOf("/")
          if(lastIndexOfSlash >= 0)
            this.upPath = this.path.substring(0, lastIndexOfSlash)
          else
            this.upPath = ""
        } 
      } else {
        if (this.path != '' && this.path != this.rootPath && this.path != this.rootPath + '/') {
                if (this.path.indexOf('/') >= 0) {
                  upPath = this.parentDirString(this.path);
                } else {
                  upPath = this.rootPath;
                }

                if (upPath != this.rootPath) {
                  this.upPath = this.absolutePath(upPath);
                } else {
                  this.upPath = this.rootPath;
                }
              } else {
                this.upPath = upPath;
              }
      }
    },
    absolutePath(relpath: any) {
      if(this.isRunner) {
        return "/" + relpath
      }

      if (this.staticRoot === false) {
        return relpath;
      }
      return this.rootPath + "/" + relpath;
    },
    loadDir(selectedPath: any) {
      this.isDropdownOpen=false
      this.clean();
      let path = '';

      if(this.isRunner) {
        path = selectedPath
      } else {
        if (selectedPath != null) {
          path = this.relativePath(selectedPath);
        }
      }

      this.path = path;
      this.inputPath = path;

      this.loadUpPath();
      this.loadKeys();
    },
    showUpPath() {
      if(this.isRunner) {
        return this.upPath
      }

      if (this.upPath != this.rootPath) {
        return this.relativePath(this.upPath);
      } else {
        return this.upPath;
      }
    },
    relativePath(path: any) {
      const root = this.rootPath;
      const statroot = this.staticRoot;
      if (!statroot) {
        return path;
      }
      let newpath = '';
      if (path != null && root != null) {
        path = this.cleanPath(path);
        newpath = this.cleanPath(path.substring(root.length));
      }
      return newpath;
    },
    cleanPath(path: any) {
      if (path != null) {
        while (path.indexOf('/') == 0) {
          path = path.substring(1);
        }
      } else {
        return '';
      }
      return path;
    },
    actionUpload() {
      const upload = {
        modifyMode: false,
        keyType: KeyType.Private,
        inputPath: this.inputPath,
        inputType: InputType.Text,
        fileName: null,
        file: '',
        fileContent: '',
        textArea: '',
        password: '',
        status: 'new',
        errorMsg: null as any,
      };
      
      this.$emit("openEditor", upload)
    },
  }
})
</script>

<style>
   .keySelector span {
        content: " ";
        margin: 0 2px;
    }

    .keySelector i {
        content: " ";
        margin: 0 1px;
    }

    .keySelector-button-group button {
        content: " ";
        margin: 0 2px;
    }

    .label-key {
        vertical-align: middle
    }

    .input-group-addon {
      color: var(--font-color)
    }
</style>
