package org.goplanit.converter.utils;

import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.misc.LoggingUtils;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generic mapping from external string-based mode identifiers (e.g. OSM, ActivitySim)
 * to PLANit {@link PredefinedModeType}s.
 *
 * <p>
 * The mapping distinguishes between:
 * <ul>
 *   <li><b>Default mappings</b>: predefined associations that can be activated</li>
 *   <li><b>Active mappings</b>: mappings currently in use during parsing/conversion</li>
 * </ul>
 *
 * <p>
 * Only active mappings are considered during lookups. Defaults are only used as a source
 * when activating or resetting mappings.
 *
 * <p>
 * The mapping can be globally enabled/disabled via the {@code active} flag. When inactive,
 * queries return empty results or {@code null}.
 *
 * <p>
 * This class is intended to be reused across different converters (e.g. OSM, ActivitySim).
 *
 * @author Mark
 */
public class ExternalToPlanitModeMapping {

  private static final Logger LOGGER = Logger.getLogger(ExternalToPlanitModeMapping.class.getCanonicalName());

  /** currently active external-to-PLANit mode mappings */
  private final Map<String, PredefinedModeType> activeMappings = new TreeMap<>();

  /** default external-to-PLANit mode mappings (used for activation/reset) */
  private final Map<String, PredefinedModeType> defaultMappings = new TreeMap<>();

  /** flag indicating whether this mapping is active (used during parsing) */
  private boolean active = true;

  /**
   * Default constructor
   */
  public ExternalToPlanitModeMapping(){
  }

  /* defaults */

  /**
   * Register a default mapping from an external mode to a PLANit mode.
   *
   * <p>
   * The mapping is not automatically activated; it serves as a source for
   * {@link #activate(String)} or {@link #activateAllDefaults()}.
   * </p>
   *
   * @param externalMode external mode identifier (must match exactly during activation)
   * @param planitMode   PLANit mode to map to
   */
  public void setDefaultMapping(String externalMode, PredefinedModeType planitMode) {
    defaultMappings.put(externalMode, planitMode);
  }

  /* activation */

  /**
   * Activate a mapping using the registered default mapping for the given external mode.
   *
   * <p>
   * If no default mapping exists for the provided mode, nothing happens.
   *</p>
   * @param externalMode external mode identifier
   */
  public void activate(String externalMode) {
    var defaultMode = defaultMappings.get(externalMode);
    if (defaultMode != null) {
      activeMappings.put(externalMode, defaultMode);
    }
  }

  /**
   * Deactivate a specific external mode by removing its active mapping.
   *
   * <p>
   * After deactivation, the mode will no longer be mapped, even if a default exists.
   *</p>
   * @param externalMode external mode identifier
   */
  public void deactivate(String externalMode) {
    activeMappings.remove(externalMode);
  }

  /**
   * Activate all registered default mappings.
   *
   * <p>
   * Existing active mappings are cleared first.
   * </p>
   */
  public void activateAllDefaults() {
    activeMappings.clear();
    activeMappings.putAll(defaultMappings);
  }

  /**
   * Deactivate all active mappings.
   *
   * <p>
   * After this call, no external modes are mapped.
   * </p>
   */
  public void deactivateAll() {
    activeMappings.clear();
  }

  /* overrides */

  /**
   * Add or overwrite an active mapping for a given external mode.
   *
   * <p>
   * This bypasses the default mappings and directly modifies the active mapping set.
   * </p>
   *
   * @param externalMode external mode identifier
   * @param planitMode   PLANit mode to map to
   */
  public void overrideMapping(String externalMode, PredefinedModeType planitMode) {
    activeMappings.put(externalMode, planitMode);
  }

  /* queries */

  /**
   * Check if an external mode is currently mapped.
   *
   * <p>
   * Returns {@code false} when the mapping is inactive.
   * </p>
   *
   * @param externalMode external mode identifier
   * @return true if mapped and active, false otherwise
   */
  public boolean isMapped(String externalMode) {
    return active && activeMappings.containsKey(externalMode);
  }

  /**
   * Retrieve the mapped PLANit mode for a given external mode.
   * Returns {@code null} if:
   * <ul>
   *   <li>the mapping is inactive</li>
   *   <li>no mapping exists for the external mode</li>
   * </ul>
   *
   * @param externalMode external mode identifier
   * @return mapped PLANit mode, or null if not available
   */
  public PredefinedModeType getMappedMode(String externalMode) {
    if (!active) {
      return null;
    }
    return activeMappings.get(externalMode);
  }

  /**
   * Retrieve all currently active external modes.
   *
   * <p>
   * The returned set is a copy and sorted.
   *
   * @return set of active external mode identifiers, or empty set when inactive
   */
  public Set<String> getActiveExternalModes() {
    return active ? new TreeSet<>(activeMappings.keySet()) : Collections.emptySet();
  }

  /**
   * Retrieve all external modes mapped to a specific PLANit mode.
   *
   * <p>
   * Only active mappings are considered.
   *
   * @param planitModeType PLANit mode to filter on
   * @return set of external modes mapped to the given PLANit mode
   */
  public Set<String> getActiveExternalModes(PredefinedModeType planitModeType) {
    if (!active || planitModeType == null) {
      return new TreeSet<>();
    }

    return activeMappings.entrySet().stream()
        .filter(entry -> entry.getValue().equals(planitModeType))
        .map(Map.Entry::getKey)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  /**
   * Retrieve all unique PLANit modes currently active in the mapping.
   *
   * <p>
   * Only active mappings are considered.
   *
   * @return stream of distinct PLANit modes
   */
  public Stream<PredefinedModeType> getActivePlanitModesStream() {
    return activeMappings.values().stream().distinct();
  }

  /* parser activation control */

  /**
   * Activate or deactivate the entire mapping.
   *
   * <p>
   * When inactive:
   * <ul>
   *   <li>{@link #isMapped(String)} returns false</li>
   *   <li>{@link #getMappedMode(String)} returns null</li>
   *   <li>collection methods return empty results</li>
   * </ul>
   *
   * @param active true to activate, false to deactivate
   */
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * Check whether this mapping is currently active.
   *
   * @return true if active, false otherwise
   */
  public boolean isActive() {
    return active;
  }

  /** verify if any mapping is present
   *
   * @return true when present, false otherwise
   */
  public boolean isAnyModeMapped() {
    return !(isActive() && this.activeMappings.isEmpty()) || !defaultMappings.isEmpty();
  }

  /**
   * Log the current active mapping from external modes to PLANit modes as a settings section.
   *
   * @param title section title to use
   * @param level indentation level to apply
   */
  public void logActiveMapping(String title, int level) {
    if(title != null && !title.isBlank()) {
      LOGGER.info(LoggingUtils.settingsSection(title, level));
      ++level;
    }

    if (!active) {
      LOGGER.info(LoggingUtils.settingsEntry("Mapping inactive", level));
      return;
    }

    if (activeMappings.isEmpty()) {
      LOGGER.info(LoggingUtils.settingsEntry("No active mode mappings defined", level));
      return;
    }

    for (var entry : activeMappings.entrySet()) {
      LOGGER.info(LoggingUtils.settingsMapping(entry.getKey(), entry.getValue(), level));
    }
  }

  /**
   * Log the current active mapping from external modes to PLANit modes.
   *
   * @param prefix to use
   */
  public void logActiveMapping(String prefix) {
    String safePrefix = prefix != null ? prefix : "";

    if (!active) {
      LOGGER.info(safePrefix + LoggingUtils.settingsEntry("Mapping inactive", 0));
      return;
    }

    if (activeMappings.isEmpty()) {
      LOGGER.info(safePrefix + LoggingUtils.settingsEntry("No active mode mappings defined", 0));
      return;
    }

    LOGGER.info(safePrefix + LoggingUtils.settingsSection("External to PLANit mode mapping", 0));
    for (var entry : activeMappings.entrySet()) {
      LOGGER.info(safePrefix + LoggingUtils.settingsMapping(entry.getKey(), entry.getValue(), 1));
    }
  }

}
