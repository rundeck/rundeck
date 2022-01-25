const translationStrings = {
  en_US: {
    message: {
      webhookPageTitle:'Webhooks',
      webhookListTitle:'Webhooks',
      webhookDetailTitle:'Webhook Detail',
      webhookListNameHdr:'Name',
      addWebhookBtn:'Add',
      webhookEnabledLabel:'Enabled',
      webhookPluginCfgTitle:'Plugin Configuration',
      webhookSaveBtn:'Save',
      webhookCreateBtn:'Create Webhook',
      cancel:'Cancel',
      webhookDeleteBtn:'Delete',
      webhookPostUrlLabel:'Post URL',
      webhookPostUrlHelp:'When a HTTP POST request to this URL is received, the Webhook Plugin chosen below will receive the data.',
      webhookPostUrlPlaceholder:'URL will be generated after the Webhook is created',
      webhookNameLabel:'Name',
      webhookUserLabel:'User',
      webhookUserHelp:'The authorization username assumed when running this webhook. All ACL policies matching this username will apply.',
      webhookRolesLabel:'Roles',
      webhookRolesHelp:'The authorization roles assumed when running this webhook (comma separated). All ACL policies matching these roles will apply.',
      webhookSecretLabel:'Authorization String',
      webhookSecretHelp:'[Optional] A Webhook Authentication string can be generated to increase security of this webhook. All posts will need to include the generated string in the Authorization header. NOTE: The generated string is only shown once! Make sure you save the string or you will need to generate it again.',
      webhookPluginLabel:'Choose Webhook Plugin'
    }
  }
}
module.exports = {
  messages: translationStrings
}
