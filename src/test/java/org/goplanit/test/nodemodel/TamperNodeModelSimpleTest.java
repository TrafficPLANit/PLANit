package org.goplanit.test.nodemodel;

import org.goplanit.algorithms.nodemodel.TampereNodeModel;
import org.goplanit.algorithms.nodemodel.TampereNodeModelFixedInput;
import org.goplanit.algorithms.nodemodel.TampereNodeModelInput;
import org.goplanit.utils.math.Precision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test the Tampere node model based on the example in Tampere et al. (2011)
 * 
 * @author markr
 *
 */
public class TamperNodeModelSimpleTest {

  Array1D<Double> inCapacities;
  Array1D<Double> outReceivingFlows;
  Array2D<Double> turnSendingflows;

  //@formatter:off
  @BeforeEach
  public void initialise() {
    inCapacities = Array1D.PRIMITIVE64.copy(new double[] { 100, 200, 100, 100 });
    outReceivingFlows = Array1D.PRIMITIVE64.copy(new double[] { 100, 200, 100, 100 });

    // rows: from, columns: to
    turnSendingflows = Array2D.PRIMITIVE64.rows(
        new double[] { 0, 0, 0, 100 },
        new double[] { 0, 0, 0, 100 },
        new double[] { 0, 0, 0, 0 },
        new double[] { 0, 0, 0, 0 });
  }
//@formatter:on

  //@formatter:off
  @Test
  public void testNodeModel() {
    try {
      TampereNodeModel tampereNodeModel = 
          TampereNodeModel.of(inCapacities, outReceivingFlows, turnSendingflows);

      Array1D<Double> inLinkFlowAcceptanceFactors = tampereNodeModel.run();
      assertEquals(inLinkFlowAcceptanceFactors.get(0), 1.0/3, Precision.EPSILON_6);
      assertEquals(inLinkFlowAcceptanceFactors.get(1), 2.0/3, Precision.EPSILON_6);
      assertEquals(inLinkFlowAcceptanceFactors.get(2), 1.0, Precision.EPSILON_6);
      assertEquals(inLinkFlowAcceptanceFactors.get(3), 1.0, Precision.EPSILON_6);

      // send more through other turn rather than congested turn
      // this should reduce fair share of higher in-link capacity turn and even out the accepted flow for each
      // into the congested turn
      turnSendingflows.set(1,2, 100);
      tampereNodeModel = TampereNodeModel.of(inCapacities, outReceivingFlows, turnSendingflows);
      inLinkFlowAcceptanceFactors = tampereNodeModel.run();

      assertEquals(inLinkFlowAcceptanceFactors.get(0), 1.0/2, Precision.EPSILON_6);
      assertEquals(inLinkFlowAcceptanceFactors.get(1), 1.0/2, Precision.EPSILON_6);

    } catch (Exception e) {
      fail("Error when running Tampere node model");
    }
  }
  //@formatter:on

}
