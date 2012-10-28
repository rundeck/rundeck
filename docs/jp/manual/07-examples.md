% 例から学ぶ Rundeck
% Alex Honor; Greg Schueler
% November 20, 2010

この章では、Rundeck を用いた様々なソリューションを表す実践例を説明します。出てくる例は、これまでの章で紹介した概念や機能を実践する際の助けとなることに焦点を当てています。抽象的な例を挙げるのではなく、オンラインアプリケーションサービスを経営する架空の組織 Acme Avils の状況に合わせた例を挙げていきます。

## Acme Anvils 

Acme Anvils はオンラインで新品・中古の金敷き台 (anvils) を売っている架空のスタートアップです。会社の中には金敷き台を売るアプリケーションの開発とサポートに関わる 2 つのチームが存在します。まだ新しい会社なので、本番環境へのアクセスはしっかり管理されていません。どちらのチームにもソースコードの変更によってミスや障害を起こす可能性があります。シニアマネージャー達がチームに新しい機能のリリースを出来る限り頻繁にさせようと夢中になっているからです。残念ながら、これは別の問題を引き起こしていました：Acme Anvil のウェブサイトは、メモリを大量に喰い、偶発的にリスタートが起こっていたのです。

リスタート処理には 2 つの方法があります: "kill" と "normal" です。"kill" でのリスタートには、アプリケーションが一切レスポンスしなくても良い時間が必要になります。"normal" でのリスタートには、十分な量のメモリを必要とします。

緊急事態または急いでいる状況下で、開発者がリスタートの実行を行うのか、システムの管理者がリスタートの実行を行うのかは全く別物です。なぜならソフトウェアを書いている開発者は、アプリケーションの観点からリスタートの必要性について理解しており、一方それらの必要性を知らされてないシステム管理社はシステムの観点からリスタートについて理解しています。これがやり方の違いを生む、顧客に対して影響を及ぼす主原因でした。

あるシステム管理者はアプリケーションをリスタートさせるよう深夜に電話がかかってくることにうんざりしていました。また開発と運用の知識の溝にイライラしていました。彼はもっと良いアプローチでやっていくために先導を取っていくことを決めました。

## Rundeck のセットアップ

管理者は本番環境のサーバーにアクセスするためのマシンを用意してそこに Rundeck をインストールすることにしました。

アプリケーションサポートに関することを管理するためにプロジェクト名 "anvils" を作りました。

管理者はシェルツール [rd-project](../manpages/man1/rd-project.html) を使ってプロジェクトを作成します。これは Rundeck GUI ([プロジェクトのセットアップ](rundeck-basics.html#rundeck+のセットアップ)を参照)からでも可能です。Rundeck サーバーにログインし、以下のコマンドを実行します:

    rd-project -p anvils -a create

このコマンドにより "anvils" プロジェクトが Rundeck 上に作られ、そこには Rundeck サーバーに当たるノード情報のみが含まれています。本番環境に置かれたノード情報の追加についてはこれから説明します。([リソースモデル](getting-started.html#リソースモデル) を参照)

本番環境には anv1 から anv5 までの 5 つのノードが存在します。アプリケーションは 3 階層あります。ウェブ、アプリケーション、データベースのコンポーネントが 5 つのノードにまたがってインストールされています。

さらに管理者はアプリケーションのコンポーネントをコントロールする SSH コマンドを実行するのに、それぞれ違ったログインユーザーを使うようルールを敷くことに決めます。ウェブコンポーネントには "www" というユーザー、アプリケーションとデータベースコンポーネントには "anvils" というユーザーを用います。

このルールをきちんと管理するために、管理者は [resource-v13(5) XML](../manpages/man5/resource-v13.html) または [resource-v13(5) YAML](../manpages/man5/resource-yaml-v13.html) を使ったプロジェクトリソースモデルを準備します。下記の定義ファイルには、5 つのノードの定義がリストアップされています -- anv1, anv2, anv3, anv4, anv5

File listing: resources.xml

    <?xml version="1.0" encoding="UTF-8"?>

    <project>
      <node name="anv1" type="Node"
         description="an anvils web server" 
         hostname="anv1.acme.com"  username="www" tags="web"/>
      <node name="anv2" type="Node" 
         description="an anvils web server" 
         hostname="anv2.acme.com"  username="www" tags="web"/>
      <node name="anv3" type="Node" 
         description="an avnils app server" 
         hostname="anv3.acme.com"  username="anvils" tags="app"/>
      <node name="anv4" type="Node" 
         description="an anvils app server" 
         hostname="anv4.acme.com"  username="anvils" tags="app"/>
      <node name="anv5" type="Node" 
         description="the anvils database server" 
         hostname="anv5.acme.com"  username="anvils" tags="db"/> 
    </project>

XML コンテンツを見てみると、全体に渡って XML タグでホスト情報が表されています。各ノードの論理名は、``name`` 属性に定義します。(例 name="anv1") ``hostname`` 属性で定義されたアドレス(例 hostname="anv1.acme.com") は ``username`` 属性にて指定されたログインユーザー(例 username="www")が SSH コマンドを実行する際に利用されます。``tags`` 属性で定義された値は、ノードの役割/機能を表しています。(例 tags="web" vs tags="app")

管理者は彼が決めたパスにファイルを保存します。Rundeck にそのファイルを認識させるために、プロジェクトの設定ファイルを編集する必要があります、`$RDECK_BASE/projects/anvils/etc/project.properties` の `project.resources.file` を編集します:

    project.resources.file = /etc/rundeck/projects/anvils/resources.xml
   
リソースファイルを置きプロジェクト設定を更新すれば、管理者としてのリソースモデルの準備とディスパッチコマンドを実行する準備が整ったことになります。

フィルタを開け、``.*`` を名前フィールドに入力し、"Filter" を押して、anvils プロジェクトのすべてのノードをリストアップします。6 つのノードが出ているはずです。

![Anvils resources](../figures/fig0601.png)

## タグ付けとコマンドディスパッチング

アプリケーションの役割を表すタグを使うと、ターゲットとなるホスト名を一切ハードコーディングせずにコマンド実行が可能になります。[dipatch](../manpages/man1/dispatch.html) コマンドにて、指定したタグによりフィルタリングされたノードセットをリストアップできます:

web ノードをリストアップするタグのキーワードを使います:

    dispatch -p anvils -I tags=web
    anv1 anv2
    
app ノードのリストアップ:

    dispatch -p anvils -I tags=app
    anv3 anv4

db ノードのリストアップ:

    dispatch -p anvils -I tags=db
    anv5

"+" (AND) 演算子を使って web と app ノードをリストアップします:

    dispatch -p anvils -I tags=web+app
    anv1 anv2 anv3 anv4

web と app ノードを除きます

    dispatch -p anvils -X tags=web+app
    anv5

ノード名にワイルドカードを使って、全てのノードをリストアップします:

    dispatch -p anvils -I '.*' 
    anv1 anv2 anv3 anv4 anv5 

以下の図はグラフィカルコンソールでのフィルタリングの使用例です。

![Anvils filtered list](../figures/fig0602.png)

タグによるフィルタリング機能によってホスト郡が抽象化され、管理者はゆるやかな分類を使ったスクリプトの処理について考えられます。新しいノードの追加も可能ですし、既に役割を与えられているホスト郡からその役割を外すことも可能です、そして、それぞれフィルタリング条件に紐づけられるため、各役割における処理に影響はありません。

この簡単な分類の仕組みは、開発者と管理者に Anvils アプリケーションのノードについて話す際の共通用語となります。

## ジョブ

ジョブは繰り返し実行する処理のライブラリをつくる便利な方法です。その性質上、保存されたジョブは処理をカプセル化する働きをします。ジョブは小さな、または大きなシェルスクリプトを呼ぶような 1 つのワークフローから、マルチステップのワークフローまで扱う事が出来ます。各ジョブを再利用可能な処理のブロックとして捉えると、より複雑な自動化を構築することができます。

起動とシャットダウンの処理の管理用として既に 2 つのスクリプトセットが存在していました。管理者は、どちらのスクリプトの方が優れているかといったことを強要することよりも、ジョブワークフローによってどのようにスクリプトがカプセル化されるかを簡単に示すために骨組みを作ることに専念しました。このシンプルなフレームワークについてのデモを行った後、管理者はジョブの定義に両方のスクリプト合わせた一番いいものを入れるにはどうしたらいいか議論することができます。

管理者は、骨組みとして実行しようとしていることと、与えられた引数をただ echo するだけのシンプルなプレースホルダースクリプトを作りました - start.sh と stop.sh - リスタートの処理を 2 つのステップで表現しています。

スクリプト:

File listing: start.sh

    #!/bin/sh
    # Usage: $0 
    echo Web started.

File listing: stop.sh

    #!/bin/sh
    # Usage: $0 [normal|kill]
    echo Web stopped with method: $1.

"method" オプションにて normal または kill のどちらの方法か指定できるようになっているため、ユーザーはスクリプトに引数を与える必要があります。

リスタート処理はジョブワークフローとして定義されるため、リスタートの処理にそのまま対応するスクリプトは存在しません。

### ジョブの構造

リスタートスクリプトのアイデアを念頭に置き、次のステップでは、リスタートの処理を含んだジョブの定義をします。最終的なゴールは、リスタートの処理をひとつ提供し、再利用できるように各ステップ毎にジョブを分けた方がいいでしょう。

管理者は以下のようなアプローチをイメージしています:

*   スタート: ウェブサービスをスタートさせるために start.sh スクリプトを実行する
*   ストップ: ウェブサービスをストップさせるために stop.sh スクリプトを実行する
*   リスタート: ストップジョブ、スタートジョブの順番で実行する

リスタートの処理が優先的であるため、大文字にして差別化しています。

後々の管理を考えて将来出てくるジョブと既存のジョブが結合できるようジョブを全ての個々のステップ毎に定義しようとすると大変複雑になってきますが、これは後からでもできることです。どのように処理を個々のジョブに分解するかは、保守性と再利用性のバランスを考えて決めます。

### ジョブのグルーピング

絶対必要というわけではないですが、ジョブのグルーピングとその命名規則を決めておくと便利です。優れた命名規則は名前を思い出しやすく、探している処理を見つけやすくしてくれます。

管理者はウェブサービスのリスタートに関連するジョブが置かれるトップレベルのグループを "/anvils/web" という名前にしました。

    anvils/
    `-- web/
        |-- Restart
        |-- start
        `-- stop

"anvils" プロジェクトを選択後、このジョブグループがあることがわかります。

![Anvils job group](../figures/fig0604.png)

## ジョブのオプション

スクリプトにてリスタートの方法を指定するために、先ほどの 3 つのジョブにて "method" というオプションを作ります。それらのパラメーターが無い場合、管理者は kill と normal の両方のストップ方法についてリスタートジョブを用意することになります。

ジョブオプションを定義するもう一つのメリットは、ジョブを実行しようとした際に選択肢のひとつとしてメニューに表示されることです。選択するとメニューから選択した値がスクリプトに渡されます。

### 許可された値

定義できるオプションは、指定されたリスト内にあるものに限ります。これは安全にスクリプトを利用できるようパラメーターの選択肢を制限してジョブを実行するセーフガードの役割を担います。

管理者はこれを利用して、"normal" または "kill" のみ選択可能になるよう "method" オプションにて制限をかけます。

以下のスクリーンショットは、"method" オプションを編集しているところです。フォームにはオプションの説明とデフォルトの値だけでなく、Allowed Values (許可された値) と Restrictions (制限) の項目が入っています。

![Option editor for method](../figures/fig0605.png)

上述のように 許可された値はカンマ区切りのリストで指定できます、また "remote URL" を使った外部リソースからも可能です。

"Enforced from Allowed values (許可された値のみという強要)" という制約を使ってオプションの選択肢を管理できます。"true" をセットしたとき、Rundeck UI は許可された値のみ選択可能なようポップメニューだけ表示されます。"false" をセットしたときは、テキストフィールドも表示されます。"Match Regular Expression" を使って入力をバリデートすることもできます。

Rundeck がどのようにメニューの選択肢を表示するかのスクリーンショットが以下になります。

![Option menu for method](../figures/fig0606.png)

### オプションデータへのスクリプトからのアクセス

オプション値は引数または参照値としてスクリプトに渡す事ができ、スクリプト内で名前を付けたトークンとして利用できます。例えば、"method" オプションの値にアクセスするにはいくつかの方法があります:

環境変数として参照する:

*   Bash: $CT\_OPTION\_METHOD

``srciptargs`` タグを使ってスクリプトやコマンドの引数として値を渡します:

*   Commandline Arguments: ${option.method}

スクリプト内である名前が作られたトークンとして値が展開され、実行前に置き換わります:

*   Script Content: @option.method@

## ジョブワークフローを作る

リスタート処理を管理するのに必要なスクリプトとオプションを理解した上で、ジョブの定義を作っていくのが最後のステップになります。

Rundeck の GUI からもドキュメントフォーマット [job-v20(5)](../manpages/man5/job-v20.html)に従った XML 形式で各ジョブを定義できます。このドキュメントフォーマットには Rundeck GUI フォームの中での選択肢に相当するタグセットについても書かれています。

以下にあるのはジョブについての XML の定義です。1 つの XML ファイルに 1 つ以上のジョブが定義できますが、どのように定義するかはあなたが決めるルール次第です。ファイルも好きなように名前を付けることが出来ます、必ずしもジョブ名やそのグループに相当する名前である必要はありません。

File listing: stop.xml

    <joblist>	
        <job> 
           <name>stop</name>  
           <description>the web stop procedure</description>  
           <loglevel>INFO</loglevel>  
           <group>anvils/web</group>  
           <context> 
               <project>anvils</project>  
                 <options> 
                   <option name="method" enforcedvalues="true"
                           required="true" 
                       values="normal,kill"/> 
                   </options> 
           </context>  
           <sequence threadcount="1" keepgoing="false" strategy="node-first"> 
             <command> 
               <script><![CDATA[#!/bin/sh
    echo Web stopped with method: $1.]]></script>  
                <scriptargs>${option.method}</scriptargs> 
             </command> 
           </sequence>  
           <nodefilters excludeprecedence="true"> 
             <include> 
              <tags>web</tags> 
              </include> 
           </nodefilters>  
           <dispatch> 
             <threadcount>1</threadcount>  
             <keepgoing>false</keepgoing> 
           </dispatch> 
         </job>
    </joblist>

/anvils/web/stop というジョブを定義します、これは "web" というタグが付いたノードに対してシェルスクリプトを実行します。``scripttags`` を使ってシェルスクリプトにジョブの実行フォームから選択された値を含んだ引数 ``${option.method}`` をひとつ渡します。

File listing: start.xml

    <joblist>	
       <job> 
         <name>start</name>  
         <description>the web start procedure</description>  
         <loglevel>INFO</loglevel>  
         <group>anvils/web</group>  
        <context> 
          <project>anvils</project>  
        </context>  
        <sequence threadcount="1" keepgoing="false" strategy="node-first"> 
         <command> 
          <script><![CDATA[#!/bin/sh
     echo Web started.]]></script>
         </command> 
      </sequence>  
        <nodefilters excludeprecedence="true"> 
          <include> 
            <tags>web</tags> 
          </include> 
       </nodefilters>  
       <dispatch> 
         <threadcount>1</threadcount>  
         <keepgoing>false</keepgoing> 
       </dispatch> 
      </job>
    </joblist>

/anvils/web/start というジョブを定義します、これは "web" というタグが付いたノードに対してシェルスクリプトを実行します。

File listing: restart.xml

    <joblist>	
       <job> 
         <name>Restart</name>  
         <description>restart the web server</description>  
         <loglevel>INFO</loglevel>  
         <group>anvils/web</group>  
         <context> 
           <project>anvils</project>  
             <options> 
               <option name="method" enforcedvalues="true" required="false" 
	          values="normal,kill" /> 
            </options> 
         </context>  
         <sequence threadcount="1" keepgoing="false" strategy="node-first"> 
          <command> 
            <jobref name="stop" group="anvils/web">
              <arg line="-method ${option.method}"/> 
            </jobref> 
          </command>  
          <command> 
            <jobref name="start" group="anvils/web">
            </jobref> 
          </command> 
        </sequence>
       </job>	
    </joblist>

/anvils/web/Restart というジョブを定義します、これは ``jobref`` タグを用いて連続するジョブ郡を実行します

今回は `<nodefilters>` または `<dispatch>` の項目を定義していないことに注意してください、今回はこの連続実行をサーバー上で**一度だけ**しか行わないからです。ジョブリファレンスはそれぞれ 1 回呼ばれ、"start" と "stop" ジョブはそれぞれ定義されているノードに対してディスパッチされます。

XML の定義ファイルを Rundeck サーバーに保存すると、[rd-jobs](../manpages/man1/rd-jobs.html) コマンドを用いてそれらを読み込む事ができます。

``rd-jobs`` を実行して各ジョブの定義ファイルからコマンドを読み込みます:

    rd-jobs load -f start.xml
    rd-jobs load -f stop.xml
    rd-jobs load -f restart.xml

``rd-jobs list`` コマンドは Rundeck に定義済みのジョブのリストを出力するよう問い合わせます:

    rd-jobs list -p anvils
    Found 3 jobs:
	- Restart - 'the web restart procedure'
	- start - 'the web start procedure'
	- stop - 'the web stop procedure'

もちろん、これらのジョブは Rundeck のグラフィカルコンソールの Jobs ページからも見る事ができます。ジョブ名 "Restart" の上にホバリングオーバーしてジョブの詳細が表示されます。

![Anvils restart jobs](../figures/fig0607.png)

ここまでで、各ジョブ(ストップとスタート)を呼ぶワークフローとして "Restart" ジョブの組み立て方を理解したことでしょう。"Restart" ジョブは ``-method`` オプションの値をより低レベルのストップジョブに渡します。

## ジョブを実行する

ジョブは Rundeck のグラフィカルコンソールの "Jobs" ページから実行できます。ここから、3 つの保存済みジョブが表示される "Anvils/web" ジョブグループまでナビゲートします。

"Run" ボタンをクリックしてジョブをリスタートさせます。するとオプションを選択するページが表示されます。"method" オプションについてのメニューが 2 つの選択肢 "normal" と "kill" を表示しています。他の選択肢や自由に入力できるテキストフィールドは利用できません、"method" オプションが選択肢を限定するように定義されているからです。

![Restart run page](../figures/fig0608.png)

シェルツール [run](../manpages/man1/run.html) を使ってコマンドラインからもジョブを起動することができます。"-j" パラメーターを使ってジョブグループとジョブ名を指定してください。ジョブがサポートするあらゆるオプションは "--" パラメーターの後に指定します。("-p" パラメーターはプロジェクトの指定に用います、しかし利用可能なプロジェクトが 1 つしか無い場合は指定しなくてもよいです。)

"normal" メソッドを指定してリスタートジョブを実行します:

    run -j "anvils/web/Restart" -p anvils -- -method normal

"kill" メソッドを指定してリスタートジョブを実行します:

    run -j "anvils/web/Restart" -p anvils -- -method kill

## ジョブのアクセスコントロール

ジョブの編集または実行へのアクセス管理は、ACL ポリシードキュメントフォーマット（[aclpolicy-v10(5)](../manpages/man5/aclpolicy-v10.html)） を使って定義できます。このファイルは、どのユーザーグループがどんなアクションが行えるのかを記述したポリシー要素で構成されています。この詳細は管理者向けガイドにおける[権限の許可(Authorization)](../administration/authorization.html) の章に載っています。

管理者は 2 つのアクセスレベルを定義した ACL ポリシーを使いたいと思っています。最初のレベルはジョブをただ実行することだけが許されます。2 つ目のレベルは、管理者作業とジョブの定義を編集できます。

ポリシー機能は、グループまたは利用パターンごとのアクセス管理情報をひとつまたは複数のポリシーファイルにまとめることができます。普通にインストールした Rundeck には 2 つのユーザーグループが定義されています: "admin" と "user" です。そして "admin" グループについてポリシーが作られています。

Acme 社の管理者は、"user" グループに入っている人たちが "anvils" と "anvlis/web" ジョブグループの実行のみ許可されるようなポリシーを 1 つ作る事に決めました。私たちは Rundeck を普通どおりインストールしたときに入っている "user" ログイングループを利用する事が出来ます。

"user" グループに対してのポリシーファイルを作る方法:

    cp $RDECK_BASE/etc/admin.aclpolicy $RDECK_BASE/etc/user.aclpolicy

以下の例にあるように `<command>` と `<group>` 要素を編集します。workflow\_read と workflow\_run アクションのみ許可されていることがわかります。

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~{.xml}
$ cat $RDECK_BASE/etc/user.aclpolicy
<policies>
  <policy description="User group access policy.">
    <context project="*">
      <command group="anvils" job="*" actions="workflow_read,workflow_run"/>
      <command group="anvils/web" job="*" actions="workflow_read,workflow_run"/>
    </context>
    <by>
      <group name="user"/>
    </by>
  </policy>
</policies>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

新しいポリシーファイルを読み込ませるために Rundeck をリスタートします。（[管理者向けガイド - 起動とシャットダウン](../administration/startup-and-shutdown.html) を参照してください）

    rundeckd restart

Rundeck ウェブアプリケーションを起動して、"user" ユーザーとしてログインします (パスワードは多分 "user" )。起動すると Jobs ページに "anvils" グループだけ表示されています。"user" ユーザーは "/anvils" グループ以外のジョブへのアクセス権限はありません。

"admin" としてログインすると表示される "New Job" ボタンがないことに気付きます。ジョブの作成権限は "user" には付与されていません。また、リストアップされたジョブについてのボタンバーにジョブの編集や削除アイコンが付いていないことにも気付きます。`user.aclpolicy` ファイルにて workflow\_read と workflow\_actions のみ許可されています。

## リソースモデルソースの例

[管理者向けガイド - ノードリソースソース - リソースモデルソース](../administration/node-resource-sources.html#リソースモデルソース) を参照してください。

## リソースエディタの例

[管理者向けガイド- ノードリソースソース - リソースエディタ](../administration/node-resource-sources.html#リソースエディタ) を参照してください。

## オプションモデルプロバイダーの例

[ジョブオプション - オプションモデルプロバイダ](http://rundeck.org/docs/manual/job-options.html#オプションモデルプロバイダ) を参照してください。
