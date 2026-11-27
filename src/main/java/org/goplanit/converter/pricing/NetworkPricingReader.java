package org.goplanit.converter.pricing;

import org.goplanit.converter.ConverterReader;
import org.goplanit.pricing.NetworkPricing;

/**
 * Interface to read a PLANit network pricing
 * <p>
 * Every kind of input has a reader interface of its own, as the network, zoning, demands and service networks do, so a
 * converter or a project can ask for a network pricing reader whatever format it reads, and logs what it reads under
 * one name.
 * </p>
 *
 * @author markr
 */
public interface NetworkPricingReader extends ConverterReader<NetworkPricing> {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "NETWORK PRICING";
  }
}
