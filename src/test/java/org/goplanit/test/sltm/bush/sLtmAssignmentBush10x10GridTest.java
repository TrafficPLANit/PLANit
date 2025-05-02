package org.goplanit.test.sltm.bush;

import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.assignment.ltm.sltm.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.StaticLtmTrafficAssignmentBuilder;
import org.goplanit.assignment.ltm.sltm.StaticLtmType;
import org.goplanit.demands.Demands;
import org.goplanit.logging.Logging;
import org.goplanit.od.demand.OdDemandMatrix;
import org.goplanit.od.demand.OdDemands;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
import org.goplanit.test.sltm.sLtmAssignmentGridTestBase;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.zoning.OdZones;
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
public class sLtmAssignmentBush10x10GridTest extends sLtmAssignmentGridTestBase {

  private final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("sLtmAssignmentBushGridTest");

  /** the logger */
  private static Logger LOGGER = null;

  /**
   * {@inheritDoc}
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(sLtmAssignmentBush10x10GridTest.class);
    }
  }

  /**
   * {@inheritDoc}
   */
  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
  }

  @BeforeEach
  public void intialise() {
    super.intialise10x10NetworkAndZoning(testToken);
  }

  /**
   * Test sLTM bush-conjugate destination-based assignment on grid based network which should result in a logical
   * spread across uncongested links in the final solution. In this test we do not enforce a
   * max entropy flow distribution as the initial distribution is more intuitive to test for.
   */
  @Test
  public void sLtmPointQueueConjugateBushDestinationBasedAssignmentNoQueueTest() {
    try {

      Demands demands = createDemands(testToken);

      // OD DEMANDS 900 A->A`
      // todo: up to 1800 for when this works, 1800 will trigger potentially congested pas updates
      //  (despite not being congested, which triggers a different process flow).
      OdZones odZones = zoning.getOdZones();
      OdDemands odDemands = new OdDemandMatrix(zoning.getOdZones());
      odDemands.setValue(odZones.getByXmlId("A"), odZones.getByXmlId("A`"), 900.0);
      demands.registerOdDemandPcuHour(
          demands.timePeriods.getFirst(), network.getModes().get(PredefinedModeType.CAR), odDemands);

      /* sLTM - POINT QUEUE */
      StaticLtmTrafficAssignmentBuilder sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(
          network.getIdGroupingToken(), null, demands, zoning, network);
      sLTMBuilder.getConfigurator().disableLinkStorageConstraints(
          StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);

      /* CONJUGATE DESTINATION BASED */
      sLTMBuilder.getConfigurator().setType(StaticLtmType.CONJUGATE_DESTINATION_BUSH_BASED);
      sLTMBuilder.getConfigurator().createAndRegisterFundamentalDiagram(FundamentalDiagram.QUADRATIC_LINEAR);
      sLTMBuilder.getConfigurator().activateMaxEntropyFlowDistribution(false);

      sLTMBuilder.getConfigurator().activateOutput(OutputType.LINK);
      sLTMBuilder.getConfigurator().registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      sLTMBuilder.getConfigurator().addTrackOdsForLogging(IdMapperType.XML, Pair.of("A","A`"));

      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      sLTM.getGapFunction().getStopCriterion().setMaxIterations(1000);
      sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

      //todo: add in 10x10 results to test against
      test4x4OutflowsNoQueue(sLTM);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM bush based assignment");
    }
  }

}
