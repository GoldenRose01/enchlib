# 🆕 EnchLib 1.7.0

[Home](Home) · [Installazione](Installazione) · [Commerci dei villager](Commerci-dei-villager)

## Modifiche dalla 1.6.1

- Aggiunti controlli per ogni incantesimo nei commerci dei villager, separati per libri e oggetti.
- Aggiunta la sezione **Villager trades** agli editor Cloth Config e YACL.
- Esclusi di base dalle nuove offerte gli incantesimi con tag `minecraft:curse`, vanilla e aggiunti da mod o datapack.
- Consentita la riabilitazione individuale, con reset al preset del tag.
- Salvate e sincronizzate le nuove regole per mondo usando il sistema di autorizzazione esistente.
- Disponibili build distinte per **Minecraft 26.3** e **26.4 Snapshot 7**.

## Passaggio alla nuova versione

Sostituisci il JAR della 1.6.1 con la build 1.7.0 adatta alla tua versione di Minecraft. I file del mondo vengono conservati: i campi mancanti `villager_books` e `villager_items` seguono automaticamente il preset che esclude le maledizioni.

Le impostazioni si applicano alle nuove offerte. Se il risultato contiene un incantesimo escluso, l'offerta viene scartata interamente e possono comparire meno offerte. Le offerte esistenti non vengono riscritte. Abilitare un incantesimo non lo aggiunge alle liste di generazione dei commerci.

## Build e verifica

Entrambe le build sono state compilate con successo e i metadati dei JAR verificati. Il test in gioco resta da completare. Per dipendenze e nomi dei file consulta [Installazione](Installazione).
