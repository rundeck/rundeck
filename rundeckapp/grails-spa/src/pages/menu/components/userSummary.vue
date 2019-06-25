<template>
  <div class="container-fluid">
    <div class="card">
      <div class="card-content">
        <div class="row">
          <div class="col-xs-12">
            <div class="form-group pull-left" style="padding-top:10px">
              <span class="prompt">{{ $t("message.pageUsersSummary")}}</span>
            </div>
            <div class="form-group pull-right" style="display:inline-block;">
              <div class="checkbox">
                <input type="checkbox" name="loggedOnly" id="loggedOnly" v-model="loggedOnly" @change="loadUsersList()">
                <label for="loggedOnly">
                  {{ $t("message.pageUserLoggedOnly")}}
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col-sm-12">
        <div class="card">
          <div class="card-content">
            <div class="pageBody" id="userProfilePage">
              <div class="row">
                <div class="col-sm-12">
                  <table class="table table-condensed  table-striped">
                    <tbody>
                      <tr>
                        <th class="table-header">
                          {{ $t("message.pageUsersLoginLabel")}}
                        </th>
                        <th class="table-header">
                          {{ $t("message.domainUserEmailLabel")}}
                        </th>
                        <th class="table-header">
                          {{ $t("message.domainUserFirstNameLabel")}}
                        </th>
                        <th class="table-header">
                          {{ $t("message.domainUserLastNameLabel")}}
                        </th>
                        <th class="table-header">
                          {{ $t("message.pageUsersCreatedLabel")}}
                        </th>
                        <th class="table-header">
                          {{ $t("message.pageUsersUpdatedLabel")}}
                        </th>
                        <th class="table-header">
                          {{ $t("message.pageUsersLastjobLabel")}}
                        </th>
                        <th class="table-header">
                          {{ $t("message.pageUsersTokensLabel")}}
                          <span class="has_tooltip text-primary" data-placement="bottom"
                                :data-original-title="$t('message.pageUsersTokensHelp')">
                            <i class="glyphicon glyphicon-question-sign"></i>
                          </span>
                        </th>
                        <th class="table-header">
                          {{ $t("message.pageUsersLoggedStatus")}}
                        </th>
                      </tr>

                      <tr v-for="user in users">
                        <td>{{user.login}}
                        </td>
                        <td v-if="user.email">{{user.email}}
                        </td>
                        <td v-else><span class="text-primary small text-uppercase">{{ $t("message.pageUserNotSet")}}</span>
                        </td>
                        <td v-if="user.firstName">{{user.firstName}}
                        </td>
                        <td v-else><span class="text-primary small text-uppercase">{{ $t("message.pageUserNotSet")}}</span>
                        </td>
                        <td v-if="user.lastName">{{user.lastName}}
                        </td>
                        <td v-else><span class="text-primary small text-uppercase">{{ $t("message.pageUserNotSet")}}</span>
                        </td>
                        <td v-if="user.created">{{user.created | moment("MM/DD/YYYY hh:mm a")}}
                        </td>
                        <td v-else><span class="text-primary small text-uppercase">{{ $t("message.pageUserNotSet")}}</span>
                        </td>
                        <td v-if="user.updated">{{user.updated | moment("MM/DD/YYYY hh:mm a")}}
                        </td>
                        <td v-else><span class="text-primary small text-uppercase">{{ $t("message.pageUserNotSet")}}</span>
                        </td>
                        <td v-if="user.lastJob">{{user.lastJob | moment("MM/DD/YYYY hh:mm a")}}
                        </td>
                        <td v-else><span class="text-primary small text-uppercase">{{ $t("message.pageUserNone")}}</span>
                        </td>
                        <td v-if="user.tokens > 0">{{user.tokens}}
                        </td>
                        <td v-else><span class="text-primary small text-uppercase">{{ $t("message.pageUserNone")}}</span>
                        </td>
                        <td>{{user.loggedStatus}}
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>

  import axios from 'axios'

  export default {
    name: 'UserSummary',
    props: [
      'menu',
      'userSummary'
    ],
    data () {
      return {
        users: [],
        loggedOnly: false
      }
    },
    methods: {
      loadUsersList: function () {
        axios({
          method: 'get',
          headers: {'x-rundeck-ajax': true},
          url: `/menu/loadUsersList`,
          params: {
            loggedOnly: `${this.loggedOnly}`
          },
          withCredentials: true
        }).then((response) => {
          this.users = response.data.users
        })
      }
    },
    beforeMount() {
      this.loadUsersList()
    }
  }
</script>

<style scoped lang="scss">
  ul.users {
    display: inline;
    margin: 0;
    padding: 0;
  }

  ul.users li {
    display: inline;
    list-style: none;
    margin: 0;
    padding: 0;
  }

  ul.users li:after {
    content: ", ";
    color: #aaa;
  }

  ul.users li:last-child:after {
    content: "";
  }
</style>
