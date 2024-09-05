const messages = {
  Edit: "Editar",
  Save: "Guardar",
  Delete: "Borrar",
  Cancel: "Cancelar",
  Revert: "Deshacer",
  jobAverageDurationPlaceholder:
    "Dejar en blanco para una duración de trabajo promedio",
  resourcesEditor: {
    "Dispatch to Nodes": "Despachar a Nodos",
    Nodes: "Nodos",
  },
  cron: {
    section: {
      0: "Segundos",
      1: "Minutos",
      2: "Horas",
      3: "Día del Mes",
      4: "Mes",
      5: "Día de la Semana",
      6: "Año",
    },
  },
  message_communityNews: "Noticias de la Comunidad",
  message_connectionError:
    "Parece que ocurrió un error al conectar con las Noticias de la Comunidad.",
  message_readMore: "Leer Más",
  message_refresh: "Por favor, actualice la página o visítenos en",
  message_subscribe: "Suscribirse",
  message_delete: "Borrar este campo",
  message_duplicated: "El campo ya existe",
  message_select: "Seleccionar un Campo",
  message_description: "Descripción",
  message_fieldLabel: "Etiqueta del Campo",
  message_fieldKey: "Clave del Campo",
  message_fieldFilter: "Escriba para filtrar un campo",
  message_empty: "Puede estar vacío",
  message_cancel: "Cancelar",
  message_add: "Agregar",
  message_addField: "Agregar Campo Personalizado",
  message_pageUsersSummary: "Lista de usuarios de Rundeck.",
  message_pageUsersLoginLabel: "Nombre de Usuario",
  message_pageUsersCreatedLabel: "Creado",
  message_pageUsersUpdatedLabel: "Actualizado",
  message_pageUsersLastjobLabel: "Última Ejecución de Trabajo",
  message_domainUserFirstNameLabel: "Nombre",
  message_domainUserLastNameLabel: "Apellido",
  message_domainUserEmailLabel: "Correo Electrónico",
  message_domainUserLabel: "Usuario",
  message_pageUsersTokensLabel: "Nº de Tokens",
  message_pageUsersTokensHelp:
    "Puedes administrar los tokens en la página del Perfil de Usuario.",
  message_pageUsersLoggedStatus: "Estado",
  message_pageUserLoggedOnly: "Solo Usuarios Conectados",
  message_pageUserNotSet: "No Establecido",
  message_pageUserNone: "Ninguno",
  message_pageFilterLogin: "Login",
  message_pageFilterHostName: "Nombre del Host",
  message_pageFilterSessionID: "ID de Sesión",
  message_pageFilterBtnSearch: "Buscar",
  message_pageUsersSessionIDLabel: "ID de Sesión",
  message_pageUsersHostNameLabel: "Nombre del Host",
  message_pageUsersLastLoginInTimeLabel: "Último Inicio de Sesión",
  message_pageUsersTotalFounds: "Total de Usuarios Encontrados",
  message_paramIncludeExecTitle: "Mostrar Última Ejecución",
  message_loginStatus: {
    "LOGGED IN": "Conectado",
    "NOT LOGGED": "Nunca",
    ABANDONED: "Expirado",
    "LOGGED OUT": "Desconectado",
  },
  message_userSummary: {
    desc: "Esta es una lista de Perfiles de Usuario que se han conectado a Rundeck.",
  },
  message_webhookPageTitle: "Webhooks",
  message_webhookListTitle: "Webhooks",
  message_webhookDetailTitle: "Detalle del Webhook",
  message_webhookListNameHdr: "Nombre",
  message_addWebhookBtn: "Agregar",
  message_webhookEnabledLabel: "Habilitado",
  message_webhookPluginCfgTitle: "Configuración del Plugin",
  message_webhookSaveBtn: "Guardar",
  message_webhookCreateBtn: "Crear Webhook",
  message_webhookDeleteBtn: "Borrar",
  message_webhookPostUrlLabel: "URL de Publicación",
  message_webhookPostUrlHelp:
    "Cuando se reciba una solicitud HTTP POST a esta URL, el Plugin Webhook elegido a continuación recibirá los datos.",
  message_webhookPostUrlPlaceholder:
    "La URL se generará después de crear el Webhook",
  message_webhookNameLabel: "Nombre",
  message_webhookUserLabel: "Usuario",
  message_webhookUserHelp:
    "El nombre de usuario de autorización asumido al ejecutar este webhook. Todas las políticas ACL que coincidan con este nombre de usuario se aplicarán.",
  message_webhookRolesLabel: "Roles",
  message_webhookRolesHelp:
    "Los roles de autorización asumidos al ejecutar este webhook (separados por comas). Todas las políticas ACL que coincidan con estos roles se aplicarán.",
  message_webhookAuthLabel: "Cadena de Autorización HTTP",
  message_webhookGenerateSecurityLabel: "Usar Encabezado de Autorización",
  message_webhookGenerateSecretCheckboxHelp:
    "[Opcional] Se puede generar una cadena de autorización de Webhook para aumentar la seguridad de este webhook. Todas las publicaciones deberán incluir la cadena generada en el encabezado de Autorización.",
  message_webhookSecretMessageHelp:
    "Copie esta cadena de autorización ahora. Después de navegar fuera de este webhook, ya no podrá ver la cadena.",
  message_webhookRegenClicked:
    "Se generará y mostrará una nueva cadena de autorización cuando se guarde el webhook.",
  message_webhookPluginLabel: "Elegir Plugin Webhook",
  message_hello: "¡Hola!",
  message_sidebarNotificationText: "Actualización de Rundeck disponible",
  message_updateAvailable: "Actualización Disponible",
  message_updateHasBeenReleased: "Se ha lanzado una actualización de Rundeck.",
  message_installedVersion: "La versión instalada de Rundeck es",
  message_currentVersion: "La versión más reciente de Rundeck es",
  message_getUpdate: "Obtener Actualización",
  message_dismissMessage:
    "Para descartar esta notificación hasta la próxima versión, haga clic aquí.",
  message_close: "Cerrar",
  "bulk.edit": "Edición en Masa",
  "in.of": "en",
  execution: "Ejecución | Ejecuciones",
  "execution.count": "1 Ejecución | {0} Ejecuciones",
  "Bulk Delete Executions: Results":
    "Borrado en Masa de Ejecuciones: Resultados",
  "Requesting bulk delete, please wait.":
    "Solicitando borrado en masa, por favor espere.",
  "bulkresult.attempted.text": "{0} Ejecuciones fueron intentadas.",
  "bulkresult.success.text": "{0} Ejecuciones fueron borradas con éxito.",
  "bulkresult.failed.text": "{0} Ejecuciones no pudieron ser borradas:",
  "delete.confirm.text": "¿Realmente borrar {0} {1}?",
  "clearselected.confirm.text":
    "¿Limpiar todos los {0} elementos seleccionados, o solo los elementos mostrados en esta página?",
  "bulk.selected.count": "{0} seleccionados",
  "results.empty.text": "No hay resultados para la consulta",
  "Only shown executions": "Solo ejecuciones mostradas",
  "Clear bulk selection": "Limpiar Selección en Masa",
  "Click to edit Search Query": "Haga clic para editar la Consulta de Búsqueda",
  "Auto refresh": "Actualización automática",
  "error.message.0": "Ocurrió un Error: {0}",
  "info.completed.0": "Completado: {0}",
  "info.completed.0.1": "Completado: {0} {1}",
  "info.missed.0.1": "Marcado como Perdido: {0} {1}",
  "info.started.0": "Iniciado: {0}",
  "info.started.expected.0.1": "Iniciado: {0}, Finalización Estimada: {1}",
  "info.scheduled.0": "Programado; iniciando {0}",
  "job.execution.starting.0": "Iniciando {0}",
  "job.execution.queued": "En Cola",
  "info.newexecutions.since.0":
    "1 Nuevo Resultado. Haga clic para cargar. | {0} Nuevos Resultados. Haga clic para cargar.",
  "In the last Day": "En el último Día",
  Referenced: "Referenciado",
  "job.has.been.deleted.0": "(El Trabajo {0} ha sido borrado)",
  Filters: "Filtros",
  "filter.delete.named.text": 'Borrar Filtro "{0}"...',
  "Delete Saved Filter": "Borrar Filtro Guardado",
  "filter.delete.confirm.text":
    '¿Está seguro de que desea borrar el Filtro Guardado llamado "{0}"?',
  "filter.save.name.prompt": "Nombre:",
  "filter.save.validation.name.blank": "El Nombre No Puede Estar en Blanco",
  "filter.save.button": "Guardar Filtro...",
  "saved.filters": "Filtros guardados",
  failed: "fallido",
  ok: "ok",
  "0.total": "{0} total",
  period: {
    label: {
      All: "cualquier momento",
      Hour: "en la última Hora",
      Day: "en el último Día",
      Week: "en la última Semana",
      Month: "en el último Mes",
    },
  },
  "empty.message.default":
    "Ninguno configurado. Haga clic en {0} para agregar un nuevo plugin.",
  CreateAcl: "Crear ACL",
  CreateAclName: "Descripción de la ACL",
  CreateAclTitle: "Crear ACL de Almacenamiento de Claves para el proyecto",
  "Edit Nodes": "Editar Nodos",
  Modify: "Modificar",
  "Edit Node Sources": "Editar Fuentes de Nodos",
  "The Node Source had an error": "La Fuente de Nodos tuvo un error",
  "Validation errors": "Errores de validación",
  "unauthorized.status.help.1":
    'Alguna Fuente de Nodos devolvió un mensaje "No Autorizado".',
  "unauthorized.status.help.2":
    "El plugin de la Fuente de Nodos podría necesitar acceso al Recurso de Almacenamiento de Claves. Esto podría habilitarse mediante entradas de Política de Control de Acceso.",
  "unauthorized.status.help.3":
    'Asegúrese de que las políticas ACL permitan "lectura" al Almacenamiento de Claves en este proyecto para la ruta URN del proyecto (urn:project:name).',
  "unauthorized.status.help.4": "Vaya a {0} para crear una ACL de Proyecto",
  "unauthorized.status.help.5": "Vaya a {0} para crear una ACL de Sistema",
  "acl.config.link.title": "Configuraciones del Proyecto > Control de Acceso",
  "acl.config.system.link.title":
    "Configuraciones del Sistema > Control de Acceso",
  "acl.example.summary": "Ejemplo de Política ACL",
  "page.keyStorage.description":
    "El Almacenamiento de Claves proporciona una estructura global similar a un directorio para guardar Claves Públicas y Privadas y Contraseñas, para uso con la autenticación de Ejecución de Nodos.",
  Duplicate: "Duplicar",
  "Node.count.vue": "Nodo | Nodos",
  "bulk.delete": "Borrado en masa",
  "select.none": "Seleccionar ninguno",
  "select.all": "Seleccionar todos",
  "cancel.bulk.delete": "Cancelar Borrado en Masa",
  "delete.selected.executions": "Borrar Ejecuciones seleccionadas",
  "click.to.refresh": "haga clic para actualizar",
  "count.nodes.matched": "{0} {1} Coincidente",
  "count.nodes.shown": "{0} nodos mostrados.",
  "delete.this.filter.confirm": "¿Realmente borrar este filtro?",
  "enter.a.node.filter":
    "Introduzca un filtro de nodo o .* para todos los nodos",
  "execute.locally": "Ejecutar localmente",
  "execution.page.show.tab.Nodes.title": "Nodos",
  "execution.show.mode.Log.title": "Salida de Registro",
  filter: "Filtro:",
  "loading.matched.nodes": "Cargando nodos coincidentes...",
  "loading.text": "Cargando...",
  "loglevel.debug": "Depurar",
  "loglevel.normal": "Normal",
  "matched.nodes.prompt": "Nodos coincidentes",
  no: "No",
  "node.access.not-runnable.message":
    "No tiene acceso para ejecutar comandos en este nodo.",
  "node.filter": "Filtro de Nodo",
  "node.filter.exclude": "Excluir Filtro",
  "node.metadata.os": "Sistema Operativo",
  "node.metadata.status": "Estado",
  nodes: "Nodos",
  "notification.event.onfailure": "En caso de fallo",
  "notification.event.onsuccess": "En caso de éxito",
  "notification.event.onstart": "Al inicio",
  "notification.event.onavgduration": "Duración media excedida",
  "notification.event.onretryablefailure": "En caso de fallo reintentable",
  refresh: "refrescar",
  "save.filter.ellipsis": "Guardar Filtro...",
  "search.ellipsis": "Buscar...",
  "scheduledExecution.property.defaultTab.label": "Pestaña por defecto",
  "scheduledExecution.property.defaultTab.description":
    "Pestaña por defecto para mostrar al seguir una ejecución.",
  "scheduledExecution.property.excludeFilterUncheck.label":
    "Mostrar Nodos Excluidos",
  "scheduledExecution.property.excludeFilterUncheck.description":
    "Si es verdadero, los nodos excluidos se indicarán al ejecutar el Trabajo. De lo contrario, no se mostrarán.",
  "scheduledExecution.property.logOutputThreshold.label":
    "Límite de Salida de Registro",
  "scheduledExecution.property.logOutputThreshold.description":
    'Ingrese el número máximo total de líneas (por ejemplo, "100"), el número máximo de líneas por nodo ("100/nodo") o el tamaño máximo del archivo de registro ' +
    '("100MB", "100KB", etc.), usando "GB", "MB", "KB", "B" como Giga- Mega- Kilo- y bytes.',
  "scheduledExecution.property.logOutputThreshold.placeholder":
    'Por ejemplo, "100", "100/nodo" o "100MB"',
  "scheduledExecution.property.logOutputThresholdAction.label":
    "Acción de Límite de Registro",
  "scheduledExecution.property.logOutputThresholdAction.description":
    "Acción a realizar si se alcanza el límite de salida.",
  "scheduledExecution.property.logOutputThresholdAction.halt.label":
    "Detener con el estado:",
  "scheduledExecution.property.logOutputThresholdAction.truncate.label":
    "Truncar y continuar",
  "scheduledExecution.property.logOutputThresholdStatus.placeholder":
    "'fallido', 'abortado' o cualquier cadena",
  "scheduledExecution.property.loglevel.help":
    "Depurar los niveles produce más registros",
  "scheduledExecution.property.maxMultipleExecutions.label":
    "¿Limitar múltiples ejecuciones?",
  "scheduledExecution.property.maxMultipleExecutions.description":
    "Número máximo de ejecuciones múltiples. Dejar en blanco o 0 para no establecer un límite.",
  "scheduledExecution.property.multipleExecutions.description":
    "¿Permitir que este trabajo se ejecute más de una vez al mismo tiempo?",
  "scheduledExecution.property.nodeKeepgoing.prompt": "Si un nodo falla",
  "scheduledExecution.property.nodeKeepgoing.true.description":
    "Continuar ejecutando en los nodos restantes antes de fallar el paso.",
  "scheduledExecution.property.nodeKeepgoing.false.description":
    "Fallar el paso sin ejecutar en los nodos restantes.",
  "scheduledExecution.property.nodeRankAttribute.label": "Atributo de Rango",
  "scheduledExecution.property.nodeRankAttribute.description":
    "Atributo de nodo para ordenar. El valor predeterminado es el nombre del nodo.",
  "scheduledExecution.property.nodeRankOrder.label": "Orden de Rango",
  "scheduledExecution.property.nodeRankOrder.ascending.label": "Ascendente",
  "scheduledExecution.property.nodeRankOrder.descending.label": "Descendente",
  "scheduledExecution.property.nodeThreadcount.label": "Número de Subprocesos",
  "scheduledExecution.property.nodeThreadcount.description":
    "El número máximo de subprocesos paralelos a usar. (Predeterminado: 1)",
  "scheduledExecution.property.nodefiltereditable.label": "Filtro Editable",
  "scheduledExecution.property.nodesSelectedByDefault.label":
    "Selección de Nodo",
  "scheduledExecution.property.nodesSelectedByDefault.true.description":
    "Los nodos de destino se seleccionan de forma predeterminada",
  "scheduledExecution.property.nodesSelectedByDefault.false.description":
    "El usuario tiene que seleccionar explícitamente nodos de destino",
  "scheduledExecution.property.notifyAvgDurationThreshold.label": "Umbral",
  "scheduledExecution.property.notifyAvgDurationThreshold.description":
    "Umbral de duración opcional para activar las notificaciones. Si no se especifica, se utilizará la duración promedio del trabajo.\n\n" +
    "- porcentaje del promedio: `20%`\n" +
    "- delta de tiempo desde el promedio: `+20s`, `+20`\n" +
    "- tiempo absoluto: `30s`, `5m`\n" +
    "Use `s`,`m`,`h`,`d`,`w`,`y` etc. como unidades de tiempo para segundos, minutos, horas, etc.\n" +
    "La unidad será segundos si no se especifica.\n\n" +
    "Puede incluir referencias de valor de opción como `{'$'}{'{'}option{'.'}avgDurationThreshold{'}'}`.",
  "scheduledExecution.property.orchestrator.label": "Orquestador",
  "scheduledExecution.property.orchestrator.description":
    "Esto se puede utilizar para controlar el orden y el momento en que se procesan los nodos.",
  "scheduledExecution.property.retry.description":
    "Número máximo de veces para reintentar la ejecución cuando se invoque directamente este trabajo. El reintento ocurrirá si el trabajo falla o el tiempo finalice, pero no si es detenido de forma manual. Puede utilizar una referencia de valor de opción como \"{'$'}{'{'}option{'.'}retry{'}'}\".",
  "scheduledExecution.property.retry.delay.description": `El tiempo entre una ejecución fallida y el reintento. Tiempo en segundos, " + 'o especifique unidades: "120m", "2h", "3d". Deje en blanco o 0 para indicar que no hay demora. Puede incluir referencias de valor de opción como \"{'$'}{'{'}option{'.'}delay{'}'}\".`,
  "scheduledExecution.property.successOnEmptyNodeFilter.prompt":
    "Si el conjunto de nodos está vacío",
  "scheduledExecution.property.successOnEmptyNodeFilter.true.description":
    "Continuar la ejecución.",
  "scheduledExecution.property.successOnEmptyNodeFilter.false.description":
    "Fallar el trabajo.",
  "scheduledExecution.property.timeout.description": `El tiempo máximo para una ejecución. Tiempo en segundos, " + 'o especifique unidades: "120m", "2h", "3d". Deje en blanco o 0 para indicar que no hay límite de tiempo. Puede incluir referencias de valor de opción como \"{'$'}{'{'}option{'.'}timeout{'}'}\".`,
  "scheduledExecution.property.scheduleEnabled.description":
    "¿Permitir que este trabajo se programe?",
  "scheduledExecution.property.scheduleEnabled.label":
    "¿Habilitar Programación?",
  "scheduledExecution.property.executionEnabled.description":
    "¿Permitir que este trabajo se ejecute?",
  "scheduledExecution.property.executionEnabled.label": "¿Habilitar Ejecución?",
  "scheduledExecution.property.timezone.prompt": "Zona Horaria",
  "scheduledExecution.property.timezone.description":
    'Una Zona Horaria válida, puede ser una abreviatura como "PST", un nombre completo como "America/Los_Angeles", o un ID personalizado como "GMT-8{\':\'}00".',
  "documentation.reference.cron.url":
    "https{':'}//www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html",
  "set.as.default.filter": "Establecer como Filtro Predeterminado",
  "show.all.nodes": "Mostrar todos los nodos",
  yes: "Sí",
  // job query field labels
  "jobquery.title.titleFilter": "Comando Adhoc",
  "jobquery.title.contextFilter": "Contexto",
  "jobquery.title.actionFilter": "Acción",
  "jobquery.title.maprefUriFilter": "URI de Recurso",
  "jobquery.title.reportIdFilter": "Nombre",
  "jobquery.title.tagsFilter": "Etiquetas",
  "jobquery.title.nodeFilter": "Nodo",
  "jobquery.title.nodeFilter.plural": "Nodos",
  "jobquery.title.messageFilter": "Mensaje",
  "jobquery.title.reportKindFilter": "Tipo de Informe",
  "jobquery.title.recentFilter": "Dentro",
  "jobquery.title.actionTypeFilter": "Tipo de Acción",
  "jobquery.title.itemTypeFilter": "Tipo de Ítem",
  "jobquery.title.filter": "Filtro",
  "jobquery.title.jobFilter": "Nombre del Trabajo",
  "jobquery.title.idlist": "ID del Trabajo",
  "jobquery.title.jobIdFilter": "ID del Trabajo",
  "jobquery.title.descFilter": "Descripción del Trabajo",
  "jobquery.title.objFilter": "Recurso",
  "jobquery.title.scheduledFilter": "Programado",
  "jobquery.title.serverNodeUUIDFilter": "UUID del Nodo del Servidor",
  "jobquery.title.typeFilter": "Tipo",
  "jobquery.title.cmdFilter": "Comando",
  "jobquery.title.userFilter": "Usuario",
  "jobquery.title.projFilter": "Proyecto",
  "jobquery.title.statFilter": "Resultado",
  "jobquery.title.startFilter": "Hora de Inicio",
  "jobquery.title.startbeforeFilter": "Comenzar Antes",
  "jobquery.title.startafterFilter": "Comenzar Después",
  "jobquery.title.endbeforeFilter": "Terminar Antes",
  "jobquery.title.endafterFilter": "Terminar Después",
  "jobquery.title.endFilter": "Hora",
  "jobquery.title.durationFilter": "Duración",
  "jobquery.title.outFilter": "Salida",
  "jobquery.title.objinfFilter": "Información del Recurso",
  "jobquery.title.cmdinfFilter": "Información del Comando",
  "jobquery.title.groupPath": "Grupo",
  "jobquery.title.summary": "Resumen",
  "jobquery.title.duration": "Duración",
  "jobquery.title.loglevelFilter": "Nivel de Registro",
  "jobquery.title.loglevelFilter.label.DEBUG": "Depurar",
  "jobquery.title.loglevelFilter.label.VERBOSE": "Verbose",
  "jobquery.title.loglevelFilter.label.INFO": "Información",
  "jobquery.title.loglevelFilter.label.WARN": "Advertencia",
  "jobquery.title.loglevelFilter.label.ERR": "Error",
  "jobquery.title.adhocExecutionFilter": "Tipo de Trabajo",
  "jobquery.title.adhocExecutionFilter.label.true": "Comando",
  "jobquery.title.adhocExecutionFilter.label.false": "Comando Definido",
  "jobquery.title.adhocLocalStringFilter": "Contenido del Script",
  "jobquery.title.adhocRemoteStringFilter": "Comando Shell",
  "jobquery.title.adhocFilepathFilter": "Ruta del Archivo del Script",
  "jobquery.title.argStringFilter": "Argumentos del Archivo del Script",
  "page.unsaved.changes": "Tienes cambios no guardados",
  "edit.nodes.file": "Editar Archivo de Nodos",
  "project.node.file.source.label": "Fuente",
  "file.display.format.label": "Formato",
  "project.node.file.source.description.label": "Descripción",
  "project.nodes.edit.save.error.message": "Error al Guardar el Contenido:",
  "project.nodes.edit.empty.description":
    "Nota: No había contenido disponible.",
  "button.action.Cancel": "Cancelar",
  "button.action.Save": "Guardar",
  "job-edit-page": {
    "nodes-tab-title": "Nodos & Runners",
    "node-dispatch-true-label": "Despachar a Nodos a través del Runner",
    "node-dispatch-false-label": "Ejecutar en el Runner",
    "section-title": "Despacho",
    "section-title-help": "Elija el Runner y sus Nodos seleccionados",
  },
  "job-exec-page": {
    "nodes-tab-title": "Runner/Nodos",
  },
  JobRunnerEdit: {
    section: {
      title: "Conjunto de Runners",
    },
  },
  gui: {
    menu: {
      Nodes: "Nodos",
    },
  },
  search: "Buscar",
  browse: "Navegar",
  result: "Resultado:",
  actions: "Acciones",
  none: "Ninguno",
  set: {
    "all.nodes.as.default.filter":
      "Establecer Todos los Nodos como Filtro Predeterminado",
    "as.default.filter": "Establecer como Filtro Predeterminado",
  },
  remove: {
    "all.nodes.as.default.filter":
      "Eliminar Todos los Nodos como Filtro Predeterminado",
    "default.filter": "Eliminar Filtro Predeterminado",
  },
  "run.a.command.on.count.nodes.ellipsis": "Ejecutar un comando en {0} {1}",
  "create.a.job.for.count.nodes.ellipsis": "Crear un trabajo para {0} {1}",
  "resource.metadata.entity.tags": "Etiquetas",
  filters: "Filtros",
  "all.nodes": "Todos los Nodos",
  "delete.this.filter.ellipsis": "Eliminar este Filtro ...",
  "enter.a.filter": "Introduzca un Filtro",
  "remove.all.nodes.as.default.filter":
    "Eliminar Todos los Nodos como Filtro Predeterminado",
  "set.all.nodes.as.default.filter":
    "Establecer Todos los Nodos como Filtro Predeterminado",
  "not.authorized": "No autorizado",
  "disabled.execution.run": "Las ejecuciones están deshabilitadas.",
  "user.at.host": "Usuario {'@'} Host",
  "node.changes.success": "Los cambios en el nodo se guardaron con éxito.",
  "node.changes.notsaved": "Los cambios en el nodo no se guardaron.",
  "node.remoteEdit.edit": "Editar nodo:",
  "node.remoteEdit.continue": "Continuar...",
  node: "Nodo",
  "this.will.select.both.nodes": "Esto seleccionará ambos nodos.",
  "node.metadata.hostname": "Nombre del Host",
  "select.nodes.by.name": "Seleccionar nodos por nombre",
  "filter.nodes.by.attribute.value": "Filtrar nodos por valor de atributo",
  "use.regular.expressions": "Usar Expresiones Regulares:",
  "regex.syntax.checking": "Verificación de sintaxis de regex",
  "edit.ellipsis": "Editar...",
  "node.metadata.username-at-hostname": "Usuario & Host",
  "node.metadata.osFamily": "Familia del SO",
  "node.metadata.osName": "Nombre del SO",
  "node.metadata.osArch": "Arquitectura del SO",
  "node.metadata.osVersion": "Versión del SO",
  "node.metadata.type": "Tipo",
  "node.metadata.username": "Nombre de Usuario",
  "node.metadata.tags": "Etiquetas",
  "button.Edit.label": "Editar",
  "default.paginate.prev": "-",
  "default.paginate.next": "+",
  "jump.to": "Saltar a",
  "per.page": "Por Página",
  "remove.default.filter": "Eliminar Filtro Predeterminado",
  "scheduledExecution.action.edit.button.label": "Editar este Trabajo…",
  "scheduledExecution.action.duplicate.button.label": "Duplicar este Trabajo…",
  "scheduledExecution.action.duplicate.other.button.label":
    "Duplicar este Trabajo a otro Proyecto…",
  "scheduledExecution.action.download.button.label": "Descargar Definición",
  "scheduledExecution.action.downloadformat.button.label":
    "Descargar definición del trabajo en {0}",
  "scheduledExecution.action.delete.button.label": "Eliminar este Trabajo",
  "scheduledExecution.action.edit.button.tooltip": "Editar este Trabajo",
  "scheduledExecution.action.duplicate.button.tooltip": "Duplicar Trabajo",
  "enable.schedule.this.job": "Habilitar Programación",
  "disable.schedule.this.job": "Deshabilitar Programación",
  "scheduledExecution.action.enable.schedule.button.label":
    "Habilitar Programación",
  "scheduledExecution.action.disable.schedule.button.label":
    "Deshabilitar Programación",
  "scheduleExecution.schedule.disabled":
    "La programación del trabajo está deshabilitada",
  "enable.execution.this.job": "Habilitar Ejecución",
  "disable.execution.this.job": "Deshabilitar Ejecución",
  "scheduledExecution.action.enable.execution.button.label":
    "Habilitar Ejecución",
  "scheduledExecution.action.disable.execution.button.label":
    "Deshabilitar Ejecución",
  "scheduleExecution.execution.disabled":
    "La ejecución del trabajo está deshabilitada",
  "delete.this.job": "Eliminar este Trabajo",
  "action.prepareAndRun.tooltip": "Elegir opciones y Ejecutar Trabajo…",
  "job.bulk.modify.confirm.panel.title":
    "Confirmar Modificación en Masa del Trabajo",
  "job.bulk.delete.confirm.message":
    "¿Realmente eliminar los Trabajos seleccionados?",
  "job.bulk.disable_schedule.confirm.message":
    "¿Deshabilitar programaciones para todos los Trabajos seleccionados?",
  "job.bulk.enable_schedule.confirm.message":
    "¿Habilitar programaciones para todos los Trabajos seleccionados?",
  "job.bulk.disable_execution.confirm.message":
    "¿Deshabilitar ejecución para todos los Trabajos seleccionados?",
  "job.bulk.enable_execution.confirm.message":
    "¿Habilitar ejecución para todos los Trabajos seleccionados?",
  "job.bulk.disable_schedule.button": "Deshabilitar Programaciones",
  "job.bulk.enable_schedule.button": "Habilitar Programaciones",
  "job.bulk.delete.button": "Eliminar Trabajos",
  "job.bulk.disable_execution.button": "Deshabilitar Ejecución",
  "job.bulk.enable_execution.button": "Habilitar Ejecución",
  "job.bulk.enable_execution.success":
    "Ejecución habilitada para {0} trabajos.",
  "job.bulk.enable_schedule.success":
    "Programación habilitada para {0} trabajos.",
  "job.bulk.disable_schedule.success":
    "Programación deshabilitada para {0} trabajos.",
  "job.bulk.disable_execution.success":
    "Ejecución deshabilitada para {0} trabajos.",
  "job.bulk.delete.success": "Eliminados {0} trabajos.",
  "delete.selected.jobs": "Eliminar Trabajos Seleccionados",
  "job.bulk.panel.select.title": "Seleccionar Trabajos para Edición en Masa",
  "job.bulk.perform.action.menu.label": "Realizar Acción",
  "job.create.button": "Crear un nuevo Trabajo",
  "job.upload.button.title": "Subir una definición de Trabajo",
  cancel: "Cancelar",
  "job.actions": "Acciones del Trabajo",
  "job.bulk.activate.menu.label": "Edición en Masa…",
  "job.bulk.deactivate.menu.label": "Salir del Modo de Edición en Masa",
  "upload.definition.button.label": "Subir Definición",
  "new.job.button.label": "Nuevo Trabajo",
  "job.bulk.panel.select.message":
    "{n} Trabajo Seleccionado | {n} Trabajos Seleccionados",
  "cannot.run.job": "No se puede ejecutar el trabajo",
  "disabled.schedule.run": "Las ejecuciones están deshabilitadas.",
  "disabled.job.run": "Las ejecuciones están deshabilitadas.",
  "schedule.on.server.x.at.y":
    "Programado para ejecutarse en el servidor {0} a las {1}",
  "schedule.time.in.future": "en {0}",
  never: "Nunca",
  disabled: "Deshabilitado",
  "project.schedule.disabled":
    "La programación del proyecto está deshabilitada",
  "project.execution.disabled": "La ejecución del proyecto está deshabilitada",
  "job.schedule.will.never.fire":
    "La programación del trabajo nunca se activará",
  "scm.import.status.UNKNOWN.display.text":
    "Estado de Importación: No Rastreado",
  "scm.import.status.LOADING.description":
    "Importación: El estado del trabajo se está cargando",
  "scm.export.status.DELETED.display.text": "Eliminado",
  "scm.export.status.EXPORT_NEEDED.display.text": "Modificado",
  "scm.export.status.CLEAN.description": "Estado de Exportación: Limpio",
  "scm.import.status.DELETE_NEEDED.title.text":
    "Importación: Archivos eliminados",
  "scm.export.status.EXPORT_NEEDED.title.text": "Exportación: Modificado",
  "scm.import.status.UNKNOWN.description": "No Rastreado para Importación SCM",
  "scm.import.status.REFRESH_NEEDED.display.text": "Sincronización Necesaria",
  "scm.export.status.CREATE_NEEDED.description":
    "Estado de Exportación: Nuevo Trabajo, Aún no agregado a SCM",
  "scm.import.status.DELETE_NEEDED.description":
    "Estado de Importación: El archivo fuente fue eliminado",
  "scm.export.status.CLEAN.display.text": "Sin Cambios",
  "scm.export.status.LOADING.description":
    "Exportación: El estado del trabajo se está cargando",
  "scm.import.status.IMPORT_NEEDED.description":
    "Estado de Importación: Los cambios en el trabajo necesitan ser recuperados",
  "scm.import.status.REFRESH_NEEDED.title.text":
    "Importación: Sincronizar cambios",
  "scm.export.status.REFRESH_NEEDED.title.text":
    "Exportación: Los cambios remotos necesitan ser sincronizados",
  "scm.export.status.LOADING.display.text": "Cargando",
  "scm.export.status.EXPORT_NEEDED.description":
    "Estado de Exportación: Modificado",
  "scm.export.status.CREATE_NEEDED.display.text": "Creado",
  "scm.import.status.IMPORT_NEEDED.display.text": "Importación Necesaria",
  "scm.import.status.DELETE_NEEDED.display.text":
    "El archivo fuente fue eliminado",
  "scm.import.status.IMPORT_NEEDED.title.text":
    "Importación: Cambios entrantes",
  "scm.import.status.CLEAN.display.text": "Actualizado",
  "scm.import.status.CLEAN.description": "Estado de Importación: Actualizado",
  "scm.import.status.REFRESH_NEEDED.description":
    "Estado de Importación: Los cambios en el trabajo necesitan ser recuperados",
  "scm.export.status.REFRESH_NEEDED.display.text": "Sincronización Necesaria",
  "scm.import.status.LOADING.display.text": "Cargando",
  "scm.export.status.ERROR.display.text": "Ocurrió un error desconocido.",
  "scm.import.status.ERROR.display.text": "Ocurrió un error desconocido.",
  "scm.status.ERROR.display.text": "Error de SCM",
  "scm.export.auth.key.noAccess":
    "El usuario no tiene acceso a la clave o contraseña especificada",
  "scm.import.auth.key.noAccess":
    "El usuario no tiene acceso a la clave o contraseña especificada",
  "scm.action.diff.clean.button.label": "Ver Información del Commit",
  "scm.import.plugin": "Plugin de Importación SCM",
  "scm.action.diff.button.label": "Diferencias de Cambios",
  "scm.export.commit.job.link.title":
    "Haga clic para commitar o agregar este Trabajo",
  "scm.export.commit.link.title": "Haga clic para commitar o agregar cambios",
  "scm.export.plugin": "Plugin de Exportación SCM",
  "job.toggle.scm.menu.on": "Activar SCM",
  "job.toggle.scm.menu.off": "Desactivar SCM",
  "scm.import.actions.title": "Acciones de Importación SCM",
  "scm.export.actions.title": "Acciones de Exportación SCM",
  "scm.export.title": "Exportación SCM",
  "scm.import.title": "Importación SCM",
  "job.toggle.scm.button.label.off": "Desactivar SCM",
  "job.toggle.scm.confirm.panel.title": "Confirmar Modificación de SCM",
  "job.toggle.scm.confirm.on":
    "¿Habilitar todos los plugins configurados de SCM?",
  "job.toggle.scm.confirm.off":
    "¿Deshabilitar todos los plugins configurados de SCM?",
  "job.toggle.scm.button.label.on": "Habilitar SCM",
  "job.scm.status.loading.message": "Cargando Estado de SCM...",
  "page.section.Activity.for.jobs": "Actividad para Trabajos",
  "widget.theme.title": "Tema",
  "widget.nextUi.title": "Habilitar Próxima UI",
  "page.section.title.AllJobs": "Todos los Trabajos",
  "advanced.search": "Avanzado",
  "jobs.advanced.search.title": "Haga clic para modificar el filtro",
  "filter.jobs": "Buscar Trabajos",
  "job.filter.quick.placeholder": "Buscar",
  "job.filter.apply.button.title": "Buscar",
  "job.filter.clear.button.title": "Limpiar Búsqueda",
  all: "Todos",
  "job.tree.breakpoint.hit.info":
    "Aviso: No se cargaron todos los detalles del Trabajo porque este grupo contiene demasiados trabajos. Haga clic en el botón para cargar los detalles faltantes.",
  "job.tree.breakpoint.load.button.title":
    "Cargar Todos los Detalles del Trabajo",
  "job.list.filter.save.modal.title": "Guardar Filtro",
  "job.filter.save.button.title": "Guardar como Filtro…",
  "job.list.filter.save.button": "Guardar Filtro",
  "job.list.filter.delete.filter.link.text": 'Eliminar Filtro "{0}"',
  "app.firstRun.title": "Bienvenido a {0} {1}",
  "you.can.see.this.message.again.by.clicking.the":
    "Puede ver este mensaje nuevamente haciendo clic en el",
  "version.number": "número de versión",
  "in.the.page.footer": "en el pie de página.",
  "no.authorized.access.to.projects":
    "No tiene acceso autorizado a los proyectos.",
  "no.authorized.access.to.projects.contact.your.administrator.user.roles.0":
    "Contacte a su administrador. (Roles de usuario: {0})",
  "page.home.loading.projects": "Cargando Proyectos",
  "app.firstRun.md":
    "Gracias por ser un suscriptor de {0}.\n\n" +
    "  \n\n\n" +
    "* [{0} Portal de Soporte &raquo;](http://support.rundeck.com)\n\n" +
    "* [{0} Documentación &raquo;]({1})",
  "page.home.section.project.title": "{0} Proyecto",
  "page.home.section.project.title.plural": "{0} Proyectos",
  "page.home.duration.in.the.last.day": "En el último día",
  by: "por",
  user: "Usuario",
  "user.plural": "Usuarios",
  "page.home.project.executions.0.failed.parenthetical": "({0} Fallido)",
  "page.home.search.projects.input.placeholder":
    "Búsqueda de proyecto: nombre, etiqueta o /regex/",
  "page.home.search.project.title":
    "{n} Proyecto encontrado | {n} Proyectos encontrados",
  "button.Action": "Acción",
  "edit.configuration": "Editar Configuración",
  "page.home.new.project.button.label": "Nuevo Proyecto",
  Execution: "{n} Ejecuciones | {n} Ejecución | {n} Ejecuciones",
  in: "en",
  "Project.plural": "Proyectos",
  discard: "Descartar",
  "commandline.arguments.prompt.unquoted":
    "Argumentos de Línea de Comando (sin comillas):",
  usage: "Uso",
  "form.label.valuesType.list.label": "Lista",
  "scheduledExecution.option.unsaved.warning":
    "Descartar o guardar los cambios en esta opción antes de completar los cambios en el trabajo",
  "bash.prompt": "Bash:",
  "script.content.prompt": "Contenido del Script:",
  "rundeck.user.guide.option.model.provider":
    "Guía del Usuario de Rundeck - Proveedor de modelo de opción",
  save: "Guardar",
  "commandline.arguments.prompt": "Argumentos de Línea de Comando:",
  "commandline.arguments.prompt.unquoted.warning":
    "¡Advertencia! Confiar en argumentos sin comillas podría hacer que este trabajo sea vulnerable a la inyección de comandos. Úselo con precaución.",
  "add.new.option": "Agregar Nueva Opción",
  "add.an.option": "Agregar una opción",
  "option.values.c": "1 Valor|{n} Valores",
  "no.options.message": "Sin Opciones",
  "the.option.values.will.be.available.to.scripts.in.these.forms":
    "Los valores de la opción estarán disponibles para scripts en estas formas:",
  "form.option.date.label": "Fecha",
  "form.option.enforcedType.label": "Restricciones",
  "form.option.usage.file.fileName.preview.description":
    "El nombre original del archivo:",
  "form.option.discard.title": "Descartar cambios en la opción",
  "form.option.valuesType.url.authentication.password.label": "Contraseña",
  "form.option.enforcedType.none.label": "Cualquier valor puede ser usado",
  "form.option.inputType.label": "Tipo de Entrada",
  "form.option.defaultStoragePath.present.description":
    "Un valor predeterminado se cargará desde el Almacenamiento de Claves",
  "form.option.secureInput.false.label": "Texto Plano",
  "form.option.name.label": "Nombre de la Opción",
  "form.option.valuesType.url.filter.label": "Filtro de Ruta JSON",
  "form.option.valuesType.url.authType.label": "Tipo de Autenticación",
  "form.option.sort.description": "Ordenar lista de Valores Permitidos",
  "form.option.usage.secureAuth.message":
    "Los valores de opción de autenticación segura no están disponibles para scripts o comandos",
  "form.option.valuesType.url.label": "URL Remota",
  "form.option.valuesList.placeholder":
    "Lista separada por delimitador (coma por defecto)",
  "form.option.secureInput.description":
    "Los valores de entrada segura no son almacenados por Rundeck después de su uso. Si el valor expuesto se usa en un script o comando, el registro de salida puede contener el valor.",
  "form.option.valuesType.url.authentication.key.label": "Clave",
  "form.option.date.description":
    "La fecha se pasará a su trabajo como una cadena formateada de esta manera: mm/dd/aa HH:MM",
  "form.option.valuesType.url.authentication.username.label":
    "Nombre de Usuario",
  "form.option.valuesType.url.authType.bearerToken.label": "Token Bearer",
  "form.option.enforced.label": "Impuesto a partir de Valores Permitidos",
  "form.option.description.label": "Descripción",
  "form.option.save.title": "Guardar cambios en la opción",
  "form.option.type.label": "Tipo de Opción",
  "form.option.multivalueAllSelected.label":
    "Seleccionar Todos los Valores por Defecto",
  "form.option.secureExposed.false.label": "Autenticación Remota Segura",
  "form.option.valuesType.url.authentication.token.label": "Token",
  "form.option.delimiter.label": "Delimitador",
  "form.option.valuesURL.placeholder": "URL Remota",
  "form.option.valuesType.url.authType.empty.label":
    "Seleccionar Tipo de Autenticación",
  "form.option.valuesDelimiter.description":
    "Establecer el delimitador para Valores Permitidos",
  "form.option.usage.file.preview.description":
    "La ruta del archivo local estará disponible para scripts en estas formas:",
  "form.option.secureExposed.false.description":
    "Entrada de contraseña, valor no expuesto en scripts o comandos, usado solo por Node Executors para autenticación.",
  "form.option.valuesType.url.filter.error.label":
    "El Filtro de Ruta JSON de la URL Remota tiene una sintaxis inválida",
  "form.option.valuesType.url.authType.apiKey.label": "Clave API",
  "form.option.optionType.text.label": "Texto",
  "form.option.secureExposed.true.label":
    "Texto Plano con Entrada de Contraseña",
  "form.option.valuesType.url.filter.description":
    'Filtrar resultados JSON usando una ruta de clave, por ejemplo "$.key.path"',
  "form.option.valuesType.url.authentication.tokenInformer.header.label":
    "Encabezado",
  "form.option.defaultStoragePath.description":
    "Ruta de Almacenamiento de Claves para un valor de contraseña predeterminado",
  "form.option.multivalued.label": "Multi-valores",
  "form.option.multivalued.description":
    "Permitir que se elijan múltiples valores de entrada.",
  "form.option.valuesType.url.authentication.tokenInformer.query.label":
    "Parámetro de Consulta",
  "form.option.create.title": "Guardar la nueva opción",
  "form.option.regex.label": "Expresión Regular de Coincidencia",
  "form.option.optionType.file.label": "Archivo",
  "form.option.valuesDelimiter.label": "Delimitador de Lista",
  "form.option.cancel.title": "Cancelar la adición de nueva opción",
  "form.option.values.label": "Valores Permitidos",
  "form.option.dateFormat.description.md":
    "Ingrese un formato de fecha como se describe en la [documentación de momentjs](http://momentjs.com/docs/#/displaying/format/)",
  "form.option.defaultStoragePath.label": "Ruta de Almacenamiento",
  "form.option.defaultValue.label": "Valor Predeterminado",
  "form.option.delimiter.description":
    "El delimitador se usará para unir todos los valores de entrada. Puede ser cualquier cadena: ' ' (espacio), ',' (coma), etc. Nota: no incluya comillas.",
  "form.option.secureInput.false.description": "Entrada de texto plano.",
  "form.option.valuesType.url.authType.basic.label": "Básico",
  "form.option.valuesType.url.authentication.tokenInformer.label":
    "Inyectar clave",
  "form.option.secureExposed.true.description":
    "Texto plano con una entrada de contraseña, valor expuesto en scripts y comandos.",
  "form.option.dateFormat.title": "Formato de Fecha",
  "form.option.label.label": "Etiqueta de la Opción",
  "form.option.valuesUrl.description": "Una URL a un servicio JSON Remoto.",
  "form.option.multivalued.secure-conflict.message":
    "Las opciones de entrada segura no permiten múltiples valores",
  "form.option.sort.label": "Ordenar Valores",
  "form.option.usage.file.sha.preview.description":
    "El valor SHA-256 del contenido del archivo:",
  "Option.property.description.description":
    "La descripción se renderizará con Markdown.",
  "option.defaultValue.regexmismatch.message":
    'El valor predeterminado "{0}" no coincide con la regex: {1}',
  "option.multivalued.secure-conflict.message":
    "La entrada segura no puede ser usada con entrada de múltiples valores",
  "option.defaultValue.notallowed.message":
    "El Valor Predeterminado no estaba en la lista de valores permitidos, y los valores son impuestos",
  "option.enforced.secure-conflict.message":
    "La entrada segura no puede ser usada con valores impuestos",
  "option.file.config.disabled.message":
    "El plugin de configuración del tipo de opción de archivo no está habilitado",
  "option.defaultValue.required.message":
    "Especifique un Valor Predeterminado para opciones obligatorias cuando el Trabajo esté programado.",
  "option.enforced.emptyvalues.message":
    "Los valores permitidos (lista o URL remota) deben ser especificados si los valores son impuestos",
  "option.file.required.message":
    "El tipo de opción de archivo no puede ser Obligatorio cuando el Trabajo esté programado.",
  "option.regex.invalid.message": "Expresión Regular Inválida: {0}",
  "option.file.config.invalid.message":
    "La configuración del tipo de opción de archivo no es válida: {0}",
  "option.delimiter.blank.message":
    "Debe especificar un delimitador para opciones de múltiples valores",
  "option.hidden.notallowed.message":
    "Las opciones ocultas deben tener un valor predeterminado o una ruta de almacenamiento.",
  "option.values.regexmismatch.message":
    'El valor permitido "{0}" no coincide con la regex: {1}',
  "option.defaultValue.multivalued.notallowed.message":
    'El Valor Predeterminado contiene una cadena que no estaba en la lista de valores permitidos, y los valores son impuestos: "{0}". Nota: los espacios en blanco son significativos.',
  "Option.required.label": "Requerido",
  "Option.hidden.description":
    "Debe estar oculto en la página de ejecución del trabajo",
  "Option.required.description":
    "Requerir que esta opción tenga un valor no vacío al ejecutar el Trabajo",
  "Option.hidden.label": "Debe estar oculto",
  "form.option.regex.placeholder": "Ingrese una Expresión Regular",
  "home.user": "{n} Usuarios | {n} Usuario | {n} Usuarios",
  "home.table.projects": "Proyectos",
  "home.table.activity": "Actividad",
  "home.table.actions": "Acciones",
  "option.click.to.edit.title": "Haga clic para editar",
  "form.option.regex.validation.error":
    "Valor inválido: Debe coincidir con el patrón: {0}",
  "form.field.required.message": "Este campo es obligatorio",
  "form.field.too.long.message":
    "Este valor no puede tener más de {max} caracteres",
  "form.option.validation.errors.message":
    "Corrija los errores de validación antes de guardar los cambios",
  "option.list.header.name.title": "Nombre",
  "option.list.header.values.title": "Valores",
  "option.list.header.restrictions.title": "Restricciones",
  "util.undoredo.undo": "Deshacer",
  "util.undoredo.redo": "Rehacer",
  "util.undoredo.revertAll": "Revertir Todos los Cambios",
  "util.undoredo.revertAll.confirm": "¿Realmente revertir todos los cambios?",
  "option.view.required.title": " (Requerido)",
  "option.view.allowedValues.label": "Valores Permitidos",
  "option.view.valuesUrl.title": "Valores cargados desde la URL Remota: {0}",
  "option.view.valuesUrl.placeholder": "URL",
  "option.view.enforced.title":
    "La entrada debe ser uno de los valores permitidos",
  "option.view.enforced.placeholder": "Estricto",
  "option.view.regex.info.note":
    "Los valores deben coincidir con la expresión regular:",
  "option.view.notenforced.title": "Sin restricciones en el valor de entrada",
  "option.view.notenforced.placeholder": "Ninguno",
  "option.view.action.delete.title": "Eliminar esta Opción",
  "option.view.action.edit.title": "Editar esta Opción",
  "option.view.action.duplicate.title": "Duplicar esta Opción",
  "option.view.action.moveUp.title": "Mover hacia Arriba",
  "option.view.action.moveDown.title": "Mover hacia Abajo",
  "option.view.action.drag.title": "Arrastrar para reordenar",
  "pagination.of": "de",
  uiv: {
    datePicker: {
      clear: "Limpiar",
      today: "Hoy",
      month: "Mes",
      month1: "Enero",
      month2: "Febrero",
      month3: "Marzo",
      month4: "Abril",
      month5: "Mayo",
      month6: "Junio",
      month7: "Julio",
      month8: "Agosto",
      month9: "Septiembre",
      month10: "Octubre",
      month11: "Noviembre",
      month12: "Diciembre",
      year: "Año",
      week1: "Lun",
      week2: "Mar",
      week3: "Mié",
      week4: "Jue",
      week5: "Vie",
      week6: "Sáb",
      week7: "Dom",
    },
    timePicker: {
      am: "AM",
      pm: "PM",
    },
    modal: {
      cancel: "Cancelar",
      ok: "OK",
    },
    multiSelect: {
      placeholder: "Seleccionar...",
      filterPlaceholder: "Buscar...",
    },
  },
  "scheduledExecution.jobName.label": "Nombre del Trabajo",
  "scheduledExecution.property.description.label": "Descripción",
  "job.editor.preview.runbook": "Vista Previa del Readme",
  "choose.action.label": "Elegir",
  "scheduledExecution.property.description.plain.description":
    "La descripción se mostrará en texto plano",
  "scheduledExecution.property.description.description":
    "La primera línea de la descripción se mostrará en texto plano, el resto se renderizará con Markdown.\n\n" +
    "Ver [Markdown](http://en.wikipedia.org/wiki/Markdown).\n\n" +
    "Dentro de la descripción extendida, puede vincular al trabajo usando {'`{{job.permalink}}`'} como la URL al trabajo, por ejemplo, `[ejecutar trabajo]({'{{job.permalink}}#runjob'})`\n\n" +
    "Puede agregar un Readme usando un separador HR `---` solo en una línea, y todo lo que siga se renderizará en una pestaña separada usando [Markdeep](https://casual-effects.com/markdeep).",
  "scheduledExecution.groupPath.description":
    "El grupo es una ruta separada por /",
  more: "Más…",
  less: "Menos…",
  "job.edit.groupPath.choose.text":
    "Haga clic en el nombre del grupo para usar",
  "scheduledExecution.property.executionLifecyclePluginConfig.help.text":
    "Los Plugins seleccionados se habilitarán para este Trabajo.",
  Workflow: {
    label: "Flujo de Trabajo",
    property: {
      keepgoing: {
        true: { description: "Ejecutar los pasos restantes antes de fallar." },
        false: { description: "Detener en el paso fallido." },
        prompt: "Si un paso falla:",
      },
      strategy: {
        label: "Estrategia",
      },
    },
  },
  "plugin.choose.title": "Elegir un Plugin",
  "plugin.type.WorkflowStep.title.plural": "Pasos del Flujo de Trabajo",
  "plugin.type.WorkflowStep.title": "Paso del Flujo de Trabajo",
  "plugin.type.WorkflowNodeStep.title.plural":
    "Pasos del Nodo del Flujo de Trabajo",
  "plugin.type.WorkflowNodeStep.title": "Paso del Nodo del Flujo de Trabajo",
  "JobExec.nodeStep.true.label": "Paso del Nodo",
};

export default messages;
