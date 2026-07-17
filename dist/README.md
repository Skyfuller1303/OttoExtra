# Build-Ausgabe

Nach einem erfolgreichen Build werden die remappten JAR-Dateien durch den Gradle-Task `dist` automatisch in diesen Ordner kopiert.

Dieses Quellarchiv enthält bewusst keine ältere OttoExtra-JAR, damit sie nicht mit dem aktuellen Quellstand der Version `0.1.13.2-y` verwechselt wird.

Build-Befehl:

```bash
./gradlew clean build dist
```
