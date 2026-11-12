package org.goplanit.algorithms.sampling.demands.enums;

import org.goplanit.utils.mode.Mode;

/**
 * The groups of modes that the down-sampling counts person kilometres and distance bands by
 * <p>
 * Vehicle kilometres drive congestion on the roads and public transport kilometres drive crowding on the services, so
 * the sample keeps each apart rather than one total over all trips. A person riding in a car adds no vehicle
 * kilometres, so only the driver's trip counts as by car as driver. The trips in a car as someone other than its
 * driver and those walked or cycled differ in length, so each is a group of its own: one group for both could match a
 * sample that swapped long rides for short walks.
 * </p>
 *
 * @author markr
 */
public enum DownSamplingModeGroup {

  /** trips by a mode its travellers drive, made by the person driving: the vehicle kilometres */
  CAR_DRIVER,

  /**
   * trips by a motorised mode that is not public transport, made by a person not driving it: a car passenger, a ride
   * share or a taxi
   */
  CAR_NOT_DRIVER,

  /** trips by a mode whose use is public, such as a bus or a train */
  PUBLIC_TRANSPORT,

  /** trips by a mode whose use is active ({@code Mode#isActiveMode}): walking and cycling */
  ACTIVE,

  /** every other trip, such as one whose mode is not known */
  OTHER;

  /**
   * The group of a person's trip
   *
   * @param mode of the trip, null when not known
   * @param drives true when the person drives the trip, which is then by a mode its travellers drive
   * @return the group
   */
  public static DownSamplingModeGroup of(Mode mode, boolean drives) {
    if (drives) {
      return CAR_DRIVER;
    }
    if (mode == null) {
      return OTHER;
    }
    if (mode.isPublicTransportMode()) {
      return PUBLIC_TRANSPORT;
    }
    if (mode.isActiveMode()) {
      return ACTIVE;
    }
    return mode.isMotorisedMode() ? CAR_NOT_DRIVER : OTHER;
  }
}
