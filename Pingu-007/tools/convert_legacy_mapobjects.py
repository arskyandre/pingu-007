#!/usr/bin/env python3
"""Convert legacy multi-layer scenery tiles into Tiled tile objects.

By default this is a dry run. Use --output to write a converted copy or
--in-place to replace the input after creating a .bak file.
"""

from __future__ import annotations

import argparse
import copy
import json
import shutil
import sys
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Pattern:
    name: str
    tileset: str
    local_id: int
    width: int
    height: int
    # (layer name, x offset, y offset, expected gid)
    cells: tuple[tuple[str, int, int, int], ...]
    # Cell used as the object's top-left origin.
    anchor_layer: str
    anchor_gid: int
    # Optional top-row offset from the anchor. Needed when a sprite has a
    # transparent upper tile that was never painted into the legacy top layer.
    object_top_dy: int | None = None


PATTERNS = (
    Pattern("large tree", "images/arvore.tsx", 0, 32, 47, (
        ("tTree", 0, -2, 93), ("tTree", 1, -2, 94),
        ("tTree", 0, -1, 107), ("tTree", 1, -1, 108),
        ("bTree", 0, 0, 121), ("bTree", 1, 0, 122),
    ), "bTree", 121),
    Pattern("small tree", "images/arvore.tsx", 1, 32, 47, (
        ("tTree", 0, -1, 109), ("tTree", 1, -1, 110),
        ("bTree", 0, 0, 123), ("bTree", 1, 0, 124),
    ), "bTree", 123),
    Pattern("side igloo", "images/iglus.tsx", 0, 48, 32, (
        ("tIglu", 0, -1, 40), ("tIglu", 1, -1, 41), ("tIglu", 2, -1, 42),
        ("bIglu", 0, 0, 54), ("bIglu", 1, 0, 55), ("bIglu", 2, 0, 56),
    ), "bIglu", 54),
    Pattern("front igloo", "images/iglus.tsx", 1, 48, 32, (
        ("tIglu", 0, -1, 68), ("tIglu", 1, -1, 69), ("tIglu", 2, -1, 70),
        ("bIglu", 0, 0, 82), ("bIglu", 1, 0, 83), ("bIglu", 2, 0, 84),
    ), "bIglu", 82),
    Pattern("small stone", "images/stone1.tsx", 0, 16, 31, (
        ("bStone", 0, 0, 48),
    ), "bStone", 48, -1),
    Pattern("tall stone", "images/stone1.tsx", 1, 16, 31, (
        ("tStone", 0, -1, 35), ("bStone", 0, 0, 49),
    ), "bStone", 49),
    Pattern("wide stone", "images/stone2.tsx", 0, 32, 32, (
        ("bStone", 0, 0, 46), ("bStone", 1, 0, 47),
    ), "bStone", 46, -1),
    Pattern("block stone", "images/stone2.tsx", 1, 32, 32, (
        ("tStone", 0, -1, 36), ("tStone", 1, -1, 37),
        ("bStone", 0, 0, 50), ("bStone", 1, 0, 51),
    ), "bStone", 50),
)

FENCE_LOCAL_IDS = {7: 0, 8: 1, 9: 2, 21: 3, 22: 4, 23: 5}
TILESET_COUNTS = {
    "images/tile_set_pingu.tsx": 126,
    "images/iglus.tsx": 2,
    "images/portao.tsx": 2,
    "images/arvore.tsx": 2,
    "images/loja_pescador.tsx": 1,
    "images/stone1.tsx": 2,
    "images/stone2.tsx": 2,
    "images/fence.tsx": 6,
}


def properties(kind: str) -> list[dict]:
    values = {
        "acao": ("string", "null"),
        "colisao": ("bool", True),
        "isActive": ("bool", True),
        "isInteractive": ("bool", False),
        "isTransparent": ("bool", False),
        "type": ("string", "map_object"),
    }
    if "igloo" in kind:
        values["acao"] = ("string", "none")
    return [
        {"name": name, "type": value_type, "value": value}
        for name, (value_type, value) in values.items()
    ]


def ensure_tileset(level: dict, source: str) -> int:
    for entry in level["tilesets"]:
        if entry.get("source") == source:
            return entry["firstgid"]

    highest_end = 1
    for entry in level["tilesets"]:
        count = TILESET_COUNTS.get(entry.get("source"))
        if count is None:
            raise ValueError(
                f"Cannot determine tile count for existing tileset {entry.get('source')!r}"
            )
        highest_end = max(highest_end, entry["firstgid"] + count)
    level["tilesets"].append({"firstgid": highest_end, "source": source})
    level["tilesets"].sort(key=lambda item: item["firstgid"])
    return highest_end


def tile_object(object_id: int, gid: int, x: int, bottom_y: int,
                width: int, height: int, kind: str) -> dict:
    # Tiled stores a tile object's y at its bottom edge.
    return {
        "gid": gid,
        "height": height,
        "id": object_id,
        "name": "",
        "properties": properties(kind),
        "rotation": 0,
        "type": "",
        "visible": True,
        "width": width,
        "x": x,
        "y": bottom_y,
    }


def convert(level: dict) -> tuple[dict[str, int], list[str]]:
    width = level["width"]
    tile_w = level["tilewidth"]
    tile_h = level["tileheight"]
    layers = {layer["name"]: layer for layer in level["layers"]}
    required = {cell[0] for pattern in PATTERNS for cell in pattern.cells} | {"fence"}
    missing = sorted(required - layers.keys())
    if missing:
        raise ValueError(f"Missing legacy layers: {', '.join(missing)}")

    object_layer = layers.get("Paredes")
    if not object_layer or object_layer.get("type") != "objectgroup":
        raise ValueError("Expected an object layer named 'Paredes'")

    next_id = max(
        level.get("nextobjectid", 1),
        1 + max((obj.get("id", 0) for layer in level["layers"]
                 for obj in layer.get("objects", [])), default=0),
    )
    counts: dict[str, int] = {}
    warnings: list[str] = []

    for pattern in PATTERNS:
        anchor_data = layers[pattern.anchor_layer]["data"]
        anchors = [i for i, gid in enumerate(anchor_data) if gid == pattern.anchor_gid]
        firstgid = ensure_tileset(level, pattern.tileset)
        converted = 0
        for index in anchors:
            col, row = index % width, index // width
            matched: list[tuple[list[int], int]] = []
            valid = True
            occluded = 0
            for layer_name, dx, dy, expected in pattern.cells:
                x, y = col + dx, row + dy
                if not (0 <= x < width and 0 <= y < level["height"]):
                    valid = False
                    break
                data = layers[layer_name]["data"]
                cell_index = y * width + x
                if data[cell_index] == expected:
                    matched.append((data, cell_index))
                elif layer_name == pattern.anchor_layer and dy == 0:
                    # The collision/bottom row uniquely identifies an instance.
                    valid = False
                    break
                else:
                    # Legacy sprites can overlap and overwrite one another's top tiles.
                    occluded += 1
            if not valid:
                warnings.append(f"Incomplete {pattern.name} near tile ({col}, {row}); left unchanged")
                continue

            for data, cell_index in matched:
                data[cell_index] = 0
            top_dy = (pattern.object_top_dy if pattern.object_top_dy is not None
                      else min(cell[2] for cell in pattern.cells))
            top_row = row + top_dy
            object_layer["objects"].append(tile_object(
                next_id, firstgid + pattern.local_id,
                col * tile_w, top_row * tile_h + pattern.height,
                pattern.width, pattern.height, pattern.name,
            ))
            next_id += 1
            converted += 1
            if occluded:
                warnings.append(
                    f"Recovered overlapping {pattern.name} near tile ({col}, {row}); "
                    f"{occluded} visual cell(s) had been overwritten"
                )
        counts[pattern.name] = converted

    fence = layers["fence"]["data"]
    fence_firstgid = ensure_tileset(level, "images/fence.tsx")
    fence_count = 0
    for index, legacy_gid in enumerate(fence):
        local_id = FENCE_LOCAL_IDS.get(legacy_gid)
        if local_id is None:
            continue
        col, row = index % width, index // width
        fence[index] = 0
        object_layer["objects"].append(tile_object(
            next_id, fence_firstgid + local_id,
            col * tile_w, (row + 1) * tile_h,
            tile_w, tile_h, "fence",
        ))
        next_id += 1
        fence_count += 1
    counts["fence pieces"] = fence_count

    # Atlas IDs 12-14/26-28 describe an older igloo that is absent from iglus.png.
    old_igloos = sum(1 for gid in layers["bIglu"]["data"] if gid == 26)
    if old_igloos:
        warnings.append(
            f"Left {old_igloos} old right-door igloo(s) unchanged: no equivalent MapObject asset exists"
        )

    level["nextobjectid"] = next_id
    return counts, warnings


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("map", type=Path, help="Tiled JSON/TMJ map to inspect")
    destination = parser.add_mutually_exclusive_group()
    destination.add_argument("--output", type=Path, help="write a converted copy")
    destination.add_argument("--in-place", action="store_true", help="replace the input and create MAP.bak")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    with args.map.open("r", encoding="utf-8") as handle:
        level = json.load(handle)
    converted = copy.deepcopy(level)
    counts, warnings = convert(converted)

    print("Conversion plan:")
    for name, count in counts.items():
        print(f"  {name}: {count}")
    for warning in warnings:
        print(f"WARNING: {warning}", file=sys.stderr)

    output = args.output
    if args.in_place:
        backup = args.map.with_suffix(args.map.suffix + ".bak")
        if backup.exists():
            raise FileExistsError(f"Refusing to overwrite existing backup: {backup}")
        shutil.copy2(args.map, backup)
        output = args.map
        print(f"Backup: {backup}")

    if output:
        with output.open("w", encoding="utf-8", newline="\n") as handle:
            # Compact JSON keeps large uncompressed tile arrays reasonably small.
            json.dump(converted, handle, ensure_ascii=False, separators=(",", ":"))
            handle.write("\n")
        print(f"Written: {output}")
    else:
        print("Dry run only; pass --output FILE or --in-place to write changes.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
