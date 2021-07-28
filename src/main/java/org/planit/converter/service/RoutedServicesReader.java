package org.planit.converter.service;

import org.planit.converter.ConverterReader;
import org.planit.service.routed.RoutedServices;

/**
 * Interface to read a PLANit RoutedServices instance
 * 
 * @author markr
 *
 */
public interface RoutedServicesReader extends ConverterReader<RoutedServices> {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "ROUTED_SERVICES";
  }

}
