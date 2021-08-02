package org.planit.service.routed;

import java.util.ArrayList;
import java.util.List;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.network.layer.service.ServiceLegSegment;

/**
 * Implementation of a RoutedTripFrequency interface.
 * 
 * @author markr
 */
public class RoutedTripFrequencyImpl extends RoutedTripImpl implements RoutedTripFrequency {

  /**
   * Ordered list of leg segments for this trip from start to end
   */
  public final List<ServiceLegSegment> orderedLegSegments;

  /** frequency of the routed trip per hour */
  public double frequencyPerHour;

  /**
   * Constructor
   * 
   * @param tokenId to use for id generation
   */
  public RoutedTripFrequencyImpl(final IdGroupingToken tokenId) {
    super(tokenId);
    this.orderedLegSegments = new ArrayList<ServiceLegSegment>(1);
    this.frequencyPerHour = -1;
  }

  /**
   * Copy constructor
   * 
   * @param routedTripFrequencyImpl to copy
   */
  public RoutedTripFrequencyImpl(RoutedTripFrequencyImpl routedTripFrequencyImpl) {
    super(routedTripFrequencyImpl);
    this.orderedLegSegments = new ArrayList<ServiceLegSegment>(routedTripFrequencyImpl.orderedLegSegments);
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
    this.orderedLegSegments.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addLegSegment(ServiceLegSegment legSegment) {
    this.orderedLegSegments.add(legSegment);
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
