package org.planit.test.nodemodel;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.ojalgo.array.Array1D;
import org.ojalgo.array.Array2D;
import org.planit.algorithms.nodemodel.TampereNodeModel;
import org.planit.algorithms.nodemodel.TampereNodeModelFixedInput;
import org.planit.algorithms.nodemodel.TampereNodeModelInput;
import org.planit.exceptions.PlanItException;
import org.planit.math.Precision;
import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.utils.network.physical.Node;

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
  
  @Before
  public void intialise() {
    inCapacities = Array1D.PRIMITIVE64.copy(new double[] {1000,2000,1000,2000});
    outReceivingFlows = Array1D.PRIMITIVE64.copy(new double[] {1000,2000,1000,2000});
    
    turnSendingflows = Array2D.PRIMITIVE64.rows(
        new double[] {0,50,150,300},
        new double[] {100,0,300,1600}, 
        new double[] {100,100,0,600}, 
        new double[] {100,800,800,0});    
  }
  
  @Test
  public void TampereNodeModelFixedInputTest() {
    try {
      TampereNodeModelFixedInput fixedInput =new TampereNodeModelFixedInput(inCapacities,outReceivingFlows);
      assertEquals(4,fixedInput.getNumberOfIncomingLinkSegments());
      assertEquals(4,fixedInput.getNumberOfOutgoingLinkSegments());
    } catch (PlanItException e) {
      fail("Error when constructing fixed input for Tampere node model");
    }
  }
  
  @Test
  public void TampereNodeModelInputTest() {
    try {
      TampereNodeModelFixedInput fixedInput =new TampereNodeModelFixedInput(inCapacities,outReceivingFlows);
      TampereNodeModelInput input = new TampereNodeModelInput(fixedInput, turnSendingflows);
      Array1D<Double> scalingFactors = input.getCapacityScalingFactors();
      assertEquals(1000/500, scalingFactors.get(0), Precision.EPSILON_6);
      assertEquals(2000/2000, scalingFactors.get(1), Precision.EPSILON_6);
      assertEquals(1000/800, scalingFactors.get(2), Precision.EPSILON_6);
      assertEquals(2000/1700, scalingFactors.get(3), Precision.EPSILON_6);
    } catch (PlanItException e) {
      fail("Error when constructing input for Tampere node model");
    }
  }  

  @Test
  public void test() {
    try {
      TampereNodeModel tampereNodeModel = new TampereNodeModel(new TampereNodeModelInput(new TampereNodeModelFixedInput(inCapacities,outReceivingFlows), turnSendingflows));
      tampereNodeModel.run();
    } catch (PlanItException e) {
      fail("Error when running Tampere node model");
    }    
  }

}
