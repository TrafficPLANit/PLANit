package org.goplanit.test.sltm.bush;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.assignment.ltm.sltm.input.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.StaticLtmTrafficAssignmentBuilder;
import org.goplanit.assignment.ltm.sltm.common.StaticLtmType;
import org.goplanit.demands.Demands;
import org.goplanit.logging.Logging;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.zoning.zonetozone.OdDemandMatrix;
import org.goplanit.zoning.zonetozone.OdDemands;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.output.property.OutputPropertyType;
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

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test travel times for a corridor network for different sLTM implementations and cost functions
 */
public class sLtmTravelTimeTest {

  /** the logger */
  private static Logger LOGGER = null;

  private static final double FREEFLOW_TT_KM = 1/130.0; // car freespeed = 130km/h

  private MacroscopicNetwork network;
  private MacroscopicNetworkLayer networkLayer;
  private Zoning zoning;

  private final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("sLtmTravelTimeTest");


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

    /* OD DEMANDS 8000 A->A` */
    OdZones odZones = zoning.getOdZones();
    OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
    odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A`"), 8000.0);
    demands.registerOdDemandPcuHour(
            demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

    return demands;
  }


  /**
   * Configure and run test
   *
   */
  private void runTest() {
    try {

      Demands demands = createDemands();

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder =
              new StaticLtmTrafficAssignmentBuilder(
                      network.getIdGroupingToken(), null, demands, zoning, network);
      sLTMBuilder.getConfigurator().disableLinkStorageConstraints(
              StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);

      /* DESTINATION BASED */
      var slTMConfigurator = sLTMBuilder.getConfigurator();
      slTMConfigurator.setType(StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED);

      var linkConf =
              slTMConfigurator.getOutputConfiguration().createAndRegisterOutputTypeConfiguration(OutputType.LINK);
      linkConf.removeProperty(OutputPropertyType.UPSTREAM_NODE_XML_ID);
      linkConf.removeProperty(OutputPropertyType.DOWNSTREAM_NODE_XML_ID);
      linkConf.addProperty(OutputPropertyType.LINK_SEGMENT_XML_ID);

      StaticLtm sLTM = sLTMBuilder.build();

      var om = sLTM.getOutputManager();
      var outputFormatter = new MemoryOutputFormatter(network.getIdGroupingToken());
      om.registerOutputFormatter(outputFormatter);

      sLTM.execute();

      om.persistOutputData(
              demands.timePeriods.getFirst(),
              Set.of(network.getModes().get(PredefinedModeType.CAR)),
              true);

      assertTravelTimes(sLTM, demands, outputFormatter);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM bush based assignment");
    }
  }

  private void assertTravelTimes(StaticLtm sLTM, Demands demands, MemoryOutputFormatter outputFormatter) {
    Map<String,Double> expectedCosts = new TreeMap<>();
    expectedCosts.put("0", FREEFLOW_TT_KM + 1.0/2.0 * ( 8000.0/4000.0 - 1));
    expectedCosts.put("1", FREEFLOW_TT_KM);
    expectedCosts.put("2", FREEFLOW_TT_KM + 1.0/2.0 * ( 4000.0/2000.0 - 1));
    expectedCosts.put("3", FREEFLOW_TT_KM);
    expectedCosts.put("4", FREEFLOW_TT_KM);

    var mode = network.getModes().get(PredefinedModeType.CAR);
    var timePeriod = demands.timePeriods.getFirst();

    int countAsserts = 1 ;
    int linkCostIndex = outputFormatter.getPositionOfOutputValueProperty(OutputType.LINK, OutputPropertyType.LINK_SEGMENT_COST);
    int linkXmlId = outputFormatter.getPositionOfOutputKeyProperty(OutputType.LINK, OutputPropertyType.LINK_SEGMENT_XML_ID);
    var linkResultIter = outputFormatter.getIterator(mode, timePeriod, outputFormatter.getLastIteration(), OutputType.LINK);
    while(linkResultIter.hasNext()){
      linkResultIter.next();
      Object[] linkResultKeys = linkResultIter.getKeys();
      Object[] linkResultValues = linkResultIter.getValues();
      String linkSegmentXmlId = (String) linkResultKeys[linkXmlId];
      double linkSegmentCost = (double) linkResultValues[linkCostIndex];
      if(expectedCosts.containsKey(linkSegmentXmlId)) {
        ++countAsserts;
        assertEquals(expectedCosts.get(linkSegmentXmlId), linkSegmentCost, Precision.EPSILON_6);
      }
    }
    assertEquals(countAsserts, expectedCosts.size());

  }

  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(sLtmTravelTimeTest.class);
    }
  }

  @BeforeEach
  public void beforeTest() {
    // construct the network.
    // Demand = 8k
    //
    //
    //         c=8k      c=4k       c=6k      c=2k      c=6k
    //     0        1            2        3         4          5
    //  A__*-->-----*----->------*---->---*---->----*---->-----*__A'
    //         0          1           2        3         4
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
        node.setPosition(geoFactory.createPoint(new Coordinate(5000, 0)));
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

      /* capacities the same (2000), difference is in number of lanes applied) */
      MacroscopicLinkSegmentTypes linkTypes = networkLayer.getLinkSegmentTypes();
      var mainType = linkTypes.getFactory().registerNew(
              "MainType", 2000, 180, network.getModes().getFirst());
      mainType.setXmlId("MainType");

      var linkSegmentFactory = networkLayer.getLinkSegments().getFactory();
      linkSegmentFactory.registerNew(
              links.getByXmlId("0"), mainType, true, true).setNumberOfLanes(4).setXmlId("0");
      linkSegmentFactory.registerNew(
              links.getByXmlId("1"), mainType, true, true).setNumberOfLanes(2).setXmlId("1");
      linkSegmentFactory.registerNew(
              links.getByXmlId("2"), mainType, true, true).setNumberOfLanes(3).setXmlId("2");
      linkSegmentFactory.registerNew(
              links.getByXmlId("3"), mainType, true, true).setNumberOfLanes(1).setXmlId("3");
      linkSegmentFactory.registerNew(
              links.getByXmlId("4"), mainType, true, true).setNumberOfLanes(3).setXmlId("4");

      zoning = new Zoning(testToken, networkLayer.getLayerIdGroupingToken());
      zoning.getOdZones().getFactory().registerNew().setXmlId("A");
      zoning.getOdZones().getFactory().registerNew().setXmlId("A`");

      var connectoidFactory = zoning.getOdConnectoids().getFactory();
      connectoidFactory.registerNewWithUndirectedEntry(zoning.getOdZones().getByXmlId("A"),  nodes.getByXmlId("0"));
      connectoidFactory.registerNewWithUndirectedEntry(zoning.getOdZones().getByXmlId("A`"), nodes.getByXmlId("4"));

    }catch(Exception e) {
      e.printStackTrace();
      fail("initialise");
    }
  }
  //@formatter:on

  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
    IdGenerator.reset();
  }

  /**
   * This test checks the travel time on a corridor path for additive costs without
   * considering upstream bottleneck impact
   */
  @Test
  public void testSltmCorridorAdditiveBasic() {
    runTest();
  }
}
