# 🧩 API per sviluppatori

[Home](Home) · [Tavoli e rarità](Tavoli-e-rarita) · [Categorie mob](Categorie-mob)

La classe JVM pubblica è **`goldenrose01.enchlib.api.EnchantLibAPI`**. Il nome della classe contiene `EnchantLib`, anche se il prodotto si chiama EnchLib. I metodi annotati `@JvmStatic` sono richiamabili da Java.

## Integrazione opzionale

Compila contro l’API come dipendenza opzionale e mantieni il JAR separato. Usa `suggests` nei metadati Fabric quando l’integrazione è facoltativa. Controlla che `enchlib` sia caricato prima di invocare direttamente le sue classi; un piccolo ponte riflessivo può evitare un riferimento obbligatorio al runtime.

Non incorporare una copia dell’API dentro la tua mod: due runtime che scrivono gli stessi file non costituiscono un’integrazione.

## Registrare preset

```java
import goldenrose01.enchlib.api.EnchantLibAPI;
import java.util.List;
import java.util.Map;

EnchantLibAPI.registerEnchantments(
    Map.of("examplemod:arcane_edge", 3),
    Map.of("examplemod:arcane_edge", List.of("#minecraft:enchantable/weapon")),
    Map.of("examplemod:arcane_edge", List.of("undead"))
);
```

L’esempio è da inserire nel punto di inizializzazione della mod, non è una classe Java autonoma. Usa ID completi e registra i valori una volta disponibili i contenuti. I preset devono conservare le modifiche già scelte nel mondo.

## Leggere i valori nel punto di utilizzo

```java
String id = "examplemod:arcane_edge";
if (!EnchantLibAPI.isEnabled(id)) return;
Integer configured = EnchantLibAPI.getMaxLevel(id);
int cap = configured == null ? 3 : configured;
int applied = Math.min(requestedLevel, cap);
```

`isEnabled` restituisce `true` per un ID non ancora configurato. `getMaxLevel` restituisce `null` se manca una voce: conserva il tuo fallback. I setter scrivono immediatamente nel contesto attivo; un valore salvato influisce sul gameplay soltanto dove il sistema lo consulta.

## Superficie principale

| Obiettivo | Metodi |
|---|---|
| Preset incantamenti | `registerEnchantments`, `registerMaxLevelDefaults`, `registerRarityDefaults` |
| Disponibilità e massimo | `isEnabled`, `setEnabled`, `getMaxLevel`, `setMaxLevel` |
| Categorie oggetti | `getItemCategories`, `setItemCategories`, `getPresetItemCategories` |
| Rarità e incompatibilità | `getRarity`, `setRarity`, `getIncompatibleEnchantments`, `setIncompatibleEnchantments` |
| Tavoli | `registerEnchantingTable`, `registerEnchantingTableDefaults`, `getTableEnabled`, `getTableChance`, relativi setter, `resetTableRule` |
| Categorie mob | `registerMobCategories`, `hasMobCategory`, `getMobCategories`, `setMobCategories` |
| Ripristino | `resetEnchantmentDetails`, getter dei preset |

## Tavoli personalizzati

Registra un ID stabile del tavolo con nome, disponibilità iniziale e probabilità. Quando generi le offerte consulta entrambe le regole, tratta l’assenza come fallback e applica il peso della rarità se vuoi rispettare il modello dell’editor. Registrare il tavolo da solo non intercetta la sua interfaccia.

## Due classificazioni differenti

`registerEnchantments(..., targetMobCategories)` descrive quali categorie un incantamento bersaglia. `registerMobCategories(...)` assegna invece categorie ai tipi di entità. Non sono lo stesso mapping.

Fonte e dettagli degli overload: [EnchantLibAPI](https://github.com/GoldenRose01/enchlib/blob/main/src/main/kotlin/goldenrose01/enchlib/api/EnchLibAPI.kt). Approfondimento: [API_INTEGRATION.md](https://github.com/GoldenRose01/enchlib/blob/main/docs/API_INTEGRATION.md).
