# Survivor Dog Tags — 倖存者狗牌 0.1.9-alpha

**Source project**, not a compiled Minecraft mod. Target: Minecraft **1.21.1 / NeoForge / Java 21**.
Required on **both client and server**: Curios API 9.5.1, Exposure (target 1.9.19), TaCZ NeoForge 1.1.8-hotfix-r7.

## Gameplay

- A custom **Dog Tag Workbench** creates Lv.0 dog tags from **one iron ingot, one copper ingot and a developed Exposure photograph**.
- The resulting dog tag records the owner's player UUID and name, current refinement level, stored photograph, and a snapshot of their zombie kills.
- Holding a bound dog tag and right-clicking opens the identity card: the owner's actual player model when loaded, or an inert offline/distant-player model preview, username, persistent zombie kills, and Exposure photo. Click the photo to enlarge it.
- **Any level** can replace its embedded photograph using the workbench's separate *Replace Photo* mode, without spending metals, losing levels or altering owner data. The new photo item is consumed.
- **Curios necklace slot** activation gives health and speed bonuses. A hand-held tag confers no stat increase. Extra necklace slots cannot stack dog-tag bonuses.
- Physical damage bonuses affect melee player attacks and **direct TaCZ bullet hits**, never ongoing bleeding/fire/explosion. Shotgun pellets from one trigger shot can grant this flat bonus only once per victim. A separate shot may grant it again.
- Zombie kills are recorded server-side in the world's SavedData, with a datapack entity type tag for additional non-vanilla zombies. Older mod counter data is migrated on player login. Existing vanilla statistics for zombie, husk, drowned, zombie villager and zombified piglin kills are also imported as a lower bound without counting historical kills twice. Non-vanilla pre-installation kills cannot be recovered if no old record exists.
- The workbench uses four material slots and explicit *Refine* / *Replace Photo* modes, and returns unused inputs when closed. Shift-click now safely routes tags to the tag slot, valid Exposure photos to the photo slot, and ingots into all four material slots. Result pickup remains limited to an empty cursor's ordinary left/right click; the server verifies and pays the full cost before releasing a result.

## Exact refinement table (total bonuses; no stacking with previous levels)

| Result | Cost | Total benefit while worn |
| --- | --- | --- |
| Lv.0 | Iron ingot ×1 + Copper ingot ×1 + photo | Identity only |
| Lv.1 | Iron ingot ×40 | Max health +2 |
| Lv.2 | Iron ingot ×80 + Aluminum ingot ×40 | Max health +4, direct physical damage +1 |
| Lv.3 | Gold ingot ×40 + Aluminum ingot ×80 | Max health +6, speed +3%, direct physical damage +1 |
| Lv.4 | Cobalt ingot ×6 + Gold ingot ×4 | Max health +8, speed +4%, direct physical damage +1 |
| Lv.5 | Tungsten ingot ×80 + Cobalt ingot ×80 | Max health +10, speed +5%, direct physical damage +2 |

Ingot matching supports the relevant `c:ingots/*` and legacy `forge:ingots/*` tags. If a pack uses custom tags, provide datapack aliases.

## Changes in 0.1.9-alpha

1. **Reliable workbench access with a held item:** the bench explicitly handles 1.21.1 `BlockBehaviour.useItemOn`, so right-clicking with a photo, dog tag or ingots opens the bench instead of activating the held item. The empty-hand path still works. There is a single server-only menu-opening method to avoid double opens.
2. **Directional interaction shape:** the raised, non-cubic 3D metal bench now has a matching 4-way rotated collision/selection shape. `.noOcclusion()` prevents the base block from incorrectly behaving as an opaque cube for adjacent face visibility.
3. **Stronger direct-hit arithmetic:** the fixed TaCZ +1/+2 base adjustment is a pure-Java tested helper. Non-finite, zero or negative inputs are ignored instead of being passed into damage events. The original one-shot/one-target pellet rule is preserved.
4. **Identity card polish:** Lv.0 now clearly indicates it is for identification only, without showing a misleading +0 health bonus; long names are width-limited in the UI.
5. **Nine independent Java regression tests:** all original eight suites plus the fixed direct-hit/headshot calculation test pass locally. Gradle build and actual gameplay still remain unverified.

Your exact refinement costs, all six levels' total bonuses and universal photograph replacement remain unchanged.

## Build and test

On a host with Java 21, Gradle 9.2.1 and network access:

```sh
bash tools/test_local.sh
# Runs independent Java tests and checks resource files, no Minecraft required.
gradle build --stacktrace --no-daemon
python3 tools/verify_jar.py
# The verification step must pass before using any JAR from build/libs/.
```

Alternatively upload the **contents of this ZIP** to a GitHub repository, then run `.github/workflows/build.yml`. The GitHub Actions run supplies a downloadable compiled JAR **only after a successful build**. This package itself must never be placed in the `mods` directory.

## Known limits and pending real-game verification

- This execution environment has **no Gradle installed and cannot resolve Maven/Gradle hosts**. All checks reported here are isolated Java and resource checks, not an actual NeoForge compilation or a Minecraft launch. No installable JAR is currently provided.
- Offline and distant owners now use a client-only player-model preview. The exact original skin is not guaranteed when the client has not cached that skin; a default skin can appear instead.
- The photo rendering integration is reflective and still needs testing against the exact Exposure JAR.
- Shift-click material routing is independently tested, but the actual Minecraft menu transfer behavior must be verified in a running game.
- Vanilla TaCZ fires a `GunFireEvent` then spawns pellets in the same server tick, which our direct-hit grouping is designed for. Gun packs which bypass this event pipeline deliberately receive no dog-tag bullet bonus rather than incorrectly multiplying damage.
- Additional custom zombie types need to be placed in `data/survivordogtags/tags/entity_type/zombie_kills.json` by datapack.
- The ownership/transfer rule is **not finalized**: as currently implemented, anyone who equips a bound dog tag receives its benefits, but it always keeps the original person's identity.
- No enchantment, durability damage, upgrade failure chances, redstone power, or extra GUI slots were added.
- No third-party mod JARs are modified. The build's dependency versions have been checked against Modrinth, but the project still needs its first full build log.
