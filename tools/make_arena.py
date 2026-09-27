"""Writes the empty GameTest arena structures: the 9x7x9 arena, and the 31x24x31 sky arena that a flying
Pegasus needs. Run from the repo root."""
import gzip
import struct


def name(s):
    b = s.encode()
    return struct.pack(">H", len(b)) + b


def tag(tag_id, key, payload):
    return bytes([tag_id]) + name(key) + payload


def int_list(values):
    return bytes([3]) + struct.pack(">i", len(values)) + b"".join(struct.pack(">i", v) for v in values)


def compound_list(items):
    return bytes([10]) + struct.pack(">i", len(items)) + b"".join(items)


air = tag(8, "Name", name("minecraft:air")) + b"\x00"


def write(path, size):
    root = (tag(3, "DataVersion", struct.pack(">i", 3955))
            + tag(9, "size", int_list(size))
            + tag(9, "palette", compound_list([air]))
            + tag(9, "blocks", compound_list([]))
            + tag(9, "entities", compound_list([]))
            + b"\x00")
    with gzip.open(path, "wb") as f:
        f.write(b"\x0a" + name("") + root)


write("src/main/resources/data/wildspellmobs/structure/arena.nbt", [9, 7, 9])
write("src/main/resources/data/wildspellmobs/structure/sky_arena.nbt", [31, 24, 31])
print("arenas written")
