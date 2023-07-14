package org.goplanit.od.demand;

import java.util.function.BiConsumer;

import org.goplanit.utils.functionalinterface.TriConsumer;
import org.goplanit.utils.od.OdData;
import org.goplanit.utils.zoning.OdZone;
import org.goplanit.utils.zoning.OdZones;

/**
 * OdData representing Origin-Destination demands without a specific container reference so a general typed definition of OdDemands can be used for access
 * 
 * @author markr
 *
 */
public interface OdDemands extends OdData<Double> {

  /**
   * Multiply all entries with given factor
   * 
   * @param factor to multiply with
   */
  public abstract void multiply(final double factor);

  /**
   * Apply the provided consumer to each origin-destination combination found that has non zero demands
   * 
   * @param odZones  to loop over
   * @param consumer to apply
   */
  public default void forEachNonZeroOdDemand(final OdZones odZones, final TriConsumer<OdZone, OdZone, Double> consumer) {
    odZones.forEachOriginDestination((o, d) -> {
      Double odDemand = getValue(o, d);
      if (odDemand != null && odDemand > 0) {
        consumer.accept(o, d, odDemand);
      }
    });
  }

  /**
   * Apply the provided consumer to each destination of the origin provided that has non zero demands
   * 
   * @param origin   to consider destinations for
   * @param odZones  to loop destinations over
   * @param consumer to apply
   */
  public default void forEachNonZeroDestinationDemand(final OdZones odZones, final OdZone origin, final BiConsumer<OdZone, Double> consumer) {
    odZones.forEach((d) -> {
      Double odDemand = getValue(origin, d);
      if (odDemand != null && odDemand > 0) {
        consumer.accept(d, odDemand);
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract OdDemands shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public default OdDemands deepClone(){
    return shallowClone();
  }

}
