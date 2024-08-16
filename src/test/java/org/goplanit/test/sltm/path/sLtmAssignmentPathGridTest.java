package org.goplanit.test.sltm.path;

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
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
import org.goplanit.test.sltm.sLtmAssignmentGridTestBase;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.zoning.OdZones;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the sLTM assignment basic functionality (route choice) with a grid based network layout
 * 
 * @author markr
 *
 */
public class sLtmAssignmentPathGridTest extends sLtmAssignmentGridTestBase {

  private final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("sLtmAssignmentPathGridTest");

  /** the logger */
  private static Logger LOGGER = null;

  private void testOutflowsQueue(StaticLtm sLTM) {
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
    double outflow20 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("20").getLinkSegmentBa());
    double outflow21 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("21").getLinkSegmentBa());
    double outflow22 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("22").getLinkSegmentBa());
    double outflow23 = sLTM.getLinkSegmentOutflowPcuHour(networkLayer.getLinks().getByXmlId("23").getLinkSegmentBa());

    assertEquals(outflow0, 3 * 600, Precision.EPSILON_3);
    assertEquals(outflow1, 2 * 600, Precision.EPSILON_3);
    assertEquals(outflow2, 1 * 600, Precision.EPSILON_3);
    assertEquals(outflow3, 1 * 600, Precision.EPSILON_3);
    assertEquals(outflow4, outflow13 + outflow3, Precision.EPSILON_3);
    assertEquals(outflow5, outflow14 + outflow4, Precision.EPSILON_3);
    assertEquals(outflow12, 1 * 600, Precision.EPSILON_3);
    assertEquals(outflow13, 1 * 600, Precision.EPSILON_3);
    assertEquals(outflow14, 1 * 600, Precision.EPSILON_3);
    assertEquals(outflow15, 1 * 600, Precision.EPSILON_3);

    assertEquals(outflow9, 3 * 600, Precision.EPSILON_3);
    assertEquals(outflow10, 2 * 600, Precision.EPSILON_3);
    assertEquals(outflow11, 1 * 600, Precision.EPSILON_3);
    assertEquals(outflow20, 1 * 600, Precision.EPSILON_3);
    assertEquals(outflow21, 1 * 600, Precision.EPSILON_3);
    assertEquals(outflow22, outflow11, Precision.EPSILON_3);
    assertEquals(outflow23, outflow11, Precision.EPSILON_3);
    assertEquals(outflow6, outflow20, Precision.EPSILON_3);
    assertEquals(outflow7, outflow21 + outflow6, Precision.EPSILON_3);
    assertEquals(outflow8, outflow22 + outflow7, Precision.EPSILON_3);
  }

  /**
   * {@inheritDoc}
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(sLtmAssignmentPathGridTest.class);
    }
  }

  @BeforeEach
  public void intialise() {
    super.intialiseNetworkAndZoning(testToken);
  }

  /**
   * {@inheritDoc}
   */
  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
  }

  /**
   * Test sLTM path-based assignment on grid based network which should result in an even spread across uncongested links
   * in the final solution.
   */
  @Test
  public void sLtmPointQueuePathAssignmentNoQueueTest() {
    try {

      Demands demands = createDemands(testToken);

      /* OD DEMANDS 1800 A->A``, 1800 A->A``` (1800 pcu/h for one hour) */
      OdZones odZones = zoning.getOdZones();
      OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
      odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A``"), 1800.0);
      odDemands.setValue(odZones.getByXmlId("A`"), odZones.getByXmlId("A```"), 1800.0);
      demands.registerOdDemandPcuHour(demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      var sLtmConf = ((StaticLtmConfigurator) sLTMBuilder.getConfigurator());
      sLtmConf.disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      sLtmConf.activateDetailedLogging(true);
      
      /* PATH BASED */
      sLtmConf.setType(StaticLtmType.PATH_BASED);

      sLtmConf.activateOutput(OutputType.LINK);
      sLtmConf.registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      sLtmConf.addTrackOdsForLogging(IdMapperType.XML, Pair.of("A", "A``"));
      sLtmConf.addTrackOdsForLogging(IdMapperType.XML, Pair.of("A`", "A```"));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(1000);

      sLTM.execute();

      testOutflowsNoQueue(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM path based grid assignment");
    }
  }


  /**
   * Test sLTM path based assignment on grid based network with demand causing some queues
   */
  @Test
  public void sLtmPointQueuePathBasedAssignmentWithQueueTest() {
    try {

      Demands demands = createDemands(testToken);

      /* OD DEMANDS 3600 A->A``, 3600 A->A``` */
      OdZones odZones = zoning.getOdZones();
      OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
      odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A``"), 3600.0);
      odDemands.setValue(odZones.getByXmlId("A`"), odZones.getByXmlId("A```"), 3600.0);
      demands.registerOdDemandPcuHour(demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder =
              new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      var sLtmConf = ((StaticLtmConfigurator) sLTMBuilder.getConfigurator());
      sLtmConf.createAndRegisterFundamentalDiagram(FundamentalDiagram.QUADRATIC_LINEAR);
      sLtmConf.disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      sLtmConf.activateDetailedLogging(false);
      
      /* PATH BASED */
      sLtmConf.setType(StaticLtmType.PATH_BASED);
      sLtmConf.activateMaxEntropyFlowDistribution(true);

      sLtmConf.activateOutput(OutputType.LINK);
      sLtmConf.registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(1000);
      sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

      testOutflowsQueue(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM path based grid assignment");
    }
  }

}
