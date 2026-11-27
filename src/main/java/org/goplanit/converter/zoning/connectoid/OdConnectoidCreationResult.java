package org.goplanit.converter.zoning.connectoid;

import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.zoning.Zone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * What creating OD connectoids gave: the number of zones per outcome, the connectoids made, the links broken, per mode
 * the zones left without a connectoid and why, and the modes not asked for whose connectoids replacing removed
 * <p>
 * The counts show whether the settings suit a zoning, and the zones without a connectoid are the ones a model cannot
 * reach for that mode, so a caller can report, fix or drop them before running a model: either no node the mode can
 * use lies within reach, or the zone kept connectoids of its own of which none allows the mode, as replacing them was
 * not asked for. Replacing removes every OD connectoid, so a mode not asked for that had connectoids has none left.
 * </p>
 *
 * @author markr
 */
public class OdConnectoidCreationResult {

  /** number of zones per outcome */
  private final Map<OdConnectoidZoneOutcome, Integer> numberOfZonesByOutcome =
      new EnumMap<>(OdConnectoidZoneOutcome.class);

  /** per mode, the zones without a connectoid for it by the outcome that left them without, in the order found */
  private final Map<Mode, Map<OdConnectoidZoneOutcome, List<Zone>>> zonesWithoutConnectoidByMode = new TreeMap<>();

  /** modes not asked for that had OD connectoids before replacing removed them all */
  private final SortedSet<Mode> modesLeftWithoutConnectoidsByReplacing = new TreeSet<>();

  /** OD connectoids made */
  private int numberOfConnectoidsCreated = 0;

  /** links broken to place connectoids at new nodes */
  private int numberOfLinksBroken = 0;

  /** An empty result, filled as the connectoids are made */
  protected OdConnectoidCreationResult() {
  }

  /**
   * Count a zone with its outcome
   *
   * @param outcome of the zone
   */
  protected void registerZoneOutcome(OdConnectoidZoneOutcome outcome) {
    numberOfZonesByOutcome.merge(outcome, 1, Integer::sum);
  }

  /**
   * Record a zone left without a connectoid for a mode
   *
   * @param mode without a connectoid
   * @param zone left without one
   * @param outcome that left it without, {@link OdConnectoidZoneOutcome#NONE_FOR_A_MODE} or
   *                {@link OdConnectoidZoneOutcome#KEPT_EXISTING}
   */
  protected void registerZoneWithoutConnectoid(Mode mode, Zone zone, OdConnectoidZoneOutcome outcome) {
    zonesWithoutConnectoidByMode.computeIfAbsent(mode, m -> new EnumMap<>(OdConnectoidZoneOutcome.class))
        .computeIfAbsent(outcome, o -> new ArrayList<>()).add(zone);
  }

  /**
   * Record a mode not asked for whose OD connectoids replacing removed
   *
   * @param mode left without OD connectoids
   */
  protected void registerModeLeftWithoutConnectoidsByReplacing(Mode mode) {
    modesLeftWithoutConnectoidsByReplacing.add(mode);
  }

  /** Count a connectoid made */
  protected void registerConnectoidCreated() {
    ++numberOfConnectoidsCreated;
  }

  /** Count a link broken */
  protected void registerLinkBroken() {
    ++numberOfLinksBroken;
  }

  /**
   * The number of zones with an outcome
   *
   * @param outcome to count
   * @return the number of zones, 0 when none
   */
  public int getNumberOfZones(OdConnectoidZoneOutcome outcome) {
    return numberOfZonesByOutcome.getOrDefault(outcome, 0);
  }

  /**
   * The number of OD connectoids made
   *
   * @return the number
   */
  public int getNumberOfConnectoidsCreated() {
    return numberOfConnectoidsCreated;
  }

  /**
   * The number of links broken to place connectoids at new nodes
   *
   * @return the number
   */
  public int getNumberOfLinksBroken() {
    return numberOfLinksBroken;
  }

  /**
   * The modes some zone has no connectoid for
   *
   * @return the modes, empty when every zone has one for every mode
   */
  public SortedSet<Mode> getModesWithZonesWithoutConnectoid() {
    return Collections.unmodifiableSortedSet(new TreeSet<>(zonesWithoutConnectoidByMode.keySet()));
  }

  /**
   * The zones without a connectoid for a mode, whatever left them without
   *
   * @param mode to collect them for
   * @return the zones, in id order, empty when none
   */
  public List<Zone> getZonesWithoutConnectoid(Mode mode) {
    return zonesWithoutConnectoidByMode.getOrDefault(mode, Collections.emptyMap()).values().stream()
        .flatMap(List::stream).sorted(Comparator.comparingLong(Zone::getId)).collect(Collectors.toUnmodifiableList());
  }

  /**
   * The zones a given outcome left without a connectoid for a mode
   *
   * @param mode to collect them for
   * @param outcome that left them without, {@link OdConnectoidZoneOutcome#NONE_FOR_A_MODE} or
   *                {@link OdConnectoidZoneOutcome#KEPT_EXISTING}
   * @return the zones, in id order, empty when none
   */
  public List<Zone> getZonesWithoutConnectoid(Mode mode, OdConnectoidZoneOutcome outcome) {
    return Collections.unmodifiableList(zonesWithoutConnectoidByMode.getOrDefault(mode, Collections.emptyMap())
        .getOrDefault(outcome, Collections.emptyList()));
  }

  /**
   * The modes not asked for that had OD connectoids before replacing removed them all, so have none now
   *
   * @return the modes, empty when replacing left every mode not asked for as it was
   */
  public SortedSet<Mode> getModesLeftWithoutConnectoidsByReplacing() {
    return Collections.unmodifiableSortedSet(modesLeftWithoutConnectoidsByReplacing);
  }
}
