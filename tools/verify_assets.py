#!/usr/bin/env python3
"""Validates packed static resources; does not replace Gradle / Minecraft runtime tests."""
from pathlib import Path
import json, re, struct, sys

root = Path(__file__).resolve().parent.parent
res = root / 'src/main/resources'
props = (root / 'gradle.properties').read_text()
assert re.search(r'(?m)^mod_version=\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?
assert 'minecraft_version=1.21.1' in props
assert 'neo_version=' in props
for file in res.rglob('*.json'):
    with file.open(encoding='utf-8') as f:
        json.load(f)
all_en = json.loads((res / 'assets/survivordogtags/lang/en_us.json').read_text())
all_zh = json.loads((res / 'assets/survivordogtags/lang/zh_tw.json').read_text())
assert set(all_zh) == set(all_en), f'Inconsistent translation keys: {set(all_zh)^set(all_en)}'
assert len(all_zh) >= 20
for path in res.rglob('*.png'):
    bs = path.read_bytes()
    assert bs.startswith(b'\x89PNG\r\n\x1a\n'), f'Not a PNG: {path}'
    width, height = struct.unpack('>II', bs[16:24]);assert 16 <= width <= 256 and 16 <= height <= 256
costs = (root/'src/main/java/tw/zonezero/survivordogtags/game/DogTagUpgradePlan.java').read_text()
for snippet in ['new Need(Material.IRON, 1), new Need(Material.COPPER, 1)',
                'new Need(Material.IRON, 40)',
                'new Need(Material.IRON, 80), new Need(Material.ALUMINUM, 40)',
                'new Need(Material.GOLD, 40), new Need(Material.ALUMINUM, 80)',
                'new Need(Material.COBALT, 6), new Need(Material.GOLD, 4)',
                'new Need(Material.TUNGSTEN, 80), new Need(Material.COBALT, 80)']:
    assert snippet in costs, f'Cost table missing: {snippet}'
modtoml = (root/'src/main/templates/META-INF/neoforge.mods.toml').read_text()
for dependency in ['modId="curios"','modId="tacz"','modId="exposure"']:
    assert dependency in modtoml
assert (res/'data/survivordogtags/curios/entities/player.json').exists(), 'Player necklace slot must be declared'
slot_config = json.loads((res/'data/survivordogtags/curios/entities/player.json').read_text())
assert 'necklace' in slot_config['slots'] and ('minecraft:player' in slot_config['entities'] or 'player' in slot_config['entities'])
assert (root/'tests/PaymentPlannerSmokeTest.java').exists(), 'Missing atomic payment regression test'
assert (root/'tests/PaymentPlannerFuzzTest.java').exists(), 'Missing cross-tag allocation fuzz test'
assert (root/'tests/ProfileSafetySmokeTest.java').exists(), 'Missing identity data safety regression test'
assert (root/'tests/KillMathSmokeTest.java').exists(), 'Missing historical kill migration regression test'
assert (root/'tests/ShotLedgerSmokeTest.java').exists(), 'Missing per-shot deduplication regression test'
ledger = (root/'src/main/java/tw/zonezero/survivordogtags/game/DogTagShotLedger.java').read_text()
assert 'beginShot(' in ledger and 'markHit(' in ledger and 'currentShot(' in ledger
assert 'now == fired.tick' in ledger, 'Shot token must not be inherited after its creation tick' 
combat = (root/'src/main/java/tw/zonezero/survivordogtags/game/DogTagCombat.java').read_text()
for event_hook in ['onGunFire(', 'onProjectileJoin(', 'onTaCZBullet(']:
    assert event_hook in combat
for material in ['forge:ingots/aluminum', 'forge:ingots/cobalt', 'forge:ingots/tungsten']:
    assert material in (root/'src/main/java/tw/zonezero/survivordogtags/workbench/DogTagMaterials.java').read_text()
assert 'shooter.server.getTickCount()' in combat, 'Bullet grouping must use server-global tick count'
assert 'if (shotId == 0L)' in combat and 'return;' in combat, 'Unknown projectiles must not bypass shotgun safety'
assert 'index == RESULT_SLOT' in (root/'src/main/java/tw/zonezero/survivordogtags/workbench/DogTagWorkbenchMenu.java').read_text(), 'Must reject shift-click result transfers'
assert 'DogTagShiftRouting.destination(' in (root/'src/main/java/tw/zonezero/survivordogtags/workbench/DogTagWorkbenchMenu.java').read_text(), 'Missing validated 4-slot quick-transfer routing'
assert 'vanillaZombieKills' in (root/'src/main/java/tw/zonezero/survivordogtags/game/DogTagTracker.java').read_text(), 'Missing vanilla stats migration'
assert (root/'tests/DamageMathSmokeTest.java').exists(), 'Missing fixed TaCZ bonus regression test'
block_code = (root/'src/main/java/tw/zonezero/survivordogtags/workbench/DogTagWorkbenchBlock.java').read_text()
assert 'protected ItemInteractionResult useItemOn(' in block_code, 'Holding Exposure photos should not block bench interaction'
assert 'protected VoxelShape getShape(' in block_code and 'rotateShape(' in block_code, 'Four-sided 3D workbench needs matching collision'
assert (root/'tests/ShiftRoutingSmokeTest.java').exists(), 'Missing quick-transfer bounds test'
# A facing-aware, non-cubic workbench must have all horizontal blockstates.
workbench_states = json.loads((res/'assets/survivordogtags/blockstates/dog_tag_workbench.json').read_text())
assert set(workbench_states['variants']) == {'facing=north', 'facing=south', 'facing=east', 'facing=west'}
assert {v.get('y', 0) for v in workbench_states['variants'].values()} == {0, 90, 180, 270}
workbench_model = json.loads((res/'assets/survivordogtags/models/block/dog_tag_workbench.json').read_text())
assert len(workbench_model['elements']) >= 6, 'The dog-tag bench requires raised metalworking details'
for element in workbench_model['elements']:
    start, end = element['from'], element['to']
    assert all(0 <= a < b <= 16 for a,b in zip(start,end)), f'Out-of-bounds 3D bench element: {element}'
    assert all(face['texture'].lstrip('#') in workbench_model['textures'] for face in element['faces'].values())
profile_code = (root/'src/main/java/tw/zonezero/survivordogtags/client/DogTagProfileScreen.java').read_text()
assert 'new RemotePlayer(' in profile_code, 'Offline/distant owners need a visible 3D model fallback'
photo_code = (root/'src/main/java/tw/zonezero/survivordogtags/client/ExposurePhotoRenderer.java').read_text()
assert 'rejectedPhoto = photo.copyWithCount(1)' in photo_code and 'found = false;' not in photo_code.split('catch (ReflectiveOperationException | LinkageError | RuntimeException ex)')[1].split('finally')[0], 'One damaged photo must not disable all Exposure photos'
menu_code = (root/'src/main/java/tw/zonezero/survivordogtags/workbench/DogTagWorkbenchMenu.java').read_text()
assert 'expectedMaterials' in menu_code and 'expectedOutput' in menu_code, 'Server must recheck the entire preview transaction'
print('PASS: JSON/localization/PNG, 6 costs, 3 shot hooks, held-item bench open, rotated collision and transaction guards')
, props), 'Missing/invalid semantic mod version'
assert 'minecraft_version=1.21.1' in props
assert 'neo_version=' in props
for file in res.rglob('*.json'):
    with file.open(encoding='utf-8') as f:
        json.load(f)
all_en = json.loads((res / 'assets/survivordogtags/lang/en_us.json').read_text())
all_zh = json.loads((res / 'assets/survivordogtags/lang/zh_tw.json').read_text())
assert set(all_zh) == set(all_en), f'Inconsistent translation keys: {set(all_zh)^set(all_en)}'
assert len(all_zh) >= 20
for path in res.rglob('*.png'):
    bs = path.read_bytes()
    assert bs.startswith(b'\x89PNG\r\n\x1a\n'), f'Not a PNG: {path}'
    width, height = struct.unpack('>II', bs[16:24]);assert 16 <= width <= 256 and 16 <= height <= 256
costs = (root/'src/main/java/tw/zonezero/survivordogtags/game/DogTagUpgradePlan.java').read_text()
for snippet in ['new Need(Material.IRON, 1), new Need(Material.COPPER, 1)',
                'new Need(Material.IRON, 40)',
                'new Need(Material.IRON, 80), new Need(Material.ALUMINUM, 40)',
                'new Need(Material.GOLD, 40), new Need(Material.ALUMINUM, 80)',
                'new Need(Material.COBALT, 6), new Need(Material.GOLD, 4)',
                'new Need(Material.TUNGSTEN, 80), new Need(Material.COBALT, 80)']:
    assert snippet in costs, f'Cost table missing: {snippet}'
modtoml = (root/'src/main/templates/META-INF/neoforge.mods.toml').read_text()
for dependency in ['modId="curios"','modId="tacz"','modId="exposure"']:
    assert dependency in modtoml
assert (res/'data/survivordogtags/curios/entities/player.json').exists(), 'Player necklace slot must be declared'
slot_config = json.loads((res/'data/survivordogtags/curios/entities/player.json').read_text())
assert 'necklace' in slot_config['slots'] and ('minecraft:player' in slot_config['entities'] or 'player' in slot_config['entities'])
assert (root/'tests/PaymentPlannerSmokeTest.java').exists(), 'Missing atomic payment regression test'
assert (root/'tests/PaymentPlannerFuzzTest.java').exists(), 'Missing cross-tag allocation fuzz test'
assert (root/'tests/ProfileSafetySmokeTest.java').exists(), 'Missing identity data safety regression test'
assert (root/'tests/KillMathSmokeTest.java').exists(), 'Missing historical kill migration regression test'
assert (root/'tests/ShotLedgerSmokeTest.java').exists(), 'Missing per-shot deduplication regression test'
ledger = (root/'src/main/java/tw/zonezero/survivordogtags/game/DogTagShotLedger.java').read_text()
assert 'beginShot(' in ledger and 'markHit(' in ledger and 'currentShot(' in ledger
assert 'now == fired.tick' in ledger, 'Shot token must not be inherited after its creation tick' 
combat = (root/'src/main/java/tw/zonezero/survivordogtags/game/DogTagCombat.java').read_text()
for event_hook in ['onGunFire(', 'onProjectileJoin(', 'onTaCZBullet(']:
    assert event_hook in combat
for material in ['forge:ingots/aluminum', 'forge:ingots/cobalt', 'forge:ingots/tungsten']:
    assert material in (root/'src/main/java/tw/zonezero/survivordogtags/workbench/DogTagMaterials.java').read_text()
assert 'shooter.server.getTickCount()' in combat, 'Bullet grouping must use server-global tick count'
assert 'if (shotId == 0L)' in combat and 'return;' in combat, 'Unknown projectiles must not bypass shotgun safety'
assert 'index == RESULT_SLOT' in (root/'src/main/java/tw/zonezero/survivordogtags/workbench/DogTagWorkbenchMenu.java').read_text(), 'Must reject shift-click result transfers'
assert 'DogTagShiftRouting.destination(' in (root/'src/main/java/tw/zonezero/survivordogtags/workbench/DogTagWorkbenchMenu.java').read_text(), 'Missing validated 4-slot quick-transfer routing'
assert 'vanillaZombieKills' in (root/'src/main/java/tw/zonezero/survivordogtags/game/DogTagTracker.java').read_text(), 'Missing vanilla stats migration'
assert (root/'tests/DamageMathSmokeTest.java').exists(), 'Missing fixed TaCZ bonus regression test'
block_code = (root/'src/main/java/tw/zonezero/survivordogtags/workbench/DogTagWorkbenchBlock.java').read_text()
assert 'protected ItemInteractionResult useItemOn(' in block_code, 'Holding Exposure photos should not block bench interaction'
assert 'protected VoxelShape getShape(' in block_code and 'rotateShape(' in block_code, 'Four-sided 3D workbench needs matching collision'
assert (root/'tests/ShiftRoutingSmokeTest.java').exists(), 'Missing quick-transfer bounds test'
# A facing-aware, non-cubic workbench must have all horizontal blockstates.
workbench_states = json.loads((res/'assets/survivordogtags/blockstates/dog_tag_workbench.json').read_text())
assert set(workbench_states['variants']) == {'facing=north', 'facing=south', 'facing=east', 'facing=west'}
assert {v.get('y', 0) for v in workbench_states['variants'].values()} == {0, 90, 180, 270}
workbench_model = json.loads((res/'assets/survivordogtags/models/block/dog_tag_workbench.json').read_text())
assert len(workbench_model['elements']) >= 6, 'The dog-tag bench requires raised metalworking details'
for element in workbench_model['elements']:
    start, end = element['from'], element['to']
    assert all(0 <= a < b <= 16 for a,b in zip(start,end)), f'Out-of-bounds 3D bench element: {element}'
    assert all(face['texture'].lstrip('#') in workbench_model['textures'] for face in element['faces'].values())
profile_code = (root/'src/main/java/tw/zonezero/survivordogtags/client/DogTagProfileScreen.java').read_text()
assert 'new RemotePlayer(' in profile_code, 'Offline/distant owners need a visible 3D model fallback'
photo_code = (root/'src/main/java/tw/zonezero/survivordogtags/client/ExposurePhotoRenderer.java').read_text()
assert 'rejectedPhoto = photo.copyWithCount(1)' in photo_code and 'found = false;' not in photo_code.split('catch (ReflectiveOperationException | LinkageError | RuntimeException ex)')[1].split('finally')[0], 'One damaged photo must not disable all Exposure photos'
menu_code = (root/'src/main/java/tw/zonezero/survivordogtags/workbench/DogTagWorkbenchMenu.java').read_text()
assert 'expectedMaterials' in menu_code and 'expectedOutput' in menu_code, 'Server must recheck the entire preview transaction'
print('PASS: JSON/localization/PNG, 6 costs, 3 shot hooks, held-item bench open, rotated collision and transaction guards')
