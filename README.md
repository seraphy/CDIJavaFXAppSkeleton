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


## このアーキタイプのローカルでのビルド方法

このアーキタイプ自身をビルドする場合、

```shell
mvn clean install archetype:update-local-catalog -U
```

これにより、アーキタイプがビルドされローカルリポジトリに格納されるとともに
 `~/.m2/repository/archetype-catalog.xml` が作成(更新)される。

### オプションの説明

- `install` アーキタイプのjarをローカルリポジトリに登録
- `archetype:update-local-catalog` アーキタイプのカタログを更新
- `-U` スナップショットの明示的な更新

https://maven.apache.org/archetype/maven-archetype-plugin/update-local-catalog-mojo.html

### ローカルリポジトリ上のアーキタイプのカタログを一括更新する場合は...

以下のようにするとローカルリポジトリにキャッシュされている全てのアーティファクトをスキャンして、
アーキタイプである場合にはカタログを更新する。

```shell
mvn archetype:crawl
```

http://maven.apache.org/archetype/maven-archetype-plugin/crawl-mojo.html


## 参考

- https://stackoverflow.com/questions/13089419/install-maven-archetype/36536155
- https://ikikko.hatenablog.com/entry/20110503/1304434174
- http://d.hatena.ne.jp/yamkazu-tech/20090516/1242482470
- https://himeji-cs.jp/blog/2018/02/25/location_archetype-catalog-xml/
- https://qiita.com/yukihane/items/004c6e6982149a0d778b



