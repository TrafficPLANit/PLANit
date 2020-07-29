package org.planit.test.nodemodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.planit.algorithms.nodemodel.TampereNodeModel;
import org.planit.algorithms.nodemodel.TampereNodeModelFixedInput;
import org.planit.algorithms.nodemodel.TampereNodeModelInput;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.math.Precision;

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
  @Before
  public void intialise() {
    inCapacities = Array1D.PRIMITIVE64.copy(new double[] { 1000, 2000, 1000, 2000 });
    outReceivingFlows = Array1D.PRIMITIVE64.copy(new double[] { 1000, 2000, 1000, 2000 });

    turnSendingflows = Array2D.PRIMITIVE64.rows(
        new double[] { 0, 50, 150, 300 }, 
        new double[] { 100, 0, 300, 1600 }, 
        new double[] { 100, 100, 0, 600 },
        new double[] { 100, 800, 800, 0 });
  }
//@formatter:on

  @Test
  public void TampereNodeModelFixedInputTest() {
    try {
      TampereNodeModelFixedInput fixedInput = new TampereNodeModelFixedInput(inCapacities, outReceivingFlows);
      assertEquals(4, fixedInput.getNumberOfIncomingLinkSegments());
      assertEquals(4, fixedInput.getNumberOfOutgoingLinkSegments());
    } catch (PlanItException e) {
      fail("Error when constructing fixed input for Tampere node model");
    }
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
    } catch (PlanItException e) {
      fail("Error when constructing input for Tampere node model");
    }
  }

  //@formatter:off
  @Test
  public void test() {
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

    } catch (PlanItException e) {
      fail("Error when running Tampere node model");
    }
  }
  //@formatter:on

}
