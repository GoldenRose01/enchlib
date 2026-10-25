# ⚙️ Configurazione del mondo

[Home](Home) · [Comandi](Comandi) · [Tavoli e rarità](Tavoli-e-rarita)

## Percorso e autorità

I file attivi sono in **`<mondo>/config/enchlib/`**. Su un server dedicato è il salvataggio del server. I client ricevono una copia sincronizzata e inviano richieste di modifica; il server le accetta soltanto per giocatori con `COMMANDS_GAMEMASTER`, normalmente operatori di livello 2 o superiore. In singleplayer il proprietario del mondo può modificare le impostazioni.

All’apertura di un mondo, il bootstrap installa i file mancanti e può migrare una volta le impostazioni dalla vecchia cartella globale `config/enchlib/`. Chiudere il mondo elimina lo stato sincronizzato del mondo precedente.

## File principali

| File | Contenuto |
|---|---|
| `AviableEnch.json5` | Elenco di ID e stato `enabled`; il nome storico «Aviable» è intenzionale nel percorso |
| `EnchantmentsDetails.json5` | Livello, categorie, rarità, incompatibilità, regole dei tavoli e preset |
| `EnchLVLmax.config` | Preset `id=livello` |
| `EnchRarity.config` | Preset di rarità |
| `EnchCompatibility.config` | Preset delle categorie di oggetti |
| `EnchUNcompatibility.config` | Preset delle incompatibilità |
| `MobCategory.json` | Categorie dei mob, con precedenza sul formato storico |
| `MobCategory.config` | Formato properties storico delle categorie |
| `MobCategory.defaults.config` | Preset di ripristino delle categorie |

I file `.config` sono preset, non una seconda copia sempre equivalente allo stato JSON5 attivo. I campi `preset_*` conservano valori di ripristino; cambiarli alla cieca può alterare cosa significa «reset».

## Esempio: disponibilità

```json5
[
  // ID completo del registry
  { "id": "minecraft:sharpness", "enabled": true },
]
```

## Esempio: dettagli

```json5
{
  "enchantments": [
    {
      "id": "minecraft:sharpness",
      "max_level": 10,
      "enc_category": ["#minecraft:enchantable/weapon"],
      "mob_category": [],
      "rarity": "common",
      "incompatible_enchantments": ["minecraft:smite"],
      "table_rules": {
        "minecraft:enchanting_table": { "enabled": true, "chance": 75 }
      }
    }
  ]
}
```

Questi esempi spiegano lo schema: aggiungi o modifica una voce nel tuo file esistente, preservando le altre. Il parser dei file di incantamento consente commenti e virgole finali. `MobCategory.json` usa invece il proprio parser JSON: mantienilo come JSON standard.

## Dopo una modifica manuale

Salva una copia dei file, modifica quelli del mondo indicato da `show-path`, quindi usa `/plusec-debug reload`. `/plusec-debug validate` confronta gli ID con il registry corrente. Un ID presente in un preset ma assente dal registry non crea un nuovo incantamento.

Fonti: [GlobalConfigIO](https://github.com/GoldenRose01/enchlib/blob/main/src/main/kotlin/goldenrose01/enchlib/config/GlobalConfigIO.kt), [bootstrap](https://github.com/GoldenRose01/enchlib/blob/main/src/main/kotlin/goldenrose01/enchlib/config/GlobalConfigBootstrap.kt).
