package org.goplanit.test.nodemodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.goplanit.algorithms.nodemodel.TampereNodeModel;
import org.goplanit.algorithms.nodemodel.TampereNodeModelFixedInput;
import org.goplanit.algorithms.nodemodel.TampereNodeModelInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.math.Precision;

/**
 * Test the Tampere node model based on the example in Tampere et al. (2011)
 * 
 * @author markr
 *
 */
public class TamperNodeModelTest {

  Array1D<Double> inCapacities;
  Array1D<Double> outReceivingFlows;
  Array2D<Double> turnSendingflows;

  //@formatter:off
  @BeforeEach
  public void intialise() {
    inCapacities = Array1D.PRIMITIVE64.copy(new double[] { 1000, 2000, 1000, 2000 });
    outReceivingFlows = Array1D.PRIMITIVE64.copy(new double[] { 1000, 2000, 1000, 2000 });

    // rows: from, columns: to
    turnSendingflows = Array2D.PRIMITIVE64.rows(
        new double[] { 0, 50, 150, 300 }, 
        new double[] { 100, 0, 300, 1600 }, 
        new double[] { 100, 100, 0, 600 },
        new double[] { 100, 800, 800, 0 });
  }
//@formatter:on

  @Test
  public void TampereNodeModelFixedInputTest() {
    TampereNodeModelFixedInput fixedInput = new TampereNodeModelFixedInput(inCapacities, outReceivingFlows);
    assertEquals(4, fixedInput.getNumberOfIncomingLinkSegments());
    assertEquals(4, fixedInput.getNumberOfOutgoingLinkSegments());
  }

  @Test
  public void TampereNodeModelInputTest() {
    try {
      TampereNodeModelFixedInput fixedInput = new TampereNodeModelFixedInput(inCapacities, outReceivingFlows);
      TampereNodeModelInput input = new TampereNodeModelInput(fixedInput, turnSendingflows);
      Array1D<Double> scalingFactors = input.getCapacityScalingFactors();
      assertEquals(1000.0 / 500.0, scalingFactors.get(0), Precision.EPSILON_6);
      assertEquals(2000.0 / 2000.0, scalingFactors.get(1), Precision.EPSILON_6);
      assertEquals(1000.0 / 800.0, scalingFactors.get(2), Precision.EPSILON_6);
      assertEquals(2000.0 / 1700.0, scalingFactors.get(3), Precision.EPSILON_6);
    } catch (Exception e) {
      fail("Error when constructing input for Tampere node model");
    }
  }

  //@formatter:off
  @Test
  public void testBasedOnPaper() {
    try {
      TampereNodeModel tampereNodeModel = 
          new TampereNodeModel(
              new TampereNodeModelInput(
                  new TampereNodeModelFixedInput(inCapacities, outReceivingFlows), turnSendingflows));
      
      
      Array1D<Double> inLinkFlowAcceptanceFactors = tampereNodeModel.run();
      assertEquals(inLinkFlowAcceptanceFactors.get(0), 1.0, Precision.EPSILON_6);
      assertEquals(inLinkFlowAcceptanceFactors.get(1), 0.68483412, Precision.EPSILON_6);
      assertEquals(inLinkFlowAcceptanceFactors.get(2), 1.0, Precision.EPSILON_6);
      assertEquals(inLinkFlowAcceptanceFactors.get(3), 0.80568720, Precision.EPSILON_6); 

    } catch (Exception e) {
      fail("Error when running Tampere node model");
    }
  }
  //@formatter:on

  /**
   * Test to verify running the node model with turn costs that
   * allow for different turn cost at flow-cost discontinuities, e.g., if a turn flow is zero but it would
   * turn into a congested out link that would cause the in-link to become mroe restricted than it currently is then
   * its zero-flow turn cost has two values on for the zero flow and one for the non zero flow in the limit to zero.
   * <p>
   *   Capturing the most retrictive turn costs considering these discontinuities can be useful in certain situations
   *   and it is an option to switch on. The results are tested here.
   * </p>
   *
   */
  @Test
  public void testForTurnCost() {
    //                               2
    //                               ^ dummy out
    //    high cap - high flow      |                restricted out cap=1k
    //   1 ------------------------>  ---------------------------> 3
    //                              ^
    //    straight turn high flow   |  right turn flow zero has no impact, but any non-zero flow immediately blocks link
    //    (reducing right turn      |
    //    fair share)               |
    //                              |
    //                              0

    try {
      var theInCapacities = Array1D.PRIMITIVE64.copy(new double[]{1000.0, 5000.0, 0.0, 0.0});
      var theOutReceivingFlows = Array1D.PRIMITIVE64.copy(new double[]{0.0, 0.0, 1000.0, 1000.0});

      // rows: from, columns: to
      var theTurnSendingflows = Array2D.PRIMITIVE64.rows(
              new double[] { 0, 0, 999.999999, 0.000001 },
              new double[] { 0, 0, 0, 3000 },
              new double[] { 0, 0, 0, 0 },
              new double[] { 0, 0, 0, 0 });


      TampereNodeModel tampereNodeModel =
              new TampereNodeModel(
                      new TampereNodeModelInput(
                              new TampereNodeModelFixedInput(theInCapacities, theOutReceivingFlows), theTurnSendingflows));

      //  with near zero flow but not zero flow on the right turn both in links are severely congested
      Array1D<Double> inLinkFlowAcceptanceFactors = tampereNodeModel.run();
      assertEquals(0.2, inLinkFlowAcceptanceFactors.get(0), Precision.EPSILON_6);
      assertEquals(0.3333333, inLinkFlowAcceptanceFactors.get(1), Precision.EPSILON_6);

      // if we now remove the super small right-turn flow, we suddenly have no congestion at all anymore on
      // the right turn

      // rows: from, columns: to
      theTurnSendingflows = Array2D.PRIMITIVE64.rows(
              new double[] { 0, 0, 999.999999, 0.0 },
              new double[] { 0, 0, 0, 3000 },
              new double[] { 0, 0, 0, 0 },
              new double[] { 0, 0, 0, 0 });
      tampereNodeModel.getInputs().replaceTurnSendingFlows(theTurnSendingflows);

      inLinkFlowAcceptanceFactors = tampereNodeModel.run();
      assertEquals(inLinkFlowAcceptanceFactors.get(0), 1, Precision.EPSILON_6);
      assertEquals(inLinkFlowAcceptanceFactors.get(1), 0.3333333, Precision.EPSILON_6);

      // now run but collect the most restricting turn level reduction factors considering flow/cost discontinuities
      var turnFlowAcceptanceFactors = tampereNodeModel.runTurnBased();
      assertEquals(turnFlowAcceptanceFactors.get(0,2), 1, Precision.EPSILON_6);
      assertEquals(turnFlowAcceptanceFactors.get(0,3), 0.2, Precision.EPSILON_6);
      assertEquals(turnFlowAcceptanceFactors.get(1, 2), 1, Precision.EPSILON_6);
      assertEquals(turnFlowAcceptanceFactors.get(1, 3), 0.3333333, Precision.EPSILON_6);

      // now we reduce the straight flow on the link with the right turn with the small flow to zero. This means
      // the right-turn flow would be scaled to capacity if it had flow receiving comparatively more barganing power
      // on the outlink, this should result in a turn acceptance factor on the zero-flow turn that is again 1 rather
      // than below 1 compared to before
      theTurnSendingflows = Array2D.PRIMITIVE64.rows(
              new double[] { 0, 0, 0.0, 0.0 },
              new double[] { 0, 0, 0, 3000 },
              new double[] { 0, 0, 0, 0 },
              new double[] { 0, 0, 0, 0 });
      tampereNodeModel.getInputs().replaceTurnSendingFlows(theTurnSendingflows);

      inLinkFlowAcceptanceFactors = tampereNodeModel.run();
      assertEquals(inLinkFlowAcceptanceFactors.get(0), 1, Precision.EPSILON_6);
      assertEquals(inLinkFlowAcceptanceFactors.get(1), 0.3333333, Precision.EPSILON_6);

      // now run and we should see all reverting to 1 except for the one turn with high flow
      turnFlowAcceptanceFactors = tampereNodeModel.runTurnBased();
      assertEquals(turnFlowAcceptanceFactors.get(0,2), 1, Precision.EPSILON_6);
      assertEquals(turnFlowAcceptanceFactors.get(0,3), 1, Precision.EPSILON_6);
      assertEquals(turnFlowAcceptanceFactors.get(1, 2), 1, Precision.EPSILON_6);
      assertEquals(turnFlowAcceptanceFactors.get(1, 3), 0.3333333, Precision.EPSILON_6);

    } catch (Exception e) {
      fail("Error when running Tampere node model");
    }
  }
  //@formatter:on

}
