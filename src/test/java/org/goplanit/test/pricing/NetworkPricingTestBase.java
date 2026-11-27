package org.goplanit.test.pricing;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.pricing.NetworkPricing;
import org.goplanit.test.LayerTestBase;
import org.goplanit.test.mode.ModeTestUtils;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.tollroad.PricedModeSelection;
import org.goplanit.utils.network.layer.macroscopic.tollroad.TollRoad;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

/**
 * Base of the tests of schemes and of the network pricing: a motorway made of two toll roads, priced as one or apart,
 * with an interchange on the first
 * <pre>
 *                5
 *                | on-ramp (primary)
 *                v
 * 9 --trunk--> 0 --m4--> 1 --m4--> 2 --m8--> 3 --m8--> 4 --trunk--> 8
 *                        |
 *                        v off-ramp (primary)
 *                        6
 * </pre>
 * The toll road m4 holds 0-1 and 1-2, m8 holds 2-3 and 3-4, both priced for the default modes. Every road allows car,
 * heavy goods, bus and an ambulance; the network has a bicycle mode too, allowed nowhere. Link segments are 1 km.
 *
 * @author markr
 */
public abstract class NetworkPricingTestBase extends LayerTestBase {

  protected MacroscopicNetwork network;
  protected NetworkPricing pricing;
  protected Mode car;
  protected Mode heavyGoods;
  protected Mode bus;
  protected Mode bicycle;
  protected Mode ambulance;
  protected MacroscopicLinkSegmentType motorway;
  protected MacroscopicLinkSegmentType trunk;
  protected MacroscopicLinkSegmentType primary;
  protected TollRoad m4;
  protected TollRoad m8;

  /**
   * A toll road on the link segment 4-8 that heavy goods alone pay on
   *
   * @return the toll road
   */
  protected TollRoad truckOnlyTollRoad() {
    return layer.getTollRoads().getFactory().registerNew(List.of(segment(4, 8)),
        PricedModeSelection.named(List.of(heavyGoods)));
  }

  @BeforeEach
  public void setUpNetwork() {
    network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    var modes = network.getModes().getFactory();
    car = modes.registerNew(PredefinedModeType.CAR);
    heavyGoods = modes.registerNew(PredefinedModeType.HEAVY_GOODS_VEHICLE);
    bus = modes.registerNew(PredefinedModeType.BUS);
    bicycle = modes.registerNew(PredefinedModeType.BICYCLE);
    ambulance = ModeTestUtils.registerAmbulance(modes);

    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(car, heavyGoods, bus, bicycle, ambulance));
    var types = layer.getLinkSegmentTypes().getFactory();
    motorway = types.registerNew("motorway", 2000, 180);
    trunk = types.registerNew("trunk", 1800, 180);
    primary = types.registerNew("primary", 1200, 180);
    for (var type : List.of(motorway, trunk, primary)) {
      AccessGroupPropertiesFactory.createOnLinkSegmentType(type, 100, List.of(car, heavyGoods, bus, ambulance));
    }

    createNodes(10);
    link(9, 0, trunk, null);
    link(0, 1, motorway, null);
    link(1, 2, motorway, null);
    link(2, 3, motorway, null);
    link(3, 4, motorway, null);
    link(4, 8, trunk, null);
    link(5, 1, primary, null);
    link(1, 6, primary, null);

    var tollRoads = layer.getTollRoads().getFactory();
    m4 = tollRoads.registerNew(List.of(segment(0, 1), segment(1, 2)), PricedModeSelection.unspecified());
    m8 = tollRoads.registerNew(List.of(segment(2, 3), segment(3, 4)), PricedModeSelection.unspecified());

    pricing = new NetworkPricing(IdGroupingToken.collectGlobalToken(), network);
  }
}
