<template>
  <div>
    <div
      v-if="!!localUploadSetting.errorMsg"
      class="alert alert-danger"
      data-testid="error-msg"
    >
      <span>{{ localUploadSetting.errorMsg }}</span>
    </div>

    <div class="row">
      <div class="col-md-12">
        <div class="form-group row text-right">
          <!-- eslint-disable-next-line vuejs-accessibility/label-has-for -- label/for and select/id are correctly paired; the plugin additionally requires nesting, which this Bootstrap horizontal-form layout (label and control in separate grid columns) can't do without breaking the layout -->
          <label
            for="storageuploadtype"
            class="col-sm-3 control-label label-key"
          >
            Key Type:
          </label>
          <div class="col-sm-9">
            <select
              id="storageuploadtype"
              v-model="localUploadSetting.keyType"
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
            v-if="localUploadSetting.keyType !== 'password'"
            class="col-sm-3 label-key"
          >
            <select
              v-model="localUploadSetting.inputType"
              class="form-control"
              name="inputType"
              aria-label="Input Type"
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
          <!-- eslint-disable-next-line vuejs-accessibility/label-has-for -- label/for and input/id are correctly paired; the plugin additionally requires nesting, which this Bootstrap horizontal-form layout (label and control in separate grid columns) can't do without breaking the layout -->
          <label
            v-if="localUploadSetting.keyType === 'password'"
            for="uploadpasswordfield"
            class="col-sm-3 control-label label-key"
          >
            Enter text
          </label>
          <div class="col-sm-9">
            <div
              v-if="
                localUploadSetting.inputType === 'text' &&
                localUploadSetting.keyType !== 'password'
              "
            >
              <textarea
                id="storageuploadtext"
                v-model="localUploadSetting.textArea"
                class="form-control"
                rows="5"
                name="uploadText"
              ></textarea>
            </div>

            <div v-if="localUploadSetting.inputType === 'file'">
              <input
                id="file"
                ref="file"
                type="file"
                @change="handleFileUpload"
              />
            </div>

            <div
              v-if="
                localUploadSetting.inputType === 'text' &&
                localUploadSetting.keyType === 'password'
              "
            >
              <input
                id="uploadpasswordfield"
                v-model="localUploadSetting.password"
                name="uploadPassword"
                type="password"
                :placeholder="$t('storage.enter.password')"
                autocomplete="new-password"
                class="form-control"
              />
            </div>
          </div>
        </div>

        <div class="form-group row text-right">
          <!-- eslint-disable-next-line vuejs-accessibility/label-has-for -- label/for and input/id are correctly paired; the plugin additionally requires nesting, which this Bootstrap horizontal-form layout (label and control in separate grid columns) can't do without breaking the layout -->
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
                v-model="localUploadSetting.inputPath"
                :disabled="localUploadSetting.modifyMode === true"
                name="relativePath"
                class="form-control"
                data-testid="key-path-input"
                :placeholder="$t('storage.enter.directory.name')"
              />
              <input
                id="uploadResourcePath3"
                v-model="localUploadSetting.inputPath"
                :disabled="localUploadSetting.modifyMode === false"
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
            localUploadSetting.fileName == null &&
            localUploadSetting.inputType !== 'file'
              ? 'has-warning'
              : '',
            localUploadSetting.fileName != null &&
            localUploadSetting.inputType !== 'file'
              ? 'has-success'
              : '',
          ]"
        >
          <!-- eslint-disable-next-line vuejs-accessibility/label-has-for -- label/for and input/id are correctly paired; the plugin additionally requires nesting, which this Bootstrap horizontal-form layout (label and control in separate grid columns) can't do without breaking the layout -->
          <label
            for="uploadResourceName2"
            class="col-sm-3 control-label label-key text-right"
          >
            Name:
          </label>

          <div class="col-sm-9">
            <input
              id="uploadResourceName2"
              v-model="localUploadSetting.fileName"
              :disabled="localUploadSetting.modifyMode === true"
              name="fileName"
              class="form-control"
              data-testid="key-name-input"
              :placeholder="$t('storage.specify.name')"
            />
            <div
              v-if="localUploadSetting.inputType === 'file'"
              class="help-block"
            >
              If not set, the name of the uploaded file is used.
            </div>
            <input
              id="uploadResourceName3"
              v-model="localUploadSetting.fileName"
              type="hidden"
              :disabled="localUploadSetting.modifyMode === false"
              name="fileName"
            />
          </div>
        </div>
        <div class="form-group row">
          <div class="col-sm-offset-3 col-sm-9">
            <div class="checkbox">
              <label for="dontOverwrite">
                <input
                  id="dontOverwrite"
                  v-model="localUploadSetting.dontOverwrite"
                  type="checkbox"
                  value="true"
                  name="dontOverwrite"
                />
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
import {
  storageKeyCreate,
  storageKeyExists,
  storageKeyGetMetadata,
  storageKeyUpdate,
} from "../../services/storage";
import type { PropType } from "vue";
import { defineComponent } from "vue";
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
    uploadSetting: {
      type: Object as PropType<UploadSetting>,
      required: true,
    },
    project: { type: String, default: "" },
    rootPath: { type: String, default: "" },
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
      // Local editable copy of the `uploadSetting` prop: the form binds to this
      // instead of mutating the prop directly (see the `uploadSetting` watcher below).
      localUploadSetting: { ...this.uploadSetting } as UploadSetting,
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
  watch: {
    uploadSetting(newVal: UploadSetting) {
      this.localUploadSetting = { ...newVal };
    },
  },
  methods: {
    handleCancel() {
      this.$emit("cancelEditing");
    },
    validInput() {
      const intype = this.localUploadSetting.inputType;
      const file = this.localUploadSetting.file;
      const textarea = this.localUploadSetting.textArea;
      const pass = this.localUploadSetting.password;
      if (intype == "text") {
        return textarea || pass ? true : false;
      } else {
        return file ? true : false;
      }
    },
    validateKeyPath(): string | null {
      const path = this.getKeyPath();

      // A truly empty path is handled by the Save-button's validInput() / backend
      // "too short" check, so we don't flag it here. Whitespace-only input,
      // however, must NOT be short-circuited — it falls through to the backend
      // pattern check below and is caught by the leading-space branch.
      if (!path) {
        return null;
      }

      // Backend regex (StorageParams.groovy):
      //   ^\/?((?!\.\.(\/|$))[a-zA-Z0-9,.+_-][\sa-zA-Z0-9,.+_-]*?\/?)+$
      // Rules, per path component:
      //   - cannot be exactly ".." (directory traversal)
      //   - first char: [a-zA-Z0-9,.+_-] (no leading space)
      //   - subsequent chars: [\sa-zA-Z0-9,.+_-] (space allowed)
      const backendPattern =
        /^\/?((?!\.\.(\/|$))[a-zA-Z0-9,.+_-][\sa-zA-Z0-9,.+_-]*?\/?)+$/;

      if (backendPattern.test(path)) {
        return null;
      }

      const components = path.split("/").filter((c) => c.length > 0);

      // Only flag ".." when it is a full path component (matches backend behavior).
      // Strings like "foo..bar" are valid and must not be reported as traversal.
      if (components.some((component) => component === "..")) {
        return this.$t("storage.keyPath.error.traversal");
      }

      if (components.some((component) => component.startsWith(" "))) {
        return this.$t("storage.keyPath.error.leadingSpace");
      }

      const validChars = /^[a-zA-Z0-9,.+_\s/-]$/;
      const invalidChar = path.split("").find((char) => !validChars.test(char));
      if (invalidChar) {
        return this.$t("storage.keyPath.error.invalidChar", [invalidChar]);
      }

      return this.$t("storage.keyPath.error.invalidFormat");
    },
    async handleUploadKey() {
      const fullPath = this.calcBrowsePath(this.getKeyPath());

      // Validate path for new items only (skip for legacy items being edited)
      if (!this.localUploadSetting.modifyMode) {
        const pathError = this.validateKeyPath();
        if (pathError) {
          this.localUploadSetting.errorMsg = pathError;
          return;
        }
      }

      let value = null as any;

      switch (this.localUploadSetting.keyType) {
        case KeyType.Password:
          value = this.localUploadSetting.password;
          break;
        case KeyType.Private:
          if (this.localUploadSetting.inputType === InputType.Text) {
            value = this.localUploadSetting.textArea;
          } else {
            if (this.localUploadSetting.fileContent == "") {
              this.localUploadSetting.errorMsg = "File content was not read";
              this.localUploadSetting.file = null;
            } else {
              value = this.localUploadSetting.fileContent;
            }
          }
          break;
        case KeyType.Public:
          if (this.localUploadSetting.inputType === InputType.Text) {
            value = this.localUploadSetting.textArea;
          } else {
            if (this.localUploadSetting.fileContent == "") {
              this.localUploadSetting.errorMsg = "File content was not read";
              this.localUploadSetting.file = null;
            } else {
              value = this.localUploadSetting.fileContent;
            }
          }
          break;
      }

      const exists = await storageKeyExists(fullPath);

      if (exists) {
        if (this.localUploadSetting.dontOverwrite) {
          this.localUploadSetting.errorMsg = "key already exists";
          return;
        }
        try {
          const response = await storageKeyUpdate(fullPath, value, {
            type: this.localUploadSetting.keyType,
          });
          this.$emit("finishEditing", response);
        } catch (err: unknown) {
          let errorMessage = "";
          if (err && typeof err === "object" && "message" in err) {
            errorMessage = (err as Error).message;
          }
          this.localUploadSetting.errorMsg = errorMessage;
        }
      } else {
        try {
          const response = await storageKeyCreate(fullPath, value, {
            type: this.localUploadSetting.keyType,
          });
          this.getCreatedKey(fullPath).then(() => {
            this.$emit("keyCreated", this.createdKey);
            this.$emit("finishEditing", response);
          });
        } catch (err: unknown) {
          let errorMessage = "";
          if (err && typeof err === "object" && "message" in err) {
            errorMessage = (err as Error).message;
          }
          this.localUploadSetting.errorMsg = errorMessage;
        }
      }
    },
    async getCreatedKey(path: string) {
      try {
        this.createdKey = await storageKeyGetMetadata(path);
      } catch (err) {
        //todo: show error message
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
    handleFileUpload(e: any) {
      const files = e.target.files || e.dataTransfer.files;
      if (!files.length) return;

      const file = files[0];
      this.localUploadSetting.file = file.name;

      const reader = new FileReader();
      reader.onload = (event: any) => {
        const text = event.target.result;
        this.localUploadSetting.fileContent = text;
        if (this.localUploadSetting.errorMsg != null) {
          this.localUploadSetting.errorMsg = null;
        }
      };
      reader.onerror = () => {
        this.localUploadSetting.errorMsg = "file cannot be read";
        this.localUploadSetting.file = null;
      };
      reader.readAsText(file);
    },
    getKeyPath() {
      let fullPath =
        this.localUploadSetting.inputPath != null &&
        this.localUploadSetting.inputPath != ""
          ? this.localUploadSetting.inputPath + "/"
          : "";

      if (this.localUploadSetting.fileName != null) {
        fullPath = fullPath + this.localUploadSetting.fileName;
      } else {
        if (this.localUploadSetting.file != null) {
          fullPath = fullPath + this.localUploadSetting.file;
        }
      }

      return fullPath;
    },
  },
});
</script>

<style scoped>
.label-key {
  vertical-align: middle;
}
</style>
