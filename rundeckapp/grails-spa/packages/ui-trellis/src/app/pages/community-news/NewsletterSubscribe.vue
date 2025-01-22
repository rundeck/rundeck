<template>
  <section>
    <button class="btn red-button" @click="isSubscribeModalActive = true">
      {{ $t("message_subscribe") }}
    </button>
    <modal
      ref="modal"
      v-model="isSubscribeModalActive"
      :header="false"
      :footer="false"
      append-to-body
    >
      <div class="modal-body">
        <div v-if="!showConfirmation">
          <h4>Receive updates in your Inbox.</h4>
          <form @submit.prevent="handleSubmit">
            <div class="form-group">
              <label class="label">Email*</label>
              <div class="control">
                <input
                  v-model="email"
                  type="email"
                  class="form-control"
                  placeholder="Type Your Email..."
                />
              </div>
            </div>
            <div class="field" style="margin-top: 2em">
              <div class="control">
                <button type="submit" class="btn btn-block btn-lg red-button">
                  Subscribe
                </button>
              </div>
            </div>
          </form>
        </div>
        <div v-else>
          <p>{{ confirmationMessage }}</p>
          <p class="is-size-6">
            This modal will close in {{ counter }} seconds.
          </p>
        </div>
      </div>
    </modal>
  </section>
</template>

<script>
import { defineComponent } from "vue";
import axios from "axios";
import { getRundeckContext } from "@/library";

export default defineComponent({
  name: "NewsletterSubscribeButton",
  data() {
    return {
      email: "",
      isSubscribeModalActive: false,
      confirmationMessage: "Thank you for your submission.",
      showConfirmation: false,
      counter: 5,
    };
  },
  mounted() {},
  methods: {
    handleSubmit() {
      const appRundeckGatewayUrl =
        getRundeckContext().appMeta.appRundeckGatewayUrl;

      axios
        .post(appRundeckGatewayUrl + "/user/v1/newsletter/subscribe", {
          email: this.email,
        })
        .then((response) => {
          if (response.status === 200) {
            if (response.data.inlineMessage) {
              this.confirmationMessage = response.data.inlineMessage;
            }
            this.showConfirmation = true;

            const countdown = setInterval(() => {
              this.counter--;
              if (this.counter === 0) {
                clearInterval(countdown);
                this.isSubscribeModalActive = false;
              }
            }, 1000);
          }
        })
        .catch((error) => {
          // eslint-disable-next-line
          console.log(
            "Error connecting to Rundeck Newsletter Subscribe API",
            error,
          );
        });
    },
  },
});
</script>

<style lang="scss" scoped>
.modal-body {
  margin: 0 auto;
  padding: 1em 1.5em 1.5em;
  text-align: center;
}
.form-group {
  text-align: left;
}

label {
  color: black;
  text-transform: lowercase;
  padding-right: 0;
  font-size: 0.5em;
}
.red-button {
  background-color: #f7403a;
  color: #fff;
  border: none;
  border-radius: 0.1875em;
  // padding: 0.82812em 1.375em;
  padding-left: 1.375em;
  padding-right: 1.375em;
  font-weight: 700;
  -webkit-transition: 0.25s all ease-in-out;
  -o-transition: 0.25s all ease-in-out;
  transition: 0.25s all ease-in-out;

  &:focus {
    color: #fff;
  }

  &:hover {
    background-color: #f97975;
    color: #fff;
  }
}
</style>
