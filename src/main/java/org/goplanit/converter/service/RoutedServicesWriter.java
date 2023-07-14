package org.goplanit.converter.service;

import org.goplanit.converter.ConverterWriter;
import org.goplanit.service.routed.RoutedServices;

/**
 * Interface to write a PLANit RoutedServices to disk
 *
 * @author markr
 *
 */
public interface RoutedServicesWriter extends ConverterWriter<RoutedServices> {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "ROUTED SERVICES";
  }

}