<script lang="ts">
import { JobBrowseItem } from "@/library/types/jobs/JobBrowse";
import BrowserJobItem from "../browse/tree/BrowserJobItem.vue";
import { defineComponent } from "vue";
import { api } from "../../../../library/services/api";
import { RecycleScroller } from "vue-virtual-scroller";
export default defineComponent({
  name: "JobUploadPage",
  components: { RecycleScroller, BrowserJobItem },
  data() {
    return {
      fileformat: "xml",
      dupeOption: "update",
      uuidOption: "preserve",
      validateJobref: "false",
      project: "",
      uploading: false,
      selectedFile: null,
      // Response data
      messages: [],
      errjobs: [],
      skipjobs: [],
      jobs: [],
      didupload: false,
      error: null,
    };
  },
  computed: {
    browseJobs(): JobBrowseItem[] {
      return this.jobs.map((job) => ({
        ...job,
        jobName: job.name,
        groupPath: job.group,
        job: true,
        id: job.id,
      }));
    },
  },
  created() {
    // Fallback to RD_PROJECT if available
    this.project = (window as any)._rundeck?.projectName || "";
  },
  methods: {
    onFileChange(event: Event) {
      const target = event.target as HTMLInputElement;
      this.selectedFile = target.files ? target.files[0] : null;
      if (this.selectedFile) {
        if (this.selectedFile.name.match(/\.ya?ml$/i)) {
          this.fileformat = "yaml";
        } else if (this.selectedFile.name.match(/\.xml$/i)) {
          this.fileformat = "xml";
        } else if (this.selectedFile.name.match(/\.json$/i)) {
          this.fileformat = "json";
        }
      }
    },
    async submitForm() {
      if (!this.selectedFile) {
        this.error = "No file was uploaded.";
        return;
      }

      this.uploading = true;
      this.error = null;
      this.messages = [];
      this.errjobs = [];
      this.skipjobs = [];
      this.jobs = [];

      try {
        const formData = new FormData();
        formData.append("xmlBatch", this.selectedFile);
        formData.append("fileformat", this.fileformat);
        formData.append("dupeOption", this.dupeOption);
        formData.append("uuidOption", this.uuidOption);
        formData.append("validateJobref", this.validateJobref);

        const response = await api.post(
          `/project/${this.project}/jobs/import`,
          formData,
          {
            headers: {
              "Content-Type": "multipart/form-data",
            },
          },
        );

        // Process the response
        this.didupload = true;

        if (response.data.succeeded) {
          this.jobs = response.data.succeeded;
        }
        if (response.data.failed) {
          this.errjobs = response.data.failed;
        }
        if (response.data.skipped) {
          this.skipjobs = response.data.skipped;
        }
        //clear the file input
        const fileInput = document.getElementById(
          "xmlBatch",
        ) as HTMLInputElement;
        if (fileInput) {
          fileInput.value = "";
        }
      } catch (error: any) {
        console.error("Error uploading jobs:", error);
        if (error.response?.data?.message) {
          this.error = error.response.data.message;
        } else {
          this.error = "An error occurred while uploading the file";
        }
      } finally {
        this.uploading = false;
      }
    },
    cancel() {
      // Redirect to jobs list
      this.$router.push({ path: `/project/${this.project}/jobs` });
    },
  },
});
</script>

<template>
  <div class="container-fluid">
    <!-- Display messages if any -->
    <div v-if="messages && messages.length" class="row">
      <div class="col-sm-12">
        <div class="alert alert-info">
          <div v-for="(msg, index) in messages" :key="index">{{ msg }}</div>
        </div>
      </div>
    </div>

    <!-- Display error jobs if any -->
    <div v-if="errjobs && errjobs.length" class="row">
      <div class="col-sm-12">
        <div class="alert alert-danger">
          <span class="prompt errors"
            >{{ errjobs.length }}
            {{ $tc("jobUpload.jobs", errjobs.length) }}
            {{
              errjobs.length === 1
                ? $t("jobUpload.wasWere.singular")
                : $t("jobUpload.wasWere.plural")
            }}
            {{ $t("jobUpload.results.error.notProcessed") }}</span
          >
        </div>
        <div class="card">
          <div class="card-header text-danger">
            <i class="glyphicon glyphicon-warning-sign"></i>
            {{ $t("jobUpload.results.error.definitionErrors") }}
          </div>
          <div class="card-content">
            <div
              v-for="(job, index) in errjobs"
              :key="index"
              class="flex-container flex-justify-space-between flex-align-items-stretch"
            >
              <div>
                #{{ job.index }}:
                <span class="jobname">
                  <a v-if="job.id" :href="job.permalink">{{
                    job.name || "(Name missing)"
                  }}</a>
                  <template v-else>{{ job.name }}</template>
                </span>
                <span class="jobdesc" style="">{{
                  job.description && job.description.length > 100
                    ? job.description.substring(0, 100)
                    : job.description
                }}</span>
              </div>
              <div class="errors">
                <template v-if="job.error">{{ job.error }}</template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Display skipped jobs if any -->
    <div v-if="skipjobs && skipjobs.length" class="row">
      <div class="col-sm-12">
        <div class="card">
          <div class="card-header text-info">
            <i class="glyphicon glyphicon-info-sign"></i>
            {{ skipjobs.length }} {{ $tc("jobUpload.jobs", skipjobs.length)
            }}{{
              skipjobs.length === 1
                ? $t("jobUpload.wasWere.singular")
                : $t("jobUpload.wasWere.plural")
            }}
            {{ $t("jobUpload.results.skipped.skippedMessage") }}
          </div>
          <div class="card-content">
            <div
              v-for="(job, index) in skipjobs"
              :key="index"
              class="flex-container flex-justify-space-between flex-align-items-stretch"
            >
              <div>
                #{{ job.index }}:
                <span class="jobname">{{ job.name }}</span>
                <span class="jobdesc" style="">{{
                  job.description && job.description.length > 100
                    ? job.description.substring(0, 100)
                    : job.description
                }}</span>
                <span class="sepL">{{
                  $t("jobUpload.results.skipped.existing")
                }}</span>
                <span class="jobname">
                  <a v-if="job.id" :href="job.permalink">{{ job.name }}</a>
                </span>
                <span class="jobdesc">{{
                  job.description && job.description.length > 100
                    ? job.description.substring(0, 100)
                    : job.description
                }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Display successful jobs if any -->
    <div v-if="jobs && jobs.length" class="row">
      <div class="col-sm-12">
        <div class="card">
          <div class="card-header text-success">
            <i class="glyphicon glyphicon-check"></i>
            <span class="text-success"
              >{{ jobs.length }} {{ $tc("jobUpload.jobs", jobs.length)
              }}{{
                jobs.length === 1
                  ? $t("jobUpload.wasWere.singular")
                  : $t("jobUpload.wasWere.plural")
              }}
              {{ $t("jobUpload.results.success.successMessage") }}</span
            >
          </div>
          <div class="card-content">
            <ul class="list-unstyled">
              <RecycleScroller
                ref="scroller"
                v-slot="{ item }"
                key="uploaded"
                :items="browseJobs"
                :item-size="27"
                key-field="id"
                item-tag="li"
                page-mode
              >
                <browser-job-item
                  v-if="item.id"
                  :job="item"
                  :load-meta="true"
                />
              </RecycleScroller>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- Upload form -->
    <div class="row">
      <div class="col-xs-12">
        <div class="card">
          <form class="form" role="form" @submit.prevent="submitForm">
            <div class="card-header">
              <h3 class="card-title">
                {{ $t("jobUpload.title") }} <b>{{ project }}</b>
              </h3>
            </div>
            <div class="card-content">
              <div class="form-group">
                <label for="xmlBatch">{{
                  $t("jobUpload.fileSelectLabel")
                }}</label>
                <input
                  id="xmlBatch"
                  type="file"
                  name="xmlBatch"
                  class="form-control"
                  @change="onFileChange"
                />
              </div>
              <div class="form-group">
                <div class="radio">
                  <input
                    id="fileformat_xml"
                    v-model="fileformat"
                    type="radio"
                    name="fileformat"
                    value="xml"
                  />
                  <label for="fileformat_xml">{{
                    $t("jobUpload.formatRadioLabel.xml")
                  }}</label>
                </div>
                <div class="radio">
                  <input
                    id="fileformat_yaml"
                    v-model="fileformat"
                    type="radio"
                    name="fileformat"
                    value="yaml"
                  />
                  <label class="radio-inline" for="fileformat_yaml">{{
                    $t("jobUpload.formatRadioLabel.yaml")
                  }}</label>
                </div>
                <div class="radio">
                  <input
                    id="fileformat_json"
                    v-model="fileformat"
                    type="radio"
                    name="fileformat"
                    value="json"
                  />
                  <label class="radio-inline" for="fileformat_json">{{
                    $t("jobUpload.formatRadioLabel.json")
                  }}</label>
                </div>
              </div>
              <div class="form-group">
                <div class="control-label text-form-label">
                  {{ $t("jobUpload.dupeOption.label") }}
                </div>
                <div class="radio">
                  <input
                    id="dupeOption_update"
                    v-model="dupeOption"
                    type="radio"
                    name="dupeOption"
                    value="update"
                  />
                  <label for="dupeOption_update">
                    <i18n-t
                      keypath="jobUpload.dupeOption.update.label"
                      tag="span"
                    >
                      <em>
                        {{ $t("jobUpload.dupeOption.update.word") }}
                      </em>
                    </i18n-t>
                  </label>
                </div>
                <div class="radio">
                  <input
                    id="dupeOption_skip"
                    v-model="dupeOption"
                    type="radio"
                    name="dupeOption"
                    value="skip"
                  />
                  <label for="dupeOption_skip">
                    <i18n-t
                      keypath="jobUpload.dupeOption.skip.label"
                      tag="span"
                    >
                      <em>
                        {{ $t("jobUpload.dupeOption.skip.word") }}
                      </em>
                    </i18n-t>
                  </label>
                </div>
                <div class="radio">
                  <input
                    id="dupeOption_create"
                    v-model="dupeOption"
                    type="radio"
                    name="dupeOption"
                    value="create"
                  />
                  <label for="dupeOption_create">
                    <i18n-t
                      keypath="jobUpload.dupeOption.create.label"
                      tag="span"
                    >
                      <em>
                        {{ $t("jobUpload.dupeOption.create.word") }}
                      </em>
                    </i18n-t>
                  </label>
                </div>
              </div>
              <div class="form-group">
                <div class="control-label text-form-label">
                  {{ $t("archive.import.uuidOption.label") }}
                </div>
                <div class="radio">
                  <input
                    id="uuidOption_preserve"
                    v-model="uuidOption"
                    type="radio"
                    name="uuidOption"
                    value="preserve"
                  />
                  <label for="uuidOption_preserve">{{
                    $t("archive.import.uuidOption.preserve.label")
                  }}</label>
                  <div class="help-block">
                    {{ $t("archive.import.uuidOption.preserve.description") }}
                  </div>
                </div>
                <div class="radio">
                  <input
                    id="uuidOption_remove"
                    v-model="uuidOption"
                    type="radio"
                    name="uuidOption"
                    value="remove"
                  />
                  <label for="uuidOption_remove">{{
                    $t("archive.import.uuidOption.remove.label")
                  }}</label>
                  <div class="help-block">
                    {{ $t("archive.import.uuidOption.remove.description") }}
                  </div>
                </div>
              </div>
              <div class="form-group">
                <div class="control-label text-form-label">
                  {{ $t("archive.import.validateJobRef.label") }}
                </div>
                <div class="radio">
                  <input
                    id="validateJobref_false"
                    v-model="validateJobref"
                    type="radio"
                    name="validateJobref"
                    value="false"
                  />
                  <label for="validateJobref_false">{{
                    $t("archive.import.validateJobRef.false.title")
                  }}</label>
                  <div class="help-block">
                    {{ $t("archive.import.validateJobRef.false.help") }}
                  </div>
                </div>
                <div class="radio">
                  <input
                    id="validateJobref_true"
                    v-model="validateJobref"
                    type="radio"
                    name="validateJobref"
                    value="true"
                  />
                  <label for="validateJobref_true">{{
                    $t("archive.import.validateJobRef.true.title")
                  }}</label>
                  <div class="help-block">
                    {{ $t("archive.import.validateJobRef.true.help") }}
                  </div>
                </div>
              </div>
            </div>
            <div class="card-footer">
              <div v-if="!uploading" id="uploadFormButtons">
                <button
                  id="createFormCancelButton"
                  type="button"
                  class="btn btn-default"
                  @click="cancel"
                >
                  {{ $t("jobUpload.button.cancel") }}
                </button>
                <button
                  id="uploadFormUpload"
                  type="submit"
                  name="Upload"
                  class="btn btn-cta"
                >
                  {{ $t("jobUpload.button.upload") }}
                </button>
              </div>
              <div
                v-if="uploading"
                id="schedUploadSpinner"
                class="spinner block"
              >
                <i class="fas fa-spinner fa-spin"></i>
                {{ $t("jobUpload.uploadingFile") }}
              </div>
              <div v-if="error" class="alert alert-danger mt-2">
                {{ error }}
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.card {
  margin-bottom: 1rem;
}

.card-header {
  font-weight: bold;
  padding: 0.75rem 1rem;
}

.card-content {
  padding: 1rem;
}

.card-footer {
  padding: 0.75rem 1rem;
}

.flex-container {
  display: flex;
}

.flex-justify-space-between {
  justify-content: space-between;
}

.flex-align-items-stretch {
  align-items: stretch;
}

.radio {
  margin-bottom: 0.5rem;
}

.text-form-label {
  font-weight: bold;
  margin-bottom: 0.5rem;
}

.mt-2 {
  margin-top: 1rem;
}
</style>
