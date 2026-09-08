package de.ottoextra.tweaks.combat;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Session-local modes, kept separately for each combat skill. */
public final class CombatModeState {
    private final Map<WeaponFamily, CombatMode> modes = new EnumMap<>(WeaponFamily.class);

    public void observe(WeaponFamily heldWeapon, String actionbar) {
        if (heldWeapon == null) return;
        CombatMode.parse(actionbar).filter(mode -> mode.family() == heldWeapon)
                .ifPresent(mode -> modes.put(heldWeapon, mode));
    }

    public Optional<CombatMode> mode(WeaponFamily heldWeapon) {
        return Optional.ofNullable(heldWeapon == null ? null : modes.get(heldWeapon));
    }

    public void clear() { modes.clear(); }
}
