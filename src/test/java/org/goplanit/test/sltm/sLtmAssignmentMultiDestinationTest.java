package org.goplanit.test.sltm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.assignment.ltm.sltm.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.StaticLtmTrafficAssignmentBuilder;
import org.goplanit.demands.Demands;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.od.demand.OdDemandMatrix;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypes;
import org.goplanit.utils.network.layer.physical.Links;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.network.layer.physical.Nodes;
import org.goplanit.utils.zoning.OdZones;
import org.goplanit.zoning.Zoning;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

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
   * Create demands an populate with OD DEMANDS 8000 A->A`
   * 
   * @return created demands
   * @throws PlanItException thrown if error
   */
  private Demands createDemands() throws PlanItException {
    Demands demands = new Demands(testToken);
    demands.timePeriods.createAndRegisterNewTimePeriod("dummyTimePeriod", 0, 3600);
    demands.travelerTypes.createAndRegisterNew("dummyTravellerType");
    demands.userClasses.createAndRegisterNewUserClass("dummyUser", network.getModes().get(PredefinedModeType.CAR), demands.travelerTypes.getFirst());

    /* OD DEMANDS 4000 A->A`, 4000 A->A`` */
    OdZones odZones = zoning.getOdZones();
    OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
    odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A`"), 4000.0);
    odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A``"), 4000.0);
    demands.registerOdDemandPcuHour(demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

    return demands;
  }

  /**
   * {@inheritDoc}
   */
  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(sLtmAssignmentMultiDestinationTest.class);
    }
  }

  /**
   * {@inheritDoc}
   */
  @After
  public void tearDown() {
    Logging.closeLogger(LOGGER);
  }

  //@formatter:off
  @Before
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
    // [0,1,4,5,8,12] = 8000 capacity
    // [2,4,6,8,9,13] = 4000 capacity
    // [9,10] = 3000 capacity
    // [3,7,10,11] = 1500 capacity
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
      Links links = networkLayer.getLinks();
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
      
      
      /* capacities the same (500), difference is in number of lanes applied), two types, one of 3 lanes (bottleneck) and one of 16 lanes (not bottleneck) */
      MacroscopicLinkSegmentTypes linkTypes = networkLayer.getLinkSegmentTypes();
      linkTypes.getFactory().registerNew("500_lane", 500, 180, network.getModes().getFirst()).setXmlId("500_lane");
      
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("0"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(16);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("1"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(16);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("2"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(8);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("3"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(3);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("4"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(16);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("5"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(16);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("6"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(8);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("7"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(3);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("8"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(16);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("9"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(6);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("10"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(6);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("11"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(3);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("12"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(16);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("13"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(8);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("14"), linkTypes.getByXmlId("500_lane"), true, true).setNumberOfLanes(8);
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.odZones.getFactory().registerNew().setXmlId("A");
      zoning.odZones.getFactory().registerNew().setXmlId("A`");
      zoning.odZones.getFactory().registerNew().setXmlId("A``");
           
      zoning.getOdConnectoids().getFactory().registerNew(nodes.getByXmlId("0"),  zoning.getOdZones().getByXmlId("A"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(nodes.getByXmlId("5"),  zoning.getOdZones().getByXmlId("A`"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(nodes.getByXmlId("9"),  zoning.getOdZones().getByXmlId("A``"), 0);
                      
    }catch(Exception e) {
      e.printStackTrace();
      assertFalse(true);
    }
  }
  //@formatter:on

  /**
   * Test sLTM path-based assignment on above network for a point queue model
   */
  @Test
  public void sLtmPointQueuePathBasedAssignmentTest() {
    try {

      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateDetailedLogging(true);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateBushBased(false);

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.execute();

      // TODO: not yet supported

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM network loading");
    }
  }

  /**
   * Test sLTM bush-based assignment on above network for a point queue model
   */
  @Test
  public void sLtmPointQueueBushBasedAssignmentTest() {
    try {

      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateDetailedLogging(false);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateBushBased(true);

      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateOutput(OutputType.LINK);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(1000);
      sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

      /*
       * If splitting rates are kept correctly on the bush it is impossible that flows from destination A' end up at destination A''. Hence, the last liunk before each destination
       * should have less flow than the OD demand of 4k
       */
      double outflow4 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("4").getLinkSegmentAb());
      assertTrue(Precision.smallerEqual(outflow4, 4000));
      double outflow8 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
      assertTrue(Precision.smallerEqual(outflow8, 4000));

      // TODO: When this is properly handled find out the correct answer and put in the rest of the assertions

//      double outflow5 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
//      double outflow8 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
//      double outflow2 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());
//
//      assertEquals(outflow5, 2333.333333, Precision.EPSILON_6);
//      assertEquals(outflow8, 2333.333333, Precision.EPSILON_6);
//      assertEquals(outflow2, 7000, Precision.EPSILON_6);
//
//      double inflow0 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("0").getLinkSegmentAb());
//      double inflow1 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
//      double inflow5 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
//      double inflow8 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
//      double inflow2 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());
//
//      assertEquals(inflow0, 8000, Precision.EPSILON_6);
//      assertEquals(inflow1, 2714.529914369357, Precision.EPSILON_6);
//      assertEquals(inflow5, 2642.7350425744858, Precision.EPSILON_6);
//      assertEquals(inflow8, 2642.7350430561573, Precision.EPSILON_6);
//      assertEquals(inflow2, 7000, Precision.EPSILON_6);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM bush based assignment");
    }
  }

}
