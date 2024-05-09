package org.goplanit.test.sltm.bush;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.assignment.ltm.sltm.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.StaticLtmTrafficAssignmentBuilder;
import org.goplanit.assignment.ltm.sltm.StaticLtmType;
import org.goplanit.choice.ChoiceModel;
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
import org.goplanit.sdinteraction.smoothing.MSRASmoothingConfigurator;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.test.sltm.sLtmAssignmentMultiDestinationTestBase;
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
public class sLtmAssignmentBushMultiDestinationTest extends sLtmAssignmentMultiDestinationTestBase {

  /** the logger */
  private static Logger LOGGER = null;


  /**
   * {@inheritDoc}
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(sLtmAssignmentBushMultiDestinationTest.class);
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
    super.intialiseBase();
  }
  //@formatter:on

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
    //assertEquals(outflow4, 3750, 1); do not test due to non-uniqueness being allowed under no congestion and triangular FD
    assertEquals(outflow5, 3190, 1);
    assertEquals(outflow6, 1500.0, Precision.EPSILON_3);
    assertEquals(outflow7, outflow6, Precision.EPSILON_3);
    //assertEquals(outflow8, 3750, 1); do not test due to non-uniqueness being allowed under no congestion and triangular FD
    assertEquals(outflow9, 3000.0, Precision.EPSILON_3);
    assertEquals(outflow10, 1500.0, Precision.EPSILON_3);
    assertEquals(outflow11, outflow10, Precision.EPSILON_3);
    assertEquals(outflow12, 4500.0, Precision.EPSILON_3);
    //assertEquals(outflow13, 2250, 1); do not test due to non-uniqueness being allowed under no congestion and triangular FD
    //assertEquals(outflow14, 2250, 1); do not test due to non-uniqueness being allowed under no congestion and triangular FD

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
   * Test sLTM bush-origin-based assignment on above network for a point queue model
   */
  @Test
  public void sLtmPointQueueBushOriginBasedAssignmentTest() {
    try {

      Demands demands = createDemands(4000);

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

      Demands demands = createDemands(4000);

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
