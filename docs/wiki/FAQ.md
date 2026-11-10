# 💬 Problemi frequenti

[Home](Home) · [Comandi](Comandi) · [Configurazione](Configurazione)

## Ho cambiato un valore ma un tavolo non lo rispetta

Controlla che il tavolo registri e legga le regole di EnchLib. La libreria può conservare un’impostazione anche se una mod esterna non la usa. Verifica anche mondo attivo, disponibilità generale, compatibilità dell’oggetto e probabilità.

## Ho modificato la cartella config sbagliata

Esegui `/plusec-debug show-path`. La configurazione attiva è nel salvataggio; la vecchia cartella globale può essere stata usata soltanto per la migrazione iniziale.

## Il client annulla le mie modifiche

Il server è autorevole. Senza i permessi richiesti la modifica viene rifiutata e il server invia di nuovo i valori validi. Controlla i permessi di operatore del giocatore, non soltanto l’accesso ai file locali del client.

## Un ID compare nel preset ma non funziona

Esegui `validate` e controlla il registry della versione corrente. I file storici possono contenere nomi diversi, per esempio `curse_of_binding` al posto di `binding_curse`. Una riga di configurazione non registra un nuovo incantamento e non rinomina automaticamente un ID vanilla.

## Ho cambiato MobCategory.config senza risultato

Se esiste `MobCategory.json`, ha precedenza. Modifica il file attivo e ricarica il mondo con il comando dedicato.

## Aumentare max_level aumenta sempre l’effetto?

No. La mod che implementa l’effetto può limitare l’amplificatore, applicare un tetto o verificare soltanto se il livello è positivo. Il cap è una regola di applicazione, non una formula universale di potenza.

## Gli esempi JSON vanno incollati al posto di tutto il file?

No: descrivono una parte dello schema. Conserva le altre voci e modifica l’ID che ti interessa. Usa commenti soltanto nei file il cui parser li supporta.

## EnchLib richiede Eldrich Surge?

No. Sono progetti separati. Per avere gli incantamenti di Eldrich Surge serve la sua mod; per configurare contenuti riconosciuti da EnchLib non occorre necessariamente installarla.

## Perché una maledizione non compare più nei commerci?

Dalla 1.7.0 gli incantesimi con tag `minecraft:curse` sono esclusi dalle nuove offerte per impostazione predefinita. Vale anche per mod e datapack che usano questo tag. Puoi riabilitare separatamente libri e oggetti nel menu **Villager trades**; l'incantesimo deve anche essere abilitato globalmente.

## Ho disabilitato un incantesimo, ma un villager lo vende ancora

Il filtro agisce alla generazione di nuove offerte. Le offerte già salvate non vengono modificate; nemmeno gli oggetti già acquistati. Inoltre un sistema di commerci personalizzato che non usa il percorso standard deve integrare esplicitamente le regole.

## Perché vedo meno offerte, oppure un incantesimo abilitato non compare?

Un'offerta con un incantesimo escluso viene scartata interamente: possono quindi comparire meno offerte. Abilitare un incantesimo permette di mantenerlo quando viene generato, ma non lo aggiunge ai possibili risultati del commercio. Per dettagli consulta [Commerci dei villager](Commerci-dei-villager).
