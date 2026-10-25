# 🔗 Integrazione con Eldrich Surge

[Home](Home) · [Wiki Eldrich Surge](https://github.com/GoldenRose01/Eldrich-Surge/wiki)

Eldrich Surge aggiunge i propri incantamenti, il tavolo avanzato e le meccaniche. EnchLib offre un sistema condiviso per configurarne le proprietà dove il ponte le consulta.

| Contenuto | Responsabilità |
|---|---|
| Danno, effetti, rituali e fusioni | Eldrich Surge |
| File JSON5 e preset EnchLib | EnchLib |
| Assegnazioni delle categorie mob | EnchLib quando presente; fallback di Eldrich Surge altrimenti |
| Regole del tavolo normale e avanzato | Registrate e consumate dal ponte di Eldrich Surge |
| Incantamenti di altre mod | Rilevati dal registry; comportamento dipendente dalla loro integrazione |

## Cosa registra il ponte

Livelli massimi, selettori degli oggetti, categorie bersaglio, rarità, incompatibilità e le due identità dei tavoli:

- `minecraft:enchanting_table`
- `eldrich-surge:advanced_enchanting_table`

Una modifica alle categorie di un mob può cambiare i contributi di Blade of Apocalypse o Star Fate. Il mob conserva il proprio tipo di registry: cambia soltanto il mapping consultato dalle regole.

## Catalogo di gioco

Le descrizioni di tutti i **104 incantamenti**, con livelli, oggetti e ottenimento, sono nel [catalogo Eldrich Surge](https://github.com/GoldenRose01/Eldrich-Surge/wiki/Enchantments). Questo collegamento evita una seconda copia delle stesse schede che potrebbe diventare incoerente.

Le etichette di rarità narrative della mod non coincidono sempre con le quattro chiavi operative dell’editor EnchLib. Verifica la conversione del ponte e il valore effettivo del mondo.

Fonte: [EnchLibIntegration di Eldrich Surge](https://github.com/GoldenRose01/Eldrich-Surge/blob/main/src/main/java/eldritch/surge/enchantment/mechanics/EnchLibIntegration.java).
