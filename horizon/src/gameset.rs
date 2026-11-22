use mod_api::{Command, RegisterCommandsEvent, RunModCommandEvent};
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Mutex;

#[derive(Clone, Copy, PartialEq, Eq)]
pub enum TreeCutMode {
    Off,
    Fell,
    Chop,
}

impl TreeCutMode {
    pub fn as_str(self) -> &'static str {
        match self {
            Self::Off => "off",
            Self::Fell => "fell",
            Self::Chop => "chop",
        }
    }

    pub fn parse(s: &str) -> Option<Self> {
        match s {
            "off" => Some(Self::Off),
            "fell" => Some(Self::Fell),
            "chop" => Some(Self::Chop),
            _ => None,
        }
    }
}

pub struct GameSetState {
    pub treecut: Mutex<TreeCutMode>,
    pub axes_are_weapons: AtomicBool,
}

impl Default for GameSetState {
    fn default() -> Self {
        Self {
            treecut: Mutex::new(TreeCutMode::Fell),
            axes_are_weapons: AtomicBool::new(false),
        }
    }
}

pub fn register(event: &mut RegisterCommandsEvent<'_>) {
    for usage in [
        "gameset treecut",
        "gameset treecut off",
        "gameset treecut fell",
        "gameset treecut chop",
        "gameset axesareweapons",
        "gameset axesareweapons true",
        "gameset axesareweapons false",
    ] {
        event.commands.push(Command::op(usage, 2));
    }
}

pub fn handle(event: &RunModCommandEvent<'_>, state: &GameSetState) -> Option<String> {
    match event.command.literals.as_slice() {
        [root, sub] if root == "gameset" && sub == "treecut" => {
            let current = state.treecut.lock().expect("treecut lock").as_str();
            Some(format!("Modalità taglio alberi attuale: {current}."))
        }
        [root, sub, mode_str] if root == "gameset" && sub == "treecut" => {
            let mode = TreeCutMode::parse(mode_str)?;
            *state.treecut.lock().expect("treecut lock") = mode;
            Some(format!("Modalità taglio alberi impostata su: {}.", mode.as_str()))
        }
        [root, sub] if root == "gameset" && sub == "axesareweapons" => {
            let current = state.axes_are_weapons.load(Ordering::Relaxed);
            Some(format!("axesAreWeapons attuale: {current}."))
        }
        [root, sub, val] if root == "gameset" && sub == "axesareweapons" => {
            let enabled = match val.as_str() {
                "true" => true,
                "false" => false,
                _ => return Some("Valore non valido: usa true o false.".to_owned()),
            };
            state.axes_are_weapons.store(enabled, Ordering::Relaxed);
            Some(format!("axesAreWeapons impostato su: {enabled}."))
        }
        _ => None,
    }
}
