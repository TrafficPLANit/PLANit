package org.goplanit.test.zoning;

import org.goplanit.logging.Logging;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.zoning.Zoning;
import org.goplanit.zoning.zonetozone.OdDemandMatrix;
import org.goplanit.zoning.zonetozone.OdDemands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the stochastic rounding of an OD demand matrix: which cells are rounded, and the values a fixed seed gives
 *
 * @author markr
 */
public class OdDemandMatrixStochasticRoundingTest {

  /** the logger, giving the log output PLANit's format */
  private static Logger LOGGER = null;

  /** demand of the 16 cells of four zones, origin by origin, with zeros, values up to the bound of 1 and above it */
  private static final double[] DEMAND =
      {0.0, 0.2, 0.5, 0.8, 1.0, 1.5, 0.05, 0.95, 0.3, 0.6, 2.0, 0.1, 0.7, 0.4, 0.9, 0.25};

  /** upper bound of the rounding */
  private static final double UPPER_BOUND = 1.0;

  /** seed of the rounding */
  private static final int SEED = 1234;

  /**
   * The demand of four zones, filled cell by cell from {@link #DEMAND}, stochastically rounded
   *
   * @return the demand of each cell after rounding, origin by origin
   */
  private static double[] roundDemand() {
    IdGroupingToken token = IdGenerator.createIdGroupingToken("OdDemandMatrixStochasticRoundingTest");
    var zoning = new Zoning(token, token);
    for (int zone = 0; zone < 4; ++zone) {
      zoning.getOdZones().getFactory().registerNew().setXmlId(String.valueOf(zone));
    }
    var zones = zoning.getOdZones();
    OdDemands demands = new OdDemandMatrix(zones);
    for (int cell = 0; cell < DEMAND.length; ++cell) {
      demands.setValue(zones.getByXmlId(String.valueOf(cell / 4)), zones.getByXmlId(String.valueOf(cell % 4)),
          DEMAND[cell]);
    }
    demands.applyStochasticRounding(UPPER_BOUND, SEED, false);
    double[] rounded = new double[DEMAND.length];
    for (int cell = 0; cell < DEMAND.length; ++cell) {
      rounded[cell] = demands.getValue(
          zones.getByXmlId(String.valueOf(cell / 4)), zones.getByXmlId(String.valueOf(cell % 4)));
    }
    return rounded;
  }

  /**
   * Create the logger once for all tests of the class
   *
   * @throws Exception when the log file cannot be opened
   */
  @BeforeAll
  public static void setUp() throws Exception {
    if (LOGGER == null) {
      LOGGER = Logging.createLogger(OdDemandMatrixStochasticRoundingTest.class);
    }
  }

  /** Close the logger after all tests of the class */
  @AfterAll
  public static void tearDown() {
    Logging.closeLogger(LOGGER);
  }

  /** Cells of zero and cells above the bound keep their demand, the others become 0 or the bound */
  @Test
  public void testOnlyCellsUpToTheBoundAreRounded() {
    double[] rounded = roundDemand();
    for (int cell = 0; cell < DEMAND.length; ++cell) {
      if (DEMAND[cell] <= 0 || DEMAND[cell] > UPPER_BOUND) {
        assertTrue(rounded[cell] == DEMAND[cell]);
      } else {
        assertTrue(rounded[cell] == 0.0 || rounded[cell] == UPPER_BOUND);
      }
    }
  }

  /** The fixed seed gives these values, cell by cell */
  @Test
  public void testFixedSeedGivesTheSameValues() {
    assertArrayEquals(
        new double[]{0.0, 0.0, 1.0, 1.0, 1.0, 1.5, 0.0, 1.0, 0.0, 1.0, 2.0, 1.0, 0.0, 1.0, 0.0, 1.0}, roundDemand());
  }
}
