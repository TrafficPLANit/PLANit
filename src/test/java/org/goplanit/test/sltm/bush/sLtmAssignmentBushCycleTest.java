package org.goplanit.test.sltm.bush;

import java.util.logging.Logger;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.assignment.ltm.sltm.input.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.StaticLtmTrafficAssignmentBuilder;
import org.goplanit.assignment.ltm.sltm.common.StaticLtmType;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the sLTM bush-based cycle avoidance capability in a network that is prone to generate cycles
 * 
 * @author markr
 *
 */
public class sLtmAssignmentBushCycleTest {

  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  private final IdGroupingToken testToken =
          IdGenerator.createIdGroupingToken("sLtmAssignmentBushCycleTest");

  /** the logger */
  private static Logger LOGGER = null;

  /**
   * Create demands an populate with OD DEMANDS 2000 A->A``, A->A``
   * 
   * @return created demands
   */
  private Demands createDemands() {
    Demands demands = new Demands(testToken);
    demands.timePeriods.getFactory().registerNew("dummyTimePeriod", 0, 3600);
    demands.travelerTypes.getFactory().registerNew("dummyTravellerType");
    demands.userClasses.getFactory().registerNew(
            "dummyUser", network.getModes().get(PredefinedModeType.CAR), demands.travelerTypes.getFirst());

    /* OD DEMANDS 8000 A->A` */
    OdZones odZones = zoning.getOdZones();
    OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
    odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A``"), 2000.0);
    odDemands.setValue(odZones.getByXmlId("A`"), odZones.getByXmlId("A``"), 2000.0);
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
      LOGGER = Logging.createLogger(sLtmAssignmentBushCycleTest.class);
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
    // Both demands are 2000
    //
    //            ---*A''(7)
    // (4) 8 (B) /   | 10 (B)
    // o------/     o(5)
    // |           |
    // ^ 7         ^ 9
    // |(3)   6    |(2) 5 
    // o------<----o--<--* A'
    // 1\ \___>3_  ^ 4   (6)
    //   \       \ | 
    //  (0)o---o>--o(1)
    //    ^  2a  2b(B)
    //    |0
    // (8)* A
    //
    // cycle occurs in destination based for vertices (3)->(1)->(2)->(3)
    // Bottlenecks (b): 10, 8 (500 veh/h), 2b (100) others (2000 veh/h)
    //
    // Make link 8 a bit longer -> 5 km, so left route becomes slightly less attractive triggering
    // alternative PASs to be used by A with a small flow being rerouted to equalise costs
    //

    
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
        // 0-1
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("0-1");
        node.setPosition(geoFactory.createPoint(new Coordinate(1500, 0)));        
        // 2
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("2");
        node.setPosition(geoFactory.createPoint(new Coordinate(2000, 1000)));
        // 3
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("3");
        node.setPosition(geoFactory.createPoint(new Coordinate(-1000, 1000)));   
        // 4
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("4");
        node.setPosition(geoFactory.createPoint(new Coordinate(-1000, 2000)));
        // 5
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("5");
        node.setPosition(geoFactory.createPoint(new Coordinate(2000, 2000)));
        // 6
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("6");
        node.setPosition(geoFactory.createPoint(new Coordinate(3000, 1000)));
        // 7
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("7");
        node.setPosition(geoFactory.createPoint(new Coordinate(2000, 3000)));   
        // 8
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("8");
        node.setPosition(geoFactory.createPoint(new Coordinate(1000, -1000)));         
      }
                     
      
      Nodes nodes = networkLayer.getNodes();
      MacroscopicLinks links = networkLayer.getLinks();
      //links
      final double oneKm = 1;
      final double fiveKm = 5;
      final var linkFactory = links.getFactory();

      linkFactory.registerNew(nodes.getByXmlId("8"), nodes.getByXmlId("0"), oneKm, true).setXmlId("0");
      linkFactory.registerNew(nodes.getByXmlId("0"), nodes.getByXmlId("3"), oneKm, true).setXmlId("1");
      linkFactory.registerNew(nodes.getByXmlId("0"), nodes.getByXmlId("0-1"), oneKm, true).setXmlId("2a");
      linkFactory.registerNew(nodes.getByXmlId("3"), nodes.getByXmlId("1"), oneKm, true).setXmlId("3");
      linkFactory.registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("2"), oneKm, true).setXmlId("4");
      linkFactory.registerNew(nodes.getByXmlId("6"), nodes.getByXmlId("2"), oneKm, true).setXmlId("5");
      linkFactory.registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("3"), oneKm, true).setXmlId("6");
      linkFactory.registerNew(nodes.getByXmlId("3"), nodes.getByXmlId("4"), oneKm, true).setXmlId("7");
      linkFactory.registerNew(nodes.getByXmlId("4"), nodes.getByXmlId("7"), fiveKm, true).setXmlId("8");
      linkFactory.registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("5"), oneKm, true).setXmlId("9");
      linkFactory.registerNew(nodes.getByXmlId("5"), nodes.getByXmlId("7"), oneKm, true).setXmlId("10");
      linkFactory.registerNew(nodes.getByXmlId("0-1"), nodes.getByXmlId("1"), oneKm, true).setXmlId("2b");

      /* capacities the same (1500), difference is in number of lanes applied) */
      MacroscopicLinkSegmentTypes linkTypes = networkLayer.getLinkSegmentTypes();
      var linkTypesFactory = linkTypes.getFactory();
      linkTypesFactory.registerNew("MainType", 2000, 180, network.getModes().getFirst()).setXmlId("MainType");
      linkTypesFactory.registerNew("BottleNeckType", 500, 180, network.getModes().getFirst()).setXmlId("BottleNeckType");
      linkTypesFactory.registerNew("SuperBottleNeckType", 100, 180, network.getModes().getFirst()).setXmlId("SuperBottleNeckType");

      var linkSegmentFactory = networkLayer.getLinkSegments().getFactory();
      linkSegmentFactory.registerNew(links.getByXmlId("0"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("0");
      linkSegmentFactory.registerNew(links.getByXmlId("1"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("1");
      linkSegmentFactory.registerNew(links.getByXmlId("2a"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("2a");
      linkSegmentFactory.registerNew(links.getByXmlId("3"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("3");
      linkSegmentFactory.registerNew(links.getByXmlId("4"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("4");
      linkSegmentFactory.registerNew(links.getByXmlId("5"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("5");
      linkSegmentFactory.registerNew(links.getByXmlId("6"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("6");
      linkSegmentFactory.registerNew(links.getByXmlId("7"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("7");
      linkSegmentFactory.registerNew(links.getByXmlId("8"), linkTypes.getByXmlId("BottleNeckType"), true, true).setNumberOfLanes(1).setXmlId("8");
      linkSegmentFactory.registerNew(links.getByXmlId("9"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("9");
      linkSegmentFactory.registerNew(links.getByXmlId("10"), linkTypes.getByXmlId("BottleNeckType"), true, true).setNumberOfLanes(1).setXmlId("10");
      linkSegmentFactory.registerNew(links.getByXmlId("2b"), linkTypes.getByXmlId("SuperBottleNeckType"), true, true).setNumberOfLanes(1).setXmlId("2b");
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      var zoneFactory = zoning.getOdZones().getFactory();
      zoneFactory.registerNew().setXmlId("A");
      zoneFactory.registerNew().setXmlId("A`");
      zoneFactory.registerNew().setXmlId("A``");

      var connectoidFactory = zoning.getOdConnectoids().getFactory();
      connectoidFactory.registerNew(nodes.getByXmlId("8"),  zoning.getOdZones().getByXmlId("A"), 0);
      connectoidFactory.registerNew(nodes.getByXmlId("6"),  zoning.getOdZones().getByXmlId("A`"), 0);
      connectoidFactory.registerNew(nodes.getByXmlId("7"),  zoning.getOdZones().getByXmlId("A``"), 0);
                      
    }catch(Exception e) {
      e.printStackTrace();
      fail("initialise");
    }
  }
  //@formatter:on

  /**
   * Test sLTM conjugate bush-destination based assignment on above network for a point queue model
   */
  @Test
  public void sLtmPointQueueConjugateBushDestinationBasedAssignmentTest() {
    try {
      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      var sltmConfigurator = sLTMBuilder.getConfigurator();
      sltmConfigurator.disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);

      /* DESTINATION BASED */
      sltmConfigurator.setType(StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED);

//      sltmConfigurator.addTrackOdsForLogging(IdMapperType.XML, Pair.of("A","A``"));
//      sltmConfigurator.addTrackOdsForLogging(IdMapperType.XML, Pair.of("A`","A``"));

      var smoothing = (MSRASmoothingConfigurator) sltmConfigurator.createAndRegisterSmoothing(Smoothing.MSRA);
      smoothing.setKappaStep(1);
      smoothing.setGammaStep(0.0);
      smoothing.setActivateLambda(true);

      sltmConfigurator.activateOutput(OutputType.LINK);
      sltmConfigurator.registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.setActivateDetailedLogging(true);
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_12);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(2000);
      sLTM.execute();

      double outflow1 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
      double outflow2a = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("2a").getLinkSegmentAb());
      double outflow2b = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("2b").getLinkSegmentAb());
      double outflow3 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("3").getLinkSegmentAb());
      double outflow4 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("4").getLinkSegmentAb());
      double outflow5 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
      double outflow6 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("6").getLinkSegmentAb());
      double outflow7 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("7").getLinkSegmentAb());
      double outflow8 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
      double outflow9 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("9").getLinkSegmentAb());
      double outflow10 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("10").getLinkSegmentAb());

      networkLayer.getLinkSegments().forEach(ls -> LOGGER.info(String.format("Link Segment ids: %s", ls.getIdsAsString())));

      assertEquals(outflow1, 1984.6153844577761, Precision.EPSILON_2);
      assertEquals(outflow2a, 15.384615542223946, Precision.EPSILON_2);
      assertEquals(outflow2b, 15.384615542223946, Precision.EPSILON_2);
      assertEquals(outflow3, 0.0, Precision.EPSILON_2);
      assertEquals(outflow4, outflow3+outflow2b, Precision.EPSILON_2);
      assertEquals(outflow5, 1984.6153844577761, Precision.EPSILON_2);
      assertEquals(outflow6, 0.0, 1);
      assertEquals(outflow7, 500, Precision.EPSILON_2);
      assertEquals(outflow8, outflow7, Precision.EPSILON_2);
      assertEquals(outflow9, 500, Precision.EPSILON_2);
      assertEquals(outflow10, outflow9, Precision.EPSILON_2);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM bush based assignment");
    }
  }

}
