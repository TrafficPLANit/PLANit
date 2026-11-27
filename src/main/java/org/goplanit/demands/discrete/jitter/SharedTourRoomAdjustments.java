package org.goplanit.demands.discrete.jitter;

import org.goplanit.demands.discrete.tour.Tour;
import org.goplanit.utils.id.ExternalIdAbleUtils;
import org.goplanit.utils.misc.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Used only by the shared tour draw, see {@link SharedTourJitterUtils}: the shared tours whose draw needed room made for
 * it, collected while drawing and reported together once every shared tour is drawn.
 * <p>
 * These are the two things the draw gives up to keep every participant's day in order. Either an earlier shared
 * tour ends sooner than it was drawn, or a tour's time points in a bin get no spread. Neither puts a day out of
 * order, so they are logged rather than returned. One line per case keeps the log readable when many tours are
 * affected.
 * </p>
 *
 * @author markr
 */
final class SharedTourRoomAdjustments {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SharedTourRoomAdjustments.class.getCanonicalName());

  /** shared tours whose end moved earlier to make room for a later shared tour of one of their participants */
  private final Set<Tour> movedEndTours = new LinkedHashSet<>();

  /** shared tours with points in a time bin kept in order at one time, as their participants left them no room */
  private final Set<Tour> orderKeptTours = new LinkedHashSet<>();

  /**
   * Record a shared tour whose end moved earlier to make room for a later shared tour of one of its participants
   *
   * @param tour whose end moved
   */
  public void addMovedEndTour(Tour tour) {
    movedEndTours.add(tour);
  }

  /**
   * Record a shared tour whose points in a time bin were kept in order at one time, as its participants left them no
   * room
   *
   * @param tour kept in order at one time
   */
  public void addOrderKeptTour(Tour tour) {
    orderKeptTours.add(tour);
  }

  /** Log the tours collected, per case their number and the first of them */
  public void log() {
    if (!movedEndTours.isEmpty()) {
      LOGGER.info(String.format(
          "Moved the end of %d shared tours earlier within their time bin, making room for a later shared tour of " +
              "one of their participants, tours %s", movedEndTours.size(),
          ExternalIdAbleUtils.toIdsAsString(movedEndTours, StringUtils.DEFAULT_MAX_LISTED_ENTRIES)));
    }
    if (!orderKeptTours.isEmpty()) {
      LOGGER.warning(String.format(
          "Kept %d shared tours in order by giving their points in a time bin one time, as their participants " +
              "left them no room there, tours %s", orderKeptTours.size(),
          ExternalIdAbleUtils.toIdsAsString(orderKeptTours, StringUtils.DEFAULT_MAX_LISTED_ENTRIES)));
    }
  }
}
