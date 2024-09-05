const messages = {
  Edit: "Editar",
  Save: "Salvar",
  Delete: "Excluir",
  Cancel: "Cancelar",
  Revert: "Reverter",
  jobAverageDurationPlaceholder:
    "deixe em branco para a duração média do trabalho",
  resourcesEditor: {
    "Dispatch to Nodes": "Despachar para Nós",
    Nodes: "Nós",
  },
  cron: {
    section: {
      0: "Segundos",
      1: "Minutos",
      2: "Horas",
      3: "Dia do Mês",
      4: "Mês",
      5: "Dia da Semana",
      6: "Ano",
    },
  },
  message_communityNews: "Notícias da Comunidade",
  message_connectionError:
    "Parece que ocorreu um erro ao conectar-se às Notícias da Comunidade.",
  message_readMore: "Leia Mais",
  message_refresh: "Por favor, atualize a página ou visite-nos em",
  message_subscribe: "Inscrever-se",
  message_delete: "Excluir este campo",
  message_duplicated: "Campo já existe",
  message_select: "Selecione um Campo",
  message_description: "Descrição",
  message_fieldLabel: "Rótulo do Campo",
  message_fieldKey: "Chave do Campo",
  message_fieldFilter: "Digite para filtrar um campo",
  message_empty: "Pode estar vazio",
  message_cancel: "Cancelar",
  message_add: "Adicionar",
  message_addField: "Adicionar Campo Personalizado",
  message_pageUsersSummary: "Lista de usuários do Rundeck.",
  message_pageUsersLoginLabel: "Nome de Usuário",
  message_pageUsersCreatedLabel: "Criado",
  message_pageUsersUpdatedLabel: "Atualizado",
  message_pageUsersLastjobLabel: "Última Execução de Trabalho",
  message_domainUserFirstNameLabel: "Primeiro Nome",
  message_domainUserLastNameLabel: "Último Nome",
  message_domainUserEmailLabel: "Email",
  message_domainUserLabel: "Usuário",
  message_pageUsersTokensLabel: "Nº de Tokens",
  message_pageUsersTokensHelp:
    "Você pode administrar os tokens na página de Perfil do Usuário.",
  message_pageUsersLoggedStatus: "Status",
  message_pageUserLoggedOnly: "Apenas Usuários Conectados",
  message_pageUserNotSet: "Não Definido",
  message_pageUserNone: "Nenhum",
  message_pageFilterLogin: "Login",
  message_pageFilterHostName: "Nome do Host",
  message_pageFilterSessionID: "ID da Sessão",
  message_pageFilterBtnSearch: "Pesquisar",
  message_pageUsersSessionIDLabel: "ID da Sessão",
  message_pageUsersHostNameLabel: "Nome do Host",
  message_pageUsersLastLoginInTimeLabel: "Último Login",
  message_pageUsersTotalFounds: "Total de Usuários Encontrados",
  message_paramIncludeExecTitle: "Mostrar Última Execução",
  message_loginStatus: {
    "LOGGED IN": "Conectado",
    "NOT LOGGED": "Nunca",
    ABANDONED: "Expirado",
    "LOGGED OUT": "Desconectado",
  },
  message_userSummary: {
    desc: "Esta é uma lista de Perfis de Usuários que se conectaram ao Rundeck.",
  },
  message_webhookPageTitle: "Webhooks",
  message_webhookListTitle: "Webhooks",
  message_webhookDetailTitle: "Detalhe do Webhook",
  message_webhookListNameHdr: "Nome",
  message_addWebhookBtn: "Adicionar",
  message_webhookEnabledLabel: "Habilitado",
  message_webhookPluginCfgTitle: "Configuração do Plugin",
  message_webhookSaveBtn: "Salvar",
  message_webhookCreateBtn: "Criar Webhook",
  message_webhookDeleteBtn: "Excluir",
  message_webhookPostUrlLabel: "URL de Postagem",
  message_webhookPostUrlHelp:
    "Quando uma solicitação HTTP POST para esta URL for recebida, o Plugin Webhook escolhido abaixo receberá os dados.",
  message_webhookPostUrlPlaceholder:
    "A URL será gerada após a criação do Webhook",
  message_webhookNameLabel: "Nome",
  message_webhookUserLabel: "Usuário",
  message_webhookUserHelp:
    "O nome de usuário de autorização assumido ao executar este webhook. Todas as políticas ACL correspondentes a este nome de usuário serão aplicadas.",
  message_webhookRolesLabel: "Funções",
  message_webhookRolesHelp:
    "As funções de autorização assumidas ao executar este webhook (separadas por vírgula). Todas as políticas ACL correspondentes a essas funções serão aplicadas.",
  message_webhookAuthLabel: "String de Autorização HTTP",
  message_webhookGenerateSecurityLabel: "Usar Cabeçalho de Autorização",
  message_webhookGenerateSecretCheckboxHelp:
    "[Opcional] Uma string de autorização de Webhook pode ser gerada para aumentar a segurança deste webhook. Todas as postagens precisarão incluir a string gerada no cabeçalho de Autorização.",
  message_webhookSecretMessageHelp:
    "Copie esta string de autorização agora. Depois de navegar para longe deste webhook, você não poderá mais ver a string.",
  message_webhookRegenClicked:
    "Uma nova string de autorização será gerada e exibida quando o webhook for salvo.",
  message_webhookPluginLabel: "Escolha o Plugin Webhook",
  message_hello: "olá mundo",
  message_sidebarNotificationText: "Atualização do Rundeck disponível",
  message_updateAvailable: "Atualização Disponível",
  message_updateHasBeenReleased: "Uma atualização do Rundeck foi lançada.",
  message_installedVersion: "A versão instalada do Rundeck é",
  message_currentVersion: "A versão mais recente do Rundeck é",
  message_getUpdate: "Obter Atualização",
  message_dismissMessage:
    "Para dispensar esta notificação até a próxima versão, clique aqui.",
  message_close: "Fechar",
  "bulk.edit": "Edição em Massa",
  "in.of": "em",
  execution: "Execução | Execuções",
  "execution.count": "1 Execução | {0} Execuções",
  "Bulk Delete Executions: Results":
    "Exclusão em Massa de Execuções: Resultados",
  "Requesting bulk delete, please wait.":
    "Solicitando exclusão em massa, por favor, aguarde.",
  "bulkresult.attempted.text": "{0} Execuções foram tentadas.",
  "bulkresult.success.text": "{0} Execuções foram excluídas com sucesso.",
  "bulkresult.failed.text": "{0} Execuções não puderam ser excluídas:",
  "delete.confirm.text": "Realmente excluir {0} {1}?",
  "clearselected.confirm.text":
    "Limpar todos os {0} itens selecionados, ou apenas os itens mostrados nesta página?",
  "bulk.selected.count": "{0} selecionados",
  "results.empty.text": "Nenhum resultado para a consulta",
  "Only shown executions": "Apenas execuções mostradas",
  "Clear bulk selection": "Limpar Seleção em Massa",
  "Click to edit Search Query": "Clique para editar a Consulta de Pesquisa",
  "Auto refresh": "Atualização automática",
  "error.message.0": "Ocorreu um Erro: {0}",
  "info.completed.0": "Concluído: {0}",
  "info.completed.0.1": "Concluído: {0} {1}",
  "info.missed.0.1": "Marcado como Perdido: {0} {1}",
  "info.started.0": "Iniciado: {0}",
  "info.started.expected.0.1": "Iniciado: {0}, Término Estimado: {1}",
  "info.scheduled.0": "Agendado; iniciando {0}",
  "job.execution.starting.0": "Iniciando {0}",
  "job.execution.queued": "Na Fila",
  "info.newexecutions.since.0":
    "1 Novo Resultado. Clique para carregar. | {0} Novos Resultados. Clique para carregar.",
  "In the last Day": "No último Dia",
  Referenced: "Referenciado",
  "job.has.been.deleted.0": "(O Trabalho {0} foi excluído)",
  Filters: "Filtros",
  "filter.delete.named.text": 'Excluir Filtro "{0}"...',
  "Delete Saved Filter": "Excluir Filtro Salvo",
  "filter.delete.confirm.text":
    'Tem certeza de que deseja excluir o Filtro Salvo chamado "{0}"?',
  "filter.save.name.prompt": "Nome:",
  "filter.save.validation.name.blank": "O Nome Não Pode Estar em Branco",
  "filter.save.button": "Salvar Filtro...",
  "saved.filters": "Filtros salvos",
  failed: "falhou",
  ok: "ok",
  "0.total": "{0} total",
  period: {
    label: {
      All: "qualquer momento",
      Hour: "na última Hora",
      Day: "no último Dia",
      Week: "na última Semana",
      Month: "no último Mês",
    },
  },
  "empty.message.default":
    "Nenhum configurado. Clique em {0} para adicionar um novo plugin.",
  CreateAcl: "Criar ACL",
  CreateAclName: "Descrição da ACL",
  CreateAclTitle: "Criar ACL de Armazenamento de Chaves para o projeto",
  "Edit Nodes": "Editar Nós",
  Modify: "Modificar",
  "Edit Node Sources": "Editar Fontes de Nós",
  "The Node Source had an error": "A Fonte de Nós teve um erro",
  "Validation errors": "Erros de validação",
  "unauthorized.status.help.1":
    'Alguma Fonte de Nós retornou uma mensagem "Não Autorizado".',
  "unauthorized.status.help.2":
    "O plugin da Fonte de Nós pode precisar de acesso ao Recurso de Armazenamento de Chaves. Isso pode ser habilitado por entradas de Política de Controle de Acesso.",
  "unauthorized.status.help.3":
    'Certifique-se de que as políticas ACL permitam "leitura" ao Armazenamento de Chaves neste projeto para o caminho URN do projeto (urn:project:name).',
  "unauthorized.status.help.4": "Vá para {0} para criar uma ACL de Projeto",
  "unauthorized.status.help.5": "Vá para {0} para criar uma ACL de Sistema",
  "acl.config.link.title": "Configurações do Projeto > Controle de Acesso",
  "acl.config.system.link.title":
    "Configurações do Sistema > Controle de Acesso",
  "acl.example.summary": "Exemplo de Política ACL",
  "page.keyStorage.description":
    "O Armazenamento de Chaves fornece uma estrutura global semelhante a um diretório para salvar Chaves Públicas e Privadas e Senhas, para uso com autenticação de Execução de Nós.",
  Duplicate: "Duplicar",
  "Node.count.vue": "Nó | Nós",
  "bulk.delete": "Exclusão em massa",
  "select.none": "Selecionar nenhum",
  "select.all": "Selecionar todos",
  "cancel.bulk.delete": "Cancelar exclusão em massa",
  "delete.selected.executions": "Excluir execuções selecionadas",
  "click.to.refresh": "clique para atualizar",
  "count.nodes.matched": "{0} {1} Correspondido",
  "count.nodes.shown": "{0} nós mostrados.",
  "delete.this.filter.confirm": "Realmente excluir este filtro?",
  "enter.a.node.filter": "Insira um filtro de nó ou .* para todos os nós",
  "execute.locally": "Executar localmente",
  "execution.page.show.tab.Nodes.title": "Nós",
  "execution.show.mode.Log.title": "Saída de Log",
  filter: "Filtro:",
  "loading.matched.nodes": "Carregando nós correspondentes...",
  "loading.text": "Carregando...",
  "loglevel.debug": "Debug",
  "loglevel.normal": "Normal",
  "matched.nodes.prompt": "Nós correspondentes",
  no: "Não",
  "node.access.not-runnable.message":
    "Você não tem acesso para executar comandos neste nó.",
  "node.filter": "Filtro de Nó",
  "node.filter.exclude": "Excluir Filtro",
  "node.metadata.os": "Sistema Operacional",
  "node.metadata.status": "Status",
  nodes: "Nós",
  "notification.event.onfailure": "Na falha",
  "notification.event.onsuccess": "No sucesso",
  "notification.event.onstart": "No início",
  "notification.event.onavgduration": "Duração média excedida",
  "notification.event.onretryablefailure": "Na falha de nova tentativa",
  refresh: "atualizar",
  "save.filter.ellipsis": "Salvar filtro",
  "search.ellipsis": "Pesquisar...",
  "scheduledExecution.property.defaultTab.label": "Aba padrão",
  "scheduledExecution.property.defaultTab.description":
    "Aba padrão para exibir ao seguir uma execução.",
  "scheduledExecution.property.excludeFilterUncheck.label":
    "Mostrar Nós Excluídos",
  "scheduledExecution.property.excludeFilterUncheck.description":
    "Se verdadeiro, os nós excluídos serão indicados ao executar o Trabalho. Caso contrário, eles não serão mostrados.",
  "scheduledExecution.property.logOutputThreshold.label":
    "Limite de Saída de Log",
  "scheduledExecution.property.logOutputThreshold.description":
    'Insira o número máximo total de linhas (por exemplo, "100"), o número máximo de linhas por nó ("100/nó") ou o tamanho máximo do arquivo de log ' +
    '("100MB", "100KB", etc.), usando "GB", "MB", "KB", "B" como Giga- Mega- Kilo- e bytes.',
  "scheduledExecution.property.logOutputThreshold.placeholder":
    'Por exemplo, "100", "100/nó" ou "100MB"',
  "scheduledExecution.property.logOutputThresholdAction.label":
    "Ação Limite de Log",
  "scheduledExecution.property.logOutputThresholdAction.description":
    "Ação a ser executada se o limite de saída for atingido.",
  "scheduledExecution.property.logOutputThresholdAction.halt.label":
    "Parar com status:",
  "scheduledExecution.property.logOutputThresholdAction.truncate.label":
    "Truncar e continuar",
  "scheduledExecution.property.logOutputThresholdStatus.placeholder":
    "'falhou', 'abortado' ou qualquer string",
  "scheduledExecution.property.loglevel.help":
    "O nível de Debug produz mais saídas",
  "scheduledExecution.property.maxMultipleExecutions.label":
    "Limitar várias execuções?",
  "scheduledExecution.property.maxMultipleExecutions.description":
    "Número máximo de múltiplas execuções. Use em branco ou 0 para indicar nenhum limite.",
  "scheduledExecution.property.multipleExecutions.description":
    "Permitir que esta tarefa seja executada mais de uma vez simultaneamente?",
  "scheduledExecution.property.nodeKeepgoing.prompt": "Se um nó falhar",
  "scheduledExecution.property.nodeKeepgoing.true.description":
    "Continue executando nos nós restantes antes de falhar na etapa.",
  "scheduledExecution.property.nodeKeepgoing.false.description":
    "Falha no passo sem executar nos nós restantes.",
  "scheduledExecution.property.nodeRankAttribute.label":
    "Atributo de classificação",
  "scheduledExecution.property.nodeRankAttribute.description":
    "Atributo de nó para ordenar. O padrão é o nome do nó.",
  "scheduledExecution.property.nodeRankOrder.label": "Ordem de classificação",
  "scheduledExecution.property.nodeRankOrder.ascending.label": "Ascendente",
  "scheduledExecution.property.nodeRankOrder.descending.label": "Descendente",
  "scheduledExecution.property.nodeThreadcount.label": "Contagem de threads",
  "scheduledExecution.property.nodeThreadcount.description":
    "Número máximo de threads paralelos a serem usados. (Padrão: 1)",
  "scheduledExecution.property.nodefiltereditable.label": "Filtro editável",
  "scheduledExecution.property.nodesSelectedByDefault.label": "Seleção de nós",
  "scheduledExecution.property.nodesSelectedByDefault.true.description":
    "Nós de destino são selecionados por padrão",
  "scheduledExecution.property.nodesSelectedByDefault.false.description":
    "O usuário precisa selecionar explicitamente os nós de destino",
  "scheduledExecution.property.notifyAvgDurationThreshold.label": "Limite",
  "scheduledExecution.property.notifyAvgDurationThreshold.description":
    "Adicione ou defina um valor limite à duração média para acionar essa notificação. Opções:\n" +
    "- porcentagem => Por exemplo: 20% \n" +
    "- tempo delta => por exemplo: + 20s, +20 \n" +
    "- tempo absoluto => 30s, 5m \n" +
    "Tempo em segundos, se você não especificar unidades de tempo " +
    "Pode incluir referências de valor de opção como {'$'}{'{'}option{'.'}avgDurationThreshold{'}'}.",
  "scheduledExecution.property.orchestrator.label": "Orquestrador",
  "scheduledExecution.property.orchestrator.description":
    "Isso pode ser usado para controlar a ordem e o momento em que os nós são processados",
  "scheduledExecution.property.retry.description":
    "Número máximo de vezes para repetir a execução quando esta job é invocada diretamente. A repetição ocorrerá se o job falhar ou expirar, mas não se for manualmente eliminado. Pode usar uma referência de valor de opção como \"{'$'}{'{'}option{'.'}retry{'}'}\".",
  "scheduledExecution.property.retry.delay.description":
    "O tempo entre a falha na execução e a nova tentativa. Tempo em segundos, " +
    'ou especifique unidades de tempo: "120m", "2h", "3d". Use em branco ou 0 para indicar nenhum atraso. Pode incluir valor da opção ' +
    "referências como \"{'$'}{'{'}option{'.'}delay{'}'}\".",
  "scheduledExecution.property.successOnEmptyNodeFilter.prompt":
    "Se o nó estiver vazio",
  "scheduledExecution.property.successOnEmptyNodeFilter.true.description":
    "Continue a execução.",
  "scheduledExecution.property.successOnEmptyNodeFilter.false.description":
    "Falha na tarefa.",
  "scheduledExecution.property.timeout.description":
    "O tempo máximo para uma execução ser executada. Tempo em segundos, " +
    'ou especifique unidades de tempo: "120m", "2h", "3d". Use em branco ou 0 para indicar nenhum tempo limite. Pode incluir valor de referência ' +
    "da opção como \"{'$'}{'{'}option{'.'}timeout{'}'}\".",
  "scheduledExecution.property.scheduleEnabled.description":
    "Permitir que esta tarefa seja agendada?",
  "scheduledExecution.property.scheduleEnabled.label": "Ativar agendamento?",
  "scheduledExecution.property.executionEnabled.description":
    "Permitir que esta tarefa seja executada?",
  "scheduledExecution.property.executionEnabled.label": "Ativar Execução?",
  "scheduledExecution.property.timezone.prompt": "Fuso horário",
  "scheduledExecution.property.timezone.description":
    'Um fuso horário válido, seja uma abreviação como "PST", um nome completo, como "America/Los_Angeles", ou um ID personalizado, como "GMT-8{\':\'}00".',
  "documentation.reference.cron.url":
    "https{':'}//www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html",
  "set.as.default.filter": "Definir como filtro padrão",
  "show.all.nodes": "Mostrar todos os nós",
  yes: "Sim",
  // job query field labels
  "jobquery.title.titleFilter": "Comando ad hoc",
  "jobquery.title.contextFilter": "Contexto",
  "jobquery.title.actionFilter": "Ação",
  "jobquery.title.maprefUriFilter": "URI de recurso",
  "jobquery.title.reportIdFilter": "Nome",
  "jobquery.title.tagsFilter": "Tag",
  "jobquery.title.nodeFilter": "Nó",
  "jobquery.title.nodeFilter.plural": "Nós",
  "jobquery.title.messageFilter": "Mensagem",
  "jobquery.title.reportKindFilter": "Tipo de relatório",
  "jobquery.title.recentFilter": "Dentro",
  "jobquery.title.actionTypeFilter": "Ação",
  "jobquery.title.itemTypeFilter": "Tipo de item",
  "jobquery.title.filter": "Filtro",
  "jobquery.title.jobFilter": "Nome do Job",
  "jobquery.title.idlist": "ID do Job",
  "jobquery.title.jobIdFilter": "ID do Job",
  "jobquery.title.descFilter": "Descrição da Job",
  "jobquery.title.objFilter": "Recurso",
  "jobquery.title.scheduledFilter": "Agendado",
  "jobquery.title.serverNodeUUIDFilter": "UUID do Nó do Servidor",
  "jobquery.title.typeFilter": "Tipo",
  "jobquery.title.cmdFilter": "Comando",
  "jobquery.title.userFilter": "Usuário",
  "jobquery.title.projFilter": "Projeto",
  "jobquery.title.statFilter": "Resultado",
  "jobquery.title.startFilter": "Hora de início",
  "jobquery.title.startbeforeFilter": "Começa Antes",
  "jobquery.title.startafterFilter": "Começa Depois",
  "jobquery.title.endbeforeFilter": "Termina Antes",
  "jobquery.title.endafterFilter": "Termina Depois",
  "jobquery.title.endFilter": "Hora",
  "jobquery.title.durationFilter": "Duração",
  "jobquery.title.outFilter": "Saída",
  "jobquery.title.objinfFilter": "Informação do Recurso",
  "jobquery.title.cmdinfFilter": "Informação do Comando",
  "jobquery.title.groupPath": "Grupo",
  "jobquery.title.summary": "Resumo",
  "jobquery.title.duration": "Duração",
  "jobquery.title.loglevelFilter": "Nível de Log",
  "jobquery.title.loglevelFilter.label.DEBUG": "Debug",
  "jobquery.title.loglevelFilter.label.VERBOSE": "Verbose",
  "jobquery.title.loglevelFilter.label.INFO": "Informação",
  "jobquery.title.loglevelFilter.label.WARN": "Aviso",
  "jobquery.title.loglevelFilter.label.ERR": "Erro",
  "jobquery.title.adhocExecutionFilter": "Tipo de tarefa",
  "jobquery.title.adhocExecutionFilter.label.true": "Comando",
  "jobquery.title.adhocExecutionFilter.label.false": "Comando Definido",
  "jobquery.title.adhocLocalStringFilter": "Conteúdo do Script",
  "jobquery.title.adhocRemoteStringFilter": "Comando Shell",
  "jobquery.title.adhocFilepathFilter": "Caminho do arquivo script",
  "jobquery.title.argStringFilter": "Parâmetros do Arquivo de Script",
  "page.unsaved.changes": "Você tem alterações não salvas",
  "edit.nodes.file": "Editar Arquivo de Nós",
  "project.node.file.source.label": "Fonte",
  "file.display.format.label": "Formato",
  "project.node.file.source.description.label": "Descrição",
  "project.nodes.edit.save.error.message": "Erro ao Salvar Conteúdo:",
  "project.nodes.edit.empty.description":
    "Nota: Nenhum conteúdo estava disponível.",
  "button.action.Cancel": "Cancelar",
  "button.action.Save": "Salvar",
  "job-edit-page": {
    "nodes-tab-title": "Nós & Runners",
    "node-dispatch-true-label": "Despachar para Nós através do Runner",
    "node-dispatch-false-label": "Executar no Runner",
    "section-title": "Despacho",
    "section-title-help": "Escolha o Runner e seus Nós selecionados",
  },
  "job-exec-page": {
    "nodes-tab-title": "Runner/Nós",
  },
  JobRunnerEdit: {
    section: {
      title: "Conjunto de Runners",
    },
  },
  gui: {
    menu: {
      Nodes: "Nós",
    },
  },
  search: "Pesquisar",
  browse: "Navegar",
  result: "Resultado:",
  actions: "Ações",
  none: "Nenhum",
  set: {
    "all.nodes.as.default.filter": "Definir Todos os Nós como Filtro Padrão",
    "as.default.filter": "Definir como Filtro Padrão",
  },
  remove: {
    "all.nodes.as.default.filter": "Remover Todos os Nós como Filtro Padrão",
    "default.filter": "Remover Filtro Padrão",
  },
  "run.a.command.on.count.nodes.ellipsis": "Executar um comando em {0} {1}",
  "create.a.job.for.count.nodes.ellipsis": "Criar um trabalho para {0} {1}",
  "resource.metadata.entity.tags": "Tags",
  filters: "Filtros",
  "all.nodes": "Todos os Nós",
  "delete.this.filter.ellipsis": "Excluir este Filtro ...",
  "enter.a.filter": "Insira um Filtro",
  "remove.all.nodes.as.default.filter":
    "Remover Todos os Nós como Filtro Padrão",
  "set.all.nodes.as.default.filter": "Definir Todos os Nós como Filtro Padrão",
  "not.authorized": "Não autorizado",
  "disabled.execution.run": "Execuções estão desativadas.",
  "user.at.host": "Usuário {'@'} Host",
  "node.changes.success": "Alterações no nó foram salvas com sucesso.",
  "node.changes.notsaved": "Alterações no nó não foram salvas.",
  "node.remoteEdit.edit": "Editar nó:",
  "node.remoteEdit.continue": "Continuar...",
  node: "Nó",
  "this.will.select.both.nodes": "Isso selecionará ambos os nós.",
  "node.metadata.hostname": "Nome do Host",
  "select.nodes.by.name": "Selecionar nós pelo nome",
  "filter.nodes.by.attribute.value": "Filtrar nós pelo valor do atributo",
  "use.regular.expressions": "Usar Expressões Regulares:",
  "regex.syntax.checking": "Verificação de sintaxe de regex",
  "edit.ellipsis": "Editar...",
  "node.metadata.username-at-hostname": "Usuário & Host",
  "node.metadata.osFamily": "Família do SO",
  "node.metadata.osName": "Nome do SO",
  "node.metadata.osArch": "Arquitetura do SO",
  "node.metadata.osVersion": "Versão do SO",
  "node.metadata.type": "Tipo",
  "node.metadata.username": "Nome de Usuário",
  "node.metadata.tags": "Tags",
  "button.Edit.label": "Editar",
  "default.paginate.prev": "-",
  "default.paginate.next": "+",
  "jump.to": "Ir para",
  "per.page": "Por Página",
  "remove.default.filter": "Remover Filtro Padrão",
  "scheduledExecution.action.edit.button.label": "Editar este Trabalho…",
  "scheduledExecution.action.duplicate.button.label": "Duplicar este Trabalho…",
  "scheduledExecution.action.duplicate.other.button.label":
    "Duplicar este Trabalho para outro Projeto…",
  "scheduledExecution.action.download.button.label": "Baixar Definição",
  "scheduledExecution.action.downloadformat.button.label":
    "Baixar definição do trabalho em {0}",
  "scheduledExecution.action.delete.button.label": "Excluir este Trabalho",
  "scheduledExecution.action.edit.button.tooltip": "Editar este Trabalho",
  "scheduledExecution.action.duplicate.button.tooltip": "Duplicar Trabalho",
  "enable.schedule.this.job": "Ativar Agendamento",
  "disable.schedule.this.job": "Desativar Agendamento",
  "scheduledExecution.action.enable.schedule.button.label":
    "Ativar Agendamento",
  "scheduledExecution.action.disable.schedule.button.label":
    "Desativar Agendamento",
  "scheduleExecution.schedule.disabled":
    "Agendamento do trabalho está desativado",
  "enable.execution.this.job": "Ativar Execução",
  "disable.execution.this.job": "Desativar Execução",
  "scheduledExecution.action.enable.execution.button.label": "Ativar Execução",
  "scheduledExecution.action.disable.execution.button.label":
    "Desativar Execução",
  "scheduleExecution.execution.disabled":
    "Execução do trabalho está desativada",
  "delete.this.job": "Excluir este Trabalho",
  "action.prepareAndRun.tooltip": "Escolher opções e Executar Trabalho…",
  "job.bulk.modify.confirm.panel.title":
    "Confirmar Modificação em Massa de Trabalho",
  "job.bulk.delete.confirm.message":
    "Realmente excluir os Trabalhos selecionados?",
  "job.bulk.disable_schedule.confirm.message":
    "Desativar agendamentos para todos os Trabalhos selecionados?",
  "job.bulk.enable_schedule.confirm.message":
    "Ativar agendamentos para todos os Trabalhos selecionados?",
  "job.bulk.disable_execution.confirm.message":
    "Desativar execução para todos os Trabalhos selecionados?",
  "job.bulk.enable_execution.confirm.message":
    "Ativar execução para todos os Trabalhos selecionados?",
  "job.bulk.disable_schedule.button": "Desativar Agendamentos",
  "job.bulk.enable_schedule.button": "Ativar Agendamentos",
  "job.bulk.delete.button": "Excluir Trabalhos",
  "job.bulk.disable_execution.button": "Desativar Execução",
  "job.bulk.enable_execution.button": "Ativar Execução",
  "job.bulk.enable_execution.success": "Execução ativada para {0} trabalhos.",
  "job.bulk.enable_schedule.success": "Agendamento ativado para {0} trabalhos.",
  "job.bulk.disable_schedule.success":
    "Agendamento desativado para {0} trabalhos.",
  "job.bulk.disable_execution.success":
    "Execução desativada para {0} trabalhos.",
  "job.bulk.delete.success": "Excluídos {0} trabalhos.",
  "delete.selected.jobs": "Excluir Trabalhos Selecionados",
  "job.bulk.panel.select.title": "Selecionar Trabalhos para Edição em Massa",
  "job.bulk.perform.action.menu.label": "Executar Ação",
  "job.create.button": "Criar um novo Trabalho",
  "job.upload.button.title": "Carregar uma definição de Trabalho",
  cancel: "Cancelar",
  "job.actions": "Ações de Trabalho",
  "job.bulk.activate.menu.label": "Edição em Massa…",
  "job.bulk.deactivate.menu.label": "Sair do Modo de Edição em Massa",
  "upload.definition.button.label": "Carregar Definição",
  "new.job.button.label": "Novo Trabalho",
  "job.bulk.panel.select.message":
    "{n} Trabalho Selecionado | {n} Trabalhos Selecionados",
  "cannot.run.job": "Não é possível executar o trabalho",
  "disabled.schedule.run": "Execuções estão desativadas.",
  "disabled.job.run": "Execuções estão desativadas.",
  "schedule.on.server.x.at.y": "Agendado para executar no servidor {0} às {1}",
  "schedule.time.in.future": "em {0}",
  never: "Nunca",
  disabled: "Desativado",
  "project.schedule.disabled": "Agendamento do projeto está desativado",
  "project.execution.disabled": "Execução do projeto está desativada",
  "job.schedule.will.never.fire":
    "O agendamento do trabalho nunca será acionado",
  "scm.import.status.UNKNOWN.display.text":
    "Status de Importação: Não Rastreado",
  "scm.import.status.LOADING.description":
    "Importação: Status do trabalho está carregando",
  "scm.export.status.DELETED.display.text": "Excluído",
  "scm.export.status.EXPORT_NEEDED.display.text": "Modificado",
  "scm.export.status.CLEAN.description": "Status de Exportação: Limpo",
  "scm.import.status.DELETE_NEEDED.title.text":
    "Importação: Arquivos excluídos",
  "scm.export.status.EXPORT_NEEDED.title.text": "Exportação: Modificado",
  "scm.import.status.UNKNOWN.description": "Não Rastreado para Importação SCM",
  "scm.import.status.REFRESH_NEEDED.display.text": "Sincronização Necessária",
  "scm.export.status.CREATE_NEEDED.description":
    "Status de Exportação: Novo Trabalho, Ainda não adicionado ao SCM",
  "scm.import.status.DELETE_NEEDED.description":
    "Status de Importação: Arquivo de origem foi excluído",
  "scm.export.status.CLEAN.display.text": "Sem Alteração",
  "scm.export.status.LOADING.description":
    "Exportação: Status do trabalho está carregando",
  "scm.import.status.IMPORT_NEEDED.description":
    "Status de Importação: Alterações no trabalho precisam ser puxadas",
  "scm.import.status.REFRESH_NEEDED.title.text":
    "Importação: Sincronizar alterações",
  "scm.export.status.REFRESH_NEEDED.title.text":
    "Exportação: Alterações remotas precisam ser sincronizadas",
  "scm.export.status.LOADING.display.text": "Carregando",
  "scm.export.status.EXPORT_NEEDED.description":
    "Status de Exportação: Modificado",
  "scm.export.status.CREATE_NEEDED.display.text": "Criado",
  "scm.import.status.IMPORT_NEEDED.display.text": "Importação Necessária",
  "scm.import.status.DELETE_NEEDED.display.text":
    "Arquivo de origem foi excluído",
  "scm.import.status.IMPORT_NEEDED.title.text":
    "Importação: Alterações recebidas",
  "scm.import.status.CLEAN.display.text": "Atualizado",
  "scm.import.status.CLEAN.description": "Status de Importação: Atualizado",
  "scm.import.status.REFRESH_NEEDED.description":
    "Status de Importação: Alterações no trabalho precisam ser puxadas",
  "scm.export.status.REFRESH_NEEDED.display.text": "Sincronização Necessária",
  "scm.import.status.LOADING.display.text": "Carregando",
  "scm.export.status.ERROR.display.text": "Ocorreu um erro desconhecido.",
  "scm.import.status.ERROR.display.text": "Ocorreu um erro desconhecido.",
  "scm.status.ERROR.display.text": "Erro de SCM",
  "scm.export.auth.key.noAccess":
    "O usuário não tem acesso à chave ou senha especificada",
  "scm.import.auth.key.noAccess":
    "O usuário não tem acesso à chave ou senha especificada",
  "scm.action.diff.clean.button.label": "Ver Informações do Commit",
  "scm.import.plugin": "Plugin de Importação SCM",
  "scm.action.diff.button.label": "Diferenças de Alterações",
  "scm.export.commit.job.link.title":
    "Clique para commitar ou adicionar este Trabalho",
  "scm.export.commit.link.title":
    "Clique para commitar ou adicionar alterações",
  "scm.export.plugin": "Plugin de Exportação SCM",
  "job.toggle.scm.menu.on": "Ativar SCM",
  "job.toggle.scm.menu.off": "Desativar SCM",
  "scm.import.actions.title": "Ações de Importação SCM",
  "scm.export.actions.title": "Ações de Exportação SCM",
  "scm.export.title": "Exportação SCM",
  "scm.import.title": "Importação SCM",
  "job.toggle.scm.button.label.off": "Desativar SCM",
  "job.toggle.scm.confirm.panel.title": "Confirmar Modificação de SCM",
  "job.toggle.scm.confirm.on": "Ativar todos os plugins configurados de SCM?",
  "job.toggle.scm.confirm.off":
    "Desativar todos os plugins configurados de SCM?",
  "job.toggle.scm.button.label.on": "Ativar SCM",
  "job.scm.status.loading.message": "Carregando Status de SCM...",
  "page.section.Activity.for.jobs": "Atividade para Trabalhos",
  "widget.theme.title": "Tema",
  "widget.nextUi.title": "Ativar Próxima UI",
  "page.section.title.AllJobs": "Todos os Trabalhos",
  "advanced.search": "Avançado",
  "jobs.advanced.search.title": "Clique para modificar o filtro",
  "filter.jobs": "Pesquisar Trabalhos",
  "job.filter.quick.placeholder": "Pesquisar",
  "job.filter.apply.button.title": "Pesquisar",
  "job.filter.clear.button.title": "Limpar Pesquisa",
  all: "Todos",
  "job.tree.breakpoint.hit.info":
    "Aviso: Nem todos os detalhes do Trabalho foram carregados porque este grupo contém muitos trabalhos. Clique no botão para carregar os detalhes ausentes.",
  "job.tree.breakpoint.load.button.title":
    "Carregar Todos os Detalhes do Trabalho",
  "job.list.filter.save.modal.title": "Salvar Filtro",
  "job.filter.save.button.title": "Salvar como Filtro…",
  "job.list.filter.save.button": "Salvar Filtro",
  "job.list.filter.delete.filter.link.text": 'Excluir Filtro "{0}"',
  "app.firstRun.title": "Bem-vindo ao {0} {1}",
  "you.can.see.this.message.again.by.clicking.the":
    "Você pode ver esta mensagem novamente clicando no",
  "version.number": "número da versão",
  "in.the.page.footer": "no rodapé da página.",
  "no.authorized.access.to.projects":
    "Você não tem acesso autorizado a projetos.",
  "no.authorized.access.to.projects.contact.your.administrator.user.roles.0":
    "Entre em contato com seu administrador. (Funções do usuário: {0})",
  "page.home.loading.projects": "Carregando Projetos",
  "app.firstRun.md":
    "Obrigado por ser um assinante do {0}.\n\n" +
    "  \n\n\n" +
    "* [{0} Portal de Suporte &raquo;](http://support.rundeck.com)\n\n" +
    "* [{0} Documentação &raquo;]({1})",
  "page.home.section.project.title": "{0} Projeto",
  "page.home.section.project.title.plural": "{0} Projetos",
  "page.home.duration.in.the.last.day": "No último dia",
  by: "por",
  user: "Usuário",
  "user.plural": "Usuários",
  "page.home.project.executions.0.failed.parenthetical": "({0} Falhou)",
  "page.home.search.projects.input.placeholder":
    "Pesquisa de projeto: nome, rótulo ou /regex/",
  "page.home.search.project.title":
    "{n} Projeto encontrado | {n} Projetos encontrados",
  "button.Action": "Ação",
  "edit.configuration": "Editar Configuração",
  "page.home.new.project.button.label": "Novo Projeto",
  Execution: "{n} Execuções | {n} Execução | {n} Execuções",
  in: "em",
  "Project.plural": "Projetos",
  discard: "Descartar",
  "commandline.arguments.prompt.unquoted":
    "Argumentos da Linha de Comando (sem aspas):",
  usage: "Uso",
  "form.label.valuesType.list.label": "Lista",
  "scheduledExecution.option.unsaved.warning":
    "Descarte ou salve as alterações nesta opção antes de concluir as alterações no trabalho",
  "bash.prompt": "Bash:",
  "script.content.prompt": "Conteúdo do Script:",
  "rundeck.user.guide.option.model.provider":
    "Guia do Usuário do Rundeck - Provedor de modelo de opção",
  save: "Salvar",
  "commandline.arguments.prompt": "Argumentos da Linha de Comando:",
  "commandline.arguments.prompt.unquoted.warning":
    "Aviso! Confiar em argumentos sem aspas pode tornar este trabalho vulnerável à injeção de comandos. Use com cuidado.",
  "add.new.option": "Adicionar Nova Opção",
  "add.an.option": "Adicionar uma opção",
  "option.values.c": "1 Valor|{n} Valores",
  "no.options.message": "Sem Opções",
  "the.option.values.will.be.available.to.scripts.in.these.forms":
    "Os valores da opção estarão disponíveis para scripts nestas formas:",
  "form.option.date.label": "Data",
  "form.option.enforcedType.label": "Restrições",
  "form.option.usage.file.fileName.preview.description":
    "O nome original do arquivo:",
  "form.option.discard.title": "Descartar alterações na opção",
  "form.option.valuesType.url.authentication.password.label": "Senha",
  "form.option.enforcedType.none.label": "Qualquer valor pode ser usado",
  "form.option.inputType.label": "Tipo de Entrada",
  "form.option.defaultStoragePath.present.description":
    "Um valor padrão será carregado do Armazenamento de Chaves",
  "form.option.secureInput.false.label": "Texto Simples",
  "form.option.name.label": "Nome da Opção",
  "form.option.valuesType.url.filter.label": "Filtro de Caminho JSON",
  "form.option.valuesType.url.authType.label": "Tipo de Autenticação",
  "form.option.sort.description": "Ordenar lista de Valores Permitidos",
  "form.option.usage.secureAuth.message":
    "Os valores da opção de autenticação segura não estão disponíveis para scripts ou comandos",
  "form.option.valuesType.url.label": "URL Remota",
  "form.option.valuesList.placeholder":
    "Lista separada por delimitador (vírgula por padrão)",
  "form.option.secureInput.description":
    "Os valores de entrada segura não são armazenados pelo Rundeck após o uso. Se o valor exposto for usado em um script ou comando, o log de saída pode conter o valor.",
  "form.option.valuesType.url.authentication.key.label": "Chave",
  "form.option.date.description":
    "A data será passada para o seu trabalho como uma string formatada desta forma: mm/dd/aa HH:MM",
  "form.option.valuesType.url.authentication.username.label": "Nome de Usuário",
  "form.option.valuesType.url.authType.bearerToken.label": "Token Bearer",
  "form.option.enforced.label": "Imposto a partir de Valores Permitidos",
  "form.option.description.label": "Descrição",
  "form.option.save.title": "Salvar alterações na opção",
  "form.option.type.label": "Tipo de Opção",
  "form.option.multivalueAllSelected.label":
    "Selecionar Todos os Valores por Padrão",
  "form.option.secureExposed.false.label": "Autenticação Remota Segura",
  "form.option.valuesType.url.authentication.token.label": "Token",
  "form.option.delimiter.label": "Delimitador",
  "form.option.valuesURL.placeholder": "URL Remota",
  "form.option.valuesType.url.authType.empty.label":
    "Selecionar Tipo de Autenticação",
  "form.option.valuesDelimiter.description":
    "Defina o delimitador para Valores Permitidos",
  "form.option.usage.file.preview.description":
    "O caminho do arquivo local estará disponível para scripts nestas formas:",
  "form.option.secureExposed.false.description":
    "Entrada de senha, valor não exposto em scripts ou comandos, usado apenas por Node Executors para autenticação.",
  "form.option.valuesType.url.filter.error.label":
    "O Filtro de Caminho JSON da URL Remota tem uma sintaxe inválida",
  "form.option.valuesType.url.authType.apiKey.label": "Chave API",
  "form.option.optionType.text.label": "Texto",
  "form.option.secureExposed.true.label": "Texto Simples com Entrada de Senha",
  "form.option.valuesType.url.filter.description":
    'Filtrar resultados JSON usando um caminho de chave, por exemplo "$.key.path"',
  "form.option.valuesType.url.authentication.tokenInformer.header.label":
    "Cabeçalho",
  "form.option.defaultStoragePath.description":
    "Caminho de Armazenamento de Chaves para um valor de senha padrão",
  "form.option.multivalued.label": "Multi-valores",
  "form.option.multivalued.description":
    "Permitir que vários valores de entrada sejam escolhidos.",
  "form.option.valuesType.url.authentication.tokenInformer.query.label":
    "Parâmetro de Consulta",
  "form.option.create.title": "Salvar a nova opção",
  "form.option.regex.label": "Expressão Regular de Correspondência",
  "form.option.optionType.file.label": "Arquivo",
  "form.option.valuesDelimiter.label": "Delimitador de Lista",
  "form.option.cancel.title": "Cancelar adição de nova opção",
  "form.option.values.label": "Valores Permitidos",
  "form.option.dateFormat.description.md":
    "Insira um formato de data conforme descrito na [documentação do momentjs](http://momentjs.com/docs/#/displaying/format/)",
  "form.option.defaultStoragePath.label": "Caminho de Armazenamento",
  "form.option.defaultValue.label": "Valor Padrão",
  "form.option.delimiter.description":
    "O delimitador será usado para juntar todos os valores de entrada. Pode ser qualquer string: ' ' (espaço), ',' (vírgula), etc. Nota: não inclua aspas.",
  "form.option.secureInput.false.description": "Entrada de texto simples.",
  "form.option.valuesType.url.authType.basic.label": "Básico",
  "form.option.valuesType.url.authentication.tokenInformer.label":
    "Injetar chave",
  "form.option.secureExposed.true.description":
    "Texto simples com uma entrada de senha, valor exposto em scripts e comandos.",
  "form.option.dateFormat.title": "Formato de Data",
  "form.option.label.label": "Rótulo da Opção",
  "form.option.valuesUrl.description": "Uma URL para um serviço JSON Remoto.",
  "form.option.multivalued.secure-conflict.message":
    "Opções de entrada segura não permitem múltiplos valores",
  "form.option.sort.label": "Ordenar Valores",
  "form.option.usage.file.sha.preview.description":
    "O valor SHA-256 do conteúdo do arquivo:",
  "Option.property.description.description":
    "A descrição será renderizada com Markdown.",
  "option.defaultValue.regexmismatch.message":
    'O valor padrão "{0}" não corresponde à regex: {1}',
  "option.multivalued.secure-conflict.message":
    "Entrada segura não pode ser usada com entrada de múltiplos valores",
  "option.defaultValue.notallowed.message":
    "O Valor Padrão não estava na lista de valores permitidos, e os valores são impostos",
  "option.enforced.secure-conflict.message":
    "Entrada segura não pode ser usada com valores impostos",
  "option.file.config.disabled.message":
    "O plugin de configuração do tipo de opção de arquivo não está habilitado",
  "option.defaultValue.required.message":
    "Especifique um Valor Padrão para opções obrigatórias quando o Trabalho for agendado.",
  "option.enforced.emptyvalues.message":
    "Valores permitidos (lista ou URL remota) devem ser especificados se os valores forem impostos",
  "option.file.required.message":
    "O tipo de opção de arquivo não pode ser Obrigatório quando o Trabalho for agendado.",
  "option.regex.invalid.message": "Expressão Regular Inválida: {0}",
  "option.file.config.invalid.message":
    "A configuração do tipo de opção de arquivo não é válida: {0}",
  "option.delimiter.blank.message":
    "Você deve especificar um delimitador para opções de múltiplos valores",
  "option.hidden.notallowed.message":
    "Opções ocultas devem ter um valor padrão ou caminho de armazenamento.",
  "option.values.regexmismatch.message":
    'O valor permitido "{0}" não corresponde à regex: {1}',
  "option.defaultValue.multivalued.notallowed.message":
    'O Valor Padrão contém uma string que não estava na lista de valores permitidos, e os valores são impostos: "{0}". Nota: espaços em branco são significativos.',
  "Option.required.label": "Obrigatório",
  "Option.hidden.description":
    "Deve ser oculto na página de execução do trabalho",
  "Option.required.description":
    "Exigir que esta opção tenha um valor não vazio ao executar o Trabalho",
  "Option.hidden.label": "Deve ser oculto",
  "form.option.regex.placeholder": "Insira uma Expressão Regular",
  "home.user": "{n} Usuários | {n} Usuário | {n} Usuários",
  "home.table.projects": "Projetos",
  "home.table.activity": "Atividade",
  "home.table.actions": "Ações",
  "option.click.to.edit.title": "Clique para editar",
  "form.option.regex.validation.error":
    "Valor inválido: Deve corresponder ao padrão: {0}",
  "form.field.required.message": "Este campo é obrigatório",
  "form.field.too.long.message":
    "Este valor não pode ter mais de {max} caracteres",
  "form.option.validation.errors.message":
    "Corrija os erros de validação antes de salvar as alterações",
  "option.list.header.name.title": "Nome",
  "option.list.header.values.title": "Valores",
  "option.list.header.restrictions.title": "Restrições",
  "util.undoredo.undo": "Desfazer",
  "util.undoredo.redo": "Refazer",
  "util.undoredo.revertAll": "Reverter Todas as Alterações",
  "util.undoredo.revertAll.confirm": "Realmente reverter todas as alterações?",
  "option.view.required.title": " (Obrigatório)",
  "option.view.allowedValues.label": "Valores Permitidos",
  "option.view.valuesUrl.title": "Valores carregados da URL Remota: {0}",
  "option.view.valuesUrl.placeholder": "URL",
  "option.view.enforced.title": "A entrada deve ser um dos valores permitidos",
  "option.view.enforced.placeholder": "Estrito",
  "option.view.regex.info.note":
    "Os valores devem corresponder à expressão regular:",
  "option.view.notenforced.title": "Sem restrições no valor de entrada",
  "option.view.notenforced.placeholder": "Nenhum",
  "option.view.action.delete.title": "Excluir esta Opção",
  "option.view.action.edit.title": "Editar esta Opção",
  "option.view.action.duplicate.title": "Duplicar esta Opção",
  "option.view.action.moveUp.title": "Mover para Cima",
  "option.view.action.moveDown.title": "Mover para Baixo",
  "option.view.action.drag.title": "Arraste para reordenar",
  "pagination.of": "de",
  uiv: {
    datePicker: {
      clear: "Limpar",
      today: "Hoje",
      month: "Mês",
      month1: "Janeiro",
      month2: "Fevereiro",
      month3: "Março",
      month4: "Abril",
      month5: "Maio",
      month6: "Junho",
      month7: "Julho",
      month8: "Agosto",
      month9: "Setembro",
      month10: "Outubro",
      month11: "Novembro",
      month12: "Dezembro",
      year: "Ano",
      week1: "Seg",
      week2: "Ter",
      week3: "Qua",
      week4: "Qui",
      week5: "Sex",
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
      placeholder: "Selecionar...",
      filterPlaceholder: "Pesquisar...",
    },
  },
  "scheduledExecution.jobName.label": "Nome do Trabalho",
  "scheduledExecution.property.description.label": "Descrição",
  "job.editor.preview.runbook": "Pré-visualizar Readme",
  "choose.action.label": "Escolher",
  "scheduledExecution.property.description.plain.description":
    "A descrição será mostrada em texto simples",
  "scheduledExecution.property.description.description":
    "A primeira linha da descrição será mostrada em texto simples, o restante será renderizado com Markdown.\n\n" +
    "Veja [Markdown](http://en.wikipedia.org/wiki/Markdown).\n\n" +
    "Dentro da descrição estendida, você pode vincular ao trabalho usando {'`{{job.permalink}}`'} como a URL para o trabalho, por exemplo, `[executar trabalho]({'{{job.permalink}}#runjob'})`\n\n" +
    "Você pode adicionar um Readme usando um separador HR `---` sozinho em uma linha, e tudo o que seguir será renderizado em uma aba separada usando [Markdeep](https://casual-effects.com/markdeep).",
  "scheduledExecution.groupPath.description":
    "Grupo é um caminho separado por /",
  more: "Mais…",
  less: "Menos…",
  "job.edit.groupPath.choose.text": "Clique no nome do grupo para usar",
  "scheduledExecution.property.executionLifecyclePluginConfig.help.text":
    "Os Plugins selecionados serão habilitados para este Trabalho.",
  Workflow: {
    label: "Fluxo de Trabalho",
    property: {
      keepgoing: {
        true: { description: "Executar etapas restantes antes de falhar." },
        false: { description: "Parar na etapa que falhou." },
        prompt: "Se uma etapa falhar:",
      },
      strategy: {
        label: "Estratégia",
      },
    },
  },
  "plugin.choose.title": "Escolher um Plugin",
  "plugin.type.WorkflowStep.title.plural": "Etapas do Fluxo de Trabalho",
  "plugin.type.WorkflowStep.title": "Etapa do Fluxo de Trabalho",
  "plugin.type.WorkflowNodeStep.title.plural":
    "Etapas do Nó do Fluxo de Trabalho",
  "plugin.type.WorkflowNodeStep.title": "Etapa do Nó do Fluxo de Trabalho",
  "JobExec.nodeStep.true.label": "Etapa do Nó",
};

export default messages;
