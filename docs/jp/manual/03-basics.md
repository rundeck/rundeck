% Rundeck の基本
% Alex Honor; Greg Schueler
% November 20, 2010

この章では Rundeck を使う上での基本をカバーします。まず Rundeck の GUI と CUI 両方のインターフェイスについて説明します。次にプロジェクトの準備からコマンド実行までの操作方法を説明します。そして実行制御に用いているコマンドディスパッチャについて理解を深め、最後に実行履歴の見方と利用方法を学びます。

## Rundeck のインターフェイス

Rundeck には 2 つの主要なインターフェイスがあります。

*   GUI: HTML ベースのグラフィカルなコンソール
*   CUI: Shell ツール

どちらのインターフェイスからも、リソースの閲覧・ディスパッチ(コマンドの発信)・ジョブの保存と実行が可能です。

さらに Rundeck にはサーバーのプログラムとやり取りできる WebAPI も用意されています。[Rundeck API](../api/index.html) を参照して下さい。

### グラフィカルコンソール

Rundeck の使用を開始するにはまず、Rundeck サーバーの URL にアクセスします。そして Rundeck のユーザーディレクトリの設定によって定義されたアカウントでログインします。

Web インターフェイスのデフォルトのポートは 4440 です。以下の URL にアクセスして下さい。

<http://localhost:4440>

アクセスするとログインページが表示されます。デフォルトのユーザー名とパスワードは `admin/admin` ですので、それらを入力してください。

#### ナビゲーション

Rundeck ページのヘッダーは、"Run"（実行）・"Jobs"（ジョブ）・"History"（履歴） タブページを行き来するためのグローバルナビゲーションになっています。また、ログアウト・ユーザーのプロフィール画面・オンラインヘルプへのリンクも載っています。

![Top navigation bar](../figures/fig0201.png)

Run
:   Run ページは、アドホックコマンドを実行する際に使います。Run ページではあなたのプロジェクトのリソースモデルとして設定されたノードをフィルタリングした結果を表示しています。条件にマッチしたノードだけリストアップするというようにフィルタを使う事ができます。

Jobs
:   Jobs ページでは、保存されたジョブの一覧・ジョブの作成・ジョブの実行を、権限のある人が行えます。Jobs ページでも条件にマッチしたジョブだけをリストアップすることができます。このジョブのフィルタリングはユーザープロフィールに保存することができます。そのユーザーに閲覧権限があるジョブのみが表示されます。

History
:   History ページでは、現在実行中のコマンド・過去のコマンド実行履歴を、権限のある人が閲覧できます。履歴についても、選択したパラメーターにマッチする履歴のみリストアップできます。それらのフィルタリングも保存可能です。現在のフィルタリング設定はページ右上にある RSS リンクにも設定できます。

Project menu
:   トップのナビゲーションバーの中に、プロジェクトを選択できるプルダウンメニューがあります。もし一つしかプロジェクトが存在しない場合は、デフォルトでそれが選択されています。
     
Admin
:   "admin" グループに属するアカウントまたは "admin" 権限が付与されたアカウントでログインした場合は、右上のログイン名がある部分に、レンチのアイコンも表示されています。このページでは全てのグループ・ユーザーの閲覧やプロフィール情報の編集ができます。

User profile
:   ユーザー名・メールアドレス・どのグループに属しているかといった情報が表示されます。編集も可能です。

Logout
:   ログアウトのリンクを押すとログインセッションを終えてログアウトします。

Help
:   オンラインヘルプページを開きます

#### Now running

"Now runnig" (実行中) という項目が Run ページと Jobs ページの上部の実行キュー内にあります。現在実行されているアドホックコマンドやジョブが、その名前や関連情報と共にリストアップされます。また誰が実行しているか表示され、出力結果へのリンクも表示されます。

![Now running](../figures/fig0215.png)

過去に実行したことがあるジョブに関しては、進捗度合いがわかるバーが表示されます。

### Shell ツール

Rundeck にはディスパッチ用の Shell ツールが数多く含まれています。ジョブの定義を読み込んで実行し、発信キューとやり取りできます。これらのコマンドツール群は、グラフィカルコンソールから利用できる機能の代わりとなります。

[dispatch]
 ~ アドホックコマンドやスクリプトを実行します
[rd-queue]
 ~ 実行中のジョブに対して ディスパッチャに問い合わせることができます。ジョブを中断することもできます。
[rd-jobs]
 ~ テキストファイルに定義されたジョブを読み込んでリストアップします。
[run]
  ~ Invoke the execution of a stored Job
  ~ 保存されているジョブを実行できます
[rd-project]
 ~ 新しい Rundeck プロジェクトをセットアップします
[rd-setup]
 ~ Rundeck の設定を読み込み直します。
  
コマンドの細かい使い方やオプションに付いては、オンラインマニュアルページを参照してください。

[dispatch]: dispatch.html
[rd-queue]: rd-queue.html
[rd-jobs]: rd-jobs.html
[run]: run.html
[rd-project]: rd-project.html
[rd-setup]: rd-setup.html

## Rundeck のセットアップ

Rundeck のプロジェクト機能は、管理業務を遂行するためのスペースを提供します。

プロジェクトはグラフィカルコンソールまたはシェルツール "rd-project" のどちらからでもセットアップできます。

グラフィカルツールにログインした後、上部のナビゲーションバー内にプロジェクトメニューがあることに気付くでしょう。もしプロジェクトが存在しなければ、新しいプロジェクトを作るよう勧められます。

![Create project prompt](../figures/fig0203-a.png)

プロジェクト名を入力するだけでセットアップが開始できます。その他の項目に付いては後から [GUI 管理者ページ](../administration/configuration.html#GUI 管理者ページ)にて変更できます。

プロジェクト名の入力が完了すると、Rundeck はその名前で初期化した後、"Run" ページに戻ります。

プロジェクトの作成はナビゲーションバーのプロジェクトメニューから "Create a new project..." を選択する事でいつでも行うことができます。

![Create project menu](../figures/fig0203.png)

シェルツール "rd-project" でもプロジェクトの作成ができます。

Rundeck のサーバー上で "rd-project" コマンドを実行し、プロジェクト名の指定をします。ここでは例として、"examples" という名前を付けます:

    rd-project -a create -p examples

コマンドの実行後、グラフィカルコンソールにてプロジェクトメニューに今作成したプロジェクトが追加されている事が分かります。

プロジェクトを作る際に同時に他の項目も追加することができます:

    rd-project -a create -p examples --project.ssh-keypath=/private/ssh.key

プロジェクトのセットアッププロセスが、サーバー内でプロジェクトの設定を生成し、リソースモデルを準備します。  

![Run page after new project](../figures/fig0204.png)

Rundeck サーバー自身を指す 1 つのノードがリストアップされます。このノードについては赤文字の "server" という識別子が付きます。

### リソースモデル

リソースモデルとは Rundeck がコマンドをディスパッチでき、サーバーのメタデータが紐づけられているノードのことです。どの Rundeck プロジェクトもそれぞれリソースモデルを持つ事になります。

初期状態のリソースモデルは Rundeck サーバー自身の情報だけを持っており、Rundeck サーバー自身にだけローカルコマンドが実行できます。プロジェクトごとのリソースモデルは "Run" ページで見ることができます。

シェルツールからもノードリソースのリストアップができます。特定のプロジェクトを指定したい場合は `-p <project>` オプションを付けます。

下記コマンドが "examples" というプロジェクトに登録されているサーバーのリストを列挙するディスパッチコマンドになります。`-v` オプションを付けるとリストの情報をより詳細に表示します:

    $ dispatch -p examples -v	
     strongbad:
        hostname: strongbad
        osArch: x86_64
        osFamily: unix
        osName: Mac OS X
        osVersion: 10.6.2
        tags: ''

ノードリソースは "hostname" のような標準のプロパティを持ち、プロパティは属性を通して拡張することもできます。便利なプロパティのひとつとして、"tags" プロパティがあります。ノードに付ける *タグ* はテキストのラベルのことで、ある環境におけるひとつの役割として使ったり、あるグループのメンバであることを示すことに使ったり等、ノードの分類に用います。一つのノードに複数の tags を付けることもできます。

上記の出力では "strongbad" というノードに現在は空の tags プロパティ `tags: ''` が付いていることを示しています。

後々コマンドディスパッチを行う際にノード群をフィルタリングするためにタグを用いるため、管理するノードにどんなタグを付けるか考えるのは重要なことです。

どのプロジェクトにも [project.properties](../administration/configuration.html#project.properties) という名前の設定ファイルが存在し、`$RDECK_BASE/projects/project/etc/project.properties` というパスに置かれています。

この設定ファイルはリソースモデルデータへのアクセス、取り込みのための 2 つの基本的なプロパティを持っています。

*   `project.resources.file`: リソースモデルドキュメントを読み込む際のローカルファイルパス
*   `project.resources.url`: 外部のリソースモデルへの URL (オプション)

さらに、あるリソースモデルを他のソースから取り出してくることで、ひとつのプロジェクトで複数のリソースモデルを追加することができます。[リソースモデルソース](plugins.html#リソースモデルソース) を参照してください。

Rundeck のリソースモデルドキュメントフォーマットに変換すれば、様々なソースから Rundeck にリソースモデルデータを取り込むことができます。([リソースモデルドキュメントフォーマット](rundeck-basics.html#リソースモデルドキュメントフォーマット)を参照して下さい) `project.resource.url` にお好みの URL リソースモデルソースをセットしてください。

ここに、先ほどの `dispatch -v` コマンドの出力結果に該当する "examples" プロジェクトのための XML ドキュメントがあります:

    <project>
        <node name="strongbad" type="Node" 
          description="the Rundeck server host" tags="" 
          hostname="strongbad" 
          osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.2"
          username="alexh" 
          editUrl="" remoteUrl=""/>
    </project>

XML のルートノードは project で、そこに "strongbad" に対する記述がひとつのノードとして書かれています。ノードの XML タグは数ある必須属性、オプション属性のうちのひとつになります。ノードを追加する際には project XML タグの内側に node XML タグを追加してください。

ホスト strongbad はタグの定義が何もされていません。ひとつまたは複数のタグが定義できます。区切りにはカンマ（,）を使います。（例. `tags="tag1,tag2"`）

以下の例では "homestar" というノードが必須属性とともに記述されています:

        <node name="homestar" type="Node" 
          hostname="192.168.1.02" 
          username="alexh" />

`name` と `type` の値がリソースモデルの中でノードを識別するために利用される一方、`hostname` と `username` の値は SSH コネクションに用いられます。ポート情報を加えて上書きする事も出来ます。(例. `hostname="somehost:2022"` ) これはデフォルトと違うポートを使って SSH を使う際に便利です。

Chef, Puppet, Nagios, Amazon EC2, RightScale または社内データベースなど、別のツール内でホストに関する情報をメンテナンスする機会があります。これらのツールは、ネットワーク内に配置されたノードの情報に関して認証を考慮しているでしょう。従って、認証ツールへのインタフェースをつくって Rundeck の URL リソースモデルソースとするのがベストです。ツールのフォーマットから Rundeck が理解可能なフォーマットへと変更するシンプルな CGI スクリプトで実現できます。

もちろん、基本的な選択肢として XML ドキュメントとして情報をメンテナンスするというのもあります、それらは Rundeck に定期的にエクスポートされるソースリポジトリに取り込まれます。もしあなたの組織のホストインフラストラクチャが頻繁に変化するなら実用的な手段になります。

Rundeck のウェブサイトで URL リソースモデルソースをチェックしてください。 自分たちのリソースモデルを作ることに興味がある場合は[リソースモデルソース](../administration/node-resource-sources.html#リソースモデルソース)の外部のデータプロバイダーとの統合項目を参照してください。

### リソースモデルドキュメントフォーマット

Rundeck には今のところ 2 種類のリソースモデルドキュメントフォーマットが組み込まれています。

*   XML: [resource-v13(5) XML](../manpages/man5/resource-v13.html).  Format name: `resourcexml`.
*   Yaml: [resource-v13(5) YAML](../manpages/man5/resource-yaml-v13.html). Format name: `resourceyaml`.

[リソースフォーマットプラグイン](plugins.html#リソースモデルフォーマットプラグイン)により他のフォーマットを使うことも可能です。

## プラガブルリソースモデルソース
どのプロジェクトもリソースモデル情報のソースを複数持つことができます。そしてリソースモデルに、新しいソースとしてプラグインを使用または作ることができます。

そのソースは GUI の管理者ページから設定できます（[GUI 管理者ページ](../administration/configuration.html#gui-管理者ページ)を参照してください）。またはプロジェクトの設定ファイルから編集することもできます（[リソースモデルソース](plugins.html#リソースモデルソース)を参照してください）

## コマンド実行

Rundeck は*アドホック*と*ジョブ*の2つの実行モードをサポートしています。

*アドホックコマンド*はコマンドディスパッチャを通してあらゆるコマンドまたはシェルスクリプトを実行できます。`dispatch` という名前のコマンドツールとグラフィカルコンソール両方から実行可能です。

*ジョブ*には 1 つのコマンドまたは連続した複数のコマンドを実行させる指令を指定することができます。それは一回実行するだけも良いですし、後から使う時のために名前を付けて保存もできます。保存されたジョブは `rd-queue` というシェルツールから実行を開始します、プロセス進捗の確認も可能です。

## ディスパッチャ オプション

ディスパッチャの処理は様々なタイプのオプションによりコントロールされます。

実行コントロール

:   "threadcount" を設定すると同時実行数をコントロールできます。また、"keepgoing" オプションが true になっていればいくつかのノードで処理に失敗しても実行を続けさせることができます。

ノードフィルタ

:   インクルードフィルタとエクスクルードフィルタリングをフィルタリングオプションで指定することで、プロジェクトリソースモデルの中でどのノードにコマンドを実行するのか決めることができます。

フィルタキーワード

:   フィルタリングのインクルードとエクスクルードパターンに用いられるキーワード。"Tags" キーワードはさらに、OR と AND といったブール演算子を条件の連結に使うことができます。

フィルタコンビネーション

:   すべてのキーワードはコマンドラインにて include と exclude オプションを指定することにより何個でも連結できます。
  
グラフィカルコンソールからでも `dispatch` ツールからでもリソースモデルを問い合わせる事ができます。

#### GUI でのノードフィルタリング

プロジェクトのノードリソースは Run ページに表示されます。ナビゲーションバーの中にあるプロジェクトメニューを使ってプロジェクトを選択します。そうするとデフォルトの状態でノードがフィルタリングされています。

フィルタフォームからインクルード・エクスクルードパターンを使ってノードをフィルタリングできます。フォームは "Filter" ボタンを押すと出てきます。三角形のトグルアイコンを押してフォームを表示させてください。

![Resource filter link](../figures/fig0205.png)

フォームが表示されると、シンプルなインクルード表現を設定できる Include と、エクスクルード表現を作れる "Extented Filters..." リンクに分かれていることがわかります。

![Resource filter form](../figures/fig0206.png)

フィルタフォームを埋めた後、"Filter" ボタンを押して新たにフィルタリングされたノードリストを生成します。"Clear" ボタンを押すとフォームがリセットされます。

インクルード・エクスクルードフィルタは次に示すキーワードごとにノードをフィルタリングすることができます：Name, Tags, Hostname, OS Name, OS Family, OS Architecture, OS Version and Type

どのキーワードでも正規表現を使うことができます。``.\*`` というパターンは何もマッチしません。

フィルタリングの結果 20 以上のノードがマッチする場合は、結果がページングされます。
  
#### シェルでのノードフィルタリング

`dispatch` コマンドは -I (include) と -X (exclude) オプションを使ってどのノードをインクルード・エクスクルードするか指定できます。これらのオプションで、ひとつの値/値のリスト/正規表現を利用できます。

*例*

OS name が Linux であるものをリストアップ(`-p` はプロジェクトの指定):

    dispatch -p examples -I os-name=Linux

"web" という接頭辞が着いたものを除き、Linux のノードをリストアップ:

    dispatch -p examples -I os-name=Linux -X "web.*"

"web" と "prod" というタグが着いたノードをリストアップ:

    dispatch -p examples -I tags=web+prod

"web" というタグが着いたノード全てに対し、10 スレッド同時に apachectl をリスタートするコマンドを発行。エラーが起こっても続けるオプション付き:

    dispatch -p examples -I tags=web -K -C 10 -- sudo apachectl restart 

ディスパッチャーコマンドで利用できるオプションについて完全なリファレンスが必要な場合は、[rd-options(1) マニュアルページ](../manpages/man1/rd-options.html) を参照してください。
  
### アドホックコマンド

一般的に、アドホックコマンドはシェルスクリプトやインタラクティブなターミナルにて実行されます。Rundeck ではアドホックコマンドは `dispatch` コマンドとグラフィカルコンソール両方にて実行可能です。

#### シェルツールでのコマンド実行

個々のコマンドやシェルスクリプトファイルを実行するには `dispatch` を使います。

`dispatch` を使って UNIX `uptime` コマンドを実行しシステムステータスを出力する例です:

    $ dispatch -I os-family=unix -- uptime
    Succeeded queueing Workflow execution: Workflow:(threadcount:1){ [command( exec: uptime)] }
    Queued job ID: 7 <http://strongbad:4440/execution/follow/7>


この ``uptime`` コマンドはキューに追加された後に実行されました。`uptime` コマンドの出力は `dispatch` コマンドの出力に出ている URL に載っています。(例, http://strongbed:4440/execution/follow/7)

キューに追加せず、すぐにコマンドを実行したい場合があります。``--noqueue`` オプションを使って実行し、コンソールの出力を追って下さい。

    $ dispatch -I os-family=unix  --noqueue -- uptime
    [ctier@centos54 dispatch][INFO]  10:34:54 up 46 min,  2 users,  load average: 0.00, 0.00, 0.00
    [alexh@strongbad dispatch][INFO] 10:34  up 2 days, 18:51, 2 users, load averages: 0.55 0.80 0.75
    [examples@ubuntu dispatch][INFO]  10:35:01 up 2 days, 18:40,  2 users,  load average: 0.00, 0.01, 0.00

**ノート** "--noqueue" フラッグは、テスト実行やデバッグ実行には便利ですが、中央管理の実行キューで管理されないやり方なため、コマンド実行状況が視覚化されません。

`dispatch` コマンドは、そのノードでコマンドを実行したかのようにヘッダーを付けて出力メッセージを出してくれます。ヘッダーのフォーマットにはログインユーザーとコマンドが実行されているノード情報が含まれます。

Unix `whoami` コマンドを実行して、各ノードごとにディスパッチされたコマンドを実行しているユーザー ID を見てみます。

    $ dispatch -I os-family=unix --noqueue -- whoami
    [ctier@centos54 dispatch][INFO] ctier
    [alexh@strongbad dispatch][INFO] alexh
    [examples@ubuntu dispatch][INFO] examples

リソースモデルは、ディスパッチコマンドを実行するログインユーザーをノード毎に定義していることがわかります。この機能により、ノードへそれぞれ違ったロールを割り当てることが簡単にできます、つまりプロセスの管理のためにログインユーザーを使い分けることができるということです。[resource-v13(5) XML](../manpages/man5/resource-v13.html) または [resource-v13(5) YAML](../manpages/man5/resource-yaml-v13.html) のユーザネーム属性について見てみてください。

`dispatch` コマンドはシェルスクリプトも実行できます。ここにちょっとだけシステム情報を生成する簡単なシェルスクリプトがあります:

    #!/bin/sh
    echo "info script"
    echo uptime=`uptime`
    echo whoami=`whoami`
    echo uname=`uname -a`

-s オプションを使って "info.sh" というスクリプトファイルを指定します。

    $ dispatch -I os-family=unix -s info.sh
    
`dispatch` コマンドは "info.sh" スクリプトをコピーしてサーバーと "unix" 系ノードに置き、実行します。

#### グラフィカルコマンドシェルでの実行

Rundeck のグラフィカルコンソールでもフィルタリングしたノードリソースに対してアドホックコマンドを実行できます。コマンドプロンプトは SSH コマンドまたはディスパッチシェルツールで実行可能な様々なコマンド文字列を受け付けます。

しかしながら、どんなコマンドを打つときでも、まずディスパッチしたいノードを含んだプロジェクトの選択をする必要があります。プロジェクトメニューから希望のプロジェクトを選んでください。プロジェクトを選択すると、"Command" というラベルが付いた水平なテキストフィールドがあるのがわかると思います。これが Rundeck のアドホックコマンドプロンプトになります。
![Ad hoc command prompt](../figures/fig0207.png)

コマンドプロンプトを利用するには、テキストフィールドに実行したいアドホックコマンドを入力し、"Run" ボタンを押してください。コマンドは現在コマンドプロンプトツールバーの下にリストアップされたノード全てに対してディスパッチされます。また、コマンドプロンプトはそのコマンド実行が完了するまで利用できなくなります。コマンド実行の出力は下記のように出力されます。([出力結果](rundeck-basics.html#コマンド実行結果を確認する)を見てみてください)

![Ad hoc execution output](../figures/fig0208.png)

また、"Now running" と書かれた実行中のアドホックコマンドのリストがコマンドプロンプトの上部に表示されることにも気付くでしょう。全てのコマンド実行はそこにリストアップされます。リストアップされている実行中のコマンドにはどれも開始時間・経過時間・出力結果の別ページでの表示リンクが付いています。

![Now running ad hoc command](../figures/fig0207-b.png)

ページの一番下には、選択されたプロジェクトにおける過去 24 時間の全てのコマンドの実行履歴がある "History" という項目があります。コマンドの実行完了後、"History" 項目に新しい履歴が追加されます。黄色でハイライトされた行は、"History" 項目に追加されかつまだ実行中のものであることを表しています。

![Run history](../figures/fig0207-c.png)

"History" 項目のコマンド実行履歴は並び替える事も可能です。"Summary" カラムは実行されたコマンド/スクリプトを表しています。"Node Failure Count" カラムはエラーが起きたノードの数を表しています。エラーが 1 つもないときは、"ok" と表示されています。"User" と "Time" カラムは、"いつ" "誰が" コマンドを実行したかを表しています。

#### コマンド実行結果を確認する

アドホックコマンドの実行結果はコマンドプロンプトの下に表示されます。

ここではコマンドの実行結果を様々なフォーマットで見る事ができます。

Tail 出力モード

:   ログファイルに対して Unix の `tail -f` コマンドを実行しているかのような形でコマンドの実行結果を表示します。デフォルトでは 20 行表示されますが、"-" または "+" ボタンを押す事で変更できます。現在の表示行数が設定されているテキストフィールドを修正することでも変更可能です。
    ![Ad hoc execution output](../figures/fig0208.png)

注釈モード

:   注釈モードでは、選択されたラベル（"Annotated" を選択するとその右側に出てくるラベル）に沿った表示をします。昇順・降順にする "Top" "Bottom" や コマンドのまとまり毎に表示を区切る "Group commands" や出力結果の詳細を折りたたみ表示にする "Collapse" などです。折りたたんだものも右端の三角形または "3-exec" のように書かれたリンクを押す事で開くことができます。
    ![Annotated output](../figures/fig0209.png)

コンパクトモード

:   コンパクトモードでは、ノード毎に表示が区切られます。デフォルトでは出力結果の詳細は折りたたまれており、右端の三角形ボタンを押す事で開きます。
    ![Node output](../figures/fig0210.png)

##### コマンド実行フォローページ

コマンドの出力結果が別ページとして切り出されていると便利なときがあります。目的のひとつとして例えば、出力結果がどうなっているか知りたい人にリンクをシェアするためといったことがあります。"Now running" 項目の "output >>" リンクをクリックするとそのコマンド実行結果に関することのみが載っているページへ飛びます。

ブラウザのロケーションバーの URL にも気付くでしょう。URL を他の人にシェアすることも可能です。URL には実行 ID（EID）が入っています。

     http://rundeckserver/execution/follow/{EID}

実行が完了後、コマンドはあるステータスを取ります:

*   成功:   フィルタリングしたノードに対してのコマンド実行で一切エラーがない状態。
*   失敗:   ひとつ以上エラーが発生した状態。エラーが起きたノードが表示されます。同じコマンドを実行したいときのために、"Retry Failed Nodes..." というリンクも表示されます。

このページをテキストファイルとしてダウンロードすることも可能です。"Download" というリンクを押してデスクトップに保存してください。

### コマンド実行をコントロールする

並列でコマンドを実行したい場合は、"-C" オプションを使い並列させたいスレッド数を指定します。Linux ホストに対して、2 スレッド平行で uptime コマンドを実行する例はこうなります:

    dispatch -I os-name=Linux -C 2 -- uptime

エラーが起こったときに、構わず続けたい・リトライさせたい場合には "-K/-R" フラッグを使います。ここにホストが 4440 ポートをリッスンしていた状態であるか確認するスクリプトの例があります。4440 ポートがリッスンされていない場合にはコード 1 を取ります。

    #!/bin/sh
    netstat -an | grep 4440 | grep -q LISTEN
    if [ "$?" != 0 ]; then
    echo "not listening on 4440"
    exit 1;
    fi
    echo  listening port=4440, host=`hostname`;

強制続行フラッグが設定されていない場合、コマンドまたはスクリプトにて 0 以外の終了コードが返ってくるとディスパッチは失敗と判断され終了します。

    $ dispatch -I os-family=unix -s /tmp/listening.sh --noqueue
    [alexh@strongbad dispatch][INFO] Connecting to centos54:22
    [alexh@strongbad dispatch][INFO] done.
    [ctier@centos54 dispatch][INFO] not listening on 4440
    error: Remote command failed with exit status 1

centos54 でのスクリプト実行が失敗すると直ちにディスパッチもエラー終了となっています。

もう一度コマンドを実行する際、今度は "-K" の強制続行フラッグを付けて、どのノードにおいてスクリプト実行が失敗したかを出力しつつ処理を続行させます。

    $ dispatch  --noqueue -K -I tags=web -s /tmp/listening.sh
    [alexh@strongbad dispatch][INFO] Connecting to centos54:22
    [alexh@strongbad dispatch][INFO] done.
    [ctier@centos54 dispatch][INFO] not listening on 4440
    [ctier@centos54 dispatch][ERROR] Failed execution for node: centos54: Remote command failed with exit status 1
    [alexh@strongbad dispatch][INFO] listening port=4440, host=strongbad
    [alexh@strongbad dispatch][INFO] Connecting to 172.16.167.211:22
    [alexh@strongbad dispatch][INFO] done.
    [examples@ubuntu dispatch][INFO] not listening on 4440
    [examples@ubuntu dispatch][ERROR] Failed execution for node: ubuntu: Remote command failed with exit status 1
    error: Execution failed on the following 2 nodes: [centos54, ubuntu]
    error: Execute this command to retry on the failed nodes:
	    dispatch -K -s /tmp/listening.sh -p examples -I
	    name=centos54,ubuntu
	
### コマンド実行をキューに入れる

デフォルトでは、`dispatch` によるコマンドライン上でのコマンドまたはスクリプトの実行は Rundeck のテンポラリジョブにキューされます。`dispatch` コマンドは、グラフィカルコンソールでの "その場限りの実行" と同等です。

下記のスクリプトは、それぞれの経路でポートがリッスンされているかを定期的に確認する長めの処理です。そして引数を渡しても渡さなくても実行できます。

    $ cat ~/bin/checkagain.sh 
    #!/bin/bash
    iterations=$1 secs=$2 port=$3
    echo "port ${port:=4440} will be checked ${iterations:=30} times waiting ${secs:=5}s between each iteration" 
    i=0
    while [ $i -lt ${iterations} ]; do
      echo "iteration: #${i}"
      netstat -an | grep $port | grep LISTEN && exit 0
      echo ----
      sleep ${secs}
      i=$(($i+1))
    done
    echo "Not listening on $port after $i checks" ; exit 1

`dispatch` コマンドにより、コマンド実行処理が Rundeck のキューに入り、テンポラリジョブとして管理されます。`-I centsos54` の部分は "centos54" というノードのみに実行ということを意味しています。

    $ dispatch -I centos54 -s ~/bin/checkagain.sh 
    Succeeded queueing workflow: Workflow:(threadcount:1){ [command( scriptfile: /Users/alexh/bin/checkagain.sh)] }
    Queued job ID: 5 <http://strongbad:4440/execution/follow/4>

引数を渡すには "\--"（ダッシュ 2 つ）の後に指定します。

    $ iters=5 secs=60 port=4440
    $ dispatch -I centos54 -s ~/bin/checkagain.sh -- $iters $secs $ports

### コマンド実行状況をトラッキングする

"Run" ページの上部にある["Now Running"](rundeck-basics.html#実行中)の部分から、キューに入っているアドホックコマンドや一時的なジョブまたは保存されたジョブの実行の経過を追うことができます。

シェルツール [rd-queue](http://rundeck.org/docs/manpages/man1/rd-queue.html) を使う事でもコマンド実行の経過を追う事ができます。

    $ rd-queue -p project
    Queue: 1 items
    [5] workflow: Workflow:(threadcount:1){[command( scriptfile: /Users/alexh/bin/checkagain.sh)]} <http://strongbad:4440/execution/follow/5>

実行キューに入っているジョブはどれも実行 ID というのを持っています。上記の例で言うと、キューに 1 つジョブが入っていて 5 というのが ID に当たります。

実行中のジョブは `rd-queue kill` コマンドによって kill することもできます。"-e" オプションにて kill する実行 ID を指定してください。

    $ rd-queue kill -e 5
    rd-queue kill: success. [5] Job status: killed

### プラグイン

Rundeck はプラグインモデルを採用しており、それによって特定のノード・プロジェクト・Rundeck 本体がリモートで（またはローカルで）コマンドやスクリプトを実行する方法をフルカスタマイズできます。

Rundeck はリモートノードとローカルの Rundeck サーバー自身へ SSH を通したコマンド実行を扱う内部プラグインをデフォルトで使っています。

プラグインのインストールは、あなたがインストールした Rundeck の `libext` ディレクトリにプラグインをコピーすることで完了します。

一般的なプラグインは、特定の"サービス"のために"プロバイダ"を追加します。"サービス"はノードでのコマンド実行を担うもので "node-executor" と "file-copier" サービスがあります。
Plugins are used to add new "providers" for particular "services".  The services used for  command execution on nodes are the "node-executor" and "file-copier" services.

特定のプラグインを使うには、`framework.properties` ファイル と `project.properties` ファイルを設定するか、またはプロジェクトリソースに定義されたノードへ属性を追加します。より詳細な設定については [プロバイダを使う](plugins.html#プロバイダを使う) を参照して下さい。

内部の SSH コマンド実行プラグインについては下記で説明します、プラグインについての詳細は [Rundeck プラグイン](#rundeck-プラグイン)の章 に載っています。

#### SSH プラグイン

デフォルトでは Rundeck はリモートのノードでコマンドを実行するために SSH を使い、リモートノードへスクリプトをコピーするのに SCP を使い、ローカルのノード（Rundeck サーバー）に対してはローカルでコマンドやスクリプト実行を行います。

SSH プラグインでは、SSH 接続を確立するために、以下のプロパティが定義されている必要があります：

*   `hostname`：リモートノードのホスト名。デフォルトのポート以外を使う場合は "hostname:port" というフォーマットになります。デフォルトポートは 22 です。
*   `username`：接続するリモートノードのユーザー名。

リモートノードでスクリプトが実行される時、まず SCP にてスクリプトがコピーされた後、そのスクリプトが実行されます。SSH 接続プロパティに加えて、SCP についてもノードで定義しておくプロパティが存在します：

*   `file-copy-destination-dir`：実行前にスクリプトファイルが置かれるリモートノード上のディレクトリ。デフォルトでは、Windowns のノードだと `C:/WINDOWS/TEMP/` その他のノードだと `/tmp` になります。
*   `osFamily`：Windows のノードには "windows" と指定します。

さらに、SSH と SCP 両方とも、SSH パスワード認証のために、リモートノードに公開鍵/秘密鍵の設定をしておく必要があります。

*   SSH サーバーの設定についてのより詳しい情報は [管理者向けガイド - SSH](../administration/ssh.html) ページを参照してください。
*   SSH のためのノードの設定についてのより詳しい情報は[SSH プロバイダ](plugins.html#ssh-プロバイダ) ページを参照してください。

#### 組み込みプラグイン

Rundeck にはテストや開発に利用するためのプラグインが 2 つデフォルトで入っています。([プリインストールプラグイン](plugins.html#プリインストールプラグイン) を参照)

*   [スタブプラグイン](plugins.html#スタブプラグン) : 実行はせずに、ただコマンドやスクリプトを表示します。
*   [スクリプトプラグン](plugins.html#スクリプトプラグン) :外部スクリプトをコマンドのように扱います。自分の [スクリプトプラグン開発](../developer/plugin-development.html#スクリプトプラグン開発) モデルを開発するのに便利です。

## 履歴

キューに入ったアドホックコマンドの履歴だけでなく、テンポラリ・保存したジョブの実行履歴が Rundeck サーバーに保存されています。履歴データは "History" ページ内にてフィルタリングして見る事ができます。

![History page](../figures/fig0211.png)

### イベント履歴のフィルタリング

History ページはデフォルトで直近一日分のコマンド実行履歴をリストアップしています。ページには履歴一覧を増やすまたは制限するフィルタリング機能が付いています。

フィルタリング機能は、条件を指定できる項目がいくつもあります：

*   Within: 期間の指定。1 日間、1 週間、1 ヶ月、その他（いつからいつまでを指定）の中から選びます
*   Name: ジョブの名前の指定
*   Project: プロジェクト名の指定。プロジェクトメニューでプロジェクトが選択されていれば予めセットされているはずです
*   User: ジョブを起動したユーザーの指定
*   Summary: メッセージテキスト
*   Result: 成功ステータスまたは失敗ステータス

![History filter form](../figures/fig0212.png)

フィルタ項目を埋めて "Filter" ボタンを押すと、ページはフィルタリング条件にあったイベント（履歴）を表示します。

より手軽に履歴のフィルタリングを行えるよう、フィルタリング結果はメニューに保存できます。"save this filter..."リンクをクリックしてフィルタリング設定を保存します。

### イベントビュー

どの履歴にも、実行したコマンド・ディスパッチオプション・成功失敗ステータス・出力メッセージを含んだファイルへのリンクが付いています。

![Event view](../figures/fig0213.png)

ひとつでもエラーが起こったものの場合、"Node Failure Count" カラムが表示され、何台のノードが失敗したか赤味で表示されています。バーチャートは何 % が失敗だったのかを示しています。

![Event view](../figures/fig0216.png)

### RSS リンク

RSS アイコンは現在のフィルタリング設定にマッチしたイベント（履歴）の一覧表示へのリンクになります。

An RSS icon provides a link to an RSS view of the events that match
the current filtering criteria.

![RSS link](../figures/fig0214.png)

### Tips とトリック
#### フィルタリングの保存

どのページのフィルタも、現在の設定を保存する機能がついています。"save this filter..." リンクを押して名前を付けて保存します。保存されたフィルタはどれも、次回フィルタ設定を行う際にはメニューに追加されています。

### 自動補完

あなたが Bash シェルを使っている場合、Rundeck はよい自動補完スクリプトの一つになりえます。以下の行をあなたの `.bashrc` ファイルに追加してみてください：

    source $RDECK_BASE/etc/bash_completion.bash
  
`dispatch` コマンドを入力している際にタブキーを押すと、入力の続きの候補が出てきます:

    $ dispatch <tab><tab>

## まとめ

ここまでであなたは基本的な Rundeck の操作、つまり "プロジェクトのセットアップ・プロジェクトリソースモデルへのクエリの定義・アドホックコマンドの実行・ジョブの実行と保存と履歴の閲覧" ができるようになっています。

次は Rundeck の中心となる機能であるジョブについて学んでいきます。
