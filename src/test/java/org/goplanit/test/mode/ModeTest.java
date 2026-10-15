package org.goplanit.test.mode;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests checking a mode against a predefined mode type
 *
 * @author markr
 */
public class ModeTest {

  private MacroscopicNetwork network;

  @BeforeEach
  public void setUp() {
    IdGenerator.reset();
    network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
  }

  @AfterEach
  public void tearDown() {
    IdGenerator.reset();
  }

  /**
   * A predefined mode is its own predefined mode type and no other
   */
  @Test
  public void isPredefinedModeTypeMatchesOnlyItsOwnType() {
    var bicycle = network.getModes().getFactory().registerNew(PredefinedModeType.BICYCLE);

    assertTrue(bicycle.isPredefinedModeType(PredefinedModeType.BICYCLE));
    assertFalse(bicycle.isPredefinedModeType(PredefinedModeType.CAR));
  }

  /**
   * A custom mode is no predefined mode type, also not the custom one
   */
  @Test
  public void customModeIsNoPredefinedModeType() {
    var scooter = network.getModes().getFactory().registerNewCustomMode("scooter", 25, 0.5, null, null);

    assertFalse(scooter.isPredefinedModeType(PredefinedModeType.CUSTOM));
    assertFalse(scooter.isPredefinedModeType(PredefinedModeType.BICYCLE));
  }
}
