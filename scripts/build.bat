SET lib=..\..\java-advanced-2023\lib\*
SET test=..\..\java-advanced-2023\artifacts\info.kgeorgiy.java.advanced.implementor.jar
SET dependencies=info\kgeorgiy\java\advanced\implementor\
SET classes=..\..\java-advanced-2023\modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/

javac -d . -cp %test% ..\..\java-advanced\java-solutions\info\kgeorgiy\ja\faizieva\implementor\Implementor.java
javac -d . -cp %test% %classes%Impler.java %classes%JarImpler.java ..\..\java-advanced-2023\modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/ImplerException.java

echo Manifest-Version: 1.0 > MANIFEST.MF
echo Main-Class: info.kgeorgiy.ja.faizieva.implementor.Implementor >> MANIFEST.MF
echo Class-Path: ../../java-advanced-2023/artifacts/info.kgeorgiy.java.advanced.implementor.jar >> MANIFEST.MF
jar -c --manifest=MANIFEST.MF --file=Implementor.jar info\kgeorgiy\ja\faizieva\implementor\* %dependencies%*.class