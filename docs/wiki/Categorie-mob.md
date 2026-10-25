# 🐾 Categorie dei mob

[Home](Home) · [Comandi](Comandi) · [API](API)

Un mob è associato al suo **ID di registry**, non al nome tradotto. Le categorie sono etichette modificabili; un’entità può appartenere a più categorie e può provenire da qualsiasi mod.

| Categoria del catalogo | Uso descrittivo |
|---|---|
| `animals` | Animali |
| `magik` | Creature magiche |
| `undead` | Non-morti |
| `rebel` | Categoria usata dalle regole di Flogging |
| `fungi` | Creature fungine |
| `hell` | Creature associate al Nether |
| `void` | Creature associate all’End |
| `water` | Creature acquatiche |
| `arthropods` | Artropodi |
| `cubic` | Creature cubiche |
| `flying` | Creature volanti |

Le etichette non obbligano il mob a trovarsi in una certa dimensione e non coincidono con i gruppi di spawn di Minecraft. Altre categorie sono ammesse: una regola deve però consultarle perché producano un effetto.

## Formato JSON

```json
{
  "schemaVersion": 1,
  "mobs": [
    { "id": "minecraft:zombie", "categories": ["undead", "hostile"] },
    { "id": "examplemod:crystal_beast", "categories": ["magik", "boss"] }
  ]
}
```

Questo è un esempio di schema, non un catalogo completo da sostituire al file del mondo. Conserva tutte le altre righe. I campi descrittivi, come `name`, `type`, `tameable`, `rideable` e `breedable`, sono accettati nel catalogo; la classificazione di gioco usa `id` e `categories`.

Quando `MobCategory.json` esiste, ha precedenza sul file storico `MobCategory.config`, il cui formato è `minecraft:zombie=undead,hostile`. Modificare soltanto il file `.config` può quindi non cambiare la classificazione attiva.

## Catalogo da foglio di calcolo

Il progetto include `doc/mob_categories.xlsx` e il convertitore `doc/excel_to_json.py`. Il convertitore controlla colonne, duplicati e valori booleani, poi esporta `doc/MobCategory.json`. Il file esportato va copiato nella cartella del mondo e ricaricato.

Varianti che condividono un tipo di registry condividono la stessa voce: una descrizione differente non crea un tipo di entità nuovo.

Fonte: [MobCategoryManager](https://github.com/GoldenRose01/enchlib/blob/main/src/main/kotlin/goldenrose01/enchlib/config/MobCategoryManager.kt), [catalogo](https://github.com/GoldenRose01/enchlib/blob/main/doc/README.md).
