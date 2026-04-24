package org.goplanit.zoning;

import org.goplanit.utils.zoning.ConnectoidAccessZoneEntry;
import org.goplanit.utils.zoning.TransferConnectoid;
import org.goplanit.utils.zoning.Zone;
import org.goplanit.utils.zoning.ZoneConnectoidType;

/**
 * Help creating instances based on type
 */
public class ConnectoidAccessZoneEntryHelper {

  /**
   * Helper that based on the provided type creates and registeres the most likely type of access zone entry:
   * <ul>
   *   <li>{@code ZoneConnectoidType.TRAVELLERS_ACCESS}: {@code ConnectoidAccessZoneEntry} for undirected access </li>
   *   <li>{@code ZoneConnectoidType.PT_VEHICLE_STOP}: {@code DirectedConnectoidAccessZoneEntry} for undirected access </li>
   *   <li>all others: ConnectoidAccessZoneEntry for undirected access </li>
   * </ul>
   * @return created entry already registere don connectoid
   */
  public static ConnectoidAccessZoneEntry createAndRegisterAccessZoneEntryInferredFromType(
      TransferConnectoid connectoid, Zone zone, ZoneConnectoidType type) {

    ConnectoidAccessZoneEntry entry;
    switch (type) {
      case ZONE_ACCESS:
      case ZONE_EGRESS:
      case PT_VEHICLE_STOP:
        entry = connectoid.createDirectedAccessZoneEntry(zone, type);
        break;
      case ZONE_ACCESS_EGRESS:
        entry = connectoid.createUndirectedAccessZoneEntry(zone, type);
        break;
      case NONE:
      case UNKNOWN:
      default:
        entry = connectoid.createUndirectedAccessZoneEntry(zone, type);
    }
    return entry;
  }
}
