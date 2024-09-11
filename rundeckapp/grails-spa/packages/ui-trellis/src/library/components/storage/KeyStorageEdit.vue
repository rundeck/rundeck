<template>
  <div>
    <div v-if="!!uploadSetting.errorMsg" class="alert alert-danger">
      <span>{{ uploadSetting.errorMsg }}</span>
    </div>

    <div class="row">
      <div class="col-md-12">
        <div class="form-group row text-right">
          <label
            for="storageuploadtype"
            class="col-sm-3 control-label label-key"
          >
            Key Type:
          </label>
          <div class="col-sm-9">
            <select
              v-model="uploadSetting.keyType"
              name="uploadKeyType"
              class="form-control"
            >
              <option
                v-for="option in keyTypes"
                :key="option.value"
                :value="option.value"
              >
                {{ option.text }}
              </option>
            </select>
            <div class="help-block text-left">
              Private Keys and Passwords are not available to download once they
              are stored. Public keys can be downloaded.
            </div>
          </div>
        </div>

        <div
          class="form-group row text-right"
          :class="[validInput() === true ? 'has-success' : 'has-warning']"
        >
          <div
            v-if="uploadSetting.keyType !== 'password'"
            class="col-sm-3 label-key"
          >
            <select
              v-model="uploadSetting.inputType"
              class="form-control"
              name="inputType"
            >
              <option
                v-for="option in inputTypes"
                :key="option.value"
                :value="option.value"
              >
                {{ option.text }}
              </option>
            </select>
          </div>
          <label
            v-if="uploadSetting.keyType === 'password'"
            for="uploadpasswordfield"
            class="col-sm-3 control-label label-key"
          >
            Enter text
          </label>
          <div class="col-sm-9">
            <div
              v-if="
                uploadSetting.inputType === 'text' &&
                uploadSetting.keyType !== 'password'
              "
            >
              <textarea
                id="storageuploadtext"
                v-model="uploadSetting.textArea"
                class="form-control"
                rows="5"
                name="uploadText"
              ></textarea>
            </div>

            <div v-if="uploadSetting.inputType === 'file'">
              <input
                id="file"
                ref="file"
                type="file"
                @change="handleFileUpload"
              />
            </div>

            <div
              v-if="
                uploadSetting.inputType === 'text' &&
                uploadSetting.keyType === 'password'
              "
            >
              <input
                id="uploadpasswordfield"
                v-model="uploadSetting.password"
                name="uploadPassword"
                type="password"
                placeholder="Enter a password"
                autocomplete="new-password"
                class="form-control"
              />
            </div>
          </div>
        </div>

        <div class="form-group row text-right">
          <label
            for="uploadResourcePath2"
            class="col-sm-3 control-label label-key"
          >
            Storage path:
          </label>

          <div class="col-sm-9">
            <div class="input-group">
              <div class="input-group-addon">
                <span>{{ rootPath }}</span>
              </div>
              <input
                id="uploadResourcePath2"
                v-model="uploadSetting.inputPath"
                :disabled="uploadSetting.modifyMode === true"
                name="relativePath"
                class="form-control"
                placeholder="Enter the directory name"
              />
              <input
                id="uploadResourcePath3"
                v-model="uploadSetting.inputPath"
                :disabled="uploadSetting.modifyMode === false"
                type="hidden"
                name="relativePath"
              />
            </div>
          </div>
        </div>

        <div
          :class="[
            'form-group',
            'row',
            uploadSetting.fileName == null && uploadSetting.inputType !== 'file'
              ? 'has-warning'
              : '',
            uploadSetting.fileName != null && uploadSetting.inputType !== 'file'
              ? 'has-success'
              : '',
          ]"
        >
          <label
            for="uploadResourceName2"
            class="col-sm-3 control-label label-key text-right"
          >
            Name:
          </label>

          <div class="col-sm-9">
            <input
              id="uploadResourceName2"
              v-model="uploadSetting.fileName"
              :disabled="uploadSetting.modifyMode === true"
              name="fileName"
              class="form-control"
              data-testid="key-name-input"
              placeholder="Specify a name."
            />
            <div v-if="uploadSetting.inputType === 'file'" class="help-block">
              If not set, the name of the uploaded file is used.
            </div>
            <input
              id="uploadResourceName3"
              v-model="uploadSetting.fileName"
              type="hidden"
              :disabled="uploadSetting.modifyMode === false"
              name="fileName"
            />
          </div>
        </div>
        <div class="form-group row">
          <div class="col-sm-offset-3 col-sm-9">
            <div class="checkbox">
              <input
                v-model="uploadSetting.dontOverwrite"
                type="checkbox"
                value="true"
                name="dontOverwrite"
              />
              <label> Do not overwrite a file with the same name. </label>
            </div>
          </div>
        </div>

        <div class="form-group row">
          <div class="col-sm-12">
            <div class="help-block">
              <p>You can reference this stored Key using the storage path:</p>

              <p>
                <strong class="text-info">{{ uploadFullPath }}</strong>
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div>
      <div class="text-right">
        <button
          type="button"
          class="btn btn-default mr-3"
          data-testid="cancel-btn"
          @click="handleCancel"
        >
          Cancel
        </button>
        <button
          type="button"
          class="btn btn-cta"
          data-testid="save-btn"
          :disabled="validInput() === false"
          @click="handleUploadKey"
        >
          Save
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { getRundeckContext } from "../../index";
import { defineComponent } from "vue";
import type { PropType } from "vue";
import InputType from "../../types/InputType";
import KeyType from "../../types/KeyType";

export interface UploadSetting {
  modifyMode: boolean;
  keyType: KeyType;
  inputPath: string;
  inputType: InputType;
  fileName?: null | string;
  file?: null | string;
  fileContent: "";
  textArea: "";
  password: "";
  status: "new" | string;
  errorMsg: null | string;
  dontOverwrite: boolean;
}

export default defineComponent({
  name: "KeyStorageEdit",
  props: {
    storageFilter: {
      type: String,
      required: false,
      default: "",
    },
    uploadSetting: {
      type: Object as PropType<UploadSetting>,
      required: true,
    },
    project: String,
    rootPath: String,
  },
  emits: ["cancelEditing", "finishEditing", "keyCreated"],
  data() {
    return {
      modalEdit: false,
      path: "",
      errorMsg: "",
      directories: [] as any,
      files: [] as any,
      createdKey: {} as any,
      keyTypes: [
        { text: "Private Key", value: "privateKey" },
        { text: "Public Key", value: "publicKey" },
        { text: "Password", value: "password" },
      ],
      inputTypes: [
        { text: "Enter text", value: "text" },
        { text: "Upload File", value: "file" },
      ],
    };
  },
  computed: {
    uploadFullPath(): string {
      return this.rootPath + "/" + this.getKeyPath();
    },
    browsePath(): string {
      return this.calcBrowsePath(this.path);
    },
  },
  methods: {
    allowedResource(meta: any) {
      const filterArray = this.storageFilter.split("=");
      const key = filterArray[0];
      const value = filterArray[1];
      if (key == "Rundeck-key-type") {
        if (value === meta["rundeckKeyType"]) {
          return true;
        }
      } else {
        if (key == "Rundeck-data-type") {
          if (value === meta["Rundeck-data-type"]) {
            return true;
          }
        }
      }
      return false;
    },
    handleCancel() {
      this.$emit("cancelEditing");
    },
    validInput() {
      const intype = this.uploadSetting.inputType;
      const file = this.uploadSetting.file;
      const textarea = this.uploadSetting.textArea;
      const pass = this.uploadSetting.password;
      if (intype == "text") {
        return textarea || pass ? true : false;
      } else {
        return file ? true : false;
      }
    },
    async handleUploadKey() {
      const rundeckContext = getRundeckContext();

      const fullPath = this.calcBrowsePath(this.getKeyPath());

      let contentType = "application/pgp-keys";

      let value = null as any;

      switch (this.uploadSetting.keyType) {
        case KeyType.Password:
          contentType = "application/x-rundeck-data-password";
          value = this.uploadSetting.password;
          break;
        case KeyType.Private:
          contentType = "application/octet-stream";

          if (this.uploadSetting.inputType === InputType.Text) {
            value = this.uploadSetting.textArea;
          } else {
            if (this.uploadSetting.fileContent == "") {
              this.uploadSetting.errorMsg = "File content was not read";
              this.uploadSetting.file = null;
            } else {
              value = this.uploadSetting.fileContent;
            }
          }
          break;
        case KeyType.Public:
          if (this.uploadSetting.inputType === InputType.Text) {
            value = this.uploadSetting.textArea;
          } else {
            if (this.uploadSetting.fileContent == "") {
              this.uploadSetting.errorMsg = "File content was not read";
              this.uploadSetting.file = null;
            } else {
              value = this.uploadSetting.fileContent;
            }
          }
          break;
      }

      const checkKey = await rundeckContext.rundeckClient.storageKeyGetMaterial(
        fullPath,
        {},
      );

      const exists = checkKey._response.status !== 404;

      if (exists) {
        if (this.uploadSetting.dontOverwrite) {
          this.uploadSetting.errorMsg = "key aready exists";
          return;
        } else {
          rundeckContext.rundeckClient
            .storageKeyUpdate(fullPath, value, {
              contentType,
              inputType: this.uploadSetting.inputType,
              keyType: this.uploadSetting.keyType,
            })
            .then((result: any) => {
              this.$emit("finishEditing", result);
            })
            .catch((err: Error) => {
              let errorMessage = "";
              if (err?.message) {
                errorMessage = JSON.parse(err.message)?.message;
              }
              this.uploadSetting.errorMsg = errorMessage;
            });
        }
      } else {
        rundeckContext.rundeckClient
          .storageKeyCreate(fullPath, value, {
            contentType,
            inputType: this.uploadSetting.inputType,
            keyType: this.uploadSetting.keyType,
          })
          .then((result: any) => {
            this.getCreatedKey(fullPath).then((r: any) => {
              this.$emit("keyCreated", this.createdKey);
              this.$emit("finishEditing", result);
            });
          })
          .catch((err: Error) => {
            let errorMessage = "";
            if (err?.message) {
              errorMessage = JSON.parse(err.message)?.message;
            }
            this.uploadSetting.errorMsg = errorMessage;
          });
      }
    },
    async getCreatedKey(path: string) {
      const rundeckContext = getRundeckContext();
      const result =
        await rundeckContext.rundeckClient.storageKeyGetMetadata(path);
      if (result._response.status == 200) {
        this.createdKey = result;
      }
    },
    calcBrowsePath(path: string) {
      let browse = path;
      if (this.rootPath != "keys/") {
        browse = this.rootPath + "/" + path;
        browse = browse.substring(5);
      }
      return browse;
    },
    loadKeys() {
      const rundeckContext = getRundeckContext();
      rundeckContext.rundeckClient
        .storageKeyGetMetadata(this.browsePath)
        .then((result: any) => {
          this.directories = [];
          this.files = [];

          if (result.resources != null) {
            result.resources.forEach((resource: any) => {
              if (!resource) return;
              if (resource.type === "directory") {
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

              if (resource.type === "file") {
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
        })
        .catch((err: Error) => {
          this.errorMsg = err.message;
        });
    },
    handleFileUpload(e: any) {
      const files = e.target.files || e.dataTransfer.files;
      if (!files.length) return;

      const file = files[0];
      this.uploadSetting.file = file.name;

      const reader = new FileReader();
      reader.onload = (event: any) => {
        const text = event.target.result;
        this.uploadSetting.fileContent = text;
        if (this.uploadSetting.errorMsg != null) {
          this.uploadSetting.errorMsg = null;
        }
      };
      reader.onerror = (event: any) => {
        this.uploadSetting.errorMsg = "file cannot be read";
        this.uploadSetting.file = null;
      };
      reader.readAsText(file);
    },
    getKeyPath() {
      let fullPath =
        this.uploadSetting.inputPath != null &&
        this.uploadSetting.inputPath != ""
          ? this.uploadSetting.inputPath + "/"
          : "";

      if (this.uploadSetting.fileName != null) {
        fullPath = fullPath + this.uploadSetting.fileName;
      } else {
        if (this.uploadSetting.file != null) {
          fullPath = fullPath + this.uploadSetting.file;
        }
      }

      return fullPath;
    },
  },
});
</script>

<style scoped></style>
