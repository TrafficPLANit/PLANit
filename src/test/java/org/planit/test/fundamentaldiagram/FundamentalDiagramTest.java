package org.planit.test.fundamentaldiagram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.planit.logging.Logging;
import org.planit.supply.fundamentaldiagram.NewellFundamentalDiagram;
import org.planit.utils.macroscopic.MacroscopicConstants;
import org.planit.utils.math.Precision;

/**
 * Test the fundamental diagram (component) basic functionality
 * 
 * @author markr
 *
 */
public class FundamentalDiagramTest {

  /** the logger */
  private static Logger LOGGER = null;

  /**
   * {@inheritDoc}
   */
  @BeforeClass
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(FundamentalDiagramTest.class);
    }
  }

  /**
   * {@inheritDoc}
   */
  @After
  public void tearDown() {
    Logging.closeLogger(LOGGER);
  }

  //@formatter:off
  @Before
  public void intialise() {

  }
  //@formatter:on

  /**
   * Test sLTM network loading on above network
   */
  @Test
  public void NewellfundamentalDiagramTest() {
    try {

      /* create symmetric FD for testing purposes */
      final double freeSpeed = -MacroscopicConstants.DEFAULT_BACKWARD_WAVE_SPEED_KM_HOUR;
      NewellFundamentalDiagram newellFd = new NewellFundamentalDiagram(freeSpeed);

      assertEquals(newellFd.getMaximumDensityPcuKm(), MacroscopicConstants.DEFAULT_MAX_DENSITY_PCU_KM_LANE, Precision.EPSILON_6);
      assertEquals(freeSpeed, newellFd.getMaximumSpeedKmHour(), Precision.EPSILON_6);
      assertEquals(MacroscopicConstants.DEFAULT_MAX_DENSITY_PCU_KM_LANE, newellFd.getCongestedBranch().getDensityPcuKm(0), Precision.EPSILON_6);
      assertEquals(0, newellFd.getCongestedBranch().getFlowPcuHour(MacroscopicConstants.DEFAULT_MAX_DENSITY_PCU_KM_LANE), Precision.EPSILON_6);
      assertEquals(0, newellFd.getFreeFlowBranch().getDensityPcuKm(0), Precision.EPSILON_6);
      assertEquals(freeSpeed * MacroscopicConstants.DEFAULT_MAX_DENSITY_PCU_KM_LANE,
          newellFd.getFreeFlowBranch().getFlowPcuHour(MacroscopicConstants.DEFAULT_MAX_DENSITY_PCU_KM_LANE), Precision.EPSILON_6);

      final double expectedCapacity = freeSpeed * MacroscopicConstants.DEFAULT_MAX_DENSITY_PCU_KM_LANE / 2;
      double computedCapacity = newellFd.getCapacityFlowPcuHour();
      assertEquals(expectedCapacity, computedCapacity, Precision.EPSILON_6);

      /*
       * now make some changes: set maximum density to half way point between current critical density and maximum density: should result in the same backward wave speed but
       * reduced capacity
       */
      double criticalDensity = computedCapacity / freeSpeed;
      newellFd.setMaximumDensityPcuKmHour(MacroscopicConstants.DEFAULT_MAX_DENSITY_PCU_KM_LANE - (criticalDensity / 2));
      assertEquals(freeSpeed, newellFd.getMaximumSpeedKmHour(), Precision.EPSILON_6);
      assertEquals(expectedCapacity * 3 / 4, newellFd.getCapacityFlowPcuHour(), Precision.EPSILON_6);
      assertEquals(newellFd.getCongestedBranch().getCharateristicWaveSpeedKmHour(), MacroscopicConstants.DEFAULT_BACKWARD_WAVE_SPEED_KM_HOUR, Precision.EPSILON_6);

      /*
       * now make some changes compared to original: Halve the capacity: should result in critical density moved forward (less) to half of what it was and therefore with the max
       * density remaining the same, the backward wave speed is -newCapacity/(maxDensity-(oldCriticalDensity/2))
       */
      newellFd.setMaximumDensityPcuKmHour(MacroscopicConstants.DEFAULT_MAX_DENSITY_PCU_KM_LANE);
      double halvedCapacity = expectedCapacity / 2;
      newellFd.setCapacityPcuHour(halvedCapacity);
      assertEquals(freeSpeed, newellFd.getMaximumSpeedKmHour(), Precision.EPSILON_6);
      assertEquals(expectedCapacity / 2, newellFd.getCapacityFlowPcuHour(), Precision.EPSILON_6);
      assertEquals(newellFd.getMaximumDensityPcuKm(), MacroscopicConstants.DEFAULT_MAX_DENSITY_PCU_KM_LANE, Precision.EPSILON_6);
      double newCriticalDensity = criticalDensity / 2;
      double deltaCongestedBranchDensity = MacroscopicConstants.DEFAULT_MAX_DENSITY_PCU_KM_LANE - newCriticalDensity;
      assertEquals(newellFd.getCongestedBranch().getCharateristicWaveSpeedKmHour(), -halvedCapacity / deltaCongestedBranchDensity, Precision.EPSILON_6);

    } catch (Exception e) {
      e.printStackTrace();
      fail("Error when testing Newell Fundamental Diagram");
    }
  }

}
