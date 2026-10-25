![EnchLib — Regole del mondo](https://raw.githubusercontent.com/wiki/GoldenRose01/enchlib/assets/banner.svg)

**Minecraft 26.3 · Fabric · EnchLib 1.6.1**

Un centro di controllo per incantamenti, tavoli e categorie dei mob. Le impostazioni appartengono al mondo; il server le salva e le sincronizza ai client.

| 🎮 Giocatori e amministratori | 🧩 Sviluppatori |
|---|---|
| [Installazione](Installazione) | [API e integrazione](API) |
| [Configurazione JSON5](Configurazione) | [Regole dei tavoli e rarità](Tavoli-e-rarita) |
| [Catalogo dei preset degli incantamenti](Incantamenti) | [Integrazione Eldrich Surge](Eldrich-Surge) |
| [Categorie dei mob](Categorie-mob) | [Fonti e manutenzione](Fonti) |
| [Comandi](Comandi) · [Problemi frequenti](FAQ) | [Guida API estesa](https://github.com/GoldenRose01/enchlib/blob/main/docs/API_INTEGRATION.md) |

## Il flusso in tre passi

1. Apri un mondo: EnchLib prepara `<mondo>/config/enchlib/`.
2. Modifica disponibilità, livelli, categorie o regole con l’editor e i comandi.
3. Il server valida e distribuisce i valori; le meccaniche integrate li consultano quando servono.

## Configurazione e gameplay

EnchLib può rilevare e memorizzare gli incantamenti registrati da altre mod. Una proprietà produce un effetto nel gioco dove l’operazione appartiene a EnchLib o dove la mod interessata legge la sua API. Visualizzare un valore nell’editor non garantisce che qualsiasi tavolo esterno lo applichi automaticamente.

EnchLib non aggiunge il catalogo di incantamenti di Eldrich Surge: lo gestisce come integrazione opzionale. Le descrizioni dei 104 incantamenti si trovano nella [wiki Eldrich Surge](https://github.com/GoldenRose01/Eldrich-Surge/wiki/Enchantments).

---

[Repository](https://github.com/GoldenRose01/enchlib) · [Segnala un problema](https://github.com/GoldenRose01/enchlib/issues) · [Minecraft Wiki: incantamenti](https://minecraft.wiki/w/Enchantment)
