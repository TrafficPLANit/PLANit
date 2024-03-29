package org.goplanit.test.sltm;

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
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
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

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the sLTM assignment basic functionality (route choice) with multiple origin-destinations
 * 
 * @author markr
 *
 */
public class sLtmAssignmentMultiOdTest {

  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  private final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("sLtmAssignmentMultiOdTest");

  /** the logger */
  private static Logger LOGGER = null;

  /**
   * Create demands an populate with OD DEMANDS 2000 A->A`, 2000 A->A``, and, 2000 A```->A`, 2000 A```->A``
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
    odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A`"), 2000.0);
    odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A``"), 2000.0);
    odDemands.setValue(odZones.getByXmlId("A```"), odZones.getByXmlId("A`"), 2000.0);
    odDemands.setValue(odZones.getByXmlId("A```"), odZones.getByXmlId("A``"), 2000.0);
    demands.registerOdDemandPcuHour(demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

    return demands;
  }

  private void testOutputs(StaticLtm sLTM) {
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
    double outflow15 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("15").getLinkSegmentAb());

    assertTrue(Precision.smallerEqual(outflow4, 4000));
    assertTrue(Precision.smallerEqual(outflow8, 4000));

    assertEquals(outflow0, 4000, Precision.EPSILON_3);
    assertEquals(outflow15, 4000, Precision.EPSILON_3);
    assertEquals(outflow1, 4523, 1);
    assertEquals(outflow2, 1500.0, Precision.EPSILON_3);
    assertEquals(outflow3, outflow2, Precision.EPSILON_3);
    // assertEquals(outflow4, 3750, 1); do not test because non-uniqueness of unsaturated flows under triangular FD
    assertEquals(outflow5, 3190, 1);
    assertEquals(outflow6, 1500.0, Precision.EPSILON_3);
    assertEquals(outflow7, outflow6, Precision.EPSILON_3);
    // assertEquals(outflow8, 3750, 1); do not test because non-uniqueness of unsaturated flows under triangular FD
    assertEquals(outflow9, 3000.0, Precision.EPSILON_3);
    assertEquals(outflow10, 1500.0, Precision.EPSILON_3);
    assertEquals(outflow11, outflow10, Precision.EPSILON_3);
    assertEquals(outflow12, 4500.0, Precision.EPSILON_3);
    //assertEquals(outflow13, 2250, 1); do not test because non-uniqueness of unsaturated flows under triangular FD
    //assertEquals(outflow14, 2250, 1); do not test because non-uniqueness of unsaturated flows under triangular FD

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

    assertEquals(outflow0 + outflow15, inflow1 + inflow5, Precision.EPSILON_6);
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
      LOGGER = Logging.createLogger(sLtmAssignmentMultiOdTest.class);
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
    // Demand AA'  = 2000
    // Demand AA'' = 2000
    // Demand A'''A' = 2000
    // Demand A'''A'' = 2000
    //
    // [0,1,4,5,8,12,15] = 8000 capacity
    // [2,4,6,8,13] = 4000 capacity
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
    //           1  / \   5
    //           15/   \ 0
    //           *(13)(0)*
    //          A```     A
    
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
        node.setPosition(geoFactory.createPoint(new Coordinate(2500, 0)));
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
        // 13
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("13");
        node.setPosition(geoFactory.createPoint(new Coordinate(1500, 0)));         
      }
                     
      
      Nodes nodes = networkLayer.getNodes();
      MacroscopicLinks links = networkLayer.getLinks();
      //links
      links.getFactory().registerNew(nodes.getByXmlId("0"), nodes.getByXmlId("1"), 1, true).setXmlId("0");
      links.getFactory().registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("2"), 1, true).setXmlId("1");
      links.getFactory().registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("3"), 1, true).setXmlId("2");     
      links.getFactory().registerNew(nodes.getByXmlId("3"), nodes.getByXmlId("4"), 1, true).setXmlId("3");
      links.getFactory().registerNew(nodes.getByXmlId("4"), nodes.getByXmlId("5"), 1, true).setXmlId("4");      
      links.getFactory().registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("6"), 1, true).setXmlId("5");
      links.getFactory().registerNew(nodes.getByXmlId("6"), nodes.getByXmlId("7"), 1, true).setXmlId("6");         
      links.getFactory().registerNew(nodes.getByXmlId("7"), nodes.getByXmlId("8"), 1, true).setXmlId("7");
      links.getFactory().registerNew(nodes.getByXmlId("8"), nodes.getByXmlId("9"), 1, true).setXmlId("8");
      links.getFactory().registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("11"), 1, true).setXmlId("9");
      links.getFactory().registerNew(nodes.getByXmlId("6"), nodes.getByXmlId("10"), 1, true).setXmlId("10");
      links.getFactory().registerNew(nodes.getByXmlId("10"), nodes.getByXmlId("11"), 1, true).setXmlId("11");
      links.getFactory().registerNew(nodes.getByXmlId("11"), nodes.getByXmlId("12"), 1, true).setXmlId("12");
      links.getFactory().registerNew(nodes.getByXmlId("12"), nodes.getByXmlId("4"), 1, true).setXmlId("13");
      links.getFactory().registerNew(nodes.getByXmlId("12"), nodes.getByXmlId("8"), 1, true).setXmlId("14");
      links.getFactory().registerNew(nodes.getByXmlId("13"), nodes.getByXmlId("1"), 1, true).setXmlId("15");      
      
      
      /* capacities the same (500), difference is in number of lanes applied), two types, one of 3 lanes (bottleneck) and one of 16 lanes (not bottleneck) */
      MacroscopicLinkSegmentTypes linkTypes = networkLayer.getLinkSegmentTypes();
      linkTypes.getFactory().registerNew("500_lane", 500, 180, network.getModes().getFirst()).setXmlId("500_lane");
      
      var linkType= linkTypes.getByXmlId("500_lane");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("0"), linkType, true, true).setNumberOfLanes(16).setXmlId("0");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("1"), linkType, true, true).setNumberOfLanes(16).setXmlId("1");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("2"), linkType, true, true).setNumberOfLanes(8).setXmlId("2");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("3"), linkType, true, true).setNumberOfLanes(3).setXmlId("3");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("4"), linkType, true, true).setNumberOfLanes(16).setXmlId("4");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("5"), linkType, true, true).setNumberOfLanes(16).setXmlId("5");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("6"), linkType, true, true).setNumberOfLanes(8).setXmlId("6");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("7"), linkType, true, true).setNumberOfLanes(3).setXmlId("7");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("8"), linkType, true, true).setNumberOfLanes(16).setXmlId("8");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("9"), linkType, true, true).setNumberOfLanes(6).setXmlId("9");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("10"), linkType, true, true).setNumberOfLanes(6).setXmlId("10");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("11"), linkType, true, true).setNumberOfLanes(3).setXmlId("11");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("12"), linkType, true, true).setNumberOfLanes(16).setXmlId("12");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("13"), linkType, true, true).setNumberOfLanes(8).setXmlId("13");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("14"), linkType, true, true).setNumberOfLanes(8).setXmlId("14");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("15"), linkType, true, true).setNumberOfLanes(16).setXmlId("15");
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.getOdZones().getFactory().registerNew().setXmlId("A");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A`");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A``");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A```");
           
      zoning.getOdConnectoids().getFactory().registerNew(nodes.getByXmlId("0"),  zoning.getOdZones().getByXmlId("A"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(nodes.getByXmlId("5"),  zoning.getOdZones().getByXmlId("A`"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(nodes.getByXmlId("9"),  zoning.getOdZones().getByXmlId("A``"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(nodes.getByXmlId("13"),  zoning.getOdZones().getByXmlId("A```"), 0);
                      
    }catch(Exception e) {
      e.printStackTrace();
      fail("initialise");
    }
  }
  //@formatter:on

  /**
   * Test sLTM bush-origin-based assignment on above network for a point queue model
   */
  @Test
  public void sLtmPointQueueBushOringinBasedAssignmentTest() {
    try {

      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateDetailedLogging(false);
      
      /* ORIGIN BASED */
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).setType(StaticLtmType.ORIGIN_BUSH_BASED);

      sLTMBuilder.getConfigurator().activateOutput(OutputType.LINK);
      sLTMBuilder.getConfigurator().registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_12);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(1000);
      sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

      testOutputs(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM bush based assignment");
    }
  }

  /**
   * Test sLTM bush-origin-based assignment on above network for a point queue model
   */
  @Test
  public void sLtmPointQueueBushDestinationBasedAssignmentTest() {
    try {

      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateDetailedLogging(false);
      
      /* DESTINATION BASED */
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).setType(StaticLtmType.DESTINATION_BUSH_BASED);

      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateOutput(OutputType.LINK);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_12);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(1000);
      sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

      testOutputs(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM bush based assignment");
    }
  }  

}
