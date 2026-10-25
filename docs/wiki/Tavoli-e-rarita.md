# 🔮 Tavoli e rarità

[Home](Home) · [Configurazione](Configurazione) · [API](API)

## Una regola per ogni tavolo

Ogni tavolo è identificato da un ID stabile, per esempio `minecraft:enchanting_table` o `eldrich-surge:advanced_enchanting_table`. `table_rules` conserva per ciascun tavolo:

| Campo | Valore | Significato |
|---|---|---|
| `enabled` | Booleano | L’incantamento è consentito nel tavolo |
| `chance` | Intero 0–100 | Probabilità base da applicare nel filtro delle offerte |

L’API restituisce `null` quando non esiste una regola, permettendo alla mod chiamante di usare il suo predefinito. Zero e assenza non sono equivalenti.

## Rarità configurabile

| Chiave | Peso | Con chance 80 |
|---|---:|---:|
| `common` | 100 | 80% |
| `uncommon` | 60 | 48% |
| `rare` | 30 | 24% |
| `very_rare` | 5 | 4% |

Formula del filtro: **`chance × peso / 100`**. Il percorso di Eldrich Surge usa interi, con troncamento. Sono percentuali di ammissione al filtro, non garanzie sulla composizione finale dell’offerta.

Le classificazioni narrative di Eldrich Surge, come «Legendary», «Ultra» o «Unique», non sono automaticamente chiavi accettate da `setRarity`, che ammette le quattro chiavi della tabella. Alcuni commenti dei vecchi preset elencano altre classi: l’API corrente è il riferimento operativo.

## Categorie e incompatibilità

Le categorie di oggetti possono usare tag, gruppi o ID. Le incompatibilità sono relazioni fra ID di incantamento; quando impostate con `setIncompatibleEnchantments`, la relazione viene resa simmetrica e vengono rimosse le vecchie relazioni non più richieste.

Queste proprietà devono essere consultate dal sistema che genera o applica l’offerta. EnchLib non riscrive automaticamente ogni interfaccia privata di tutte le altre mod.

Fonte: [EnchantLibAPI](https://github.com/GoldenRose01/enchlib/blob/main/src/main/kotlin/goldenrose01/enchlib/api/EnchLibAPI.kt).
