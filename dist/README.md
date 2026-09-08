# Lokale Release-Artefakte

Die Alpha zum Weitergeben heißt `ottoextra-0.1.17-l.jar`.
Installation, Testschwerpunkte und bekannte Beobachtungen stehen in
[den Release-Notizen](../release-notes-0.1.17-l.md).

JARs und ZIPs werden lokal erzeugt und nicht in Git versioniert.
Der Alpha-Build verwendet `gradlew.bat build -x dist`; anschließend wird die
remappte JAR aus `build/libs/` hierher kopiert. So bleiben ältere Artefakte erhalten.
