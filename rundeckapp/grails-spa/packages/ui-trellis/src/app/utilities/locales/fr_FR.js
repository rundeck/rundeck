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
    datePicker: {
      clear: "Nettoyer",
      month: "Mois",
      year: "An",
      today: "Aujourd\u02bchui",
      month1: "Janvier",
      month2: "F\u00e9vrier",
      month3: "Mars",
      month4: "Avril",
      month5: "Mai",
      month6: "Juin",
      month7: "Juillet",
      month8: "Ao\u00fbt",
      month9: "Septembre",
      month10: "Octobre",
      month11: "Novembre",
      month12: "D\u00e9cembre",
      week1: "Lun",
      week2: "Mar",
      week3: "Mer",
      week4: "Jeu",
      week5: "Ven",
      week6: "Sam",
      week7: "Dim",
    },
    timePicker: {
      am: "AM",
      pm: "PM",
    },
    multiSelect: {
      placeholder: "S\u00e9lectionner...",
      filterPlaceholder: "Rechercher...",
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
  message_delete: "Supprimer ce champ",
  message_duplicated: "Le champ existe d\u00e9j\u00e0",
  message_select: "Selectionner un champ",
  message_description: "Description",
  message_fieldLabel: "Label du champ",
  message_fieldKey: "Cl\u00e9 du champ",
  message_fieldFilter: "Ecrire pour filtrer un champ",
  message_cancel: "Annuler",
  message_add: "Ajouter",
  message_addField: "Ajouter un champ personnalis\u00e9",
  message_pageUsersSummary: "Liste des utilisateurs Rundeck.",
  message_pageUsersLoginLabel: "Nom d\u02bcutilisateur",
  message_pageUsersCreatedLabel: "Cr\u00e9\u00e9",
  message_pageUsersUpdatedLabel: "Mis \u00e0 jour",
  message_pageUsersLastjobLabel: "Derni\u00e8re ex\u00e9cution de traitement",
  message_domainUserFirstNameLabel: "Pr\u00e9nom",
  message_domainUserLastNameLabel: "Nom",
  message_domainUserEmailLabel: "Email",
  message_domainUserLabel: "Utilisateur",
  message_pageUsersTokensLabel: "N\u00ba Jetons",
  message_pageUsersTokensHelp:
    "Vous pouvez adminisitrer les jetons depuis la page de profil utilisateur.",
  message_pageUsersLoggedStatus: "Statut",
  message_pageUserLoggedOnly: "Utilisateurs connect\u00e9s uniquement",
  message_pageUserNotSet: "Non d\u00e9fini",
  message_pageUserNone: "Aucun",
  message_pageFilterLogin: "Identifiant",
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
    desc: "Liste des profils utilisateurs qui se sont connect\u00e9s \u00e0 Rundeck.",
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
    "Lorsqu\u02bcune requ\u00eate HTTP POST vers cette URL est re\u00e7ue, le plugin webhook choisi ci-dessous recevra les donn\u00e9es.",
  message_webhookPostUrlPlaceholder:
    "L\u02bcURL sera g\u00e9n\u00e9r\u00e9e apr\u00e8s la cr\u00e9ation du Webhook",
  message_webhookNameLabel: "Nom",
  message_webhookUserLabel: "Utilisateur",
  message_webhookUserHelp:
    "Nom d\u02bcutilisateur d\u02bcautorisation utilis\u00e9 lors de l\u02bcex\u00e9cution de ce webhook. Toutes les politiques ACL correspondant \u00e0 ce nom d\u02bcutilisateur s\u02bcappliqueront.",
  message_webhookRolesLabel: "R\u00f4les",
  message_webhookRolesHelp:
    "Les r\u00f4les d\u02bcautorisation assum\u00e9s lors de l\u02bcex\u00e9cution de ce webhook (s\u00e9par\u00e9s par des virgules). Toutes les politiques ACL correspondant \u00e0 ces r\u00f4les s\u02bcappliqueront.",
  message_webhookAuthLabel: "Cha\u00eene d\u02bcautorisation HTTP",
  message_webhookGenerateSecurityLabel:
    "Utiliser l\u02bcen-t\u00eate d\u02bcautorisation",
  message_webhookGenerateSecretCheckboxHelp:
    "[Optionnel] Un jeton d\u02bcautorisation Webhook peut \u00eatre g\u00e9n\u00e9r\u00e9e pour augmenter la s\u00e9curit\u00e9 de ce webhook. Tous les messages devront inclure le jeton g\u00e9n\u00e9r\u00e9e dans l\u02bcen-t\u00eate Autorisation.",
  message_webhookSecretMessageHelp:
    "Copiez ce jeton d\u02bcautorisation maintenant. Apr\u00e8s avoir quitt\u00e9 ce webhook, vous ne pourrez plus voir la cha\u00eene.",
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
    "{0} Les \u00e9x\u00e9cutions n\u02bcont pas pu \u00eatre supprim\u00e9es:",
  "delete.confirm.text": "Vraiment supprimer {0} {1}?",
  "clearselected.confirm.text":
    "Effacer tous les {0} \u00e9l\u00e9ments s\u00e9lectionn\u00e9s ou uniquement les \u00e9l\u00e9ments affich\u00e9s sur cette page ?",
  "bulk.selected.count": "{0} selectionn\u00e9s",
  "results.empty.text": "Pas de r\u00e9sultats pour cette requ\u00eate",
  "Only shown executions": "Uniquement les ex\u00e9cutions affich\u00e9es",
  "Clear bulk selection": "D\u00e9cocher la s\u00e9lection en masse",
  "Click to edit Search Query":
    "Cliquez pour modifier la requ\u00eate de recherche",
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
    '\u00eates-vous s\u00fbr de vouloir supprimer le filtre enregistr\u00e9 nomm\u00e9 "{0}" ?',
  "filter.save.name.prompt": "Nom:",
  "filter.save.validation.name.blank": "Le nom ne peut pas \u00eatre vide",
  "filter.save.button": "Enregistrer le filtre...",
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
    "Certaines sources de n\u0153ud ont renvoy\u00e9 un message 'Non autoris\u00e9'.",
  "unauthorized.status.help.2":
    "Le plugin de source de n\u0153ud peut avoir besoin d\u02bcacc\u00e9der \u00e0 la ressource de stockage de cl\u00e9s. il pourrait \u00eatre activ\u00e9 par les entr\u00e9es de la politique de contr\u00f4le d\u02bcacc\u00e8s.",
  "unauthorized.status.help.3":
    "Assurez-vous que les strat\u00e9gies ACL autorisent l\u02bcacc\u00e8s en lecture au stockage de cl\u00e9s dans ce projet pour le chemin URN du projet (urn:project:name). ",
  "unauthorized.status.help.4":
    "Aller \u00e0 {0} pour cr\u00e9er une ACL de projet ",
  "unauthorized.status.help.5":
    "Aller \u00e0 {0} pour cr\u00e9er une ACL syst\u00e8me ",
  "use.the.node.sources.tab.2":
    " Onglet pour mettre \u00e0 jour vos sources de n\u0153uds.",
  "project.node.sources.title.short": "Sources",
  "use.the.node.sources.tab.1": "Utiliser le ",
  "no.modifiable.sources.found": "Aucune source modifiable trouv\u00e9e",
  "framework.service.NodeEnhancer.explanation":
    "Les Node Enhancers peuvent modifier les donn\u00e9es charg\u00e9es \u00e0 partir des sources de n\u0153uds",
  "framework.service.NodeEnhancer.label.short.plural": "Enhancers",
  "modifiable.node.sources.will.appear.here":
    "Les sources de n\u0153uds modifiables appara\u00cetront ici",
  "edit.node.enhancers": "Modifier les Node Enhancers",
  "add.node.enhancer": "Ajouter un nouveau Node Enhancer",
  "add.node.source": "Ajouter une nouvelle source de n\u0153ud",
  "nodes.title": "N\u0153uds",
  "project.edit.ResourceModelSource.explanation":
    "Sources de n\u0153uds pour le projet. Les sources sont charg\u00e9es dans l'ordre d\u00e9fini, les sources ult\u00e9rieures rempla\u00e7ant les pr\u00e9c\u00e9dentes. (Vous pouvez utiliser {'${project.name}'} dans les valeurs de configuration pour substituer le nom du projet.)",
  "acl.config.link.title":
    "Param\u00e8tres du projet > Contr\u00f4le d\u02bcacc\u00e8s",
  "acl.config.system.link.title":
    "Param\u00e8tres syst\u00e8me > Contr\u00f4le d\u02bcacc\u00e8s",
  "acl.example.summary": "Exemple de strat\u00e9gie ACL",
  "empty.message.default":
    "Aucune configuration trouv\u00e9e. Cliquez sur {0} pour ajouter un nouveau plugin.",
  "page.keyStorage.description":
    "Le stockage de cl\u00e9s fournit une structure globale de type r\u00e9pertoire pour enregistrer les cl\u00e9s et mots de passe publics et priv\u00e9s, \u00e0 utiliser avec l\u02bcauthentification des n\u0153uds d\u02bcex\u00e9cution..",
  "bulk.delete": "Suppression en masse",
  "select.none": "Ne rien s\u00e9lectionner",
  "select.all": "Tout s\u00e9lectionner",
  "cancel.bulk.delete": "Annuler la suppression en masse",
  "delete.selected.executions":
    "Supprimer les ex\u00e9cutions s\u00e9lectionn\u00e9es",
  "click.to.refresh": "cliquez pour actualiser",
  "count.nodes.matched": "{0} {1} Correspondant",
  "count.nodes.shown": "{0} n\u0153uds affich\u00e9s.",
  "delete.this.filter": "Supprimer ce filtre",
  "delete.this.filter.confirm": "Vraiment supprimer ce filtre ?",
  "saved.filters": "Filtres enregistr\u00e9s",
  "save.node.filter": "Enregistrer le filtre de n\u0153ud",
  "name.prompt": "Nom",
  "delete.saved.node.filter":
    "Supprimer le filtre de n\u0153ud enregistr\u00e9",
  duplicate: "Dupliquer",
  "enter.a.node.filter":
    "Entrez un filtre de n\u0153ud, ou .* Pour tous les n\u0153uds",
  "execute.locally": "Ex\u00e9cuter localement",
  "execution.page.show.tab.Nodes.title": "N\u0153uds",
  "execution.show.mode.Log.title": "Sortie de journal",
  filter: "Filtre :",
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
  "node.metadata.status": "Statut",
  nodes: "N\u0153uds :",
  "notification.event.onfailure": "En cas d\u02bc\u00e9chec",
  "edit.nodes.header": "Modifier les n\u0153uds",
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
    "Si vrai, les n\u0153uds exclus seront indiqu\u00e9s lors de l\u02bcex\u00e9cution du Job. Sinon, ils ne seront pas affich\u00e9s du tout.",
  "scheduledExecution.property.logOutputThreshold.label":
    "Limite de sortie du journal",
  "scheduledExecution.property.logOutputThreshold.description":
    'Entrez soit le nombre de lignes total maximum (par exemple "100"), le nombre maximum de lignes par n\u0153ud ("100 / n\u0153ud") ou la taille maximale du fichier journal ("100MB", "100KB", etc. "," MB "," KB "," B "comme Giga- Mega-Kilo et octets.',
  "scheduledExecution.property.logOutputThreshold.placeholder":
    "Par exemple comme \u02bc100\u02bc, \u02bc100 / n\u0153ud\u02bcou \u02bc100MB\u02bc",
  "scheduledExecution.property.logOutputThresholdAction.description":
    "Action \u00e0 effectuer si la limite de sortie est atteinte.",
  "scheduledExecution.property.logOutputThresholdAction.halt.label":
    "Arr\u00eater avec le status:",
  "scheduledExecution.property.logOutputThresholdAction.label":
    "Action de limite de journal",
  "scheduledExecution.property.logOutputThresholdAction.truncate.label":
    "Tronquer et continuer",
  "scheduledExecution.property.logOutputThresholdStatus.placeholder":
    "'failed', 'aborted' ou n\u02bcimporte quelle cha\u00eene",
  "scheduledExecution.property.loglevel.help":
    "Le niveau de d\u00e9bogage produit plus de sortie",
  "scheduledExecution.property.maxMultipleExecutions.description":
    "Nombre maximal d\u02bcex\u00e9cutions multiples. Utilisez vide ou 0 pour indiquer illimit\u00e9.",
  "scheduledExecution.property.maxMultipleExecutions.label":
    "Limiter ex\u00e9cutions multiples?",
  "scheduledExecution.property.multipleExecutions.description":
    "Autoriser ce traitement \u00e0 \u00eatre ex\u00e9cut\u00e9 plus d\u02bcune fois simultan\u00e9ment ?",
  "scheduledExecution.property.nodeKeepgoing.false.description":
    "Faire \u00e9chouer l\u02bc\u00e9tape sans continuer d\u02bcex\u00e9cuter sur les n\u0153uds restants.",
  "scheduledExecution.property.nodeKeepgoing.prompt":
    "Si un n\u0153ud \u00e9choue",
  "scheduledExecution.property.nodeKeepgoing.true.description":
    "Continuez l\u02bcex\u00e9cution sur tous les n\u0153uds restants avant de faire \u00e9chouer l\u02bc\u00e9tape.",
  "scheduledExecution.property.nodeRankAttribute.description":
    "Attribut des n\u0153uds utilis\u00e9 pour le tri. La valeur par d\u00e9faut est le nom du n\u0153ud.",
  "scheduledExecution.property.nodeRankAttribute.label":
    "Attribut de classement",
  "scheduledExecution.property.nodeRankOrder.label": "Ordre de classement",
  "scheduledExecution.property.nodeRankOrder.ascending.label": "Croissant",
  "scheduledExecution.property.nodeRankOrder.descending.label":
    "D\u00e9croissant",
  "scheduledExecution.property.nodeThreadcount.label": "Nombre de threads",
  "scheduledExecution.property.nodeThreadcount.description":
    "Nombre maximal de threads parall\u00e8les \u00e0 utiliser. (Par d\u00e9faut : 1)",
  "scheduledExecution.property.nodefiltereditable.label": "Filtre modifiable",
  "scheduledExecution.property.nodesSelectedByDefault.false.description":
    "L\u02bcutilisateur doit s\u00e9lectionner explicitement les n\u0153uds cibles",
  "scheduledExecution.property.nodesSelectedByDefault.label":
    "S\u00e9lection de n\u0153ud",
  "scheduledExecution.property.nodesSelectedByDefault.true.description":
    "Les n\u0153uds cibles sont s\u00e9lectionn\u00e9s par d\u00e9faut",
  "scheduledExecution.property.notifyAvgDurationThreshold.description":
    "Ajoutez ou d\u00e9finissez une valeur de seuil \u00e0 la dur\u00e9e moyenne pour d\u00e9clencher les notifications.\n\n- pourcentage => ex .: 20%\n- temps delta => ex .: + 20s, +20\n- temps absolu => 30s, 5m\nUtilisez `s`,`m`,`h`,`d`,`w`,`y` etc. comme unit\u00e9s de temps pour les secondes, les minutes, les heures, etc...L\u02bcunit\u00e9 de temps par d\u00e9faut sera la seconde si elle n\u02bcest pas sp\u00e9cifi\u00e9e\nPeut inclure des r\u00e9f\u00e9rences de valeur d\u02bcoption telles que `{'$'}{'{'}option{'.'}avgDurationThreshold{'}'}`.",
  "scheduledExecution.property.notifyAvgDurationThreshold.label": "Seuil",
  "scheduledExecution.property.orchestrator.label": "Orchestrateur",
  "scheduledExecution.property.orchestrator.description":
    "Cela peut \u00eatre utilis\u00e9 pour contr\u00f4ler l\u02bcordre et le moment dans lesquels les n\u0153uds sont trait\u00e9s",
  "scheduledExecution.property.retry.delay.description":
    "Le temps entre l\u02bcchec de l\u02bcex\u00e9cution et la nouvelle tentative. Temps en secondes, ou pr\u00e9cisez les unit\u00e9s de temps : \"120m\", \"2h\", \"3d\". Utilisez vide ou 0 pour indiquer aucun d\u00e9lai. Peut inclure la valeur de l\u02bcoption r\u00e9f\u00e9rences comme \"{'$'}{'{'}option{'.'}delay{'}'}\".",
  "scheduledExecution.property.retry.description":
    "Nombre maximal de tentatives de r\u00e9-ex\u00e9cution lorsque ce traitement est directement appel\u00e9. Une nouvelle tentative se produira si le traitement \u00e9choue ou expire, mais pas s\u02bcil est tu\u00e9 manuellement. Peut utiliser une r\u00e9f\u00e9rence de valeur d\u02bcoption comme \"{'$'}{'{'}option{'.'}retry{'}'}\".",
  "scheduledExecution.property.successOnEmptyNodeFilter.false.description":
    "\u00c9chec du traitement",
  "scheduledExecution.property.successOnEmptyNodeFilter.prompt":
    "Si le n\u0153ud est vide",
  "scheduledExecution.property.successOnEmptyNodeFilter.true.description":
    "Poursuivre l\u02bcex\u00e9cution",
  "scheduledExecution.property.timeout.description":
    "Dur\u00e9e maximale d\u02bcex\u00e9cution d\u02bcune ex\u00e9cution. Temps en secondes, ou pr\u00e9cisez les unit\u00e9s de temps : \"120m\", \"2h\", \"3d\". Utilisez vide ou 0 pour indiquer qu\u02bcil n\u02bcy a pas de d\u00e9lai d\u02bcattente. Peut inclure la valeur de l\u02bcoption r\u00e9f\u00e9rences comme \"{'$'}{'{'}option{'.'}timeout{'}'}\".",
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
  "jobquery.title.loglevelFilter": "Niveau de log",
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
    "Erreur lors de l\u02bcenregistrement du contenu:",
  "project.nodes.edit.empty.description":
    "Remarque : Aucun contenu n'\u00e9tait disponible.",
  "button.action.Cancel": "Annuler",
  "button.action.Save": "Enregistrer",
  "job-edit-page": {
    "nodes-tab-title": "N\u0153uds & Runners",
    "node-dispatch-true-label": "Envoi aux n\u0153uds via le Runner",
    "node-dispatch-false-label": "Ex\u00e9cuter sur le Runner",
    "section-title": "Envoi",
    "section-title-help":
      "Choisissez le Runner et les n\u0153uds s\u00e9lectionn\u00e9s",
  },
  "job-exec-page": {
    "nodes-tab-title": "Runner/N\u0153uds",
  },
  JobRunnerEdit: {
    section: {
      title: "D\u00e9finir le Runner",
    },
  },
  gui: {
    menu: {
      Nodes: "N\u0153uds",
    },
  },
  search: "Rechercher",
  browse: "Parcourir",
  result: "R\u00e9sultat:",
  actions: "Actions",
  none: "Aucun",
  set: {
    "all.nodes.as.default.filter":
      "D\u00e9finir tous les n\u0153uds comme filtre par d\u00e9faut",
    "as.default.filter": "D\u00e9finir en tant que filtre par d\u00e9faut",
  },
  remove: {
    "all.nodes.as.default.filter":
      "Supprimer tous les n\u0153uds comme filtre par d\u00e9faut",
    "default.filter": "Supprimer le filtre par d\u00e9faut",
  },
  "run.a.command.on.count.nodes.ellipsis":
    "Ex\u00e9cuter une commande sur {0} {1}",
  "create.a.job.for.count.nodes.ellipsis":
    "Cr\u00e9er un traitement pour {0} {1}",
  "resource.metadata.entity.tags": "Mots cl\u00e9s",
  filters: "Filtres",
  "all.nodes": "Tous les n\u0153uds",
  "delete.this.filter.ellipsis": "Supprimer ce filtre...",
  "enter.a.filter": "Entrer un filtre",
  "remove.all.nodes.as.default.filter":
    "Supprimer tous les n\u0153uds comme filtre par d\u00e9faut",
  "set.all.nodes.as.default.filter":
    "D\u00e9finir tous les n\u0153uds comme filtre par d\u00e9faut",
  "not.authorized": "Non autoris\u00e9",
  "disabled.execution.run": "Les ex\u00e9cutions sont d\u00e9sactiv\u00e9es.",
  "user.at.host": "Utilisateur {'@'} nom d\u02bch\u00f4te",
  "node.changes.success":
    "Les modifications de n\u0153ud ont \u00e9t\u00e9 enregistr\u00e9es avec succ\u00e8s.",
  "node.changes.notsaved":
    "Les modifications de n\u0153ud n'ont pas \u00e9t\u00e9 enregistr\u00e9es.",
  "node.remoteEdit.edit": "Modifier le n\u0153ud:",
  "node.remoteEdit.continue": "Continuer...",
  node: "N\u0153ud",
  "this.will.select.both.nodes": "Cela s\u00e9lectionnera les deux n\u0153uds.",
  "node.metadata.hostname": "Nom d\u02bch\u00f4te",
  "select.nodes.by.name": "S\u00e9lectionner les n\u0153uds par leur nom",
  "filter.nodes.by.attribute.value":
    "Filtrer les n\u0153uds par valeur d\u02bcattribut",
  "use.regular.expressions": "Utiliser des expressions r\u00e9guli\u00e8res:",
  "regex.syntax.checking":
    "V\u00e9rification de la syntaxe de l\u02bcexpression r\u00e9guli\u00e8re",
  "edit.ellipsis": "Modifier...",
  "node.metadata.username-at-hostname": "Nom d\u02bcutilisateur & h\u00f4te",
  "node.metadata.osFamily": "Famille OS",
  "node.metadata.osName": "Nom d\u02bcOS",
  "node.metadata.osArch": "Architecture OS",
  "node.metadata.osVersion": "Version d\u02bcOS",
  "node.metadata.type": "Type",
  "node.metadata.username": "Utilisateur",
  "node.metadata.tags": "Mots cl\u00e9s",
  "button.Edit.label": "Modifier",
  "default.paginate.prev": "-",
  "default.paginate.next": "+",
  "jump.to": "Sauter \u00e0:",
  "per.page": "Par page",
  "remove.default.filter": "Supprimer le filtre par d\u00e9faut",
  "scheduledExecution.action.edit.button.label": "Modifier ce traitement...",
  "scheduledExecution.action.duplicate.button.label":
    "Dupliquer ce traitement...",
  "scheduledExecution.action.duplicate.other.button.label":
    "Dupliquer ce traitement vers un autre projet...",
  "scheduledExecution.action.download.button.label":
    "T\u00e9l\u00e9charger la d\u00e9finition",
  "scheduledExecution.action.downloadformat.button.label":
    "T\u00e9l\u00e9charger la d\u00e9finition du traitement vers {0}",
  "scheduledExecution.action.delete.button.label": "Supprimer ce traitement",
  "scheduledExecution.action.edit.button.tooltip": "Modifier ce traitement",
  "scheduledExecution.action.duplicate.button.tooltip":
    "Dupliquer le traitement",
  "enable.schedule.this.job": "Activer la planification",
  "disable.schedule.this.job": "D\u00e9sctiver la planification",
  "scheduledExecution.action.enable.schedule.button.label":
    "Activer la planification",
  "scheduledExecution.action.disable.schedule.button.label":
    "D\u00e9sactiver la planification",
  "scheduleExecution.schedule.disabled":
    "La planification du traitement est d\u00e9sactiv\u00e9e",
  "enable.execution.this.job": "Activer l\u02bcex\u00e9cution",
  "disable.execution.this.job": "D\u00e9sactiver l\u02bcex\u00e9cution",
  "scheduledExecution.action.enable.execution.button.label":
    "Activer l\u02bcex\u00e9cution",
  "scheduledExecution.action.disable.execution.button.label":
    "D\u00e9sactiver l\u02bcex\u00e9cution",
  "scheduleExecution.execution.disabled":
    "L\u02bcex\u00e9cution du traitement est d\u00e9sactiv\u00e9e",
  "delete.this.job": "Supprimer ce traitement",
  "action.prepareAndRun.tooltip":
    "Choisir les options et lancer l\u02bcex\u00e9cution du traitement\u2026",
  "job.bulk.modify.confirm.panel.title":
    "Confirmer la modification du traitement en masse",
  "job.bulk.delete.confirm.message":
    "Voulez-vous vraiment supprimer les traitements s\u00e9lectionn\u00e9s ?",
  "job.bulk.disable_schedule.confirm.message":
    "D\u00e9planifier la planification pour tous les traitements s\u00e9lectionn\u00e9s ?",
  "job.bulk.enable_schedule.confirm.message":
    "Activer la planification pour tous les traitements s\u00e9lectionn\u00e9s ?",
  "job.bulk.disable_execution.confirm.message":
    "D\u00e9planifier l\u02bcex\u00e9cution pour tous les traitements s\u00e9lectionn\u00e9s ?",
  "job.bulk.enable_execution.confirm.message":
    "Activer l\u02bcex\u00e9cution pour tous les traitements s\u00e9lectionn\u00e9s ?",
  "job.bulk.disable_schedule.button": "D\u00e9sactiver les planifications",
  "job.bulk.enable_schedule.button": "Activer les planifications",
  "job.bulk.delete.button": "Supprimer les traitements",
  "job.bulk.disable_execution.button": "D\u00e9sactiver l\u02bcex\u00e9cution",
  "job.bulk.enable_execution.button": "Activer l\u02bcex\u00e9cution",
  "job.bulk.enable_execution.success":
    "Ex\u00e9cution activ\u00e9e pour {0} traitements.",
  "job.bulk.enable_schedule.success":
    "Planification activ\u00e9e pour {0} traitements.",
  "job.bulk.disable_schedule.success":
    "Planification d\u00e9sactiv\u00e9e pour {0} traitements.",
  "job.bulk.disable_execution.success":
    "Ex\u00e9cution d\u00e9sactiv\u00e9e pour {0} traitements.",
  "job.bulk.delete.success": " {0} traitements supprim\u00e9s.",
  "delete.selected.jobs": "Supprimer les traitements s\u00e9lectionn\u00e9s",
  "job.bulk.panel.select.title":
    "S\u00e9lectionner les traitements pour une modification en masse",
  "job.bulk.perform.action.menu.label": "Effectuer une action",
  "job.create.button": "Cr\u00e9er un nouveau traitement",
  "job.upload.button.title": "Charger une d\u00e9finition de traitement",
  cancel: "Annuler",
  "job.actions": "Actions de traitement",
  "job.bulk.activate.menu.label": "Modification en masse\u2026",
  "job.bulk.deactivate.menu.label": "Quitter le mode d'\u00e9dition en masse",
  "upload.definition.button.label": "Charger une d\u00e9finition",
  "new.job.button.label": "Nouveau traitement",
  "job.bulk.panel.select.message":
    "{n} Traitement s\u00e9lectionn\u00e9 | {n} Traitements s\u00e9lectionn\u00e9s",
  "cannot.run.job": "Ne peut pas ex\u00e9cuter le traitement",
  "disabled.schedule.run": "Les ex\u00e9cutions sont d\u00e9sactiv\u00e9es.",
  "disabled.job.run": "Les ex\u00e9cutions sont d\u00e9sactiv\u00e9es.",
  "schedule.on.server.x.at.y":
    "Planifi\u00e9 pour \u00eatre ex\u00e9cut\u00e9 sur le serveur {0} \u00e0 {1}",
  "schedule.time.in.future": "dans {0}",
  never: "Jamais",
  disabled: "D\u00e9sactiv\u00e9",
  "project.schedule.disabled":
    "La planification du projet est d\u00e9sactiv\u00e9e",
  "project.execution.disabled":
    "L\u02bcex\u00e9cution du projet est d\u00e9sactiv\u00e9e",
  "job.schedule.will.never.fire":
    "La planification du traitement ne se lancera jamais",
  "scm.import.status.UNKNOWN.display.text":
    "Statut de l\u02bcimport : non suivi",
  "scm.import.status.LOADING.description":
    "Import: le status du traitement est en cours de chargement",
  "scm.export.status.DELETED.display.text": "Supprim\u00e9",
  "scm.export.status.EXPORT_NEEDED.display.text": "Modifi\u00e9",
  "scm.export.status.CLEAN.description": "Status de l\u02bcexport: propre",
  "scm.import.status.DELETE_NEEDED.title.text":
    "Import: fichiers supprim\u00e9s",
  "scm.export.status.EXPORT_NEEDED.title.text": "Export: Modifi\u00e9",
  "scm.import.status.UNKNOWN.description": "Non suivi pour l\u02bcimport SCM",
  "scm.import.status.REFRESH_NEEDED.display.text":
    "Synchronisation n\u00e9cessaire",
  "scm.export.status.CREATE_NEEDED.description":
    "Status de l\u02bcexport: Nouveau traitement, pas encore ajout\u00e9 \u00e0 SCM",
  "scm.import.status.DELETE_NEEDED.description":
    "Status de l\u02bcimport: le fichier source a \u00e9t\u00e9 supprim\u00e9",
  "scm.export.status.CLEAN.display.text": "Pas de changement",
  "scm.export.status.LOADING.description":
    "Export: le status du traitement est en cours de chargement",
  "scm.import.status.IMPORT_NEEDED.description":
    "Status de l\u02bcimport: Les modifications du traitement doivent \u00eatre synchronis\u00e9es",
  "scm.import.status.REFRESH_NEEDED.title.text":
    "Import: Synchroniser les changements",
  "scm.export.status.REFRESH_NEEDED.title.text":
    "Export: Les modifications \u00e0 distance doivent \u00eatre synchronis\u00e9es",
  "scm.export.status.LOADING.display.text": "En cours de chargement",
  "scm.export.status.EXPORT_NEEDED.description":
    "Statut d\u02bcexportation : modifi\u00e9",
  "scm.export.status.CREATE_NEEDED.display.text": "Cr\u00e9\u00e9",
  "scm.import.status.IMPORT_NEEDED.display.text": "Importation n\u00e9cessaire",
  "scm.import.status.DELETE_NEEDED.display.text":
    "Le fichier source a \u00e9t\u00e9 supprim\u00e9",
  "scm.import.status.IMPORT_NEEDED.title.text":
    "Import: modifications entrantes",
  "scm.import.status.CLEAN.display.text": "\u00c0 jour",
  "scm.import.status.CLEAN.description":
    "Statut d\u02bcimportation: \u00e0 jour",
  "scm.import.status.REFRESH_NEEDED.description":
    "Statut d\u02bcimportation : les modifications du traitement doivent \u00eatre extraites",
  "scm.export.status.REFRESH_NEEDED.display.text":
    "Synchronisation n\u00e9cessaire",
  "scm.import.status.LOADING.display.text": "En cours de chargement",
  "scm.export.status.ERROR.display.text": "Une erreur inconnue est survenue.",
  "scm.import.status.ERROR.display.text": "Une erreur inconnue est survenue.",
  "scm.status.ERROR.display.text": "Erreur SCM",
  "scm.export.auth.key.noAccess":
    "L\u02bcutilisateur n'a pas acc\u00e8s \u00e0 la cl\u00e9 ou au mot de passe sp\u00e9cifi\u00e9",
  "scm.import.auth.key.noAccess":
    "L\u02bcutilisateur n'a pas acc\u00e8s \u00e0 la cl\u00e9 ou au mot de passe sp\u00e9cifi\u00e9",
  "scm.action.diff.clean.button.label": "Afficher les informations de Commit",
  "scm.import.plugin": "Plugin d\u02bcimportation SCM",
  "scm.action.diff.button.label": "Modifications des diff\u00e9rences",
  "scm.export.commit.job.link.title":
    "Cliquer pour valider ou ajouter ce traitement",
  "scm.export.commit.link.title":
    "Cliquer pour valider ou ajouter des changements",
  "scm.export.plugin": "Plugin d'export SCM",
  "job.toggle.scm.menu.on": "Activer SCM",
  "job.toggle.scm.menu.off": "D\u00e9sactiver SCM",
  "scm.import.actions.title": "Actions d'import SCM",
  "scm.export.actions.title": "Actions d'export SCM",
  "scm.export.title": "Export SCM",
  "scm.import.title": "Import SCM",
  "job.toggle.scm.button.label.off": "D\u00e9sactiver SCM",
  "job.toggle.scm.confirm.panel.title": "Confirmer la modification SCM",
  "job.toggle.scm.confirm.on":
    "Activer tous les plugins configur\u00e9s par SCM ?",
  "job.toggle.scm.confirm.off":
    "D\u00e9sctiver tous les plugins configur\u00e9s par SCM ?",
  "job.toggle.scm.button.label.on": "Activer SCM",
  "job.scm.status.loading.message": "Chargement du status SCM...",
  "page.section.Activity.for.jobs": "Activit\u00e9 des traitements",
  "widget.theme.title": "Th\u00e8me",
  "widget.nextUi.title": "Activer Next UI",
  "page.section.title.AllJobs": "Tous les traitements",
  "advanced.search": "Avanc\u00e9",
  "jobs.advanced.search.title": "Cliquer pour modifier le filtre",
  "filter.jobs": "Rechercher des traitements",
  "job.filter.quick.placeholder": "Rechercher",
  "job.filter.apply.button.title": "Rechercher",
  "job.filter.clear.button.title": "Effacer la recherche",
  all: "Tous",
  "job.tree.breakpoint.hit.info":
    "Remarque : Tous les d\u00e9tails du traitement n'ont pas \u00e9t\u00e9 charg\u00e9s car ce groupe contient trop de traitements. Cliquez sur le bouton pour charger les d\u00e9tails manquants.",
  "job.tree.breakpoint.load.button.title":
    "Charger tous les d\u00e9tails du traitement",
  "job.list.filter.save.modal.title": "Enregistrer le filtre",
  "job.filter.save.button.title": "Enregistrer en tant que filtre...",
  "job.list.filter.save.button": "Enregistrer le filtre",
  "job.list.filter.delete.filter.link.text": 'Supprimer le filtre "{0}"',
  "app.firstRun.title": "Bienvenue \u00e0 {0} {1}",
  "you.can.see.this.message.again.by.clicking.the":
    "Vous pouvez revoir ce message en cliquant sur le",
  "version.number": "num\u00e9ro de version",
  "in.the.page.footer": "dans le pied de page.",
  "no.authorized.access.to.projects":
    "Vous n\u02bcavez aucun acc\u00e8s autoris\u00e9 aux projets.",
  "no.authorized.access.to.projects.contact.your.administrator.user.roles.0":
    "Contactez votre administrateur. (R\u00f4les de l\u02bcutilisateur : {0})",
  "page.home.loading.projects": "Chargement de projets",
  "app.firstRun.md":
    "Merci d'\u00eatre abonn\u00e9 \u00e0 {0}.\n\n  \n\n\n* [{0} Portail d'assistance &raquo;](http://support.rundeck.com)\n\n* [{0} Documentation &raquo;]({1})",
  "page.home.section.project.title": "{0} Projet",
  "page.home.section.project.title.plural": "{0} Projets",
  "page.home.duration.in.the.last.day": "Au cours des derni\u00e8res 24 heures",
  by: "par",
  user: "Utilisateur",
  "user.plural": "Utilisateurs",
  "page.home.project.executions.0.failed.parenthetical":
    "({0} \u00e9chou\u00e9)",
  "page.home.search.projects.input.placeholder":
    "Recherche de projet : nom, label ou /regex/",
  "page.home.search.project.title":
    "{n} Projet trouv\u00e9 | {n} Projets trouv\u00e9s",
  "button.Action": "Action",
  "edit.configuration": "Modifier la configuration",
  "page.home.new.project.button.label": "Nouveau project",
  Execution: "{n} Ex\u00e9cutions | {n} Ex\u00e9cution | {n} Ex\u00e9cutions",
  in: "dans",
  "Project.plural": "Projets",
  discard: "Ignorer",
  "commandline.arguments.prompt.unquoted":
    "Arguments de la ligne de commande (sans quotes):",
  usage: "Utilisation",
  "form.label.valuesType.list.label": "Liste",
  "scheduledExecution.option.unsaved.warning":
    "Ignorez ou enregistrez les modifications apport\u00e9es \u00e0 cette option avant de finaliser les modifications apport\u00e9es au traitement.",
  "bash.prompt": "Bash:",
  "script.content.prompt": "Contenu du script:",
  "rundeck.user.guide.option.model.provider":
    "Guide utilisateur Rundeck - Fournisseur de mod\u00e8les d'options",
  save: "Enregistrer",
  "commandline.arguments.prompt": "Arguments de ligne de commande :",
  "commandline.arguments.prompt.unquoted.warning":
    "Avertissement ! S'appuyer sur des arguments sans quotes pourrait rendre ce traitement vuln\u00e9rable \u00e0 l\u02bcinjection de commandes. Utiliser avec pr\u00e9caution.",
  "add.new.option": "Ajouter une nouvelle option",
  "add.an.option": "Ajouter une option",
  "option.values.c": "1 valeur|{n} valeurs",
  "no.options.message": "Pas d'options",
  "the.option.values.will.be.available.to.scripts.in.these.forms":
    "Les valeurs des options seront disponibles pour les scripts sous ces formes:",
  "form.option.date.label": "Date",
  "form.option.enforcedType.label": "Restrictions",
  "form.option.usage.file.fileName.preview.description":
    "Le nom du fichier d'origine:",
  "form.option.discard.title":
    "Annuler les modifications apport\u00e9es \u00e0 l\u02bcoption",
  "form.option.valuesType.url.authentication.password.label": "Mot de passe",
  "form.option.enforcedType.none.label":
    "N\u02bcimporte quelle valeur peut \u00eatre utilis\u00e9e",
  "form.option.inputType.label": "Type d'entr\u00e9e",
  "form.option.defaultStoragePath.present.description":
    "Une valeur par d\u00e9faut sera charg\u00e9e \u00e0 partir du stockage de cl\u00e9s",
  "form.option.secureInput.false.label": "Texte brut",
  "form.option.name.label": "Nom de l\u02bcoption",
  "form.option.valuesType.url.filter.label": "Filtre de chemin JSON",
  "form.option.valuesType.url.authType.label": "Type d'authentification",
  "form.option.sort.description": "Trier la liste des valeurs autoris\u00e9es",
  "form.option.usage.secureAuth.message":
    "Les valeurs des options d'authentification s\u00e9curis\u00e9e ne sont pas disponibles pour les scripts ou les commandes",
  "form.option.valuesType.url.label": "URL distante",
  "form.option.valuesList.placeholder":
    "Liste s\u00e9par\u00e9e par des d\u00e9limiteurs (virgule par d\u00e9faut)",
  "form.option.secureInput.description":
    "Les valeurs d'entr\u00e9e s\u00e9curis\u00e9es ne sont pas stock\u00e9es par Rundeck apr\u00e8s utilisation. Si la valeur expos\u00e9e est utilis\u00e9e dans un script ou une commande, le journal de sortie peut contenir la valeur.",
  "form.option.valuesType.url.authentication.key.label": "Cl\u00e9",
  "form.option.date.description":
    "La date sera transmise \u00e0 votre traitement sous forme de cha\u00eene format\u00e9e de cette fa\u00e7on : mm/jj/aa HH:MM",
  "form.option.valuesType.url.authentication.username.label":
    "Nom d'utilisateur",
  "form.option.valuesType.url.authType.bearerToken.label": "Bearer Token",
  "form.option.enforced.label":
    "Appliqu\u00e9 \u00e0 partir des valeurs autoris\u00e9es",
  "form.option.description.label": "Description",
  "form.option.save.title":
    "Enregistrer les modifications apport\u00e9es \u00e0 l\u02bcoption",
  "form.option.type.label": "Type d'option",
  "form.option.multivalueAllSelected.label":
    "S\u00e9lectionner toutes les valeurs par d\u00e9faut",
  "form.option.secureExposed.false.label":
    "Authentification \u00e0 distance s\u00e9curis\u00e9e",
  "form.option.valuesType.url.authentication.token.label": "Jeton",
  "form.option.delimiter.label": "D\u00e9limiteur",
  "form.option.valuesURL.placeholder": "URL distante",
  "form.option.valuesType.url.authType.empty.label":
    "S\u00e9lectionner le type d\u02bcauthentification",
  "form.option.valuesDelimiter.description":
    "D\u00e9finir le d\u00e9limiteur pour les valeurs autoris\u00e9es",
  "form.option.usage.file.preview.description":
    "Le chemin d\u02bcacc\u00e8s local sera accessible aux scripts sous ces formes:",
  "form.option.secureExposed.false.description":
    "Entr\u00e9e de mot de passe, valeur non expos\u00e9e dans les scripts ou les commandes, utilis\u00e9e uniquement par Node Executors pour l\u02bcauthentification.",
  "form.option.valuesType.url.filter.error.label":
    "Le filtre de chemin JSON de l\u02bcURL distante a une syntaxe non valide",
  "form.option.valuesType.url.authType.apiKey.label": "Cl\u00e9 API",
  "form.option.optionType.text.label": "Texte",
  "form.option.secureExposed.true.label":
    "Texte brut avec saisie du mot de passe",
  "form.option.valuesType.url.filter.description":
    'Filtrez les r\u00e9sultats JSON \u00e0 l\u02bcaide d\u02bcun chemin de cl\u00e9. Par exemple "$.key.path"',
  "form.option.valuesType.url.authentication.tokenInformer.header.label":
    "En-t\u00eate",
  "form.option.defaultStoragePath.description":
    "Chemin de stockage des cl\u00e9s pour une valeur de mot de passe par d\u00e9faut",
  "form.option.multivalued.label": "Valeurs multiples",
  "form.option.multivalued.description":
    "Autoriser \u00e0 choisir plusieurs valeurs d\u02bcentr\u00e9e.",
  "form.option.valuesType.url.authentication.tokenInformer.query.label":
    "Requ\u00eater un param\u00e8tre",
  "form.option.create.title": "Enregistrer la nouvelle option",
  "form.option.regex.label":
    "Correspondance avec une expression r\u00e9guli\u00e8re",
  "form.option.optionType.file.label": "Fichier",
  "form.option.valuesDelimiter.label": "D\u00e9limiteur de liste",
  "form.option.cancel.title": "Annuler l\u02bcajout d\u02bcune nouvelle option",
  "form.option.values.label": "Valeurs autoris\u00e9es",
  "form.option.dateFormat.description.md":
    "Entrez un format de date comme d\u00e9crit dans [docs momentjs] (http://momentjs.com/docs/#/displaying/format/)",
  "form.option.defaultStoragePath.label": "Chemin de stockage",
  "form.option.defaultValue.label": "Valeur par d\u00e9faut",
  "form.option.delimiter.description":
    "Le d\u00e9limiteur sera utilis\u00e9 pour joindre toutes les valeurs d\u02bcentr\u00e9e. Cela peut \u00eatre n\u02bcimporte quelle cha\u00eene : \u02bc\u02bc(espace), \u02bc, \u02bc(virgule), etc. Note : ne pas utiliser de guillemets.",
  "form.option.secureInput.false.description": "Entr\u00e9e de texte brut.",
  "form.option.valuesType.url.authType.basic.label": "Basique ",
  "form.option.valuesType.url.authentication.tokenInformer.label":
    "Injecter une cl\u00e9 ",
  "form.option.secureExposed.true.description":
    "Mot de passe entr\u00e9, valeur expos\u00e9e dans les scripts et les commandes.",
  "form.option.dateFormat.title": "Format de date",
  "form.option.label.label": "\u00c9tiquette d\u02bcoption",
  "form.option.valuesUrl.description": "URL vers un service JSON distant.",
  "form.option.multivalued.secure-conflict.message":
    "Les options d\u02bcentr\u00e9e s\u00e9curis\u00e9e n\u02bcautorisent pas plusieurs valeurs",
  "form.option.sort.label": "Trier les valeurs",
  "form.option.usage.file.sha.preview.description":
    "La valeur SHA-256 du contenu du fichier:",
  "Option.property.description.description":
    "La description sera affich\u00e9 avec Markdown.",
  "option.defaultValue.regexmismatch.message":
    'La valeur par d\u00e9faut "{0}" ne correspond pas \u00e0 la regex: {1}',
  "option.multivalued.secure-conflict.message":
    "L\u02bcentr\u00e9e s\u00e9curis\u00e9e ne peut pas \u00eatre utilis\u00e9e avec une entr\u00e9e \u00e0 valeurs multiples",
  "option.defaultValue.notallowed.message":
    "La valeur par d\u00e9faut ne figurait pas dans la liste des valeurs autoris\u00e9es et les valeurs sont pr\u00e9d\u00e9finies",
  "option.enforced.secure-conflict.message":
    "L\u02bcentr\u00e9e s\u00e9curis\u00e9e ne peut pas \u00eatre utilis\u00e9e avec des valeurs pr\u00e9d\u00e9finies",
  "option.file.config.disabled.message":
    "Le plugin de configuration du type d\u02bcoption de fichier n\u02bcest pas activ\u00e9",
  "option.defaultValue.required.message":
    "Sp\u00e9cifiez une valeur par d\u00e9faut pour les options requises lorsque le traitement est planifi\u00e9.",
  "option.enforced.emptyvalues.message":
    "Les valeurs autoris\u00e9es (liste ou URL distante) doivent \u00eatre sp\u00e9cifi\u00e9es si les valeurs sont obligatoires",
  "option.file.required.message":
    "Le type d'option de fichier ne peut pas \u00eatre obligatoire lorsque le traitement est planifi\u00e9.",
  "option.regex.invalid.message":
    "Expression r\u00e9guli\u00e8re non valide : {0}",
  "option.file.config.invalid.message":
    "La configuration du type d'option de fichier n'est pas valide : {0}",
  "option.delimiter.blank.message":
    "Vous devez sp\u00e9cifier un d\u00e9limiteur pour les options \u00e0 plusieurs valeurs",
  "option.hidden.notallowed.message":
    "Les options masqu\u00e9es doivent avoir une valeur par d\u00e9faut ou un chemin de stockage.",
  "option.values.regexmismatch.message":
    'La valeur autoris\u00e9e "{0}" ne correspond pas \u00e0 l\u02bcexpression r\u00e9guli\u00e8re : {1}',
  "option.defaultValue.multivalued.notallowed.message":
    'La valeur par d\u00e9faut contient une cha\u00eene qui ne figurait pas dans la liste des valeurs autoris\u00e9es et ces valeurs sont pr\u00e9d\u00e9fnies : "{0}". Remarque : les espaces sont importants.',
  "Option.required.label": "Obligatoire",
  "Option.hidden.description":
    "Doit \u00eatre masqu\u00e9 sur la page d\u02bcex\u00e9cution du traitement",
  "Option.required.description":
    "Exiger que cette option ait une valeur non vide lors de l\u02bcex\u00e9cution du traitement",
  "Option.hidden.label": "Devrait \u00eatre masqu\u00e9",
  "form.option.regex.placeholder": "Entrez une expression r\u00e9guli\u00e8re",
  "home.user": "{n} Utilisateurs | {n} Utilisateur | {n} Utilisateurs",
  "home.table.projects": "Projets",
  "home.table.activity": "Activit\u00e9",
  "home.table.actions": "Actions",
  "option.click.to.edit.title": "Cliquer pour modifier",
  "form.option.regex.validation.error":
    "Valeur non valide : elle doit correspondre au mod\u00e8le : {0}",
  "form.field.required.message": "Ce champ est requis",
  "form.field.too.long.message":
    "Cette valeur ne peut pas comporter plus de {max} caract\u00e8res",
  "form.option.validation.errors.message":
    "Corriger les erreurs de validation avant d\u2019enregistrer les modifications",
  "option.list.header.name.title": "Nom",
  "option.list.header.values.title": "Valeurs",
  "option.list.header.restrictions.title": "Restrictions",
  "util.undoredo.undo": "Annuler",
  "util.undoredo.redo": "Refaire",
  "util.undoredo.revertAll": "Annuler toutes les modifications",
  "util.undoredo.revertAll.confirm":
    "Vraiment annuler toutes les modifications ?",
  "option.view.required.title": " (Obligatoire)",
  "option.view.allowedValues.label": "Valeurs autoris\u00e9es",
  "option.view.valuesUrl.title":
    "Valeurs charg\u00e9es \u00e0 partir de l\u02bcURL distante : {0}",
  "option.view.valuesUrl.placeholder": "URL",
  "option.view.enforced.title":
    "L\u02bcentr\u00e9e doit \u00eatre une valeur autoris\u00e9e",
  "option.view.enforced.placeholder": "Strict",
  "option.view.regex.info.note":
    "Les valeurs doivent correspondre \u00e0 l\u02bcexpression r\u00e9guli\u00e8re:",
  "option.view.notenforced.title":
    "Aucune restriction sur la valeur d\u02bcentr\u00e9e",
  "option.view.notenforced.placeholder": "Aucun",
  "option.view.action.delete.title": "Supprimer cette option",
  "option.view.action.edit.title": "Modifier cette option",
  "option.view.action.duplicate.title": "Dupliquer cette option",
  "option.view.action.moveUp.title": "D\u00e9placer vers le haut",
  "option.view.action.moveDown.title": "D\u00e9placer vers le bas",
  "option.view.action.drag.title": "Faire glisser pour r\u00e9organiser",
  "storage.enter.path": "Saisissez un chemin",
  "storage.enter.password": "Saisissez un mot de passe",
  "storage.enter.directory.name": "Saisissez le nom du r\u00e9pertoire",
  "storage.specify.name": "Sp\u00e9cifiez un nom.",
  "soft.wrap": "Retour \u00e0 la ligne automatique",
  "average.duration": "Dur\u00e9e moyenne",
  "job.starting.execution": "Commencer l\u02bcex\u00e9cution...",
  "enter.a.command": "Entrez une commande",
  "retry.failed.nodes": "R\u00e9essayer les n\u0153uds ayant \u00e9chou\u00e9s",
  "Execution.plural": "Ex\u00e9cutions",
  "Workflow.property.keepgoing.prompt": "Si une \u00e9tape \u00e9choue :",
  "Workflow.strategy.description.node-first":
    "Ex\u00e9cutez toutes les \u00e9tapes sur un n\u0153ud avant de passer au n\u0153ud suivant.",
  "Workflow.strategy.label.parallel": "Parall\u00e8le",
  "access.control": "Contr\u00f4le d\u02bcacc\u00e8s",
  "all.nodes.menu.item": "-Tous les n\u0153uds-",
  "button.action.Create": "Cr\u00e9er",
  "collapse.all": "Tout r\u00e9duire",
  "delete.project": "Supprimer le projet",
  "dispatch.to.nodes": "Envoi aux n\u0153uds",
  "edit.message.of.the.day": "Modifier le message du jour...",
  "edit.nodes": "Modifier les n\u0153uds",
  "edit.nodes.title": "Editer les n\u0153uds",
  "edit.readme": "Editer Lisez\u00admoi",
  "expand.all": "D\u00e9velopper tout",
  "export.archive": "Export d\u02bcarchive",
  "gui.menu.AccessControl": "Contr\u00f4le d\u02bcacc\u00e8s",
  "gui.menu.KeyStorage": "Stockage de cl\u00e9s",
  "gui.menu.Scm": "Configuration SCM",
  "import.archive": "Import d\u02bcarchives",
  "project.admin.menu.Scm.title": "Configurer SCM...",
  "project.node.sources.title": "Sources de n\u0153ud",
  "schedule.to.run.repeatedly": "Ex\u00e9cution cyclique ?",
  "scheduledExecution.property.loglevel.label": "Niveau de journal",
  "scheduledExecution.property.multipleExecutions.label":
    "Ex\u00e9cutions multiples ?",
  "scheduledExecution.property.retry.delay.label":
    "d\u00e9lai avant nouvel essai",
  "scheduledExecution.property.retry.label": "Recommencez",
  "scheduledExecution.property.timeout.label": "Temps max",
  "step.type.jobreference.title": "R\u00e9f\u00e9rence \u00e0 un traitement",
  add: "ajouter",
  Node: "N\u0153ud",
  "Node.plural": "N\u0153uds",
  "your.recently.executed.commands":
    "Vos commandes r\u00e9cemment ex\u00e9cut\u00e9es",
  "node.dispatch.settings": "Param\u00e8tres d\u02bcenvoi de n\u0153ud",
  running1: "En cours...",
  "maximum.number.of.parallel.threads.to.use":
    "Nombre maximal de threads parall\u00e8les \u00e0 utiliser",
  "maximum.threadcount.for.nodes":
    "Nombre maximal de threads pour les n\u0153uds",
  "continue.to.execute.on.other.nodes":
    "Continuer \u00e0 ex\u00e9cuter sur d\u02bcautres n\u0153uds",
  continue: "Continuer",
  "do.not.execute.on.any.other.nodes":
    "Ne pas ex\u00e9cuter sur d\u02bcautres n\u0153uds",
  stop: "Arr\u00eater",
  "job.execution.acl.missing.owner.warning.title":
    "Cette planification n\u02bca aucun utilisateur enregistr\u00e9 : rien n\u02bcautorise l\u02bcex\u00e9cution de ce traitement. La planification s\u02bcex\u00e9cute toujours, mais une version future pourrait appliquer cette v\u00e9rification. Enregistrez \u00e0 nouveau le traitement avec un utilisateur autoris\u00e9 \u00e0 l\u02bcex\u00e9cuter.",
  "job.execution.acl.missing.owner.blocked.warning.title":
    "Cette planification n\u02bca aucun utilisateur enregistr\u00e9 : rien n\u02bcautorise l\u02bcex\u00e9cution de ce traitement et la planification est ignor\u00e9e. Enregistrez \u00e0 nouveau le traitement avec un utilisateur autoris\u00e9 \u00e0 l\u02bcex\u00e9cuter.",
  "script.content.multiline.prompt.warning":
    "ATTENTION : cet usage peut \u00eatre DANGEREUX. Consultez la documentation pour plus de d\u00e9tails.",
  "global.log.filters": "Filtres de journal global",
  "option.usage.multiline.note":
    "Les valeurs d\u02bcune option de texte multiligne devront probablement \u00eatre plac\u00e9es entre guillemets lorsqu\u02bcelles sont utilis\u00e9es dans des scripts ou des arguments de ligne de commande :",
  "form.option.optionType.multiline.label": "Texte multiligne",
  "scheduledExecution.jobName.label": "Nom du traitement",
  "scheduledExecution.property.description.label": "Description",
  "job.editor.preview.runbook": "Aper\u00e7u Readme",
  "job.editor.workflow.unsavedchanges.warning":
    "Certaines modifications apport\u00e9es au workflow ne sont pas enregistr\u00e9es.",
  "choose.action.label": "Choisir \u2026",
  "scheduledExecution.groupPath.description":
    "Le groupe est un chemin s\u00e9par\u00e9 par /",
  more: "Plus...",
  "job.edit.groupPath.choose.text":
    "Cliquez sur le nom du groupe \u00e0 utiliser",
  "scheduledExecution.property.executionLifecyclePluginConfig.help.text":
    "Les plugins s\u00e9lectionn\u00e9s seront activ\u00e9s pour ce traitement.",
  "JobExec.nodeStep.true.label": "\u00c9tape du n\u0153ud",
  "step.plugins.filter.prompt": "Etape de recherche",
  Project: "Projet",
  "Workflow.Step.jobreference.title": "Selectionner le traitement par",
  "Workflow.Step.jobreference.name.label": "Nom",
  "Workflow.Step.jobreference.name.description":
    "Utilisez Nom et groupe pour rechercher le traitement.",
  "Workflow.Step.jobreference.uuid.label": "UUID",
  "Workflow.Step.jobreference.uuid.description":
    "Recherche de traitement par UUID.",
  "Workflow.Step.jobreference.name-group.label": "Nom du poste / groupe",
  "scheduledExecution.groupPath.label": "Chemin de groupe",
  "Workflow.Step.uuid.label": "UUID",
  "Workflow.Step.jobreference.uuid.help":
    "Vous pouvez saisir un nom de traitement ou un UUID, mais si l\u02bcUUID et le nom de traitement sont sp\u00e9cifi\u00e9s, ils doivent correspondre au m\u00eame traitement.",
  "Workflow.Step.argString.label": "Arguments",
  "Workflow.Step.jobreference.import.options.label": "Importer des options ?",
  "Workflow.Step.jobreference.import.options.help":
    "Passez en argument les valeurs de l\u02bcoption de traitement parente correspondant aux options du traitement r\u00e9f\u00e9renc\u00e9.",
  "Workflow.Step.jobreference.ignore.notifications.label":
    "Ignorer les notifications",
  "Workflow.Step.jobreference.ignore.notifications.help":
    "Ignorer toute notification dans un traitement r\u00e9f\u00e9renc\u00e9.",
  "Workflow.Step.jobreference.fail.on.disabled.label":
    "\u00c9chec de l\u02bcex\u00e9cution d\u00e9sactiv\u00e9e ?",
  "Workflow.Step.jobreference.fail.on.disabled.help":
    "Si le traitement r\u00e9f\u00e9renc\u00e9 a \u00e9t\u00e9 d\u00e9sactiv\u00e9, il sera consid\u00e9r\u00e9 comme un \u00e9chec.",
  "Workflow.Step.jobreference.child.nodes.label":
    "Utiliser les n\u0153uds du traitement r\u00e9f\u00e9renc\u00e9.",
  "Workflow.Step.jobreference.child.nodes.help":
    "Si la t\u00e2che r\u00e9f\u00e9renc\u00e9e provient d\u02bcun autre projet, vous pouvez utiliser la liste de n\u0153uds de t\u00e2che r\u00e9f\u00e9renc\u00e9e au lieu des n\u0153uds du parent.",
  "override.node.filters": "Remplacer les filtres de n\u0153uds ?",
  "JobExec.property.nodeFilter.help.description":
    "Entrez un filtre de n\u0153ud pour remplacer les n\u0153uds cibles du traitement d\u00e9clench\u00e9. Si vous entrez une nouvelle cha\u00eene de filtre, vous pouvez \u00e9galement modifier le nombre de threads et le comportement si le job \u00e9choue pour un n\u0153ud. Si le job r\u00e9f\u00e9renc\u00e9 appartient \u00e0 un autre projet, ce filtre n'utilisera que les n\u0153uds du projet parent.",
  "scheduledExecution.property.nodeIntersect.label":
    "Intersection de n\u0153uds",
  "scheduledExecution.property.nodeIntersect.false":
    "Non, utilisez le filtre de n\u0153ud r\u00e9f\u00e9renc\u00e9 ou remplac\u00e9 par un traitement r\u00e9f\u00e9renc\u00e9",
  "scheduledExecution.property.nodeIntersect.true":
    "Oui, cr\u00e9ez une intersection de n\u0153uds avec les n\u0153uds de traitement actuels",
  "node.filter.prompt": "Filtre de n\u0153ud",
  "JobExec.property.nodeThreadcount.null.description":
    "Si ce champ est vide, utilisez le nombre de threads d\u00e9fini pour le traitement d\u00e9clench\u00e9.",
  "JobExec.property.nodeKeepgoing.null.description":
    "Maintenir le comportement d\u00e9fini pour le traitement d\u00e9clench\u00e9",
  "Workflow.property.keepgoing.true.description":
    "Ex\u00e9cutez les \u00e9tapes restantes avant d\u02bc\u00e9chouer.",
  "Workflow.property.keepgoing.false.description":
    "Arr\u00eatez \u00e0 l\u02bc\u00e9tape \u00e9chou\u00e9e.",
  "JobExec.property.nodeRankOrder.null.description":
    "Maintenir l\u02bcordre des n\u0153uds d\u00e9fini dans le traitement d\u00e9clench\u00e9",
  "JobExec.nodeStep.title": "Ex\u00e9cuter en tant que",
  "JobExec.nodeStep.true.description":
    "Ex\u00e9cute pour chaque n\u0153ud et permet l\u02bcutilisation de variables de contexte de n\u0153ud en tant qu\u02bcarguments du traitement.",
  "JobExec.nodeStep.false.label": "\u00c9tape du flux de traitement",
  "JobExec.nodeStep.false.description":
    "N\u02bcex\u00e9cute qu\u02bcune seule fois et n\u02bca aucune information de contexte de n\u0153ud.",
  "Workflow.Step.jobreference.argString.placeholder":
    "Entrez des arguments, par ex. -option1 valeur -option2 valeur",
  "Workflow.Step.jobreference.uuid.placeholder":
    "Entrez l\u02bcUUID, par ex. 5910b723-2436-4497-96d5-49a08db95d2b",
  "select.an.existing.job.to.use":
    "S\u00e9lectionnez un traitement existant \u00e0 utiliser",
  "Workflow.step.property.description.placeholder":
    "\u00c9tiquette ou description de cette \u00e9tape",
  "enter.a.node.filter.override":
    "Entrez une cha\u00eene de filtre. Laissez vide pour utiliser le filtre d\u00e9fini pour le traitement d\u00e9clench\u00e9.",
  "mynode1.mynode2": "monn\u0153ud1 monn\u0153ud2",
  include: "Inclure",
  exclude: "Exclure",
  attribute: "attribut",
  value: "valeur",
  "step.type.jobreference.project.label": "Projet en cours ({0})",
  "no.jobs.in.this.project": "Aucun traitement dans ce projet",
  "JobExec.property.nodeFilter.null.description":
    "Les n\u0153uds utilis\u00e9s seront ceux d\u00e9finis pour le traitement d\u00e9clench\u00e9.",
  "results.truncated.count.results.shown":
    "R\u00e9sultats tronqu\u00e9s. {0} r\u00e9sultats affich\u00e9s.",
  "options.label": "Options",
  "Workflow.stepErrorHandler.keepgoingOnSuccess.label":
    "Continuez \u00e0 r\u00e9ussir.",
  "Workflow.stepErrorHandler.keepgoingOnSuccess.description":
    "Si le flux de traitement keepgoing est false, cela permet au workflow de continuer lorsque le gestionnaire d\u02bcerreurs a r\u00e9ussi",
  "Workflow.stepErrorHandler.label.keep.going.on.success":
    "continuer \u00e0 r\u00e9ussir",
  "framework.service.WorkflowNodeStep.description":
    "Fonctionne une fois pour chaque n\u0153ud dans le flux de traitement.",
  "framework.service.WorkflowStep.description":
    "Fonctionne une seule fois dans un flux de traitement.",
  "archive.import.importConfig.true.help":
    "Remplace la configuration du projet par les propri\u00e9t\u00e9s stock\u00e9es dans l\u02bcarchive.",
  "archive.import.importACL.true.title":
    "Importer les strat\u00e9gies ACL du projet",
  "archive.import.importWebhooks.false.help":
    "Ne pas importer les webhooks dans le projet",
  "archive.import.importWebhooks.true.help":
    "Importer les webhooks dans le projet",
  "archive.import.importScm.false.help":
    "Ne pas importer la configuration SCM.",
  "archive.import.importConfig.false.help":
    "N\u02bcimporte pas la configuration du projet.",
  "archive.import.importWebhooks.true.title": "Importer les webhooks",
  "archive.import.importACL.false.help":
    "N\u02bcimporte pas les strat\u00e9gies ACL.",
  "archive.import.importWebhooks.false.title": "Ne pas importer les webhooks",
  "archive.import.importACL.true.help":
    "Importer toutes les strat\u00e9gies ACL du projet en \u00e9crasant les strat\u00e9gies portant le m\u00eame nom.",
  "archive.import.importNodesSource.false.help":
    "Ne pas importer les sources de n\u0153uds.",
  "archive.import.importScm.unauthorized.help":
    "Non autoris\u00e9 \u00e0 configurer SCM.",
  "archive.import.importConfig.true.title":
    "Importer la configuration du projet",
  "archive.import.importScm.true.title":
    "Importer la configuration SCM du projet.",
  "archive.import.importNodesSource.true.help":
    "Remplace les sources du n\u0153ud par les propri\u00e9t\u00e9s pr\u00e9sentes dans l\u02bcarchive.",
  "archive.import.importNodesSource.true.title":
    "Importer les sources de n\u0153uds",
  "archive.import.importNodesSource.failed.message":
    "Echec de l\u02bcenregistrement du fichier de n\u0153uds, veuillez contacter votre administrateur.",
  "archive.import.importACL.unauthorized.help":
    "Non autoris\u00e9 \u00e0 cr\u00e9er des strat\u00e9gies ACL.",
  "archive.import.importScm.true.help":
    "Importer la configuration SCM du projet sauf si le projet dispose d\u02bcune configuration SCM.",
  profile: "Profil",
  logout: "D\u00e9connexion",
  "domain.User.email.label": "Email",
  "domain.User.firstName.label": "Pr\u00e9nom",
  "domain.User.lastName.label": "Nom de famille",
  "security.groups.label": "Groupes",
  "security.groups.description":
    "La liste des noms de groupes / r\u00f4les fournis par le syst\u00e8me de connexion. (Ne peut pas \u00eatre modifi\u00e9 via le profil utilisateur.)",
  "not.set": "Pas encore d\u00e9fini",
  "Add a new Node Source":
    "Ajouter une nouvelle source de mod\u00e8le de ressource",
  "Add a new Node Enhancer": "Ajouter un nouveau modificateur de n\u0153ud",
  "Node Sources for the project. Sources are loaded in the defined order, with later sources overriding earlier sources. (You can use ${project.name} inside configuration values to substitute the project name.)":
    "Vous pouvez ajouter des sources personnalis\u00e9es suppl\u00e9mentaires et leurs r\u00e9sultats seront utilis\u00e9s dans l\u02bcordre affich\u00e9 ci-dessous. Les sources ult\u00e9rieures remplaceront les sources ant\u00e9rieures. (Vous pouvez utiliser <code>${project.name}</code> dans les valeurs de configuration pour remplacer le nom du projet.)",
  "Node Enhancers can modify the data loaded from Node Sources.":
    "Les modificateurs de n\u0153ud peuvent modifier les donn\u00e9es charg\u00e9es depuis les sources de n\u0153uds.",
  message_webhookButtonRegenerate: "G\u00e9n\u00e9rer \u00e0 nouveau",
  "project.activitySummary.failedCount": "({0} \u00c9chec)",
  "execution.status.succeeded": "R\u00e9ussi",
  "execution.status.failed": "\u00c9chou\u00e9",
  "execution.status.running": "En cours",
  "execution.status.waiting": "En attente",
  "scheduledExecution.crontab.field.hour": "Heure",
  "Activity for Jobs": "Activit\u00e9 des traitements",
  "Add an option": "Ajouter une option",
  "All Jobs": "Tous les traitements",
  "Collapse All": "Tout r\u00e9duire",
  "Execute locally": "Ex\u00e9cuter localement",
  Executions: "Ex\u00e9cutions",
  "Expand All": "D\u00e9velopper tout",
  "Global Log Filters": "Filtres de journal global",
  "No Options": "Aucune option",
  "Save Filter": "Enregistrer le filtre \u2026",
  "Selected Plugins will be enabled for this Job.":
    "Les plugins s\u00e9lectionn\u00e9s seront activ\u00e9s pour ce traitement.",
  "Workflow.property.strategy.label": "Strat\u00e9gie",
  "button.cancel": "Annuler",
  "button.create": "Cr\u00e9er",
  "scheduledExecution.property.timeout.title": "Temps max",
  Enhancers: "Modificateurs",
  Yes: "Oui",
  No: "Non",
  "thread.count": "Nombre de threads",
  "settings.theme.options.system": "Syst\u00e8me",
  "Allow this Job to be scheduled?":
    "Autoriser ce traitement \u00e0 \u00eatre planifi\u00e9 ?",
  "Allow this Job to be executed?":
    "Autoriser l\u02bcex\u00e9cution de ce traitement ?",
  "scheduledExecution.property.description.plain.description":
    "La description sera affich\u00e9e en texte brut",
  Workflow: {
    property: {
      keepgoing: {
        true: {
          description:
            "Ex\u00e9cutez les \u00e9tapes restantes avant d\u02bc\u00e9chouer.",
        },
        false: {
          description:
            "Arr\u00eatez \u00e0 l\u02bc\u00e9tape \u00e9chou\u00e9e.",
        },
        prompt: "Si une \u00e9tape \u00e9choue :",
      },
      strategy: {
        label: "Strat\u00e9gie",
        description:
          "S\u00e9lectionnez la strat\u00e9gie de chronologie des \u00e9tapes du workflow.",
        learnMore: "En savoir plus",
        selectPlaceholder: "S\u00e9lectionnez une strat\u00e9gie",
      },
      executionBehavior: {
        label: "Comportement d\u02bcex\u00e9cution",
      },
      stepFailureBehavior: {
        label: "Comportement en cas d\u02bc\u00e9chec d\u02bcune \u00e9tape",
      },
    },
    logFilters: "Filtres de journal",
    edit: "modifier",
    duplicateStep: "Dupliquer cette \u00e9tape",
    deleteThisStep: "Supprimer cette \u00e9tape",
    dragToReorder: "Faites glisser pour r\u00e9organiser",
    errorHandlerDescription:
      "Le gestionnaire d\u02bcerreurs s\u02bcex\u00e9cutera si l\u02bc\u00e9tape \u00e9choue.",
    nodeStep: "\u00c9tape du n\u0153ud",
    workflowStep: "\u00c9tape du flux de traitement",
    label: "Workflow",
    addStep: "Ajouter une \u00e9tape",
    jobSteps: {
      label: "\u00c9tapes du traitement",
    },
    logFiltersTooltip:
      "Filtres qui affecteront les journaux produits par ces \u00e9tapes",
    addLogFilter: "Ajouter un filtre de journal",
    clickToEdit: "Cliquez pour modifier",
    itemControls: "Contr\u00f4les de l\u02bc\u00e9l\u00e9ment",
    clickOnStepType:
      "Cliquez sur un type d\u02bc\u00e9tape pour l\u02bcajouter",
    editStep: "Modifier l\u02bc\u00e9tape",
    saveStep: "Enregistrer l\u02bc\u00e9tape",
    setSteps: "D\u00e9finir les \u00e9tapes",
    conditional: {
      defineCondition: "D\u00e9finir la condition",
      conditionDescription:
        "Toutes les conditions suivantes doivent \u00eatre remplies. Ajoutez un ensemble de conditions pour int\u00e9grer une logique OU.",
      conditionGroups: "Ensemble de conditions",
      addCondition: "+ Ajouter",
      addconditionGroups: "+ Ajouter un ensemble de conditions",
      removeconditionGroups: "Supprimer l\u02bcensemble de conditions",
      removeCondition: "Supprimer la condition",
      selectField: "S\u00e9lectionner un champ",
      selectOperator: "S\u00e9lectionner un op\u00e9rateur",
      enterValue: "Saisir une valeur",
      operator: {
        equals: "\u00c9gal \u00e0",
        notEquals: "Diff\u00e9rent de",
        contains: "Contient",
        notContains: "Ne contient pas",
        matches: "Correspond \u00e0",
        greaterThan: "Sup\u00e9rieur \u00e0",
        greaterThanOrEqual: "Sup\u00e9rieur ou \u00e9gal \u00e0",
        lessThan: "Inf\u00e9rieur \u00e0",
        lessThanOrEqual: "Inf\u00e9rieur ou \u00e9gal \u00e0",
      },
    },
    stepLabel: "Libell\u00e9 de l\u02bc\u00e9tape",
    noSteps: "Aucune \u00e9tape de workflow",
    clickAddStep:
      "Ajoutez des \u00e9tapes pour d\u00e9finir ce que fera ce traitement. Cliquez sur \u00ab Ajouter une \u00e9tape \u00bb pour commencer.",
    addErrorHandler: "Ajouter un gestionnaire d\u02bcerreur",
    errorHandler: "Gestionnaire d\u02bcerreur",
    editErrorHandler: "Modifier le gestionnaire d\u02bcerreur",
    collapse: "R\u00e9duire",
    expand: "D\u00e9velopper",
  },
  "plugin.type.WorkflowStep.title.plural": "\u00c9tapes du flux de traitement",
  "plugin.type.WorkflowStep.title": "\u00c9tape du flux de traitement",
  "plugin.type.WorkflowNodeStep.title.plural": "N\u0153ud \u00e9tapes",
  "plugin.type.WorkflowNodeStep.title": "\u00c9tape du n\u0153ud",
  "job.label": "Traitement",
  "archive.import.validateJobRef.false.title": "Ne pas valider",
  "archive.import.validateJobRef.true.help":
    "G\u00e9n\u00e9rer des erreurs lors de l\u02bcutilisation d\u02bcun traitement r\u00e9f\u00e9renc\u00e9 qui n\u02bcexiste pas.",
  "archive.import.validateJobRef.true.title":
    "Valider les traitements r\u00e9f\u00e9renc\u00e9s",
  "archive.import.validateJobRef.false.help":
    "Importer les traitements r\u00e9f\u00e9renc\u00e9s sans v\u00e9rifier leur existence.",
  "archive.import.uuidOption.remove.description":
    "Supprimer les UUID des traitements import\u00e9s.",
  "archive.import.uuidOption.preserve.description":
    "Conservez les UUID de traitements import\u00e9s. Remarque : Si un traitement avec le m \u00eame UUID existe dans un autre projet, le nouveau traitement ne sera pas import\u00e9.",
  "archive.import.uuidOption.preserve.label": "Pr\u00e9server les UUID",
  "archive.import.uuidOption.remove.label": "Supprimer les UUID",
  jobUpload: {
    dupeOption: {
      create: {
        word: "Cr\u00e9er",
        label: "Toujours {0} un nouveau traitement",
      },
      label:
        "Lorsqu\u02bcun traitement portant le m\u00eame nom existe d\u00e9j\u00e0 :",
      update: {
        word: "Mettre \u00e0 jour",
        label: "{0} le traitement existant",
      },
      skip: {
        word: "Ignorer",
        label: "{0} le traitement t\u00e9l\u00e9vers\u00e9",
      },
    },
    button: {
      cancel: "Annuler",
      upload: "T\u00e9l\u00e9charger",
      uploadAnother:
        "T\u00e9l\u00e9verser un autre fichier de d\u00e9finition de traitement...",
    },
    title:
      "T\u00e9l\u00e9verser une d\u00e9finition de traitement dans le projet",
    fileSelectLabel:
      "S\u00e9lectionnez un fichier de d\u00e9finition de traitement.",
    formatRadioLabel: {
      xml: "Format XML",
      yaml: "Format YAML",
      json: "Format JSON",
    },
    uploadingFile: "T\u00e9l\u00e9versement du fichier...",
    jobs: "Traitement | Traitements",
    wasWere: {
      singular: " a \u00e9t\u00e9",
      plural: " ont \u00e9t\u00e9",
    },
    results: {
      error: {
        notProcessed: "non trait\u00e9(s) en raison d\u02bcerreurs",
        definitionErrors: "Erreurs de d\u00e9finition de traitement",
      },
      skipped: {
        skippedMessage:
          "ignor\u00e9(s) car des traitements portant le m\u00eame nom existent d\u00e9j\u00e0",
        existing: "Existant :",
      },
      success: {
        successMessage: "cr\u00e9\u00e9(s)/modifi\u00e9(s) avec succ\u00e8s",
      },
    },
  },
  sysConfigMenuHeader: "Syst\u00e8me",
  nodeSteps: "N\u0153ud \u00e9tapes",
  workflowSteps: "\u00c9tapes du flux de traitement",
  hideIncompleteNodes: "Masquer les n\u0153uds incomplets",
  hideIncompleteNodesDescription:
    "Masquer les n\u0153uds dont les seules \u00e9tapes inachev\u00e9es ont \u00e9t\u00e9 ignor\u00e9es par une condition. Ex\u00e9cutions r\u00e9ussies uniquement.",
  "user.list.title": "Utilisateurs",
  "user.list.edit": "modifier",
  Configuration: "Configuration",
  "None configured. Click NodeEnhancer to add a new plugin.":
    "Aucun configur\u00e9. Cliquez sur NodeEnhancer pour ajouter un nouveau plugin.",
  "Scans a directory and loads all resource document files":
    "Analyse un r\u00e9pertoire et charge tous les fichiers de documents de ressources",
  "Reads a file containing node definitions in a supported format":
    "Lit un fichier contenant des d\u00e9finitions de n\u0153uds dans un format pris en charge",
  "Provides the local node as the single resource":
    "Fournit le n\u0153ud local comme unique ressource",
  "Run a script to produce resource model data":
    "Ex\u00e9cute un script pour produire les donn\u00e9es du mod\u00e8le de ressources",
  "Retrieves a URL containing node definitions in a supported format":
    "R\u00e9cup\u00e8re une URL contenant des d\u00e9finitions de n\u0153uds dans un format pris en charge",
  "storage.keyPath.error.traversal":
    "La travers\u00e9e de r\u00e9pertoire \u00ab .. \u00bb n\u02bcest pas autoris\u00e9e dans les chemins du stockage de cl\u00e9s.",
  "storage.keyPath.error.leadingSpace":
    "Les composants du chemin ne peuvent pas commencer par un espace.",
  "storage.keyPath.error.invalidChar":
    "Le caract\u00e8re \u00ab {0} \u00bb n\u02bcest pas autoris\u00e9. Les chemins du stockage de cl\u00e9s ne peuvent contenir que : lettres, chiffres, espaces (pas au d\u00e9but), / . + _ - ,",
  "storage.keyPath.error.invalidFormat":
    "Format de chemin du stockage de cl\u00e9s non valide.",
  message_warning: "Attention !",
  message_fieldLabelHelp:
    "Facultatif. Affich\u00e9 comme nom de ce champ. Par d\u00e9faut, la cl\u00e9 du champ si laiss\u00e9 vide.",
  message_fieldKeyHelp:
    "La cl\u00e9 utilis\u00e9e pour identifier ce champ (par ex. \u00ab environment \u00bb). Doit \u00eatre unique dans cette liste.",
  message_fieldKeyDescription: "Cl\u00e9 du champ : {0}",
  message_fieldKeyOnlyDescription: "Cl\u00e9 du champ {0}",
  message_fieldKeyAppendedDescription: "{0} (Cl\u00e9 du champ : {1})",
  message_fieldDescriptionHelp:
    "Facultatif. D\u00e9crit ce champ pour votre propre r\u00e9f\u00e9rence ; si laiss\u00e9 vide, une description est g\u00e9n\u00e9r\u00e9e automatiquement \u00e0 partir de la cl\u00e9 du champ.",
  notifications: {
    helpText:
      "Les notifications peuvent \u00eatre d\u00e9clench\u00e9es par diff\u00e9rents \u00e9v\u00e9nements pendant l\u02bcex\u00e9cution du traitement.",
    emptyText:
      "Aucune notification n\u02bcest d\u00e9finie. Cliquez sur un \u00e9v\u00e9nement ci-dessous pour ajouter une notification pour ce d\u00e9clencheur.",
    addButton: "Ajouter une notification",
    triggerLabel: "D\u00e9clencheur",
    selectTrigger: "S\u00e9lectionnez un d\u00e9clencheur",
    typeLabel: "Type de notification",
    selectNotification: "S\u00e9lectionnez une notification",
  },
  message_webhookFilterListPlaceholder: "Filtrer les webhooks",
  message_webhookTabGeneral: "G\u00e9n\u00e9ral",
  message_webhookTabHandlerConfiguration: "Configuration du gestionnaire",
  message_webhookNewHookName: "Nouveau hook",
  "success.rate": "Taux de r\u00e9ussite",
  "Bulk Delete Executions: Results":
    "Suppression en masse des ex\u00e9cutions : r\u00e9sultats",
  noResultsFound: "Aucun r\u00e9sultat trouv\u00e9",
  noMatchesFound: "Aucune correspondance trouv\u00e9e",
  noMatchesFoundSecondary: "Veuillez essayer une autre recherche.",
  "project.activitySummary.userCount": "par {count} {users} :",
  "users.plural": "utilisateur|utilisateurs",
  "execution.action.saveAsJob": "Enregistrer comme traitement",
  "execution.status.aborted": "Abandonn\u00e9e",
  "kill.job": "Tuer le traitement",
  killing: "Arr\u00eat forc\u00e9 en cours...",
  "scheduledExecution.uuid.autogenerated.label":
    "G\u00e9n\u00e9r\u00e9 automatiquement \u00e0 l\u02bcenregistrement",
  "scheduledExecution.crontab.tab.simple": "Simple",
  "scheduledExecution.crontab.tab.crontab": "Crontab",
  "scheduledExecution.crontab.field.minute": "Minute",
  "scheduledExecution.crontab.everyDay": "Tous les jours",
  "scheduledExecution.crontab.everyMonth": "Tous les mois",
  "scheduledExecution.crontab.help.ranges": "Plages :",
  "scheduledExecution.crontab.help.lists": "Listes :",
  "scheduledExecution.crontab.help.increments": "Incr\u00e9ments :",
  "scheduledExecution.crontab.help.increments.description":
    "\u00ab toutes les 15 unit\u00e9s \u00e0 partir de 0 \u00bb.",
  "scheduledExecution.crontab.help.validDayOfWeek":
    "Valeurs valides du jour de la semaine : 1-7 ou SUN-SAT",
  "scheduledExecution.crontab.help.validMonth":
    "Valeurs valides du mois : 1-12 ou JAN-DEC",
  "scheduledExecution.crontab.help.see": "Voir :",
  "scheduledExecution.crontab.help.formatting":
    "pour l\u02bcaide sur le format",
  "Add Notification": "Ajouter une notification",
  "Also kill processes whose process SID matches the tracked PIDs":
    "Tuer aussi les processus dont le SID correspond aux PID suivis",
  Explain: "Expliquer",
  "Kill all processes collected by the 'Capture Process IDs' log filter":
    "Tuer tous les processus collect\u00e9s par le filtre de journal \u00ab Capture Process IDs \u00bb",
  "Kill processes only if job failed or is killed":
    "Tuer les processus uniquement si le traitement a \u00e9chou\u00e9 ou est tu\u00e9",
  "Kill spawned processes": "Tuer les processus lanc\u00e9s",
  "Kill tracked processes after execution":
    "Tuer les processus suivis apr\u00e8s l\u02bcex\u00e9cution",
  Modules: "Modules",
  "More...": "Plus...",
  "No Workflow steps": "Aucune \u00e9tape de workflow",
  "Node First": "N\u0153ud d\u02bcabord",
  "Node Sources for the project. Sources are loaded in the defined order, with later sources overriding earlier sources. (You can use  {'${project.name}'} inside configuration values to substitute the project name.)":
    "Sources de n\u0153uds du projet. Les sources sont charg\u00e9es dans l\u02bcordre d\u00e9fini, les sources suivantes rempla\u00e7ant les pr\u00e9c\u00e9dentes. (Vous pouvez utiliser  {'${project.name}'} dans les valeurs de configuration pour substituer le nom du projet.)",
  "None configured. Click ResourceModelSource to add a new plugin.":
    "Aucun configur\u00e9. Cliquez sur ResourceModelSource pour ajouter un nouveau plugin.",
  "Save as a Filter...": "Enregistrer comme filtre...",
  "This operation will use the 'kill' and 'pkill' for Unix and 'taskkill' for Windows commands. These commands must be available at the node.":
    "Cette op\u00e9ration utilisera les commandes \u00ab kill \u00bb et \u00ab pkill \u00bb pour Unix et \u00ab taskkill \u00bb pour Windows. Ces commandes doivent \u00eatre disponibles sur le n\u0153ud.",
  "Workflow.noSteps": "Aucune \u00e9tape de workflow",
  "Workflow.strategy.description.step-first":
    "Ex\u00e9cute chaque \u00e9tape sur tous les n\u0153uds avant de passer \u00e0 l\u02bc\u00e9tape suivante.",
  "Workflow.strategy.label.node-first": "N\u0153ud d\u02bcabord",
  "Workflow.strategy.label.step-first": "\u00c9tape d\u02bcabord",
  "delete.project.ellipsis": "Supprimer le projet...",
  "edit.readme.ellipsis": "Modifier le Readme...",
  "export.archive.ellipsis": "Exporter l\u02bcarchive...",
  "import.archive.ellipsis": "Importer une archive...",
  "no.nodes.selected.match.nodes.by.selecting.or.entering.a.filter":
    "Aucun n\u0153ud s\u00e9lectionn\u00e9. S\u00e9lectionnez des n\u0153uds en choisissant ou en saisissant un filtre",
  "none.configured.click.plugin.to.add.a.new.plugin":
    "Aucun configur\u00e9. Cliquez sur {0} pour ajouter un nouveau plugin.",
  "notifications.addButton": "Ajouter une notification",
  "notifications.emptyText":
    "Aucune notification n\u02bcest d\u00e9finie. Cliquez sur un \u00e9v\u00e9nement ci-dessous pour ajouter une notification pour ce d\u00e9clencheur.",
  "notifications.helpText":
    "Les notifications peuvent \u00eatre d\u00e9clench\u00e9es par diff\u00e9rents \u00e9v\u00e9nements pendant l\u02bcex\u00e9cution du traitement.",
  "scheduledExecution.property.doNodedispatch.description":
    "Choisissez d\u02bcex\u00e9cuter ce traitement sur les n\u0153uds filtr\u00e9s ou localement.",
  "scheduledExecution.property.excludeFilter.description":
    "Filtre secondaire pour exclure des n\u0153uds des r\u00e9sultats du filtre de n\u0153uds.",
  "step.type.jobreference.nodestep.description":
    "Ex\u00e9cute un traitement sur le n\u0153ud distant",
  description: "description",
  explain: "Expliquer",
  "adhoc.no.nodes.matched": "Aucun n\u0153ud",
  "adhoc.loading.output": "Chargement de la sortie\u2026",
  "adhoc.form.not.found": "Formulaire introuvable",
  "adhoc.server.response.invalid":
    "La r\u00e9ponse du serveur n\u02bc\u00e9tait pas valide",
  "adhoc.request.failed": "La requ\u00eate a \u00e9chou\u00e9",
  "adhoc.execution.output.load.failed":
    "\u00c9chec du chargement de la sortie de l\u02bcex\u00e9cution",
  "adhoc.execution.polling.timeout":
    "D\u00e9lai d\u02bcinterrogation de l\u02bcex\u00e9cution d\u00e9pass\u00e9 - l\u02bcex\u00e9cution est peut-\u00eatre bloqu\u00e9e sur le serveur",
  "view.in.nodes.page.prompt": "Afficher dans la page N\u0153uds",
  recent: "r\u00e9cent",
  "run.on.count.nodes": "Ex\u00e9cuter une commande sur {0} {1}",
  "on.node.failure": "En cas d\u02bc\u00e9chec du n\u0153ud",
  "page.section.Activity.for.adhoc.commands":
    "Activit\u00e9 des commandes ad hoc",
  "jobquery.title.optionFilter": "Options",
  "jobquery.title.optionFilter.label":
    "Options du traitement par paire, nom ou valeur (par ex. -sleep 10 ou -debug 1 -env prod ou -debug)",
  "jobquery.title.execnodeFilter": "Filtre de n\u0153uds d\u02bcex\u00e9cution",
  "scheduledExecution.action.activity.button.label":
    "Voir l\u02bcactivit\u00e9 de ce traitement",
  "job.execution.acl.invalid.warning.title":
    "Les ex\u00e9cutions planifi\u00e9es utilisent l\u02bcutilisateur enregistr\u00e9 \u00ab {user} \u00bb, qui n\u02bcest pas autoris\u00e9 \u00e0 ex\u00e9cuter ce traitement. La planification s\u02bcex\u00e9cute toujours, mais une version future pourrait appliquer cette v\u00e9rification. Accordez \u00e0 cet utilisateur le droit d\u02bcex\u00e9cution, ou enregistrez \u00e0 nouveau le traitement avec un utilisateur qui dispose de ce droit.",
  "job.execution.acl.invalid.blocked.warning.title":
    "Les ex\u00e9cutions planifi\u00e9es utilisent l\u02bcutilisateur enregistr\u00e9 \u00ab {user} \u00bb, qui n\u02bcest pas autoris\u00e9 \u00e0 ex\u00e9cuter ce traitement ; la planification est donc ignor\u00e9e. Accordez \u00e0 cet utilisateur le droit d\u02bcex\u00e9cution, ou enregistrez \u00e0 nouveau le traitement avec un utilisateur qui dispose de ce droit.",
  "settings.support.title": "Support",
  "settings.support.label": "Support",
  "settings.theme.title": "Th\u00e8me",
  "settings.theme.description":
    "S\u00e9lectionnez votre th\u00e8me visuel pr\u00e9f\u00e9r\u00e9 pour l\u02bcinterface.",
  "settings.theme.options.light": "Clair",
  "settings.theme.options.dark": "Sombre",
  "settings.tabs.theme": "Th\u00e8me",
  "settings.tabs.uiEarlyAccess":
    "Acc\u00e8s anticip\u00e9 \u00e0 l\u02bcinterface",
  "settings.nextUi.indicatorTitle": "Mode nouvelle interface",
  "settings.nextUi.available": "Nouvelle interface disponible",
  "settings.nextUi.enabled": "Nouvelle interface activ\u00e9e",
  "settings.uiEarlyAccess.title":
    "Acc\u00e8s anticip\u00e9 \u00e0 l\u02bcinterface",
  "settings.uiEarlyAccess.description":
    "Nous am\u00e9liorons progressivement l\u02bcinterface de Rundeck. Activez l\u02bcacc\u00e8s anticip\u00e9 ci-dessous pour essayer les nouvelles fonctionnalit\u00e9s d\u00e8s leur publication. Remarque : les fonctionnalit\u00e9s en acc\u00e8s anticip\u00e9 peuvent contenir des bogues ou \u00eatre incompl\u00e8tes. Nous vous encourageons vivement \u00e0 soumettre vos commentaires ou \u00e0 signaler les probl\u00e8mes via un ticket de support.",
  "settings.uiEarlyAccess.enableLabel": "Activer la nouvelle interface",
  "settings.uiEarlyAccess.feature1":
    "Lorsque cette option est activ\u00e9e, vous verrez des mises \u00e0 jour exp\u00e9rimentales de l\u02bcinterface dans tout le produit. Un petit indicateur en bas de la page s\u02bcaffichera lorsque vous consulterez une fonctionnalit\u00e9 en acc\u00e8s anticip\u00e9.",
  "settings.uiEarlyAccess.learnMore": "En savoir plus",
  "settings.uiEarlyAccess.feedbackLink":
    "Donner votre avis sur l\u02bcacc\u00e8s anticip\u00e9",
  "settings.uiEarlyAccess.toast.enabled":
    "Acc\u00e8s anticip\u00e9 \u00e0 la nouvelle interface activ\u00e9. Vous pouvez revenir en arri\u00e8re \u00e0 tout moment.",
  "settings.uiEarlyAccess.toast.disabled":
    "La nouvelle interface a \u00e9t\u00e9 d\u00e9sactiv\u00e9e. Vous pouvez revenir en arri\u00e8re \u00e0 tout moment.",
  Any: "Tous",
  "Any Time": "\u00c0 tout moment",
  "Other...": "Autre...",
  "Search Activity": "Rechercher dans l\u02bcactivit\u00e9",
  "workflow.all.steps": "Toutes les \u00e9tapes du workflow",
  "form.option.regex.description.md":
    "Saisissez une expression r\u00e9guli\u00e8re au format [Java Pattern](https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html).",
  "form.option.regex.multiline.description.md":
    "Pour les options de texte multiligne, utilisez `(?s)` pour faire correspondre toutes les lignes.",
  "pagination.of": "sur",
  "scheduledExecution.property.description.description":
    "La premi\u00e8re ligne de la description sera affich\u00e9e en texte brut, le reste sera rendu avec Markdown.\n\nVoir [Markdown](http://en.wikipedia.org/wiki/Markdown).\n\nDans la description \u00e9tendue, vous pouvez cr\u00e9er un lien vers le traitement en utilisant {'`{{job.permalink}}`'} comme URL du traitement, par ex. `[ex\u00e9cuter le traitement]({'{{job.permalink}}#runjob'})`\n\nVous pouvez ajouter un Readme en pla\u00e7ant un s\u00e9parateur horizontal `---` seul sur une ligne ; tout ce qui suit sera rendu dans un onglet distinct \u00e0 l\u02bcaide de [Markdeep](https://casual-effects.com/markdeep).",
  less: "Moins\u2026",
  "plugin.choose.title": "Choisir un plugin",
  "enter.a.step.filter.override": "Rechercher",
  "workflow.search.help.string1": "Recherche simple :",
  "workflow.search.help.string2": "mystep1",
  "workflow.search.help.string3":
    "Affiche les \u00e9tapes dont le titre, la description ou le nom contient \u00ab mystep1 \u00bb.",
  "workflow.search.help.string4":
    "Filtrer en faisant correspondre un champ pr\u00e9cis :",
  "workflow.search.help.string5": "description :",
  "workflow.search.help.string6": "description=value",
  "workflow.search.help.string7": "name :",
  "workflow.search.help.string8": "name=value",
  "workflow.search.help.string9": "title :",
  "workflow.search.help.string10": "title=value",
  "workflow.search.help.string11":
    "Filtrer en faisant correspondre la valeur d\u02bcune propri\u00e9t\u00e9 d\u02bcentr\u00e9e :",
  "workflow.search.help.string12": "description de la propri\u00e9t\u00e9 :",
  "workflow.search.help.string13": "property:description=value",
  "workflow.search.help.string14": "titre de la propri\u00e9t\u00e9 :",
  "workflow.search.help.string15": "property:title=value",
  "workflow.search.help.string16": "nom de la propri\u00e9t\u00e9 :",
  "workflow.search.help.string17": "property:name=value",
  "node.step.plugin.plural": "{0} plugins d\u02bc\u00e9tape de n\u0153ud",
  "workflow.step.plugin.plural": "{0} plugins d\u02bc\u00e9tape de workflow",
  "choose.a.job...": "Choisir un traitement\u2026",
  "Workflow.Step.jobreference.jobName.help":
    "S\u00e9lectionnez un traitement existant \u00e0 l\u02bcaide du s\u00e9lecteur de projet et du bouton \u00ab Choisir un traitement... \u00bb ci-dessus, ou en saisissant le nom et/ou le groupe du traitement dans les champs ci-dessous.",
  "plugin.edit.title": "Modifier l\u02bc\u00e9tape",
  "Workflow.step.property.description.label": "Nom de l\u02bc\u00e9tape",
  "Workflow.step.property.description.help": "Nom de cette \u00e9tape",
  "notification.create": "Cr\u00e9er une notification",
  "notification.edit": "Modifier la notification",
  Search: "Rechercher",
  "commandExec.jobName.blank.message":
    "Le traitement doit \u00eatre sp\u00e9cifi\u00e9",
  "Workflow.stepErrorHandler.label.on.error": "en cas d\u02bcerreur",
  "Workflow.validation.oneError": "1 erreur",
  "Workflow.validation.multipleErrors": "{count} erreurs",
  "archive.import.validateJobRef.label":
    "Valider les traitements r\u00e9f\u00e9renc\u00e9s :",
  "archive.import.uuidOption.label": "Traitements import\u00e9s :",
  userMenuGreeting: "Bonjour {0} !",
  unknownUser: "(Utilisateur inconnu)",
  searchForStep: "Rechercher une \u00e9tape",
  learnMoreSearchPatterns:
    "pour effectuer une recherche avec des motifs pr\u00e9cis",
  nodeStepsDescription:
    "Les \u00e9tapes de n\u0153ud s\u02bcex\u00e9cutent une fois pour chaque n\u0153ud cibl\u00e9 par le filtre de n\u0153uds du traitement.",
  workflowStepsDescription:
    "Les \u00e9tapes de workflow s\u02bcex\u00e9cutent une fois par ex\u00e9cution du traitement.",
  commonNodeSteps: "\u00c9tapes de n\u0153ud courantes",
  commonWorkflowSteps: "\u00c9tapes de workflow courantes",
  learnMore: "En savoir plus",
  plugins: "plugins",
  "markdown.help.link.label": "Aide sur la syntaxe Markdown",
  "job.tree.browse.group.label": "Parcourir le groupe de traitements : {0}",
  "repository.plugin.sourceLink.label": "Code source du plugin",
  "repository.plugin.pageLink.label": "Page du plugin",
  "repository.plugin.docsLink.label": "Documentation du plugin",
  "storage.path.link.label": "Lien vers le chemin de stockage",
  "user.list.newProfile": "Nouveau profil \u2026",
  "user.list.toggleDetails":
    "Afficher/masquer les d\u00e9tails de l\u02bcutilisateur {0}",
};

export default messages;
