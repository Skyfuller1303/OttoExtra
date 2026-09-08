# TWEAKS-01: Low-Health-Unschärfe bleibt trotz deaktiviertem Haupteffekt aktiv

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Codepfad, unabhängig gegengeprüft; kein Ingame-Test.

## Auslöser und Folge

Mit `tweaks.lowHealth.enabled=false`, `blurEnabled=true` und niedrigen Lebenspunkten wird die Welt weiterhin unscharf gerendert. Das betrifft bereits die Standardkonfiguration. Auch `/ottoextra tweaks lowhealth off` beendet diesen Teil des Effekts nicht.

## Codebeleg

- [OttoExtraConfig.java](../../../src/client/java/de/ottoextra/config/OttoExtraConfig.java#L353): Standardwerte sind `enabled=false`, `blurEnabled=true`, `blurStartHearts=7`, `blurStrength=1`.
- [TweaksModule.java](../../../src/client/java/de/ottoextra/tweaks/TweaksModule.java#L99): `lowHealthBlurPasses()` prüft den Blur-Schalter, aber nicht `cfg.enabled`. Außerhalb des Testmodus berechnet die Methode ihre Intensität direkt aus den Lebenspunkten.
- [GameRendererBlurMixin.java](../../../src/client/java/de/ottoextra/mixin/GameRendererBlurMixin.java#L41): Rendert jeden positiven Rückgabewert ohne zusätzliche Aktivierungsprüfung.

## Reproduktion

1. Low-Health-Effekt ausschalten; Blur-Unteroption auf Standard lassen.
2. In einer Testwelt auf 4 Lebenspunkte fallen.
3. Der Code berechnet weiterhin drei Blur-Durchläufe pro Frame. Erwartet: kein Effekt.

## Fixansatz und Regressionstest

Den Hauptschalter auch im Blur-Pfad berücksichtigen. Gewünschtes Verhalten des expliziten Testbefehls separat definieren. Prüfen: ausgeschaltet + niedrige HP ergibt null Durchläufe; eingeschaltet + dieselben HP aktiviert Blur; Ausschalten beendet den Effekt.

## Umsetzung am 06.09.2026

Der Blur-Pfad berücksichtigt den Hauptschalter und liefert bei ausgeschaltetem Low-Health-Effekt sofort null Durchläufe. Der ausdrücklich gestartete Testmodus kann den Hauptschalter weiterhin übersteuern; Stop/Off beendet die Vorschau.

Regression: [LowHealthBlurTest.java](../../../src/test/java/de/ottoextra/tweaks/LowHealthBlurTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
