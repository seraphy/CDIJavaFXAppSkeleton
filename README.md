# JavaFXアプリケーションスケルトン作成アーキタイプ

## 利用方法

MavenでGenerateするまえに、以下のURLからアーキタイプのカタログを参照する。

```
https://raw.githubusercontent.com/seraphy/JavaFXAppSkeleton/master/mvnrepo/archtype-catalog.xml
```

このアーキタイプのリポジトリはgithub上にあるが、このカタログによりGithub上のリポジトリを参照・取得するようになっている。

このアーキタイプのグループID, アーティファクトIDは以下のようになっている。

| name                | value                        |
|:--------------------|:-----------------------------|
| archetypeGroupId    | jp.seraphyware.javafxexample |
| archetypeArtifactId | jfxappskeleton               |
| archetypeVersion    | 0.0.1-SNAPSHOT               |


### コマンドラインから生成する場合

以下のようにアーキタイプのGroupId, ArtifactId, Versionを指定してスケルトンを生成する。

```shell
mvn archetype:generate -DarchetypeGroupId=jp.seraphyware.javafxexample -DarchetypeArtifactId=jfxappskeleton -DarchetypeVersion=0.0.1-SNAPSHOT -DgroupId=jp.seraphyware.mvnexam -DartifactId=jfxapp -Dversion=1.0.0-SNAPSHOT
```

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
mvn clean install archetype:update-local-catalog -U
```

これにより、アーキタイプがビルドされローカルリポジトリに格納されるとともに
 `~/.m2/repository/archetype-catalog.xml` が作成(更新)される。

- `install` アーキタイプのjarをローカルリポジトリに登録
- `archetype:update-local-catalog` アーキタイプのカタログを更新
- `-U` スナップショットの明示的な更新

https://maven.apache.org/archetype/maven-archetype-plugin/update-local-catalog-mojo.html


## deployによるMavenリポジトリの更新

このプロジェクトは mvnの `deploy` フェーズにより、自身の `mvnrepo` フォルダをリポジトリとして成果物を出力する。

このフォルダには `archtype-catalog.xml` アーキタイプのカタログファイルもあらかじめ用意しており、
リポジトリの位置として、このプロジェクトのGitHubのrawページ先をURLとして指定してある。

これを含めてプロジェクト全体をgithubにプッシュすれば、そのままアーキタイプのリポジトリとして利用可能になる。


## 既存のプロジェクトをもとに、アーキタイプ用のひな形を作成する方法

```shell
mvn clean archetype:create-from-project
```

これにより、`target` ディレクトリ下に archtype のひな形が作成される。

ソースコード上の `package` などは `${package}` のように置換されており、フォルダ構造も短く補正される。


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


