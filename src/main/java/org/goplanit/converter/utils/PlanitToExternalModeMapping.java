package org.goplanit.converter.utils;

import org.goplanit.utils.mode.PredefinedModeType;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Maps PLANit {@link PredefinedModeType} enums to external string-based identifiers.
 * Optimized internally via an active set, falling back to default mappings or explicit user overrides.
 *
 * @author markr
 */
public class PlanitToExternalModeMapping {

  private static final Logger LOGGER = Logger.getLogger(PlanitToExternalModeMapping.class.getCanonicalName());

  /** Set of currently active PLANit modes. */
  private final Set<PredefinedModeType> activeModes = EnumSet.noneOf(PredefinedModeType.class);

  /** Registered defaults used for standard fallback values. */
  private final Map<PredefinedModeType, String> defaultMappings = new EnumMap<>(PredefinedModeType.class);

  /** Explicit user overrides that take precedence over defaults when active. */
  private final Map<PredefinedModeType, String> overriddenMappings = new EnumMap<>(PredefinedModeType.class);

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
   * Activates a PLANit mode into the active set.
   *
   * @param planitMode PLANit mode type to activate
   */
  public void activate(PredefinedModeType planitMode) {
    if (defaultMappings.containsKey(planitMode) || overriddenMappings.containsKey(planitMode)) {
      activeModes.add(planitMode);
    }
  }

  /**
   * Deactivates a PLANit mode from the active set.
   * Preserves any existing override and default configurations.
   *
   * @param planitMode PLANit mode type to remove
   */
  public void deactivate(PredefinedModeType planitMode) {
    activeModes.remove(planitMode);
  }

  /**
   * Clears the active set and populates it with all modes that have a default or override mapping.
   */
  public void activateAllDefaults() {
    activeModes.clear();
    activeModes.addAll(defaultMappings.keySet());
    activeModes.addAll(overriddenMappings.keySet());
  }

  /**
   * Clears all currently active mappings, but retains override and default records.
   */
  public void deactivateAll() {
    activeModes.clear();
  }

  /**
   * Overwrites or explicitly sets an override mapping for a given PLANit mode,
   * and ensures it is added to the active set.
   *
   * @param planitMode   Source PLANit mode
   * @param externalMode Target external mode string
   */
  public void overrideMapping(PredefinedModeType planitMode, String externalMode) {
    overriddenMappings.put(planitMode, externalMode);
    activeModes.add(planitMode);
  }

  /**
   * Clears a specific user override, reverting the mode back to its default mapping if available.
   *
   * @param planitMode PLANit mode type
   */
  public void clearOverride(PredefinedModeType planitMode) {
    overriddenMappings.remove(planitMode);
    if (!defaultMappings.containsKey(planitMode)) {
      activeModes.remove(planitMode);
    }
  }

  /**
   * Checks if a PLANit mode mapping has an active user override.
   *
   * @param planitMode PLANit mode type
   * @return true if an override is registered
   */
  public boolean isOverridden(PredefinedModeType planitMode) {
    return overriddenMappings.containsKey(planitMode);
  }

  /**
   * Gets a type-safe set copy of all currently overridden PLANit mode keys.
   *
   * @return EnumSet containing overridden PLANit modes
   */
  public Set<PredefinedModeType> getOverriddenPlanitModes() {
    return overriddenMappings.isEmpty() ? EnumSet.noneOf(PredefinedModeType.class) :
        EnumSet.copyOf(overriddenMappings.keySet());
  }

  /**
   * Checks if a PLANit mode is actively enabled.
   *
   * @param planitMode PLANit mode type
   * @return true if active
   */
  public boolean isMapped(PredefinedModeType planitMode) {
    return activeModes.contains(planitMode);
  }

  /**
   * Resolves the external string identifier for a given PLANit mode.
   * Prioritizes user overrides, then falls back to defaults if active.
   *
   * @param planitMode PLANit mode type
   * @return Mapped external string, or null if unmapped or inactive
   */
  public String getMappedMode(PredefinedModeType planitMode) {
    if (!activeModes.contains(planitMode)) {
      return null;
    }
    if (overriddenMappings.containsKey(planitMode)) {
      return overriddenMappings.get(planitMode);
    }
    return defaultMappings.get(planitMode);
  }

  /**
   * Gets a type-safe set copy of all currently active PLANit modes.
   *
   * @return EnumSet containing active PLANit modes
   */
  public Set<PredefinedModeType> getActivePlanitModes() {
    return activeModes.isEmpty() ? EnumSet.noneOf(PredefinedModeType.class) : EnumSet.copyOf(activeModes);
  }

  /**
   * Stream of distinct external mode strings currently used by active mappings.
   *
   * @return Stream of distinct external modes
   */
  public Stream<String> getActiveExternalModesStream() {
    return activeModes.stream()
        .map(this::getMappedMode)
        .filter(Objects::nonNull)
        .distinct();
  }

  /**
   * Verifies if any functional mapping rules are active.
   *
   * @return true if active modes exist
   */
  public boolean isAnyModeMapped() {
    return !activeModes.isEmpty();
  }

  /**
   * Logs all active configuration rules, highlighting which ones are active overrides.
   *
   * @param prefix Optional log formatting line prefix
   */
  public void logActiveMapping(String prefix) {
    String safePrefix = prefix != null ? prefix : "";

    if (activeModes.isEmpty()) {
      LOGGER.info(safePrefix + "No active mode mappings defined");
      return;
    }

    int maxKeyLength = activeModes.stream()
        .map(PredefinedModeType::name)
        .mapToInt(String::length)
        .max()
        .orElse(0);

    LOGGER.info(String.format("%sPLANit to External mode mapping: ", safePrefix));

    for (var planitMode : activeModes) {
      String resolvedMode = getMappedMode(planitMode);
      String overrideMarker = overriddenMappings.containsKey(planitMode) ? " (overridden)" : "";
      LOGGER.info(String.format("%s%-" + maxKeyLength + "s -> %s%s",
          safePrefix, planitMode.name(), resolvedMode, overrideMarker));
    }
  }
}