package org.goplanit.converter.utils;

import org.goplanit.utils.mode.PredefinedModeType;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Maps PLANit {@link PredefinedModeType} enums to external string-based identifiers.
 * Optimized internally via an {@link EnumMap} structure.
 *
 * @author markr
 */
public class PlanitToExternalModeMapping {

  private static final Logger LOGGER = Logger.getLogger(PlanitToExternalModeMapping.class.getCanonicalName());

  /** Mappings currently used during file writing/exporting. */
  private final Map<PredefinedModeType, String> activeMappings = new EnumMap<>(PredefinedModeType.class);

  /** Registered defaults used for fallback initialization or resets. */
  private final Map<PredefinedModeType, String> defaultMappings = new EnumMap<>(PredefinedModeType.class);

  /**
   * Default constructor.
   */
  public PlanitToExternalModeMapping() {
  }

  /**
   * Registers a default mapping rule. Does not automatically activate it.
   *
   * @param planitMode   Source PLANit mode
   * @param externalMode Target external mode string
   */
  public void addDefaultMapping(PredefinedModeType planitMode, String externalMode) {
    defaultMappings.put(planitMode, externalMode);
  }

  /**
   * Activates a PLANit mode using its registered default configuration.
   *
   * @param planitMode PLANit mode type to activate
   */
  public void activate(PredefinedModeType planitMode) {
    String defaultMode = defaultMappings.get(planitMode);
    if (defaultMode != null) {
      activeMappings.put(planitMode, defaultMode);
    }
  }

  /**
   * Deactivates an active mapping for a specific PLANit mode.
   *
   * @param planitMode PLANit mode type to remove
   */
  public void deactivate(PredefinedModeType planitMode) {
    activeMappings.remove(planitMode);
  }

  /**
   * Clears active rules and overwrites them with all registered defaults.
   */
  public void activateAllDefaults() {
    activeMappings.clear();
    activeMappings.putAll(defaultMappings);
  }

  /**
   * Deactivates all currently active mappings.
   */
  public void deactivateAll() {
    activeMappings.clear();
  }

  /**
   * Overwrites or explicitly sets an active mapping for a given PLANit mode.
   *
   * @param planitMode   Source PLANit mode
   * @param externalMode Target external mode string
   */
  public void overrideMapping(PredefinedModeType planitMode, String externalMode) {
    activeMappings.put(planitMode, externalMode);
  }

  /**
   * Checks if a PLANit mode is actively mapped.
   *
   * @param planitMode PLANit mode type
   * @return true if an active mapping exists
   */
  public boolean isMapped(PredefinedModeType planitMode) {
    return activeMappings.containsKey(planitMode);
  }

  /**
   * Resolves the external string identifier for a given PLANit mode.
   *
   * @param planitMode PLANit mode type
   * @return Mapped external string, or null if unmapped
   */
  public String getMappedMode(PredefinedModeType planitMode) {
    return activeMappings.get(planitMode);
  }

  /**
   * Gets a type-safe set copy of all currently active PLANit mode keys.
   *
   * @return EnumSet containing active PLANit modes
   */
  public Set<PredefinedModeType> getActivePlanitModes() {
    return activeMappings.isEmpty() ? EnumSet.noneOf(PredefinedModeType.class) : EnumSet.copyOf(activeMappings.keySet());
  }

  /**
   * Stream of distinct external mode strings currently used in active mappings.
   *
   * @return Stream of distinct external modes
   */
  public Stream<String> getActiveExternalModesStream() {
    return activeMappings.values().stream().distinct();
  }

  /**
   * Verifies if any functional mapping rules are configured.
   *
   * @return true if mappings exist
   */
  public boolean isAnyModeMapped() {
    return !activeMappings.isEmpty();
  }

  /**
   * Logs all active configuration rules using left-aligned text padding for readability.
   *
   * @param prefix Optional log formatting line prefix
   */
  public void logActiveMapping(String prefix) {
    String safePrefix = prefix != null ? prefix : "";

    if (activeMappings.isEmpty()) {
      LOGGER.info(safePrefix + "No active mode mappings defined");
      return;
    }

    int maxKeyLength = activeMappings.keySet().stream()
        .map(PredefinedModeType::name)
        .mapToInt(String::length)
        .max()
        .orElse(0);

    LOGGER.info(String.format("%sPLANit to External mode mapping: ", safePrefix));

    for (var entry : activeMappings.entrySet()) {
      LOGGER.info(String.format("%s%-" + maxKeyLength + "s -> %s",
          safePrefix, entry.getKey().name(), entry.getValue()));
    }
  }
}