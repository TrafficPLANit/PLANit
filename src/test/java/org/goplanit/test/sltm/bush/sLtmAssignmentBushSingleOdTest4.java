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
import org.goplanit.sdinteraction.smoothing.FixedStepSmoothingConfigurator;
import org.goplanit.sdinteraction.smoothing.MSRASmoothingConfigurator;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
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

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the sLTM assignment basic functionality (route choice) using a single PAS where the merge
 * and diverge are working against each other.
 * <p>
 *   Merge has one congested entry claiming potentially beyond its fair share and one that is not
 *   necessarily congested claiming less than its fair share, resulting in difficult to solve derivatives
 * </p>
 * <p>
 *   Diverge works opposite, getting only congested when moving flow from congested merge towards uncongested
 *   entry of the merge. In doing so the merge's previously congested arm becomes more attractive again.
 * </p>
 * 
 * @author markr
 *
 */
public class sLtmAssignmentBushSingleOdTest4 {

  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  private final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("sLtmAssignmentSingleOdTest4");

  /** the logger */
  private static Logger LOGGER = null;

  /**
   * Create demands an populate with OD DEMANDS 8000 A->A`
   * 
   * @return created demands
   */
  private Demands createDemands() {
    Demands demands = new Demands(testToken);
    demands.timePeriods.getFactory().registerNew("dummyTimePeriod", 0, 3600);
    demands.travelerTypes.getFactory().registerNew("dummyTravellerType");
    demands.userClasses.getFactory().registerNew(
        "dummyUser", network.getModes().get(PredefinedModeType.CAR), demands.travelerTypes.getFirst());

    /* OD DEMANDS 6320 A->A` for bottleneck with 6300 capacity */
    OdZones odZones = zoning.getOdZones();
    OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
    odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A`"), 6320.0);
    demands.registerOdDemandPcuHour(
        demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

    return demands;
  }

  private void testOutputs(StaticLtm sLTM) {
    //TODO
    double outflow0 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("0").getLinkSegmentAb());
    double outflow1 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double outflow2 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());
    double outflow3 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("3").getLinkSegmentAb());
    double outflow4 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("4").getLinkSegmentAb());
    double outflow5 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());

    assertEquals(7421.59977, outflow0, Precision.EPSILON_3);
    assertEquals(4000, outflow1, Precision.EPSILON_3);
    assertEquals(4000, outflow2, Precision.EPSILON_3);
    assertEquals(6000, outflow3, Precision.EPSILON_3);
    assertEquals(2421.599775766677, outflow4, Precision.EPSILON_3);
    assertEquals(2000, outflow5, Precision.EPSILON_3);

    // connectoid edge segments
    double outflow10 = sLTM.getLinkSegmentOutflowsPcuHour()[8]; // A out
    double outflow11 = sLTM.getLinkSegmentOutflowsPcuHour()[9]; // A in
    double outflow12 = sLTM.getLinkSegmentOutflowsPcuHour()[10]; // A' out
    double outflow13 = sLTM.getLinkSegmentOutflowsPcuHour()[11]; // A' in
    assertEquals(outflow10, 8000, Precision.EPSILON_3);
    assertEquals(outflow13, 6000, Precision.EPSILON_3);
    assertEquals(outflow11, 0, Precision.EPSILON_3);
    assertEquals(outflow11, outflow12, Precision.EPSILON_3);

    double inflow0 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("0").getLinkSegmentAb());
    double inflow1 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double inflow2 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());
    double inflow3 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("3").getLinkSegmentAb());
    double inflow4 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("4").getLinkSegmentAb());
    double inflow5 = sLTM.getLinkSegmentInflowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());

    assertEquals(inflow0, 8000, Precision.EPSILON_3);
    assertEquals(inflow1, 5000, Precision.EPSILON_3);
    assertEquals(inflow2, 4000, Precision.EPSILON_3);
    assertEquals(inflow3, 6000, Precision.EPSILON_3);
    assertEquals(inflow4, 2421.599775766677, Precision.EPSILON_3);
    assertEquals(inflow5, inflow4, Precision.EPSILON_3);
  }

  /**
   * {@inheritDoc}
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(sLtmAssignmentBushSingleOdTest4.class);
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
    // 4 + 5 = same length as 2
    //
    // 0,1=7200
    // 1,2,3=6300
    // 2 =6320
    // 5= 3600
    // 4 = 2286 (just below fair share of 5->3)
    //
    //                           (5)
    //                            *--->-------\
    //                           |       5     |
    //                         4 ^             V
    //     (0)                   |            |         (4)
    //  A  *-->-----*----->------*---->-------*---------* A'
    //         0   (1)    1     (2)    2     (3)     3

    
    try {
      // local CRS in meters
      GeometryFactory geoFactory = JTSFactoryFinder.getGeometryFactory();
      
      network = new MacroscopicNetwork(testToken);
      network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
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
        node.setPosition(geoFactory.createPoint(new Coordinate(4000, 0)));
        // 5
        node = networkLayer.getNodes().getFactory().registerNew();
        node.setXmlId("5");
        node.setPosition(geoFactory.createPoint(new Coordinate(2000, 1000)));
      }
                     
      
      Nodes nodes = networkLayer.getNodes();
      MacroscopicLinks links = networkLayer.getLinks();
      var linkFactory = links.getFactory();
      //links
      linkFactory.registerNew(nodes.getByXmlId("0"), nodes.getByXmlId("1"), 1, true).setXmlId("0");
      linkFactory.registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("2"), 1, true).setXmlId("1");
      linkFactory.registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("3"), 1, true).setXmlId("2");
      linkFactory.registerNew(nodes.getByXmlId("3"), nodes.getByXmlId("4"), 1, true).setXmlId("3");
      linkFactory.registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("5"), 0.5, true).setXmlId("4");
      linkFactory.registerNew(nodes.getByXmlId("5"), nodes.getByXmlId("3"), 0.5, true).setXmlId("5");

      MacroscopicLinkSegmentTypes linkTypes = networkLayer.getLinkSegmentTypes();
      var lt_2286 = linkTypes.getFactory().registerNew(
          "2286_per_lane",
          2286,
          180,
          network.getModes().getFirst());
      lt_2286.setXmlId("2286_per_lane");
      var lt_3600 = linkTypes.getFactory().registerNew(
          "3600_per_lane",
          3600,
          180,
          network.getModes().getFirst());
      lt_3600.setXmlId("3600_per_lane");
      var lt_6300 = linkTypes.getFactory().registerNew(
          "6300_per_lane",
          6300,
          180,
          network.getModes().getFirst());
      lt_6300.setXmlId("6300_per_lane");
      var lt_6320 = linkTypes.getFactory().registerNew(
          "6320_per_lane",
          6320,
          180,
          network.getModes().getFirst());
      lt_6320.setXmlId("6320_per_lane");

      var linkSegmentFactory = networkLayer.getLinkSegments().getFactory();
      linkSegmentFactory.registerNew(links.getByXmlId("0"), lt_3600, true, true).setNumberOfLanes(2);
      linkSegmentFactory.registerNew(links.getByXmlId("1"), lt_3600, true, true).setNumberOfLanes(2);
      linkSegmentFactory.registerNew(links.getByXmlId("2"), lt_6320, true, true).setNumberOfLanes(1);
      linkSegmentFactory.registerNew(links.getByXmlId("3"), lt_6300, true, true).setNumberOfLanes(1);
      linkSegmentFactory.registerNew(links.getByXmlId("4"), lt_2286, true, true).setNumberOfLanes(1);
      linkSegmentFactory.registerNew(links.getByXmlId("5"), lt_3600, true, true).setNumberOfLanes(1);
      networkLayer.getLinkSegments().forEach(ls -> ls.setXmlId(""+ls.getParent().getId()));
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.getOdZones().getFactory().registerNew().setXmlId("A");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A`");

      var connectoidsFactory = zoning.getOdConnectoids().getFactory();
      connectoidsFactory.registerNew(nodes.getByXmlId("0"),  zoning.getOdZones().getByXmlId("A"), 0);
      connectoidsFactory.registerNew(nodes.getByXmlId("4"),  zoning.getOdZones().getByXmlId("A`"), 0);
                      
    }catch(Exception e) {
      e.printStackTrace();
      fail("initialise");
    }
  }
  //@formatter:on

  /**
  /**
   * Test sLTM conjugate bush-based assignment on above network for a point queue model
   */
  @Test
  public void sLtmPointQueueConjugateBushDestinationBasedAssignmentTest() {
    try {

      /* OD DEMANDS A->A` */
      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder =
              new StaticLtmTrafficAssignmentBuilder(
                      network.getIdGroupingToken(), null, demands, zoning, network);
      sLTMBuilder.getConfigurator().disableLinkStorageConstraints(
              StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);

      // NEWELL diagram
      sLTMBuilder.getConfigurator().createAndRegisterFundamentalDiagram(FundamentalDiagram.QUADRATIC_LINEAR);

      var fixedStepSmoothing = (FixedStepSmoothingConfigurator)
              sLTMBuilder.getConfigurator().createAndRegisterSmoothing(Smoothing.FIXED_STEP);
      fixedStepSmoothing.setStepSize(1);

//      var msraSmoothing = (MSRASmoothingConfigurator)
//          sLTMBuilder.getConfigurator().createAndRegisterSmoothing(Smoothing.MSRA);

      /* CONJUGATE DESTINATION BASED */
      sLTMBuilder.getConfigurator().setType(StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED);

      sLTMBuilder.getConfigurator().addTrackOdsForLogging(IdMapperType.XML, Pair.of("A","A`"));

      sLTMBuilder.getConfigurator().activateOutput(OutputType.LINK);
      sLTMBuilder.getConfigurator().registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(100);
      sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

      testOutputs(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM conjugate bush based assignment");
    }
  }

}
