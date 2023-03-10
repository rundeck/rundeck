<template>
  <div class="alert alert-danger" v-if="upload.errorMsg!==null">
    <span>{{upload.errorMsg}}</span>
  </div>

  <div class="row">
    <div class="col-md-12">
      <div class="form-group row text-right">
        <label for="storageuploadtype" class="col-sm-3 control-label label-key">
          Key Type:
        </label>
        <div class="col-sm-9">
          <select name="uploadKeyType" class="form-control" v-model="upload.keyType">
            <option v-for="option in keyTypes" v-bind:value="option.value">
              {{ option.text }}
            </option>
          </select>
          <div class="help-block text-left">
            Private Keys and Passwords are not available to download once they are stored. Public
            keys can be downloaded.
          </div>
        </div>
      </div>

      <div class="form-group row text-right" :class="[validInput()===true?'has-success':'has-warning']">
        <div class="col-sm-3 label-key" v-if="upload.keyType!=='password'">
          <select class="form-control" name="inputType" v-model="upload.inputType">
            <option v-for="option in inputTypes" v-bind:value="option.value">
              {{ option.text }}
            </option>
          </select>
        </div>
        <label for="uploadpasswordfield" class="col-sm-3 control-label label-key"
               v-if="upload.keyType==='password'">
          Enter text
        </label>
        <div class="col-sm-9">

          <div v-if="upload.inputType==='text' && upload.keyType!=='password'">
                                <textarea class="form-control" rows="5" id="storageuploadtext" v-model="upload.textArea"
                                          name="uploadText"></textarea>
          </div>

          <div v-if="upload.inputType==='file' ">
            <input type="file" id="file" ref="file" @change="handleFileUpload"/>

          </div>

          <div v-if="upload.inputType==='text' && upload.keyType==='password' ">
            <input name="uploadPassword" type="password" placeholder="Enter a password"
                   autocomplete="new-password" v-model="upload.password" id="uploadpasswordfield"
                   class="form-control"/>
          </div>
        </div>
      </div>

      <div class="form-group row text-right">
        <label for="uploadResourcePath2" class="col-sm-3 control-label label-key">
          Storage path:
        </label>

        <div class="col-sm-9">
          <div class="input-group">
            <div class="input-group-addon">
              <span>{{showRootPath}}</span>
            </div>
            <input v-model="upload.inputPath" :disabled="upload.modifyMode===true"
                   id="uploadResourcePath2" name="relativePath" class="form-control"
                   placeholder="Enter the directory name"/>
            <input v-model="upload.inputPath" :disabled="upload.modifyMode===false"
                   id="uploadResourcePath3" type="hidden" name="relativePath"/>
          </div>
        </div>
      </div>

      <div :class="['form-group','row',upload.fileName==null&&upload.inputType!=='file'?'has-warning':'', upload.fileName!=null&&upload.inputType!=='file'?'has-success':'']">
        <label for="uploadResourceName2" class="col-sm-3 control-label label-key text-right">
          Name:
        </label>

        <div class="col-sm-9">
          <input id="uploadResourceName2" v-model="upload.fileName"
                 :disabled="upload.modifyMode===true" name="fileName" class="form-control"
                 placeholder="Specify a name."/>
          <div class="help-block" v-if="upload.inputType === 'file'">
            If not set, the name of the uploaded file is used.
          </div>
          <input id="uploadResourceName3" type="hidden" v-model="upload.fileName"
                 :disabled="upload.modifyMode===false" name="fileName"/>
        </div>
      </div>
      <div class="form-group row">
        <div class=" col-sm-offset-3 col-sm-9">
          <div class="checkbox">
            <input type="checkbox" value="true" name="dontOverwrite"
                   v-model="upload.dontOverwrite"/>
            <label>
              Do not overwrite a file with the same name.
            </label>
          </div>
        </div>
      </div>

      <div class="form-group row">
        <div class="col-sm-12">
          <div class="help-block">
            <p>You can reference this stored Key using the storage path:</p>

            <p>
              <strong class="text-info">{{uploadFullPath}}</strong>
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div slot="footer">
    <button type="button" class="btn btn-sm btn-default" @click="">Cancel</button>
    <button type="button" class="btn btn-sm btn-success" :disabled="validInput()===false"
            @click="handleUploadKey">
      Save
    </button>
  </div>
</template>

<script lang="ts">
import {getRundeckContext} from "../../index";
import {InputType, KeyType} from "../plugins/KeyStorageSelector.vue";
import Vue from "vue";

export default Vue.extend({
  name: "KeyStorageEdit",
  props: [
      'storageFilter'
  ],
  data() {
    return {
      upload: {} as any,
      modalEdit: false,
      path: '',
      errorMsg: '',
      directories: [] as any,
      files: [] as any,
      keyTypes: [
        {text: 'Private Key', value: 'private'},
        {text: 'Public Key', value: 'public'},
        {text: 'Password', value: 'password'},
      ],
      inputTypes: [
        {text: 'Enter text', value: 'text'},
        {text: 'Upload File', value: 'file'},
      ],
    }
  },
  methods: {
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
    handleCancel(){
      this.modalEdit=false
      this.$emit("closeEditor")
    },
    validInput() {
      var intype = this.upload.inputType;
      var file = this.upload.file;
      var textarea = this.upload.textArea;
      var pass = this.upload.password;
      if (intype == 'text') {
        return (textarea || pass) ? true : false;
      } else {
        return file ? true : false;
      }
    },
    async handleUploadKey() {
      const rundeckContext = getRundeckContext();

      let fullPath = this.getKeyPath();

      let type = KeyType.Public;
      let contentType = 'application/pgp-keys';

      let value = null as any;

      switch (this.upload.keyType) {
        case 'password':
          type = KeyType.Password;
          contentType = 'application/x-rundeck-data-password';
          value = this.upload.password;
          break;
        case 'private':
          type = KeyType.Private;
          contentType = 'application/octet-stream';

          if (this.upload.inputType === InputType.Text) {
            value = this.upload.textArea;
          } else {
            if(this.upload.fileContent ==''){
              this.upload.errorMsg = 'File content was not read';
              this.upload.file = null;
            }else{
              value = this.upload.fileContent;
            }
          }
          break;
        case 'public':
          if (this.upload.inputType === InputType.Text) {
            value = this.upload.textArea;
          } else {
            if(this.upload.fileContent ==''){
              this.upload.errorMsg = 'File content was not read';
              this.upload.file = null;
            }else{
              value = this.upload.fileContent;
            }
          }
          break;
      }

      let saved = true;

      if (this.upload.status == 'new') {

        const checkKey = await rundeckContext.rundeckClient.storageKeyGetMaterial(fullPath);
        let exists = true;
        if(checkKey._response.status==404){
          exists = false;
        }

        if(exists && this.upload.dontOverwrite ) {
          this.upload.errorMsg = 'key aready exists';
          saved = false;
        }else{
          if(!exists) {
            const resp = await rundeckContext.rundeckClient.storageKeyCreate(fullPath, value, {contentType});
            if(resp._response.status!=201){
              saved = false;
              this.upload.errorMsg = resp.error;
            }
          }else {
            this.upload.status = 'update';
          }
        }
      }

      if (this.upload.status === 'update') {
        const resp = await rundeckContext.rundeckClient.storageKeyUpdate(fullPath, value, {contentType});

        if(resp._response.status!=201){
          saved = false;
          this.upload.errorMsg = resp.error;
        }

        saved = true;
      }

      if(saved){
        this.loadKeys();
        this.modalEdit = false;
        this.$emit("closeEditor")
      }
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
      }).catch((err) => {
        this.errorMsg = err;
      });
    },
    handleFileUpload(e: any){
      var files = e.target.files || e.dataTransfer.files;
      if (!files.length)
        return;

      const file = files[0];
      this.upload.file = file.name;

      let reader = new FileReader();
      reader.onload = (event:any) => {
        let text = event.target.result;
        this.upload.fileContent = text;
        if(this.upload.errorMsg!=null){
          this.upload.errorMsg = null;
        }
      };
      reader.onerror = (event:any) => {
        this.upload.errorMsg = "file cannot be read";
        this.upload.file = null;
      };
      reader.readAsText(file);

    },
    getKeyPath(){
      let fullPath = this.upload.inputPath!=null && this.upload.inputPath!=''? this.upload.inputPath + '/':'';

      if(this.upload.fileName != null) {
        fullPath = fullPath + this.upload.fileName;
      }else{
        if(this.upload.file!=null){
          fullPath = fullPath + this.upload.file;
        }
      }

      return fullPath;

    }
  },
  computed: {
    showRootPath: function () {
      return "keys/"
    },
    uploadFullPath(): string {
      return 'keys/' + this.getKeyPath();
    },
  }
})
</script>

<style scoped>

</style>