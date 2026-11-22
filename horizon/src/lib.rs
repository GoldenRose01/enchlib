mod debug;
mod gameset;

use mod_api::{
    Command, CommandArgumentValue, CommandInvocation, CommandSource, ComponentId, Compound, Flow,
    Hooks, ItemResource, RegisterCommandsEvent, RunModCommandEvent, ServerHost, Tag, Text,
};

const ACTIVE: &str = "minecraft:enchantments";
const STORED: &str = "minecraft:stored_enchantments";

#[derive(Default)]
struct EnchLib {
    debug: debug::DebugState,
    gameset: gameset::GameSetState,
}

#[mod_api::horizon_mod]
impl Hooks for EnchLib {
    fn register_commands(&self, _: &mut dyn ServerHost, event: &mut RegisterCommandsEvent<'_>) {
        for usage in [
            "plusec add <id:resource:minecraft:enchantment>",
            "plusec add <id:resource:minecraft:enchantment> <level:integer>",
            "plusec remove <id:resource:minecraft:enchantment>",
            "plusec info <id:resource:minecraft:enchantment>",
            "plusec list",
            "plusec clear",
            "plusec repair",
            "plusec setdurability <percent:integer>",
            "plusec damage <delta:word>",
        ] {
            event.commands.push(Command::op(usage, 2));
        }
        debug::register(event);
        gameset::register(event);
    }

    fn run_mod_command(
        &self,
        host: &mut dyn ServerHost,
        event: &mut RunModCommandEvent<'_>,
    ) -> Flow {
        if let Some(res) = debug::handle(event, &self.debug) {
            let _ = host
                .commands()
                .feedback(Text::literal(res.unwrap_or_else(|e| e)));
            return Flow::Cancel;
        }
        if let Some(msg) = gameset::handle(event, &self.gameset) {
            let _ = host.commands().feedback(Text::literal(msg));
            return Flow::Cancel;
        }
        let [root, action] = event.command.literals.as_slice() else {
            return Flow::Continue;
        };
        if root != "plusec" {
            return Flow::Continue;
        }
        let result = run(host, event.source, event.command, action);
        let _ = host
            .commands()
            .feedback(Text::literal(result.unwrap_or_else(|error| error)));
        Flow::Cancel
    }
}

fn run(
    host: &mut dyn ServerHost,
    source: &CommandSource,
    command: &CommandInvocation,
    action: &str,
) -> Result<String, String> {
    let enchantment = match command.argument("id") {
        Some(CommandArgumentValue::Id(id)) => Some(id.to_string()),
        _ => None,
    };
    let CommandSource::Player { player, .. } = source else {
        return Err("Questo comando richiede un giocatore.".into());
    };
    let slot = player
        .selected_slot(host)
        .ok_or("Slot attivo indisponibile.")? as u32;
    let mut stack = player
        .inventory_slots(host)
        .and_then(|slots| slots.into_iter().find(|(index, _)| *index == slot))
        .map(|(_, stack)| stack)
        .ok_or("Tieni un oggetto nella mano principale.")?;

    let result = if matches!(action, "repair" | "setdurability" | "damage") {
        let item = ItemResource::of(&stack);
        if !item.damageable(host) {
            return Err("L'oggetto non ha durabilità.".into());
        }
        let maximum_id = ComponentId::new("minecraft:max_damage").expect("vanilla component ID");
        let maximum = item
            .value(host, &maximum_id)
            .as_ref()
            .map(level_number)
            .filter(|value| *value > 0)
            .ok_or("Durabilità massima indisponibile.")?;
        let (damage, message) = match action {
            "repair" => (0, "Oggetto riparato.".to_owned()),
            "setdurability" => {
                let Some(CommandArgumentValue::Integer(percent)) = command.argument("percent")
                else {
                    return Err("Specificare la percentuale residua.".into());
                };
                if !(0..=100).contains(percent) {
                    return Err("La percentuale deve essere compresa tra 0 e 100.".into());
                }
                let damage = (maximum - maximum * i64::from(*percent) / 100).min(maximum - 1);
                (damage, format!("Durabilità impostata al {percent}%."))
            }
            _ => {
                let Some(CommandArgumentValue::String(delta)) = command.argument("delta") else {
                    return Err("Specificare la variazione con segno.".into());
                };
                let points = delta
                    .strip_prefix(['+', '-'])
                    .filter(|value| value.bytes().all(|ch| ch.is_ascii_digit()))
                    .and_then(|_| delta.parse::<i32>().ok())
                    .filter(|points| *points != 0)
                    .ok_or("Indica un intero con segno diverso da zero, per esempio -20 o +20.")?;
                let damage = (i64::from(stack.damage()) - i64::from(points)).clamp(0, maximum - 1);
                (damage, format!("Danno impostato a {damage} su {maximum}."))
            }
        };
        stack.set_damage(u32::try_from(damage).map_err(|_| "Durabilità fuori intervallo.")?);
        message
    } else {
        let component = if stack.item().id(host) == "minecraft:enchanted_book" {
            STORED
        } else {
            ACTIVE
        };
        let component_key = component.into();
        let mut patch: Compound = stack.components().map(Compound::from).unwrap_or_default();
        let mut levels = match patch.get(&component_key) {
            Some(Tag::Compound(values)) => values.clone(),
            Some(_) => return Err("Formato del componente incantesimi non valido.".into()),
            None => Compound::new(),
        };

        let message = match action {
            "info" => {
                let id = enchantment.ok_or("Specificare un incantesimo.")?;
                let key = id.as_str().into();
                let rank = levels.get(&key).map(level_number).unwrap_or(0);
                return Ok(format!("{id}: livello sull'oggetto in mano {rank}."));
            }
            "list" => {
                let list = levels
                    .into_iter()
                    .map(|(id, rank)| format!("{} {}", id.to_string_lossy(), level_number(&rank)))
                    .collect::<Vec<_>>();
                return Ok(if list.is_empty() {
                    "Nessun incantesimo.".to_owned()
                } else {
                    list.join(", ")
                });
            }
            "add" => {
                let id = enchantment.ok_or("Specificare un incantesimo.")?;
                let level = match command.argument("level") {
                    Some(CommandArgumentValue::Integer(level)) if (1..=255).contains(level) => {
                        *level
                    }
                    None => 1,
                    _ => return Err("Il livello deve essere compreso tra 1 e 255.".into()),
                };
                levels.insert(id.as_str().into(), Tag::Int(level));
                format!("Applicato {id} al livello {level}.")
            }
            "remove" => {
                let id = enchantment.ok_or("Specificare un incantesimo.")?;
                let key = id.as_str().into();
                if levels.remove(&key).is_none() {
                    return Err(format!("Incantesimo assente: {id}."));
                }
                format!("Rimosso {id}.")
            }
            "clear" => {
                levels.clear();
                "Rimossi tutti gli incantesimi.".to_owned()
            }
            _ => return Err("Sottocomando sconosciuto.".into()),
        };
        patch.remove(&format!("!{component}").as_str().into());
        patch.insert(component_key, Tag::Compound(levels));
        stack
            .set_components(patch)
            .map_err(|error| error.to_string())?;
        message
    };
    player
        .set_inventory_slot(host, slot, Some(stack))
        .map_err(|error| format!("Modifica rifiutata da Horizon: {error:?}"))?;
    Ok(result)
}

fn level_number(rank: &Tag) -> i64 {
    match rank {
        Tag::Byte(n) => i64::from(*n),
        Tag::Short(n) => i64::from(*n),
        Tag::Int(n) => i64::from(*n),
        Tag::Long(n) => *n,
        _ => 0,
    }
}
