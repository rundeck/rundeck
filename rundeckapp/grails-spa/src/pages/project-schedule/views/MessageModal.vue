<template>
  <div>
    <modal id="messagesModal" size="lg" :footer=false @hide="closeMessagesModal()">
    </modal>
  </div>
</template>

<script>

    export default {
        name: "MessageModal",
        props:["success","messages"],
        methods:{
            closeMessagesModal(){
                this.eventBus.$emit('closeMessagesModal', {});
            }
        }
    }
</script>

<style scoped>

</style>
