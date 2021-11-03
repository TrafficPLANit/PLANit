package org.goplanit.converter.service;

import org.goplanit.converter.ConverterReader;
import org.goplanit.service.routed.RoutedServices;

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
