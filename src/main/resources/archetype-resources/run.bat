rem java8
rem java -jar target/${artifactId}.jar

rem java11
java -p target/mods --add-modules javafx.controls,javafx.fxml,javafx.web -jar target/${artifactId}.jar
