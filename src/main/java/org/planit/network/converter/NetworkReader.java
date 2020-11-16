package org.planit.network.converter;

import org.planit.network.physical.macroscopic.MacroscopicNetwork;
import org.planit.utils.exceptions.PlanItException;

/**
 * Interface for classes able to parse networks of some type which are then returned
 * 
 * @author markr
 *
 */
public interface NetworkReader {

  /**
   * Each reader maps the modes of the original reader format to PLANit modes. This mapping should be made available via this method It is required for any network converter to
   * verify if the planit modes that are used are compatible with the planit modes that the writer expects and in turn maps onto the writer's output format modes
   * 
   * @return hte mode mapper
   */
  // ReaderMode2PlanitModeMapper getModeMapper();

  /**
   * parse the network based on the configuration of the implementing class to yield a PLANit network
   * 
   * @return parsed network
   * @throws PlanItException thrown if error
   */
  public MacroscopicNetwork read() throws PlanItException;

  /**
   * each network reader should be able to produce the country in which this network is created. In case the network is fictitious and no country is available, null is to be
   * returned. In case the network spans multiple countries, it is best to simply use the {@Link CountryNames.WORLD} as the country.
   * 
   * @return
   */
  public String getCountry();

  /**
   * Verify if the reader is linked to a specific country or not
   * 
   * @return true when country is set, false otherwise
   */
  default public boolean hasCountry() {
    return getCountry() != null;
  }
}
