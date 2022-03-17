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
 * Test the sLTM assignment basic functionality (route choice) using two consecutive alternatives in the network
 * 
 * @author markr
 *
 */
public class sLtmAssignmentSingleOdTest2 {

  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  private final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("sLtmAssignmentSingleOdTest2");

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
      LOGGER = Logging.createLogger(sLtmAssignmentSingleOdTest2.class);
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
    // Inspired by the network in Bliemer et al (2014). With 8000 demand, but with slightly altered
    //  capacities to ensure both PASs are indeed PASs, in the original work the first PAS would not be used
    //  in a deterministic setting.
    
    // 0=8000
    // 1=5000
    // 2=2500
    // 3=2000
    // 4=5000
    // 5,6=4000
    // 7=3000
    // 8,9=2000
    //
    //             (5)        (6)(7)         (8)
    //              *-->---->---* *-->---------*
    //              |      5    | |      8    |
    //            4 ^         6 V ^ 7         V 9
    //              |           | |           |
    //     (0)      |           | |           |         (4)
    //  A  *-->-----*----->------*---->-------*---------* A'
    //         0   (1)    1     (2)       2  (3)     3

    
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
        node.setPosition(geoFactory.createPoint(new Coordinate(4000, 0)));
        // 5
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("5");
        node.setPosition(geoFactory.createPoint(new Coordinate(1000, 1000)));
        // 6
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("6");
        node.setPosition(geoFactory.createPoint(new Coordinate(1999, 1000)));
        // 7
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("7");
        node.setPosition(geoFactory.createPoint(new Coordinate(2001, 1000)));
        // 8
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("8");
        node.setPosition(geoFactory.createPoint(new Coordinate(3000, 1000)));             
      }
                     
      
      Nodes nodes = networkLayer.getNodes();
      Links links = networkLayer.getLinks();
      //links
      links.getFactory().registerNew(nodes.getByXmlId("0"), nodes.getByXmlId("1"), 1, true).setXmlId("0");
      links.getFactory().registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("2"), 1, true).setXmlId("1");
      links.getFactory().registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("3"), 1, true).setXmlId("2");
      links.getFactory().registerNew(nodes.getByXmlId("3"), nodes.getByXmlId("4"), 1, true).setXmlId("3");
      links.getFactory().registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("5"), 1, true).setXmlId("4");
      links.getFactory().registerNew(nodes.getByXmlId("5"), nodes.getByXmlId("6"), 1, true).setXmlId("5");
      links.getFactory().registerNew(nodes.getByXmlId("6"), nodes.getByXmlId("2"), 1, true).setXmlId("6");
      links.getFactory().registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("7"), 1, true).setXmlId("7");
      links.getFactory().registerNew(nodes.getByXmlId("7"), nodes.getByXmlId("8"), 1, true).setXmlId("8");
      links.getFactory().registerNew(nodes.getByXmlId("8"), nodes.getByXmlId("3"), 1, true).setXmlId("9");
      
      
      MacroscopicLinkSegmentTypes linkTypes = networkLayer.getLinkSegmentTypes();
      linkTypes.getFactory().registerNew("500_per_lane", 500, 180, network.getModes().getFirst()).setXmlId("500_per_lane");
      
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("0"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(16);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("1"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(10);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("2"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(5);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("3"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(4);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("4"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(10);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("5"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(8);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("6"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(8);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("7"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(6);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("8"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(4);
      networkLayer.getLinkSegments().getFactory().registerNew(links.getByXmlId("9"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(4);
      networkLayer.getLinkSegments().forEach(ls -> ls.setXmlId(""+ls.getParentLink().getId()));
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.getOdZones().getFactory().registerNew().setXmlId("A");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A`");
           
      zoning.getOdConnectoids().getFactory().registerNew(nodes.getByXmlId("0"),  zoning.getOdZones().getByXmlId("A"), 0);
      zoning.getOdConnectoids().getFactory().registerNew(nodes.getByXmlId("4"),  zoning.getOdZones().getByXmlId("A`"), 0);
                      
    }catch(Exception e) {
      e.printStackTrace();
      assertFalse(true);
    }
  }
  //@formatter:on

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
      sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

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

      assertEquals(8000, outflow0, Precision.EPSILON_3);
      assertEquals(2484.8053168536194, outflow1, Precision.EPSILON_3);
      assertEquals(1111.111111111111, outflow2, Precision.EPSILON_3);
      assertEquals(2000.0, outflow3, Precision.EPSILON_3);
      assertEquals(3521.575311898768, outflow4, Precision.EPSILON_3);
      assertEquals(outflow4, outflow5, Precision.EPSILON_3);
      assertEquals(1987.8442542873556, outflow6, Precision.EPSILON_3);
      assertEquals(1972.649573691407, outflow7, Precision.EPSILON_3);
      assertEquals(outflow7, outflow8, Precision.EPSILON_3);
      assertEquals(888.8888888888889, outflow9, Precision.EPSILON_3);

      // conectoid edge segments
      double outflow10 = sLTM.getLinkSegmentOutflowsPcuHour()[10]; // A out
      double outflow11 = sLTM.getLinkSegmentOutflowsPcuHour()[11]; // A in
      double outflow12 = sLTM.getLinkSegmentOutflowsPcuHour()[12]; // A' out
      double outflow13 = sLTM.getLinkSegmentOutflowsPcuHour()[13]; // A' in
      assertEquals(outflow10, 8000, Precision.EPSILON_3);
      assertEquals(outflow13, 1999.9999999974425, Precision.EPSILON_3);
      assertEquals(outflow11, 0, Precision.EPSILON_3);
      assertEquals(outflow11, outflow12, Precision.EPSILON_3);

      double inflow0 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("0").getLinkSegmentAb());
      double inflow1 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
      double inflow2 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());
      double inflow3 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("3").getLinkSegmentAb());
      double inflow4 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("4").getLinkSegmentAb());
      double inflow5 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
      double inflow6 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("6").getLinkSegmentAb());
      double inflow7 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("7").getLinkSegmentAb());
      double inflow8 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
      double inflow9 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("9").getLinkSegmentAb());

      assertEquals(inflow0, 8000, Precision.EPSILON_3);
      assertEquals(inflow1, 4478.424688101231, Precision.EPSILON_3);
      assertEquals(inflow2, 2500, Precision.EPSILON_3);
      assertEquals(inflow3, 2000.0, Precision.EPSILON_3);
      assertEquals(inflow4, 3521.575311898768, Precision.EPSILON_3);
      assertEquals(inflow5, inflow4, Precision.EPSILON_3);
      assertEquals(inflow6, inflow5, Precision.EPSILON_3);
      assertEquals(inflow7, 1972.649573691407, Precision.EPSILON_3);
      assertEquals(inflow8, inflow7, Precision.EPSILON_3);
      assertEquals(inflow9, inflow8, Precision.EPSILON_3);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM bush based assignment");
    }
  }

}
