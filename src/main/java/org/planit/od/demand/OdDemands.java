package org.planit.od.demand;

import org.planit.utils.functionalinterface.TriConsumer;
import org.planit.utils.od.OdData;
import org.planit.utils.zoning.OdZone;
import org.planit.utils.zoning.OdZones;

/**
 * OdData representing Origin-Destination demands without a specific container reference so a general typed definition of OdDemands can be used for access
 * 
 * @author markr
 *
 */
public interface OdDemands extends OdData<Double> {

  /**
   * Apply the provided consumer to each origin-destination combination found that has non zero demands
   * 
   * @param odZones  to loop over
   * @param consumer to apply
   */
  public default void forEachNonZeroOdDemand(OdZones odZones, TriConsumer<OdZone, OdZone, Double> consumer) {
    odZones.forEachOriginDestination((o, d) -> {
      Double odDemand = getValue(o, d);
      if (odDemand != null && odDemand > 0) {
        consumer.accept(o, d, odDemand);
      }
    });
  }

}
