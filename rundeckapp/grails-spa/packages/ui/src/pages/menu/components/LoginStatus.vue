<!--
  - Copyright 2019 Rundeck, Inc. (http://rundeck.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
    <span v-if="showLoginStatus">
        <b v-if="status === 'LOGGED IN'" class="text-success fas fa-dot-circle"></b>
        <b v-if="status === 'LOGGED OUT'" class="logged-out fas fa-dot-circle"></b>
        <b v-if="status === 'NOT LOGGED'" class="text-muted fas fa-minus-circle"></b>
        <b v-if="status === 'ABANDONED'" class="text-muted fas fa-bed"></b>
        <span v-if="label">
            {{$t('message.loginStatus.'+status)}}
        </span>
    </span>
</template>
<script>

export default {
    name: 'LoginStatus',
    props: [
        'status',
        'label',
        'showLoginStatus'
    ]
}
</script>
<style scoped lang="scss">
    .logged-out {
        color: burlywood;
    }
</style>
