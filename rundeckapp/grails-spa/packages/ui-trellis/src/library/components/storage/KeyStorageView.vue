<template>
<div>
  <div class="alert alert-warning" v-if="errorMsg!==''">
    <span>{{errorMsg}}</span>
  </div>

  <div class="row text-info ">
    <div class="form-group col-sm-12" :class="[invalid===true ? 'has-error' : '']">
      <div class="input-group">
        <div class="input-group-addon" v-if="staticRoot">
          <span>{{showRootPath}}</span>
        </div>
        <input type="text" class="form-control" style="padding-left:18px"
               v-model="inputPath" @keyup.enter="loadDirInputPath()"
               :disabled="readOnly"
               placeholder="Enter a path"/>
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
        <button @click="actionUpload()" class="btn btn-sm btn-cta" v-if="this.allowUpload===true">
          <i class="glyphicon glyphicon-plus"></i>
          Add or Upload a Keys
        </button>

        <button @click="actionUploadModify()" class="btn btn-sm btn-warning"
                v-if="this.allowUpload===true && this.isSelectedKey===true">
          <i class="glyphicon glyphicon-pencil"></i>
          Overwrite Key
        </button>
        <button class="btn btn-sm btn-danger" @click="deleteKey()" v-if="this.selectedKey.path !== null && this.selectedKey.path !== ''">
                <i class="glyphicon glyphicon-trash"></i>
                {{"Delete"}}</button>
      </div>


      <table class="table table-hover table-condensed">
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
        <tr v-for="key in files" :class="[key.path=== selectedKey.path ? selectedClass : '','action']"
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
            <span>{{key.name}}</span>
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
  <modal v-model="isConfirmingDeletion" title="Confirm Delete" id="storagedeletekey" ref="modalDelete" auto-focus append-to-body>
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" aria-hidden="true">&times;</button>
        <h4 class="modal-title" id="storageconfirmdeletetitle">{{"Delete Selected Key"}}</h4>
      </div>

      <div class="modal-body">
        <p>{{"Really delete the selected key at this path?"}} </p>

        <p>
          <strong class="text-info"> {{this.selectedKey.path}}</strong>
        </p>
      </div>

      <div class="modal-footer">
        <button type="button" @click="this.isConfirmingDeletion=false" class="btn btn-sm btn-default">{{"Cancel"}}</button>

        <button type="button" @click="confirmDeleteKey()" class="btn btn-sm btn-danger obs-storagedelete-select"> {{"Delete"}}</button>
      </div>
    </div>
  </modal>
  <div class="row" v-if="isKeySelected()===true">
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
        <div v-if="selectedIsDownloadable" class="pull-right">
          <a href="#" data-bind="click: download">
                <i class="glyphicon glyphicon-download"></i>
                {{"Download"}}</a>
        </div>
      </div>
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
import KeyStorageDelete from "./KeyStorageDelete.vue";

export default Vue.extend({
  name: "KeyStorageView",
  props: {
    readOnly: Boolean,
    allowUpload: Boolean,
    value: String,
    storageFilter: String,
  } ,
  data() {
    return {
      errorMsg: '',
      modalOpen: false,
      invalid: false,
      staticRoot: true,
      inputPath: '',
      upPath: '',
      rootPath: 'keys',
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
    }
  },
  computed: {
    showRootPath: function () {
      return "keys/"
    }
  },
  mounted() {
    console.log("==> load keys from path: ", this.path)
    this.loadKeys()
  },
  methods: {
    deleteKey(){
      this.isConfirmingDeletion=true
    },
    confirmDeleteKey(){
      const rundeckContext = getRundeckContext();
      rundeckContext.rundeckClient.storageKeyDelete(this.selectedKey.path)
      this.loadKeys()
      this.isConfirmingDeletion=false
    },
    loadKeys() {
      const rundeckContext = getRundeckContext();
      rundeckContext.rundeckClient.storageKeyGetMetadata(this.path).then((result: any) => {
        this.directories = [];
        this.files = [];

        if (result.resources != null) {
          result.resources.forEach((resource: any) => {
            if (resource.type === 'directory') {
              this.directories.push(resource);

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

            if (resource.type === 'file') {
              if (this.storageFilter != null) {
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
      }).catch((err: Error) => {
        this.errorMsg = err.message
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
    isKeySelected() {
      return this.selectedKey.path != null;

    },
    dirNameString(path: any) {
      if (path.lastIndexOf('/') >= 0) {
        return path.substring(path.lastIndexOf('/') + 1);
      } else {
        return path;
      }
    },
    isPrivateKey(key: any) {
      return key.meta['rundeckKeyType'] && key.meta["rundeckKeyType"] === 'private';

    },
    isPublicKey(key: any) {
      return key.meta['rundeckKeyType'] && key.meta["rundeckKeyType"] === 'public';

    },
    isPassword(key: any) {
      return key.meta["Rundeck-data-type"] === 'password';

    },
    notFound() {
      return false;
    },
    selectKey(key: any) {
      if (this.selectedKey != null && this.selectedKey.path === key.path) {
        this.selectedKey = {};
        this.isSelectedKey = false;
      } else {
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
      const isPassword = this.isPassword(this.selectedKey);
      const isPrivateKey = this.isPrivateKey(this.selectedKey);
      const isPublicKey = this.isPublicKey(this.selectedKey);

      // Default to KeyType.Password
      let type = KeyType.Password;
      if (isPrivateKey) {
        type = KeyType.Private;
      }
      if (isPublicKey) {
        type = KeyType.Public;
      }

      const inputPath = this.relativePath(this.parentDirString(this.selectedKey.path));

      let inputType = InputType.File;
      if (isPassword) {
        inputType = InputType.Text;
      }

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
      this.clean()
      this.loadDir(this.inputPath)
    },
    // openSelector() {
    //   if(this.readOnly!==true) {
    //     this.invalid = false;
    //     this.setRootPath();
    //     this.clean();
    //     if (this.value != null) {
    //       const parentDir = this.parentDirString(this.value)
    //       this.loadDir(parentDir);
    //       this.loadUpPath();
    //       this.defaultSelectKey(this.value);
    //     }
    //     this.modalOpen = true;
    //     this.$emit("openSelector")
    //   }
    // },
    defaultSelectKey(path: any) {
      const rundeckContext = getRundeckContext();

      rundeckContext.rundeckClient.storageKeyGetMetadata(this.path).then((result: any) => {
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
    },
    absolutePath(relpath: any) {
      var root = this.rootPath;
      var statroot = this.staticRoot;
      if (statroot === false) {
        return relpath;
      }
      return root + '/' + relpath;
    },
    setRootPath() {
      if (this.rootPath == null || this.rootPath === '') {
        this.rootPath = 'keys';
      }
      this.path = '';
    },
    loadDir(selectedPath: any) {
      this.clean();
      let path = '';
      if (selectedPath != null) {
        path = this.relativePath(selectedPath);
      }

      this.path = path;
      this.inputPath = path;

      this.checkParentDir(path)

      this.loadUpPath();
      this.loadKeys();
    },
    checkParentDir(path: any) {
      const rundeckContext = getRundeckContext();
      const fullPath = this.absolutePath(path);
      rundeckContext.rundeckClient.storageKeyGetMetadata(path).then((result: any) => {
        if (result.resources != null) {
          const keys = result.resources.filter((resource: any) => resource.path.indexOf(fullPath) >= 0);
          if (keys.length == 0) {
            this.invalid = true
            this.errorMsg = 'invalid path';
          }
        } else {
          this.invalid = true
          this.errorMsg = 'invalid path';
        }
      }).catch((err: Error) => {
        this.invalid = true
        this.errorMsg = `Failed to change parent directory. Error: ${err.message}`;
      });
    },
    showUpPath() {
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

    label-key {
        vertical-align: middle
    }
</style>