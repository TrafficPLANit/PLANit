package org.goplanit.test.sltm;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.assignment.ltm.sltm.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.StaticLtmTrafficAssignmentBuilder;
import org.goplanit.assignment.ltm.sltm.StaticLtmType;
import org.goplanit.choice.logit.BoundedMultinomialLogit;
import org.goplanit.choice.logit.BoundedMultinomialLogitConfigurator;
import org.goplanit.demands.Demands;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.od.demand.OdDemandMatrix;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.path.choice.PathChoice;
import org.goplanit.path.choice.StochasticPathChoiceConfigurator;
import org.goplanit.choice.ChoiceModel;
import org.goplanit.sdinteraction.smoothing.FixedStepSmoothingConfigurator;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypes;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinks;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.Nodes;
import org.goplanit.utils.path.PathUtils;
import org.goplanit.utils.zoning.OdZones;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the sLTM assignment basic functionality (route choice) with multiple destinations for a single origin
 * 
 * @author markr
 *
 */
public class sLtmAssignmentMultiDestinationTest {

  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  private final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("sLtmAssignmentMultiDestinationTest");

  /** the logger */
  private static Logger LOGGER = null;

  /**
   * Create demands an populate with OD DEMANDS 4000 A->A`, 4000 A->A``
   * 
   * @return created demands
   */
  private Demands createDemands() {
    Demands demands = new Demands(testToken);
    demands.timePeriods.getFactory().registerNew("dummyTimePeriod", 0, 3600);
    demands.travelerTypes.getFactory().registerNew("dummyTravellerType");
    demands.userClasses.getFactory().registerNew("dummyUser", network.getModes().get(PredefinedModeType.CAR), demands.travelerTypes.getFirst());

    /* OD DEMANDS 4000 A->A`, 4000 A->A`` */
    OdZones odZones = zoning.getOdZones();
    OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
    odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A`"), 4000.0);
    odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A``"), 4000.0);
    demands.registerOdDemandPcuHour(demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

    return demands;
  }

  private void testDeterministicOutputs(StaticLtm sLTM) {
    double outflow0 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("0").getLinkSegmentAb());
    double outflow1 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double outflow2 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());
    double outflow3 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("3").getLinkSegmentAb());
    double outflow4 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("4").getLinkSegmentAb());
    double outflow5 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
    double outflow6 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("6").getLinkSegmentAb());
    double outflow7 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("7").getLinkSegmentAb());
    double outflow8 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
    double outflow9 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("9").getLinkSegmentAb());
    double outflow10 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("10").getLinkSegmentAb());
    double outflow11 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("11").getLinkSegmentAb());
    double outflow12 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("12").getLinkSegmentAb());
    double outflow13 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("13").getLinkSegmentAb());
    double outflow14 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("14").getLinkSegmentAb());

    assertTrue(Precision.smallerEqual(outflow4, 4000));
    assertTrue(Precision.smallerEqual(outflow8, 4000));

    assertEquals(outflow0, 8000, Precision.EPSILON_3);
    assertEquals(outflow1, 4522.6, 1);
    assertEquals(outflow2, 1500.0, Precision.EPSILON_3);
    assertEquals(outflow3, outflow2, Precision.EPSILON_3);
    //assertEquals(outflow4, 3750, 1); do not test due to non-uniqueness being allowed under no congestion an triangular FD
    assertEquals(outflow5, 3190, 1);
    assertEquals(outflow6, 1500.0, Precision.EPSILON_3);
    assertEquals(outflow7, outflow6, Precision.EPSILON_3);
    //assertEquals(outflow8, 3750, 1); do not test due to non-uniqueness being allowed under no congestion an triangular FD
    assertEquals(outflow9, 3000.0, Precision.EPSILON_3);
    assertEquals(outflow10, 1500.0, Precision.EPSILON_3);
    assertEquals(outflow11, outflow10, Precision.EPSILON_3);
    assertEquals(outflow12, 4500.0, Precision.EPSILON_3);
    //assertEquals(outflow13, 2250, 1); do not test due to non-uniqueness being allowed under no congestion an triangular FD
    //assertEquals(outflow14, 2250, 1); do not test due to non-uniqueness being allowed under no congestion an triangular FD

    double inflow1 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double inflow2 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());
    double inflow3 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("3").getLinkSegmentAb());
    double inflow4 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("4").getLinkSegmentAb());
    double inflow5 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
    double inflow6 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("6").getLinkSegmentAb());
    double inflow7 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("7").getLinkSegmentAb());
    double inflow8 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
    double inflow9 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("9").getLinkSegmentAb());
    double inflow10 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("10").getLinkSegmentAb());
    double inflow11 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("11").getLinkSegmentAb());
    double inflow12 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("12").getLinkSegmentAb());
    double inflow13 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("13").getLinkSegmentAb());
    double inflow14 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("14").getLinkSegmentAb());

    assertEquals(outflow0, inflow1 + inflow5, Precision.EPSILON_6);
    assertEquals(outflow1, inflow2 + inflow9, Precision.EPSILON_6);
    assertEquals(outflow2, inflow3, Precision.EPSILON_6);
    assertEquals(outflow3 + outflow13, inflow4, Precision.EPSILON_6);
    assertEquals(outflow4, inflow4, Precision.EPSILON_6);
    assertEquals(outflow5, inflow10 + inflow6, Precision.EPSILON_6);
    assertEquals(outflow6, inflow7, Precision.EPSILON_6);
    assertEquals(outflow7 + outflow14, inflow8, Precision.EPSILON_6);
    assertEquals(outflow8, inflow8, Precision.EPSILON_6);
    assertEquals(outflow9 + outflow11, inflow12, Precision.EPSILON_6);
    assertEquals(outflow10, inflow11, Precision.EPSILON_6);
    assertEquals(outflow12, inflow13 + inflow14, Precision.EPSILON_6);
  }

  private void testStochasticOutputs(StaticLtm sLTM) {
    double outflow0 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("0").getLinkSegmentAb());
    double outflow1 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double outflow2 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());
    double outflow3 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("3").getLinkSegmentAb());
    double outflow4 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("4").getLinkSegmentAb());
    double outflow5 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
    double outflow6 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("6").getLinkSegmentAb());
    double outflow7 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("7").getLinkSegmentAb());
    double outflow8 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
    double outflow9 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("9").getLinkSegmentAb());
    double outflow10 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("10").getLinkSegmentAb());
    double outflow11 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("11").getLinkSegmentAb());
    double outflow12 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("12").getLinkSegmentAb());
    double outflow13 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("13").getLinkSegmentAb());
    double outflow14 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("14").getLinkSegmentAb());

    assertTrue(Precision.smallerEqual(outflow4, 4000));
    assertTrue(Precision.smallerEqual(outflow8, 4000));

    assertEquals(outflow0, 8000, Precision.EPSILON_3);
    assertEquals(outflow1, 4520.3, 1);
    assertEquals(outflow2, 1500.0, Precision.EPSILON_3);
    assertEquals(outflow3, outflow2, Precision.EPSILON_3);
    assertEquals(outflow4, 3749.7, 1);
    assertEquals(outflow5, 3290.3, 1);
    assertEquals(outflow6, 1500.0, Precision.EPSILON_3);
    assertEquals(outflow7, outflow6, Precision.EPSILON_3);
    assertEquals(outflow8, 3750.3, 1);
    assertEquals(outflow9, 3000.0, Precision.EPSILON_3);
    assertEquals(outflow10, 1500.0, Precision.EPSILON_3);
    assertEquals(outflow11, outflow10, Precision.EPSILON_3);
    assertEquals(outflow12, 4500.0, Precision.EPSILON_3);
    assertEquals(outflow13, 2249.7, 1);
    assertEquals(outflow14, 2249.7, 1);

    double inflow1 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double inflow2 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());
    double inflow3 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("3").getLinkSegmentAb());
    double inflow4 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("4").getLinkSegmentAb());
    double inflow5 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
    double inflow6 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("6").getLinkSegmentAb());
    double inflow7 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("7").getLinkSegmentAb());
    double inflow8 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
    double inflow9 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("9").getLinkSegmentAb());
    double inflow10 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("10").getLinkSegmentAb());
    double inflow11 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("11").getLinkSegmentAb());
    double inflow12 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("12").getLinkSegmentAb());
    double inflow13 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("13").getLinkSegmentAb());
    double inflow14 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("14").getLinkSegmentAb());

    assertEquals(outflow0, inflow1 + inflow5, Precision.EPSILON_6);
    assertEquals(outflow1, inflow2 + inflow9, Precision.EPSILON_6);
    assertEquals(outflow2, inflow3, Precision.EPSILON_6);
    assertEquals(outflow3 + outflow13, inflow4, Precision.EPSILON_6);
    assertEquals(outflow4, inflow4, Precision.EPSILON_6);
    assertEquals(outflow5, inflow10 + inflow6, Precision.EPSILON_6);
    assertEquals(outflow6, inflow7, Precision.EPSILON_6);
    assertEquals(outflow7 + outflow14, inflow8, Precision.EPSILON_6);
    assertEquals(outflow8, inflow8, Precision.EPSILON_6);
    assertEquals(outflow9 + outflow11, inflow12, Precision.EPSILON_6);
    assertEquals(outflow10, inflow11, Precision.EPSILON_6);
    assertEquals(outflow12, inflow13 + inflow14, Precision.EPSILON_6);
  }

  /**
   * {@inheritDoc}
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(sLtmAssignmentMultiDestinationTest.class);
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
    //
    // The network is constructed such that it is symmetric for both destinations apart from one of the destination's
    // PAS having a bottleneck whereas the other does not. After the flows merge the composition of flows differs and is 
    // not 50/50 as the bush-splitting rates would indicate if we do not consider splitting rates by unique compositions.
    // Hence this test identifies the need to store bush splitting rates by each unique flow composition that passes a 
    // diverge node, otherwise it will fail
    //
    // ! = bottleneck
    // Demand AA'  = 4000
    // Demand AA'' = 4000
    //
    // [0,1,4,5,12] = 8000 capacity
    // [2,4,6,8,9,13] = 4000 capacity
    // [9,10] = 3000 capacity
    // [3,7,11] = 1500 capacity
    // 
    //            
    //    A'                    A''
    //    *(5)               (9)*
    //  4 |    13        14     | 8
    //    *----------*----------* (8)
    //    |(4)       |(12)   (5)|
    //  3!|        12|          | 7!
    //    *(3)       * (11)     * (7)
    //    \         / \ 11!     /
    //     \       /   * (10)  /
    //  2   \   9 /     \10   / 6
    //       \   /       \   /
    //     (2) *    (1)    * (6)
    //          \----*----/
    //           1   |   5
    //               | 0
    //           (0) *
    //               A
    
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
        node.setPosition(geoFactory.createPoint(new Coordinate(2000, 0)));
        // 1
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("1");
        node.setPosition(geoFactory.createPoint(new Coordinate(2000, 1000)));
        // 2
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("2");
        node.setPosition(geoFactory.createPoint(new Coordinate(1000, 1000)));
        // 3
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("3");
        node.setPosition(geoFactory.createPoint(new Coordinate(0, 2000)));   
        // 4
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("4");
        node.setPosition(geoFactory.createPoint(new Coordinate(0, 3000)));
        // 5
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("5");
        node.setPosition(geoFactory.createPoint(new Coordinate(0, 4000)));
        // 6
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("6");
        node.setPosition(geoFactory.createPoint(new Coordinate(3000, 1000)));
        // 7
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("7");
        node.setPosition(geoFactory.createPoint(new Coordinate(4000, 2000)));
        // 8
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("8");
        node.setPosition(geoFactory.createPoint(new Coordinate(4000, 3000)));
        // 9
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("9");
        node.setPosition(geoFactory.createPoint(new Coordinate(4000, 4000)));
        // 10
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("10");
        node.setPosition(geoFactory.createPoint(new Coordinate(2500, 1500)));
        // 11
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("11");
        node.setPosition(geoFactory.createPoint(new Coordinate(2000, 2000)));
        // 12
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("12");
        node.setPosition(geoFactory.createPoint(new Coordinate(2000, 3000)));        
      }
                     
      
      Nodes nodes = networkLayer.getNodes();
      MacroscopicLinks links = networkLayer.getLinks();
      var linkFactory = links.getFactory();
      //links
      linkFactory.registerNew(nodes.getByXmlId("0"), nodes.getByXmlId("1"), 1, true).setXmlId("0");
      linkFactory.registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("2"), 1, true).setXmlId("1");
      linkFactory.registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("3"), 1, true).setXmlId("2");
      linkFactory.registerNew(nodes.getByXmlId("3"), nodes.getByXmlId("4"), 1, true).setXmlId("3");
      linkFactory.registerNew(nodes.getByXmlId("4"), nodes.getByXmlId("5"), 1, true).setXmlId("4");
      linkFactory.registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("6"), 1, true).setXmlId("5");
      linkFactory.registerNew(nodes.getByXmlId("6"), nodes.getByXmlId("7"), 1, true).setXmlId("6");
      linkFactory.registerNew(nodes.getByXmlId("7"), nodes.getByXmlId("8"), 1, true).setXmlId("7");
      linkFactory.registerNew(nodes.getByXmlId("8"), nodes.getByXmlId("9"), 1, true).setXmlId("8");
      linkFactory.registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("11"), 1, true).setXmlId("9");
      linkFactory.registerNew(nodes.getByXmlId("6"), nodes.getByXmlId("10"), 1, true).setXmlId("10");
      linkFactory.registerNew(nodes.getByXmlId("10"), nodes.getByXmlId("11"), 1, true).setXmlId("11");
      linkFactory.registerNew(nodes.getByXmlId("11"), nodes.getByXmlId("12"), 1, true).setXmlId("12");
      linkFactory.registerNew(nodes.getByXmlId("12"), nodes.getByXmlId("4"), 1, true).setXmlId("13");
      linkFactory.registerNew(nodes.getByXmlId("12"), nodes.getByXmlId("8"), 1, true).setXmlId("14");
      
      
      /* capacities the same (500), difference is in number of lanes applied), two types, one of 3 lanes (bottleneck) and one of 16 lanes (not bottleneck) */
      MacroscopicLinkSegmentTypes linkTypes = networkLayer.getLinkSegmentTypes();
      linkTypes.getFactory().registerNew("500_lane", 500, 180, network.getModes().getFirst()).setXmlId("500_lane");

      var linkSegmentFactory = networkLayer.getLinkSegments().getFactory();
      linkSegmentFactory.registerNew(links.getByXmlId("0"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(16);
      linkSegmentFactory.registerNew(links.getByXmlId("1"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(16);
      linkSegmentFactory.registerNew(links.getByXmlId("2"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(8);
      linkSegmentFactory.registerNew(links.getByXmlId("3"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(3);
      linkSegmentFactory.registerNew(links.getByXmlId("4"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(16);
      linkSegmentFactory.registerNew(links.getByXmlId("5"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(16);
      linkSegmentFactory.registerNew(links.getByXmlId("6"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(8);
      linkSegmentFactory.registerNew(links.getByXmlId("7"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(3);
      linkSegmentFactory.registerNew(links.getByXmlId("8"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(16);
      linkSegmentFactory.registerNew(links.getByXmlId("9"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(6);
      linkSegmentFactory.registerNew(links.getByXmlId("10"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(6);
      linkSegmentFactory.registerNew(links.getByXmlId("11"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(3);
      linkSegmentFactory.registerNew(links.getByXmlId("12"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(16);
      linkSegmentFactory.registerNew(links.getByXmlId("13"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(8);
      linkSegmentFactory.registerNew(links.getByXmlId("14"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(8);
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.getOdZones().getFactory().registerNew().setXmlId("A");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A`");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A``");

      var connectoidFactory = zoning.getOdConnectoids().getFactory();
      connectoidFactory.registerNew(nodes.getByXmlId("0"),  zoning.getOdZones().getByXmlId("A"), 0);
      connectoidFactory.registerNew(nodes.getByXmlId("5"),  zoning.getOdZones().getByXmlId("A`"), 0);
      connectoidFactory.registerNew(nodes.getByXmlId("9"),  zoning.getOdZones().getByXmlId("A``"), 0);
                      
    }catch(Exception e) {
      e.printStackTrace();
      fail("initialise");
    }
  }
  //@formatter:on

  /**
   * Test sLTM path based assignment on above network for a point queue model with Weibit SUE
   */
  @Test
  public void sLtmPointQueuePathBasedWeibitSueAssignmentTest() {
    try {

      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      var sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      var configurator = sLTMBuilder.getConfigurator();
      configurator.disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      configurator.activateDetailedLogging(false);

      /* PATH BASED */
      configurator.setType(StaticLtmType.PATH_BASED);

      /* fixed smoothing step */
      var smoothingConfig = (FixedStepSmoothingConfigurator) configurator.createAndRegisterSmoothing(Smoothing.FIXED_STEP);
      smoothingConfig.setStepSize(0.9);

      /* PATH CHOICE - STOCHASTIC */
      final var suePathChoice = (StochasticPathChoiceConfigurator) configurator.createAndRegisterPathChoice(PathChoice.STOCHASTIC);
      {
        /* Weibit for path choice */
        var choiceModel = suePathChoice.createAndRegisterChoiceModel(ChoiceModel.WEIBIT);
        choiceModel.setScalingFactor(1); // we go for rather muddled perceived cost to differentiate from deterministic result
        // by not setting a fixed od path set (suePathChoice.setFixedOdPathMatrix(...)), it is assumed we want a dynamic path set

        // You can add your own custom filters alternatively, the one below being identical to the predefined max overlap one for 80%
        final var MAX_OVERLAP = 0.6;
        suePathChoice.getPathFilter().addCustomFilter(
                (p, paths) -> paths.stream().noneMatch(pAlt -> {
                  var factor = PathUtils.getOverlapFactor(p, pAlt);
                  if(factor > MAX_OVERLAP){
                    LOGGER.info(String.format("OVERLAP TOO HIGH %s", factor));
                  }else{
                    LOGGER.info(String.format("OVERLAP OK %s", factor));
                  };
                  return factor > MAX_OVERLAP;}));
      }

      /* OUTPUT CONFIG */
      configurator.activateOutput(OutputType.LINK);
      configurator.registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      /* GAP AND CONVERGENCE */
      configurator.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      configurator.getGapFunction().getStopCriterion().setMaxIterations(1000);

      /* BUILD AND EXECUTE */
      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

      testStochasticOutputs(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM multi-destination path based assignment");
    }
  }

  /**
   * Test sLTM path based assignment on above network for a point queue model with bounded MNL
   */
  @Test
  public void sLtmPointQueuePathBasedBoundedMnlSueAssignmentTest() {
    try {

      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      var sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      var configurator = sLTMBuilder.getConfigurator();
      configurator.disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      configurator.activateDetailedLogging(false);

      /* PATH BASED */
      configurator.setType(StaticLtmType.PATH_BASED);

      /* fixed smoothing step */
      var smoothingConfig = (FixedStepSmoothingConfigurator) configurator.createAndRegisterSmoothing(Smoothing.FIXED_STEP);
      smoothingConfig.setStepSize(0.25);

      /* PATH CHOICE - STOCHASTIC */
      final var suePathChoice = (StochasticPathChoiceConfigurator) configurator.createAndRegisterPathChoice(PathChoice.STOCHASTIC);
      {
        /* Bounded MNL for path choice */
        var choiceModel = (BoundedMultinomialLogitConfigurator) suePathChoice.createAndRegisterChoiceModel(ChoiceModel.BOUNDED_MNL);
        choiceModel.setScalingFactor(10);  // we go for sharp perceived cost for challenging convergence
        choiceModel.setDelta(0.5);        // we set bound with 0.5h of travel time difference for a path to be considered
        // by not setting a fixed od path set (suePathChoice.setFixedOdPathMatrix(...)), it is assumed we want a dynamic path set

        // You can add your own custom filters alternatively, the one below being identical to the predefined max overlap one for 80%
        final var MAX_OVERLAP = 0.6;
        suePathChoice.getPathFilter().addCustomFilter(
                (p, paths) -> paths.stream().noneMatch(pAlt -> {
                  var factor = PathUtils.getOverlapFactor(p, pAlt);
                  if(factor > MAX_OVERLAP){
                    LOGGER.info(String.format("OVERLAP TOO HIGH %s", factor));
                  }else{
                    LOGGER.info(String.format("OVERLAP OK %s", factor));
                  };
                  return factor > MAX_OVERLAP;}));
      }

      /* OUTPUT CONFIG */
      configurator.activateOutput(OutputType.LINK);
      configurator.registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      /* GAP AND CONVERGENCE */
      configurator.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      configurator.getGapFunction().getStopCriterion().setMaxIterations(1000);

      /* BUILD AND EXECUTE */
      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

      testStochasticOutputs(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM multi-destination path based assignment");
    }
  }

  /**
   * Test sLTM bush-origin-based assignment on above network for a point queue model
   */
  @Test
  public void sLtmPointQueueBushOriginBasedAssignmentTest() {
    try {

      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      sLTMBuilder.getConfigurator().disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      sLTMBuilder.getConfigurator().activateDetailedLogging(false);
      
      /* ORIGIN BASED */
      sLTMBuilder.getConfigurator().setType(StaticLtmType.ORIGIN_BUSH_BASED);

      sLTMBuilder.getConfigurator().activateOutput(OutputType.LINK);
      sLTMBuilder.getConfigurator().registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(1000);
      sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

      testDeterministicOutputs(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM bush based assignment");
    }
  }

  /**
   * Test sLTM bush-destination-based assignment on above network for a point queue model
   */
  @Test
  public void sLtmPointQueueBushDestinationBasedAssignmentTest() {
    try {

      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      var sLtmConfig = sLTMBuilder.getConfigurator();
      sLtmConfig.disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      sLtmConfig.activateDetailedLogging(false);
      
      /* DESTINATION BASED */
      sLtmConfig.setType(StaticLtmType.DESTINATION_BUSH_BASED);

      sLtmConfig.activateOutput(OutputType.LINK);
      sLtmConfig.registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(1000);
      sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

      testDeterministicOutputs(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM bush based assignment");
    }
  }  

}
