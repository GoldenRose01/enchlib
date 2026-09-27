#!/usr/bin/env python3
"""Convert the EnchLib mob catalog workbook to MobCategory.json."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from openpyxl import load_workbook

ROOT = Path(__file__).resolve().parent
DEFAULT_INPUT = ROOT / "mob_categories.xlsx"
DEFAULT_OUTPUT = ROOT / "MobCategory.json"
CATEGORY_COLUMNS = (
    "Animals", "Magik", "Undead", "Rebel", "Fungi", "Hell", "Void",
    "Water", "Arthropods", "Cubic", "Flying",
)


def as_bool(value: Any, *, cell: str) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, int) and value in (0, 1):
        return bool(value)
    if isinstance(value, str):
        normalized = value.strip().casefold()
        if normalized in {"true", "vero", "yes", "si", "1", "x"}:
            return True
        if normalized in {"false", "falso", "no", "0", ""}:
            return False
    raise ValueError(f"{cell}: atteso un valore booleano, trovato {value!r}")


def convert(source: Path, destination: Path) -> None:
    workbook = load_workbook(source, read_only=True, data_only=True)
    sheet = workbook["Mobs"] if "Mobs" in workbook.sheetnames else workbook.active
    rows = sheet.iter_rows(values_only=True)
    headers = [str(value).strip() if value is not None else "" for value in next(rows)]
    positions = {header.casefold(): index for index, header in enumerate(headers) if header}
    required = ["mob id", "mob name", "type", "tameable", "rideable", "breedable", *[c.casefold() for c in CATEGORY_COLUMNS]]
    missing = [name for name in required if name not in positions]
    if missing:
        raise ValueError("Colonne mancanti nel foglio Mobs: " + ", ".join(missing))

    mobs = []
    seen_ids: set[str] = set()
    for row_number, row in enumerate(rows, start=2):
        def get(column: str) -> Any:
            index = positions[column.casefold()]
            return row[index] if index < len(row) else None

        raw_id = get("Mob ID")
        raw_name = get("Mob name")
        if raw_id is None and raw_name is None:
            continue
        if not raw_id or not raw_name:
            raise ValueError(f"Riga {row_number}: Mob ID e Mob name sono obbligatori")
        mob_id, name = str(raw_id).strip(), str(raw_name).strip()
        if ":" not in mob_id:
            raise ValueError(f"Riga {row_number}: '{mob_id}' deve essere un ID namespaced, ad esempio minecraft:zombie")
        if mob_id in seen_ids:
            raise ValueError(f"Riga {row_number}: ID duplicato '{mob_id}'")
        seen_ids.add(mob_id)
        categories = [column.casefold() for column in CATEGORY_COLUMNS if as_bool(get(column), cell=f"{column}, riga {row_number}")]
        mobs.append({
            "id": mob_id,
            "name": name,
            "type": str(get("Type") or "").strip(),
            "tameable": as_bool(get("Tameable"), cell=f"Tameable, riga {row_number}"),
            "rideable": as_bool(get("Rideable"), cell=f"Rideable, riga {row_number}"),
            "breedable": as_bool(get("Breedable"), cell=f"Breedable, riga {row_number}"),
            "categories": categories,
        })
    workbook.close()
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps({"schemaVersion": 1, "minecraftVersion": "26.3", "mobs": mobs}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Esportati {len(mobs)} mob in {destination}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", nargs="?", type=Path, default=DEFAULT_INPUT, help="Workbook .xlsx (default: doc/mob_categories.xlsx)")
    parser.add_argument("output", nargs="?", type=Path, default=DEFAULT_OUTPUT, help="JSON result (default: doc/mob_categories.json)")
    args = parser.parse_args()
    convert(args.input, args.output)


if __name__ == "__main__":
    main()
