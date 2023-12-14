package org.goplanit.test.sltm;

import java.util.logging.Logger;

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
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentTypes;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinks;
import org.goplanit.utils.network.layer.physical.Links;
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

  private final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("sLtmAssignmentSingleOdTest1");

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
    demands.userClasses.getFactory().registerNew("dummyUser", network.getModes().get(PredefinedModeType.CAR), demands.travelerTypes.getFirst());

    /* OD DEMANDS 8000 A->A` */
    OdZones odZones = zoning.getOdZones();
    OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
    odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A``"), 2000.0);
    odDemands.setValue(odZones.getByXmlId("A`"), odZones.getByXmlId("A``"), 2000.0);
    demands.registerOdDemandPcuHour(demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

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
    //
    //          ---*A''(7)
    // (4)   8 /   | 10
    // o------/    o(5)
    // |           |
    // ^ 7         ^ 9
    // |(3)   6    |(2) 5 
    // o------<----o--<--* A'
    // 1\ \___>3_  ^ 4   (6)
    //   \       \ | 
    //  (0)o---o>--o(1)
    //    ^  2a  2b
    //    |0
    // (8)* A
    //
    // cycle occurs in destination based for vertices (3)->(1)->(2)->(3)
    // Bottlenecks: 10, 8 (500 veh/h), 2b (100) others (2000 veh/h)
    
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
      double oneKm = 1;
      double manyKm = 5;
      links.getFactory().registerNew(nodes.getByXmlId("8"), nodes.getByXmlId("0"), oneKm, true).setXmlId("0");
      links.getFactory().registerNew(nodes.getByXmlId("0"), nodes.getByXmlId("3"), oneKm, true).setXmlId("1");
      links.getFactory().registerNew(nodes.getByXmlId("0"), nodes.getByXmlId("0-1"), oneKm, true).setXmlId("2a");
      links.getFactory().registerNew(nodes.getByXmlId("3"), nodes.getByXmlId("1"), oneKm, true).setXmlId("3");
      links.getFactory().registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("2"), oneKm, true).setXmlId("4");      
      links.getFactory().registerNew(nodes.getByXmlId("6"), nodes.getByXmlId("2"), oneKm, true).setXmlId("5");
      links.getFactory().registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("3"), oneKm, true).setXmlId("6");         
      links.getFactory().registerNew(nodes.getByXmlId("3"), nodes.getByXmlId("4"), oneKm, true).setXmlId("7");
      links.getFactory().registerNew(nodes.getByXmlId("4"), nodes.getByXmlId("7"), manyKm, true).setXmlId("8");
      links.getFactory().registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("5"), oneKm, true).setXmlId("9");
      links.getFactory().registerNew(nodes.getByXmlId("5"), nodes.getByXmlId("7"), oneKm, true).setXmlId("10");
      links.getFactory().registerNew(nodes.getByXmlId("0-1"), nodes.getByXmlId("1"), oneKm, true).setXmlId("2b");
      
      
      /* capacities the same (1500), difference is in number of lanes applied) */
      MacroscopicLinkSegmentTypes linkTypes = networkLayer.getLinkSegmentTypes();
      linkTypes.getFactory().registerNew("MainType", 2000, 180, network.getModes().getFirst()).setXmlId("MainType");
      linkTypes.getFactory().registerNew("BottleNeckType", 500, 180, network.getModes().getFirst()).setXmlId("BottleNeckType");
      linkTypes.getFactory().registerNew("SuperBottleNeckType", 100, 180, network.getModes().getFirst()).setXmlId("SuperBottleNeckType");
      
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("0"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("0");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("1"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("1");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("2a"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("2a");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("3"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("3");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("4"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("4");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("5"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("5");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("6"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("6");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("7"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("7");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("8"), linkTypes.getByXmlId("BottleNeckType"), true, true).setNumberOfLanes(1).setXmlId("8");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("9"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(1).setXmlId("9");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("10"), linkTypes.getByXmlId("BottleNeckType"), true, true).setNumberOfLanes(1).setXmlId("10");
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("2b"), linkTypes.getByXmlId("SuperBottleNeckType"), true, true).setNumberOfLanes(1).setXmlId("2b");
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.getOdZones().getFactory().registerNew().setXmlId("A");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A`");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A``");
           
      zoning.getOdConnectoids().getFactory().registerNew(nodes.getByXmlId("8"),  zoning.getOdZones().getByXmlId("A"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(nodes.getByXmlId("6"),  zoning.getOdZones().getByXmlId("A`"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(nodes.getByXmlId("7"),  zoning.getOdZones().getByXmlId("A``"), 0);
                      
    }catch(Exception e) {
      e.printStackTrace();
      fail("initialise");
    }
  }
  //@formatter:on

  /**
   * Test sLTM bush-destination based assignment on above network for a point queue model
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

      sLTMBuilder.getConfigurator().activateOutput(OutputType.LINK);
      sLTMBuilder.getConfigurator().registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.setActivateDetailedLogging(true);
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(1000);
      sLTM.execute();

//      double outflow1 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
//      double outflow5 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
//      double outflow8 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
//      double outflow2 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());
//
//      assertEquals(outflow1, 2333.333333, Precision.EPSILON_6);
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
