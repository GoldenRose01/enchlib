# 📗 Commerci dei villager

[Home](Home) · [Configurazione](Configurazione) · [Novità 1.7.0](Novita-1.7.0)

Dalla **1.7.0** puoi scegliere, per ciascun incantesimo registrato, se consentirlo nei **libri incantati** e negli **oggetti incantati** venduti dai villager. Le scelte appartengono al mondo e vengono sincronizzate dal server.

## Modifica dal menu

1. Apri un mondo e la configurazione di EnchLib da Mod Menu.
2. Con **Cloth Config**, apri la scheda **Villager trades**. Con **YACL**, apri **Enchant Detail → Villager trades**.
3. Espandi l'incantesimo e modifica **Enchanted books** o **Enchanted items**.
4. Salva le modifiche. In multiplayer servono i permessi di modifica della configurazione, normalmente operatore di livello 2.

Il reset ripristina il valore predefinito ricavato dal tag, anziché consentire indiscriminatamente tutti gli incantesimi.

## Valori predefiniti

| Incantesimo | Libri | Oggetti |
|---|---|---|
| Nel tag `minecraft:curse` | Escluso | Escluso |
| Non nel tag `minecraft:curse` | Consentito | Consentito |

Il controllo usa il tag del registry corrente, non il nome dell'incantesimo. Comprende quindi le maledizioni vanilla e quelle aggiunte da mod o datapack che appartengono a `minecraft:curse`. Il tag corretto è `curse`, non `course`.

Puoi consentire esplicitamente una maledizione. Un incantesimo disabilitato globalmente nella disponibilità di EnchLib resta comunque escluso dai commerci.

## Configurazione JSON5

I campi si trovano nella voce dell'incantesimo in `<mondo>/config/enchlib/EnchantmentsDetails.json5`:

```json5
{
  "enchantments": [
    {
      "id": "minecraft:binding_curse",
      "villager_books": true,
      "villager_items": false
    }
  ]
}
```

Questo è un frammento di esempio: integra i due campi nella voce esistente, preservando gli altri valori e gli altri incantesimi.

| Valore del campo | Significato |
|---|---|
| Assente o `null` | Segue il preset del tag |
| `true` | Consente esplicitamente quel tipo di commercio |
| `false` | Esclude esplicitamente quel tipo di commercio |

Le configurazioni della 1.6.1 senza questi campi applicano automaticamente i nuovi valori predefiniti. Dopo una modifica manuale usa `/plusec-debug reload`.

## Effetto sulle offerte

Il filtro esamina il risultato di una **nuova offerta**. Se contiene un incantesimo escluso, l'intera offerta viene scartata: non viene trasformata in un libro vuoto o in un oggetto privo dell'incantesimo. Questo può ridurre il numero di offerte disponibili.

Le offerte già salvate e gli oggetti già acquistati restano invariati. Le opzioni non modificano bottini o tavoli di incantamento. Consentire un incantesimo non lo aggiunge al gruppo dei possibili risultati: deve già essere previsto dalla generazione del gioco, della mod o del datapack.

Il filtro copre le offerte generate dal percorso standard `VillagerTrade.getOffer`. I sistemi di commercio esterni che lo sostituiscono devono integrare le regole; consulta l'[API](API).
