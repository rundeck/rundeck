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
      webhookAuthLabel: 'HTTP Authorization String',
      webhookGenerateSecurityLabel: "Use Authorization Header",
      webhookGenerateSecretCheckboxHelp: "[Optional] A Webhook authorization string can be generated to increase security of this webhook. All posts will need to include the generated string in the Authorization header.",
      webhookSecretMessageHelp: "Copy this authorization string now. After you navigate away from this webhook you will no longer be able to see the string.",
      webhookRegenClicked:'A new authorization string will be generated and displayed when the webhook is saved.',
      webhookPluginLabel:'Choose Webhook Plugin'
    }
  }
}
export default {
  messages: translationStrings
}
