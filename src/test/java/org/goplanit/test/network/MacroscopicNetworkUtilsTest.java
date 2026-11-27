package org.goplanit.test.network;

import java.util.List;
import java.util.Set;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.MacroscopicNetworkUtils;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the network wide utilities of {@link MacroscopicNetworkUtils}
 *
 * @author markr
 */
public class MacroscopicNetworkUtilsTest extends LayerTestBase {

  private MacroscopicNetwork network;

  @BeforeEach
  public void setUp() {
    network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    layer = network.getTransportLayers().getFactory().registerNew();
  }

  /**
   * Expanding car with taxi bans taxis on a turn banned for cars, leaves a turn banned for buses only as it is, and
   * leaves a turn banned for every mode naming no modes, since it bans taxis already
   * <pre>
   *        2
   *        ^
   *        |
   *   1 -> 0 -> 3      1->0->2 banned for car and bus
   *        |           1->0->3 banned for bus only
   *        v           1->0->4 banned for every mode
   *        4
   * </pre>
   */
  @Test
  public void expandingModeSupportBansExpansionModesWhereReferenceModeIsBanned() {
    var car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    var bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    layer.registerSupportedModes(List.of(car, bus));
    createNodes(5);
    oneWay(1, 0);
    oneWay(0, 2);
    oneWay(0, 3);
    oneWay(0, 4);
    var bannedTurns = layer.getBannedTurns().getFactory();
    var carAndBus = bannedTurns.registerNew(segment(1, 0), segment(0, 2), List.of(car, bus));
    var busOnly = bannedTurns.registerNew(segment(1, 0), segment(0, 3), List.of(bus));
    var everyMode = bannedTurns.registerNew(segment(1, 0), segment(0, 4));

    MacroscopicNetworkUtils.expandModeSupport(network, PredefinedModeType.CAR, PredefinedModeType.TAXI);

    var taxi = network.getModes().get(PredefinedModeType.TAXI);
    assertEquals(Set.of(car, bus, taxi), carAndBus.getExplicitlyBannedModes());
    assertEquals(Set.of(bus), busOnly.getExplicitlyBannedModes());
    assertFalse(everyMode.hasExplicitlyBannedModes());
  }

  /** Expanding a reference mode the network does not have does nothing: the expansion modes are not registered */
  @Test
  public void expandingModeSupportOfMissingReferenceModeDoesNothing() {
    var bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    layer.registerSupportedModes(List.of(bus));

    MacroscopicNetworkUtils.expandModeSupport(network, PredefinedModeType.CAR, PredefinedModeType.TAXI);

    assertNull(network.getModes().get(PredefinedModeType.TAXI));
    assertEquals(Set.of(bus), Set.copyOf(layer.getSupportedModes()));
  }
}
