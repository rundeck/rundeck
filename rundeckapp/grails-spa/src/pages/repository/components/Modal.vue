<template>
  <div class="modal fade" v-show="modal" tabindex="-1" role="dialog">
    <div class="modal-dialog" role="document">
      <div class="modal-content">
        <div class="modal-header">
          <button type="button" class="close" data-dismiss="modal" aria-label="Close">
            <span aria-hidden="true">&times;</span>
          </button>
          <h4 class="modal-title" id="myModalLabel">Modal title</h4>
        </div>
        <div class="modal-body">...</div>
        <div class="modal-footer">
          <button type="button" class="btn btn-default" @click="closeModal">Close</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState, mapActions } from "vuex";

export default {
  name: "Modal",
  computed: {
    ...mapState("modal", ["modal"])
  },
  methods: {
    ...mapActions("modal", ["closeModal"])
  }
};
</script>
<style lang="scss" scoped>
</style>
