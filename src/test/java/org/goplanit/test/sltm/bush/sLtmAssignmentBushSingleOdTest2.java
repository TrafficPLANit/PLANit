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
import org.goplanit.sdinteraction.smoothing.MSRASmoothing;
import org.goplanit.sdinteraction.smoothing.MSRASmoothingConfigurator;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the sLTM assignment basic functionality (route choice) using two consecutive alternatives in the network
 * 
 * @author markr
 *
 */
public class sLtmAssignmentBushSingleOdTest2 {

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
   */
  private Demands createDemands() {
    Demands demands = new Demands(testToken);
    demands.timePeriods.getFactory().registerNew("dummyTimePeriod", 0, 3600);
    demands.travelerTypes.getFactory().registerNew("dummyTravellerType");
    demands.userClasses.getFactory().registerNew("dummyUser", network.getModes().get(PredefinedModeType.CAR), demands.travelerTypes.getFirst());

    /* OD DEMANDS 8000 A->A` */
    OdZones odZones = zoning.getOdZones();
    OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
    odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A`"), 8000.0);
    demands.registerOdDemandPcuHour(demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

    return demands;
  }

  private void testCongestedOutputs(StaticLtm sLTM) {
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

    // also valid equilibrium result - in this case turn 6->2 is NOT zero flow
    assertEquals(8000, outflow0, Precision.EPSILON_3);
    assertEquals(2500.9205661070932, outflow1, Precision.EPSILON_3);
    assertEquals(1111.111111111111, outflow2, Precision.EPSILON_3);
    assertEquals(2000.0, outflow3, Precision.EPSILON_3);
    assertEquals(3556.00809762169, outflow4, Precision.EPSILON_3);
    assertEquals(outflow4, outflow5, Precision.EPSILON_3);
    assertEquals(2000.7364528856745, outflow6, Precision.EPSILON_3);
    assertEquals(2000.0, outflow7, Precision.EPSILON_3);
    assertEquals(outflow7, outflow8, Precision.EPSILON_3);
    assertEquals(888.8888888888889, outflow9, Precision.EPSILON_3);

    // below is also valid equilibrium result - in this case turn 6->2 is zero flow and identified as discontinuity cost
//    assertEquals(8000, outflow0, Precision.EPSILON_3);
//    assertEquals(2500, outflow1, Precision.EPSILON_3);
//    assertEquals(1111.111111111111, outflow2, Precision.EPSILON_3);
//    assertEquals(2000.0, outflow3, Precision.EPSILON_3);
//    assertEquals(3681.7978920214005, outflow4, Precision.EPSILON_3);
//    assertEquals(outflow4, outflow5, Precision.EPSILON_3);
//    assertEquals(3000, outflow6, Precision.EPSILON_3);
//    assertEquals(2000.0, outflow7, Precision.EPSILON_3);
//    assertEquals(outflow7, outflow8, Precision.EPSILON_3);
//    assertEquals(888.8888888888889, outflow9, Precision.EPSILON_3);

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

    // goes with other possible solution above
    assertEquals(inflow0, 8000, Precision.EPSILON_3);
    assertEquals(inflow1, 4443.9919023783095, Precision.EPSILON_3);
    assertEquals(inflow2, 2500, Precision.EPSILON_3);
    assertEquals(inflow3, 2000.0, Precision.EPSILON_3);
    assertEquals(inflow4, 3556.008097622691, Precision.EPSILON_3);
    assertEquals(inflow5, inflow4, Precision.EPSILON_3);
    assertEquals(inflow6, inflow5, Precision.EPSILON_3);
    assertEquals(inflow7, 2001.6570189879562, Precision.EPSILON_3);
    assertEquals(inflow8, 2000.0, Precision.EPSILON_3);
    assertEquals(inflow9, inflow8, Precision.EPSILON_3);

    // goes with other possible solution above
//    assertEquals(inflow0, 8000, Precision.EPSILON_3);
//    assertEquals(inflow1, 4318.202107978599, Precision.EPSILON_3);
//    assertEquals(inflow2, 2500, Precision.EPSILON_3);
//    assertEquals(inflow3, 2000.0, Precision.EPSILON_3);
//    assertEquals(inflow4, 3681.7978920214005, Precision.EPSILON_3);
//    assertEquals(inflow5, inflow4, Precision.EPSILON_3);
//    assertEquals(inflow6, inflow5, Precision.EPSILON_3);
//    assertEquals(inflow7, 3000.0, Precision.EPSILON_3);
//    assertEquals(inflow8, 2000.0, Precision.EPSILON_3);
//    assertEquals(inflow9, inflow8, Precision.EPSILON_3);

    double demand0 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("0").getLinkSegmentAb());
    double demand1 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double demand2 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());
    double demand3 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("3").getLinkSegmentAb());
    double demand4 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("4").getLinkSegmentAb());
    double demand5 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
    double demand6 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("6").getLinkSegmentAb());
    double demand7 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("7").getLinkSegmentAb());
    double demand8 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
    double demand9 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("9").getLinkSegmentAb());

    assertEquals(demand0, 8000, Precision.EPSILON_3);
    assertEquals(demand4, demand5, Precision.EPSILON_3);
    assertEquals(demand5, demand6, Precision.EPSILON_3);
    assertEquals(demand1+demand4, demand0, Precision.EPSILON_3);
    assertEquals(demand2 + demand7, demand0, Precision.EPSILON_3);
    assertEquals(demand7, demand8, Precision.EPSILON_3);
    assertEquals(demand8, demand9, Precision.EPSILON_3);
    assertEquals(demand0, demand3, Precision.EPSILON_3);
  }

  private void testUncongestedOutputs(StaticLtm sLTM) {
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

    // not symmetric because of different lane/capacity distributions (detours have on average less capacity)
    assertEquals(2000, outflow0, Precision.EPSILON_3);
    assertEquals(1077.396, outflow1, Precision.EPSILON_3);
    assertEquals(1055.919, outflow2, Precision.EPSILON_3);
    assertEquals(2000.0, outflow3, Precision.EPSILON_3);
    assertEquals(922.604, outflow4, Precision.EPSILON_3);
    assertEquals(outflow4, outflow5, Precision.EPSILON_3);
    assertEquals(outflow4, outflow6, Precision.EPSILON_3);
    assertEquals(944.081, outflow7, Precision.EPSILON_3);
    assertEquals(outflow7, outflow8, Precision.EPSILON_3);
    assertEquals(outflow7, outflow9, Precision.EPSILON_3);

    // conectoid edge segments
    double outflow10 = sLTM.getLinkSegmentOutflowsPcuHour()[10]; // A out
    double outflow11 = sLTM.getLinkSegmentOutflowsPcuHour()[11]; // A in
    double outflow12 = sLTM.getLinkSegmentOutflowsPcuHour()[12]; // A' out
    double outflow13 = sLTM.getLinkSegmentOutflowsPcuHour()[13]; // A' in
    assertEquals(outflow10, 2000, Precision.EPSILON_3);
    assertEquals(outflow13, 2000, Precision.EPSILON_3);
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

    assertEquals(inflow0, outflow0, Precision.EPSILON_3);
    assertEquals(inflow1, outflow1, Precision.EPSILON_3);
    assertEquals(inflow2, outflow2, Precision.EPSILON_3);
    assertEquals(inflow3, outflow3, Precision.EPSILON_3);
    assertEquals(inflow4, outflow4, Precision.EPSILON_3);
    assertEquals(inflow5, inflow4, Precision.EPSILON_3);
    assertEquals(inflow6, inflow5, Precision.EPSILON_3);
    assertEquals(inflow7, outflow7, Precision.EPSILON_3);
    assertEquals(inflow8, inflow7, Precision.EPSILON_3);
    assertEquals(inflow9, inflow8, Precision.EPSILON_3);

    double demand0 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("0").getLinkSegmentAb());
    double demand1 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("1").getLinkSegmentAb());
    double demand2 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("2").getLinkSegmentAb());
    double demand3 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("3").getLinkSegmentAb());
    double demand4 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("4").getLinkSegmentAb());
    double demand5 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("5").getLinkSegmentAb());
    double demand6 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("6").getLinkSegmentAb());
    double demand7 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("7").getLinkSegmentAb());
    double demand8 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("8").getLinkSegmentAb());
    double demand9 = sLTM.getLinkSegmentUnconstrainedFlowPcuHour(networkLayer.getLinks().getByXmlId("9").getLinkSegmentAb());

    assertEquals(demand0, 2000, Precision.EPSILON_3);
    assertEquals(demand4, demand5, Precision.EPSILON_3);
    assertEquals(demand5, demand6, Precision.EPSILON_3);
    assertEquals(demand1 + demand4, demand0, Precision.EPSILON_3);
    assertEquals(demand2 + demand7, demand0, Precision.EPSILON_3);
    assertEquals(demand7, demand8, Precision.EPSILON_3);
    assertEquals(demand8, demand9, Precision.EPSILON_3);
    assertEquals(demand0, demand3, Precision.EPSILON_3);
  }

  /**
   * {@inheritDoc}
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(sLtmAssignmentBushSingleOdTest2.class);
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
    // Inspired by the network in Bliemer et al (2014). With 8000 demand, but with slightly altered
    //  capacities to ensure both PASs are indeed PASs, in the original work the first PAS would not be used
    //  in a deterministic setting.
    //
    // each detour is equally long as the "normal route", i.e., 1 km total.
    
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
      var carMode = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
      networkLayer = network.getTransportLayers().getFactory().registerNew(carMode);

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
      MacroscopicLinks links = networkLayer.getLinks();
      var linkFactory = links.getFactory();
      //links
      final double oneKm = 1.0;
      final double oneThirdKm = oneKm/3;
      linkFactory.registerNew(nodes.getByXmlId("0"), nodes.getByXmlId("1"), oneKm, true).setXmlId("0");
      linkFactory.registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("2"), oneKm, true).setXmlId("1");
      linkFactory.registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("3"), oneKm, true).setXmlId("2");
      linkFactory.registerNew(nodes.getByXmlId("3"), nodes.getByXmlId("4"), oneKm, true).setXmlId("3");
      linkFactory.registerNew(nodes.getByXmlId("1"), nodes.getByXmlId("5"), oneThirdKm, true).setXmlId("4");
      linkFactory.registerNew(nodes.getByXmlId("5"), nodes.getByXmlId("6"), oneThirdKm, true).setXmlId("5");
      linkFactory.registerNew(nodes.getByXmlId("6"), nodes.getByXmlId("2"), oneThirdKm, true).setXmlId("6");
      linkFactory.registerNew(nodes.getByXmlId("2"), nodes.getByXmlId("7"), oneThirdKm, true).setXmlId("7");
      linkFactory.registerNew(nodes.getByXmlId("7"), nodes.getByXmlId("8"), oneThirdKm, true).setXmlId("8");
      linkFactory.registerNew(nodes.getByXmlId("8"), nodes.getByXmlId("3"), oneThirdKm, true).setXmlId("9");
      
      
      MacroscopicLinkSegmentTypes linkTypes = networkLayer.getLinkSegmentTypes();
      var linkType = linkTypes.getFactory().registerNew(
          "500_per_lane", 500, 180, carMode);
      linkType.setXmlId("500_per_lane");
      linkType.getAccessProperties(carMode).setCriticalSpeedKmH(carMode.getMaximumSpeedKmH() * 0.75);

      var linkSegmentFactory = networkLayer.getLinkSegments().getFactory();
      linkSegmentFactory.registerNew(links.getByXmlId("0"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(16);
      linkSegmentFactory.registerNew(links.getByXmlId("1"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(10);
      linkSegmentFactory.registerNew(links.getByXmlId("2"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(5);
      linkSegmentFactory.registerNew(links.getByXmlId("3"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(4);
      linkSegmentFactory.registerNew(links.getByXmlId("4"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(10);
      linkSegmentFactory.registerNew(links.getByXmlId("5"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(8);
      linkSegmentFactory.registerNew(links.getByXmlId("6"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(8);
      linkSegmentFactory.registerNew(links.getByXmlId("7"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(6);
      linkSegmentFactory.registerNew(links.getByXmlId("8"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(4);
      linkSegmentFactory.registerNew(links.getByXmlId("9"), linkTypes.getByXmlId("500_per_lane"), true, true).setNumberOfLanes(4);
      networkLayer.getLinkSegments().forEach(ls -> ls.setXmlId(""+ls.getParent().getId()));
              
      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      var zoneFactory = zoning.getOdZones().getFactory();
      zoneFactory.registerNew().setXmlId("A");
      zoneFactory.registerNew().setXmlId("A`");

      var connectoidFactory = zoning.getOdConnectoids().getFactory();
      connectoidFactory.registerNew(nodes.getByXmlId("0"),  zoning.getOdZones().getByXmlId("A"), 0);
      connectoidFactory.registerNew(nodes.getByXmlId("4"),  zoning.getOdZones().getByXmlId("A`"), 0);
                      
    }catch(Exception e) {
      e.printStackTrace();
      fail("initialise");
    }
  }
  //@formatter:on

  /**
   * Test sLTM bush-based assignment on above network for a point queue model
   */
  @Test
  public void sLtmPointQueueBushDestinationBasedAssignmentTest() {
    try {

      /* OD DEMANDS 8000 A->A` */
      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      sLTMBuilder.getConfigurator().disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);

      //var fixedStepSmoothing = (FixedStepSmoothingConfigurator) sLTMBuilder.getConfigurator().createAndRegisterSmoothing(Smoothing.FIXED_STEP);
      //fixedStepSmoothing.setStepSize(0.1);
      var msraSmoothing = (MSRASmoothingConfigurator) sLTMBuilder.getConfigurator().createAndRegisterSmoothing(Smoothing.MSRA);

      /* DESTINATION BASED */
      sLTMBuilder.getConfigurator().setType(StaticLtmType.DESTINATION_BUSH_BASED);

      sLTMBuilder.getConfigurator().activateOutput(OutputType.LINK);
      sLTMBuilder.getConfigurator().registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(1000);
      sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

      testCongestedOutputs(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM bush based assignment");
    }
  }

  /**
   * Test sLTM conjugate bush-based assignment on above network for a point queue model for congested demands
   */
  @Test
  public void sLtmPointQueueBushConjugateCongestedAssignmentTest() {
    try {

      /* OD DEMANDS 8000 A->A` */
      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      sLTMBuilder.getConfigurator().createAndRegisterFundamentalDiagram(FundamentalDiagram.QUADRATIC_LINEAR);
      sLTMBuilder.getConfigurator().disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);

//      var smoothing = (MSRASmoothingConfigurator) sLTMBuilder.getConfigurator().createAndRegisterSmoothing(Smoothing.MSRA);
//      smoothing.setKappaStep(1);
//      smoothing.setGammaStep(-0.1);

      var smoothing = (FixedStepSmoothingConfigurator) sLTMBuilder.getConfigurator().createAndRegisterSmoothing(Smoothing.FIXED_STEP);
      smoothing.setStepSize(1);

      /* DESTINATION BASED */
      sLTMBuilder.getConfigurator().setType(StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED);

      sLTMBuilder.getConfigurator().activateOutput(OutputType.LINK);
      sLTMBuilder.getConfigurator().registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_12);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(100);
      sLTM.setActivateDetailedLogging(true);

      sLTM.addTrackOdForLoggingByXmlId("A","A`");

      sLTM.execute();

      testCongestedOutputs(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM congested bush based assignment");
    }
  }

  /**
   * Test sLTM conjugate bush-based assignment on above network for a point queue model for uncongested demands
   */
  @Test
  public void sLtmPointQueueBushConjugateUncongestedAssignmentTest() {
    try {

      /* OD DEMANDS 2000 (instead of original 8000) A->A` */
      Demands demands = createDemands();
      demands.getFirst().multiply(0.25); // make uncongested

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(
          network.getIdGroupingToken(), null, demands, zoning, network);
      sLTMBuilder.getConfigurator().createAndRegisterFundamentalDiagram(FundamentalDiagram.QUADRATIC_LINEAR);
      sLTMBuilder.getConfigurator().disableLinkStorageConstraints(
          StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);

      var fixedStepSmoothing = (FixedStepSmoothingConfigurator) sLTMBuilder.getConfigurator().createAndRegisterSmoothing(Smoothing.FIXED_STEP);
      fixedStepSmoothing.setStepSize(1);
//      var msraSmoothing = (MSRASmoothingConfigurator)
//          sLTMBuilder.getConfigurator().createAndRegisterSmoothing(Smoothing.MSRA);

      /* DESTINATION BASED */
      sLTMBuilder.getConfigurator().setType(StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED);

      sLTMBuilder.getConfigurator().activateOutput(OutputType.LINK);
      sLTMBuilder.getConfigurator().registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(15);
      sLTM.setActivateDetailedLogging(true);

      sLTM.addTrackOdForLoggingByXmlId("A","A`");

      sLTM.execute();

      testUncongestedOutputs(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM uncongested bush based assignment");
    }
  }

}
