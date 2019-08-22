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
        <div class="base-filters">
          <div class="row">
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <input
                  type="text"
                  class="form-control"
                  :placeholder="$t( 'message.pageFilterLogin')"
                  v-model="loginFilter"
                >
              </div>
            </div>
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <input
                  type="text"
                  class="form-control"
                  :placeholder="$t( 'message.pageFilterSessionID')"
                  v-model="sessionIdFilter"
                >
              </div>
            </div>
            <div class="col-xs-12 col-sm-4">
              <div class="form-group">
                <input
                  type="text"
                  class="form-control"
                  :placeholder="$t( 'message.pageFilterHostName')"
                  v-model="hostNameFilter"
                >
              </div>
            </div>
            <div class="col-xs-12">
              <div class="form-group pull-right">
                <btn @click="filterByCriteria()" type="primary">{{ $t("message.pageFilterBtnSearch")}}</btn>
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
                          {{ $t("message.pageUsersSessionIDLabel")}}
                          SESSION ID
                        </th>
                        <th class="table-header">
                          {{ $t("message.pageUsersHostNameLabel")}}
                          HOSTNAME
                        </th>
                        <th class="table-header">
                          {{ $t("message.pageUsersEventTimeLabel")}}
                          EVENT TIME
                        </th>
                        <th class="table-header">
                          {{ $t("message.pageUsersLoggedStatus")}}
                        </th>
                      </tr>

                      <tr v-for="user in filteredUsers">
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
                        <td v-if="user.lastSessionId">{{user.lastSessionId}}
                        </td>
                        <td v-else><span class="text-primary small text-uppercase">{{ $t("message.pageUserNotSet")}}</span>
                        <td v-if="user.lastHostName">{{user.lastHostName}}
                        </td>
                        <td v-else><span class="text-primary small text-uppercase">{{ $t("message.pageUserNotSet")}}</span>
                        <td v-if="user.loggedInTime">{{user.loggedInTime | moment("MM/DD/YYYY hh:mm a")}}
                        </td>
                        <td v-else><span class="text-primary small text-uppercase">{{ $t("message.pageUserNotSet")}}</span>
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
        loggedOnly: false,
        filteredUsers: [],
        sessionIdFilter: "",
        hostNameFilter: "",
        loginFilter: ""
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
          this.filterByCriteria()
        })
      },
      filterByCriteria: function (){
        var sessionFilterMatch = true
        var hostNameFilterMatch = true
        var loginFilterMatch = true
        if(this.sessionIdFilter.trim().length == 0){
          sessionFilterMatch = false
        }
        if(this.hostNameFilter.trim().length == 0){
          hostNameFilterMatch = false
        }
        if(this.loginFilter.trim().length == 0){
          loginFilterMatch = false
        }
        global = this
        const filteredUsers = this.users.filter(function(user){
          var singleUserSessionMatch = true
          var singleUserHostnameMatch = true
          var singleUserLoginMatch = true
          if(sessionFilterMatch === true){
            singleUserSessionMatch = user.lastSessionId === global.sessionIdFilter.trim()? true : false
          }
          if(hostNameFilterMatch === true){
            singleUserHostnameMatch = user.lastHostName === global.hostNameFilter.trim()? true : false
          }
          if(loginFilterMatch === true){
            singleUserLoginMatch = user.login === global.loginFilter.trim()? true : false
          }
          return (singleUserSessionMatch && singleUserHostnameMatch && singleUserLoginMatch)
        })
        this.filteredUsers = filteredUsers
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
