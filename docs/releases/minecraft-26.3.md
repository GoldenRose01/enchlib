# EnchLib 1.4.0 – Matrice Minecraft 26.x

La release 1.4.0 fornisce un JAR dedicato a ciascuna versione Minecraft supportata. Ogni JAR dichiara nel metadata la versione esatta e la relativa Fabric API.

| Minecraft | Fabric API | JAR |
| --- | --- | --- |
| 26.1 | `0.145.1+26.1` | [`enchlib-mc26.1-1.4.0.jar`](../../jar/enchlib-mc26.1-1.4.0.jar) |
| 26.1.1 | `0.145.4+26.1.1` | [`enchlib-mc26.1.1-1.4.0.jar`](../../jar/enchlib-mc26.1.1-1.4.0.jar) |
| 26.1.2 | `0.155.3+26.1.2` | [`enchlib-mc26.1.2-1.4.0.jar`](../../jar/enchlib-mc26.1.2-1.4.0.jar) |
| 26.2 | `0.161.0+26.2` | [`enchlib-mc26.2-1.4.0.jar`](../../jar/enchlib-mc26.2-1.4.0.jar) |
| 26.3 | `0.161.0+26.3` | [`enchlib-mc26.3-1.4.0.jar`](../../jar/enchlib-mc26.3-1.4.0.jar) |

## Modifiche

- Versione EnchLib aggiornata a `1.4.0`.
- Categorie mob configurabili tramite ID namespaced, inclusi quelli registrati da altre mod.
- Il JSON generato dal catalogo Excel è caricabile dal mondo; `/plusec-debug reload` ricarica i dati senza riavviare.
- Schermata Mod Menu con YACL per modificare le categorie mentre si gioca. In single-player applica le modifiche direttamente; su server usa il comando debug e richiede i permessi corrispondenti.
- L'integrazione UI è opzionale: Mod Menu e Yet Another Config Lib non vengono inclusi nel JAR EnchLib.

## Requisiti

Fabric Loader `0.19.5` o superiore e Java `25` o superiore. Fabric API deve corrispondere alla versione Minecraft scelta.
