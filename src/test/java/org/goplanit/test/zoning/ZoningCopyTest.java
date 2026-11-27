package org.goplanit.test.zoning;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.connectoid.OdConnectoid;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Copying a zoning: a shallow and a deep copy each get a modifier whose events reach their own connectoids, so zone ids
 * renumbered on the copy are followed by the copy's connectoids
 *
 * <pre>
 *   0        1      two nodes; OD zones A (id 0) and B (id 1), the OD connectoid of B at node 1. Removing A from the
 *                   copy and renumbering its zones gives B id 0
 * </pre>
 */
public class ZoningCopyTest extends LayerTestBase {

  /** the zoning to copy */
  private Zoning zoning;

  /** OD zone A */
  private OdZone zoneA;

  /** OD zone B */
  private OdZone zoneB;

  /** the OD connectoid of zone B */
  private OdConnectoid connectoidOfB;

  /**
   * Remove zone A from a copy and renumber the copy's zones, so zone B takes id 0
   *
   * @param copy to renumber the zones of
   */
  private void renumberZonesWithoutA(Zoning copy) {
    copy.getOdZones().remove(copy.getOdZones().get(zoneA.getId()));
    copy.getZoningModifier().recreateZoneIds();
  }

  /** One layer with two nodes, and a zoning with zones A and B, B's connectoid at node 1 */
  @BeforeEach
  public void setUp() {
    var token = IdGroupingToken.collectGlobalToken();
    var network = new MacroscopicNetwork(token);
    var car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(car));
    createNodesOnLine(2);
    zoning = new Zoning(token, layer.getLayerIdGroupingToken());
    zoneA = zoning.getOdZones().getFactory().registerNew();
    zoneB = zoning.getOdZones().getFactory().registerNew();
    connectoidOfB = zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(zoneB, nodes.get(1));
  }

  /** The connectoid of a shallow copy, shared with the original, follows zone B's id renumbered on the copy */
  @Test
  public void shallowCopyConnectoidsFollowZoneIdsRenumberedOnTheCopy() {
    var copy = zoning.shallowClone();
    var copiedB = copy.getOdZones().get(zoneB.getId());
    var copiedConnectoid = copy.getOdConnectoids().get(connectoidOfB.getId());

    renumberZonesWithoutA(copy);

    assertEquals(0, copiedB.getId());
    assertTrue(copiedConnectoid.hasAccessZoneEntry(copiedB));
  }

  /** The connectoid of a deep copy follows the copy's zone B, its id renumbered on the copy */
  @Test
  public void deepCopyConnectoidsFollowZoneIdsRenumberedOnTheCopy() {
    var copy = zoning.deepClone();
    var copiedB = copy.getOdZones().get(zoneB.getId());
    var copiedConnectoid = copy.getOdConnectoids().get(connectoidOfB.getId());
    assertNotSame(zoneB, copiedB);

    renumberZonesWithoutA(copy);

    assertEquals(0, copiedB.getId());
    assertTrue(copiedConnectoid.hasAccessZoneEntry(copiedB));
  }
}
