package org.goplanit.test.converter;

import org.goplanit.converter.utils.ExternalToPlanitModeMapping;
import org.goplanit.utils.mode.PredefinedModeType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test which external modes a mapping to PLANit modes knows, and the mappings it gives, whether active or not
 *
 * @author markr
 */
public class ExternalToPlanitModeMappingTest {

  /**
   * A mode is known by its default mapping, though not active, or by an overridden one; a mode with neither is not
   */
  @Test
  public void hasMappingWhetherActiveOrNot() {
    var mapping = new ExternalToPlanitModeMapping();
    mapping.setDefaultMapping("car", PredefinedModeType.CAR);
    mapping.overrideMapping("cab", PredefinedModeType.TAXI);

    assertTrue(mapping.hasMapping("car"));
    assertFalse(mapping.isMapped("car"));
    assertTrue(mapping.hasMapping("cab"));
    assertFalse(mapping.hasMapping("tram"));
  }

  /** The default mappings are given whether active or not, in alphabetical order of the external mode */
  @Test
  public void defaultMappingsWhetherActiveOrNot() {
    var mapping = new ExternalToPlanitModeMapping();
    mapping.setDefaultMapping("taxi", PredefinedModeType.TAXI);
    mapping.setDefaultMapping("car", PredefinedModeType.CAR);
    mapping.activate("car");

    assertEquals(List.of("car", "taxi"), List.copyOf(mapping.getDefaultMappings().keySet()));
    assertEquals(PredefinedModeType.TAXI, mapping.getDefaultMappings().get("taxi"));
  }
}
