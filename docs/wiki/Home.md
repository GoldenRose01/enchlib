![EnchLib — Regole del mondo](https://raw.githubusercontent.com/wiki/GoldenRose01/enchlib/assets/banner.svg)

**Minecraft 26.3 · Fabric · EnchLib 2.0.0**

**EnchLib** è il centro di controllo per incantamenti, tavoli da incantamento, commerci dei villager, categorie mob e meccaniche di gameplay. Ogni impostazione appartiene al singolo mondo; il server la salva, la valida e la sincronizza automaticamente ai client.

## Guida rapida per i giocatori

1. **Installa** EnchLib insieme a Fabric Loader, Fabric API e Fabric Language Kotlin (opzionali: Mod Menu + Cloth Config o YACL per l'editor grafico).
2. **Apri un mondo**: EnchLib genera automaticamente `<mondo>/config/enchlib/` con tutti i preset predefiniti.
3. **Personalizza**: apri l'editor da Mod Menu oppure usa i comandi `/plusec` e `/plusec-debug`.

### Categorie nell'editor grafico (2.0.0)
L'interfaccia Cloth Config è organizzata in 9 tab chiare, con incantesimi e mob raggruppati per mod/namespace:
- **Available**: abilita o disabilita singoli incantesimi nel mondo.
- **Max Levels**: imposta il livello massimo (1–255) per ogni incantesimo.
- **Rarity**: regola la rarità (`common`, `uncommon`, `rare`, `very_rare`).
- **Tables**: gestisce l'abilitazione e la probabilità (%) per ogni tavolo (Vanilla e moddati come *Advanced Enchanting Table*).
- **Villagers**: controlla separatamente i libri incantati e gli oggetti incantati venduti dai villager.
- **Items**: definisce le categorie di oggetti compatibili.
- **Incompatibilities**: gestisce gli incantesimi mutualmente esclusivi.
- **Mobs**: assegna i mob alle categorie (`undead`, `arthropods`, `hell`, `void`, `water`, ecc.).
- **Gameplay**: configura **Tree Cutting** (`off`, `fell`, `chop`) e **Axes as Weapons**.

| 🎮 Giocatori e amministratori | 🧩 Sviluppatori |
|---|---|
| [Installazione](Installazione) | [API e integrazione](API) |
| [Configurazione JSON5](Configurazione) | [Regole dei tavoli e rarità](Tavoli-e-rarita) |
| [Commerci dei villager](Commerci-dei-villager) | [Integrazione Eldrich Surge 2.0.0](Eldrich-Surge) |
| [Catalogo dei preset degli incantamenti](Incantamenti) | [Fonti e manutenzione](Fonti) |
| [Categorie dei mob](Categorie-mob) · [Comandi](Comandi) · [Problemi frequenti](FAQ) | [Release 2.0.0](https://github.com/GoldenRose01/enchlib/blob/main/docs/releases/v2.0.0.md) |

---

[Repository](https://github.com/GoldenRose01/enchlib) · [Segnala un problema](https://github.com/GoldenRose01/enchlib/issues) · [Minecraft Wiki: incantamenti](https://minecraft.wiki/w/Enchantment)
