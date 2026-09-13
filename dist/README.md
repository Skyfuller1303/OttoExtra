# Lokale Release-Artefakte

Die JAR zum Weitergeben heißt `ottoextra-0.1.17.jar`.
Installation, Testschwerpunkte und bekannte Beobachtungen stehen in
[den Release-Notizen](../release-notes-0.1.17.md).

JARs und ZIPs werden lokal erzeugt und nicht in Git versioniert.
`gradlew.bat build` kopiert die remappte JAR und das Sources-Archiv nach `dist/`.
Dabei werden vorhandene JARs in diesem Ausgabeordner ersetzt. Für die Installation
nur `ottoextra-0.1.17.jar` verwenden, nicht das Sources-Archiv.
