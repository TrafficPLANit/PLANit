package org.goplanit.test.network;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.test.LayerTestBase;
import org.goplanit.test.mode.ModeTestUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.goplanit.utils.network.layer.physical.LinkSegmentUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the toll roads a layer holds: registering them, the toll road of a link segment, the modes that pay on
 * them, and copying them with the layer. The motorway allows every mode but the bicycle
 * <pre>
 * 0 --motorway--> 1 --motorway--> 2 --motorway--> 3
 * </pre>
 *
 * @author markr
 */
public class TollRoadsTest extends LayerTestBase {

  private Mode car;
  private Mode heavyGoods;
  private Mode motorBike;
  private Mode bus;
  private Mode bicycle;
  private Mode ambulance;

  /**
   * Register a toll road on the layer
   *
   * @param pricedModes the modes that pay on it, as given
   * @param segmentEnds the nodes each link segment runs between, in pairs: from, to
   * @return the toll road registered, null when none of its link segments could be added
   */
  private TollRoad tollRoad(PricedModeSelection pricedModes, int... segmentEnds) {
    var linkSegments = new ArrayList<MacroscopicLinkSegment>();
    for (int i = 0; i < segmentEnds.length; i += 2) {
      linkSegments.add(segment(segmentEnds[i], segmentEnds[i + 1]));
    }
    return layer.getTollRoads().getFactory().registerNew(linkSegments, pricedModes);
  }

  @BeforeEach
  public void setUp() {
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    var modeFactory = network.getModes().getFactory();
    car = modeFactory.registerNew(PredefinedModeType.CAR);
    heavyGoods = modeFactory.registerNew(PredefinedModeType.HEAVY_GOODS_VEHICLE);
    motorBike = modeFactory.registerNew(PredefinedModeType.MOTOR_BIKE);
    bus = modeFactory.registerNew(PredefinedModeType.BUS);
    bicycle = modeFactory.registerNew(PredefinedModeType.BICYCLE);
    ambulance = ModeTestUtils.registerAmbulance(modeFactory);

    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(car, heavyGoods, motorBike, bus, bicycle, ambulance));
    var motorway = layer.getLinkSegmentTypes().getFactory().registerNew("motorway", 2000, 180);
    AccessGroupPropertiesFactory.createOnLinkSegmentType(
        motorway, 110, List.of(car, heavyGoods, motorBike, bus, ambulance));

    createNodes(4);
    link(0, 1, motorway, null);
    link(1, 2, motorway, null);
    link(2, 3, motorway, null);
  }

  /** A layer to which no toll road is added has none, and no link segment is tolled ("Layer without toll roads") */
  @Test
  public void layerWithoutTollRoadsHasNone() {
    assertTrue(layer.getTollRoads().isEmpty());
    assertNull(layer.getTollRoads().getByLinkSegment(segment(0, 1)));
  }

  /**
   * The toll road of a link segment is the one holding it, none for a link segment not tolled ("Toll road of a link
   * segment")
   */
  @Test
  public void tollRoadOfALinkSegmentIsTheOneHoldingIt() {
    var tollRoad = tollRoad(PricedModeSelection.unspecified(), 0, 1, 1, 2);
    tollRoad.setName("M4");

    assertEquals(List.of(segment(0, 1), segment(1, 2)), tollRoad.getLinkSegments());
    assertTrue(tollRoad.containsLinkSegment(segment(1, 2)));
    assertFalse(tollRoad.containsLinkSegment(segment(2, 3)));
    assertSame(tollRoad, layer.getTollRoads().getByLinkSegment(segment(1, 2)));
    assertNull(layer.getTollRoads().getByLinkSegment(segment(2, 3)));
    assertEquals("M4", tollRoad.getName());
  }

  /**
   * The toll roads tell which of the link segments given a toll road already holds, and which none holds, each once:
   * a second toll road can take only the latter ("Link segment already on another toll road")
   */
  @Test
  public void linkSegmentsAlreadyOnAnotherTollRoadAreFound() {
    var first = tollRoad(PricedModeSelection.unspecified(), 0, 1, 1, 2);
    var given = List.of(segment(1, 2), segment(2, 3), segment(2, 3), segment(1, 2));

    assertEquals(List.of(segment(1, 2)), layer.getTollRoads().findLinkSegmentsOnATollRoad(given));
    assertEquals(List.of(segment(2, 3)), layer.getTollRoads().findLinkSegmentsNotOnATollRoad(given));
    assertSame(first, layer.getTollRoads().getByLinkSegment(segment(1, 2)));
  }

  /**
   * A toll road naming no modes is paid by the motorised modes whose use is not public, active or emergency: on a
   * network with car, heavy goods, bus, bicycle and an ambulance, car and heavy goods ("Default priced modes")
   */
  @Test
  public void tollRoadNamingNoModesIsPaidByTheDefaultPricedModes() {
    var tollRoad = tollRoad(PricedModeSelection.unspecified(), 0, 1);
    assertEquals(Set.of(car, heavyGoods), tollRoad.findPricedModes(List.of(car, heavyGoods, bus, bicycle, ambulance)));
  }

  /** A toll road naming heavy goods only is paid by heavy goods only, and car may still use it ("Trucks only") */
  @Test
  public void tollRoadNamingTrucksIsPaidByTrucksOnly() {
    var tollRoad = tollRoad(PricedModeSelection.named(List.of(heavyGoods)), 0, 1, 1, 2);
    assertEquals(Set.of(heavyGoods), tollRoad.findPricedModes(layer.getSupportedModes()));
    assertTrue(segment(0, 1).isModeAllowed(car));
  }

  /**
   * A toll road exempting motorcycles, on a network with car, motorcycle and heavy goods, is paid by car and heavy
   * goods ("Exempt modes")
   */
  @Test
  public void tollRoadExemptingMotorcyclesIsPaidByTheOthers() {
    var tollRoad = tollRoad(PricedModeSelection.exempting(List.of(motorBike)), 0, 1);
    assertEquals(Set.of(car, heavyGoods), tollRoad.findPricedModes(List.of(car, motorBike, heavyGoods)));
  }

  /**
   * A toll road naming the bicycle, which none of its link segments allows, keeps it as a priced mode, and the bicycle
   * is found as the mode none of them allows ("Priced mode that cannot use the road")
   */
  @Test
  public void pricedModeThatCannotUseTheRoadIsKeptAndFound() {
    var tollRoad = tollRoad(PricedModeSelection.named(List.of(car, bicycle)), 0, 1, 1, 2);
    assertEquals(List.of(car, bicycle), tollRoad.getPricedModeSelection().getPricedModes());
    assertEquals(Set.of(car, bicycle), tollRoad.findPricedModes(layer.getSupportedModes()));
    assertEquals(List.of(bicycle),
        LinkSegmentUtils.findModesAllowedByNone(List.of(car, bicycle), tollRoad.getLinkSegments()));
  }

  /**
   * A deep copy of the layer holds its own copy of the toll road, on the copy's link segments, with the same name and
   * priced modes, and changing the copy leaves the original as it was; a shallow copy shares the toll road ("Deep
   * copy")
   */
  @Test
  public void deepCopyOfLayerHoldsItsOwnTollRoadOnItsOwnLinkSegments() {
    var tollRoad = tollRoad(PricedModeSelection.named(List.of(heavyGoods)), 0, 1, 1, 2);
    tollRoad.setName("M4");
    tollRoad.setExternalId("way 1");

    var copy = layer.deepClone();
    var copied = copy.getTollRoads().get(tollRoad.getId());
    assertNotSame(tollRoad, copied);
    var copiedSegments = List.of(copy.getLinkSegments().get(segment(0, 1).getId()),
        copy.getLinkSegments().get(segment(1, 2).getId()));
    assertEquals(copiedSegments, copied.getLinkSegments());
    assertSame(copied, copy.getTollRoads().getByLinkSegment(copiedSegments.get(0)));
    assertNull(copy.getTollRoads().getByLinkSegment(segment(0, 1)));
    assertEquals("M4", copied.getName());
    assertEquals("way 1", copied.getExternalId());
    assertEquals(List.of(heavyGoods), copied.getPricedModeSelection().getPricedModes());

    copied.setName("M4 East");
    copy.getTollRoads().remove(copied);
    assertEquals("M4", tollRoad.getName());
    assertSame(tollRoad, layer.getTollRoads().getByLinkSegment(segment(0, 1)));

    assertSame(tollRoad, layer.shallowClone().getTollRoads().get(tollRoad.getId()));
  }

  /** Recreating the layer's ids renumbers its toll roads from zero, as it does its intersections */
  @Test
  public void recreatingTheLayerIdsRenumbersItsTollRoads() {
    var first = tollRoad(PricedModeSelection.unspecified(), 0, 1);
    var second = tollRoad(PricedModeSelection.unspecified(), 2, 3);
    layer.getTollRoads().remove(first);
    layer.getLayerModifier().recreateManagedIdEntities();
    assertEquals(0, second.getId());
    assertSame(second, layer.getTollRoads().get(0));
  }
}
