<template>
  <div class="content">
    <div id="layoutBody">
      <div class="title">
        <span class="text-h3"><i class="fas fa-key"></i> {{ $t('Key Storage') }}</span>
      </div>
      <div id="page_storage" class="container-fluid">
        <div class="row">
          <div class="col-sm-12">
          </div>
        </div>
        <div class="row">
          <div class="col-xs-12">
            <div class="card">
              <div class="card-content">
                <key-storage-selector></key-storage-selector>
              </div>
              <div class="card-footer">
                <hr>
                <span class=" text-info">
                  {{ $t('Key Storage provides a global directory-like structure to save Public and Private Keys and Passwords, for use with Node Execution authentication.') }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

import KeyStorageBrowser from "@/app/components/storage/KeyStorageBrowser";
import Vue from "vue";
export default Vue.extend({
  name: "KeyStorage.vue",
  components: {KeyStorageBrowser},
})
</script>

<style scoped>

</style>