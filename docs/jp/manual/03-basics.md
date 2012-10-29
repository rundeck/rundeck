% Rundeck の基本
% Alex Honor; Greg Schueler
% November 20, 2010

この章では Rundeck を使う上での基本をカバーします。まず Rundeck の GUI と CUI 両方のインターフェイスについて説明します。次にプロジェクトの準備からコマンド実行までの操作方法を説明します。そして実行制御に用いているコマンドディスパッチャについて理解を深め、最後に実行履歴の見方と利用方法を学びます。

## Rundeck のインターフェイス

Rundeck には 2 つの主要なインターフェイスがあります。

*   GUI: HTML ベースのグラフィカルなコンソール
*   CUI: Shell ツール

どちらのインターフェイスからも、リソースの閲覧・ディスパッチ(コマンドの発信)・ジョブの保存と実行が可能です。

さらに Rundeck にはサーバーのプログラムとやり取りできる WebAPI も用意されています。[Rundeck API](../api/index.html) を見てみてください。

### グラフィカルコンソール

Rundeck の使用を開始するにはまず、Rundeck サーバーの URL にアクセスします。そして Rundeck のユーザーディレクトリの設定によって定義されたアカウントでログインします。

Web インターフェイスのデフォルトのポートは 4440 です、以下の URL で試してみてください:

<http://localhost:4440>

アクセスするとログインページが表示されます。デフォルトのユーザー名とパスワードは `admin/admin` ですので、それらを入力してください。

#### ナビゲーション

Rundeck ページのヘッダーは、「Run」（実行）・「Jobs」（ジョブ）・「History」（履歴） タブページを行き来するためのグローバルナビゲーションになっています。また、ログアウト・ユーザーのプロフィール画面・オンラインヘルプへのリンクも載っています。

![Top navigation bar](../figures/fig0201.png)

Run
:   Run ページは、その場限りのコマンドを実行する際に使います。Run ページではあなたのプロジェクトのリソースモデルとして設定されたノードをフィルタリングしたものを表示しています。フィルタは条件にマッチしたノードだけリストアップするというように使う事ができます。

Jobs
:   Jobs ページでは、保存されたジョブの一覧・ジョブの作成・ジョブの実行を、権限のある人が行えます。Jobs ページでも条件にマッチしたジョブだけをリストアップすることができます。このジョブのフィルタリングはユーザープロフィールに保存することができます。そのユーザーに閲覧権限があるジョブのみが表示されます。

History
:   History ページでは、現在実行中のコマンド・過去のコマンド実行履歴を、権限のある人が閲覧できます。履歴についても、選択したパラメーターにマッチする履歴のみリストアップできます。それらのフィルタリングも保存可能です。現在のフィルタリング設定はページ右上にある RSS リンクにも設定できます。

Project menu
:   トップのナビゲーションバーの中に、プロジェクトを選択できるプルダウンメニューがあります。もし一つしかプロジェクトが存在しない場合は、デフォルトでそれが選択されています。
     
Admin
:   「admin」グループに属するアカウントまたは「admin」権限が付与されたアカウントでログインした場合は、右上のログイン名がある部分に、レンチのアイコンも表示されています。このページでは全てのグループ・ユーザーの閲覧やプロフィール情報の編集ができます。

User profile
:   ユーザー名・メールアドレス・どのグループに属しているかといった情報が表示されます。編集も可能です。

Logout
:   ログアウトのリンクを押すとログインセッションを終えてログアウトします。

Help
:   オンラインヘルプページを開きます

#### Now running

「Now runnig」(実行中) という項目が Run ページと Jobs ページの上部の実行キュー内にあります。現在実行されているアドホックコマンドやジョブが、その名前や関連情報と共にリストアップされます。また誰が実行しているか表示され、出力結果へのリンクも表示されます。

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

プロジェクトはグラフィカルコンソールまたはシェルツール「rd-project」のどちらからでもセットアップできます。

グラフィカルツールにログインした後、上部のナビゲーションバー内にプロジェクトメニューがあることに気付くでしょう。もしプロジェクトが存在しなければ、新しいプロジェクトを作るよう勧められます。

![Create project prompt](../figures/fig0203-a.png)

プロジェクト名を入力するだけでセットアップが開始できます。その他の項目に付いては後から [GUI 管理者ページ](../administration/configuration.html#gui-admin-page)にて変更できます。

プロジェクト名の入力が完了すると、Rundeck はその名前で初期化した後、「Run」ページに戻ります。

プロジェクトの作成はナビゲーションバーのプロジェクトメニューから「Create a new project...」を選択する事でいつでも行うことができます。

![Create project menu](../figures/fig0203.png)

シェルツール「rd-project」でもプロジェクトの作成ができます。

Rundeck のサーバー上で「rd-project」コマンドを実行し、プロジェクト名の指定をします。ここでは例として、「examples」という名前を付けます:

    rd-project -a create -p examples

コマンドの実行後、グラフィカルコンソールにてプロジェクトメニューに今作成したプロジェクトが追加されている事が分かります。

プロジェクトを作る際に同時に他の項目も追加することができます:

    rd-project -a create -p examples --project.ssh-keypath=/private/ssh.key

プロジェクトのセットアッププロセスが、サーバー内でプロジェクトの設定を生成し、リソースモデルを準備します。  

![Run page after new project](../figures/fig0204.png)

Rundeck サーバー自身を指す 1 つのノードがリストアップされます。このノードについては赤文字の「server」という識別子が付きます。

### リソースモデル

リソースモデルとは Rundeck がコマンドをディスパッチでき、サーバーのメタデータが紐づけられているノードのことです。どの Rundeck プロジェクトもそれぞれリソースモデルを持つ事になります。

初期状態のリソースモデルは Rundeck サーバー自身の情報だけを持っており、Rundeck サーバー自身にだけローカルコマンドが実行できます。プロジェクトごとのリソースモデルは「Run」ページで見ることができます。

シェルツールからもノードリソースのリストアップができます。特定のプロジェクトを指定したい場合は `-p <project>` オプションを付けます。

下記コマンドが「examples」というプロジェクトに登録されているサーバーのリストを列挙するディスパッチコマンドになります。`-v` オプションを付けるとリストの情報をより詳細に表示します:

    $ dispatch -p examples -v	
     strongbad:
        hostname: strongbad
        osArch: x86_64
        osFamily: unix
        osName: Mac OS X
        osVersion: 10.6.2
        tags: ''

ノードリソースは「hostname」のような標準のプロパティを持ち、プロパティは属性を通して拡張することもできます。便利なプロパティのひとつとして、「tags」プロパティがあります。ノードに付ける「tag」はテキストのラベルのことで、ある環境におけるひとつの役割として使ったり、あるグループのメンバであることを示すことに使ったり等、ノードの分類に用います。一つのノードに複数の「tag」を付けることもできます。

上記の出力では「strongbad」というノードに現在は空のタグ（''） が付いていることを示しています。

後々コマンドディスパッチを行う際にノード群をフィルタリングするのにタグを用いるため、管理するノードにどんなタグを付けるか考える始めるのは重要なことです。

どのプロジェクトにも [project.properties](../administration/configuration.html#project.properties) という名前の設定ファイルが存在し、`$RDECK_BASE/projects/project/etc/project.properties` というパスに置かれています。

この設定ファイルはリソースモデルデータへのアクセス、取り込みのための 2 つの基本的なプロパティを持っています。

*   `project.resources.file`: リソースモデルドキュメントを読み込む際のローカルファイルパス
*   `project.resources.url`: 外部のリソースモデルへの URL (オプション)

さらに、あるリソースモデルを他のソースから取り出してくることで、ひとつのプロジェクトで複数のリソースモデルを追加することができます。[リソースモデルソース](plugins.html#リソースモデルソース) を参照してください。

Rundeck のリソースモデルフォーマットが生成できる限り、あやゆるソースからリソースモデルデータを取ってきて Rundeck に取り込むよう設定することが可能です。([リソースモデルドキュメントフォーマット](rundeck-basics.html#リソースモデルドキュメントフォーマット)を参照して下さい) `project.resource.url` にお好みの URL リソースモデルソースをセットしてください。

ここに、先ほどの `dispatch -v` コマンドの出力結果に該当する 「examples」プロジェクトのための XML ドキュメントがあります:

    <project>
        <node name="strongbad" type="Node" 
          description="the Rundeck server host" tags="" 
          hostname="strongbad" 
          osArch="x86_64" osFamily="unix" osName="Mac OS X" osVersion="10.6.2"
          username="alexh" 
          editUrl="" remoteUrl=""/>
    </project>

XML のルートノードは project で、そこに「strongbad」に対する記述がひとつのノードとして書かれています。ノードの XML タグは数ある必須属性、オプション属性のうちのひとつになります。ノードを追加する際には project XML タグの内側に node XML タグを追加してください。

ホスト strongbad はタグの定義が何もされていません。ひとつまたは複数のタグが定義できます。区切りにはカンマ（,）を使います。（例. `tags="tag1,tag2"`）

以下の例では「homestar」というノードが必須属性とともに記述されています:

        <node name="homestar" type="Node" 
          hostname="192.168.1.02" 
          username="alexh" />

`name` と `type` の値がリソースモデルの中でノードを識別するために利用する一方、`hostname` と `username` の値は SSH コネクションに用います。ポート情報を加えて上書きする事も出来ます。(例. `hostname="somehost:2022"` ) これはデフォルトと違うポートを使って SSH を使う際に便利です。

Chef, Puppet, Nagios, Amazon EC2, RightScale または社内データベースなど、別のツール内でホストに関する情報をメンテナンスする機会があります。これらのツールのいずれかは、あなたのネットワークの置かれたノードに関する情報を握っているツールかもしれません。そこで、信頼されたツールへのインターフェイスを作ることと、そしてそれを Rundeck URL リソースモデルソースとして見せることが一番です。これは（ノード情報を管理している）tool のフォーマットから Rundeck のフォーマットへ変換するシンプルな CGI スクリプトとして実現できます。

もちろん、基本的な選択肢として XML ドキュメントとして情報をメンテナンスするというのもあります、それらは Rundeck に定期的にエクポーとされるソースリポジトリに取り込まれます。もしあなたの組織のホストインフラストラクチャが頻繁に変化するなら実用的な手段になります。

Rundeck のウェブサイトで URL リソースモデルソースをチェックしてください。 自分たちのリソースモデルを作ることに興味がある場合は[リソースモデルソース](../administration/node-resource-sources.html#リソースモデルソース)の外部のデータプロバイダーとの統合項目を参照してください。

### リソースモデルドキュメントフォーマット

Rundeck は今のところ 2 種類のリソースモデルドキュメントフォーマットが組み込まれています。

*   XML: [resource-v13(5) XML](../manpages/man5/resource-v13.html).  Format name: `resourcexml`.
*   Yaml: [resource-v13(5) YAML](../manpages/man5/resource-yaml-v13.html). Format name: `resourceyaml`.

[リソースフォーマットプラグイン](plugins.html#リソースモデルフォーマットプラグイン)により他のフォーマットを使うことも可能です。

## プラガブルリソースモデルソース
どのプロジェクトもリソースモデル情報のソースを複数持つことができます。そしてリソースモデルへ新しいソースとして、プラグインを使用または作ることができます。

Each project can have multiple sources for Resource model information, and
you can use or write plugins to enable new sources for entries in the Resource model.

You can configure the sources via the GUI from the Admin page, see
[GUI Admin Page](../administration/configuration.html#gui-admin-page), or by modifying the project configuration file,
see [Resource Model Sources](plugins.html#resource-model-sources).

## Command Execution

Rundeck supports two modes of execution: *ad-hoc commands* and *Job*.

An *ad-hoc command* is any system command or shell script executed
via the command dispatcher. Ad hoc commands can be executed via a
command line utility named `dispatch` or run from
the graphical console.

A *Job* specifies a sequence of one or more command invocations that
can be run once (i.e, is temporary) or named and stored for later use.
Stored jobs can be started via the shell tool, `run`, and
their progress checked with `rd-queue`.

### Dispatcher options

Dispatcher execution can be controlled by various types of options.

Execution control

:    Concurrency
     is controlled by setting the "threadcount". Execution can continue even if
     some node fails if the "keepgoing" option is set to true.

Node Filters

:    Filtering options specify include and exclude filters to
     determine which nodes from the project resource model to distribute
     commands to.

Filter Keywords

:    Keywords are used within they include and exclude patterns. The
     "tags" keyword additionally can use a boolean operator to combine
     logical ORs and ANDs.

Filter combinations

:    All keywords can be combined by specifying the include and
     exclude options multiple times on the command line.
  
One can experiment querying the resource model in the graphical
console or with the `dispatch` tool.

#### Filtering nodes graphically  

A project's Node resources are displayed in the Run page. Use the
project menu in the navigation bar to change to the desired project.
After choosing a project, the server node will be filtered by default.

Nodes can be filtered using include and exclude patterns by using
the Filter form. The form can be opened by pressing the "Filter" button.
Press the triangular disclosure icon to display the form.

![Resource filter link](../figures/fig0205.png)

When the form opens, you will see it divided into an Include section
where simple include expressions can be set, as well as, an "Extended
Filters..." link where exclude expressions can be made. 

![Resource filter form](../figures/fig0206.png)

After filling out the filter form, press "Filter" to generate a new
listing. Pressing "Clear" resets the form.

The Include and Exclude filters allow for filtering nodes based on the
following keywords: Name, Tags, Hostname, OS Name, OS Family, OS
Architecture, OS Version and Type.

Regular expressions can be used for any of the keywords. The ``.*`` pattern
will match any text.

If more than 20 nodes match the filter, the UI will page the results.
  
#### Filtering nodes in the shell

`dispatch` uses the commandline options -I (include) and
-X (exclude) to specify which nodes to include and
exclude from the base set of nodes. You can specify a single value, a
list of values, or a regular expression as the argument to these
options.

*Examples*

List nodes  with OS name, Linux:

    dispatch -p examples -I os-name=Linux

List Linux nodes but exclude ones with names prefixed "web.":

    dispatch -p examples -I os-name=Linux -X "web.*"

List nodes that are tagged both "web" and "prod" :

    dispatch -p examples -I tags=web+prod

Here's an example that will execute the `apachectl restart`
command in 10 threads across all nodes tagged "web" and keepgoing in
case an error occurs :

    dispatch -p examples -I tags=web -K -C 10 -- sudo apachectl restart 

Consult the [rd-options(1)](../manpages/man1/rd-options.html) manual page for the complete reference on
available dispatcher options.
  
### Ad-hoc commands 

Typically, an ad-hoc command is a shell script or system executable
that you run at an interactive terminal. Ad-hoc commands can be
executed via the `dispatch` shell command or a graphical
shell.

#### Shell tool command execution

Use `dispatch` to execute individual commands or shell script files.

Here `dispatch` is used to run the Unix `uptime` command to
print system status:

    $ dispatch -I os-family=unix -- uptime
    Succeeded queueing Workflow execution: Workflow:(threadcount:1){ [command( exec: uptime)] }
    Queued job ID: 7 <http://strongbad:4440/execution/follow/7>

The ``uptime`` command is queued and executed. The output can be followed by
going to the URL returned in the output (eg, http://strongbad:4440/execution/follow/7). 

Sometimes it is desirable to execute the command
directly, and not queue it. Use the ``--noqueue`` option to execute
and follow the output from the console.

    $ dispatch -I os-family=unix  --noqueue -- uptime
    [ctier@centos54 dispatch][INFO]  10:34:54 up 46 min,  2 users,  load average: 0.00, 0.00, 0.00
    [alexh@strongbad dispatch][INFO] 10:34  up 2 days, 18:51, 2 users, load averages: 0.55 0.80 0.75
    [examples@ubuntu dispatch][INFO]  10:35:01 up 2 days, 18:40,  2 users,  load average: 0.00, 0.01, 0.00

**Note**: The "--noqueue" flag is useful for testing and debugging execution
but undermines visibility since execution is not managed through the central execution
queue.

Notice, the `dispatch` command prepends the message output
with a header that helps understand from where the output originates. The header
format includes the login and node where the `dispatch` execution
occurred.

Execute the Unix `whomi` command to see what user ID is
used by that Node to run dispatched commands:

    $ dispatch -I os-family=unix --noqueue -- whoami
    [ctier@centos54 dispatch][INFO] ctier
    [alexh@strongbad dispatch][INFO] alexh
    [examples@ubuntu dispatch][INFO] examples

You can see that the resource model defines each Node to use a
different login to execute `dispatch` commands.  That
feature can be handy when Nodes serve different roles and therefore,
use different logins to manage processes. See the
`username` attribute in [resource-v13(5) XML](../manpages/man5/resource-v13.html) or [resource-v13(5) YAML](../manpages/man5/resource-yaml-v13.html) manual page.

The `dispatch` command can also execute shell
scripts. Here's a trivial script that generates a bit of system info:

    #!/bin/sh
    echo "info script"
    echo uptime=`uptime`
    echo whoami=`whoami`
    echo uname=`uname -a`

Use the -s option to specify the "info.sh" script file:

    $ dispatch -I os-family=unix -s info.sh
    
The `dispatch` command copies the "info.sh" script located
on the server to each "unix" Node and then executes it.

#### Graphical command shell execution

The Rundeck graphical console also provides the ability to execute
ad-hoc commands to a set of filtered Node resources.
The command prompt can accept any ad-hoc command string you might run
via an SSH command or via the `dispatch` shell tool.

But before running any commands, you need to select the project
containing the Nodes you wish to dispatch. Use the project
menu to select the desired project name. After the project has been
selected you will see a long horizontal textfield labeled
"Command". This is the Rundeck ad hoc command prompt.

![Ad hoc command prompt](../figures/fig0207.png)

To use the command prompt, type the desired ad-hoc command string into
the textfield and press the "Run" button. The command will be
dispatched to all the Node resources currently listed below the
command prompt tool bar. The command prompt also becomes disabled until
the execution completes. Output from the command execution will be shown
below (see [output](rundeck-basics.html#following-execution-output)).

![Ad hoc execution output](../figures/fig0208.png)

You will also notice the ad hoc execution listed in the "Now running" 
part of the page, located above the command prompt.
All running executions are listed there. Each running execution
is listed, showing the start time, the user running it, and a link
to follow execution output on a separate page.

![Now running ad hoc command](../figures/fig0207-b.png)

At the bottom of the page, you will see a "History" section containing
all executions in the selected project for the last 24 hours. After the execution
completes, a new event will be added to the history. A yellow highlight
indicates when the command leaves the Now running section and enters
the history table.

![Run history](../figures/fig0207-c.png)

History is organized in summary form using a table layout. The "Summary" column
shows the command or script executed. The "Node Failure Count" contains
the number of nodes where an error in execution occurred. If no errors occurred,
"ok" will be displayed. The "User" and "Time" columns show the user that executed
the command and when.

##### Following execution output

Ad hoc command execution output is displayed below the command prompt.

This page section provides several views to read the output using different formats.

Tail Output

:   Displays output messages from the command execution as if you were
    running the Unix `tail -f` command on the output log file. 
    By default, only the last 20 lines of output is displayed but this
    can be expanded or reduced by pressing the "-" or "+" buttons. You
    can also type in an exact number into the textfield.
    ![Ad hoc execution output](../figures/fig0208.png)

Annotated

:   The annotated mode displays the output messages in the order they
    are received but labels the each line with the Node from which the
    message originated. Through its additional controls each Node
    context can be expanded to show the output it produced, or
    completely collapsed to hide the textual detail.    
    ![Annotated output](../figures/fig0209.png)

Compact

:   Output messages are sorted into Node specific sections and are not
    interlaced. By default, the messages are collapsed but can be
    revealed by pressing the disclosure icon to the right. 
    ![Node output](../figures/fig0210.png)

###### Separate execution follow page

Sometimes it is useful to have a page where just the execution output
is displayed separately. One purpose is to share a link to others 
interested in following the output messages. Click the "output >>"
link in the "Now running" section to go to the execution follow page.

Also, notice the URL in the location bar of your browser. This URL can
be shared to others interested in the progress of execution. The URL
contains the execution ID (EID) and has a form like:

     http://rundeckserver/execution/follow/{EID}

After execution completes, the command will have a status: 

* Successful: No errors occurred during execution of the command
  across the filtered Node set
* Failed: One or more errors occurred. A list of Nodes that incurred
  an error is displayed. The page will also contain a link "Retry
  Failed Nodes..." in case you would like to retry the command.

You can download the entire output as a text file from this
page. Press the "Download" link to retrieve the file to your desk top.

### Controlling command execution

Parallel execution is managed using thread count via "-C" option. The
"-C" option specifies the number of execution threads. Here's an
example that runs the uptime command across the Linux hosts with two
threads:

    dispatch -I os-name=Linux -C 2 -- uptime

The keepgoing and retry flags control when to exit incase an error
occurs. Use "-K/-R" flags. Here's an example script that checks if the
host has port 4440 in the listening state. If it does not, it will
exit with code 1.

    #!/bin/sh
    netstat -an | grep 4440 | grep -q LISTEN
    if [ "$?" != 0 ]; then
    echo "not listening on 4440"
    exit 1;
    fi
    echo  listening port=4440, host=`hostname`;

Commands or scripts that exit with a non-zero exit code will cause the
dispatch to fail unless the keepgoing flag is set.

    $ dispatch -I os-family=unix -s /tmp/listening.sh --noqueue
    [alexh@strongbad dispatch][INFO] Connecting to centos54:22
    [alexh@strongbad dispatch][INFO] done.
    [ctier@centos54 dispatch][INFO] not listening on 4440
    error: Remote command failed with exit status 1

The script failed on centos54 and caused dispatch to error out immediately.

Running the command again, but this time with the "-K" keepgoing flag
will cause dispatch to continue and print on which nodes the script
failed:

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
	
### Queuing commands to Rundeck

By default, commands or scripts executed on the command line by `dispatch` are
queued as temporary jobs in Rundeck. The `dispatch` command
is equivalent to a "Run and Forget" action in the graphical console.

The script below is a long running check that will conduct a check periodically
waiting a set time between each pass. The script can be run with or without
arguments as the parameters are defaulted inside the script:

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

Running `dispatch` causes the execution to queue in
Rundeck and controlled as  temporary Job. The `-I centos54` limits
execution to just the "centos54" node:

    $ dispatch -I centos54 -s ~/bin/checkagain.sh 
    Succeeded queueing workflow: Workflow:(threadcount:1){ [command( scriptfile: /Users/alexh/bin/checkagain.sh)] }
    Queued job ID: 5 <http://strongbad:4440/execution/follow/4>

To pass arguments to the script pass them after the "\--" (double
dash):

    $ iters=5 secs=60 port=4440
    $ dispatch -I centos54 -s ~/bin/checkagain.sh -- $iters $secs $ports


### Tracking execution

Queued ad-hoc command and temporary or saved Job executions can be
tracked from the "Run" page in the "[Now Running](rundeck-basics.html#now-running)" area at the top of
the page.

Execution can also be tracked using the [rd-queue](../manpages/man1/rd-queue.html) shell tool.

    $ rd-queue -p project
    Queue: 1 items
    [5] workflow: Workflow:(threadcount:1){[command( scriptfile: /Users/alexh/bin/checkagain.sh)]} <http://strongbad:4440/execution/follow/5>

Each job in the execution queue has an execution ID. The example above
shows one item with the ID, 5.

Running jobs can also be killed via `rd-queue kill`. 
Specify execution ID using the "-e" option:

    $ rd-queue kill -e 5
    rd-queue kill: success. [5] Job status: killed

### Plugins

Rundeck supports a plugin model for the execution service, which allows you to
fully customize the way that a particular Node, Project or your entire Rundeck installation executes commands and scripts remotely (or locally).

By default Rundeck uses an internal plugin to perform execution via SSH for remote nodes, 
and local execution on the Rundeck server itself.

Plugins can be installed by copying them to the `libext` directory of your Rundeck
installation.

Plugins are used to add new "providers" for particular "services".  The services used for  command execution on nodes are the "node-executor" and "file-copier" services.

To use a particular plugin, it must be set as the provider for a service by configuring the `framework.properties` file, the `project.properties` file, or by adding attributes to Nodes in your project's resources definitions.  For more about configuring the providers, see [Using Providers](plugins.html#using-providers).

The internal SSH command execution plugin is described below, and more information about plugins can be found in the [Rundeck Plugins](#rundeck-plugins) chapter.

#### SSH Plugin

Rundeck by default uses SSH to execute commands on remote nodes, SCP to copy scripts to remote nodes, and locally executes commands and scripts for the local (server) node.

The SSH plugin expects each node definition to have the following properties in order to create the SSH connection:

* `hostname`: the hostname of the remote node.  It can be in the format "hostname:port" to indicate that a non-default port should be used. The default port is 22.
* `username`: the username to connect to the remote node.

When a Script is executed on a remote node, it is copied over via SCP first, and then executed.  In addition to the SSH connection properties above, these node attributes
can be configured for SCP:

* `file-copy-destination-dir`: The directory on the remote node to copy the script file to before executing it. The default value is `C:/WINDOWS/TEMP/` on Windows nodes, and `/tmp` for other nodes.
* `osFamily`: specify "windows" for windows nodes.

In addition, for both SSH and SCP, you must either configure a public/private keypair for the remote node or configure the node for SSH Password authentication.

* See [Administration - SSH](../administration/ssh.html) for more information on setting up your SSH server
* See [SSH Provider](plugins.html#ssh-provider) for more information on the configuration of Nodes for SSH

#### Included Plugins

Two plugin files are included with the default Rundeck installation for your use in testing or development. (See [Pre-Installed Plugins](plugins.html#pre-installed-plugins))

* [Stub plugin](plugins.html#stub-plugin): simply prints the command or script instead of running it.
* [Script plugin](plugins.html#script-plugin): executes an external script file to perform the command, useful for developing your own plugin with the [Script Plugin Development](../developer/plugin-development.html#script-plugin-development) model.

## History

History for queued ad-hoc commands, as well as, temporary and
saved Job executions  is stored by the Rundeck server. History data
can be filtered and viewed inside the "History" page.

![History page](../figures/fig0211.png)

### Filtering event history

By default, the History page will list history for the last day's
executions. The page contains a filter control that can be used to
expand or limit the executions.

The filter form contains a number of fields to limit search:

* Within: Time range. Choices include 1 day, 1 week, 1 month or other
  (given a start after/before to ended after/before).
* Name: Job title name.
* Project: Project name. This may be set if the project menu was used.
* User: User initiating action.
* Summary: Message text.
* Result: Success or failure status.

![History filter form](../figures/fig0212.png)

After filling the form pressing the "Filter" button, the page will
display events matching the search.

Filters can be saved to a menu that makes repeating searches more
convenient. Click the "save this filter..." link to save the filter
configuration.

### Event view

History for each execution contains the command(s) executed,
dispatcher options, success status and a link to a file containing all
the output messages.

![Event view](../figures/fig0213.png)

If any errors occurred, the "Node Failure Count" column will show
the number of nodes in red text. A bar chart indicates the percent
failed.

![Event view](../figures/fig0216.png)

### RSS link

An RSS icon provides a link to an RSS view of the events that match
the current filtering criteria.

![RSS link](../figures/fig0214.png)

## Tips and Tricks 

### Saving filters

Each of the filter controls provides the means to save the current
filter configuration. Press the "save this filter..." link to give it
a name. Each saved filter is added to a menu you can access the next
time you want that filter configuration.

### Auto-Completion

If you use the Bash shell, Rundeck comes with a nice auto-completion
script you can enable. Add this to your `.bashrc` file:

    source $RDECK_BASE/etc/bash_completion.bash
  
Press the Tab key when you're writing a dispatch command, and it should
return a set of suggestions for you to pick from:

    $ dispatch <tab><tab>

## Summary

At this point, you can do basic Rundeck operations - setup a project,
define and query the project resource model, execute ad-hoc
commands, run and save Jobs and view history.

Next, we'll cover one of Rundeck's core features: Jobs.
