rm -rf build
mkdir build
javac -cp libs/istack-commons-runtime-4.2.0.jar:libs/jakarta.activation-api-2.2.0-M1.jar:libs/jakarta.xml.bind-api-4.0.5.jar:libs/jaxb-runtime-4.0.6.jar:libs/jaxb-core-4.0.6.jar -sourcepath src -d build src/Main.java
java -cp build:libs/istack-commons-runtime-4.2.0.jar:libs/jakarta.activation-api-2.2.0-M1.jar:libs/jakarta.xml.bind-api-4.0.5.jar:libs/jaxb-runtime-4.0.6.jar:libs/jaxb-core-4.0.6.jar Main