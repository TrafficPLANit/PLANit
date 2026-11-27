package org.goplanit.project;

import java.util.TreeMap;

import org.goplanit.pricing.NetworkPricing;
import org.goplanit.utils.wrapper.LongMapWrapperImpl;

/**
 * The network pricings registered on a project
 * <p>
 * A project may price its networks, one pricing per scenario of prices, as it holds zonings and demands.
 * </p>
 *
 * @author markr
 */
public class ProjectNetworkPricings extends LongMapWrapperImpl<NetworkPricing> {

  /**
   * Constructor
   */
  protected ProjectNetworkPricings() {
    super(new TreeMap<>(), NetworkPricing::getId);
  }

  /**
   * Copy constructor
   *
   * @param other to copy
   */
  protected ProjectNetworkPricings(ProjectNetworkPricings other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ProjectNetworkPricings shallowClone() {
    return new ProjectNetworkPricings(this);
  }
}
