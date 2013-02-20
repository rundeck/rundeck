% ジョブオプション
% Alex Honor; Greg Schueler
% November 20, 2010

様々なコマンドやスクリプトをジョブ化できます。
しかし全てのユースケースに対応するジョブを作ろうとすると、スクリプトの呼び出し方が少し違う程度のジョブを大量に作ることになってしまうでしょう。
それらの違いとは、往々にして環境やアプリケーションバージョンに関連します。
そのほかの部分については人が必要な情報を与えてジョブを実行しているにすぎません。

スクリプトやコマンドをデータドリブンにしましょう。
そうすればより一般化でき、他のコンテキストでも再利用できます。
同じプロセスの変数をメンテナンスするより、ジョブが外部データからのオプションモデルで駆動するようにすることで、よりよい抽象化とカプセル化を期待できます。

Rundeck のジョブは、1 つ以上の名前付き *オプション* を定義させるために入力プロンプトをユーザへ出すよう設定できます。
名前付きパラメータと呼ばれる *オプション* モデルは、必須もしくはオプショナルにでき、ジョブが実行されるときにユーザに提示される選択肢の範囲が含まれています。

ユーザは、値を入力するか選択肢メニューから選んでジョブへ受け渡します。
バリデーションパターンは、入力をオプションの要件通りにコンパイルすることを保証します。

オプション選択肢は固定か、動的ソースからモデリングされます。
固定選択肢は、ジョブ定義の中でコンマ区切りでモデリングされます。
オプション値を動的にしなければならない場合、ジョブがオプションデータを外部ソースから拾うために有効な URL を定義しなけれななりません。
ジョブが URL から外部ソースにアクセスできるようになると、Rundeck を他のツールと統合して、そのデータをジョブワークフローに組み込むことができるようになります。

## ユーザへのプロンプト提示

ジョブオプションを定義することによる明確な影響は、ジョブ実行時のアピアランスです。
ユーザに、入力と選択が求められる "Choose Execution Options...（オプションを選択して下さい）" ページが提示されます。

コマンドラインユーザがシェルからジョブを実行する場合は、`run` シェルツールを通じて引数でオプションを明記します。

オプションをどのようにジョブへのユーザインタフェースの一部とするか考えることに時間をかける価値があります。
そうすることで、手続きの汎用化を次のレベルへとすすめる、いくつかのアイデアを得られます。

* ネーミングと説明に関する規約: ユーザがオプション名を呼んだだけで役割をイメージでき、説明を読めばその目的を判断できること
* 必須オプション: ユーザが入力しないと失敗するものについては必須オプションとすること
* 入力値の制約とバリデーション: オプションの値を絞り込む必要がある場合、その制御のためにセーフガードを作れることを頭に入れておきましょう

## 入力タイプ

オプションの入力タイプは GUI での表示方式と同じで、それらがジョブ実行時に利用されます。

Option Input Types define how the option is presented in the GUI, and how it is used when the Job executes.

入力タイプ:

* "Plain" - 通常オプション。入力文字列が表示される
* "Secure" - セキュアオプション。入力は隠蔽され、DB にも保存されません。
* "Secure Remote Authentication" - リモート認証でのみ利用されるセキュアなオプションで、スクリプトやコマンド内では参照されません

## セキュアオプション

平文やドロップダウンメニュー以外のパスワードプロンプト用に、オプションをセキュアとしてマークできます。
セキュアオプションの値は他のオプション値のように実行と一緒に保存されるということはありません。

セキュアオプションには二つのタイプがあります:

* Secure - 入力値はスクリプトやコマンド内へ展開されます
* Secure Remote Authentication - 入力値はスクリプトやコマンド内に展開*されず*、ノードの認証と実行のためだけに Node Executor に利用されます

Secure オプションは複数の値を選択する入力はサポートしません。

また、Node Executors への入力にも使えません。
そのような用途には Secure Remote AUthentication オプションを利用して下さい。

**重要**

"Secure" オプション値はジョブ実行時に Rundeck データべースへは保存されません。
しかし、その値はスクリプトやコマンド内へ展開されます。
このセキュリティ的な影響について承知しておいてください。
Secure オプションはスクリプトとコマンド内では他のオプション値と同じように利用できます。

* 平文引数として利用する `${option.name}`
    * コマンドへの引数としてオプション値を使うとシステムプロセステーブル内で平文の値として展開されます
* リモート及びローカルスクリプト実行時の環境変数として使う `$RD_OPTION_NAME` 
    * ローカルとリモートスクリプトはこの値を環境変数として利用することが可能です
* リモートスクリプト内で展開される平文トークンとして使う `@option.name@` 
    * トークン展開を含むインラインスクリプトのワークフローステップはテンポラリファイルにいったん展開されます。そして展開されたファイル内には平文のオプション値があります。

注：ジョブリファレンスへの引数とする場合、他のセキュアオプションの値としてのみ通すことができます。[ジョブリファレンスでのセキュアオプションの利用](#ジョブリファレンスでのセキュアオプションの利用) を参照して下さい。

### セキュアリモート認証オプション

ノード実行するための組み込みの SSH プロパイダは SSH, Sudo 認証メカニズムのため、パスワードを利用します。
パスワードはジョブ内に定義されたセキュアリモート認証オプションで供給することができます。

セキュアリモート認証オプションは平文・セキュアオプションと比較していくつかの制約を持ちます。

通常のスクリプトとコマンドオプション値の展開にユーザが入力した値を使うことはできません。
言い換えると、このオプションはリモート認証のためにしか利用できません。

### ジョブリファレンスでのセキュアオプションの利用

[ジョブ参照ステップをワークフロー内で定義する](job-workflows.html#ジョブ参照ステップ)と、それに引き渡す引数を指定することができます。
セキュアオプションとセキュアリモート認証オプションの値を最上位層のジョブからジョブ参照へ引き渡すことができます。
しかし、*あるオプション値を他の違うタイプのオプションの値として引き渡すことは不可能です。*
そのため、親ジョブはオプションタイプが子ジョブと同じならジョブ参照へオプション値を引き渡すことができます。

この制約はこれらのオプションのセキュリティデザインを保つためのものです:

1. セキュアオプションは Rundeck 実行データベースに保存してはならない。そのため、平文オプション値を利用してはならない。
2. セキュアリモート認証オプションはスクリプトやコマンド内で利用してはならない。そのため、セキュアや平文オプション値を利用してはならない

例を挙げます。Job A, Job B の2つのジョブがそれぞれ以下の定義で存在するとしましょう:

* Job A
    * オプション "plain1" - 平文
    * オプション "secure1" - セキュア
    * オプション "auth1" - セキュアリモート認証
* Job B
    * オプション "plain2" - 平文
    * オプション "secure2" - セキュア
    * オプション "auth2" - セキュアリモート認証

もし Job A が Job B へのジョブ参照を定義しているとすると、可能となるマッピングは以下の通りです:

* plain1 -> plain2
* secure1 -> secure2
* auth1 -> auth2

よって、ジョブ参照のための引数はこのようになるはずです。

    -plain2 ${option.plain1} -secure2 ${option.secure1} -auth2 ${option.auth1}

注: もしルールをまもらずに引数を定義すると、セキュアとセキュアリモート認証オプションはジョブ参照が呼ばれたときにセットされません。平文オプションはコマンドもしくはスクリプトの引数として振る舞い、残りは解釈不能のプロパティリファレンスとなります。

## オプションエディタ

オプションは保存されたジョブに作成できます。ジョブ編集ページには存在するオプションのサマリを表示するエリアがあり、新規に追加するか、既存の物を編集するためのリンクがあります。

![Add option link](../figures/fig0501.png)

オプションサマリはそれぞれのオプションと定義があればそのデフォルト値を表示します。

"edit" リンクを押すとオプションエディタが開かれます。

![Option editor](../figures/fig0503.png)

オプションエディタはそれぞれのオプションについて、拡大されたサマリを表示します。
それぞれのオプションは利用法のサマリ、説明、値リスト、制約と共にリストアップされます。
"Add an option" リンクを押すと、新しいパラメータを定義するためのフォームがオープンします。
"Close" リンクを押すとエディタはたたまれてサマリビューに戻ります。

オプションエディタのいずれかの行にマウスオーバすると、ハイライトされたオプションの削除や編集用リンクが表示されます。
削除アイコンを押すと、本当にこのオプションをジョブから削除しても良いか確認するプロンプトが表示されます。
"edit" リンクをクリックするとそのオプションの定義を変更するためのフォームが開きます。

オプションはジョブ定義の一部として定義され、Rundeck サーバにロードされます。
ジョブ定義の具体的な中身について興味がある場合は [job-v20(5)(XML)](http://rundeck.org/docs/manpages/man5/job-v20.html), [job-yaml-v12(5)(YAML)](http://rundeck.org/docs/manpages/man5/job-yaml-v12.html), [rd-jobs(1)](http://rundeck.org/docs/manpages/man1/rd-jobs.html) マニュアルページを参考にして下さい。

## オプションを定義する

新しいオプションは "Add an option" リンクをクリックすれば定義でき、定義されたものについては "edit" リンクを押せば変更できます。

![Option edit form](../figures/fig0502.png)

オプション定義フォームはいくつかのエリアに分けられています:

Identification

:    オプション名と説明を入れて下さい。名前は他のジョブが受け入れ可能な引数の一部となり、説明は実行中ジョブのヘルプテキストとなります。
     デフォルト値はオプションが表示されたときに GUI 内で既にセレクト状態になります。

Input Type

:   "Plain"（平文）・"Secure"（セキュア）・"Secure Remote Authentication"（セキュアリモート）認証から選んで下さい。"Plain" 以外を選ぶとマルチ選択オプションは利用不可になります。

Allowed values

:    選択可能な入力値のモデルです。値のリストかオプションデータを提供するサーバへの URL を含めることができます。
     値にはコンマ区切りのリストを指定することができますが、[以下に記載するように](job-options.html#リモートオプション値) "remote URL" を使った外部ソースからのリクエストも可能です。

Restrictions

:    入力を受け入れたり提示するための基準を定義します。オプションの選択肢は "Enforced drom values" 制約を利用してコントロールできます。
     もし "true" ならば、Rundeck はポップアップメニューのみを出します。"false" であれば、テキストフィールドが提示されます。
     正規表現を "Match Regular Expression" フィールドに入れるとジョブ実行時にそれが評価されます。

Requirement

:    そのオプションが与えられている場合のみ実行を許可することを示します。"No" を選択すると、そのオプションは必須となりません。"Yes" を選ぶと要求されます。
    
     デフォルト値がオプションにセットされる場合、"Yes" が選択されると、コマンドラインか API からの実行時に引数が無ければデフォルト値が自動でオプションにセットされるようになります。

Multi-valued

:    ユーザの入力が複数の値で構成できるかどうかを定義します。"No" を選ぶと、1つの値しか入力できません。"Yes" を選ぶと許可されている値の選択と、ユーザによる入力の組み合わせを利用できるようになります。ジョブ実行時に複数の入力値を区切るためにデリミタ文字列を利用します。

オプション定義ができたら、"Save" ボタンをおしてジョブ定義にそれを追加してください。
"Cancel" ボタンを押すと変更を捨ててフォームがクローズします。

## リモートオプション値

オプション値のモデルは外部リソースから検索することが出来ます。
オプションで `valuesUrl` を定義すると、その URL から入力を許可するモデルが取得されます。

これは、Rundeck を他のシステムに依存するプロセスを調整するために利用したいときに便利です。たとえば:

* Hudson などの CI サーバやビルドで生成されるパッケージやバイナリのデプロイ
    * Hudson の最近のビルド成果リストをオプションデータとして読み込むことができます。その結果、ユーザはデプロイ対象の適切なパッケージ名をリストから選ぶことができます。
* CMDB に定義されている有効な環境セットを選ぶ
* ジョブへの入力値に、違うシステムによって生成された値セットのいくつかを選択しなければならない時

参照：[Chapter 9 - オプションモデルプロバイダ](job-options.html#オプションモデルプロバイダ).

See [Chapter 9 - Option Model Provider](job-options.html#option-model-provider).

## Script usage

オプション値は引数としてスクリプトに通るか、スクリプト内の名前付きトークンを通じて参照されます。
全てのオプションは、`option.NAME` のように Options コンテキスト内に定義されます。

[ジョブワークフロー - コンテキスト変数](job-workflows.html#context-variables) を参照して下さい。

See the [Job Workflows - Context Variables](job-workflows.html#context-variables) Section.

**例:**

**Example:**

"hello" という名前で、"message" というオプションをもつジョブがあるとします。

"Hello" ジョブのオプションシグネチャは `-message <>` になります。

![Option usage](../figures/fig0504.png)

引数は `${option.message}` として定義されてスクリプトに渡ります。

スクリプトの中身はこのようになります。

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
    #!/bin/sh    
    echo envvar=$RD_OPTION_MESSAGE ;# read from environment
    echo args=$1                   ;# comes from argument vector
    echo message=@option.message@  ;# replacement token
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

ユーザが "hello" ジョブを走らせると、"message" 値用のプロンプトが出ます。

![Option entered](../figures/fig0505.png)

"howdy" を入力したと仮定しましょう。ジョブのアウトプットは以下のようになるはずです。

    envar=howdy
    args=howdy    
    message=howdy    

これはオプションがセットされていないときの挙動を知るために大切です。
必須ではくデフォルト値もないオプションを定義しているとそのようなことが起こり得ます。

message オプション無しでジョブが実行された場合を考えてみて下さい。きっと以下のようなアウトプットになるでしょう。

    envar=
    args=
    message=@option.message@

これに対処するためのいくつかの tips があります。

環境変数:

:    予防として、変数の存在をテストして（恐らく）デフォルト値をセットしているでしょう。
     変数の存在をチェックするために以下の構文を使っているかもしれません

         test -s  $RD_OPTION_NAME

:    Bash の機能を使えば、テストしつつデフォルト値をセットすることが可能です。

         ${RD_OPTION_NAME:=mydefault} 

置換トークン
Replacement token	 

:    オプションがセットされていないと、トークンはスクリプト内で放置されるでしょう。
     スクリプトの実装を以下のように変更することで、少しだけ堅牢にすることができます。

        message=@option.message@
        atsign="@"
        if [ "$message" == "${atsign}option.message${atsign}" ] ; then
           message=mydefault
        fi

## Calling a Job with options

ジョブはコマンドラインの `run` シェルツールから、もしくは他のジョブのワークフローから実行することができます。

オプションを指定するフォーマットは `-name value` です。

`run` コマンドのあとに、ダブルハイフンでオプションとその値を指定します:

    run -i jobId -- -paramA valA -paramB valB
    run -j group/name -p project -- -paramA valA -paramB valB

XML 定義内では、これらを `arg` 要素として挿入します:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.xml}
<command>
    <jobref group="test" name="other tests">
        <arg line="-paramA valA -paramB valB"/>
    </jobref>
</command>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 

より詳細については [run(1)](../manpages/man1/run.html), [job-v20(5)](../manpages/man5/job-v20.html) のマニュアルページを参照して下さい。

## オプションモデルプロパイダ

オプションモデルプロバイダは、ジョブに定義されたオプションに対して、リモートサービスやデータベースから入力を受けることを可能にするメカニズムです。

オプションモデルプロバイダは基本的にオプション毎に設定されます。

### 必須条件 ###

1. オプションモデルデータは [JSON フォーマット](http://www.json.org)でなければなりません。
2. HTTP(S) アクセス可能であるか Runeck サーバのローカルディスクに配置する必要があります。
3. 二つの内のいずれかの JSON 構造でなければなりません。
    * 文字列配列
    * もしくは、Map の配列 (`name` and `value` 形式)

### 設定 ###

それぞれのジョブオプションのエントリは可能な値をリモート URL から取得するよう設定することができます。
もしジョブを [job.xml ファイルフォーマット](../manpages/man5/job-v20.html#option)で編集する場合、シンプルに `valuesUrl` 属性を `<option>` に加えて下さい。
もし Rundec web GUI から編集する場合、URL をオプションの Remote URL フィールドに入れて下さい。

例：

    <option valuesUrl="http://site.example.com/values.json" ...

*注*：File URL スキーマも利用可能です (例 `file:/path/to/job/options/optA.json`).

オプション値のデータは、必ず以下に示す JSON データフォーマットに沿ったものを返さなければなりません。

### JSON フォーマット

戻すデータについては3つのスタイルがサポートされています。
シンプルリスト、シンプルオブジェクト、そして name/value リストです。
シンプルリストでは、ジョブ実行時にリスト値がポップアップリストで表示されます。
シンプルオブジェクトか name/value ペアが返されると、`name` がリストに表示され、`value` が実際の入力値として利用されます。

*例*

シンプルリスト:

    ["x value for test","y value for test"]

値がセレクトメニューに投入されます。

シンプルオブジェクト:

    { "Name": "value1", "Name2":"value2" }

names が表示され、value が使われるようにセレクトメニューへ投入されます。

Name Value List:
 
    [
      {name:"X Label", value:"x value"},
      {name:"Y Label", value:"y value"},
      {name:"A Label", value:"a value"}
    ] 

### カスケーディングリモートオプション

カスケーディングオプションは、ジョブ実行時にユーザが他のオプション値用に入力した値をリモートバリュー URL で利用することを可能にします。
ユーザが入力するか値を選択すると、リモートの JSON が現状のオプションに更新されます。

これは、階層的もしくは独立したオプション値のセットを宣言するメカニズムを実現します。

例. あなたが "repository" オプションを選択し、他のオプションでそのリポジトリ内の特定の "branch" を選択したいとします。
オプションプロパイダを選択された "repository" 値に正常に応答するように定義して、リモートオプション URL を "repository" オプション値への参照を含むように定義します。
Rundeck GUI は "branch" オプション値をロードする時に JSON 値を remote URL から再読込し、正しい "repository" 値を挿入します。
ユーザが選択中のリポジトリを変更すれば、branch 値も自動で更新されます。

あるオプションと別のオプションの依存関係を、リモートバリュー URL によるプロパティリファレンスを埋め込むことで宣言できます。
プロパティリファレンスは、`${option.[name].value}` の形式をとります。もし "http://server/options?option2=${option.option2.value}" のようなリモートバリューでオプションを宣言すれば、そのオプションは "option2" オプションの値に依存するようになります。

GUI では、オプションがロードされると、option2 がまず表示され、その後 opiton2 が選択されたら option1 が一度だけ読み込まれます。

オプションが他のオプションと依存関係をもち、URL をロードしたときに値がセットされていない場合は、埋め込まれた参照は ""（空文字列）として評価されます。

オプションが他のオプションと依存関係を持ち、リモートバリューの [JSON data](#json-data) が空（空リスト or 空オブジェクト）なら、ユーザに必要な値を選択させるよう促す GUI が表示されます。
これは、オプションモデルプロパイダに、いくつかあるいは全ての依存関係を渡すようにできます。

1 つ以上のオプションが依存関係を持つ時は、ある変更によって、他のオプション値がリモート URL からリロードされる可能性があります。

注: オプション値同士で循環参照を形成すると、自動リロードがオフになります。この場合、ユーザは手動でリロードボタンをクリックしてオプション値を更新する必要があります。

### リモート URL 内での変数展開

"valuesUrl" に定義された URL はリモートリクエスト時に特定のジョブコンテクスト項目が入る変数を埋め込めます。
これはジョブに対して URL をより汎用的に、コンテクスチュアルにします。

展開パターンとして、ジョブコンテクストとオプションコンテクストがあります。

URL 内にジョブ情報を含めるには、${job._property_} 形式で変数を指定します。

ジョブコンテクストで埋め込めるプロパティは以下の通りです:

* `name`: ジョブ名
* `group`: グループ名
* `description`: 説明
* `project`: プロジェクト名
* `argString`: ジョブのデフォルト引数の文字列

URL 内にオプション情報を含めるには、${option._property_} 形式で変数を指定します。

オプションコンテキストで埋め込めるプロパティは以下の通りです:

* `name`: 現在のオプション名

[カスケーディングリモートオプション](#カスケーディングリモートオプション) 値の情報を URL 内に含めるには、${option._name_.value} で指定します:

* `option.[name].value`: 選択された値を他のオプション名で置き換える。もしオプションがセットされていなければ空文字列（""）で置き換える。

*例*

*Examples*

    http://server.com/test?name=${option.name}

オプション名は "name" クエリパラメータとして URL に渡る。

    http://server.com/test?jobname=${job.name}&jobgroup=${job.group}

ジョブ名とグループがクエリパラメータとして渡る。

    http://server.com/branches?repository=${option.repository.value}

選択された "repository" オプションの値か、セットされていなければ ""（空文字列）が渡ります。
このオプションは "repository" オプションに依存していて、"repository" 値が変化すれば、リモートオプション値もリロードされます。

### リモートリクエストの失敗

リモートオプション値の取得に失敗した場合、GUI フォームは警告メッセージを出します。

![](../figures/fig0901.png)

この場合、オプションはテキストフィールドからセットされた値しか使えません。
    
### 実装例 ###

2 セクションに渡って、オプションモデルプロパイダとして振る舞うシンプルな CGI スクリプトを用いた例を説明します。

#### Hudson アーティファクトオプションプロパイダ

end-to-end リリースプロセスでは、ビルド成果物の取得と、それを後で配布するための中央リポジトリへのアップが頻繁に求められます。
Hudson のような継続的インテグレーションサーバはシンプルなジョブ設定ステップでビルド成果物を特定できます。
Hudson API はビルドに成功した成果物の一覧をシンプルな HTTP GET リクエストで取得できるネットワークインタフェースを提供しています。

Acme は RPM として成果物を構築し、それらを識別するためにビルドジョブを設定しています。
オペレーションチームは自動ビルドで生成された成果物のバージョンを選択できるジョブをつくりたいと思っています。

[JSON] ドキュメントを生成して Hudson に情報を要求するシンプルな CGI スクリプトがあります。
クエリパラメータに Hudson サーバ、Hudson ジョブ、成果物のパスを利用できます。
ジョブライターは、成果物リストをオプションモデルとして受け取り、結果をジョブユーザにメニューとみせるためにパラメータ化された CGI スクリプトへの URL を指定できます。

以下に示すコードは、成果物の情報を含む XML を取得するために [curl] コマンドを呼び出し、それをパースするために [xmlstarlet] を利用しています。

File listing: hudson-artifacts.cgi
 
    #!/bin/bash
    # Requires: curl, xmlstarlet
    # Returns a JSON list of key/val pairs
    #
    # Query Params and their defaults
    hudsonUrl=https://build.acme.com:4440/job
    hudsonJob=ApplicationBuild
    artifactPath=/artifact/bin/dist/RPMS/noarch/
    
    echo Content-type: application/json
    echo ""
    for VAR in `echo $QUERY_STRING | tr "&" "\t"`
    do
      NAME=$(echo $VAR | tr = " " | awk '{print $1}';);
      VALUE=$(echo $VAR | tr = " " | awk '{ print $2}' | tr + " ");
      declare $NAME="$VALUE";
    done

    curl -s -L -k $hudsonUrl/${hudsonJob}/api/xml?depth=1 | \
      xmlstarlet sel -t -o "{" \
        -t -m "//build[result/text()='SUCCESS']" --sort A:T:L number  \
        -m . -o "&quot;Release" -m changeSet/item -o ' ' -v revision -b \
        -m . -o ", Hudson Build " -v number -o "&quot;:" \
        -m 'artifact[position() = 1]' -o "&quot;" -v '../number' -o $artifactPath -o "{" -b \
        -m 'artifact[position() != last()]' -v 'fileName' -o "," -b \
        -m 'artifact[position() = last()]' -v 'fileName' -o "}&quot;," \
        -t -o "}"

このスクリプトを運用している web サーバの CGI 動作可能なディレクトリに配置したら、`curl` をつかって直接リクエストすることでテストできます。

    curl -d "hudsonJob=anvils&artifactPath=/artifact/bin/dist/RPMS/noarch/" \
        --get http://opts.acme.com/cgi/hudson-artifacts.cgi

サーバレスポンスは以下の例のような JSON データである必要があります:

    [ 
      {name:"anvils-1.1.rpm", value:"/artifact/bin/dist/RPMS/noarch/anvils-1.1.rpm"}, 
      {name:"anvils-1.2.rpm", value:"/artifact/bin/dist/RPMS/noarch/anvils-1.2.rpm"} 
    ]	

ここで、ジョブはオプションデータを以下のようにリクエストできます:

Now in place, jobs can request this option data like so:

     <option name="package" enforcedvalues="true" required="true"
        valuesUrl="http://ops.acme.com/cgi/hudson-artifacts.cgi?hudsonJob=anvils"/> 

Rundeck UI はパッケージ名をメニュー内に表示し、それが選択されると、ジョブは Hudson サーバ上のビルド成果物へのパスを保持することになります。

[Hudson]: http://hudson-ci.org/
[Hudson API]: http://wiki.hudson-ci.org/display/HUDSON/Remote+access+API
[JSON]: http://www.json.org/

#### Yum リポジトリオプションモデルプロパイダ

[Yum] は [RPM] パッケージ管理を自動化してくれるグレートなツールです。Yum があれば、管理者はパッケージをリポジトリへ登録し、yum クライアントツールを依存関係に沿って自動でインストールするために利用できます。
Yum は [repoquery] と呼ばれる rpm クエリとよく似た問い合わせを Yum リポジトリにするための、便利なコマンド群を含んでいます。

Acme はアプリケーションのリリースパッケージを配布するための自分自身の Yum リポジトリーをセットアップします。
Acme 管理者は、どのパッケージが与えられた能力を供給できるかを知るためのオプションモデルを提供したいです。

以下に示すコードは、repoquery コマンドをシンプルにラップして JSON データで結果を整形するものです。

File listing: yum-repoquery.cgi
    
    #!/bin/bash
    # Requires: repoquery
    # 
    # Query Params and their defaults
    repo=acme-staging
    label="Anvils Release"
    package=anvils
    max=30
    #
    echo Content-type: application/json
    echo ""
    for VAR in `echo $QUERY_STRING | tr "&" "\t"`
    do
      NAME=$(echo $VAR | tr = " " | awk '{print $1}';);
      VALUE=$(echo $VAR | tr = " " | awk '{ print $2}' | tr + " ");
      declare $NAME="$VALUE";
    done

    echo '{'
    repoquery --enablerepo=$repo --show-dupes \
      --qf='"${label} %{VERSION}-%{RELEASE}":"%{NAME}-%{VERSION}-%{RELEASE}",' \
      -q --whatprovides ${package} | sort -t - -k 4,4nr | head -n${max}
    echo '}'

このスクリプトを運用している web サーバの CGI 動作可能なディレクトリに配置したら、`curl` をつかって直接リクエストすることでテストできます。

    curl -d "repo=acme&label=Anvils&package=anvils" \
        --get http://ops.acme.com/cgi/yum-repoquery.cgi
 
サーバレスポンスは以下の例のような JSON データである必要があります:

    TODO: include JSON example
 
ここで、ジョブはオプションデータを以下のようにリクエストできます:

     <option name="package" enforcedvalues="true" required="true"
        valuesUrl="http://ops.acme.com/cgi/yum-repoquery.cgi?package=anvils"/> 

Rundeck UI はパッケージ名をメニュー内に表示し、それが選択されると、ジョブはマッチしたパッケージバージョンを保持することになります。

[Yum]: http://yum.baseurl.org/
[RPM]: http://www.rpm.org/
[repoquery]: http://linux.die.net/man/1/repoquery


## まとめ 

この章を読んだので、ジョブをオプションを含めて走らせる方法や、それを変更する方法について理解できたはずです。
オプションデータを生成してあなたのジョブで使うことに興味がでてきたら、[例から学ぶ Rundeck](#rundeck-by-example)の[オプションモデルプロパイダの例](rundeck-by-example.html#オプションモデルプロパイダの例)を参照してください。
