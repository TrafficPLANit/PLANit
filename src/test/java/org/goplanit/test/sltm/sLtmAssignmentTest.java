package org.goplanit.test.sltm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
 * Test the sLTM assignment basic functionality (route choice)
 * 
 * @author markr
 *
 */
public class sLtmAssignmentTest {

  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  private final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("sLtmAssignmentTest");

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

    /* OD DEMANDS 8000 A->A` */
    OdZones odZones = zoning.getOdZones();
    OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
    odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A`"), 8000.0);
    demands.registerOdDemandPcuHour(demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

    return demands;
  }

  /**
   * {@inheritDoc}
   */
  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(sLtmAssignmentTest.class);
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
    // Inspired by the network in Raadsen and Bliemer (2021), but not identical since we use three separate links for the 
    // alternative routes and capacities might be slightly different as well. Link 2 is the bottleneck with the middle route
    // being shorter yet having the least capacity
    //
    //            4 *----->------* 5
    //              |     4      |
    //            3 ^          5 V
    //              |            |
    //     0        1            2        3
    //  A  *-->-----*----->------*---->---* A'
    //         0    |     1      |    2
    //              V 6        8 ^
    //              |            |
    //            6 *----->------* 7
    //                    7
    
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
        node.setPosition(geoFactory.createPoint(new Coordinate(0, 0)));
        // 1
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("1");
        node.setPosition(geoFactory.createPoint(new Coordinate(1000, 0)));
        // 2
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("2");
        node.setPosition(geoFactory.createPoint(new Coordinate(2000, 0)));
        // 3
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("3");
        node.setPosition(geoFactory.createPoint(new Coordinate(3000, 0)));   
        // 4
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("4");
        node.setPosition(geoFactory.createPoint(new Coordinate(1000, 1000)));
        // 5
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("5");
        node.setPosition(geoFactory.createPoint(new Coordinate(2000, 1000)));
        // 6
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("6");
        node.setPosition(geoFactory.createPoint(new Coordinate(1000, -1000)));
        // 7
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("7");
        node.setPosition(geoFactory.createPoint(new Coordinate(2000, -1000)));         
      }
                     
      
      Nodes nodes = networkLayer.getNodes();
      Links links = networkLayer.getLinks();
      //links
      links.getFactory().registerNew(nodes.getByXmlId("0"), nodes.getByXmlId("1"), 1, true).setXmlId("0");
      links.getFactory().registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("2"), 1, true).setXmlId("1");
      links.getFactory().registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("3"), 1, true).setXmlId("2");     
      links.getFactory().registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("4"), 1, true).setXmlId("3");
      links.getFactory().registerNew(nodes.getByXmlId("4"), nodes.getByXmlId("5"), 1, true).setXmlId("4");      
      links.getFactory().registerNew(nodes.getByXmlId("5"), nodes.getByXmlId("2"), 1, true).setXmlId("5");
      links.getFactory().registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("6"), 1, true).setXmlId("6");         
      links.getFactory().registerNew(nodes.getByXmlId("6"), nodes.getByXmlId("7"), 1, true).setXmlId("7");
      links.getFactory().registerNew(nodes.getByXmlId("7"), nodes.getByXmlId("2"), 1, true).setXmlId("8");
      
      
      /* capacities the same (1500), difference is in number of lanes applied) */
      MacroscopicLinkSegmentTypes linkTypes = networkLayer.getLinkSegmentTypes();
      linkTypes.getFactory().registerNew("MainType", 2000, 180, network.getModes().getFirst()).setXmlId("MainType");
      linkTypes.getFactory().registerNew("BottleNeckType", 7000/4.0, 180, network.getModes().getFirst()).setXmlId("BottleNeckType");
      
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("0"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(4);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("1"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(4);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("2"), linkTypes.getByXmlId("BottleNeckType"), true, true).setNumberOfLanes(4);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("3"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(4);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("4"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(4);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("5"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(4);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("6"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(4);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("7"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(4);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("8"), linkTypes.getByXmlId("MainType"), true, true).setNumberOfLanes(4);
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.odZones.getFactory().registerNew().setXmlId("A");
      zoning.odZones.getFactory().registerNew().setXmlId("A`");
           
      zoning.odConnectoids.getFactory().registerNew(nodes.getByXmlId("0"),  zoning.getOdZones().getByXmlId("A"), 0);
      zoning.odConnectoids.getFactory().registerNew(nodes.getByXmlId("3"),  zoning.getOdZones().getByXmlId("A`"), 0);
                      
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

      /* OD DEMANDS 8000 A->A` */
      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateDetailedLogging(true);
      ((StaticLtmConfigurator) sLTMBuilder.getConfigurator()).activateBushBased(false);

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.execute();

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

      /* OD DEMANDS 8000 A->A` */
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
      sLTM.execute();

      double outflow1 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
      double outflow5 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
      double outflow8 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
      double outflow2 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());

      assertEquals(outflow1, 2333.333333, Precision.EPSILON_6);
      assertEquals(outflow5, 2333.333333, Precision.EPSILON_6);
      assertEquals(outflow8, 2333.333333, Precision.EPSILON_6);
      assertEquals(outflow2, 7000, Precision.EPSILON_6);

      double inflow0 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("0").getLinkSegmentAb());
      double inflow1 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
      double inflow5 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
      double inflow8 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
      double inflow2 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());

      assertEquals(inflow0, 8000, Precision.EPSILON_6);
      assertEquals(inflow1, 2714.529914369357, Precision.EPSILON_6);
      assertEquals(inflow5, 2642.7350425744858, Precision.EPSILON_6);
      assertEquals(inflow8, 2642.7350430561573, Precision.EPSILON_6);
      assertEquals(inflow2, 7000, Precision.EPSILON_6);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM network loading");
    }
  }

}
