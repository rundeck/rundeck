const messages = {
  Edit: "編集",
  Save: "保存",
  Delete: "削除",
  Cancel: "キャンセル",
  Revert: "元に戻す",
  jobAverageDurationPlaceholder: "ジョブの平均時間を空白のままにする",
  resourcesEditor: {
    "Dispatch to Nodes": "ノードに配信",
    Nodes: "ノード",
  },
  cron: {
    section: {
      0: "秒",
      1: "分",
      2: "時間",
      3: "月の日",
      4: "月",
      5: "週の日",
      6: "年",
    },
  },
  message_communityNews: "コミュニティニュース",
  message_connectionError:
    "コミュニティニュースへの接続中にエラーが発生したようです。",
  message_readMore: "もっと読む",
  message_refresh: "ページを更新するか、こちらをご覧ください",
  message_subscribe: "購読する",
  message_delete: "このフィールドを削除",
  message_duplicated: "フィールドは既に存在します",
  message_select: "フィールドを選択",
  message_description: "説明",
  message_fieldLabel: "フィールドラベル",
  message_fieldKey: "フィールドキー",
  message_fieldFilter: "フィールドをフィルタリングするために入力",
  message_empty: "空にすることができます",
  message_cancel: "キャンセル",
  message_add: "追加",
  message_addField: "カスタムフィールドを追加",
  message_pageUsersSummary: "Rundeckユーザーのリスト。",
  message_pageUsersLoginLabel: "ユーザー名",
  message_pageUsersCreatedLabel: "作成日",
  message_pageUsersUpdatedLabel: "更新日",
  message_pageUsersLastjobLabel: "最後のジョブ実行",
  message_domainUserFirstNameLabel: "名",
  message_domainUserLastNameLabel: "姓",
  message_domainUserEmailLabel: "メール",
  message_domainUserLabel: "ユーザー",
  message_pageUsersTokensLabel: "トークン数",
  message_pageUsersTokensHelp:
    "ユーザープロファイルページでトークンを管理できます。",
  message_pageUsersLoggedStatus: "ステータス",
  message_pageUserLoggedOnly: "ログイン中のユーザーのみ",
  message_pageUserNotSet: "設定されていません",
  message_pageUserNone: "なし",
  message_pageFilterLogin: "ログイン",
  message_pageFilterHostName: "ホスト名",
  message_pageFilterSessionID: "セッションID",
  message_pageFilterBtnSearch: "検索",
  message_pageUsersSessionIDLabel: "セッションID",
  message_pageUsersHostNameLabel: "ホスト名",
  message_pageUsersLastLoginInTimeLabel: "最終ログイン",
  message_pageUsersTotalFounds: "見つかったユーザーの合計",
  message_paramIncludeExecTitle: "最後の実行を表示",
  message_loginStatus: {
    "LOGGED IN": "ログイン中",
    "NOT LOGGED": "一度もログインしていない",
    ABANDONED: "期限切れ",
    "LOGGED OUT": "ログアウト",
  },
  message_userSummary: {
    desc: "これはRundeckにログインしたユーザープロファイルのリストです。",
  },
  message_webhookPageTitle: "Webhook",
  message_webhookListTitle: "Webhook",
  message_webhookDetailTitle: "Webhookの詳細",
  message_webhookListNameHdr: "名前",
  message_addWebhookBtn: "追加",
  message_webhookEnabledLabel: "有効",
  message_webhookPluginCfgTitle: "プラグイン設定",
  message_webhookSaveBtn: "保存",
  message_webhookCreateBtn: "Webhookを作成",
  message_webhookDeleteBtn: "削除",
  message_webhookPostUrlLabel: "投稿URL",
  message_webhookPostUrlHelp:
    "このURLにHTTP POSTリクエストが送信されると、以下で選択されたWebhookプラグインがデータを受け取ります。",
  message_webhookPostUrlPlaceholder: "Webhookが作成された後にURLが生成されます",
  message_webhookNameLabel: "名前",
  message_webhookUserLabel: "ユーザー",
  message_webhookUserHelp:
    "このWebhookを実行する際に使用される認証ユーザー名。すべてのACLポリシーがこのユーザー名に適用されます。",
  message_webhookRolesLabel: "ロール",
  message_webhookRolesHelp:
    "このWebhookを実行する際に使用される認証ロール（カンマ区切り）。すべてのACLポリシーがこれらのロールに適用されます。",
  message_webhookAuthLabel: "HTTP認証文字列",
  message_webhookGenerateSecurityLabel: "認証ヘッダーを使用",
  message_webhookGenerateSecretCheckboxHelp:
    "[オプション] このWebhookのセキュリティを強化するために認証文字列を生成できます。すべての投稿には生成された文字列を認証ヘッダーに含める必要があります。",
  message_webhookSecretMessageHelp:
    "今すぐこの認証文字列をコピーしてください。このWebhookを離れた後は文字列を再度表示することはできません。",
  message_webhookRegenClicked:
    "Webhookが保存されると、新しい認証文字列が生成され表示されます。",
  message_webhookPluginLabel: "Webhookプラグインを選択",
  message_hello: "こんにちは",
  message_sidebarNotificationText: "Rundeckの更新が利用可能です",
  message_updateAvailable: "更新が利用可能",
  message_updateHasBeenReleased: "Rundeckの更新がリリースされました。",
  message_installedVersion: "インストールされているRundeckのバージョンは",
  message_currentVersion: "最新のRundeckリリースは",
  message_getUpdate: "更新を取得",
  message_dismissMessage:
    "次のリリースまでこの通知を無視するには、ここをクリックしてください。",
  message_close: "閉じる",
  "bulk.edit": "一括編集",
  "in.of": "の中の",
  execution: "実行 | 実行",
  "execution.count": "1回の実行 | {0}回の実行",
  "Bulk Delete Executions: Results": "一括削除実行：結果",
  "Requesting bulk delete, please wait.":
    "一括削除をリクエストしています。お待ちください。",
  "bulkresult.attempted.text": "{0}回の実行が試みられました。",
  "bulkresult.success.text": "{0}回の実行が正常に削除されました。",
  "bulkresult.failed.text": "{0}回の実行を削除できませんでした：",
  "delete.confirm.text": "{0} {1}を本当に削除しますか？",
  "clearselected.confirm.text":
    "すべての{0}選択されたアイテムをクリアしますか、それともこのページに表示されているアイテムのみをクリアしますか？",
  "bulk.selected.count": "{0}選択済み",
  "results.empty.text": "クエリに対する結果はありません",
  "Only shown executions": "表示されている実行のみ",
  "Clear bulk selection": "一括選択をクリア",
  "Click to edit Search Query": "検索クエリを編集するにはクリック",
  "Auto refresh": "自動更新",
  "error.message.0": "エラーが発生しました：{0}",
  "info.completed.0": "完了：{0}",
  "info.completed.0.1": "完了：{0} {1}",
  "info.missed.0.1": "見逃し：{0} {1}",
  "info.started.0": "開始：{0}",
  "info.started.expected.0.1": "開始：{0}、推定終了：{1}",
  "info.scheduled.0": "スケジュール済み；開始{0}",
  "job.execution.starting.0": "開始{0}",
  "job.execution.queued": "キューに入れられました",
  "info.newexecutions.since.0":
    "1つの新しい結果。クリックして読み込む。 | {0}つの新しい結果。クリックして読み込む。",
  "In the last Day": "過去1日間",
  Referenced: "参照済み",
  "job.has.been.deleted.0": "(ジョブ{0}は削除されました)",
  Filters: "フィルター",
  "filter.delete.named.text": 'フィルター"{0}"を削除...',
  "Delete Saved Filter": "保存されたフィルターを削除",
  "filter.delete.confirm.text":
    '保存されたフィルター"{0}"を本当に削除しますか？',
  "filter.save.name.prompt": "名前：",
  "filter.save.validation.name.blank": "名前を空白にすることはできません",
  "filter.save.button": "フィルターを保存...",
  "saved.filters": "保存されたフィルター",
  failed: "失敗",
  ok: "OK",
  "0.total": "合計{0}",
  period: {
    label: {
      All: "いつでも",
      Hour: "過去1時間",
      Day: "過去1日間",
      Week: "過去1週間",
      Month: "過去1か月",
    },
  },
  "empty.message.default":
    "設定されていません。新しいプラグインを追加するには{0}をクリックしてください。",
  CreateAcl: "ACLを作成",
  CreateAclName: "ACLの説明",
  CreateAclTitle: "プロジェクトのキー保存ACLを作成",
  "Edit Nodes": "ノードを編集",
  Modify: "変更",
  "Edit Node Sources": "ノードソースを編集",
  "The Node Source had an error": "ノードソースにエラーが発生しました",
  "Validation errors": "検証エラー",
  "unauthorized.status.help.1":
    "一部のノードソースが「未承認」のメッセージを返しました。",
  "unauthorized.status.help.2":
    "ノードソースプラグインがキー保存リソースへのアクセスを必要とする場合があります。アクセス制御ポリシーエントリによって有効にすることができます。",
  "unauthorized.status.help.3":
    "ACLポリシーがこのプロジェクトのキー保存への「読み取り」アクセスを許可していることを確認してください（プロジェクトURNパス（urn:project:name））。",
  "unauthorized.status.help.4": "プロジェクトACLを作成するには{0}に移動",
  "unauthorized.status.help.5": "システムACLを作成するには{0}に移動",
  "acl.config.link.title": "プロジェクト設定 > アクセス制御",
  "acl.config.system.link.title": "システム設定 > アクセス制御",
  "acl.example.summary": "ACLポリシーの例",
  "page.keyStorage.description":
    "キー保存は、ノード実行認証で使用するための公開鍵および秘密鍵とパスワードを保存するためのグローバルなディレクトリのような構造を提供します。",
  Duplicate: "複製",
  "Node.count.vue": "ノード | ノード",
  "bulk.delete": "一括削除",
  "select.none": "選択解除",
  "select.all": "すべて選択",
  "cancel.bulk.delete": "一括削除をキャンセル",
  "delete.selected.executions": "選択された実行を削除",
  "click.to.refresh": "クリックして更新",
  "count.nodes.matched": "{0} {1} 一致",
  "count.nodes.shown": "{0} ノードが表示されました。",
  "delete.this.filter.confirm": "このフィルターを本当に削除しますか？",
  "enter.a.node.filter":
    "ノードフィルターを入力するか、すべてのノードに対して.*を使用",
  "execute.locally": "ローカルで実行",
  "execution.page.show.tab.Nodes.title": "ノード",
  "execution.show.mode.Log.title": "ログ出力",
  filter: "フィルター：",
  "loading.matched.nodes": "一致するノードを読み込み中...",
  "loading.text": "読み込み中...",
  "loglevel.debug": "デバッグ",
  "loglevel.normal": "通常",
  "matched.nodes.prompt": "一致するノード",
  no: "いいえ",
  "node.access.not-runnable.message":
    "このノードでコマンドを実行するアクセス権がありません。",
  "node.filter": "ノードフィルター",
  "node.filter.exclude": "フィルターを除外",
  "node.metadata.os": "オペレーティングシステム",
  "node.metadata.status": "ステータス",
  nodes: "ノード：",
  "notification.event.onfailure": "失敗時",
  "notification.event.onsuccess": "成功時",
  "notification.event.onstart": "開始時",
  "notification.event.onavgduration": "平均時間を超えた場合",
  "notification.event.onretryablefailure": "再試行可能な失敗時",
  refresh: "更新",
  "save.filter.ellipsis": "フィルターを保存...",
  "search.ellipsis": "検索...",
  "scheduledExecution.property.defaultTab.label": "デフォルトタブ",
  "scheduledExecution.property.defaultTab.description":
    "実行をフォローするときに表示するデフォルトタブ。",
  "scheduledExecution.property.excludeFilterUncheck.label":
    "除外されたノードを表示",
  "scheduledExecution.property.excludeFilterUncheck.description":
    "trueの場合、ジョブの実行時に除外されたノードが表示されます。それ以外の場合は表示されません。",
  "scheduledExecution.property.logOutputThreshold.label": "ログ出力制限",
  "scheduledExecution.property.logOutputThreshold.description":
    '最大合計行数（例：「100」）、ノードごとの最大行数（「100/ノード」）または最大ログファイルサイズ（「100MB」、「100KB」など）を入力します。 "GB"、"MB"、"KB"、"B"をギガ、メガ、キロ、バイトとして使用します。',
  "scheduledExecution.property.logOutputThreshold.placeholder":
    "例：「100」、「100/ノード」または「100MB」",
  "scheduledExecution.property.logOutputThresholdAction.label":
    "ログ制限アクション",
  "scheduledExecution.property.logOutputThresholdAction.description":
    "出力制限に達した場合に実行するアクション。",
  "scheduledExecution.property.logOutputThresholdAction.halt.label":
    "ステータスで停止：",
  "scheduledExecution.property.logOutputThresholdAction.truncate.label":
    "切り捨てて続行",
  "scheduledExecution.property.logOutputThresholdStatus.placeholder":
    "'失敗'、'中止'、または任意の文字列",
  "scheduledExecution.property.loglevel.help":
    "デバッグレベルはより多くの出力を生成します",
  "scheduledExecution.property.maxMultipleExecutions.label":
    "複数の実行を制限しますか？",
  "scheduledExecution.property.maxMultipleExecutions.description":
    "複数の実行の最大数。制限なしを示すには空白または0を使用します。",
  "scheduledExecution.property.multipleExecutions.description":
    "このジョブを同時に複数回実行することを許可しますか？",
  "scheduledExecution.property.nodeKeepgoing.prompt": "ノードが失敗した場合",
  "scheduledExecution.property.nodeKeepgoing.true.description":
    "ステップが失敗する前に残りのノードで実行を続行します。",
  "scheduledExecution.property.nodeKeepgoing.false.description":
    "残りのノードで実行せずにステップを失敗させます。",
  "scheduledExecution.property.nodeRankAttribute.label": "ランク属性",
  "scheduledExecution.property.nodeRankAttribute.description":
    "並べ替えのためのノード属性。デフォルトはノード名です。",
  "scheduledExecution.property.nodeRankOrder.label": "ランク順",
  "scheduledExecution.property.nodeRankOrder.ascending.label": "昇順",
  "scheduledExecution.property.nodeRankOrder.descending.label": "降順",
  "scheduledExecution.property.nodeThreadcount.label": "スレッド数",
  "scheduledExecution.property.nodeThreadcount.description":
    "使用する最大並列スレッド数。（デフォルト：1）",
  "scheduledExecution.property.nodefiltereditable.label":
    "編集可能なフィルター",
  "scheduledExecution.property.nodesSelectedByDefault.label": "ノード選択",
  "scheduledExecution.property.nodesSelectedByDefault.true.description":
    "ターゲットノードはデフォルトで選択されています",
  "scheduledExecution.property.nodesSelectedByDefault.false.description":
    "ユーザーは明示的にターゲットノードを選択する必要があります",
  "scheduledExecution.property.notifyAvgDurationThreshold.label": "しきい値",
  "scheduledExecution.property.notifyAvgDurationThreshold.description":
    "通知をトリガーするためのオプションの期間しきい値。指定しない場合、ジョブの平均時間が使用されます。\n\n" +
    "- 平均の割合：`20%`\n" +
    "- 平均からの時間差：`+20s`、`+20`\n" +
    "- 絶対時間：`30s`、`5m`\n" +
    "秒、分、時間などの時間単位として`s`、`m`、`h`、`d`、`w`、`y`などを使用します。\n" +
    "単位が指定されていない場合、単位は秒になります。\n\n" +
    "オプション値の参照を含めることができます。例：`{'$'}{'{'}option{'.'}avgDurationThreshold{'}'}`。",
  "scheduledExecution.property.orchestrator.label": "オーケストレーター",
  "scheduledExecution.property.orchestrator.description":
    "これは、ノードが処理される順序とタイミングを制御するために使用できます。",
  "scheduledExecution.property.retry.description":
    "このジョブが直接呼び出されたときに再試行する最大回数。ジョブが失敗またはタイムアウトした場合に再試行が行われますが、手動で停止された場合は再試行されません。オプション値の参照を使用できます。例：\"{'$'}{'{'}option{'.'}retry{'}'}\"。",
  "scheduledExecution.property.retry.delay.description":
    "失敗した実行と再試行の間の時間。秒単位の時間、または時間単位を指定します。例：\"120m\"、\"2h\"、\"3d\"。遅延なしを示すには空白または0を使用します。オプション値の参照を含めることができます。例：\"{'$'}{'{'}option{'.'}delay{'}'}\"。",
  "scheduledExecution.property.successOnEmptyNodeFilter.prompt":
    "ノードセットが空の場合",
  "scheduledExecution.property.successOnEmptyNodeFilter.true.description":
    "実行を続行します。",
  "scheduledExecution.property.successOnEmptyNodeFilter.false.description":
    "ジョブを失敗させます。",
  "scheduledExecution.property.timeout.description":
    "実行の最大時間。秒単位の時間、または時間単位を指定します。例：\"120m\"、\"2h\"、\"3d\"。制限なしを示すには空白または0を使用します。オプション値の参照を含めることができます。例：\"{'$'}{'{'}option{'.'}timeout{'}'}\"。",
  "scheduledExecution.property.scheduleEnabled.description":
    "このジョブをスケジュール可能にしますか？",
  "scheduledExecution.property.scheduleEnabled.label":
    "スケジュールを有効にしますか？",
  "scheduledExecution.property.executionEnabled.description":
    "このジョブを実行可能にしますか？",
  "scheduledExecution.property.executionEnabled.label":
    "実行を有効にしますか？",
  "scheduledExecution.property.timezone.prompt": "タイムゾーン",
  "scheduledExecution.property.timezone.description":
    "有効なタイムゾーン。例：略語「PST」、フルネーム「America/Los_Angeles」、またはカスタムID「GMT-8{':'}00」。",
  "documentation.reference.cron.url":
    "https{':'}//www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html",
  "set.as.default.filter": "デフォルトフィルターとして設定",
  "show.all.nodes": "すべてのノードを表示",
  yes: "はい",
  // job query field labels
  "jobquery.title.titleFilter": "アドホックコマンド",
  "jobquery.title.contextFilter": "コンテキスト",
  "jobquery.title.actionFilter": "アクション",
  "jobquery.title.maprefUriFilter": "リソースURI",
  "jobquery.title.reportIdFilter": "名前",
  "jobquery.title.tagsFilter": "タグ",
  "jobquery.title.nodeFilter": "ノード",
  "jobquery.title.nodeFilter.plural": "ノード",
  "jobquery.title.messageFilter": "メッセージ",
  "jobquery.title.reportKindFilter": "レポートタイプ",
  "jobquery.title.recentFilter": "以内",
  "jobquery.title.actionTypeFilter": "アクションタイプ",
  "jobquery.title.itemTypeFilter": "アイテムタイプ",
  "jobquery.title.filter": "フィルター",
  "jobquery.title.jobFilter": "ジョブ名",
  "jobquery.title.idlist": "ジョブID",
  "jobquery.title.jobIdFilter": "ジョブID",
  "jobquery.title.descFilter": "ジョブの説明",
  "jobquery.title.objFilter": "リソース",
  "jobquery.title.scheduledFilter": "スケジュール済み",
  "jobquery.title.serverNodeUUIDFilter": "サーバーノードUUID",
  "jobquery.title.typeFilter": "タイプ",
  "jobquery.title.cmdFilter": "コマンド",
  "jobquery.title.userFilter": "ユーザー",
  "jobquery.title.projFilter": "プロジェクト",
  "jobquery.title.statFilter": "結果",
  "jobquery.title.startFilter": "開始時間",
  "jobquery.title.startbeforeFilter": "前に開始",
  "jobquery.title.startafterFilter": "後に開始",
  "jobquery.title.endbeforeFilter": "前に終了",
  "jobquery.title.endafterFilter": "後に終了",
  "jobquery.title.endFilter": "時間",
  "jobquery.title.durationFilter": "期間",
  "jobquery.title.outFilter": "出力",
  "jobquery.title.objinfFilter": "リソース情報",
  "jobquery.title.cmdinfFilter": "コマンド情報",
  "jobquery.title.groupPath": "グループ",
  "jobquery.title.summary": "概要",
  "jobquery.title.duration": "期間",
  "jobquery.title.loglevelFilter": "ログレベル",
  "jobquery.title.loglevelFilter.label.DEBUG": "デバッグ",
  "jobquery.title.loglevelFilter.label.VERBOSE": "詳細",
  "jobquery.title.loglevelFilter.label.INFO": "情報",
  "jobquery.title.loglevelFilter.label.WARN": "警告",
  "jobquery.title.loglevelFilter.label.ERR": "エラー",
  "jobquery.title.adhocExecutionFilter": "ジョブタイプ",
  "jobquery.title.adhocExecutionFilter.label.true": "コマンド",
  "jobquery.title.adhocExecutionFilter.label.false": "定義されたコマンド",
  "jobquery.title.adhocLocalStringFilter": "スクリプト内容",
  "jobquery.title.adhocRemoteStringFilter": "シェルコマンド",
  "jobquery.title.adhocFilepathFilter": "スクリプトファイルパス",
  "jobquery.title.argStringFilter": "スクリプトファイル引数",
  "page.unsaved.changes": "保存されていない変更があります",
  "edit.nodes.file": "ノードファイルを編集",
  "project.node.file.source.label": "ソース",
  "file.display.format.label": "フォーマット",
  "project.node.file.source.description.label": "説明",
  "project.nodes.edit.save.error.message": "コンテンツの保存エラー：",
  "project.nodes.edit.empty.description":
    "注：利用可能なコンテンツはありませんでした。",
  "button.action.Cancel": "キャンセル",
  "button.action.Save": "保存",
  "job-edit-page": {
    "nodes-tab-title": "ノード＆ランナー",
    "node-dispatch-true-label": "ランナーを介してノードに配信",
    "node-dispatch-false-label": "ランナーで実行",
    "section-title": "配信",
    "section-title-help": "ランナーとその選択されたノードを選択",
  },
  "job-exec-page": {
    "nodes-tab-title": "ランナー/ノード",
  },
  JobRunnerEdit: {
    section: {
      title: "ランナーセット",
    },
  },
  gui: {
    menu: {
      Nodes: "ノード",
    },
  },
  search: "検索",
  browse: "閲覧",
  result: "結果：",
  actions: "アクション",
  none: "なし",
  set: {
    "all.nodes.as.default.filter":
      "すべてのノードをデフォルトフィルターとして設定",
    "as.default.filter": "デフォルトフィルターとして設定",
  },
  remove: {
    "all.nodes.as.default.filter":
      "すべてのノードをデフォルトフィルターから削除",
    "default.filter": "デフォルトフィルターを削除",
  },
  "run.a.command.on.count.nodes.ellipsis": "{0} {1}でコマンドを実行",
  "create.a.job.for.count.nodes.ellipsis": "{0} {1}のジョブを作成",
  "resource.metadata.entity.tags": "タグ",
  filters: "フィルター",
  "all.nodes": "すべてのノード",
  "delete.this.filter.ellipsis": "このフィルターを削除...",
  "enter.a.filter": "フィルターを入力",
  "remove.all.nodes.as.default.filter":
    "すべてのノードをデフォルトフィルターから削除",
  "set.all.nodes.as.default.filter":
    "すべてのノードをデフォルトフィルターとして設定",
  "not.authorized": "未承認",
  "disabled.execution.run": "実行は無効です。",
  "user.at.host": "ユーザー {'@'} ホスト",
  "node.changes.success": "ノードの変更が正常に保存されました。",
  "node.changes.notsaved": "ノードの変更が保存されませんでした。",
  "node.remoteEdit.edit": "ノードを編集：",
  "node.remoteEdit.continue": "続行...",
  node: "ノード",
  "this.will.select.both.nodes": "これにより、両方のノードが選択されます。",
  "node.metadata.hostname": "ホスト名",
  "select.nodes.by.name": "名前でノードを選択",
  "filter.nodes.by.attribute.value": "属性値でノードをフィルタリング",
  "use.regular.expressions": "正規表現を使用：",
  "regex.syntax.checking": "正規表現の構文チェック",
  "edit.ellipsis": "編集...",
  "node.metadata.username-at-hostname": "ユーザー＆ホスト",
  "node.metadata.osFamily": "OSファミリー",
  "node.metadata.osName": "OS名",
  "node.metadata.osArch": "OSアーキテクチャ",
  "node.metadata.osVersion": "OSバージョン",
  "node.metadata.type": "タイプ",
  "node.metadata.username": "ユーザー名",
  "node.metadata.tags": "タグ",
  "button.Edit.label": "編集",
  "default.paginate.prev": "-",
  "default.paginate.next": "+",
  "jump.to": "ジャンプ先",
  "per.page": "ページあたり",
  "remove.default.filter": "デフォルトフィルターを削除",
  "scheduledExecution.action.edit.button.label": "このジョブを編集...",
  "scheduledExecution.action.duplicate.button.label": "このジョブを複製...",
  "scheduledExecution.action.duplicate.other.button.label":
    "このジョブを別のプロジェクトに複製...",
  "scheduledExecution.action.download.button.label": "定義をダウンロード",
  "scheduledExecution.action.downloadformat.button.label":
    "{0}でジョブ定義をダウンロード",
  "scheduledExecution.action.delete.button.label": "このジョブを削除",
  "scheduledExecution.action.edit.button.tooltip": "このジョブを編集",
  "scheduledExecution.action.duplicate.button.tooltip": "ジョブを複製",
  "enable.schedule.this.job": "スケジュールを有効にする",
  "disable.schedule.this.job": "スケジュールを無効にする",
  "scheduledExecution.action.enable.schedule.button.label":
    "スケジュールを有効にする",
  "scheduledExecution.action.disable.schedule.button.label":
    "スケジュールを無効にする",
  "scheduleExecution.schedule.disabled": "ジョブのスケジュールは無効です",
  "enable.execution.this.job": "実行を有効にする",
  "disable.execution.this.job": "実行を無効にする",
  "scheduledExecution.action.enable.execution.button.label": "実行を有効にする",
  "scheduledExecution.action.disable.execution.button.label":
    "実行を無効にする",
  "scheduleExecution.execution.disabled": "ジョブの実行は無効です",
  "delete.this.job": "このジョブを削除",
  "action.prepareAndRun.tooltip": "オプションを選択してジョブを実行...",
  "job.bulk.modify.confirm.panel.title": "ジョブの一括変更を確認",
  "job.bulk.delete.confirm.message": "選択されたジョブを本当に削除しますか？",
  "job.bulk.disable_schedule.confirm.message":
    "選択されたすべてのジョブのスケジュールを無効にしますか？",
  "job.bulk.enable_schedule.confirm.message":
    "選択されたすべてのジョブのスケジュールを有効にしますか？",
  "job.bulk.disable_execution.confirm.message":
    "選択されたすべてのジョブの実行を無効にしますか？",
  "job.bulk.enable_execution.confirm.message":
    "選択されたすべてのジョブの実行を有効にしますか？",
  "job.bulk.disable_schedule.button": "スケジュールを無効にする",
  "job.bulk.enable_schedule.button": "スケジュールを有効にする",
  "job.bulk.delete.button": "ジョブを削除",
  "job.bulk.disable_execution.button": "実行を無効にする",
  "job.bulk.enable_execution.button": "実行を有効にする",
  "job.bulk.enable_execution.success": "{0}ジョブの実行が有効になりました。",
  "job.bulk.enable_schedule.success":
    "{0}ジョブのスケジュールが有効になりました。",
  "job.bulk.disable_schedule.success":
    "{0}ジョブのスケジュールが無効になりました。",
  "job.bulk.disable_execution.success": "{0}ジョブの実行が無効になりました。",
  "job.bulk.delete.success": "{0}ジョブが削除されました。",
  "delete.selected.jobs": "選択されたジョブを削除",
  "job.bulk.panel.select.title": "一括編集のためにジョブを選択",
  "job.bulk.perform.action.menu.label": "アクションを実行",
  "job.create.button": "新しいジョブを作成",
  "job.upload.button.title": "ジョブ定義をアップロード",
  cancel: "キャンセル",
  "job.actions": "ジョブアクション",
  "job.bulk.activate.menu.label": "一括編集...",
  "job.bulk.deactivate.menu.label": "一括編集モードを終了",
  "upload.definition.button.label": "定義をアップロード",
  "new.job.button.label": "新しいジョブ",
  "job.bulk.panel.select.message":
    "{n}ジョブが選択されました | {n}ジョブが選択されました",
  "cannot.run.job": "ジョブを実行できません",
  "disabled.schedule.run": "実行は無効です。",
  "disabled.job.run": "実行は無効です。",
  "schedule.on.server.x.at.y":
    "サーバー{0}で{1}に実行するようにスケジュールされています",
  "schedule.time.in.future": "{0}後",
  never: "決して",
  disabled: "無効",
  "project.schedule.disabled": "プロジェクトのスケジュールは無効です",
  "project.execution.disabled": "プロジェクトの実行は無効です",
  "job.schedule.will.never.fire": "ジョブのスケジュールは決して発火しません",
  "scm.import.status.UNKNOWN.display.text":
    "インポートステータス：追跡されていません",
  "scm.import.status.LOADING.description":
    "インポート：ジョブステータスを読み込み中",
  "scm.export.status.DELETED.display.text": "削除済み",
  "scm.export.status.EXPORT_NEEDED.display.text": "変更あり",
  "scm.export.status.CLEAN.description": "エクスポートステータス：クリーン",
  "scm.import.status.DELETE_NEEDED.title.text":
    "インポート：ファイルが削除されました",
  "scm.export.status.EXPORT_NEEDED.title.text": "エクスポート：変更あり",
  "scm.import.status.UNKNOWN.description": "SCMインポートの追跡なし",
  "scm.import.status.REFRESH_NEEDED.display.text": "同期が必要",
  "scm.export.status.CREATE_NEEDED.description":
    "エクスポートステータス：新しいジョブ、まだSCMに追加されていません",
  "scm.import.status.DELETE_NEEDED.description":
    "インポートステータス：ソースファイルが削除されました",
  "scm.export.status.CLEAN.display.text": "変更なし",
  "scm.export.status.LOADING.description":
    "エクスポート：ジョブステータスを読み込み中",
  "scm.import.status.IMPORT_NEEDED.description":
    "インポートステータス：ジョブの変更を取得する必要があります",
  "scm.import.status.REFRESH_NEEDED.title.text": "インポート：変更を同期",
  "scm.export.status.REFRESH_NEEDED.title.text":
    "エクスポート：リモート変更を同期する必要があります",
  "scm.export.status.LOADING.display.text": "読み込み中",
  "scm.export.status.EXPORT_NEEDED.description":
    "エクスポートステータス：変更あり",
  "scm.export.status.CREATE_NEEDED.display.text": "作成済み",
  "scm.import.status.IMPORT_NEEDED.display.text": "インポートが必要",
  "scm.import.status.DELETE_NEEDED.display.text":
    "ソースファイルが削除されました",
  "scm.import.status.IMPORT_NEEDED.title.text": "インポート：受信変更",
  "scm.import.status.CLEAN.display.text": "最新",
  "scm.import.status.CLEAN.description": "インポートステータス：最新",
  "scm.import.status.REFRESH_NEEDED.description":
    "インポートステータス：ジョブの変更を取得する必要があります",
  "scm.export.status.REFRESH_NEEDED.display.text": "同期が必要",
  "scm.import.status.LOADING.display.text": "読み込み中",
  "scm.export.status.ERROR.display.text": "不明なエラーが発生しました。",
  "scm.import.status.ERROR.display.text": "不明なエラーが発生しました。",
  "scm.status.ERROR.display.text": "SCMエラー",
  "scm.export.auth.key.noAccess":
    "指定されたキーまたはパスワードにアクセスできません",
  "scm.import.auth.key.noAccess":
    "指定されたキーまたはパスワードにアクセスできません",
  "scm.action.diff.clean.button.label": "コミット情報を表示",
  "scm.import.plugin": "SCMインポートプラグイン",
  "scm.action.diff.button.label": "変更の差分",
  "scm.export.commit.job.link.title":
    "このジョブをコミットまたは追加するにはクリック",
  "scm.export.commit.link.title": "変更をコミットまたは追加するにはクリック",
  "scm.export.plugin": "SCMエクスポートプラグイン",
  "job.toggle.scm.menu.on": "SCMをオンに切り替え",
  "job.toggle.scm.menu.off": "SCMをオフに切り替え",
  "scm.import.actions.title": "SCMインポートアクション",
  "scm.export.actions.title": "SCMエクスポートアクション",
  "scm.export.title": "SCMエクスポート",
  "scm.import.title": "SCMインポート",
  "job.toggle.scm.button.label.off": "SCMを無効にする",
  "job.toggle.scm.confirm.panel.title": "SCM変更の確認",
  "job.toggle.scm.confirm.on":
    "すべてのSCM設定済みプラグインを有効にしますか？",
  "job.toggle.scm.confirm.off":
    "すべてのSCM設定済みプラグインを無効にしますか？",
  "job.toggle.scm.button.label.on": "SCMを有効にする",
  "job.scm.status.loading.message": "SCMステータスを読み込み中...",
  "page.section.Activity.for.jobs": "ジョブのアクティビティ",
  "widget.theme.title": "テーマ",
  "widget.nextUi.title": "次のUIを有効にする",
  "page.section.title.AllJobs": "すべてのジョブ",
  "advanced.search": "高度な検索",
  "jobs.advanced.search.title": "フィルターを変更するにはクリック",
  "filter.jobs": "ジョブを検索",
  "job.filter.quick.placeholder": "検索",
  "job.filter.apply.button.title": "検索",
  "job.filter.clear.button.title": "検索をクリア",
  all: "すべて",
  "job.tree.breakpoint.hit.info":
    "通知：このグループにはジョブが多すぎるため、すべてのジョブの詳細が読み込まれませんでした。欠落している詳細を読み込むにはボタンをクリックしてください。",
  "job.tree.breakpoint.load.button.title": "すべてのジョブの詳細を読み込む",
  "job.list.filter.save.modal.title": "フィルターを保存",
  "job.filter.save.button.title": "フィルターとして保存...",
  "job.list.filter.save.button": "フィルターを保存",
  "job.list.filter.delete.filter.link.text": 'フィルター"{0}"を削除',
  "app.firstRun.title": "{0} {1}へようこそ",
  "you.can.see.this.message.again.by.clicking.the":
    "このメッセージを再度表示するには、",
  "version.number": "バージョン番号",
  "in.the.page.footer": "ページのフッターにあります。",
  "no.authorized.access.to.projects":
    "プロジェクトへのアクセス権がありません。",
  "no.authorized.access.to.projects.contact.your.administrator.user.roles.0":
    "管理者に連絡してください。（ユーザーロール：{0}）",
  "page.home.loading.projects": "プロジェクトを読み込み中",
  "app.firstRun.md":
    "ありがとうございます、{0}のサブスクライバーです。\n\n" +
    "  \n\n\n" +
    "* [{0} サポートポータル &raquo;](http://support.rundeck.com)\n\n" +
    "* [{0} ドキュメント &raquo;]({1})",
  "page.home.section.project.title": "{0} プロジェクト",
  "page.home.section.project.title.plural": "{0} プロジェクト",
  "page.home.duration.in.the.last.day": "過去1日間",
  by: "によって",
  user: "ユーザー",
  "user.plural": "ユーザー",
  "page.home.project.executions.0.failed.parenthetical": "（{0} 失敗）",
  "page.home.search.projects.input.placeholder":
    "プロジェクト検索：名前、ラベル、または/regex/",
  "page.home.search.project.title":
    "{n} プロジェクトが見つかりました | {n} プロジェクトが見つかりました",
  "button.Action": "アクション",
  "edit.configuration": "設定を編集",
  "page.home.new.project.button.label": "新しいプロジェクト",
  Execution: "{n} 実行 | {n} 実行 | {n} 実行",
  in: "で",
  "Project.plural": "プロジェクト",
  discard: "破棄",
  "commandline.arguments.prompt.unquoted": "コマンドライン引数（引用符なし）：",
  usage: "使用法",
  "form.label.valuesType.list.label": "リスト",
  "scheduledExecution.option.unsaved.warning":
    "ジョブの変更を完了する前に、このオプションの変更を破棄または保存してください",
  "bash.prompt": "Bash：",
  "script.content.prompt": "スクリプト内容：",
  "rundeck.user.guide.option.model.provider":
    "Rundeckユーザーガイド - オプションモデルプロバイダー",
  save: "保存",
  "commandline.arguments.prompt": "コマンドライン引数：",
  "commandline.arguments.prompt.unquoted.warning":
    "警告！引用符なしの引数に依存すると、このジョブがコマンドインジェクションに対して脆弱になる可能性があります。注意して使用してください。",
  "add.new.option": "新しいオプションを追加",
  "add.an.option": "オプションを追加",
  "option.values.c": "1つの値|{n}つの値",
  "no.options.message": "オプションなし",
  "the.option.values.will.be.available.to.scripts.in.these.forms":
    "オプションの値は、次の形式でスクリプトで使用できます：",
  "form.option.date.label": "日付",
  "form.option.enforcedType.label": "制限",
  "form.option.usage.file.fileName.preview.description": "元のファイル名：",
  "form.option.discard.title": "オプションの変更を破棄",
  "form.option.valuesType.url.authentication.password.label": "パスワード",
  "form.option.enforcedType.none.label": "任意の値を使用できます",
  "form.option.inputType.label": "入力タイプ",
  "form.option.defaultStoragePath.present.description":
    "デフォルト値はキー保存から読み込まれます",
  "form.option.secureInput.false.label": "プレーンテキスト",
  "form.option.name.label": "オプション名",
  "form.option.valuesType.url.filter.label": "JSONパスフィルター",
  "form.option.valuesType.url.authType.label": "認証タイプ",
  "form.option.sort.description": "許可された値のリストを並べ替え",
  "form.option.usage.secureAuth.message":
    "セキュア認証オプションの値はスクリプトやコマンドで使用できません",
  "form.option.valuesType.url.label": "リモートURL",
  "form.option.valuesList.placeholder":
    "デリミタで区切られたリスト（デフォルトはカンマ）",
  "form.option.secureInput.description":
    "セキュア入力値は使用後にRundeckによって保存されません。スクリプトやコマンドで使用される場合、出力ログに値が含まれる可能性があります。",
  "form.option.valuesType.url.authentication.key.label": "キー",
  "form.option.date.description":
    "日付は次の形式でジョブに渡されます：mm/dd/yy HH:MM",
  "form.option.valuesType.url.authentication.username.label": "ユーザー名",
  "form.option.valuesType.url.authType.bearerToken.label": "ベアラートークン",
  "form.option.enforced.label": "許可された値から強制",
  "form.option.description.label": "説明",
  "form.option.save.title": "オプションの変更を保存",
  "form.option.type.label": "オプションタイプ",
  "form.option.multivalueAllSelected.label": "デフォルトで全ての値を選択",
  "form.option.secureExposed.false.label": "セキュアリモート認証",
  "form.option.valuesType.url.authentication.token.label": "トークン",
  "form.option.delimiter.label": "デリミタ",
  "form.option.valuesURL.placeholder": "リモートURL",
  "form.option.valuesType.url.authType.empty.label": "認証タイプを選択",
  "form.option.valuesDelimiter.description": "許可された値のデリミタを設定",
  "form.option.usage.file.preview.description":
    "ローカルファイルパスは次の形式でスクリプトで使用できます：",
  "form.option.secureExposed.false.description":
    "パスワード入力、スクリプトやコマンドで値は公開されません。ノードエグゼキュータによる認証にのみ使用されます。",
  "form.option.valuesType.url.filter.error.label":
    "リモートURLのJSONパスフィルターに無効な構文があります",
  "form.option.valuesType.url.authType.apiKey.label": "APIキー",
  "form.option.optionType.text.label": "テキスト",
  "form.option.secureExposed.true.label": "パスワード入力付きプレーンテキスト",
  "form.option.valuesType.url.filter.description":
    "キーのパスを使用してJSON結果をフィルタリングします。例：「$.key.path」",
  "form.option.valuesType.url.authentication.tokenInformer.header.label":
    "ヘッダー",
  "form.option.defaultStoragePath.description":
    "デフォルトのパスワード値のためのキー保存パス",
  "form.option.multivalued.label": "複数値",
  "form.option.multivalued.description":
    "複数の入力値を選択できるようにします。",
  "form.option.valuesType.url.authentication.tokenInformer.query.label":
    "クエリパラメータ",
  "form.option.create.title": "新しいオプションを保存",
  "form.option.regex.label": "正規表現に一致",
  "form.option.optionType.file.label": "ファイル",
  "form.option.valuesDelimiter.label": "リストデリミタ",
  "form.option.cancel.title": "新しいオプションの追加をキャンセル",
  "form.option.values.label": "許可された値",
  "form.option.dateFormat.description.md":
    "[momentjsドキュメント](http://momentjs.com/docs/#/displaying/format/)に記載されているように日付形式を入力してください",
  "form.option.defaultStoragePath.label": "保存パス",
  "form.option.defaultValue.label": "デフォルト値",
  "form.option.delimiter.description":
    "デリミタはすべての入力値を結合するために使用されます。任意の文字列を使用できます：' '（スペース）、','（カンマ）など。注：引用符を含めないでください。",
  "form.option.secureInput.false.description": "プレーンテキスト入力。",
  "form.option.valuesType.url.authType.basic.label": "基本",
  "form.option.valuesType.url.authentication.tokenInformer.label": "キーを挿入",
  "form.option.secureExposed.true.description":
    "パスワード入力付きプレーンテキスト、スクリプトやコマンドで値が公開されます。",
  "form.option.dateFormat.title": "日付形式",
  "form.option.label.label": "オプションラベル",
  "form.option.valuesUrl.description": "リモートJSONサービスへのURL。",
  "form.option.multivalued.secure-conflict.message":
    "セキュア入力オプションは複数の値を許可しません",
  "form.option.sort.label": "値を並べ替え",
  "form.option.usage.file.sha.preview.description": "ファイル内容のSHA-256値：",
  "Option.property.description.description":
    "説明はMarkdownでレンダリングされます。",
  "option.defaultValue.regexmismatch.message":
    "デフォルト値「{0}」は正規表現に一致しません：{1}",
  "option.multivalued.secure-conflict.message":
    "セキュア入力は複数値入力と一緒に使用できません",
  "option.defaultValue.notallowed.message":
    "デフォルト値は許可された値のリストに含まれていませんでした。値は強制されます",
  "option.enforced.secure-conflict.message":
    "セキュア入力は強制値と一緒に使用できません",
  "option.file.config.disabled.message":
    "ファイルオプションタイプの設定プラグインは有効ではありません",
  "option.defaultValue.required.message":
    "ジョブがスケジュールされているときに必須オプションのデフォルト値を指定してください。",
  "option.enforced.emptyvalues.message":
    "値が強制される場合、許可された値（リストまたはリモートURL）を指定する必要があります",
  "option.file.required.message":
    "ジョブがスケジュールされているときにファイルオプションタイプを必須にすることはできません。",
  "option.regex.invalid.message": "無効な正規表現：{0}",
  "option.file.config.invalid.message":
    "ファイルオプションタイプの設定が無効です：{0}",
  "option.delimiter.blank.message":
    "複数値オプションのデリミタを指定する必要があります",
  "option.hidden.notallowed.message":
    "隠しオプションにはデフォルト値または保存パスが必要です。",
  "option.values.regexmismatch.message":
    "許可された値「{0}」は正規表現に一致しません：{1}",
  "option.defaultValue.multivalued.notallowed.message":
    "デフォルト値に許可された値のリストに含まれていない文字列が含まれています。値は強制されます：「{0}」。注：空白は重要です。",
  "Option.required.label": "必須",
  "Option.hidden.description": "ジョブ実行ページから隠す必要があります",
  "Option.required.description":
    "ジョブを実行する際にこのオプションに空白でない値が必要です",
  "Option.hidden.label": "隠す必要があります",
  "form.option.regex.placeholder": "正規表現を入力",
  "home.user": "{n} ユーザー | {n} ユーザー | {n} ユーザー",
  "home.table.projects": "プロジェクト",
  "home.table.activity": "アクティビティ",
  "home.table.actions": "アクション",
  "option.click.to.edit.title": "クリックして編集",
  "form.option.regex.validation.error":
    "無効な値：パターンに一致する必要があります：{0}",
  "form.field.required.message": "このフィールドは必須です",
  "form.field.too.long.message": "この値は{max}文字を超えることはできません",
  "form.option.validation.errors.message":
    "変更を保存する前に検証エラーを修正してください",
  "option.list.header.name.title": "名前",
  "option.list.header.values.title": "値",
  "option.list.header.restrictions.title": "制限",
  "util.undoredo.undo": "元に戻す",
  "util.undoredo.redo": "やり直し",
  "util.undoredo.revertAll": "すべての変更を元に戻す",
  "util.undoredo.revertAll.confirm": "本当にすべての変更を元に戻しますか？",
  "option.view.required.title": "（必須）",
  "option.view.allowedValues.label": "許可された値",
  "option.view.valuesUrl.title": "リモートURLから読み込まれた値：{0}",
  "option.view.valuesUrl.placeholder": "URL",
  "option.view.enforced.title": "入力は許可された値の1つでなければなりません",
  "option.view.enforced.placeholder": "厳格",
  "option.view.regex.info.note": "値は正規表現に一致する必要があります：",
  "option.view.notenforced.title": "入力値に制限なし",
  "option.view.notenforced.placeholder": "なし",
  "option.view.action.delete.title": "このオプションを削除",
  "option.view.action.edit.title": "このオプションを編集",
  "option.view.action.duplicate.title": "このオプションを複製",
  "option.view.action.moveUp.title": "上に移動",
  "option.view.action.moveDown.title": "下に移動",
  "option.view.action.drag.title": "ドラッグして並べ替え",
  "pagination.of": "の",
  uiv: {
    datePicker: {
      clear: "クリア",
      today: "今日",
      month: "月",
      month1: "1月",
      month2: "2月",
      month3: "3月",
      month4: "4月",
      month5: "5月",
      month6: "6月",
      month7: "7月",
      month8: "8月",
      month9: "9月",
      month10: "10月",
      month11: "11月",
      month12: "12月",
      year: "年",
      week1: "月",
      week2: "火",
      week3: "水",
      week4: "木",
      week5: "金",
      week6: "土",
      week7: "日",
    },
    timePicker: {
      am: "午前",
      pm: "午後",
    },
    modal: {
      cancel: "キャンセル",
      ok: "OK",
    },
    multiSelect: {
      placeholder: "選択...",
      filterPlaceholder: "検索...",
    },
  },
  "scheduledExecution.jobName.label": "ジョブ名",
  "scheduledExecution.property.description.label": "説明",
  "job.editor.preview.runbook": "Readmeのプレビュー",
  "choose.action.label": "選択",
  "scheduledExecution.property.description.plain.description":
    "説明はプレーンテキストで表示されます",
  "scheduledExecution.property.description.description":
    "説明の最初の行はプレーンテキストで表示され、残りはMarkdownでレンダリングされます。\n\n" +
    "Markdownについては[こちら](http://en.wikipedia.org/wiki/Markdown)をご覧ください。\n\n" +
    "拡張説明内で、{'`{{job.permalink}}`'}をURLとして使用してジョブにリンクできます。例：`[ジョブを実行]({'{{job.permalink}}#runjob'})`\n\n" +
    "HRセパレータ`---`を単独で行に使用することでReadmeを追加でき、その後のすべてが[Markdeep](https://casual-effects.com/markdeep)を使用して別のタブにレンダリングされます。",
  "scheduledExecution.groupPath.description": "グループは/で区切られたパスです",
  more: "もっと見る…",
  less: "少なくする…",
  "job.edit.groupPath.choose.text": "使用するグループ名をクリック",
  "scheduledExecution.property.executionLifecyclePluginConfig.help.text":
    "選択されたプラグインはこのジョブに対して有効になります。",
  Workflow: {
    label: "ワークフロー",
    property: {
      keepgoing: {
        true: { description: "失敗する前に残りのステップを実行します。" },
        false: { description: "失敗したステップで停止します。" },
        prompt: "ステップが失敗した場合：",
      },
      strategy: {
        label: "戦略",
      },
    },
  },
  "plugin.choose.title": "プラグインを選択",
  "plugin.type.WorkflowStep.title.plural": "ワークフローステップ",
  "plugin.type.WorkflowStep.title": "ワークフローステップ",
  "plugin.type.WorkflowNodeStep.title.plural": "ワークフローノードステップ",
  "plugin.type.WorkflowNodeStep.title": "ワークフローノードステップ",
  "JobExec.nodeStep.true.label": "ノードステップ",
};

export default messages;
