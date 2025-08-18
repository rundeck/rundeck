const messages = {
  Edit: "Modifier",
  Save: "Enregistrer",
  Delete: "Supprimer",
  Cancel: "Annuler",
  Revert: "Retour arri\u00e8re",
  jobAverageDurationPlaceholder:
    "laisser vide pour la dur\u00e9e moyenne du traitement",
  resourcesEditor: {
    "Dispatch to Nodes": "Envoyer vers les n\u0153uds",
    Nodes: "N\u0153uds",
  },
  uiv: {
    modal: {
      cancel: "Annuler",
      ok: "OK",
    },
  },
  cron: {
    section: {
      0: "Secondes",
      1: "Minutes",
      2: "Heures",
      3: "Jour du mois",
      4: "Mois",
      5: "Jour de la semaine",
      6: "Ann\u00e9e",
    },
  },
  message_communityNews: "Nouvelles de la communaut\u00e9",
  message_connectionError:
    "Il semble qu\u02bcune erreur se soit produite lors de la connexion \u00e0 Community News.",
  message_readMore: "En savoir plus",
  message_refresh: "Veuillez actualiser la page ou nous rendre visite \u00e0",
  message_subscribe: "S\u02bcabonner",
  message_delete: "Supprimer ce champ",
  message_duplicated: "Le champ existe d\u00e9j\u00e0",
  message_select: "Selectionner un champ",
  message_description: "Description",
  message_fieldLabel: "Label du champ",
  message_fieldKey: "Cl\u00e9 du champ",
  message_fieldFilter: "Ecrire pour filtrer un champ",
  message_empty: "Peut être vide",
  message_cancel: "Annuler",
  message_add: "Ajouter",
  message_addField: "Ajouter un champ personnalis\u00e9",
  message_pageUsersSummary: "Liste des utilisateurs Rundeck.",
  message_pageUsersLoginLabel: "Nom d\u02bcutilisateur",
  message_pageUsersCreatedLabel: "Cr\u00e9\u00e9",
  message_pageUsersUpdatedLabel: "Mis \u00e0 jour",
  message_pageUsersLastjobLabel: "Dernier ex\u00e9cution de traitement",
  message_domainUserFirstNameLabel: "Pr\u00e9nom",
  message_domainUserLastNameLabel: "Nom",
  message_domainUserEmailLabel: "Email",
  message_domainUserLabel: "Utilisateur",
  message_pageUsersTokensLabel: "N\u00BA Jetons",
  message_pageUsersTokensHelp:
    "Vous pouvez adminisitrer les jetons depuis la page de profil utilisateur.",
  message_pageUsersLoggedStatus: "Status",
  message_pageUserLoggedOnly: "Utilisateurs connect\u00e9s uniquement",
  message_pageUserNotSet: "Non d\u00e9fini",
  message_pageUserNone: "Aucun",
  message_pageFilterLogin: "Login",
  message_pageFilterHostName: "Nom d\u02bch\u00f4te",
  message_pageFilterSessionID: "ID de session",
  message_pageFilterBtnSearch: "Rechercher",
  message_pageUsersSessionIDLabel: "ID de session",
  message_pageUsersHostNameLabel: "Nom d\u02bch\u00f4te",
  message_pageUsersLastLoginInTimeLabel: "Derni\u00e8re connexion",
  message_pageUsersTotalFounds: "Nombre total d\u02bcutilisateurs trouv\u00e9s",
  message_paramIncludeExecTitle: "Afficher la derni\u00e8re ex\u00e9cution",
  message_loginStatus: {
    "LOGGED IN": "Connect\u00e9",
    "NOT LOGGED": "Jamais",
    ABANDONED: "Expir\u00e9",
    "LOGGED OUT": "D\u00e9connect\u00e9",
  },
  message_userSummary: {
    desc: "Ceci est une liste des profils d\u02bcutilisateurs qui se sont connect\u00e9s \u00e0 Rundeck.",
  },
  message_webhookPageTitle: "Webhooks",
  message_webhookListTitle: "Webhooks",
  message_webhookDetailTitle: "D\u00e9tail du webhook",
  message_webhookListNameHdr: "Nom",
  message_addWebhookBtn: "Ajouter",
  message_webhookEnabledLabel: "Activ\u00e9",
  message_webhookPluginCfgTitle: "Configuration du plugin",
  message_webhookSaveBtn: "Enregistrer",
  message_webhookCreateBtn: "Cr\u00e9er le webhook",
  message_webhookDeleteBtn: "Supprimer",
  message_webhookPostUrlLabel: "URL Post",
  message_webhookPostUrlHelp:
    "Lorsqu\u02bcune requête HTTP POST vers cette URL est reçue, le plugin webhook choisi ci-dessous recevra les donn\u00e9es.",
  message_webhookPostUrlPlaceholder:
    "L\u02bcURL sera g\u00e9n\u00e9r\u00e9e apr\u00e8s la cr\u00e9ation du Webhook",
  message_webhookNameLabel: "Nom",
  message_webhookUserLabel: "Utilisateur",
  message_webhookUserHelp:
    "Nom d\u02bcutilisateur d\u02bcautorisation utilis\u00e9 lors de l\u02bcex\u00e9cution de ce webhook. Toutes les politiques ACL correspondant \u00e0 ce nom d\u02bcutilisateur s\u02bcappliqueront.",
  message_webhookRolesLabel: "R\u00f4les",
  message_webhookRolesHelp:
    "Les r\u00f4les d\u02bcautorisation assum\u00e9s lors de l\u02bcex\u00e9cution de ce webhook (s\u00e9par\u00e9s par des virgules). Toutes les politiques ACL correspondant \u00e0 ces r\u00f4les s’appliqueront.",
  message_webhookAuthLabel: "Cha\u00eene d\u02bcautorisation HTTP",
  message_webhookGenerateSecurityLabel: "Utiliser l'en-tête d'autorisation",
  message_webhookGenerateSecretCheckboxHelp:
    "[Optionnel] Une jeton d'autorisation Webhook peut être g\u00e9n\u00e9r\u00e9e pour augmenter la s\u00e9curit\u00e9 de ce webhook. Tous les messages devront inclure le jeton g\u00e9n\u00e9r\u00e9e dans l'en-tête Autorisation.",
  message_webhookSecretMessageHelp:
    "Copiez ce jeton d'autorisation maintenant. Apr\u00e8s avoir quitt\u00e9 ce webhook, vous ne pourrez plus voir la cha\u00eene.",
  message_webhookRegenClicked:
    "Un nouveau jeton d\u02bcautorisation sera g\u00e9n\u00e9r\u00e9 et affich\u00e9 lorsque le webhook sera enregistr\u00e9.",
  message_webhookPluginLabel: "Choisir le plugin webhook",
  message_hello: "Bonjour",
  message_sidebarNotificationText:
    "Une mise \u00e0 jour pour Rundeck est disponible",
  message_updateAvailable: "Mise \u00e0 jour disponible",
  message_updateHasBeenReleased:
    "Une mise \u00e0 jour de Rundeck a \u00e9t\u00e9 publi\u00e9e.",
  message_installedVersion: "La version install\u00e9e de Rundeck est",
  message_currentVersion: "La version la plus r\u00e9cente de Rundeck est",
  message_getUpdate: "Obtenir la mise \u00e0 jour",
  message_dismissMessage:
    "Pour ignorer cette notification jusqu\u02bc\u00e0 la prochaine version, veuillez cliquer ici.",
  message_close: "Fermer",
  "bulk.edit": "Modification de masse",
  "in.of": "dans",
  execution: "Ex\u00e9cution | Ex\u00e9cutions",
  "execution.count": "1 Ex\u00e9cution | {0} Ex\u00e9cutions",
  "Bulk Delete Executions: R\u00e9sultats":
    "Suppression en masse des ex\u00e9cutions: R\u00e9sultats",
  "Requesting bulk delete, please wait.":
    "Demande de suppression en masse, veuillez patienter.",
  "bulkresult.attempted.text":
    "{0} ex\u00e9cutions ont \u00e9t\u00e9 tent\u00e9es.",
  "bulkresult.success.text":
    "{0} Les ex\u00e9cutions ont \u00e9t\u00e9 supprim\u00e9es avec succ\u00e8s.",
  "bulkresult.failed.text":
    "{0} Les \u00e9x\u00e9cutions n\u02bcont pas pu être supprim\u00e9es:",
  "delete.confirm.text": "Vraiment supprimer {0} {1}?",
  "clearselected.confirm.text":
    "Effacer tous les {0} \u00e9l\u00e9ments s\u00e9lectionn\u00e9s ou uniquement les \u00e9l\u00e9ments affich\u00e9s sur cette page ?",
  "bulk.selected.count": "{0} selectionn\u00e9s",
  "results.empty.text": "Pas de r\u00e9sultats pour cette requête",
  "Only shown executions": "Uniquement les ex\u00e9cutions affich\u00e9es",
  "Clear bulk selection": "D\u00e9cocher la s\u00e9lection en masse",
  "Click to edit Search Query": "Cliquez pour modifier la requête de recherche",
  "Auto refresh": "Actualisation automatique",
  "error.message.0": "Une erreur est survenue: {0}",
  "info.completed.0": "Termin\u00e9: {0}",
  "info.completed.0.1": "Termin\u00e9: {0} {1}",
  "info.missed.0.1": "Manqu\u00e9: {0} {1}",
  "info.started.0": "D\u00e9marr\u00e9: {0}",
  "info.started.expected.0.1": "D\u00e9marr\u00e9: {0}, Fin estim\u00e9e: {1}",
  "info.scheduled.0": "Planifi\u00e9; d\u00e9marrage {0}",
  "job.execution.starting.0": "D\u00e9marrage {0}",
  "job.execution.queued": "En file d\u02bcattente",
  "info.newexecutions.since.0":
    "1 nouveau r\u00e9sultat. Cliquez pour charger. | {0} Nouveau(x) r\u00e9sultat(s). Cliquez pour charger.",
  "In the last Day": "Au cours des derni\u00e8res 24 heures",
  Referenced: "R\u00e9f\u00e9renc\u00e9",
  "job.has.been.deleted.0": "(Le traitement {0} a \u00e9t\u00e9 supprim\u00e9)",
  Filters: "Filtres",
  "filter.delete.named.text": 'Supprimer le filtre "{0}"...',
  "Delete Saved Filter": "Supprimer le filtre enregistr\u00e9",
  "filter.delete.confirm.text":
    'Êtes-vous sûr de vouloir supprimer le filtre enregistr\u00e9 nomm\u00e9 "{0}" ?',
  "filter.save.name.prompt": "Nom:",
  "filter.save.validation.name.blank": "Le nom ne peut pas être vide",
  "filter.save.button": "Enregistrer le filtre...",
  "saved.filters": "Filtres enregistr\u00e9s",
  failed: "\u00e9chou\u00e9",
  ok: "ok",
  "0.total": "{0} total",

  period: {
    label: {
      All: "n\u02bcimporte quand",
      Hour: "au cours de la derni\u00e8re heure",
      Day: "au cours du dernier jour",
      Week: "au cours de la derni\u00e8re semaine",
      Month: "au cours du dernier mois",
    },
  },
  "empty.message.default":
    "Aucune configuration trouv\u00e9e. Cliquez sur {0} pour ajouter un nouveau plugin.",
  CreateAcl: "Cr\u00e9er l\u02bcACL",
  CreateAclName: "Description de l\u02bcACL",
  CreateAclTitle: "Cr\u00e9er une ACL de stockage de cl\u00e9s pour le projet",
  "Edit Nodes": "Modifier les n\u0153uds",
  Modify: "Modifier",
  "Edit Node Sources": "Modifier les sources de n\u0153ud",
  "The Node Source had an error":
    "La source de n\u0153ud a rencontr\u00e9 une erreur",
  "Validation errors": "Erreurs de validation",

  "unauthorized.status.help.1":
    "Certaines sources de n\u0153ud ont renvoy\u00e9 un message « Non autoris\u00e9 ».",
  "unauthorized.status.help.2":
    "Le plugin de source de n\u0153ud peut avoir besoin d\u02bcacc\u00e9der \u00e0 la ressource de stockage de cl\u00e9s. il pourrait être activ\u00e9 par les entr\u00e9es de la politique de contr\u00f4le d\u02bcacc\u00e8s.",
  "unauthorized.status.help.3":
    "Assurez-vous que les strat\u00e9gies ACL autorisent l\u02bcacc\u00e8s en « lecture » au stockage de cl\u00e9s dans ce projet pour le chemin URN du projet (urn:project:name). ",
  "unauthorized.status.help.4":
    "Aller \u00e0 {0} pour cr\u00e9er une ACL de projet ",
  "unauthorized.status.help.5":
    "Aller \u00e0 {0} pour cr\u00e9er une ACL syst\u00e8me ",

  "acl.config.link.title":
    "Param\u00e8tres du projet > Contr\u00f4le d\u02bcacc\u00e8s",
  "acl.config.system.link.title":
    "Param\u00e8tres syst\u00e8me > Contr\u00f4le d\u02bcacc\u00e8s",
  "acl.example.summary": "Exemple de strat\u00e9gie ACL",

  "page.keyStorage.description":
    "Le stockage de cl\u00e9s fournit une structure globale de type r\u00e9pertoire pour enregistrer les cl\u00e9s et mots de passe publics et priv\u00e9s, \u00e0 utiliser avec l\u02bcauthentification des n\u0153uds d\u02bcex\u00e9cution..",

  Duplicate: "Dupliquer",
  "bulk.delete": "Suppression en masse",
  "select.none": "Ne rien s\u00e9lectionner",
  "select.all": "Tout s\u00e9lectionner",
  "cancel.bulk.delete": "Annuler la suppression en masse",
  "delete.selected.executions":
    "Supprimer les ex\u00e9cutions s\u00e9lectionn\u00e9es",
  "click.to.refresh": "cliquez pour actualiser",
  "count.nodes.matched": "{0} {1} Correspondant",
  "count.nodes.shown": "{0} n\u0153uds affich\u00e9s.",
  "delete.this.filter.confirm": "Vraiment supprimer ce filtre ?",
  "enter.a.node.filter":
    "Entrez un filtre de n\u0153ud, ou .* Pour tous les n\u0153uds",
  "execute.locally": "Ex\u00e9cuter localement",
  "execution.page.show.tab.Nodes.title": "N\u0153uds",
  "execution.show.mode.Log.title": "Sortie de journal",
  filter: "Filtre :",
  "name.prompt": "Nom :",
  "loading.matched.nodes": "Chargement des n\u0153uds correspondants...",
  "loading.text": "Chargement...",
  "loglevel.debug": "D\u00e9boguer",
  "loglevel.normal": "Standard",
  "matched.nodes.prompt": "N\u0153uds correspondants",
  no: "Non",
  "node.access.not-runnable.message":
    "Vous n\u02bcavez pas acc\u00e8s \u00e0 l\u02bcex\u00e9cution de commandes sur ce n\u0153ud.",
  "Node.count.vue": "Node | Nodes",
  "node.filter": "Filtre de n\u0153ud",
  "node.filter.exclude": "Exclure le filtre",
  "node.metadata.os": "Syst\u00e8me d\u02bcexploitation",
  "node.metadata.status": "Status",
  nodes: "N\u0153uds :",
  "notification.event.onfailure": "En cas d\u02bc\u00e9chec",
  "notification.event.onsuccess": "En cas de succ\u00e8s",
  "notification.event.onstart": "Au d\u00e9marrage",
  "notification.event.onavgduration": "Dur\u00e9e moyenne d\u00e9pass\u00e9e",
  "notification.event.onretryablefailure":
    "En cas d\u02bc\u00e9chec r\u00e9essayable",
  refresh: "rafra\u00eechir",
  "save.filter.ellipsis": "Enregistrer le filtre \u2026",
  "search.ellipsis": "Rechercher\u2026",
  "ScheduledExecution.page.edit.title": "Modifier le traitement",
  "ScheduledExecution.page.create.title": "Cr\u00e9er un nouveau traitement",
  "scheduledExecution.property.defaultTab.label": "Onglet par d\u00e9faut",
  "scheduledExecution.property.defaultTab.description":
    "L\u02bconglet par d\u00e9faut \u00e0 afficher lorsque vous suivez une ex\u00e9cution.",
  "scheduledExecution.property.excludeFilterUncheck.label":
    "Montrer les n\u0153uds exclus",
  "scheduledExecution.property.excludeFilterUncheck.description":
    "Si vrai, les n\u0153uds exclus seront indiqu\u00e9s lors de l'ex\u00e9cution du Job. Sinon, ils ne seront pas affich\u00e9s du tout.",
  "scheduledExecution.property.logOutputThreshold.label":
    "Limite de sortie du journal",
  "scheduledExecution.property.logOutputThreshold.description":
    'Entrez soit le nombre de lignes total maximum (par exemple "100"), le nombre maximum de lignes par n\u0153ud ("100 / n\u0153ud") ou la taille maximale du fichier journal ("100MB", "100KB", etc. "," MB "," KB "," B "comme Giga- Mega-Kilo et octets.',
  "scheduledExecution.property.logOutputThreshold.placeholder":
    "E.g comme \u02bc100\u02bc, \u02bc100 / n\u0153ud\u02bcou \u02bc100MB\u02bc",
  "scheduledExecution.property.logOutputThresholdAction.label":
    "Action de limite de journal",
  "scheduledExecution.property.logOutputThresholdAction.description":
    "Action \u00e0 effectuer si la limite de sortie est atteinte.",
  "scheduledExecution.property.logOutputThresholdAction.halt.label":
    "Arr\u00eater avec le status:",
  "scheduledExecution.property.logOutputThresholdAction.truncate.label":
    "Tronquer et continuer",
  "scheduledExecution.property.logOutputThresholdStatus.placeholder":
    "\u02bcfailed\u02bc, \u02bcaborted\u02bc, ou n\u02bcimporte quelle cha\u00eene",
  "scheduledExecution.property.loglevel.help":
    "Le niveau de d\u00e9bogage produit plus de sortie",
  "scheduledExecution.property.maxMultipleExecutions.label":
    "Limiter ex\u00e9cutions multiples?",
  "scheduledExecution.property.maxMultipleExecutions.description":
    "Nombre maximal d\u02bcex\u00e9cutions multiples. Utilisez vide ou 0 pour indiquer illimit\u00e9.",
  "scheduledExecution.property.multipleExecutions.description":
    "Autoriser ce traitement \u00e0 \u00eatre ex\u00e9cut\u00e9 plus d\u02bcune fois simultan\u00e9ment ?",
  "scheduledExecution.property.nodeKeepgoing.prompt":
    "Si un n\u0153ud \u00e9choue",
  "scheduledExecution.property.nodeKeepgoing.true.description":
    "Continuez l\u02bcex\u00e9cution sur tous les n\u0153uds restants avant de faire \u00e9chouer l\u02bc\u00e9tape.",
  "scheduledExecution.property.nodeKeepgoing.false.description":
    "Faire \u00e9chouer l\u02bc\u00e9tape sans continuer d\u02bcex\u00e9cuter sur les n\u0153uds restants.",
  "scheduledExecution.property.nodeRankAttribute.label":
    "Attribut de classement",
  "scheduledExecution.property.nodeRankAttribute.description":
    "Attribut des n\u0153uds utilis\u00e9 pour le tri. La valeur par d\u00e9faut est le nom du n\u0153ud.",
  "scheduledExecution.property.nodeRankOrder.label": "Ordre de classement",
  "scheduledExecution.property.nodeRankOrder.ascending.label": "Croissant",
  "scheduledExecution.property.nodeRankOrder.descending.label":
    "D\u00e9croissant",
  "scheduledExecution.property.nodeThreadcount.label": "Nombre de threads",
  "scheduledExecution.property.nodeThreadcount.description":
    "Nombre maximal de threads parall\u00e8les \u00e0 utiliser. (Par d\u00e9faut : 1)",
  "scheduledExecution.property.nodefiltereditable.label": "Filtre modifiable",
  "scheduledExecution.property.nodesSelectedByDefault.label":
    "S\u00e9lection de n\u0153ud",
  "scheduledExecution.property.nodesSelectedByDefault.true.description":
    "Les n\u0153uds cibles sont s\u00e9lectionn\u00e9s par d\u00e9faut",
  "scheduledExecution.property.nodesSelectedByDefault.false.description":
    "L\u02bcutilisateur doit s\u00e9lectionner explicitement les n\u0153uds cibles",
  "scheduledExecution.property.notifyAvgDurationThreshold.label": "Seuil",
  "scheduledExecution.property.notifyAvgDurationThreshold.description":
    "Ajoutez ou d\u00e9finissez une valeur de seuil \u00e0 la dur\u00e9e moyenne pour d\u00e9clencher cette notification. Options : - pourcentage => ex .: 20% - temps delta => ex .: + 20s, +20 - temps absolu => 30s, 5m Temps en secondes si vous ne sp\u00e9cifiez pas d\u02bcunit\u00e9s de temps Peut inclure des r\u00e9f\u00e9rences de valeur d\u02bcoption comme {'$'}{'{'}option{'.'}avgDurationThreshold{'}'}.",
  "scheduledExecution.property.orchestrator.label": "Orchestrateur",
  "scheduledExecution.property.orchestrator.description":
    "Il peut \u00eatre utilis\u00e9 pour contr\u00f4ler l\u02bcordre et le timing dans lequel les n\u0153uds sont trait\u00e9s",
  "scheduledExecution.property.retry.description":
    "Nombre maximal de tentatives de r\u00e9-ex\u00e9cution lorsque ce traitement est directement appel\u00e9. Une nouvelle tentative se produira si le traitement \u00e9choue ou expire, mais pas s\u02bcil est tu\u00e9 manuellement. Peut utiliser une r\u00e9f\u00e9rence de valeur d\u02bcoption comme \"{'$'}{'{'}option{'.'}retry{'}'}\".",
  "scheduledExecution.property.retry.delay.description":
    "Le d\u00e9lai entre l\u02bcex\u00e9cution \u00e9chou\u00e9e et la nouvelle tentative. Temps en secondes, ou sp\u00e9cifier les unit\u00e9s de temps: \"120m\", \"2h\", \"3d\". Utilisez vide ou 0 pour indiquer aucun d\u00e9lai. Peut inclure des r\u00e9f\u00e9rences de valeur d\u02bcoption telles que \"{'$'}{'{'}option{'.'}delay{'}'}\".",
  "scheduledExecution.property.successOnEmptyNodeFilter.prompt":
    "Si le n\u0153ud est vide",
  "scheduledExecution.property.successOnEmptyNodeFilter.true.description":
    "Poursuivre l\u02bcex\u00e9cution",
  "scheduledExecution.property.successOnEmptyNodeFilter.false.description":
    "\u00c9chec du traitement",
  "scheduledExecution.property.timeout.description":
    "La dur\u00e9e maximale d\u02bcex\u00e9cution d\u02bcune ex\u00e9cution. Temps en secondes, ou sp\u00e9cifier les unit\u00e9s de temps : \"120m\", \"2h\", \"3d\". Utilisez vide ou 0 pour n\u02bcindiquer aucun d\u00e9lai. Peut inclure des r\u00e9f\u00e9rences de valeur d\u02bcoption telles que \"{'$'}{'{'}option{'.'}timeout{'}'}\".",
  "scheduledExecution.property.scheduleEnabled.description":
    "Autoriser ce traitement \u00e0 \u00eatre planifi\u00e9 ?",
  "scheduledExecution.property.scheduleEnabled.label":
    "Activer la planification ?",
  "scheduledExecution.property.executionEnabled.description":
    "Autoriser l\u02bcex\u00e9cution de ce traitement ?",
  "scheduledExecution.property.executionEnabled.label":
    "Activer l\u02bcex\u00e9cution?",
  "scheduledExecution.property.timezone.prompt": "Fuseau horaire",
  "scheduledExecution.property.timezone.description":
    'Un fuseau horaire valide, soit une abr\u00e9viation telle que "PST", un nom complet tel que "America / Los_Angeles", ou un identifiant personnalis\u00e9 tel que "GMT-8{\':\'} 00".',
  "documentation.reference.cron.url":
    "https{':'}//www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html",
  "set.as.default.filter": "D\u00e9finir comme filtre par d\u00e9faut",
  "show.all.nodes": "Afficher tous les n\u0153uds",
  yes: "Oui",
  // job query field labels
  "jobquery.title.titleFilter": "Commande Adhoc",
  "jobquery.title.contextFilter": "Le contexte",
  "jobquery.title.actionFilter": "action",
  "jobquery.title.maprefUriFilter": "URI de ressource",
  "jobquery.title.reportIdFilter": "pr\u00e9nom",
  "jobquery.title.tagsFilter": "Mots cl\u00e9s",
  "jobquery.title.nodeFilter": "N\u0153ud",
  "jobquery.title.nodeFilter.plural": "N\u0153uds",
  "jobquery.title.messageFilter": "Message",
  "jobquery.title.reportKindFilter": "Type de rapport",
  "jobquery.title.recentFilter": "Dans",
  "jobquery.title.actionTypeFilter": "action",
  "jobquery.title.itemTypeFilter": "Type d\u02bc\u00e9l\u00e9ment",
  "jobquery.title.filter": "Filtre",
  "jobquery.title.jobFilter": "Nom du traitement",
  "jobquery.title.idlist": "ID du traitement",
  "jobquery.title.jobIdFilter": "ID du traitement",
  "jobquery.title.descFilter": "Description du traitement",
  "jobquery.title.objFilter": "Ressource",
  "jobquery.title.scheduledFilter": "Pr\u00e9vu",
  "jobquery.title.serverNodeUUIDFilter": "N\u0153ud de serveur UUID",
  "jobquery.title.typeFilter": "Type",
  "jobquery.title.cmdFilter": "Commande",
  "jobquery.title.userFilter": "Utilisateur",
  "jobquery.title.projFilter": "Projet",
  "jobquery.title.statFilter": "R\u00e9sultat",
  "jobquery.title.startFilter": "Heure de d\u00e9but",
  "jobquery.title.startbeforeFilter": "Commencez avant",
  "jobquery.title.startafterFilter": "Commencer apr\u00e8s",
  "jobquery.title.endbeforeFilter": "Fin avant",
  "jobquery.title.endafterFilter": "Fin apr\u00e8s",
  "jobquery.title.endFilter": "Temps",
  "jobquery.title.durationFilter": "Dur\u00e9e",
  "jobquery.title.outFilter": "Sortie",
  "jobquery.title.objinfFilter": "Informations sur la ressource",
  "jobquery.title.cmdinfFilter": "Informations de commande",
  "jobquery.title.groupPath": "Groupe",
  "jobquery.title.summary": "R\u00e9sum\u00e9",
  "jobquery.title.duration": "Dur\u00e9e",
  "jobquery.title.loglevelFilter": "Loglevel",
  "jobquery.title.loglevelFilter.label.DEBUG": "D\u00e9boguer",
  "jobquery.title.loglevelFilter.label.VERBOSE": "Verbeux",
  "jobquery.title.loglevelFilter.label.INFO": "Information",
  "jobquery.title.loglevelFilter.label.WARN": "Attention",
  "jobquery.title.loglevelFilter.label.ERR": "Erreur",
  "jobquery.title.adhocExecutionFilter": "Type de traitement",
  "jobquery.title.adhocExecutionFilter.label.true": "Commande",
  "jobquery.title.adhocExecutionFilter.label.false": "Commande d\u00e9finie",
  "jobquery.title.adhocLocalStringFilter": "Contenu du script",
  "jobquery.title.adhocRemoteStringFilter": "Commande Shell",
  "jobquery.title.adhocFilepathFilter": "Chemin du fichier script",
  "jobquery.title.argStringFilter": "Arguments du fichier script",
  "page.unsaved.changes": "Vous avez des changements non enregistr\u00e9s",
  "edit.nodes.file": "Editer le fichier de n\u0153uds",
  "project.node.file.source.label": "Source",
  "file.display.format.label": "Format",
  "project.node.file.source.description.label": "Description",
  "project.nodes.edit.save.error.message":
    "Erreur lors de l'enregistrement du contenu:",
  "project.nodes.edit.empty.description":
    "Remarque : Aucun contenu n'\u00e9tait disponible.",
  "button.action.Cancel": "Annuler",
  "button.action.Save": "Enregistrer",
};

export default messages;
