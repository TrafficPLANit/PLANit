package org.goplanit.converter.zoning.connectoid;

/**
 * What creating OD connectoids gave a zone. A zone has the first outcome, in the order declared, that holds for it, so
 * the outcomes earlier in the order are the ones that need looking at
 * <p>
 * A count per outcome tells at a glance whether the settings suit a zoning: many zones without a connectoid for a mode
 * point to a maximum distance too short or a mode the network barely carries; many with fewer than asked for point to
 * a preferred distance too short for the zones' sizes.
 * </p>
 *
 * @author markr
 */
public enum OdConnectoidZoneOutcome {

  /**
   * the zone had OD connectoids of its own and kept them, as replacing them was not asked for; a mode asked for that
   * none of them allows has no connectoid, and the zone is reported without one for it
   */
  KEPT_EXISTING,

  /** the zone has neither a centroid position nor a geometry, so it has no point to join the network from */
  WITHOUT_POINT,

  /** a mode asked for has no connectoid, as no node it can use lies within the maximum distance */
  NONE_FOR_A_MODE,

  /** no candidate lies within the preferred distance, so the connectoids lie beyond it, within the maximum distance */
  NONE_WITHIN_PREFERRED_DISTANCE,

  /** fewer connectoids than asked for, as no further candidate lies within the preferred distance in a free direction */
  FEWER_THAN_ASKED_FOR,

  /** every connectoid asked for */
  ALL_ASKED_FOR;
}
