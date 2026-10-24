<template>
  <div>
    <div v-if="errorMsg !== ''" class="alert alert-warning">
      <span>{{ errorMsg }}</span>
    </div>

    <div class="card">
      <div class="card-content">
        <div class="row text-info">
          <div
            class="form-group col-sm-12"
            :class="[invalid === true ? 'has-error' : '']"
          >
            <div class="input-group">
              <div v-if="staticRoot" class="input-group-addon bg-3">
                <span>{{ rootPath }}</span>
              </div>
              <input
                v-model="inputPath"
                type="text"
                class="form-control bg-2"
                style="padding-left: 18px"
                :placeholder="$t('storage.enter.path')"
                @keyup.enter="loadDirInputPath()"
              />
              <div
                v-if="!isProject && !readOnly"
                class="input-group-btn"
                :class="
                  isDropdownOpen ? 'open input-group-btn' : 'input-group-btn'
                "
              >
                <button
                  type="button"
                  class="btn btn-default dropdown-toggle"
                  :aria-expanded="isDropdownOpen"
                  @click="toggleDropdown"
                >
                  <span>{{ linksTitle }}</span>
                  <span class="caret"></span>
                </button>
                <ul
                  v-if="isDropdownOpen"
                  class="dropdown-menu dropdown-menu-right"
                >
                  <li v-for="link in jumpLinks" :key="link.path">
                    <a
                      href="#"
                      data-testid="load-dir-link"
                      @click="loadDir(link.path)"
                      >{{ link.name }}</a
                    >
                  </li>
                </ul>
              </div>
              <div v-if="isRunner" class="input-group-btn">
                <button
                  type="button"
                  class="btn btn-default"
                  @click="loadDir(path, true)"
                >
                  <span>{{ "Reload" }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
        <div class="row keySelector">
          <div class="col-sm-12">
            <div class="keySelector-button-group" style="margin-bottom: 1em">
              <button
                type="button"
                class="btn btn-sm btn-default"
                :disabled="upPath === ''"
                @click="loadDir(upPath)"
              >
                <i class="glyphicon glyphicon-folder-open"></i>
                <i class="glyphicon glyphicon-arrow-up"></i>
                <span>{{ showUpPath() }}</span>
              </button>
              <button
                v-if="!readOnly || allowUpload === true"
                class="btn btn-sm btn-cta"
                data-testid="add-key-btn"
                @click="actionUpload"
              >
                <i class="glyphicon glyphicon-plus"></i>
                Add or Upload a Key
              </button>

              <button
                v-if="
                  allowUpload === true && isSelectedKey === true && !readOnly
                "
                class="btn btn-sm btn-warning"
                data-testid="overwrite-key-btn"
                @click="actionUploadModify"
              >
                <i class="glyphicon glyphicon-pencil"></i>
                Overwrite Key
              </button>
              <button
                v-if="selectedKeyResource?.path && isSelectedKey && !readOnly"
                class="btn btn-sm btn-danger"
                data-testid="delete-key-btn"
                @click="deleteKey"
              >
                <i class="glyphicon glyphicon-trash"></i>
                {{ "Delete" }}
              </button>
            </div>

            <div
              v-if="loading"
              class="loading-area text-info"
              style="
                width: 100%;
                height: 200px;
                padding: 50px;
                background-color: #eee;
              "
            >
              <i class="glyphicon glyphicon-time">{{ "Loading..." }}</i>
              <div v-if="isRunner">
                <span v-if="countDownLimit > 0">
                  Reload from the remote Runner in {{ countDownLimit }} seconds
                </span>
                <span v-if="countDownLimit === 0"> Reload </span>
              </div>
            </div>
            <table v-if="!loading" class="table table-hover table-condensed">
              <tbody>
                <tr>
                  <td colspan="2" class="text-strong">
                    <span v-if="files.length < 1">No keys</span>
                    <span v-if="files.length > 0">
                      <span>{{ files.length }}</span>
                      keys
                    </span>
                  </td>
                </tr>
              </tbody>
              <tbody>
                <tr
                  v-for="key in files"
                  :key="key.name"
                  :class="[
                    selectedKeyResource && key.path === selectedKeyResource.path
                      ? selectedClass
                      : '',
                    'action',
                  ]"
                  @click="selectKey(key)"
                >
                  <td>
                    <i
                      :class="[
                        key.path === selectedKeyResource?.path
                          ? 'glyphicon glyphicon-ok'
                          : 'glyphicon glyphicon-unchecked',
                      ]"
                    ></i>

                    <span
                      v-if="isPrivateKey(key)"
                      title="This path contains a private key that can be used for remote node execution."
                    >
                      <i class="glyphicon glyphicon-lock"></i>
                    </span>
                    <span v-if="isPublicKey(key)">
                      <i class="glyphicon glyphicon-eye-open"></i>
                    </span>
                    <span
                      v-if="isPassword(key)"
                      title="This path contains a password that can be used for remote node execution."
                    >
                      <i class="glyphicon glyphicon-lock"></i>
                    </span>
                    <span data-testid="created-key">{{ key.name }}</span
                    ><span v-if="key.expired">{{ "[CACHE EXPIRED]" }}</span>
                  </td>
                  <td class="text-strong">
                    <span class="pull-right">
                      <span
                        v-if="isPrivateKey(key)"
                        title="This path contains a private key that can be used for remote node execution."
                      >
                        Private Key
                      </span>
                      <span v-if="isPublicKey(key)"> Public Key </span>
                      <span
                        v-if="isPassword(key)"
                        title="This path contains a password that can be used for remote node execution."
                      >
                        Password
                      </span>
                    </span>
                  </td>
                </tr>
              </tbody>

              <tbody v-if="notFound() === true">
                <tr>
                  <td colspan="2">
                    <span class="text-strong"
                      >Nothing found at this path.
                    </span>
                  </td>
                </tr>
              </tbody>
              <tbody>
                <tr v-for="directory in directories" :key="directory.name">
                  <td
                    data-testid="keyDirectoryButton"
                    class="action"
                    colspan="2"
                    @click="directory.path && loadDir(directory.path)"
                  >
                    <i class="glyphicon glyphicon-arrow-down"></i>
                    <i class="glyphicon glyphicon-folder-close"></i>
                    <span>{{ dirNameString(directory.path) }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <modal
          v-if="isConfirmingDeletion === true"
          id="storagedeletekey"
          ref="modalDelete"
          v-model="isConfirmingDeletion"
          title="Delete Selected Key"
          auto-focus
          append-to-body
          :footer="false"
        >
          <div class="modal-body">
            <p>{{ "Really delete the selected key at this path?" }}</p>

            <p>
              <strong class="text-info">
                {{ selectedKeyResource?.path }}
              </strong>
            </p>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              data-testid="confirm-delete-btn"
              class="btn btn-sm btn-danger obs-storagedelete-select"
              @click="confirmDeleteKey"
            >
              {{ "Delete" }}
            </button>
            <button
              type="button"
              class="pull-right btn btn-sm btn-default"
              @click="cancelDeleteKey"
            >
              {{ "Cancel" }}
            </button>
          </div>
        </modal>
        <div
          v-if="isSelectedKey && selectedKeyResource?.type === 'file'"
          class="row"
        >
          <div class="col-sm-12">
            <div class="well">
              <div>
                Storage path:
                <code class="text-success">{{
                  selectedKeyResource?.path
                }}</code>
                <a
                  href="#"
                  data-bind="attr: { href: selectedPathUrl() }"
                  :aria-label="$t('storage.path.link.label')"
                >
                  <i class="glyphicon glyphicon-link"></i>
                </a>
              </div>
              <div v-if="createdTime !== ''">
                <div>
                  Created:
                  <span class="timeabs text-strong">
                    {{ formatKeyStorageCreatedTime }}
                  </span>

                  <span v-if="createdUsername() !== ''">
                    by:
                    <span class="text-strong">{{ createdUsername() }}</span>
                  </span>
                </div>
              </div>
              <div v-if="wasModified() !== ''">
                <div>
                  Modified:
                  <span class="timeago text-strong">
                    {{ formatHumanizedModifiedTimeAgoDuration }} ago
                  </span>

                  <span v-if="modifiedUsername() !== ''">
                    by:
                    <span class="text-strong">{{ modifiedUsername() }}</span>
                  </span>
                </div>
              </div>
              <div
                v-if="selectedKeyResource && isPublicKey(selectedKeyResource)"
                class="pull-right"
              >
                <span>
                  <a :href="downloadUrl()">
                    <i class="glyphicon glyphicon-download"></i>
                    {{ "Download" }}</a
                  >
                </span>
              </div>
              <div v-if="selectedKeyResource?.expired" class="pull-right">
                <span>
                  <a
                    :href="'javascript:void(0)'"
                    @click="loadKeys(selectedKeyResource)"
                  >
                    <i class="glyphicon glyphicon-download"></i>
                    {{ "Refresh Expired Key" }}</a
                  >
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="card-footer">
        <hr />
        <span class="text-info">
          {{
            $t(
              "Key Storage provides a global directory-like structure to save Public and Private Keys and Passwords, for use with Node Execution authentication.",
            )
          }}
        </span>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { listProjects } from "../../services/projects";
import {
  storageKeyDelete,
  storageKeyGetMetadata,
  type StorageKeyMetadata,
  StorageKeyListResponse,
} from "../../services/storage";
import moment from "moment";
import { getRundeckContext } from "../../index";
import { defineComponent, PropType } from "vue";
import KeyType from "../../types/KeyType";
import InputType from "../../types/InputType";
import {
  formatHumanizedDuration,
  formatKeyStorageDate,
} from "../../utilities/DateTimeFormatters";
import type { UploadSetting } from "./KeyStorageEdit.vue";

interface StorageResource extends StorageKeyMetadata {
  expired?: boolean;
}

interface StorageListResult extends StorageKeyListResponse {
  cacheStatus?: string;
  resources?: StorageResource[];
}

type SelectedStorageValue = StorageResource | string | null;

const selectedResource = (
  value: SelectedStorageValue,
): StorageResource | undefined =>
  value !== null && typeof value === "object" ? value : undefined;

const metadataValue = (
  meta: Record<string, unknown> | undefined,
  key: string,
): string | undefined => {
  const value = meta?.[key];
  return typeof value === "string" ? value : undefined;
};

export default defineComponent({
  name: "KeyStorageView",
  props: {
    readOnly: Boolean,
    allowUpload: Boolean,
    modelValue: String,
    storageFilter: String,
    rootPath: String,
    createdKey: {
      type: Object as PropType<StorageResource>,
      default: undefined,
    },
    runnerId: String,
    getKeyMetadata: {
      type: Function as PropType<
        (
          path: string,
          options?: { queryParameters?: { refresh?: string } },
        ) => Promise<StorageListResult>
      >,
      default: storageKeyGetMetadata,
      required: true,
    },
  },
  emits: ["update:modelValue", "openEditor"],
  data() {
    return {
      errorMsg: "",
      isDropdownOpen: false,
      modalOpen: false,
      invalid: false,
      project: "",
      staticRoot: true,
      inputPath: "",
      upPath: "",
      path: "",
      isConfirmingDeletion: false,
      modalEdit: false,
      upload: {} as Partial<UploadSetting>,
      selectedKey: null as SelectedStorageValue,
      isSelectedKey: false,
      files: [] as StorageResource[],
      selectedClass: "success",
      directories: [] as StorageResource[],
      uploadErrors: {} as Record<string, unknown>,
      selectedIsDownloadable: true,
      loading: true,
      linksTitle: "",
      projectList: [],
      jumpLinks: [] as Array<{ name: string | undefined; path: string }>,
      countDownLimit: 0,
      countDownInterval: 0,
    };
  },
  computed: {
    selectedKeyResource(): StorageResource | undefined {
      return selectedResource(this.selectedKey);
    },
    formatKeyStorageCreatedTime() {
      return formatKeyStorageDate(this.createdTime);
    },
    formatHumanizedModifiedTimeAgoDuration() {
      return formatHumanizedDuration(this.modifiedTimeAgoText);
    },
    isProject(): boolean {
      return this.rootPath?.startsWith("keys/project") || false;
    },
    createdTime() {
      const selectedKey = selectedResource(this.selectedKey);
      return (
        metadataValue(selectedKey?.meta, "Rundeck-content-creation-time") ?? ""
      );
    },
    modifiedTimeAgoText() {
      const selectedKey = selectedResource(this.selectedKey);
      const time = metadataValue(
        selectedKey?.meta,
        "Rundeck-content-modify-time",
      );
      return time ? this.duration(time) : 0;
    },
    isRunner(): boolean {
      return this.rootPath ? this.rootPath.startsWith("runner:") : false;
    },
  },
  watch: {
    createdKey: function (newValue) {
      if (newValue) {
        this.selectKey(newValue);
      }
    },
    rootPath: function () {
      // Reset current path when rootPath changed.

      this.path = "";
      this.inputPath = "";
      this.selectedKey = null;
      this.loadKeys();
    },
  },
  mounted() {
    this.loadKeys();
    this.loadProjectNames();
  },
  unmounted() {
    if (this.countDownInterval > 0) {
      clearInterval(this.countDownInterval);
      this.countDownInterval = 0;
    }
  },
  methods: {
    downloadUrl(): string {
      const downloadBaseUrl = "storage/download/keys";
      const rundeckContext = getRundeckContext();
      const key = selectedResource(this.selectedKey);

      if (!key?.path) return "#";

      return `${rundeckContext.rdBase}/${downloadBaseUrl}?resourcePath=${encodeURIComponent(key.path)}`;
    },
    async loadProjectNames() {
      try {
        const response = await listProjects();

        this.linksTitle = "Projects";
        this.jumpLinks = response.map((v) => {
          return { name: v.name, path: "keys/project/" + v.name };
        });
      } catch (error) {
        console.error("Error loading project names:", error);
      }
    },
    toggleDropdown() {
      this.isDropdownOpen = !this.isDropdownOpen;
    },
    deleteKey() {
      this.isConfirmingDeletion = true;
    },
    async confirmDeleteKey() {
      this.isConfirmingDeletion = false;
      const key = selectedResource(this.selectedKey);
      if (!key?.path) return;

      try {
        const resp = await storageKeyDelete(key.path.slice(5));
        if (!resp) {
          this.errorMsg = "Not found";
          return;
        }
      } catch (e) {
        this.errorMsg = (e as Error).message;
        return;
      }
      this.selectedKey = null;
      this.isSelectedKey = false;

      this.loadKeys();
    },
    cancelDeleteKey() {
      this.isConfirmingDeletion = false;
    },
    calcBrowsePath(path: string) {
      let browse = path;

      if (this.isRunner) {
        return path;
      }

      if (this.rootPath != "keys/" && this.rootPath != "keys") {
        browse = this.rootPath + "/" + path;
        browse = browse.substring(5);
      }
      return browse;
    },
    countDown(selectedKey?: StorageResource) {
      if (this.countDownLimit > 0) return;
      this.countDownLimit = 5;
      this.countDownInterval = window.setInterval(() => {
        this.countDownLimit--;

        if (this.countDownLimit <= 0) {
          clearInterval(this.countDownInterval);
          this.countDownInterval = 0;

          const delayExec = setTimeout(() => {
            this.loadKeys(selectedKey);
            clearTimeout(delayExec);
          }, 600); // Delay 600ms to execute to give better user experience.
        }
      }, 1000);
    },
    loadKeys(selectedKey?: SelectedStorageValue, forceRefresh?: boolean) {
      if (selectedKey) {
        this.selectedKey = selectedKey;
      }
      this.loading = true;

      const getPath = this.calcBrowsePath(this.path);

      const requestOptions = {
        queryParameters: forceRefresh ? { refresh: "true" } : {},
      };
      this.getKeyMetadata(getPath, requestOptions)
        .then((result) => {
          this.directories = [];
          this.files = [];

          if (result.cacheStatus === "LOADING") {
            this.loading = true;
            this.countDown(
              typeof selectedKey === "object" && selectedKey !== null
                ? selectedKey
                : undefined,
            );
            return;
          }

          if (result.resources != null) {
            result.resources.forEach((resource: StorageResource) => {
              if (resource.type === "directory") {
                this.directories.push(resource);
                if (this.directories.length > 1) {
                  this.directories.sort((obj1, obj2) => {
                    const path1 = obj1.path ?? "";
                    const path2 = obj2.path ?? "";
                    if (path1 > path2) return 1;
                    if (path1 < path2) return -1;
                    return 0;
                  });
                }
              }

              if (resource.type === "file") {
                if (this.storageFilter != null && this.storageFilter !== "") {
                  if (this.allowedResource(resource.meta)) {
                    this.files.push(resource);
                  }
                } else {
                  this.files.push(resource);
                }

                this.files.sort((obj1, obj2) => {
                  const path1 = obj1.path ?? "";
                  const path2 = obj2.path ?? "";
                  if (path1 > path2) return 1;
                  if (path1 < path2) return -1;
                  return 0;
                });
              }
            });
          }

          this.loading = false;
        })
        .catch((err: Error) => {
          try {
            const jsonError = JSON.parse(err.message);

            if (
              jsonError.message ===
              "Error: Key browsing is not allowed in CCP mode."
            ) {
              // Generate sample key path string
              const sampleKeyPath = this.rootPath?.endsWith("/")
                ? `${this.rootPath}${this.path}`
                : `${this.rootPath}/${this.path}`;

              // Remove the Error prefix
              const softenedMessage = jsonError.message.startsWith("Error: ")
                ? jsonError.message.substring("Error: ".length)
                : jsonError.message;

              this.errorMsg = `${softenedMessage} If you need to select a key, please type the path of the key with the prefix "${sampleKeyPath}" into the form field. E.g. ${sampleKeyPath}/path/to/the/key`;
            } else {
              this.errorMsg = jsonError.message;
            }
          } catch (parseError) {
            this.errorMsg = err.message;
          }

          this.loading = false;
        });
    },
    allowedResource(meta?: Record<string, unknown>) {
      const filterArray = this.storageFilter?.split("=") || ["", ""];
      const key = filterArray[0];
      const value = filterArray[1];
      if (key == "Rundeck-key-type") {
        if (
          value === metadataValue(meta, "rundeckKeyType") ||
          value === metadataValue(meta, "Rundeck-key-type")
        ) {
          return true;
        }
      } else {
        if (key == "Rundeck-data-type") {
          if (value === metadataValue(meta, "Rundeck-data-type")) {
            return true;
          }
        }
      }
      return false;
    },
    duration(start: string) {
      return moment().diff(moment(start));
    },
    modifiedUsername() {
      const selectedKey = selectedResource(this.selectedKey);
      return (
        metadataValue(selectedKey?.meta, "Rundeck-auth-modified-username") ?? ""
      );
    },
    wasModified() {
      const selectedKey = selectedResource(this.selectedKey);
      const created = metadataValue(
        selectedKey?.meta,
        "Rundeck-content-creation-time",
      );
      const modified = metadataValue(
        selectedKey?.meta,
        "Rundeck-content-modify-time",
      );
      return created !== undefined && modified !== undefined
        ? created !== modified
        : false;
    },
    createdUsername() {
      const selectedKey = selectedResource(this.selectedKey);
      return (
        metadataValue(selectedKey?.meta, "Rundeck-auth-created-username") ?? ""
      );
    },
    dirNameString(path?: string) {
      if (!path) {
        return "";
      }
      if (path.lastIndexOf("/") >= 0) {
        return path.substring(path.lastIndexOf("/") + 1);
      } else {
        return path;
      }
    },
    isPrivateKey(key: StorageResource) {
      let keyType = metadataValue(key.meta, "rundeckKeyType");
      if (keyType == null) {
        keyType = metadataValue(key.meta, "Rundeck-key-type");
      }

      return keyType === "private";
    },
    isPublicKey(key: StorageResource) {
      let keyType = metadataValue(key.meta, "rundeckKeyType");
      if (keyType == null) {
        keyType = metadataValue(key.meta, "Rundeck-key-type");
      }
      return keyType === "public";
    },
    isPassword(key: StorageResource) {
      return metadataValue(key.meta, "Rundeck-data-type") === "password";
    },
    notFound() {
      return false;
    },
    selectKey(key: StorageResource) {
      const currentKey = selectedResource(this.selectedKey);
      if (currentKey?.path === key.path && this.isSelectedKey == false) {
        this.isSelectedKey = true;
      } else if (currentKey?.path === key.path) {
        this.selectedKey = null;
        this.isSelectedKey = false;
      } else {
        this.selectedKey = key;
        this.isSelectedKey = true;
      }

      this.$emit(
        "update:modelValue",
        selectedResource(this.selectedKey)?.path || "",
      );
    },
    parentDirString(path: string) {
      if (null != path && path.lastIndexOf("/") >= 0) {
        return path.substring(0, path.lastIndexOf("/"));
      } else {
        return "";
      }
    },
    actionUploadModify() {
      const selectedKey = selectedResource(this.selectedKey);
      if (!selectedKey?.path) return;

      // Default to KeyType.Password
      let type = KeyType.Password;
      if (this.isPrivateKey(selectedKey)) {
        type = KeyType.Private;
      }
      if (this.isPublicKey(selectedKey)) {
        type = KeyType.Public;
      }

      const inputPath = this.relativePath(
        this.parentDirString(selectedKey.path),
      );
      const inputType = InputType.Text;

      const upload = {
        modifyMode: true,
        keyType: type,
        inputPath: inputPath,
        inputType: inputType,
        fileName: selectedKey.name,
        file: "",
        fileContent: "",
        textArea: "",
        password: "",
        status: "update",
        errorMsg: null,
      };
      this.$emit("openEditor", upload);
    },
    clean() {
      this.directories = [];
      this.files = [];
      this.selectedKey = null;
      this.isSelectedKey = false;
      this.inputPath = "";
      this.invalid = false;
      this.errorMsg = "";
      this.uploadErrors = {};
    },
    loadDirInputPath() {
      if (this.isRunner) {
        this.loadDir(this.inputPath);
      } else {
        this.loadDir(this.rootPath + "/" + this.inputPath);
      }
    },
    defaultSelectKey(path: string) {
      storageKeyGetMetadata(this.calcBrowsePath(path)).then((result) => {
        if (result.resources != null) {
          result.resources.forEach((resource: StorageKeyMetadata) => {
            if (resource.type === "file") {
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
      let upPath = "";
      if (this.isRunner) {
        // upPath is the path without trailing part
        if (!this.path) this.upPath = "";
        else {
          const lastIndexOfSlash = this.path.lastIndexOf("/");
          if (lastIndexOfSlash >= 0)
            this.upPath = this.path.substring(0, lastIndexOfSlash);
          else this.upPath = "";
        }
      } else {
        if (
          this.path != "" &&
          this.path != this.rootPath &&
          this.path != this.rootPath + "/"
        ) {
          if (this.path.indexOf("/") >= 0) {
            upPath = this.parentDirString(this.path);
          } else {
            upPath = this.rootPath || "";
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
    absolutePath(relpath: string) {
      if (this.isRunner) {
        return "/" + relpath;
      }

      if (this.staticRoot === false) {
        return relpath;
      }
      return this.rootPath + "/" + relpath;
    },
    loadDir(selectedPath: string, forceRefresh?: boolean) {
      this.isDropdownOpen = false;
      this.clean();
      let path = "";

      if (this.isRunner) {
        path = selectedPath;
      } else {
        if (selectedPath != null) {
          path = this.relativePath(selectedPath);
        }
      }

      this.path = path;
      this.inputPath = path;

      this.loadUpPath();

      this.loadKeys(path, forceRefresh);
    },
    showUpPath() {
      if (this.isRunner) {
        return this.upPath;
      }

      if (this.upPath != this.rootPath) {
        return this.relativePath(this.upPath);
      } else {
        return this.upPath;
      }
    },
    relativePath(path: string) {
      const root = this.rootPath;
      const statroot = this.staticRoot;

      if (!statroot) {
        return path;
      }
      let newpath = "";
      if (path != null && root != null) {
        path = this.cleanPath(path);

        newpath = this.cleanPath(path.substring(root.length));
      }
      return newpath;
    },

    cleanPath(path: string | null | undefined): string {
      if (path != null) {
        while (path.indexOf("/") == 0) {
          path = path.substring(1);
        }
      } else {
        return "";
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
        file: "",
        fileContent: "",
        textArea: "",
        password: "",
        status: "new",
        errorMsg: null,
      };

      this.$emit("openEditor", upload);
    },
  },
});
</script>

<!-- eslint-disable-next-line vue/enforce-style-attribute -- defines page-wide input-group-addon color -->
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
  vertical-align: middle;
}

.input-group-addon {
  color: var(--font-color);
}
</style>
