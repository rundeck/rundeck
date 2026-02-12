import en_US from "./en_US";

const messages = {
  ...en_US,
  Edit: "Bearbeiten",
  Save: "Speichern",
  Delete: "Löschen",
  Cancel: "Abbrechen",
  Revert: "Zurücksetzen",
  Search: "Suchen",
  search: "Suchen",
  add: "hinzufügen",
  yes: "Ja",
  no: "Nein",
  Yes: "Ja",
  No: "Nein",
  Configuration: "Konfiguration",
  Enhancers: "Enhancer",
  "edit.nodes.header": "Nodes bearbeiten",
  "project.node.sources.title": "Node-Quellen",
  "project.node.sources.title.short": "Quellen",
  "The Node Source had an error": "Die Node-Quelle hat einen Fehler gemeldet",
  "Validation errors": "Validierungsfehler",

  "results.empty.text": "Keine Ergebnisse für diese Abfrage",
  "Any Time": "Jederzeit",
  Any: "Beliebig",
  "Other...": "Andere...",
  "More...": "Mehr...",
  "Save Filter": "Filter speichern",
  "Save as a Filter...": "Als Filter speichern...",
  "filter.save.button": "Filter speichern...",
  "save.filter.ellipsis": "Filter speichern",
  "job.filter.save.button.title": "Als Filter speichern...",
  "job.list.filter.save.button": "Filter speichern",
  "page.section.title.AllJobs": "Alle Jobs",
  "All Jobs": "Alle Jobs",
  "page.section.Activity.for.jobs": "Aktivität für Jobs",
  "Activity for Jobs": "Aktivitaet fuer Jobs",
  "expand.all": "Alle ausklappen",
  "Expand All": "Alle ausklappen",
  "collapse.all": "Alle einklappen",
  "Collapse All": "Alle einklappen",
  "Modules": "Module",
  "Executions": "Ausfuehrungen",
  execution: "Ausführung | Ausführungen",
  "execution.count": "1 Ausführung | {0} Ausführungen",
  Execution: "{n} Ausführungen | {n} Ausführung | {n} Ausführungen",
  "Execution.plural": "Ausfuehrungen",
  "filter.jobs": "Jobs suchen",
  "search.ellipsis": "Suchen...",
  "job.filter.quick.placeholder": "Suchen",
  "job.filter.apply.button.title": "Suchen",
  "jobquery.title.titleFilter": "Ad-hoc-Befehl",
  "button.action.Cancel": "Abbrechen",
  "button.action.Create": "Erstellen",
  "options.label": "Optionen",
  "No Options": "Keine Optionen",
  "add.an.option": "Option hinzufügen",
  "Add an option": "Option hinzufügen",
  "no.options.message": "Keine Optionen",
  "util.undoredo.undo": "Rückgängig",
  "util.undoredo.redo": "Wiederholen",
  "Workflow.property.keepgoing.prompt": "Wenn ein Schritt fehlschlägt:",
  "Workflow.property.keepgoing.false.description":
    "Beim fehlgeschlagenen Schritt stoppen.",
  "Workflow.property.keepgoing.true.description":
    "Verbleibende Schritte vor dem Fehlschlag ausführen.",
  "Workflow.property.strategy.label": "Strategie",
  "Workflow.strategy.label.node-first": "Node-zentriert",
  "Workflow.strategy.label.step-first": "Schritt-zentriert",
  "Workflow.strategy.label.parallel": "Parallel",
  "Workflow.strategy.description.node-first":
    "Führe alle Schritte auf einem Node aus, bevor mit dem nächsten Node fortgefahren wird.",
  "Workflow.strategy.description.step-first":
    "Führe einen Schritt auf allen Nodes aus, bevor mit dem nächsten Schritt fortgefahren wird.",
  "Node First": "Node-zentriert",
  "Explain": "Erklärung",
  explain: "Erklärung",
  "global.log.filters": "Globale Log-Filter",
  "workflow.all.steps": "Alle Workflow-Schritte",
  "Global Log Filters": "Globale Log-Filter",
  "No Workflow steps": "Keine Workflow-Schritte",
  "Workflow.noSteps": "Keine Workflow-Schritte",
  "step.plugins.filter.prompt": "Schritt suchen",
  "framework.service.WorkflowNodeStep.description":
    "Wird einmal für jeden Node im Workflow ausgeführt.",
  "step.type.jobreference.title": "Job-Referenz",
  "step.type.jobreference.nodestep.description":
    "Job auf dem Remote-Node ausführen",
  add: "hinzufügen",

  "Edit Nodes": "Nodes bearbeiten",
  "Edit Node Sources": "Node-Quellen bearbeiten",
  "edit.node.enhancers": "Node-Enhancer bearbeiten",
  "Add a new Node Source": "Neue Node-Quelle hinzufügen",
  "add.node.source": "Neue Node-Quelle hinzufügen",
  "Add a new Node Enhancer": "Neuen Node-Enhancer hinzufügen",
  "add.node.enhancer": "Neuen Node-Enhancer hinzufügen",
  "framework.service.NodeEnhancer.label.short.plural": "Enhancer",
  "framework.service.NodeEnhancer.explanation":
    "Node-Enhancer können die aus Node-Quellen geladenen Daten verändern.",
  "no.modifiable.sources.found": "Keine änderbaren Quellen gefunden",
  "modifiable.node.sources.will.appear.here":
    "Änderbare Node-Quellen werden hier angezeigt.",

  "project.edit.ResourceModelSource.explanation":
    "Node-Quellen für das Projekt. Quellen werden in der definierten Reihenfolge geladen; spätere Quellen überschreiben frühere Quellen. (Sie können {'${project.name}'} in Konfigurationswerten verwenden, um den Projektnamen einzusetzen.)",
  "Node Sources for the project. Sources are loaded in the defined order, with later sources overriding earlier sources. (You can use ${project.name} inside configuration values to substitute the project name.)":
    "Node-Quellen für das Projekt. Quellen werden in der festgelegten Reihenfolge geladen; spätere Quellen überschreiben frühere Quellen. (Sie können ${project.name} in Konfigurationswerten verwenden, um den Projektnamen einzusetzen.)",
  "Node Sources for the project. Sources are loaded in the defined order, with later sources overriding earlier sources. (You can use  {'${project.name}'} inside configuration values to substitute the project name.)":
    "Node-Quellen für das Projekt. Quellen werden in der festgelegten Reihenfolge geladen; spätere Quellen überschreiben frühere Quellen. (Sie können ${project.name} in Konfigurationswerten verwenden, um den Projektnamen einzusetzen.)",
  "Node Enhancers can modify the data loaded from Node Sources.":
    "Node-Enhancer können die aus Node-Quellen geladenen Daten ändern.",
  "use.the.node.sources.tab.1": "Verwenden Sie den ",
  "use.the.node.sources.tab.2":
    "Tab, um Ihre Node-Quellen zu aktualisieren.",
  "none.configured.click.plugin.to.add.a.new.plugin":
    "Nichts konfiguriert. Klicken Sie auf {0}, um ein neues Plugin hinzuzufügen.",
  "None configured. Click ResourceModelSource to add a new plugin.":
    "Nichts konfiguriert. Klicken Sie auf ResourceModelSource, um ein neues Plugin hinzuzufügen.",
  "None configured. Click NodeEnhancer to add a new plugin.":
    "Nichts konfiguriert. Klicken Sie auf NodeEnhancer, um ein neues Plugin hinzuzufügen.",
  "Scans a directory and loads all resource document files":
    "Durchsucht ein Verzeichnis und lädt alle Ressourcen-Dokumentdateien",
  "Reads a file containing node definitions in a supported format":
    "Liest eine Datei mit Node-Definitionen in einem unterstützten Format",
  "Provides the local node as the single resource":
    "Stellt den lokalen Node als einzige Ressource bereit",
  "Run a script to produce resource model data":
    "Führt ein Skript aus, um Ressourcenmodell-Daten zu erzeugen",
  "Retrieves a URL containing node definitions in a supported format":
    "Ruft eine URL mit Node-Definitionen in einem unterstützten Format ab",

  "ScheduledExecution.page.create.title": "Neuen Job erstellen",
  "upload.definition.button.label": "Definition hochladen",
  "scheduledExecution.jobName.label": "Job-Name",
  "scheduledExecution.property.description.label": "Beschreibung",
  "scheduledExecution.groupPath.description":
    "Gruppe ist ein durch / getrennter Pfad",
  "scheduledExecution.property.description.description":
    "Die erste Zeile der Beschreibung wird als Klartext angezeigt, der Rest wird mit Markdown dargestellt.\n\n" +
    "Siehe [Markdown](http://en.wikipedia.org/wiki/Markdown).\n\n" +
    "In der erweiterten Beschreibung können Sie mit {'`{{job.permalink}}`'} auf den Job verlinken, z. B. `[Job ausführen]({'{{job.permalink}}#runjob'})`.\n\n" +
    "Sie können ein Readme hinzufügen, indem Sie eine HR-Trennlinie `---` in einer eigenen Zeile verwenden; alles danach wird in einem separaten Tab mit [Markdeep](https://casual-effects.com/markdeep) gerendert.",
  "scheduledExecution.property.description.plain.description":
    "Die Beschreibung wird als Klartext angezeigt",
  "choose.action.label": "Auswählen",
  "new.job.button.label": "Neuer Job",
  "job.actions": "Job-Aktionen",
  "advanced.search": "Erweitert",
  "job.create.button": "Neuen Job erstellen",
  "job.upload.button.title": "Job-Definition hochladen",
  "button.Action": "Aktion",
  "execute.locally": "Lokal ausführen",
  "dispatch.to.nodes": "Auf Nodes ausführen",
  "Dispatch to Nodes": "Auf Nodes ausführen",
  "Execute locally": "Lokal ausführen",
  "schedule.to.run.repeatedly": "Wiederholt ausführen?",
  "scheduledExecution.property.scheduleEnabled.label":
    "Zeitplanung aktivieren?",
  "scheduledExecution.property.scheduleEnabled.description":
    "Darf dieser Job geplant werden?",
  "scheduledExecution.property.executionEnabled.label":
    "Ausführung aktivieren?",
  "scheduledExecution.property.executionEnabled.description":
    "Darf dieser Job ausgeführt werden?",
  "scheduledExecution.property.loglevel.label": "Log-Level",
  "scheduledExecution.property.loglevel.help":
    "Debug-Level erzeugt mehr Ausgaben",
  "scheduledExecution.property.multipleExecutions.label":
    "Mehrere Ausführungen?",
  "scheduledExecution.property.multipleExecutions.description":
    "Darf dieser Job mehr als einmal gleichzeitig ausgeführt werden?",
  "scheduledExecution.property.maxMultipleExecutions.label":
    "Mehrere Ausführungen begrenzen?",
  "scheduledExecution.property.maxMultipleExecutions.description":
    "Maximale Anzahl gleichzeitiger Ausführungen. Leer oder 0 bedeutet kein Limit.",
  "scheduledExecution.property.timeout.label": "Timeout",
  "scheduledExecution.property.timeout.title": "Timeout-Dauer",
  "scheduledExecution.property.timeout.description":
    "Maximale Laufzeit einer Ausführung. Zeit in Sekunden oder mit Zeiteinheiten wie \"120m\", \"2h\", \"3d\". Leer oder 0 bedeutet kein Timeout. Kann Optionsreferenzen enthalten wie \"{'$'}{'{'}option{'.'}timeout{'}'}\".",
  "scheduledExecution.property.retry.label": "Wiederholung",
  "scheduledExecution.property.retry.description":
    "Maximale Anzahl von Wiederholungen, wenn dieser Job direkt aufgerufen wird. Eine Wiederholung erfolgt bei Fehler oder Timeout, nicht bei manuellem Abbruch. Kann eine Optionsreferenz enthalten wie \"{'$'}{'{'}option{'.'}retry{'}'}\".",
  "scheduledExecution.property.retry.delay.label": "Wiederholungsverzögerung",
  "scheduledExecution.property.retry.delay.description":
    "Zeit zwischen fehlgeschlagener Ausführung und Wiederholung. Zeit in Sekunden oder mit Zeiteinheiten wie \"120m\", \"2h\", \"3d\". Leer oder 0 bedeutet keine Verzögerung. Kann Optionsreferenzen enthalten wie \"{'$'}{'{'}option{'.'}delay{'}'}\".",
  "scheduledExecution.property.nodeKeepgoing.prompt": "Wenn ein Node fehlschlägt",
  "scheduledExecution.property.nodeKeepgoing.true.description":
    "Auf allen verbleibenden Nodes weiter ausführen, bevor der Schritt fehlschlägt.",
  "scheduledExecution.property.nodeKeepgoing.false.description":
    "Schritt fehlschlagen lassen, ohne auf verbleibenden Nodes auszuführen.",
  "scheduledExecution.property.successOnEmptyNodeFilter.prompt":
    "Wenn Node-Menge leer ist",
  "scheduledExecution.property.successOnEmptyNodeFilter.true.description":
    "Ausführung fortsetzen.",
  "scheduledExecution.property.successOnEmptyNodeFilter.false.description":
    "Job fehlschlagen lassen.",
  "scheduledExecution.property.nodeRankAttribute.label": "Rang-Attribut",
  "scheduledExecution.property.nodeRankAttribute.description":
    "Node-Attribut für die Sortierung. Standard ist der Node-Name.",
  "scheduledExecution.property.nodeRankOrder.label": "Rangfolge",
  "scheduledExecution.property.nodeRankOrder.ascending.label": "Aufsteigend",
  "scheduledExecution.property.nodeRankOrder.descending.label": "Absteigend",
  "scheduledExecution.property.nodeIntersect.label": "Schnittmenge der Nodes",
  "scheduledExecution.property.nodeIntersect.true":
    "Ja, Node-Schnittmenge mit den aktuellen Job-Nodes bilden",
  "scheduledExecution.property.nodeIntersect.false":
    "Nein, Node-Filter des referenzierten Jobs verwenden (definiert oder überschrieben)",
  "scheduledExecution.property.nodeThreadcount.label": "Anzahl Threads",
  "scheduledExecution.property.nodeThreadcount.description":
    "Maximale Anzahl paralleler Threads. (Standard: 1)",
  "scheduledExecution.property.doNodedispatch.description":
    "Wählen Sie, ob der Job auf gefilterten Nodes oder nur lokal ausgeführt wird.",
  "scheduledExecution.property.nodesSelectedByDefault.label": "Node-Auswahl",
  "scheduledExecution.property.nodesSelectedByDefault.true.description":
    "Ziel-Nodes sind standardmäßig ausgewählt",
  "scheduledExecution.property.nodesSelectedByDefault.false.description":
    "Der Benutzer muss Ziel-Nodes explizit auswählen",
  "scheduledExecution.property.orchestrator.label": "Orchestrator",
  "scheduledExecution.property.orchestrator.description":
    "Damit kann Reihenfolge und Timing der Node-Verarbeitung gesteuert werden",
  "scheduledExecution.property.defaultTab.label": "Standard-Tab",
  "scheduledExecution.property.defaultTab.description":
    "Standard-Tab, der beim Öffnen einer Ausführung angezeigt wird.",
  "scheduledExecution.property.nodefiltereditable.label": "Bearbeitbarer Filter",
  "scheduledExecution.property.excludeFilterUncheck.label":
    "Ausgeschlossene Nodes anzeigen",
  "scheduledExecution.property.excludeFilterUncheck.description":
    "Wenn aktiv, werden ausgeschlossene Nodes bei der Jobausführung angezeigt. Andernfalls werden sie gar nicht angezeigt.",
  "scheduledExecution.property.excludeFilter.description":
    "Sekundärer Filter, der Nodes aus dem Ergebnis des Node-Filters ausschließt.",
  "scheduledExecution.property.timezone.prompt": "Zeitzone",
  "scheduledExecution.property.timezone.description":
    "Gültige Zeitzone, z. B. \"PST\", ein vollständiger Name wie \"Europe/Berlin\" oder eine benutzerdefinierte ID wie \"GMT-8:00\".",
  "Allow this Job to be scheduled?": "Darf dieser Job geplant werden?",
  "Allow this Job to be executed?": "Darf dieser Job ausgeführt werden?",
  "notification.event.onstart": "Beim Start",
  "notification.event.onsuccess": "Bei Erfolg",
  "notification.event.onfailure": "Bei Fehler",
  "notification.event.onretryablefailure": "Bei wiederholbarem Fehler",
  "notification.event.onavgduration": "Durchschnittsdauer überschritten",
  "scheduledExecution.property.notifyAvgDurationThreshold.label": "Schwelle",
  jobAverageDurationPlaceholder:
    "leer lassen für die durchschnittliche Job-Dauer",
  "Add Notification": "Benachrichtigung hinzufügen",
  "Selected Plugins will be enabled for this Job.":
    "Ausgewählte Plugins werden für diesen Job aktiviert.",
  "Kill tracked processes after execution":
    "Nach der Ausführung verfolgte Prozesse beenden",
  "Kill all processes collected by the 'Capture Process IDs' log filter":
    "Alle Prozesse beenden, die durch den Log-Filter 'Capture Process IDs' erfasst wurden",
  "This operation will use the 'kill' and 'pkill' for Unix and 'taskkill' for Windows commands. These commands must be available at the node.":
    "Dieser Vorgang verwendet für Unix die Befehle 'kill' und 'pkill' sowie für Windows 'taskkill'. Diese Befehle müssen auf dem Node verfügbar sein.",
  "Kill spawned processes": "Erzeugte Prozesse beenden",
  "Also kill processes whose process SID matches the tracked PIDs":
    "Auch Prozesse beenden, deren Prozess-SID mit den verfolgten PIDs übereinstimmt",
  "Kill processes only if job failed or is killed":
    "Prozesse nur beenden, wenn der Job fehlschlägt oder abgebrochen wird",
  "notifications.helpText":
    "Benachrichtigungen können durch verschiedene Ereignisse während der Job-Ausführung ausgelöst werden.",
  "notifications.emptyText":
    "Es sind keine Benachrichtigungen definiert. Klicken Sie unten auf ein Ereignis, um eine Benachrichtigung für diesen Auslöser hinzuzufügen.",
  "notifications.addButton": "Benachrichtigung hinzufügen",

  "storage.enter.path": "Pfad eingeben",
  "storage.enter.password": "Passwort eingeben",
  "storage.enter.directory.name": "Verzeichnisnamen eingeben",
  "storage.specify.name": "Namen angeben.",

  "Search Activity": "Aktivität durchsuchen",
  "button.cancel": "Abbrechen",
  "button.create": "Erstellen",
  "message_cancel": "Abbrechen",

  notifications: {
    ...en_US.notifications,
    helpText:
      "Benachrichtigungen können durch verschiedene Ereignisse während der Job-Ausführung ausgelöst werden.",
    emptyText:
      "Es sind keine Benachrichtigungen definiert. Klicken Sie unten auf ein Ereignis, um eine Benachrichtigung für diesen Auslöser hinzuzufügen.",
    addButton: "Benachrichtigung hinzufügen",
    triggerLabel: "Auslöser",
    selectTrigger: "Auslöser auswählen",
    typeLabel: "Benachrichtigungstyp",
    selectNotification: "Benachrichtigung auswählen",
  },

  message_webhookPageTitle: "Webhooks",
  message_webhookListTitle: "Webhooks",
  message_webhookDetailTitle: "Webhook-Details",
  message_addWebhookBtn: "Hinzufügen",
  message_webhookEnabledLabel: "Aktiviert",
  message_webhookPluginCfgTitle: "Plugin-Konfiguration",
  message_webhookSaveBtn: "Speichern",
  message_webhookCreateBtn: "Webhook erstellen",
  message_webhookDeleteBtn: "Löschen",
  message_webhookPostUrlLabel: "POST-URL",
  message_webhookPostUrlHelp:
    "Wenn eine HTTP-POST-Anfrage an diese URL empfangen wird, erhält das unten ausgewählte Webhook-Plugin die Daten.",
  message_webhookPostUrlPlaceholder:
    "Die URL wird erzeugt, nachdem der Webhook erstellt wurde",
  message_webhookNameLabel: "Name",
  message_webhookUserLabel: "Benutzer",
  message_webhookRolesLabel: "Rollen",
  message_webhookAuthLabel: "HTTP-Autorisierungszeichenfolge",
  message_webhookGenerateSecurityLabel: "Authorization-Header verwenden",
  message_webhookPluginLabel: "Webhook-Plugin auswählen",
  message_webhookFilterListPlaceholder: "Webhooks filtern",
  message_webhookTabGeneral: "Allgemein",
  message_webhookTabHandlerConfiguration: "Handler-Konfiguration",
  message_webhookButtonRegenerate: "Neu generieren",
  message_webhookNewHookName: "Neuer Webhook",

  "edit.configuration": "Konfiguration bearbeiten",
  "edit.message.of.the.day": "Meldung des Tages bearbeiten",
  "edit.readme": "Readme bearbeiten",
  "edit.readme.ellipsis": "Readme bearbeiten…",
  "edit.nodes": "Nodes bearbeiten",
  "edit.nodes.title": "Nodes bearbeiten",
  "gui.menu.Scm": "SCM einrichten",
  "project.admin.menu.Scm.title": "SCM einrichten",
  "export.archive": "Archiv exportieren",
  "import.archive": "Archiv importieren",
  "delete.project": "Projekt löschen",
  "export.archive.ellipsis": "Archiv exportieren…",
  "import.archive.ellipsis": "Archiv importieren…",
  "delete.project.ellipsis": "Projekt löschen…",
  "gui.menu.KeyStorage": "Schlüsselspeicher",
  "gui.menu.AccessControl": "Zugriffskontrolle",
  "access.control": "Zugriffskontrolle",

  browse: "Durchsuchen",
  result: "Ergebnis:",
  actions: "Aktionen",
  filters: "Filter",
  none: "Keine",
  "resource.metadata.entity.tags": "Tags",
  "enter.a.node.filter": "Node-Filter eingeben oder .* für alle Nodes",
  "run.a.command.on.count.nodes.ellipsis":
    "Befehl auf {0} {1} ausführen",
  "create.a.job.for.count.nodes.ellipsis":
    "Job für {0} {1} erstellen",
  "all.nodes": "Alle Nodes",
  "all.nodes.menu.item": "-Alle Nodes-",
  "no.nodes.selected.match.nodes.by.selecting.or.entering.a.filter":
    "Keine Nodes ausgewählt. Wählen Sie Nodes durch Auswahl oder Eingabe eines Filters.",

  resourcesEditor: {
    ...en_US.resourcesEditor,
    "Dispatch to Nodes": "Auf Nodes ausführen",
    "Execute locally": "Lokal ausführen",
    Nodes: "Nodes",
  },
  "job-edit-page": {
    ...(en_US["job-edit-page"] || {}),
    "node-dispatch-true-label": "Auf Nodes über Runner ausführen",
    "node-dispatch-false-label": "Auf Runner ausführen",
    "section-title": "Ausführung",
    "section-title-help": "Runner und zugeordnete Nodes auswählen",
  },

  Workflow: {
    ...en_US.Workflow,
    property: {
      ...(en_US.Workflow?.property || {}),
      keepgoing: {
        ...((en_US.Workflow?.property && en_US.Workflow.property.keepgoing) || {}),
        true: {
          ...((en_US.Workflow?.property &&
            en_US.Workflow.property.keepgoing &&
            en_US.Workflow.property.keepgoing.true) ||
            {}),
          description: "Verbleibende Schritte vor dem Fehlschlag ausführen.",
        },
        false: {
          ...((en_US.Workflow?.property &&
            en_US.Workflow.property.keepgoing &&
            en_US.Workflow.property.keepgoing.false) ||
            {}),
          description: "Beim fehlgeschlagenen Schritt stoppen.",
        },
        prompt: "Wenn ein Schritt fehlschlägt:",
      },
      strategy: {
        ...((en_US.Workflow?.property && en_US.Workflow.property.strategy) || {}),
        label: "Strategie",
      },
    },
    addStep: "Schritt hinzufügen",
    logFilters: "Log-Filter",
    addLogFilter: "Log-Filter hinzufügen",
    clickToEdit: "Zum Bearbeiten klicken",
    edit: "Bearbeiten",
    deleteThisStep: "Diesen Schritt löschen",
    dragToReorder: "Zum Neuordnen ziehen",
    clickOnStepType: "Klicken Sie auf einen Schritttyp, um ihn hinzuzufügen",
    editStep: "Schritt bearbeiten",
    stepLabel: "Schrittbezeichnung",
    noSteps: "Keine Workflow-Schritte",
    addErrorHandler: "Fehlerbehandlung hinzufügen",
    errorHandler: "Fehlerbehandlung",
    editErrorHandler: "Fehlerbehandlung bearbeiten",
  },

  period: {
    ...en_US.period,
    label: {
      ...(en_US.period?.label || {}),
      All: "jederzeit",
      Hour: "in der letzten Stunde",
      Day: "am letzten Tag",
      Week: "in der letzten Woche",
      Month: "im letzten Monat",
    },
  },
  "show.all.nodes": "Alle Nodes anzeigen",
  "scheduledExecution.crontab.tab.simple": "Einfach",
  "scheduledExecution.crontab.tab.crontab": "Crontab",
  "scheduledExecution.crontab.field.hour": "Stunde",
  "scheduledExecution.crontab.field.minute": "Minute",
  "scheduledExecution.crontab.everyDay": "Jeden Tag",
  "scheduledExecution.crontab.everyMonth": "Jeden Monat",
  "scheduledExecution.crontab.help.ranges": "Bereiche:",
  "scheduledExecution.crontab.help.lists": "Listen:",
  "scheduledExecution.crontab.help.increments": "Schritte:",
  "scheduledExecution.crontab.help.increments.description":
    '"alle 15 Einheiten beginnend bei 0".',
  "scheduledExecution.crontab.help.validDayOfWeek":
    "Gültige Werte für Wochentag: 1-7 oder SUN-SAT",
  "scheduledExecution.crontab.help.validMonth":
    "Gültige Werte für Monat: 1-12 oder JAN-DEC",
  "scheduledExecution.crontab.help.see": "Siehe:",
  "scheduledExecution.crontab.help.formatting": "für Hilfe zur Formatierung",
  more: "Mehr…",
  less: "Weniger…",
};

export default messages;
