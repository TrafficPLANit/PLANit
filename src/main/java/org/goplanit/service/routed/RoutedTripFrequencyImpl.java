package org.goplanit.service.routed;

import java.util.*;
import java.util.logging.Logger;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.service.ServiceLegSegment;
import org.goplanit.utils.network.layer.service.ServiceNode;
import org.goplanit.utils.service.routed.RoutedTripFrequency;

/**
 * Implementation of a RoutedTripFrequency interface.
 * 
 * @author markr
 */
public class RoutedTripFrequencyImpl extends RoutedTripImpl implements RoutedTripFrequency {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(RoutedTripFrequencyImpl.class.getCanonicalName());

  /**
   * Ordered list of leg segments for this trip from start to end (leg segments not owned)
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
    this.orderedLegSegments = new ArrayList<>(1);
    this.frequencyPerHour = -1;
  }

  /**
   * Copy constructor
   * 
   * @param routedTripFrequencyImpl to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public RoutedTripFrequencyImpl(RoutedTripFrequencyImpl routedTripFrequencyImpl, boolean deepCopy) {
    super(routedTripFrequencyImpl, deepCopy);
    this.orderedLegSegments = new ArrayList<>(routedTripFrequencyImpl.orderedLegSegments);
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
    return new RoutedTripFrequencyImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripFrequencyImpl deepClone() {
    return new RoutedTripFrequencyImpl(this, false);
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
    if (hasLegSegments()) {
      ServiceLegSegment lastSegment = getLastLegSegment();
      if (!lastSegment.getDownstreamVertex().equals(legSegment.getUpstreamVertex())) {
        LOGGER.warning("IGNORE: Unable to add leg segment that is not contiguous to current last leg segment");
        return;
      }
    }
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

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<ServiceLegSegment> iterator() {
    return this.orderedLegSegments.iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfLegSegments() {
    return this.orderedLegSegments.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceLegSegment getLegSegment(int index) {
    return this.orderedLegSegments.get(index);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<ServiceNode> getUsedServiceNodes() {
    Set<ServiceNode> usedServiceNodes = new HashSet<>();
    for(var legSegments : this){
      usedServiceNodes.add(legSegments.getUpstreamServiceNode());
    }
    usedServiceNodes.add(getLastLegSegment().getDownstreamServiceNode());
    return usedServiceNodes;
  }
}
