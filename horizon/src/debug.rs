use mod_api::{Command, CommandArgumentValue, RegisterCommandsEvent, RunModCommandEvent};
use std::collections::HashMap;
use std::sync::Mutex;

#[derive(Default)]
pub struct DebugState {
    pub disabled: Mutex<HashMap<String, bool>>,
    pub max_levels: Mutex<HashMap<String, i32>>,
    pub mob_categories: Mutex<HashMap<String, Vec<String>>>,
}

pub fn register(event: &mut RegisterCommandsEvent<'_>) {
    for usage in [
        "plusec-debug list-enabled",
        "plusec-debug reload",
        "plusec-debug validate",
        "plusec-debug toggle <id:resource:minecraft:enchantment> <enabled:word>",
        "plusec-debug setmax <id:resource:minecraft:enchantment> <level:integer>",
        "plusec-debug mob get <entity:word>",
        "plusec-debug mob set <entity:word> <categories:word>",
    ] {
        event.commands.push(Command::op(usage, 2));
    }
}

pub fn handle(event: &RunModCommandEvent<'_>, state: &DebugState) -> Option<Result<String, String>> {
    let lits: Vec<&str> = event.command.literals.iter().map(String::as_str).collect();
    if lits.first().copied() != Some("plusec-debug") {
        return None;
    }
    let res = match lits.as_slice() {
        ["plusec-debug", "reload"] => Ok("Configurazione EnchLib ricaricata.".to_owned()),
        ["plusec-debug", "validate"] => {
            let disabled = state.disabled.lock().expect("disabled lock").len();
            let max_levels = state.max_levels.lock().expect("max_levels lock").len();
            let mobs = state.mob_categories.lock().expect("mob_categories lock").len();
            Ok(format!(
                "Configurazione valida: {disabled} override stato, {max_levels} max-level, {mobs} categorie mob."
            ))
        }
        ["plusec-debug", "list-enabled"] => {
            let guard = state.disabled.lock().expect("disabled lock");
            let off: Vec<&str> = guard
                .iter()
                .filter_map(|(k, v)| (!*v).then_some(k.as_str()))
                .collect();
            if off.is_empty() {
                Ok("Tutti gli incantesimi sono abilitati.".to_owned())
            } else {
                Ok(format!("Incantesimi disabilitati: {}", off.join(", ")))
            }
        }
        ["plusec-debug", "toggle"] => {
            let Some(CommandArgumentValue::Id(id)) = event.command.argument("id") else {
                return Some(Err("Specificare l'ID incantesimo.".into()));
            };
            let Some(CommandArgumentValue::String(raw)) = event.command.argument("enabled") else {
                return Some(Err("Specificare true o false.".into()));
            };
            let enabled = match raw.as_str() {
                "true" => true,
                "false" => false,
                _ => return Some(Err("Valore non valido: usa true o false.".into())),
            };
            state
                .disabled
                .lock()
                .expect("disabled lock")
                .insert(id.to_string(), enabled);
            Ok(format!("{id}: enabled = {enabled}."))
        }
        ["plusec-debug", "setmax"] => {
            let Some(CommandArgumentValue::Id(id)) = event.command.argument("id") else {
                return Some(Err("Specificare l'ID incantesimo.".into()));
            };
            let Some(CommandArgumentValue::Integer(level)) = event.command.argument("level") else {
                return Some(Err("Specificare il livello massimo.".into()));
            };
            if !(1..=255).contains(level) {
                return Some(Err("Il livello deve essere tra 1 e 255.".into()));
            }
            state
                .max_levels
                .lock()
                .expect("max_levels lock")
                .insert(id.to_string(), *level);
            Ok(format!("{id}: max-level = {level}."))
        }
        ["plusec-debug", "mob", "get"] => {
            let Some(CommandArgumentValue::String(entity)) = event.command.argument("entity") else {
                return Some(Err("Specificare l'ID entità.".into()));
            };
            let guard = state.mob_categories.lock().expect("mob_categories lock");
            let cats = guard.get(entity).map(|v| v.join(", ")).unwrap_or_default();
            Ok(format!(
                "{entity}: {}",
                if cats.is_empty() { "nessuna" } else { &cats }
            ))
        }
        ["plusec-debug", "mob", "set"] => {
            let Some(CommandArgumentValue::String(entity)) = event.command.argument("entity") else {
                return Some(Err("Specificare l'ID entità.".into()));
            };
            let Some(CommandArgumentValue::String(raw)) = event.command.argument("categories") else {
                return Some(Err("Specificare le categorie o 'none'.".into()));
            };
            let cats: Vec<String> = if raw.eq_ignore_ascii_case("none") {
                Vec::new()
            } else {
                raw.split(',')
                    .map(|s| s.trim().to_owned())
                    .filter(|s| !s.is_empty())
                    .collect()
            };
            let summary = if cats.is_empty() {
                "nessuna".to_owned()
            } else {
                cats.join(", ")
            };
            state
                .mob_categories
                .lock()
                .expect("mob_categories lock")
                .insert(entity.clone(), cats);
            Ok(format!("Categorie mob salvate per {entity}: {summary}."))
        }
        _ => Err("Sottocomando /plusec-debug sconosciuto.".into()),
    };
    Some(res)
}
