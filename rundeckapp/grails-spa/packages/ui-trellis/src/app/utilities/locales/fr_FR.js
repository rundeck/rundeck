const messages = {
  Edit: "Modifier",
  Save: "Enregistrer",
  Delete: "Supprimer",
  Cancel: "Annuler",
  Revert: "Retour arrière",
  jobAverageDurationPlaceholder:
    "laisser vide pour la durée moyenne du travail",
  resourcesEditor: {
    "Dispatch to Nodes": "Envoyer vers les nœuds",
    Nodes: "Nœuds",
  },
  cron: {
    section: {
      0: "Secondes",
      1: "Minutes",
      2: "Heures",
      3: "Jour du mois",
      4: "Mois",
      5: "Jour de la semaine",
      6: "Année",
    },
  },
  message_communityNews: "Nouvelles de la communauté",
  message_connectionError:
    "Il semble qu'une erreur se soit produite lors de la connexion à Community News.",
  message_readMore: "En savoir plus",
  message_refresh: "Veuillez actualiser la page ou nous rendre visite à",
  message_subscribe: "S'abonner",
  message_delete: "Supprimer ce champ",
  message_duplicated: "Le champ existe déjà",
  message_select: "Sélectionner un champ",
  message_description: "Description",
  message_fieldLabel: "Étiquette du champ",
  message_fieldKey: "Clé du champ",
  message_fieldFilter: "Écrire pour filtrer un champ",
  message_empty: "Peut être vide",
  message_cancel: "Annuler",
  message_add: "Ajouter",
  message_addField: "Ajouter un champ personnalisé",
  message_pageUsersSummary: "Liste des utilisateurs Rundeck.",
  message_pageUsersLoginLabel: "Nom d'utilisateur",
  message_pageUsersCreatedLabel: "Créé",
  message_pageUsersUpdatedLabel: "Mis à jour",
  message_pageUsersLastjobLabel: "Dernière exécution de travail",
  message_domainUserFirstNameLabel: "Prénom",
  message_domainUserLastNameLabel: "Nom",
  message_domainUserEmailLabel: "Email",
  message_domainUserLabel: "Utilisateur",
  message_pageUsersTokensLabel: "Nº Jetons",
  message_pageUsersTokensHelp:
    "Vous pouvez administrer les jetons depuis la page de profil utilisateur.",
  message_pageUsersLoggedStatus: "Statut",
  message_pageUserLoggedOnly: "Utilisateurs connectés uniquement",
  message_pageUserNotSet: "Non défini",
  message_pageUserNone: "Aucun",
  message_pageFilterLogin: "Login",
  message_pageFilterHostName: "Nom d'hôte",
  message_pageFilterSessionID: "ID de session",
  message_pageFilterBtnSearch: "Rechercher",
  message_pageUsersSessionIDLabel: "ID de session",
  message_pageUsersHostNameLabel: "Nom d'hôte",
  message_pageUsersLastLoginInTimeLabel: "Dernière connexion",
  message_pageUsersTotalFounds: "Nombre total d'utilisateurs trouvés",
  message_paramIncludeExecTitle: "Afficher la dernière exécution",
  message_loginStatus: {
    "LOGGED IN": "Connecté",
    "NOT LOGGED": "Jamais",
    ABANDONED: "Expiré",
    "LOGGED OUT": "Déconnecté",
  },
  message_userSummary: {
    desc: "Ceci est une liste des profils d'utilisateurs qui se sont connectés à Rundeck.",
  },
  message_webhookPageTitle: "Webhooks",
  message_webhookListTitle: "Webhooks",
  message_webhookDetailTitle: "Détail du webhook",
  message_webhookListNameHdr: "Nom",
  message_addWebhookBtn: "Ajouter",
  message_webhookEnabledLabel: "Activé",
  message_webhookPluginCfgTitle: "Configuration du plugin",
  message_webhookSaveBtn: "Enregistrer",
  message_webhookCreateBtn: "Créer le webhook",
  message_webhookDeleteBtn: "Supprimer",
  message_webhookPostUrlLabel: "URL Post",
  message_webhookPostUrlHelp:
    "Lorsqu'une requête HTTP POST vers cette URL est reçue, le plugin webhook choisi ci-dessous recevra les données.",
  message_webhookPostUrlPlaceholder:
    "L'URL sera générée après la création du Webhook",
  message_webhookNameLabel: "Nom",
  message_webhookUserLabel: "Utilisateur",
  message_webhookUserHelp:
    "Nom d'utilisateur d'autorisation utilisé lors de l'exécution de ce webhook. Toutes les politiques ACL correspondant à ce nom d'utilisateur s'appliqueront.",
  message_webhookRolesLabel: "Rôles",
  message_webhookRolesHelp:
    "Les rôles d'autorisation assumés lors de l'exécution de ce webhook (séparés par des virgules). Toutes les politiques ACL correspondant à ces rôles s’appliqueront.",
  message_webhookAuthLabel: "Chaîne d'autorisation HTTP",
  message_webhookGenerateSecurityLabel: "Utiliser l'en-tête d'autorisation",
  message_webhookGenerateSecretCheckboxHelp:
    "[Optionnel] Une chaîne d'autorisation Webhook peut être générée pour augmenter la sécurité de ce webhook. Tous les messages devront inclure la chaîne générée dans l'en-tête Autorisation.",
  message_webhookSecretMessageHelp:
    "Copiez cette chaîne d'autorisation maintenant. Après avoir quitté ce webhook, vous ne pourrez plus voir la chaîne.",
  message_webhookRegenClicked:
    "Une nouvelle chaîne d'autorisation sera générée et affichée lorsque le webhook sera enregistré.",
  message_webhookPluginLabel: "Choisir le plugin webhook",
  message_hello: "bonjour le monde",
  message_sidebarNotificationText: "Mise à jour de Rundeck disponible",
  message_updateAvailable: "Mise à jour disponible",
  message_updateHasBeenReleased: "Une mise à jour de Rundeck a été publiée.",
  message_installedVersion: "La version installée de Rundeck est",
  message_currentVersion: "La version la plus récente de Rundeck est",
  message_getUpdate: "Obtenir la mise à jour",
  message_dismissMessage:
    "Pour ignorer cette notification jusqu'à la prochaine version, veuillez cliquer ici.",
  message_close: "Fermer",
  "bulk.edit": "Modification de masse",
  "in.of": "dans",
  execution: "Exécution | Exécutions",
  "execution.count": "1 Exécution | {0} Exécutions",
  "Bulk Delete Executions: Results":
    "Suppression en masse des exécutions: Résultats",
  "Requesting bulk delete, please wait.":
    "Demande de suppression en masse, veuillez patienter.",
  "bulkresult.attempted.text": "{0} exécutions ont été tentées.",
  "bulkresult.success.text": "{0} exécutions ont été supprimées avec succès.",
  "bulkresult.failed.text": "{0} exécutions n'ont pas pu être supprimées:",
  "delete.confirm.text": "Vraiment supprimer {0} {1}?",
  "clearselected.confirm.text":
    "Effacer tous les {0} éléments sélectionnés ou uniquement les éléments affichés sur cette page ?",
  "bulk.selected.count": "{0} sélectionnés",
  "results.empty.text": "Pas de résultats pour cette requête",
  "Only shown executions": "Uniquement les exécutions affichées",
  "Clear bulk selection": "Décocher la sélection en masse",
  "Click to edit Search Query": "Cliquez pour modifier la requête de recherche",
  "Auto refresh": "Actualisation automatique",
  "error.message.0": "Une erreur est survenue: {0}",
  "info.completed.0": "Terminé: {0}",
  "info.completed.0.1": "Terminé: {0} {1}",
  "info.missed.0.1": "Manqué: {0} {1}",
  "info.started.0": "Démarré: {0}",
  "info.started.expected.0.1": "Démarré: {0}, Fin estimée: {1}",
  "info.scheduled.0": "Planifié; démarrage {0}",
  "job.execution.starting.0": "Démarrage {0}",
  "job.execution.queued": "En file d'attente",
  "info.newexecutions.since.0":
    "1 nouveau résultat. Cliquez pour charger. | {0} Nouveau(x) résultat(s). Cliquez pour charger.",
  "In the last Day": "Au cours des dernières 24 heures",
  Referenced: "Référencé",
  "job.has.been.deleted.0": "(Le travail {0} a été supprimé)",
  Filters: "Filtres",
  "filter.delete.named.text": 'Supprimer le filtre "{0}"...',
  "Delete Saved Filter": "Supprimer le filtre enregistré",
  "filter.delete.confirm.text":
    'Êtes-vous sûr de vouloir supprimer le filtre enregistré nommé "{0}" ?',
  "filter.save.name.prompt": "Nom:",
  "filter.save.validation.name.blank": "Le nom ne peut pas être vide",
  "filter.save.button": "Enregistrer le filtre...",
  "saved.filters": "Filtres enregistrés",
  failed: "échoué",
  ok: "ok",
  "0.total": "{0} total",
  period: {
    label: {
      All: "n'importe quand",
      Hour: "au cours de la dernière heure",
      Day: "au cours du dernier jour",
      Week: "au cours de la dernière semaine",
      Month: "au cours du dernier mois",
    },
  },
  "empty.message.default":
    "Aucune configuration trouvée. Cliquez sur {0} pour ajouter un nouveau plugin.",
  CreateAcl: "Créer l'ACL",
  CreateAclName: "Description de l'ACL",
  CreateAclTitle: "Créer une ACL de stockage de clés pour le projet",
  "Edit Nodes": "Modifier les nœuds",
  Modify: "Modifier",
  "Edit Node Sources": "Modifier les sources de nœud",
  "The Node Source had an error": "La source de nœud a rencontré une erreur",
  "Validation errors": "Erreurs de validation",
  "unauthorized.status.help.1":
    "Certaines sources de nœud ont renvoyé un message « Non autorisé ».",
  "unauthorized.status.help.2":
    "Le plugin de source de nœud peut avoir besoin d'accéder à la ressource de stockage de clés. Il pourrait être activé par les entrées de la politique de contrôle d'accès.",
  "unauthorized.status.help.3":
    "Assurez-vous que les stratégies ACL autorisent l'accès en « lecture » au stockage de clés dans ce projet pour le chemin URN du projet (urn:project:name).",
  "unauthorized.status.help.4": "Aller à {0} pour créer une ACL de projet",
  "unauthorized.status.help.5": "Aller à {0} pour créer une ACL système",
  "acl.config.link.title": "Paramètres du projet > Contrôle d'accès",
  "acl.config.system.link.title": "Paramètres système > Contrôle d'accès",
  "acl.example.summary": "Exemple de stratégie ACL",
  "page.keyStorage.description":
    "Le stockage de clés fournit une structure globale de type répertoire pour enregistrer les clés et mots de passe publics et privés, à utiliser avec l'authentification des nœuds d'exécution.",
  Duplicate: "Dupliquer",
  "Node.count.vue": "Nœud | Nœuds",
  "bulk.delete": "Suppression en masse",
  "select.none": "Ne rien sélectionner",
  "select.all": "Tout sélectionner",
  "cancel.bulk.delete": "Annuler la suppression en masse",
  "delete.selected.executions": "Supprimer les exécutions sélectionnées",
  "click.to.refresh": "cliquez pour actualiser",
  "count.nodes.matched": "{0} {1} Correspondant",
  "count.nodes.shown": "{0} nœuds affichés.",
  "delete.this.filter.confirm": "Vraiment supprimer ce filtre ?",
  "enter.a.node.filter": "Entrez un filtre de nœud, ou .* Pour tous les nœuds",
  "execute.locally": "Exécuter localement",
  "execution.page.show.tab.Nodes.title": "Nœuds",
  "execution.show.mode.Log.title": "Sortie de journal",
  filter: "Filtre :",
  "name.prompt": "Nom :",
  "loading.matched.nodes": "Chargement des nœuds correspondants...",
  "loading.text": "Chargement...",
  "loglevel.debug": "Déboguer",
  "loglevel.normal": "Standard",
  "matched.nodes.prompt": "Nœuds correspondants",
  no: "Non",
  "node.access.not-runnable.message":
    "Vous n'avez pas accès à l'exécution de commandes sur ce nœud.",
  "node.filter": "Filtre de nœud",
  "node.filter.exclude": "Exclure le filtre",
  "node.metadata.os": "Système d'exploitation",
  "node.metadata.status": "Statut",
  nodes: "Nœuds :",
  "notification.event.onfailure": "En cas d'échec",
  "notification.event.onsuccess": "En cas de succès",
  "notification.event.onstart": "Au démarrage",
  "notification.event.onavgduration": "Durée moyenne dépassée",
  "notification.event.onretryablefailure": "En cas d'échec réessayable",
  refresh: "rafraîchir",
  "save.filter.ellipsis": "Enregistrer le filtre …",
  "search.ellipsis": "Rechercher…",
  "scheduledExecution.property.defaultTab.label": "Onglet par défaut",
  "scheduledExecution.property.defaultTab.description":
    "L'onglet par défaut à afficher lorsque vous suivez une exécution.",
  "scheduledExecution.property.excludeFilterUncheck.label":
    "Montrer les nœuds exclus",
  "scheduledExecution.property.excludeFilterUncheck.description":
    "Si vrai, les nœuds exclus seront indiqués lors de l'exécution du Job. Sinon, ils ne seront pas affichés du tout.",
  "scheduledExecution.property.logOutputThreshold.label":
    "Limite de sortie du journal",
  "scheduledExecution.property.logOutputThreshold.description":
    'Entrez soit le nombre de lignes total maximum (par exemple "100"), le nombre maximum de lignes par nœud ("100 / nœud") ou la taille maximale du fichier journal ("100MB", "100KB", etc.), en utilisant "GB", "MB", "KB", "B" comme Giga- Mega-Kilo et octets.',
  "scheduledExecution.property.logOutputThreshold.placeholder":
    'Par exemple, "100", "100 / nœud" ou "100MB"',
  "scheduledExecution.property.logOutputThresholdAction.label":
    "Action de limite de journal",
  "scheduledExecution.property.logOutputThresholdAction.description":
    "Action à effectuer si la limite de sortie est atteinte.",
  "scheduledExecution.property.logOutputThresholdAction.halt.label":
    "Arrêter avec le statut:",
  "scheduledExecution.property.logOutputThresholdAction.truncate.label":
    "Tronquer et continuer",
  "scheduledExecution.property.logOutputThresholdStatus.placeholder":
    "'échoué', 'avorté', ou n'importe quelle chaîne",
  "scheduledExecution.property.loglevel.help":
    "Le niveau de débogage produit plus de sortie",
  "scheduledExecution.property.maxMultipleExecutions.label":
    "Limiter exécutions multiples?",
  "scheduledExecution.property.maxMultipleExecutions.description":
    "Nombre maximal d'exécutions multiples. Utilisez vide ou 0 pour indiquer illimité.",
  "scheduledExecution.property.multipleExecutions.description":
    "Autoriser ce travail à être exécuté plus d'une fois simultanément ?",
  "scheduledExecution.property.nodeKeepgoing.prompt": "Si un nœud échoue",
  "scheduledExecution.property.nodeKeepgoing.true.description":
    "Continuez l'exécution sur tous les nœuds restants avant de faire échouer l'étape.",
  "scheduledExecution.property.nodeKeepgoing.false.description":
    "Faire échouer l'étape sans continuer d'exécuter sur les nœuds restants.",
  "scheduledExecution.property.nodeRankAttribute.label":
    "Attribut de classement",
  "scheduledExecution.property.nodeRankAttribute.description":
    "Attribut des nœuds utilisé pour le tri. La valeur par défaut est le nom du nœud.",
  "scheduledExecution.property.nodeRankOrder.label": "Ordre de classement",
  "scheduledExecution.property.nodeRankOrder.ascending.label": "Croissant",
  "scheduledExecution.property.nodeRankOrder.descending.label": "Décroissant",
  "scheduledExecution.property.nodeThreadcount.label": "Nombre de threads",
  "scheduledExecution.property.nodeThreadcount.description":
    "Nombre maximal de threads parallèles à utiliser. (Par défaut : 1)",
  "scheduledExecution.property.nodefiltereditable.label": "Filtre modifiable",
  "scheduledExecution.property.nodesSelectedByDefault.label":
    "Sélection de nœud",
  "scheduledExecution.property.nodesSelectedByDefault.true.description":
    "Les nœuds cibles sont sélectionnés par défaut",
  "scheduledExecution.property.nodesSelectedByDefault.false.description":
    "L'utilisateur doit sélectionner explicitement les nœuds cibles",
  "scheduledExecution.property.notifyAvgDurationThreshold.label": "Seuil",
  "scheduledExecution.property.notifyAvgDurationThreshold.description":
    "Ajoutez ou définissez une valeur de seuil à la durée moyenne pour déclencher cette notification. Options : - pourcentage => ex .: 20% - temps delta => ex .: + 20s, +20 - temps absolu => 30s, 5m Temps en secondes si vous ne spécifiez pas d'unités de temps Peut inclure des références de valeur d'option comme {'$'}{'{'}option{'.'}avgDurationThreshold{'}'}.",
  "scheduledExecution.property.orchestrator.label": "Orchestrateur",
  "scheduledExecution.property.orchestrator.description":
    "Il peut être utilisé pour contrôler l'ordre et le timing dans lequel les nœuds sont traités",
  "scheduledExecution.property.retry.description":
    "Nombre maximal de tentatives de ré-exécution lorsque ce travail est directement appelé. Une nouvelle tentative se produira si le travail échoue ou expire, mais pas s'il est tué manuellement. Peut utiliser une référence de valeur d'option comme \"{'$'}{'{'}option{'.'}retry{'}'}\".",
  "scheduledExecution.property.retry.delay.description":
    "Le délai entre l'exécution échouée et la nouvelle tentative. Temps en secondes, ou spécifier les unités de temps: \"120m\", \"2h\", \"3d\". Utilisez vide ou 0 pour indiquer aucun délai. Peut inclure des références de valeur d'option telles que \"{'$'}{'{'}option{'.'}delay{'}'}\".",
  "scheduledExecution.property.successOnEmptyNodeFilter.prompt":
    "Si le nœud est vide",
  "scheduledExecution.property.successOnEmptyNodeFilter.true.description":
    "Poursuivre l'exécution",
  "scheduledExecution.property.successOnEmptyNodeFilter.false.description":
    "Échec du travail",
  "scheduledExecution.property.timeout.description":
    "La durée maximale d'exécution d'une exécution. Temps en secondes, ou spécifier les unités de temps : \"120m\", \"2h\", \"3d\". Utilisez vide ou 0 pour n'indiquer aucun délai. Peut inclure des références de valeur d'option telles que \"{'$'}{'{'}option{'.'}timeout{'}'}\".",
  "scheduledExecution.property.scheduleEnabled.description":
    "Autoriser ce travail à être planifié ?",
  "scheduledExecution.property.scheduleEnabled.label":
    "Activer la planification ?",
  "scheduledExecution.property.executionEnabled.description":
    "Autoriser l'exécution de ce travail ?",
  "scheduledExecution.property.executionEnabled.label": "Activer l'exécution ?",
  "scheduledExecution.property.timezone.prompt": "Fuseau horaire",
  "scheduledExecution.property.timezone.description":
    'Un fuseau horaire valide, soit une abréviation telle que "PST", un nom complet tel que "America / Los_Angeles", ou un identifiant personnalisé tel que "GMT-8{\':\'} 00".',
  "documentation.reference.cron.url":
    "https{':'}//www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html",
  "set.as.default.filter": "Définir comme filtre par défaut",
  "show.all.nodes": "Afficher tous les nœuds",
  yes: "Oui",
  // job query field labels
  "jobquery.title.titleFilter": "Commande Adhoc",
  "jobquery.title.contextFilter": "Contexte",
  "jobquery.title.actionFilter": "Action",
  "jobquery.title.maprefUriFilter": "URI de ressource",
  "jobquery.title.reportIdFilter": "Nom",
  "jobquery.title.tagsFilter": "Tags",
  "jobquery.title.nodeFilter": "Nœud",
  "jobquery.title.nodeFilter.plural": "Nœuds",
  "jobquery.title.messageFilter": "Message",
  "jobquery.title.reportKindFilter": "Type de rapport",
  "jobquery.title.recentFilter": "Dans",
  "jobquery.title.actionTypeFilter": "Action",
  "jobquery.title.itemTypeFilter": "Type d'élément",
  "jobquery.title.filter": "Filtre",
  "jobquery.title.jobFilter": "Nom du travail",
  "jobquery.title.idlist": "ID du travail",
  "jobquery.title.jobIdFilter": "ID du travail",
  "jobquery.title.descFilter": "Description du travail",
  "jobquery.title.objFilter": "Ressource",
  "jobquery.title.scheduledFilter": "Planifié",
  "jobquery.title.serverNodeUUIDFilter": "UUID du nœud serveur",
  "jobquery.title.typeFilter": "Type",
  "jobquery.title.cmdFilter": "Commande",
  "jobquery.title.userFilter": "Utilisateur",
  "jobquery.title.projFilter": "Projet",
  "jobquery.title.statFilter": "Résultat",
  "jobquery.title.startFilter": "Heure de début",
  "jobquery.title.startbeforeFilter": "Commence avant",
  "jobquery.title.startafterFilter": "Commence après",
  "jobquery.title.endbeforeFilter": "Se termine avant",
  "jobquery.title.endafterFilter": "Se termine après",
  "jobquery.title.endFilter": "Temps",
  "jobquery.title.durationFilter": "Durée",
  "jobquery.title.outFilter": "Sortie",
  "jobquery.title.objinfFilter": "Informations sur la ressource",
  "jobquery.title.cmdinfFilter": "Informations sur la commande",
  "jobquery.title.groupPath": "Groupe",
  "jobquery.title.summary": "Résumé",
  "jobquery.title.duration": "Durée",
  "jobquery.title.loglevelFilter": "Niveau de log",
  "jobquery.title.loglevelFilter.label.DEBUG": "Déboguer",
  "jobquery.title.loglevelFilter.label.VERBOSE": "Verbeux",
  "jobquery.title.loglevelFilter.label.INFO": "Information",
  "jobquery.title.loglevelFilter.label.WARN": "Avertissement",
  "jobquery.title.loglevelFilter.label.ERR": "Erreur",
  "jobquery.title.adhocExecutionFilter": "Type de travail",
  "jobquery.title.adhocExecutionFilter.label.true": "Commande",
  "jobquery.title.adhocExecutionFilter.label.false": "Commande définie",
  "jobquery.title.adhocLocalStringFilter": "Contenu du script",
  "jobquery.title.adhocRemoteStringFilter": "Commande Shell",
  "jobquery.title.adhocFilepathFilter": "Chemin du fichier script",
  "jobquery.title.argStringFilter": "Arguments du fichier script",
  "page.unsaved.changes": "Vous avez des modifications non enregistrées",
  "edit.nodes.file": "Éditer le fichier de nœuds",
  "project.node.file.source.label": "Source",
  "file.display.format.label": "Format",
  "project.node.file.source.description.label": "Description",
  "project.nodes.edit.save.error.message":
    "Erreur lors de l'enregistrement du contenu :",
  "project.nodes.edit.empty.description":
    "Remarque : Aucun contenu n'était disponible.",
  "button.action.Cancel": "Annuler",
  "button.action.Save": "Enregistrer",
  "job-edit-page": {
    "nodes-tab-title": "Nœuds & Runners",
    "node-dispatch-true-label": "Envoyer vers les nœuds via Runner",
    "node-dispatch-false-label": "Exécuter sur Runner",
    "section-title": "Envoi",
    "section-title-help": "Choisissez le Runner et ses nœuds sélectionnés",
  },
  "job-exec-page": {
    "nodes-tab-title": "Runner/Nœuds",
  },
  JobRunnerEdit: {
    section: {
      title: "Ensemble de Runners",
    },
  },
  gui: {
    menu: {
      Nodes: "Nœuds",
    },
  },
  search: "Rechercher",
  browse: "Parcourir",
  result: "Résultat :",
  actions: "Actions",
  none: "Aucun",
  set: {
    "all.nodes.as.default.filter":
      "Définir tous les nœuds comme filtre par défaut",
    "as.default.filter": "Définir comme filtre par défaut",
  },
  remove: {
    "all.nodes.as.default.filter":
      "Supprimer tous les nœuds comme filtre par défaut",
    "default.filter": "Supprimer le filtre par défaut",
  },
  "run.a.command.on.count.nodes.ellipsis": "Exécuter une commande sur {0} {1}",
  "create.a.job.for.count.nodes.ellipsis": "Créer un travail pour {0} {1}",
  "resource.metadata.entity.tags": "Tags",
  filters: "Filtres",
  "all.nodes": "Tous les nœuds",
  "delete.this.filter.ellipsis": "Supprimer ce filtre ...",
  "enter.a.filter": "Entrez un filtre",
  "remove.all.nodes.as.default.filter":
    "Supprimer tous les nœuds comme filtre par défaut",
  "set.all.nodes.as.default.filter":
    "Définir tous les nœuds comme filtre par défaut",
  "not.authorized": "Non autorisé",
  "disabled.execution.run": "Les exécutions sont désactivées.",
  "user.at.host": "Utilisateur {'@'} Hôte",
  "node.changes.success":
    "Les modifications du nœud ont été enregistrées avec succès.",
  "node.changes.notsaved":
    "Les modifications du nœud n'ont pas été enregistrées.",
  "node.remoteEdit.edit": "Éditer le nœud :",
  "node.remoteEdit.continue": "Continuer...",
  node: "Nœud",
  "this.will.select.both.nodes": "Cela sélectionnera les deux nœuds.",
  "node.metadata.hostname": "Nom d'hôte",
  "select.nodes.by.name": "Sélectionner les nœuds par nom",
  "filter.nodes.by.attribute.value": "Filtrer les nœuds par valeur d'attribut",
  "use.regular.expressions": "Utiliser des expressions régulières :",
  "regex.syntax.checking": "Vérification de la syntaxe regex",
  "edit.ellipsis": "Modifier...",
  "node.metadata.username-at-hostname": "Utilisateur & Hôte",
  "node.metadata.osFamily": "Famille du système d'exploitation",
  "node.metadata.osName": "Nom du système d'exploitation",
  "node.metadata.osArch": "Architecture du système d'exploitation",
  "node.metadata.osVersion": "Version du système d'exploitation",
  "node.metadata.type": "Type",
  "node.metadata.username": "Nom d'utilisateur",
  "node.metadata.tags": "Tags",
  "button.Edit.label": "Modifier",
  "default.paginate.prev": "-",
  "default.paginate.next": "+",
  "jump.to": "Aller à",
  "per.page": "Par page",
  "remove.default.filter": "Supprimer le filtre par défaut",
  "scheduledExecution.action.edit.button.label": "Modifier ce travail…",
  "scheduledExecution.action.duplicate.button.label": "Dupliquer ce travail…",
  "scheduledExecution.action.duplicate.other.button.label":
    "Dupliquer ce travail vers un autre projet…",
  "scheduledExecution.action.download.button.label":
    "Télécharger la définition",
  "scheduledExecution.action.downloadformat.button.label":
    "Télécharger la définition du travail en {0}",
  "scheduledExecution.action.delete.button.label": "Supprimer ce travail",
  "scheduledExecution.action.edit.button.tooltip": "Modifier ce travail",
  "scheduledExecution.action.duplicate.button.tooltip": "Dupliquer le travail",
  "enable.schedule.this.job": "Activer la planification",
  "disable.schedule.this.job": "Désactiver la planification",
  "scheduledExecution.action.enable.schedule.button.label":
    "Activer la planification",
  "scheduledExecution.action.disable.schedule.button.label":
    "Désactiver la planification",
  "scheduleExecution.schedule.disabled":
    "La planification du travail est désactivée",
  "enable.execution.this.job": "Activer l'exécution",
  "disable.execution.this.job": "Désactiver l'exécution",
  "scheduledExecution.action.enable.execution.button.label":
    "Activer l'exécution",
  "scheduledExecution.action.disable.execution.button.label":
    "Désactiver l'exécution",
  "scheduleExecution.execution.disabled":
    "L'exécution du travail est désactivée",
  "delete.this.job": "Supprimer ce travail",
  "action.prepareAndRun.tooltip": "Choisir des options et exécuter le travail…",
  "job.bulk.modify.confirm.panel.title":
    "Confirmer la modification en masse du travail",
  "job.bulk.delete.confirm.message":
    "Vraiment supprimer les travaux sélectionnés ?",
  "job.bulk.disable_schedule.confirm.message":
    "Désactiver les planifications pour tous les travaux sélectionnés ?",
  "job.bulk.enable_schedule.confirm.message":
    "Activer les planifications pour tous les travaux sélectionnés ?",
  "job.bulk.disable_execution.confirm.message":
    "Désactiver l'exécution pour tous les travaux sélectionnés ?",
  "job.bulk.enable_execution.confirm.message":
    "Activer l'exécution pour tous les travaux sélectionnés ?",
  "job.bulk.disable_schedule.button": "Désactiver les planifications",
  "job.bulk.enable_schedule.button": "Activer les planifications",
  "job.bulk.delete.button": "Supprimer les travaux",
  "job.bulk.disable_execution.button": "Désactiver l'exécution",
  "job.bulk.enable_execution.button": "Activer l'exécution",
  "job.bulk.enable_execution.success": "Exécution activée pour {0} travaux.",
  "job.bulk.enable_schedule.success": "Planification activée pour {0} travaux.",
  "job.bulk.disable_schedule.success":
    "Planification désactivée pour {0} travaux.",
  "job.bulk.disable_execution.success":
    "Exécution désactivée pour {0} travaux.",
  "job.bulk.delete.success": "Supprimé {0} travaux.",
  "delete.selected.jobs": "Supprimer les travaux sélectionnés",
  "job.bulk.panel.select.title":
    "Sélectionner des travaux pour modification en masse",
  "job.bulk.perform.action.menu.label": "Exécuter l'action",
  "job.create.button": "Créer un nouveau travail",
  "job.upload.button.title": "Télécharger une définition de travail",
  cancel: "Annuler",
  "job.actions": "Actions de travail",
  "job.bulk.activate.menu.label": "Modification en masse…",
  "job.bulk.deactivate.menu.label": "Quitter le mode de modification en masse",
  "upload.definition.button.label": "Télécharger la définition",
  "new.job.button.label": "Nouveau travail",
  "job.bulk.panel.select.message":
    "{n} Travail sélectionné | {n} Travaux sélectionnés",
  "cannot.run.job": "Impossible d'exécuter le travail",
  "disabled.schedule.run": "Les exécutions sont désactivées.",
  "disabled.job.run": "Les exécutions sont désactivées.",
  "schedule.on.server.x.at.y":
    "Planifié pour exécuter sur le serveur {0} à {1}",
  "schedule.time.in.future": "dans {0}",
  never: "Jamais",
  disabled: "Désactivé",
  "project.schedule.disabled": "La planification du projet est désactivée",
  "project.execution.disabled": "L'exécution du projet est désactivée",
  "job.schedule.will.never.fire":
    "La planification du travail ne sera jamais déclenchée",
  "scm.import.status.UNKNOWN.display.text": "Statut d'importation : Non suivi",
  "scm.import.status.LOADING.description":
    "Importation : Le statut du travail est en cours de chargement",
  "scm.export.status.DELETED.display.text": "Supprimé",
  "scm.export.status.EXPORT_NEEDED.display.text": "Modifié",
  "scm.export.status.CLEAN.description": "Statut d'exportation : Propre",
  "scm.import.status.DELETE_NEEDED.title.text":
    "Importation : Fichiers supprimés",
  "scm.export.status.EXPORT_NEEDED.title.text": "Exportation : Modifié",
  "scm.import.status.UNKNOWN.description": "Non suivi pour l'importation SCM",
  "scm.import.status.REFRESH_NEEDED.display.text": "Synchronisation nécessaire",
  "scm.export.status.CREATE_NEEDED.description":
    "Statut d'exportation : Nouveau travail, pas encore ajouté au SCM",
  "scm.import.status.DELETE_NEEDED.description":
    "Statut d'importation : Le fichier source a été supprimé",
  "scm.export.status.CLEAN.display.text": "Aucune modification",
  "scm.export.status.LOADING.description":
    "Exportation : Le statut du travail est en cours de chargement",
  "scm.import.status.IMPORT_NEEDED.description":
    "Statut d'importation : Les modifications du travail doivent être récupérées",
  "scm.import.status.REFRESH_NEEDED.title.text":
    "Importation : Synchroniser les modifications",
  "scm.export.status.REFRESH_NEEDED.title.text":
    "Exportation : Les modifications à distance doivent être synchronisées",
  "scm.export.status.LOADING.display.text": "Chargement",
  "scm.export.status.EXPORT_NEEDED.description":
    "Statut d'exportation : Modifié",
  "scm.export.status.CREATE_NEEDED.display.text": "Créé",
  "scm.import.status.IMPORT_NEEDED.display.text": "Importation nécessaire",
  "scm.import.status.DELETE_NEEDED.display.text":
    "Le fichier source a été supprimé",
  "scm.import.status.IMPORT_NEEDED.title.text":
    "Importation : Modifications entrantes",
  "scm.import.status.CLEAN.display.text": "À jour",
  "scm.import.status.CLEAN.description": "Statut d'importation : À jour",
  "scm.import.status.REFRESH_NEEDED.description":
    "Statut d'importation : Les modifications du travail doivent être récupérées",
  "scm.export.status.REFRESH_NEEDED.display.text": "Synchronisation nécessaire",
  "scm.import.status.LOADING.display.text": "Chargement",
  "scm.export.status.ERROR.display.text": "Une erreur inconnue s'est produite.",
  "scm.import.status.ERROR.display.text": "Une erreur inconnue s'est produite.",
  "scm.status.ERROR.display.text": "Erreur SCM",
  "scm.export.auth.key.noAccess":
    "L'utilisateur n'a pas accès à la clé ou au mot de passe spécifié",
  "scm.import.auth.key.noAccess":
    "L'utilisateur n'a pas accès à la clé ou au mot de passe spécifié",
  "scm.action.diff.clean.button.label": "Voir les informations du commit",
  "scm.import.plugin": "Plugin d'importation SCM",
  "scm.action.diff.button.label": "Différences de modifications",
  "scm.export.commit.job.link.title":
    "Cliquez pour commiter ou ajouter ce travail",
  "scm.export.commit.link.title":
    "Cliquez pour commiter ou ajouter des modifications",
  "scm.export.plugin": "Plugin d'exportation SCM",
  "job.toggle.scm.menu.on": "Activer SCM",
  "job.toggle.scm.menu.off": "Désactiver SCM",
  "scm.import.actions.title": "Actions d'importation SCM",
  "scm.export.actions.title": "Actions d'exportation SCM",
  "scm.export.title": "Exportation SCM",
  "scm.import.title": "Importation SCM",
  "job.toggle.scm.button.label.off": "Désactiver SCM",
  "job.toggle.scm.confirm.panel.title": "Confirmer la modification SCM",
  "job.toggle.scm.confirm.on": "Activer tous les plugins SCM configurés ?",
  "job.toggle.scm.confirm.off": "Désactiver tous les plugins SCM configurés ?",
  "job.toggle.scm.button.label.on": "Activer SCM",
  "job.scm.status.loading.message": "Chargement du statut SCM...",
  "page.section.Activity.for.jobs": "Activité pour les travaux",
  "widget.theme.title": "Thème",
  "widget.nextUi.title": "Activer la prochaine UI",
  "page.section.title.AllJobs": "Tous les travaux",
  "advanced.search": "Avancé",
  "jobs.advanced.search.title": "Cliquez pour modifier le filtre",
  "filter.jobs": "Rechercher des travaux",
  "job.filter.quick.placeholder": "Rechercher",
  "job.filter.apply.button.title": "Rechercher",
  "job.filter.clear.button.title": "Effacer la recherche",
  all: "Tous",
  "job.tree.breakpoint.hit.info":
    "Avis : Tous les détails du travail n'ont pas été chargés car ce groupe contient trop de travaux. Cliquez sur le bouton pour charger les détails manquants.",
  "job.tree.breakpoint.load.button.title":
    "Charger tous les détails du travail",
  "job.list.filter.save.modal.title": "Enregistrer le filtre",
  "job.filter.save.button.title": "Enregistrer comme filtre…",
  "job.list.filter.save.button": "Enregistrer le filtre",
  "job.list.filter.delete.filter.link.text": 'Supprimer le filtre "{0}"',
  "app.firstRun.title": "Bienvenue à {0} {1}",
  "you.can.see.this.message.again.by.clicking.the":
    "Vous pouvez voir ce message à nouveau en cliquant sur le",
  "version.number": "numéro de version",
  "in.the.page.footer": "dans le pied de page.",
  "no.authorized.access.to.projects":
    "Vous n'avez pas accès autorisé aux projets.",
  "no.authorized.access.to.projects.contact.your.administrator.user.roles.0":
    "Contactez votre administrateur. (Rôles utilisateur : {0})",
  "page.home.loading.projects": "Chargement des projets",
  "app.firstRun.md":
    "Merci d'être un abonné {0}.\n\n" +
    "  \n\n\n" +
    "* [{0} Portail de support &raquo;](http://support.rundeck.com)\n\n" +
    "* [{0} Documentation &raquo;]({1})",
  "page.home.section.project.title": "{0} Projet",
  "page.home.section.project.title.plural": "{0} Projets",
  "page.home.duration.in.the.last.day": "Au cours du dernier jour",
  by: "par",
  user: "Utilisateur",
  "user.plural": "Utilisateurs",
  "page.home.project.executions.0.failed.parenthetical": "({0} Échoué)",
  "page.home.search.projects.input.placeholder":
    "Recherche de projet : nom, étiquette ou /regex/",
  "page.home.search.project.title": "{n} Projet trouvé | {n} Projets trouvés",
  "button.Action": "Action",
  "edit.configuration": "Modifier la configuration",
  "page.home.new.project.button.label": "Nouveau projet",
  Execution: "{n} Exécutions | {n} Exécution | {n} Exécutions",
  in: "dans",
  "Project.plural": "Projets",
  discard: "Jeter",
  "commandline.arguments.prompt.unquoted":
    "Arguments de ligne de commande (non cités) :",
  usage: "Utilisation",
  "form.label.valuesType.list.label": "Liste",
  "scheduledExecution.option.unsaved.warning":
    "Jetez ou enregistrez les modifications apportées à cette option avant de terminer les modifications du travail",
  "bash.prompt": "Bash :",
  "script.content.prompt": "Contenu du script :",
  "rundeck.user.guide.option.model.provider":
    "Guide de l'utilisateur Rundeck - Fournisseur de modèle d'option",
  save: "Enregistrer",
  "commandline.arguments.prompt": "Arguments de ligne de commande :",
  "commandline.arguments.prompt.unquoted.warning":
    "Attention ! Compter sur des arguments non cités pourrait rendre ce travail vulnérable à l'injection de commandes. Utilisez avec précaution.",
  "add.new.option": "Ajouter une nouvelle option",
  "add.an.option": "Ajouter une option",
  "option.values.c": "1 Valeur|{n} Valeurs",
  "no.options.message": "Aucune option",
  "the.option.values.will.be.available.to.scripts.in.these.forms":
    "Les valeurs de l'option seront disponibles pour les scripts sous ces formes :",
  "form.option.date.label": "Date",
  "form.option.enforcedType.label": "Restrictions",
  "form.option.usage.file.fileName.preview.description":
    "Le nom de fichier original :",
  "form.option.discard.title": "Jeter les modifications apportées à l'option",
  "form.option.valuesType.url.authentication.password.label": "Mot de passe",
  "form.option.enforcedType.none.label":
    "Tous les valeurs peuvent être utilisées",
  "form.option.inputType.label": "Type d'entrée",
  "form.option.defaultStoragePath.present.description":
    "Une valeur par défaut sera chargée à partir du stockage de clés",
  "form.option.secureInput.false.label": "Texte brut",
  "form.option.name.label": "Nom de l'option",
  "form.option.valuesType.url.filter.label": "Filtre de chemin JSON",
  "form.option.valuesType.url.authType.label": "Type d'authentification",
  "form.option.sort.description": "Trier la liste des valeurs autorisées",
  "form.option.usage.secureAuth.message":
    "Les valeurs des options d'authentification sécurisée ne sont pas disponibles pour les scripts ou les commandes",
  "form.option.valuesType.url.label": "URL distante",
  "form.option.valuesList.placeholder":
    "Liste séparée par un délimiteur (virgule par défaut)",
  "form.option.secureInput.description":
    "Les valeurs d'entrée sécurisées ne sont pas stockées par Rundeck après utilisation. Si la valeur exposée est utilisée dans un script ou une commande, le journal de sortie peut contenir la valeur.",
  "form.option.valuesType.url.authentication.key.label": "Clé",
  "form.option.date.description":
    "La date sera transmise à votre travail sous forme de chaîne formatée de cette manière : mm/jj/aa HH:MM",
  "form.option.valuesType.url.authentication.username.label":
    "Nom d'utilisateur",
  "form.option.valuesType.url.authType.bearerToken.label": "Jeton Bearer",
  "form.option.enforced.label": "Imposé à partir des valeurs autorisées",
  "form.option.description.label": "Description",
  "form.option.save.title":
    "Enregistrer les modifications apportées à l'option",
  "form.option.type.label": "Type d'option",
  "form.option.multivalueAllSelected.label":
    "Sélectionner toutes les valeurs par défaut",
  "form.option.secureExposed.false.label":
    "Authentification à distance sécurisée",
  "form.option.valuesType.url.authentication.token.label": "Jeton",
  "form.option.delimiter.label": "Délimiteur",
  "form.option.valuesURL.placeholder": "URL distante",
  "form.option.valuesType.url.authType.empty.label":
    "Sélectionner le type d'authentification",
  "form.option.valuesDelimiter.description":
    "Définir le délimiteur pour les valeurs autorisées",
  "form.option.usage.file.preview.description":
    "Le chemin du fichier local sera disponible pour les scripts sous ces formes :",
  "form.option.secureExposed.false.description":
    "Entrée de mot de passe, valeur non exposée dans les scripts ou les commandes, utilisée uniquement par les exécuteurs de nœuds pour l'authentification.",
  "form.option.valuesType.url.filter.error.label":
    "Le filtre de chemin JSON de l'URL distante a une syntaxe invalide",
  "form.option.valuesType.url.authType.apiKey.label": "Clé API",
  "form.option.optionType.text.label": "Texte",
  "form.option.secureExposed.true.label":
    "Texte brut avec entrée de mot de passe",
  "form.option.valuesType.url.filter.description":
    'Filtrer les résultats JSON en utilisant un chemin de clé, par exemple "$.key.path"',
  "form.option.valuesType.url.authentication.tokenInformer.header.label":
    "En-tête",
  "form.option.defaultStoragePath.description":
    "Chemin de stockage de clés pour une valeur de mot de passe par défaut",
  "form.option.multivalued.label": "Multi-valeurs",
  "form.option.multivalued.description":
    "Permettre de choisir plusieurs valeurs d'entrée.",
  "form.option.valuesType.url.authentication.tokenInformer.query.label":
    "Paramètre de requête",
  "form.option.create.title": "Enregistrer la nouvelle option",
  "form.option.regex.label": "Expression régulière de correspondance",
  "form.option.optionType.file.label": "Fichier",
  "form.option.valuesDelimiter.label": "Délimiteur de liste",
  "form.option.cancel.title": "Annuler l'ajout de la nouvelle option",
  "form.option.values.label": "Valeurs autorisées",
  "form.option.dateFormat.description.md":
    "Entrez un format de date comme décrit dans la [documentation momentjs](http://momentjs.com/docs/#/displaying/format/)",
  "form.option.defaultStoragePath.label": "Chemin de stockage",
  "form.option.defaultValue.label": "Valeur par défaut",
  "form.option.delimiter.description":
    "Le délimiteur sera utilisé pour joindre toutes les valeurs d'entrée. Peut être n'importe quelle chaîne : ' ' (espace), ',' (virgule), etc. Remarque : ne pas inclure de guillemets.",
  "form.option.secureInput.false.description": "Entrée de texte brut.",
  "form.option.valuesType.url.authType.basic.label": "Basique",
  "form.option.valuesType.url.authentication.tokenInformer.label":
    "Injecter la clé",
  "form.option.secureExposed.true.description":
    "Texte brut avec une entrée de mot de passe, valeur exposée dans les scripts et les commandes.",
  "form.option.dateFormat.title": "Format de date",
  "form.option.label.label": "Étiquette de l'option",
  "form.option.valuesUrl.description": "Une URL vers un service JSON distant.",
  "form.option.multivalued.secure-conflict.message":
    "Les options d'entrée sécurisée ne permettent pas plusieurs valeurs",
  "form.option.sort.label": "Trier les valeurs",
  "form.option.usage.file.sha.preview.description":
    "La valeur SHA-256 du contenu du fichier :",
  "Option.property.description.description":
    "La description sera rendue avec Markdown.",
  "option.defaultValue.regexmismatch.message":
    'La valeur par défaut "{0}" ne correspond pas à la regex : {1}',
  "option.multivalued.secure-conflict.message":
    "L'entrée sécurisée ne peut pas être utilisée avec une entrée multi-valeurs",
  "option.defaultValue.notallowed.message":
    "La valeur par défaut n'était pas dans la liste des valeurs autorisées, et les valeurs sont imposées",
  "option.enforced.secure-conflict.message":
    "L'entrée sécurisée ne peut pas être utilisée avec des valeurs imposées",
  "option.file.config.disabled.message":
    "Le plugin de configuration du type d'option de fichier n'est pas activé",
  "option.defaultValue.required.message":
    "Spécifiez une valeur par défaut pour les options obligatoires lorsque le travail est planifié.",
  "option.enforced.emptyvalues.message":
    "Les valeurs autorisées (liste ou URL distante) doivent être spécifiées si les valeurs sont imposées",
  "option.file.required.message":
    "Le type d'option de fichier ne peut pas être obligatoire lorsque le travail est planifié.",
  "option.regex.invalid.message": "Expression régulière invalide : {0}",
  "option.file.config.invalid.message":
    "La configuration du type d'option de fichier n'est pas valide : {0}",
  "option.delimiter.blank.message":
    "Vous devez spécifier un délimiteur pour les options multi-valeurs",
  "option.hidden.notallowed.message":
    "Les options cachées doivent avoir une valeur par défaut ou un chemin de stockage.",
  "option.values.regexmismatch.message":
    'La valeur autorisée "{0}" ne correspond pas à la regex : {1}',
  "option.defaultValue.multivalued.notallowed.message": `La valeur par défaut contient une chaîne qui n'était pas dans la liste des valeurs autorisées, et les valeurs sont imposées : "{0}". Remarque : les espaces sont significatifs.`,
  "Option.required.label": "Obligatoire",
  "Option.hidden.description":
    "Doit être caché sur la page d'exécution du travail",
  "Option.required.description":
    "Exiger que cette option ait une valeur non vide lors de l'exécution du travail",
  "Option.hidden.label": "Doit être caché",
  "form.option.regex.placeholder": "Entrez une expression régulière",
  "home.user": "{n} Utilisateurs | {n} Utilisateur | {n} Utilisateurs",
  "home.table.projects": "Projets",
  "home.table.activity": "Activité",
  "home.table.actions": "Actions",
  "option.click.to.edit.title": "Cliquez pour modifier",
  "form.option.regex.validation.error":
    "Valeur invalide : Elle doit correspondre au modèle : {0}",
  "form.field.required.message": "Ce champ est obligatoire",
  "form.field.too.long.message":
    "Cette valeur ne peut pas dépasser {max} caractères",
  "form.option.validation.errors.message":
    "Corrigez les erreurs de validation avant d'enregistrer les modifications",
  "option.list.header.name.title": "Nom",
  "option.list.header.values.title": "Valeurs",
  "option.list.header.restrictions.title": "Restrictions",
  "util.undoredo.undo": "Annuler",
  "util.undoredo.redo": "Rétablir",
  "util.undoredo.revertAll": "Revenir à toutes les modifications",
  "util.undoredo.revertAll.confirm":
    "Vraiment revenir à toutes les modifications ?",
  "option.view.required.title": " (Obligatoire)",
  "option.view.allowedValues.label": "Valeurs autorisées",
  "option.view.valuesUrl.title": "Valeurs chargées depuis l'URL distante : {0}",
  "option.view.valuesUrl.placeholder": "URL",
  "option.view.enforced.title":
    "L'entrée doit être l'une des valeurs autorisées",
  "option.view.enforced.placeholder": "Strict",
  "option.view.regex.info.note":
    "Les valeurs doivent correspondre à l'expression régulière :",
  "option.view.notenforced.title": "Aucune restriction sur la valeur d'entrée",
  "option.view.notenforced.placeholder": "Aucun",
  "option.view.action.delete.title": "Supprimer cette option",
  "option.view.action.edit.title": "Modifier cette option",
  "option.view.action.duplicate.title": "Dupliquer cette option",
  "option.view.action.moveUp.title": "Déplacer vers le haut",
  "option.view.action.moveDown.title": "Déplacer vers le bas",
  "option.view.action.drag.title": "Faites glisser pour réorganiser",
  "pagination.of": "de",
  uiv: {
    datePicker: {
      clear: "Effacer",
      today: "Aujourd'hui",
      month: "Mois",
      month1: "Janvier",
      month2: "Février",
      month3: "Mars",
      month4: "Avril",
      month5: "Mai",
      month6: "Juin",
      month7: "Juillet",
      month8: "Août",
      month9: "Septembre",
      month10: "Octobre",
      month11: "Novembre",
      month12: "Décembre",
      year: "Année",
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
    modal: {
      cancel: "Annuler",
      ok: "OK",
    },
    multiSelect: {
      placeholder: "Sélectionner...",
      filterPlaceholder: "Rechercher...",
    },
  },
  "scheduledExecution.jobName.label": "Nom du travail",
  "scheduledExecution.property.description.label": "Description",
  "job.editor.preview.runbook": "Aperçu du Readme",
  "choose.action.label": "Choisir",
  "scheduledExecution.property.description.plain.description":
    "La description sera affichée en texte brut",
  "scheduledExecution.property.description.description":
    "La première ligne de la description sera affichée en texte brut, le reste sera rendu avec Markdown.\n\n" +
    "Voir [Markdown](http://en.wikipedia.org/wiki/Markdown).\n\n" +
    "Dans la description étendue, vous pouvez lier au travail en utilisant {'`{{job.permalink}}`'} comme URL vers le travail, par exemple, `[exécuter le travail]({'{{job.permalink}}#runjob'})`\n\n" +
    "Vous pouvez ajouter un Readme en utilisant un séparateur HR `---` seul sur une ligne, et tout ce qui suit sera rendu dans un onglet séparé en utilisant [Markdeep](https://casual-effects.com/markdeep).",
  "scheduledExecution.groupPath.description":
    "Le groupe est un chemin séparé par /",
  more: "Plus…",
  less: "Moins…",
  "job.edit.groupPath.choose.text": "Cliquez sur le nom du groupe à utiliser",
  "scheduledExecution.property.executionLifecyclePluginConfig.help.text":
    "Les plugins sélectionnés seront activés pour ce travail.",
  Workflow: {
    label: "Flux de travail",
    property: {
      keepgoing: {
        true: { description: "Exécuter les étapes restantes avant d'échouer." },
        false: { description: "Arrêter à l'étape échouée." },
        prompt: "Si une étape échoue :",
      },
      strategy: {
        label: "Stratégie",
      },
    },
  },
  "plugin.choose.title": "Choisir un plugin",
  "plugin.type.WorkflowStep.title.plural": "Étapes du flux de travail",
  "plugin.type.WorkflowStep.title": "Étape du flux de travail",
  "plugin.type.WorkflowNodeStep.title.plural":
    "Étapes du nœud du flux de travail",
  "plugin.type.WorkflowNodeStep.title": "Étape du nœud du flux de travail",
  "JobExec.nodeStep.true.label": "Étape du nœud",
};

export default messages;
