#!/usr/bin/env python3
"""Verify a *real* Gradle-built NeoForge JAR; never creates or fakes a JAR."""
from pathlib import Path
from zipfile import ZipFile, BadZipFile
import sys

root = Path(__file__).resolve().parent.parent
path = Path(sys.argv[1]) if len(sys.argv) == 2 else None
if path is None:
    candidates = [
        p for p in (root / 'build/libs').glob('SurvivorDogTags-*.jar')
        if not p.name.endswith(('-sources.jar','-dev.jar', '-javadoc.jar'))
    ]
    if len(candidates) != 1:
        raise SystemExit(f'FAIL: expected exactly one compiled mod JAR, got {len(candidates)}')
    path = candidates[0]
if not path.exists() or path.stat().st_size < 1_000:
    raise SystemExit(f'FAIL: missing or unusually small JAR: {path}')
required = {
    'META-INF/neoforge.mods.toml',
    'tw/zonezero/survivordogtags/SurvivorDogTags.class',
    'tw/zonezero/survivordogtags/game/DogTagItem.class',
    'tw/zonezero/survivordogtags/workbench/DogTagWorkbenchMenu.class',
    'tw/zonezero/survivordogtags/game/DogTagCombat.class',
    'tw/zonezero/survivordogtags/game/DogTagDamageMath.class',
    'assets/survivordogtags/lang/zh_tw.json',
    'assets/survivordogtags/models/block/dog_tag_workbench.json',
    'assets/survivordogtags/blockstates/dog_tag_workbench.json',
    'data/survivordogtags/curios/entities/player.json',
    'data/curios/tags/item/necklace.json',
}
try:
    with ZipFile(path) as z:
        damaged = z.testzip()
        if damaged:
            raise SystemExit(f'FAIL: corrupt JAR member: {damaged}')
        missing = required.difference(z.namelist())
        if missing:
            raise SystemExit('FAIL: JAR missing mandatory classes or assets: ' + ', '.join(sorted(missing)))
        meta = z.read('META-INF/neoforge.mods.toml').decode('utf-8')
        if 'modId="survivordogtags"' not in meta or '${' in meta:
            raise SystemExit('FAIL: mod metadata missing or still contains unexpanded placeholders')
        for name in sorted(required):
            if name.endswith('.class'):
                bytecode = z.read(name)
                if len(bytecode) < 8 or bytecode[:4] != b'\xCA\xFE\xBA\xBE':
                    raise SystemExit(f'FAIL: entry is not JVM bytecode: {name}')
                if int.from_bytes(bytecode[6:8], 'big') != 65:
                    raise SystemExit(f'FAIL: expected Java 21 bytecode: {name}')
except (BadZipFile, UnicodeDecodeError) as err:
    raise SystemExit(f'FAIL: invalid Gradle JAR: {err}')
print(f'PASS: genuine JAR structure, compiled entrypoint, mandatory assets ({path.name})')
