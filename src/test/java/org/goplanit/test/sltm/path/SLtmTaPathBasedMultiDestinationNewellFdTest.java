package org.goplanit.test.sltm.path;

import org.goplanit.assignment.ltm.sltm.StaticLtm;
import org.goplanit.assignment.ltm.sltm.StaticLtmConfigurator;
import org.goplanit.assignment.ltm.sltm.StaticLtmTrafficAssignmentBuilder;
import org.goplanit.assignment.ltm.sltm.StaticLtmType;
import org.goplanit.choice.logit.BoundedMultinomialLogitConfigurator;
import org.goplanit.demands.Demands;
import org.goplanit.logging.Logging;
import org.goplanit.output.enums.OutputType;
import org.goplanit.output.formatter.MemoryOutputFormatter;
import org.goplanit.path.choice.PathChoice;
import org.goplanit.path.choice.StochasticPathChoiceConfigurator;
import org.goplanit.choice.ChoiceModel;
import org.goplanit.sdinteraction.smoothing.MSRASmoothingConfigurator;
import org.goplanit.sdinteraction.smoothing.Smoothing;
import org.goplanit.supply.fundamentaldiagram.FundamentalDiagram;
import org.goplanit.test.sltm.sLtmAssignmentMultiDestinationTestBase;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.path.PathUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the sLTM assignment basic functionality (route choice) with multiple destinations for a single origin using
 * a triangular shaped fundamental diagram
 * 
 * @author markr
 *
 */
public class SLtmTaPathBasedMultiDestinationNewellFdTest extends sLtmAssignmentMultiDestinationTestBase {

  /** the logger */
  private static Logger LOGGER = null;

  private void testStochasticLinkOutputs(StaticLtm sLTM, String logitModel) {
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

    //todo: change to inflow check as outflow does not reveal differences between models all that clearly
    if(logitModel.equals(ChoiceModel.BOUNDED_MNL)) {
      assertEquals(outflow0, 8000, Precision.EPSILON_3);
      assertEquals(outflow1, 4519.24, 1);
      assertEquals(outflow2, 1500.0, Precision.EPSILON_3);
      assertEquals(outflow3, outflow2, Precision.EPSILON_3);
      assertEquals(outflow4, 3749.72, 1);
      assertEquals(outflow5, 3296.5, 1);
      assertEquals(outflow6, 1500.0, Precision.EPSILON_3);
      assertEquals(outflow7, outflow6, Precision.EPSILON_3);
      assertEquals(outflow8, 3750.28, 1);
      assertEquals(outflow9, 3000.0, Precision.EPSILON_3);
      assertEquals(outflow10, 1500.0, Precision.EPSILON_3);
      assertEquals(outflow11, outflow10, Precision.EPSILON_3);
      assertEquals(outflow12, 4500.0, Precision.EPSILON_3);
      assertEquals(outflow13, 2249.72, 1);
      assertEquals(outflow14, 2250.28, 1);
    }else if(logitModel.equals(ChoiceModel.WEIBIT)){
      assertEquals(outflow0, 8000, Precision.EPSILON_3);
      assertEquals(outflow1, 4522.72, 1);
      assertEquals(outflow2, 1500, Precision.EPSILON_3);
      assertEquals(outflow3, outflow2, Precision.EPSILON_3);
      assertEquals(outflow4, 3749.36, 1);
      assertEquals(outflow5, 3200.14, 1);
      assertEquals(outflow6, 1500.0, Precision.EPSILON_3);
      assertEquals(outflow7, outflow6, Precision.EPSILON_3);
      assertEquals(outflow8, 3750.64, 1);
      assertEquals(outflow9, 3000.0, Precision.EPSILON_3);
      assertEquals(outflow10, 1500.0, Precision.EPSILON_3);
      assertEquals(outflow11, outflow10, Precision.EPSILON_3);
      assertEquals(outflow12, 4500.0, Precision.EPSILON_3);
      assertEquals(outflow13, 2249.36, 1);
      assertEquals(outflow14, 2250.64, 1);
    }

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

    assertEquals(outflow0, inflow1 + inflow5, Precision.EPSILON_3);
    assertEquals(outflow1, inflow2 + inflow9, Precision.EPSILON_3);
    assertEquals(outflow2, inflow3, Precision.EPSILON_3);
    assertEquals(outflow3 + outflow13, inflow4, Precision.EPSILON_3);
    assertEquals(outflow4, inflow4, Precision.EPSILON_3);
    assertEquals(outflow5, inflow10 + inflow6, Precision.EPSILON_3);
    assertEquals(outflow6, inflow7, Precision.EPSILON_3);
    assertEquals(outflow7 + outflow14, inflow8, Precision.EPSILON_3);
    assertEquals(outflow8, inflow8, Precision.EPSILON_3);
    assertEquals(outflow9 + outflow11, inflow12, Precision.EPSILON_3);
    assertEquals(outflow10, inflow11, Precision.EPSILON_3);
    assertEquals(outflow12, inflow13 + inflow14, Precision.EPSILON_3);
  }

  /**
   * {@inheritDoc}
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(SLtmTaPathBasedMultiDestinationNewellFdTest.class);
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

  /**
   * Test sLTM path based assignment on above network for a point queue model with Weibit SUE
   */
  @Test
  public void sLtmPointQueuePathBasedWeibitSueAssignmentTest() {
    try {

      Demands demands = createDemands(4000);

      /* sLTM - POINT QUEUE */
      var sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      var configurator = sLTMBuilder.getConfigurator();
      configurator.createAndRegisterFundamentalDiagram(FundamentalDiagram.NEWELL);
      configurator.disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      configurator.activateDetailedLogging(false);

      /* PATH BASED */
      configurator.setType(StaticLtmType.PATH_BASED);

      /* MSRA */
      var smoothingConfig = (MSRASmoothingConfigurator) configurator.createAndRegisterSmoothing(Smoothing.MSRA);
      // we can even use it with negatives as it ismade  foolproof, this makes it possible to keep going back to more aggressive
      // steps which for some approaches such as Weibit appears to be beneficial
      smoothingConfig.setActivateLambda(true);
      smoothingConfig.setKappaStep(0.8);
      smoothingConfig.setGammaStep(0.01);
      smoothingConfig.setBadIterationThreshold(0.99);

      /* PATH CHOICE - STOCHASTIC */
      final var suePathChoice = (StochasticPathChoiceConfigurator) configurator.createAndRegisterPathChoice(PathChoice.STOCHASTIC);
      configurator.setActivateRelativeScalingFactor(false);
      {
        /* Weibit for path choice */
        var choiceModel = suePathChoice.createAndRegisterChoiceModel(ChoiceModel.WEIBIT);
        choiceModel.setScalingFactor(10);
        // by not setting a fixed od path set (suePathChoice.setFixedOdPathMatrix(...)), it is assumed we want a dynamic path set

        // You can add your own custom filters alternatively, the one below being identical to the predefined max overlap one for 80%
        final var MAX_OVERLAP = 0.6;
        suePathChoice.getPathFilter().addCustomFilter(
                (p, paths) -> paths.stream().noneMatch(pAlt -> {
                  var factor = PathUtils.getOverlapFactor(p, pAlt);
                  if(factor > MAX_OVERLAP){
                    LOGGER.info(String.format("OVERLAP TOO HIGH %s", factor));
                  }else{
                    LOGGER.info(String.format("OVERLAP OK %s", factor));
                  };
                  return factor > MAX_OVERLAP;}));
      }

      /* OUTPUT CONFIG */
      configurator.activateOutput(OutputType.LINK);
      configurator.registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      /* GAP AND CONVERGENCE */
      configurator.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      configurator.getGapFunction().getStopCriterion().setMaxIterations(1000);

      /* BUILD AND EXECUTE */
      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.setActivateDetailedLogging(false);
      sLTM.execute();

      testStochasticLinkOutputs(sLTM, ChoiceModel.WEIBIT);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM multi-destination path based assignment");
    }
  }

  /**
   * Test sLTM path based assignment on above network for a point queue model with bounded MNL
   */
  @Test
  public void sLtmPointQueuePathBasedBoundedMnlSueAssignmentTest() {
    try {

      Demands demands = createDemands(4000);

      /* sLTM - POINT QUEUE */
      var sLTMBuilder = new StaticLtmTrafficAssignmentBuilder(network.getIdGroupingToken(), null, demands, zoning, network);
      var configurator = sLTMBuilder.getConfigurator();
      configurator.createAndRegisterFundamentalDiagram(FundamentalDiagram.NEWELL);
      configurator.disableLinkStorageConstraints(StaticLtmConfigurator.DEFAULT_DISABLE_LINK_STORAGE_CONSTRAINTS);
      configurator.activateDetailedLogging(false);

      /* no relative scaling factors */
      configurator.setActivateRelativeScalingFactor(false);

      /* PATH BASED */
      configurator.setType(StaticLtmType.PATH_BASED);

      /* MSRA */
      var smoothingConfig = (MSRASmoothingConfigurator) configurator.createAndRegisterSmoothing(Smoothing.MSRA);
      // "abusing" the self-regulating average to keep searching for the most aggressive step-size rather than reducing it by definition
      smoothingConfig.setActivateLambda(true);
      smoothingConfig.setKappaStep(1);
      smoothingConfig.setGammaStep(0.05);
      smoothingConfig.setBadIterationThreshold(0.99);

      /* PATH CHOICE - STOCHASTIC */
      final var suePathChoice = (StochasticPathChoiceConfigurator) configurator.createAndRegisterPathChoice(PathChoice.STOCHASTIC);
      {
        /* Bounded MNL for path choice */
        var choiceModel = (BoundedMultinomialLogitConfigurator) suePathChoice.createAndRegisterChoiceModel(ChoiceModel.BOUNDED_MNL);
        choiceModel.setScalingFactor(10);
        choiceModel.setDelta(0.5);        // we set bound with 0.5h of travel time difference for a path to be considered
        // by not setting a fixed od path set (suePathChoice.setFixedOdPathMatrix(...)), it is assumed we want a dynamic path set

        // You can add your own custom filters alternatively, the one below being identical to the predefined max overlap one for 80%
        final var MAX_OVERLAP = 0.6;
        suePathChoice.getPathFilter().addCustomFilter(
                (p, paths) -> paths.stream().noneMatch(pAlt -> {
                  var factor = PathUtils.getOverlapFactor(p, pAlt);
                  if(factor > MAX_OVERLAP){
                    LOGGER.info(String.format("OVERLAP TOO HIGH %s", factor));
                  }else{
                    LOGGER.info(String.format("OVERLAP OK %s", factor));
                  };
                  return factor > MAX_OVERLAP;}));
      }

      /* OUTPUT CONFIG */
      configurator.activateOutput(OutputType.LINK);
      configurator.activateOutput(OutputType.PATH);
      configurator.activateOutput(OutputType.OD);
      configurator.registerOutputFormatter(new MemoryOutputFormatter(network.getIdGroupingToken()));

      /* GAP AND CONVERGENCE */
      configurator.getGapFunction().getStopCriterion().setEpsilon(Precision.EPSILON_9);
      configurator.getGapFunction().getStopCriterion().setMaxIterations(1000);

      /* BUILD AND EXECUTE */
      StaticLtm sLTM = sLTMBuilder.build();
      sLTM.setActivateDetailedLogging(true);
      sLTM.execute();

      testStochasticLinkOutputs(sLTM, ChoiceModel.BOUNDED_MNL);


    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing sLTM multi-destination path based assignment");
    }
  }

}
