package org.goplanit.converter.pricing;

import org.goplanit.converter.ConverterWriter;
import org.goplanit.pricing.NetworkPricing;

/**
 * Interface to write a PLANit network pricing to disk
 * <p>
 * Every kind of output has a writer interface of its own, as the network, zoning, demands and service networks do, so
 * a converter can ask for a network pricing writer whatever format it writes, and logs what it writes under one name.
 * </p>
 *
 * @author markr
 */
public interface NetworkPricingWriter extends ConverterWriter<NetworkPricing> {

  /**
   * {@inheritDoc}
   */
  @Override
  default String getTypeDescription() {
    return "NETWORK PRICING";
  }
}
