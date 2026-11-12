package org.goplanit.algorithms.sampling.demands.enums;

/**
 * A household's cars against the persons in it who may drive, as the down-sampling counts households by
 * <p>
 * Whether a household has a car for each person who may drive shapes how its persons travel more than its number of
 * cars alone: two cars mean a car each for a couple, but one less than needed for a family of three adults. So the
 * sample keeps the share of households in each case.
 * </p>
 *
 * @author markr
 */
public enum DownSamplingCarAvailability {

  /** no car */
  NO_CAR,

  /** fewer cars than persons who may drive */
  FEWER_CARS_THAN_DRIVERS,

  /** at least one car per person who may drive */
  A_CAR_PER_DRIVER;

  /**
   * The availability of a household's cars
   *
   * @param cars of the household
   * @param drivers persons of the household who may drive
   * @return the availability
   */
  public static DownSamplingCarAvailability of(int cars, int drivers) {
    if (cars == 0) {
      return NO_CAR;
    }
    return cars < drivers ? FEWER_CARS_THAN_DRIVERS : A_CAR_PER_DRIVER;
  }
}
