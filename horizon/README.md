# EnchLib per Horizon

Anteprima server-side per Horizon Nightly 26.3 del 22 novembre 2026, build `2b77287`. Copia il file `.hmod` in `horizon-mods/` e aggiungi `mod.enchlib:true` a `horizon-mods.txt`, accanto alla cartella.

Gli operatori di livello 2 possono usare `/plusec add <id> [livello]`, `remove <id>`, `list`, `clear`, `info <id>` `repair`, `setdurability <percentuale>` e `damage <+/-punti>` sull'oggetto nella mano principale. `add` accetta livelli da 1 a 255. Sui libri incantati, i comandi modificano il componente degli incantesimi conservati.

Configurazioni JSON5 per mondo, schermate, API per altre mod e comandi `/plusec-debug` restano da portare.

## Build

Da `horizon/`, esegui `cargo horizon-mod build --wasm` e `cargo horizon-mod check target/horizon-mod/enchlib.hmod`.
