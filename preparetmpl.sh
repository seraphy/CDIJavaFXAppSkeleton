#! /bin/bash

#############################
# Cygwinで実行してください。#
#############################

if [ -z "$JAVA_HOME" ]; then
	export JAVA_HOME="C:/java/jdk1.8.0_121"
	export PATH="$(cygpath -u $JAVA_HOME)/bin:$PATH"
fi

if [ -z "$MAVEN_HOME" ]; then
	export MAVEN_HOME="C:/java/apache-maven-3.5.4"
	export PATH="$(cygpath -u $MAVEN_HOME)/bin:$PATH"
fi

echo mavenで既存のプロジェクトからアーキタイプ用の雛形を生成します
pushd jfxtemplate
# archtype.propertiesファイルを指定して、archetype-metadata.xmlにカスタムプロパティを追加します。
mvn -Darchetype.properties=archtype.properties clean archetype:create-from-project
popd

echo 生成されたソースをワークにコピーします
rm -fr work
mkdir work
cp -r jfxtemplate/target/generated-sources/archetype work/

echo 未変換のパッケージ名、メインクラス名を置換します
find work -name '*.java' | xargs sed -i 's/jp.seraphyware.javafxexam.jfxexam1/${package}/g;s/MainApp/${mainClassName}/g'
sed -i 's/jp.seraphyware.javafxexam.jfxexam1/${package}/g;s/MainApp/${mainClassName}/g' work/archetype/src/main/resources/archetype-resources/pom.xml
sed -i 's/jfxtemplate/${artifactId}/g' work/archetype/src/main/resources/archetype-resources/.project

echo 不要ファイルを削除します
rm work/archetype/src/main/resources/archetype-resources/archtype.properties

echo 現在のarchtypeソースを置換します
mkdir work/backup
cp -pr src/main/resources work/backup || exit 1
rm -fr src/main/resources
cp -pr work/archetype/src/main/resources src/main/

echo ワークディレクトリを削除します。
rm -fr work

echo 完了しました。
