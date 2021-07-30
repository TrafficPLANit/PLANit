package org.planit.service.routed;

import java.util.ArrayList;
import java.util.List;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.service.ServiceLeg;

/**
 * Implementation of a RoutedTripFrequency interface.
 * 
 * @author markr
 */
public class RoutedTripFrequencyImpl extends RoutedTripImpl implements RoutedTripFrequency {

  /**
   * Ordered list of legs for this trip from start to end
   */
  public final List<ServiceLeg> orderedLegs;

  /** frequency of the routed trip per hour */
  public double frequencyPerHour;

  /**
   * Constructor
   * 
   * @param tokenId to use for id generation
   */
  public RoutedTripFrequencyImpl(final IdGroupingToken tokenId) {
    super(tokenId);
    this.orderedLegs = new ArrayList<ServiceLeg>(1);
    this.frequencyPerHour = -1;
  }

  /**
   * Copy constructor
   * 
   * @param routedTripFrequencyImpl to copy
   */
  public RoutedTripFrequencyImpl(RoutedTripFrequencyImpl routedTripFrequencyImpl) {
    super(routedTripFrequencyImpl);
    this.orderedLegs = new ArrayList<ServiceLeg>(routedTripFrequencyImpl.orderedLegs);
    this.frequencyPerHour = -1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generateId(tokenId);
    setId(newId);
    return newId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripFrequencyImpl clone() {
    return new RoutedTripFrequencyImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clearLegs() {
    this.orderedLegs.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addLeg(ServiceLeg leg) {
    this.orderedLegs.add(leg);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getFrequencyPerHour() {
    return frequencyPerHour;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setFrequencyPerHour(double frequencyPerHour) {
    this.frequencyPerHour = frequencyPerHour;
  }

}
