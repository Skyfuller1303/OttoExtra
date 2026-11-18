package de.ottoextra.resources.xaero;

import de.ottoextra.resources.ResourceMapOverlay;
import de.ottoextra.resources.ResourceSnapshot;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;

public final class ResourceWaypointNavigation {
    private static WaypointSet activeSet;
    private static Waypoint activeWaypoint;

    private ResourceWaypointNavigation() {
    }

    public static boolean navigate(ResourceSnapshot resource) {
        clear();
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null) return false;
        MinimapWorld world = session.getWorldManager().getCurrentWorld();
        if (world == null || world.getCurrentWaypointSet() == null) return false;
        Waypoint waypoint = new Waypoint(
                (int) Math.round(ResourceMapOverlay.centerX(resource.zone())),
                ResourceMapOverlay.centerY(resource.zone()),
                (int) Math.round(ResourceMapOverlay.centerZ(resource.zone())),
                "OttoExtra: " + resource.name(), "OE", WaypointColor.YELLOW,
                WaypointPurpose.NORMAL, true, true);
        activeSet = world.getCurrentWaypointSet();
        activeWaypoint = waypoint;
        activeSet.add(waypoint);
        return true;
    }

    public static void clear() {
        if (activeSet != null && activeWaypoint != null) activeSet.remove(activeWaypoint);
        activeSet = null;
        activeWaypoint = null;
    }
}
