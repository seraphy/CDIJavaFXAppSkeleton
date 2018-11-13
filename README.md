# CDIを使ったJavaFXアプリケーションのスケルトンを作成するアーキタイプ

[ ![Download](https://api.bintray.com/packages/seraphy/maven/cdi-jfxapp-skeleton/images/download.svg) ](https://bintray.com/seraphy/maven/cdi-jfxapp-skeleton/_latestVersion)

## 概要

これは、CDI2.0 を使った JavaFX のアプリケーションの骨格を生成するアーキタイプである。

CDIを使わないシンプルなアーキタイプは [SimpleJFXSkeleton](https://github.com/seraphy/SimpleJFXSkeleton) にある。


### 特徴

一般的なアプリケーションが必要とするような、以下のような機能をスケルトンで提供する。

- CDI2.0の利用(WeldSE)
- 設定ファイル、ウィンドウサイズの永続化
- ログ出力(SLF4J + Log4j2)
- 非同期タスクによるプログレスダイアログ
- メニューの制御
- 基本フォントサイズの設定


## 利用方法

MavenでGenerateするまえに、以下のURLからアーキタイプのカタログを参照する。

- GitHub

```
https://raw.githubusercontent.com/seraphy/CDIJavaFXAppSkeleton/master/mvnrepo/archtype-catalog.xml
```

このアーキタイプのリポジトリはgithub上にあるが、このカタログによりGithub上のリポジトリを参照・取得するようになっている。

- Bintray

```
https://dl.bintray.com/seraphy/maven/archtype-catalog.xml
```

同じものを [Bintray](https://bintray.com/seraphy/maven/cdi-jfxapp-skeleton) からも取得できるようにしてある。


このアーキタイプのグループID, アーティファクトIDは以下のようになっている。

| name                | value                     |
|:--------------------|:--------------------------|
| archetypeGroupId    | jp.seraphyware.archetypes |
| archetypeArtifactId | cdi-jfxapp-skeleton       |
| archetypeVersion    | 0.0.1                     |


生成時には必須パラメータとして以下のものが必要である。

- mainClassName メインクラス名(デフォルトはMainApp)


### EclipseからMavenプロジェクトとして新規作成する場合

上記の `archtype-catalog.xml` のURLをカタログのURLとして追加すれば、利用可能になる。

### コマンドラインから生成する場合

以下のようにアーキタイプのGroupId, ArtifactId, Versionを指定してスケルトンを生成する。

```shell
mvn archetype:generate -DarchetypeGroupId=jp.seraphyware.archetypes -DarchetypeArtifactId=cdi-jfxapp-skeleton -DarchetypeVersion=0.0.1 -DgroupId=jp.seraphyware.mvnexam -DartifactId=jfxapp -Dversion=1.0.0-SNAPSHOT
```

ただし、まだローカルマシン上の `~/.m2/repository` にアーキタイプが格納されておらず、`archtype-catalog.xml` も存在しない場合は、以下のいずれかの方法をとる。

- 明示的にarchtypeが格納されているリポジトリを指定する
  - ~/.e2/settings.xml にリポジトリを追加する
  - コマンドラインで明示的にarchtypeが格納されているリポジトリを指定する
- 明示的に `archtype-catalog.xml` を追記する


### ~/.e2/settings.xmlにリポジトリを追加する場合

以下のようなmavenの設定ファイルを作成する。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings
    xmlns="http://maven.apache.org/SETTINGS/1.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <profiles>
        <profile>
            <id>local-development</id>
            <repositories>
                <repository>
                    <id>seraphy-binray</id>
                    <name>seraphy's binray</name>
                    <url>https://dl.bintray.com/seraphy/maven/</url>
                </repository>
                <!--
                <repository>
                    <id>cdijavafxappskeletonrepo</id>
                    <name>cdijavafxappskeletonrepo</name>
                    <url>https://raw.githubusercontent.com/seraphy/CDIJavaFXAppSkeleton/master/mvnrepo/</url>
                </repository>
                -->
            </repositories>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>local-development</activeProfile>
    </activeProfiles>
</settings>
```

### コマンドラインで明示的にリポジトリを指定する場合

以下のようなmavenの設定ファイルを作成する。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings
    xmlns="http://maven.apache.org/SETTINGS/1.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <profiles>
        <profile>
            <id>local-development</id>
            <repositories>
                <repository>
                    <id>seraphy-binray</id>
                    <name>seraphy's binray</name>
                    <url>https://dl.bintray.com/seraphy/maven/</url>
                </repository>
                <!--
                <repository>
                    <id>cdijavafxappskeletonrepo</id>
                    <name>cdijavafxappskeletonrepo</name>
                    <url>https://raw.githubusercontent.com/seraphy/CDIJavaFXAppSkeleton/master/mvnrepo/</url>
                </repository>
                -->
            </repositories>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>local-development</activeProfile>
    </activeProfiles>
</settings>
```

このファイルを作業フォルダ上に `settings.xml` として作成した場合は、

```shell
mvn archetype:generate -DarchetypeGroupId=jp.seraphyware.archetypes -DarchetypeArtifactId=cdi-jfxapp-skeleton -DarchetypeVersion=0.0.1 -DgroupId=jp.seraphyware.mvnexam -DartifactId=jfxapp -Dversion=1.0.0-SNAPSHOT -s settings.xml
```

のように設定ファイルへのパスを明示して、archtypeが格納されたリポジトリにアクセスすることができる。

また、このようにして一度使用すると、`~/.m2/repository` にキャッシュされる。

カタログは作成されていないが、次からは設定ファイルを指定せずとも、ローカル上にキャッシュされたものを参照できるようになる。


### 明示的に `archtype-catalog.xml` を追記する場合

上記のカタログのURLから取得されるカタログ定義を、ローカルマシンの `~/.m2/repository/archtype-catalog.xml` に追記する。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<archetype-catalog xsi:schemaLocation="http://maven.apache.org/plugins/maven-archetype-plugin/archetype-catalog/1.0.0 http://maven.apache.org/xsd/archetype-catalog-1.0.0.xsd"
	xmlns="http://maven.apache.org/plugins/maven-archetype-plugin/archetype-catalog/1.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<archetypes>
		<archetype>
			<groupId>jp.seraphyware.archetypes</groupId>
			<artifactId>cdi-jfxapp-skeleton</artifactId>
			<version>0.0.1</version>
			<description>cdi javafx app skeleton</description>
			<repository>https://raw.githubusercontent.com/seraphy/CDIJavaFXAppSkeleton/master/mvnrepo/</repository>
		</archetype>
	</archetypes>
</archetype-catalog>
```

これでアーキタイプの検索ができるようになる。


### ローカルリポジトリ上の利用可能なアーキタイプのカタログを一括更新する場合は...

アーキタイプの一覧を表示するにはカタログが必要である。

以下のようにするとローカルリポジトリにキャッシュされている全てのアーティファクトをスキャンして、
それがアーキタイプである場合にはカタログを更新する。

```shell
mvn archetype:crawl
```

http://maven.apache.org/archetype/maven-archetype-plugin/crawl-mojo.html


-----------------------------------------------
# アーキタイプ作成手順

## このアーキタイプのローカルでのビルド方法

このアーキタイプ自身をビルドする場合、

```shell
mvn clean install archetype:update-local-catalog -U -Duser.name=AUTHOR
```

これにより、アーキタイプがビルドされローカルリポジトリに格納されるとともに
 `~/.m2/repository/archetype-catalog.xml` が作成(更新)される。

- `install` アーキタイプのjarをローカルリポジトリに登録
- `archetype:update-local-catalog` アーキタイプのカタログを更新
- `-U` スナップショットの明示的な更新
- `-Duser.name=AUTHOR` Mavenが生成するMANIFEST.MFの `build-By` に設定されるデフォルト値。

https://maven.apache.org/archetype/maven-archetype-plugin/update-local-catalog-mojo.html


## deployによるMavenリポジトリの更新

このプロジェクトは mvnの `deploy` フェーズにより、自身の `mvnrepo` フォルダをリポジトリとして成果物を出力する。

このフォルダには `archtype-catalog.xml` アーキタイプのカタログファイルもあらかじめ用意しており、
リポジトリの位置として、このプロジェクトのGitHubのrawページ先をURLとして指定してある。

これを含めてプロジェクト全体をgithubにプッシュすれば、そのままアーキタイプのリポジトリとして利用可能になる。


## 既存のプロジェクトをもとに、アーキタイプ用のひな形を作成する方法

本アーキタイプは、`jfxtemplate` フォルダ下にある、eclipseによるJavaFX8アプリケーションのプロジェクトをもとに、テンプレートを作成している。

元にしたいプロジェクト下において以下のようにすると、

```shell
mvn clean archetype:create-from-project
```

これにより、`target` ディレクトリ下に archtype のひな形が作成される。

ソースコード上の `package` などは `${package}` のように置換され、フォルダ構造も短く補正されており、
archtypeに必要なメタデータなども生成されるので、これを元に手直することでテンプレートを作成する。


なお、本アーキタイプでは、シェルスクリプト `preparetmpl.sh` を cygwin上から実行することで、これらの雑多な処理をまとめて実行して構築している。

実際の生成手順については、このシェルスクリプトを参照のこと。


### カスタムプロパティ定義の作成

プロパティを定義したプロパティファィルを用意し、以下のようにひな形を生成する。

```shell
mvn -Darchetype.properties=generate.properties clean archetype:create-from-project
```

これにより、`archetype-metadata.xml` ファイル上の `requiredProperties` セクションにカスタムプロパティの定義を追記することができる。

以下のようなプロパティファイルを指定した場合、

```
mainClassName=Main
```

`archetype-metadata.xml` には、以下のようなプロパティが追記される。

```xml
	<requiredProperties>
		<requiredProperty key="mainClassName">
			<defaultValue>Main</defaultValue>
		</requiredProperty>
	</requiredProperties>
```

### リソースのフォルダ構造

Javaのソースファイルのpackage構造は認識され自動的に補正されるが、リソースファイルのフォルダ構造は補正されない。



### ファイルやフォルダ名の置換方法

ソースコード上のパラメータは${name}によって置換されるが、

ファイルやフォルダの名前は `__propertyName__` のようにアンダースコア2つで囲んだファイル名にすることで置換することができる。


### Bintrayへのアップロード

このアーキタイプの成果物はBintrayのリポジトリにも格納する。

https://bintray.com/seraphy/maven/cdi-jfxapp-skeleton


※ pom.xmlのdistributionManagementでは管理していないので、手動でアップロードする必要がある。

*``/archtype-catalog.xml`` は  [simple-jfx-skeleton](https://bintray.com/seraphy/maven/simple-jfx-skeleton) 側のファイルとして管理・保持されているので、こちらのものとマージすること。*


## 参考

- https://stackoverflow.com/questions/13089419/install-maven-archetype/36536155
- https://ikikko.hatenablog.com/entry/20110503/1304434174
- http://d.hatena.ne.jp/yamkazu-tech/20090516/1242482470
- https://himeji-cs.jp/blog/2018/02/25/location_archetype-catalog-xml/
- https://qiita.com/yukihane/items/004c6e6982149a0d778b

archtypeのテスト方法について

- http://cynipe.hateblo.jp/entry/20110113/1294938351

テンプレート生成について

- https://rsankarx.wordpress.com/2013/10/24/creating-maven-archetype-using-create-from-project/


