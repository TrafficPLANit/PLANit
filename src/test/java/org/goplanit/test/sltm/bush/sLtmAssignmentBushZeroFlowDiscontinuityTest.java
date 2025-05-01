package org.goplanit.test.sltm.bush;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.assignment.ltm.sltm.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.StaticLtmTrafficAssignmentBuilder;
import org.goplanit.assignment.ltm.sltm.StaticLtmType;
import org.goplanit.demands.Demands;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.od.demand.OdDemandMatrix;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.sdinteraction.smoothing.MSRASmoothingConfigurator;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypes;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinks;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.Nodes;
import org.goplanit.utils.zoning.OdZones;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the sLTM bush-based capability in a network that yields a situation where a change from a
 * zero flow to a non-zero flow turn triggers a cost discontinuity that may result in flip-flopping
 * in absence of turn specific cost and dcost/flow support.
 * 
 * @author markr
 *
 */
public class sLtmAssignmentBushZeroFlowDiscontinuityTest {

  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  private final IdGroupingToken testToken =
          IdGenerator.createIdGroupingToken("sLtmAssignmentBushZeroFlowDiscontinuityTest");

  /** the logger */
  private static Logger LOGGER = null;

  /**
   * Create demands and populate with OD DEMANDS 1000 O1->D2, O1->D1, O2->D1, O2->D2
   * 
   * @return created demands
   */
  private Demands createDemands() {
    Demands demands = new Demands(testToken);
    demands.timePeriods.getFactory().registerNew("dummyTimePeriod", 0, 3600);
    demands.travelerTypes.getFactory().registerNew("dummyTravellerType");
    demands.userClasses.getFactory().registerNew(
            "dummyUser", network.getModes().get(PredefinedModeType.CAR), demands.travelerTypes.getFirst());

    /* OD DEMANDS 1000 for all combinations */
    OdZones odZones = zoning.getOdZones();
    OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
    odDemands.setValue(odZones.getByXmlId("O1"), odZones.getByXmlId("D1"), 1000.0);
    odDemands.setValue(odZones.getByXmlId("O1"), odZones.getByXmlId("D2"), 1000.0);
    odDemands.setValue(odZones.getByXmlId("O2"), odZones.getByXmlId("D2"), 1000.0);
    odDemands.setValue(odZones.getByXmlId("O2"), odZones.getByXmlId("D1"), 1000.0);
    demands.registerOdDemandPcuHour(
            demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

    return demands;
  }

  /**
   * {@inheritDoc}
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(sLtmAssignmentBushZeroFlowDiscontinuityTest.class);
    }
  }

  /**
   * {@inheritDoc}
   */
  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
  }

  //@formatter:off
  @BeforeEach
  public void intialise() {
    // construct the network.
    // all demands are 1000
    // all capacities are 2000 except for bottleneck link 4 which is 100
    //
    //           * D1
    //           ^
    //           |
    //           |
    //        (6)o <--
    //           ^    \__7__
    //           6          \
    //   O2      | (3)      |   (5)    D2
    //   *------>o ----4--->o---5-->o----->*
    //           ^        (4)^
    //           2            \__3__
    //           |                  |
    //       (0) o----0---->o---1---o
    //           |         (1)      (2)
    //           |
    //           |
    //           * O1
    //
    //
    // Bottleneck: link 4
    // Situation: O1->D2 will use bottleneck link (4) initially, It is congested so revert to not using it.
    //            When not using it, it will use bottom route (and O1->D1 diverts as well). This route is longer.
    //            When turn 2->4 is not used anymore, the derivative of cost on 2 becomes (near) 0 and the 2->4
    //            turn appears to be attractive again. The moment any flow is diverted here again, a big queue
    //            materialises (because link 4 is still saturated due to O2->D2) and we get a repeat.
    //
    // Solution: the PAS for 2->4 should never be allowed to be selected/created after 4 becomes congested because
    // the non-zero flow cost/derivatives into 4 from 2 should be considered instead of the zero flow situation.

    
    try {
      // local CRS in meters
      GeometryFactory geoFactory = JTSFactoryFinder.getGeometryFactory();
      
      network = new MacroscopicNetwork(testToken);
      network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
      networkLayer = network.getTransportLayers().getFactory().registerNew(network.getModes().get(PredefinedModeType.CAR));

      {
        // 0
        Node node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("0");
        node.setPosition(geoFactory.createPoint(new Coordinate(1000, 0)));
        // 1
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("1");
        node.setPosition(geoFactory.createPoint(new Coordinate(2000, 0)));
        // 2
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("2");
        node.setPosition(geoFactory.createPoint(new Coordinate(3000, 0)));
        // 3
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("3");
        node.setPosition(geoFactory.createPoint(new Coordinate(1000, 1000)));
        // 4
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("4");
        node.setPosition(geoFactory.createPoint(new Coordinate(2000, 1000)));
        // 5
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("5");
        node.setPosition(geoFactory.createPoint(new Coordinate(3000, 1000)));
        // 6
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("6");
        node.setPosition(geoFactory.createPoint(new Coordinate(1000, 2000)));
      }
                     
      
      Nodes nodes = networkLayer.getNodes();
      MacroscopicLinks links = networkLayer.getLinks();
      //links
      final double oneKm = 1;
      final var linkFactory = links.getFactory();

      linkFactory.registerNew(nodes.getByXmlId("0"), nodes.getByXmlId("1"), oneKm, true).setXmlId("0");
      linkFactory.registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("2"), oneKm, true).setXmlId("1");
      linkFactory.registerNew(nodes.getByXmlId("0"), nodes.getByXmlId("3"), oneKm, true).setXmlId("2");
      linkFactory.registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("4"), oneKm, true).setXmlId("3");
      linkFactory.registerNew(nodes.getByXmlId("3"), nodes.getByXmlId("4"), oneKm, true).setXmlId("4");
      linkFactory.registerNew(nodes.getByXmlId("4"), nodes.getByXmlId("5"), oneKm, true).setXmlId("5");
      linkFactory.registerNew(nodes.getByXmlId("3"), nodes.getByXmlId("6"), oneKm, true).setXmlId("6");

      linkFactory.registerNew(nodes.getByXmlId("4"), nodes.getByXmlId("6"), oneKm, true).setXmlId("7");

      /* capacities the same (2000), except for 4 which is 100*/
      MacroscopicLinkSegmentTypes linkTypes = networkLayer.getLinkSegmentTypes();
      var linkTypesFactory = linkTypes.getFactory();
      double capacityPcuH = 2000;
      double bottleneckCapacityPcuH = 100;
      double maxDensityPcuKm = 180;
      var carMode = network.getModes().getFirst();
      linkTypesFactory.registerNew("MainType", capacityPcuH, maxDensityPcuKm, carMode).setXmlId("MainType");
      linkTypesFactory.registerNew("BottleNeckType", bottleneckCapacityPcuH, maxDensityPcuKm, carMode).setXmlId("BottleNeckType");

      MacroscopicLinkSegmentType mainType = linkTypes.getByXmlId("MainType");
      boolean abDirection = true;
      boolean registerOnLink = true;
      var linkSegmentFactory = networkLayer.getLinkSegments().getFactory();
      linkSegmentFactory.registerNew(
              links.getByXmlId("0"), mainType, abDirection, registerOnLink).setNumberOfLanes(1).setXmlId("0");
      linkSegmentFactory.registerNew(
              links.getByXmlId("1"), mainType, abDirection, registerOnLink).setNumberOfLanes(1).setXmlId("1");
      linkSegmentFactory.registerNew(
              links.getByXmlId("2"), mainType, abDirection, registerOnLink).setNumberOfLanes(1).setXmlId("2");
      linkSegmentFactory.registerNew(
              links.getByXmlId("3"), mainType, abDirection, registerOnLink).setNumberOfLanes(1).setXmlId("3");
      linkSegmentFactory.registerNew(
              links.getByXmlId("4"), linkTypes.getByXmlId("BottleNeckType"), abDirection, registerOnLink).setNumberOfLanes(1).setXmlId("4");
      linkSegmentFactory.registerNew(
              links.getByXmlId("5"), mainType, abDirection, registerOnLink).setNumberOfLanes(1).setXmlId("5");
      linkSegmentFactory.registerNew(
              links.getByXmlId("6"), mainType, abDirection, registerOnLink).setNumberOfLanes(1).setXmlId("6");

      linkSegmentFactory.registerNew(
              links.getByXmlId("7"), mainType, abDirection, registerOnLink).setNumberOfLanes(1).setXmlId("7");
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      var zoneFactory = zoning.getOdZones().getFactory();
      zoneFactory.registerNew().setXmlId("O1");
      zoneFactory.registerNew().setXmlId("O2");
      zoneFactory.registerNew().setXmlId("D1");
      zoneFactory.registerNew().setXmlId("D2");

      var connectoidFactory = zoning.getOdConnectoids().getFactory();
      connectoidFactory.registerNew(nodes.getByXmlId("0"),  zoning.getOdZones().getByXmlId("O1"), 0);
      connectoidFactory.registerNew(nodes.getByXmlId("3"),  zoning.getOdZones().getByXmlId("O2"), 0);
      connectoidFactory.registerNew(nodes.getByXmlId("5"),  zoning.getOdZones().getByXmlId("D2"), 0);
      connectoidFactory.registerNew(nodes.getByXmlId("6"),  zoning.getOdZones().getByXmlId("D1"), 0);
                      
    }catch(Exception e) {
      e.printStackTrace();
      fail("initialise");
    }
  }
  //@formatter:on

  /**
   * Shared test component
   * @param type
   */
  public void commonTest(StaticLtmType type) {
    try {
      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      var sltmConfigurator = sLTMBuilder.getConfigurator();
      sltmConfigurator.disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      sltmConfigurator.activateDetailedLogging(false);

      /* DESTINATION BASED */
      sltmConfigurator.setType(type);

      // solution -> each PAS update should also perform local loading update for all its bushes
      sltmConfigurator.setAllowOverlappingPasUpdate(true);
      sltmConfigurator.addTrackOdsForLogging(IdMapperType.XML, Pair.of("O1", "D2"));
      sltmConfigurator.addTrackOdsForLogging(IdMapperType.XML, Pair.of("O1", "D1"));

      var smoothing = (MSRASmoothingConfigurator) sltmConfigurator.createAndRegisterSmoothing(Smoothing.MSRA);
      smoothing.setKappaStep(1);
      smoothing.setGammaStep(0.0);
      smoothing.setActivateLambda(true);

      sltmConfigurator.activateOutput(OutputType.LINK);
      sltmConfigurator.registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.setActivateDetailedLogging(true);
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_12);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(500);
      sLTM.execute();

      double finalGap = sLTM.getGapFunction().getGap();
      assert (finalGap < Precision.EPSILON_12);

//      double outflow1 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
//      double outflow2a = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("2a").getLinkSegmentAb());
//      double outflow2b = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("2b").getLinkSegmentAb());
//      double outflow3 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("3").getLinkSegmentAb());
//      double outflow4 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("4").getLinkSegmentAb());
//      double outflow5 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
//      double outflow6 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("6").getLinkSegmentAb());
//      double outflow7 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("7").getLinkSegmentAb());
//      double outflow8 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
//      double outflow9 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("9").getLinkSegmentAb());
//      double outflow10 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("10").getLinkSegmentAb());
//
//      networkLayer.getLinkSegments().forEach(ls -> LOGGER.info(String.format("Link Segment ids: %s", ls.getIdsAsString())));
//
//      assertEquals(outflow1, 2000, Precision.EPSILON_6);
//      assertEquals(outflow2a, 0, Precision.EPSILON_6);
//      assertEquals(outflow2b, 0, Precision.EPSILON_6);
//      assertEquals(outflow3, 15.3846156, Precision.EPSILON_6);
//      assertEquals(outflow4, outflow3, Precision.EPSILON_6);
//      assertEquals(outflow5, 1984.6, 1);
//      assertEquals(outflow6, 0.0, 1);
//      assertEquals(outflow7, 500, Precision.EPSILON_3);
//      assertEquals(outflow8, outflow7, Precision.EPSILON_3);
//      assertEquals(outflow9, 500, Precision.EPSILON_3);
//      assertEquals(outflow10, outflow9, Precision.EPSILON_3);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM bush based assignment for non zero flow discontuituy test");
    }
  }

  /**
   * Test sLTM non-conjugate bush-destination based assignment on above network for a point queue model -->
   * unable to solve properly due to link based costs and derivatives.
   */
  @Test
  public void sLtmPointQueueBushDestinationBasedAssignmentTest() {
   commonTest(StaticLtmType.DESTINATION_BUSH_BASED);
  }

  /**
   * Test sLTM conjugate bush-destination based assignment on above network for a point queue model -->
   * should be able to solve properly once we have turn based costs and derivatives.
   * TODO: not yet implemented turn based costs and derivatives
   */
  @Test
  public void sLtmPointQueueConjugateBushDestinationBasedAssignmentTest() {
    commonTest(StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED);
  }
}
