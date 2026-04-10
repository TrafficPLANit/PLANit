package org.goplanit.test.sltm.bush;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.assignment.ltm.sltm.input.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.StaticLtmTrafficAssignmentBuilder;
import org.goplanit.assignment.ltm.sltm.common.StaticLtmType;
import org.goplanit.demands.Demands;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.AccessGroupPropertiesFactory;
import org.goplanit.od.demand.OdDemandMatrix;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.sdinteraction.smoothing.FixedStepSmoothingConfigurator;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.PredefinedMode;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the sLTM assignment basic functionality (route choice)
 * 
 * @author markr
 *
 */
public class sLtmAssignmentBushSingleOdTest1 {

  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  private final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("sLtmAssignmentSingleOdTest1");

  /** the logger */
  private static Logger LOGGER = null;


  private Demands createEmptyDemands() {
    Demands demands = new Demands(testToken);
    demands.timePeriods.getFactory().registerNew("dummyTimePeriod", 0, 3600);
    demands.travelerTypes.getFactory().registerNew("dummyTravellerType");
    demands.userClasses.getFactory().registerNew("dummyUser", network.getModes().get(PredefinedModeType.CAR), demands.travelerTypes.getFirst());
    return demands;
  }

  /**
   * Create demands and populate with OD DEMANDS 8000 A->A`
   * 
   * @return created demands
   */
  private Demands createCongestedDemands() {
    Demands demands = createEmptyDemands();

    /* OD DEMANDS 8000 A->A` */
    OdZones odZones = zoning.getOdZones();
    OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
    odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A`"), 8000.0);
    demands.registerOdDemandPcuHour(
        demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

    return demands;
  }

  /**
   * Create demands and populate with uncongested OD DEMANDS
   *
   * @return created demands
   */
  private Demands createUncongestedDemands() {
    Demands demands = createEmptyDemands();

    OdZones odZones = zoning.getOdZones();
    OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
    odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A`"), 6000.0);
    demands.registerOdDemandPcuHour(
        demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

    return demands;
  }

  private void testCongestedOutputs(StaticLtm sLTM) {
    double outflow1 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double outflow5 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
    double outflow8 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
    double outflow2 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());

    assertEquals(2333.333333 ,outflow1, Precision.EPSILON_3);
    assertEquals(2333.333333, outflow5, Precision.EPSILON_3);
    assertEquals( 2333.333333,outflow8, Precision.EPSILON_3);
    assertEquals( 7000,outflow2, Precision.EPSILON_6);

    double inflow0 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("0").getLinkSegmentAb());
    double inflow1 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double inflow5 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
    double inflow8 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
    double inflow2 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());

    assertEquals(8000,inflow0, Precision.EPSILON_6);
    assertEquals( 2666.666,inflow1, Precision.EPSILON_3);
    assertEquals( 2666.666,inflow5, Precision.EPSILON_3);
    assertEquals( 2666.666,inflow8, Precision.EPSILON_3);
    assertEquals( 7000,inflow2, Precision.EPSILON_6);

    double demand0 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("0").getLinkSegmentAb());
    double demand1 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double demand5 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
    double demand8 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
    double demand2 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());

    assertEquals(demand0, inflow0, Precision.EPSILON_6);
    assertEquals(inflow1, demand1, Precision.EPSILON_3);
    assertEquals(inflow5, demand5, Precision.EPSILON_3);
    assertEquals(inflow8, demand8, Precision.EPSILON_3);
    assertEquals(demand2, demand0, Precision.EPSILON_6);
  }

  private void testUncongestedOutputs(StaticLtm sLTM) {
    double outflow1 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double outflow5 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
    double outflow8 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
    double outflow2 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());

    assertEquals(2000.0 ,outflow1, Precision.EPSILON_3);
    assertEquals(outflow1, outflow5, Precision.EPSILON_3);
    assertEquals( outflow1, outflow8, Precision.EPSILON_3);
    assertEquals( outflow1 + outflow8 + outflow5, outflow2, Precision.EPSILON_6);

    double inflow0 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("0").getLinkSegmentAb());
    double inflow1 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double inflow5 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
    double inflow8 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
    double inflow2 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());

    assertEquals(6000, inflow0, Precision.EPSILON_6);
    assertEquals( outflow1, inflow1, Precision.EPSILON_3);
    assertEquals( outflow5, inflow5, Precision.EPSILON_3);
    assertEquals( outflow8, inflow8, Precision.EPSILON_3);
    assertEquals( outflow2, inflow2, Precision.EPSILON_6);

    double demand0 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("0").getLinkSegmentAb());
    double demand1 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double demand5 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
    double demand8 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
    double demand2 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());

    assertEquals(demand0, inflow0, Precision.EPSILON_6);
    assertEquals(inflow1, demand1, Precision.EPSILON_3);
    assertEquals(inflow5, demand5, Precision.EPSILON_3);
    assertEquals(inflow8, demand8, Precision.EPSILON_3);
    assertEquals(demand2, demand0, Precision.EPSILON_6);
  }

  private StaticLtm initialiseSltm(
      StaticLtmType staticLtmType, String fundamentalDiagramType, Demands demands) throws PlanItException {

    /* sLTM - POINT QUEUE */
    StaticLtmTrafficAssignmentBuilder sLTMBuilder =
        new StaticLtmTrafficAssignmentBuilder(
            network.getIdGroupingToken(), null, demands, zoning, network);

    sLTMBuilder.getConfigurator().disableLinkStorageConstraints(
        StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
    sLTMBuilder.getConfigurator().setType(staticLtmType);

    sLTMBuilder.getConfigurator().createAndRegisterFundamentalDiagram(fundamentalDiagramType);

    sLTMBuilder.getConfigurator().activateOutput(OutputType.LINK);
    sLTMBuilder.getConfigurator().registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

    var fixedSmoothing = (FixedStepSmoothingConfigurator) sLTMBuilder.getConfigurator().createAndRegisterSmoothing(Smoothing.FIXED_STEP);
    fixedSmoothing.setStepSize(1);

    sLTMBuilder.getConfigurator().addTrackOdsForLogging(IdMapperType.XML, Pair.of("A","A`"));

    return sLTMBuilder.build();
  }

  /**
   * {@inheritDoc}
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(sLtmAssignmentBushSingleOdTest1.class);
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
    // Inspired by the network in Raadsen and Bliemer (2021), but not identical since we use three separate links for the 
    // alternative routes and capacities might be slightly different as well. Link 2 (7k capacity) is the bottleneck
    // with all routes equal length, all link shave 8k capacity.
    //  - Demand is 8k as well for the congested test
    //  - Demand is 7k with QL diagram and lower speeds for uncongested test
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
      PredefinedMode carMode = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
      networkLayer = network.getTransportLayers().getFactory().registerNew(
          network.getModes().get(PredefinedModeType.CAR));

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
      MacroscopicLinks links = networkLayer.getLinks();
      var linkFactory = links.getFactory();
      //links
      linkFactory.registerNew(nodes.getByXmlId("0"), nodes.getByXmlId("1"), 1, true).setXmlId("0");
      linkFactory.registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("2"), 1, true).setXmlId("1");
      linkFactory.registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("3"), 1, true).setXmlId("2");
      linkFactory.registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("4"), 1.0/3, true).setXmlId("3");
      linkFactory.registerNew(nodes.getByXmlId("4"), nodes.getByXmlId("5"), 1.0/3, true).setXmlId("4");
      linkFactory.registerNew(nodes.getByXmlId("5"), nodes.getByXmlId("2"), 1.0/3, true).setXmlId("5");
      linkFactory.registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("6"), 1.0/3, true).setXmlId("6");
      linkFactory.registerNew(nodes.getByXmlId("6"), nodes.getByXmlId("7"), 1.0/3, true).setXmlId("7");
      linkFactory.registerNew(nodes.getByXmlId("7"), nodes.getByXmlId("2"), 1.0/3, true).setXmlId("8");

      var access = AccessGroupPropertiesFactory.create(130, 80, carMode);
      
      // capacities the same (1500), difference is in number of lanes applied)
      MacroscopicLinkSegmentTypes linkTypes = networkLayer.getLinkSegmentTypes();
      var mainType = linkTypes.getFactory().registerNew("MainType", 2000, 180, network.getModes().getFirst());
      mainType.setXmlId("MainType");
      mainType.setAccessGroupProperties(access);
      var bottleneckType = linkTypes.getFactory().registerNew("BottleNeckType", 7000/4.0, 180, network.getModes().getFirst());
      bottleneckType.setXmlId("BottleNeckType");
      bottleneckType.setAccessGroupProperties(access);

      var linkSegmentFactory = networkLayer.getLinkSegments().getFactory();
      linkSegmentFactory.registerNew(
          links.getByXmlId("0"), linkTypes.getByXmlId("MainType"), true, true).
          setNumberOfLanes(4).setXmlId("0");
      linkSegmentFactory.registerNew(
          links.getByXmlId("1"), linkTypes.getByXmlId("MainType"), true, true).
          setNumberOfLanes(4).setXmlId("1");
      linkSegmentFactory.registerNew(
          links.getByXmlId("2"), linkTypes.getByXmlId("BottleNeckType"), true, true).
          setNumberOfLanes(4).setXmlId("2");
      linkSegmentFactory.registerNew(
          links.getByXmlId("3"), linkTypes.getByXmlId("MainType"), true, true).
          setNumberOfLanes(4).setXmlId("3");
      linkSegmentFactory.registerNew(
          links.getByXmlId("4"), linkTypes.getByXmlId("MainType"), true, true).
          setNumberOfLanes(4).setXmlId("4");
      linkSegmentFactory.registerNew(
          links.getByXmlId("5"), linkTypes.getByXmlId("MainType"), true, true).
          setNumberOfLanes(4).setXmlId("5");
      linkSegmentFactory.registerNew(
          links.getByXmlId("6"), linkTypes.getByXmlId("MainType"), true, true).
          setNumberOfLanes(4).setXmlId("6");
      linkSegmentFactory.registerNew(
          links.getByXmlId("7"), linkTypes.getByXmlId("MainType"), true, true).
          setNumberOfLanes(4).setXmlId("7");
      linkSegmentFactory.registerNew(
          links.getByXmlId("8"), linkTypes.getByXmlId("MainType"), true, true).
          setNumberOfLanes(4).setXmlId("8");
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.getOdZones().getFactory().registerNew().setXmlId("A");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A`");

      var connectoidFactory = zoning.getOdConnectoids().getFactory();
      connectoidFactory.registerNew(zoning.getOdZones().getByXmlId("A"), nodes.getByXmlId("0") , 0);
      connectoidFactory.registerNew(zoning.getOdZones().getByXmlId("A`"), nodes.getByXmlId("3"),   0);
                      
    }catch(Exception e) {
      e.printStackTrace();
      fail("initialise");
    }
  }
  //@formatter:on

  /**
   * Test sLTM conjugate bush-destination based conjugate assignment on above network for a point queue model
   */
  @Test
  public void sLtmPointQueueConjBushCongestedAssignmentTest() {
    try {

      /* OD DEMANDS 8000 A->A` */
      Demands demands = createCongestedDemands();

      var sLTM = initialiseSltm(
          StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED, FundamentalDiagram.QUADRATIC_LINEAR, demands);

      sLTM.setActivateDetailedLogging(true);
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_12);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(100);

      sLTM.execute();

      testCongestedOutputs(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM congested conjugate bush based assignment");
    }
  }

  /**
   * Test sLTM bush-destination based conjugate uncongested assignment on above network for a point queue model
   */
  @Test
  public void sLtmPointQueueConjBushUncongestedAssignmentTest() {
    try {

      /* OD DEMANDS 6000 A->A` */
      Demands demands = createUncongestedDemands();

      var sLTM = initialiseSltm(
          StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED, FundamentalDiagram.QUADRATIC_LINEAR, demands);

      sLTM.setActivateDetailedLogging(true);
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_12);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(40);
      sLTM.execute();

      testUncongestedOutputs(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM uncongested conjugate bush based assignment");
    }
  }

}
