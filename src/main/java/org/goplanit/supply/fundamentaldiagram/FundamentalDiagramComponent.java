package org.goplanit.supply.fundamentaldiagram;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.component.PlanitComponent;
import org.goplanit.component.event.PlanitComponentEvent;
import org.goplanit.component.event.PlanitComponentEventType;
import org.goplanit.component.event.PopulateFundamentalDiagramEvent;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegments;

/**
 * Fundamental diagram traffic component. Here we track the relation between a macroscopic link segment and its fundamental diagram. To minimise memory usage only unique
 * fundamental diagrams are retained based on their relaxed hash codes. IF a duplicate it registered it is simply discarded and the related link segment is attached to the already
 * present fundamental diagram in the pool
 *
 * @author markr
 *
 */
public abstract class FundamentalDiagramComponent extends PlanitComponent<FundamentalDiagramComponent> implements Serializable {

  /** generated UID */
  private static final long serialVersionUID = 5815100111048623093L;

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(FundamentalDiagramComponent.class.getCanonicalName());

  /** all unique fundamental diagrams found so far based on their relaxed hash code */
  private final Map<Integer, FundamentalDiagram> uniqueFundamentalDiagrams;

  /** track fundamental diagrams where a link segment does not adopt the link segment type FD, but has a different one */
  private final Map<MacroscopicLinkSegment, FundamentalDiagram> linkSegmentFundamentalDiagrams;

  /** track fundamental diagram registered per link segment type */
  private final Map<MacroscopicLinkSegmentType, FundamentalDiagram> linkSegmentTypeFundamentalDiagrams;

  /**
   * precision scale to apply for the relaxed hash code to compare fundamental diagrams on being identical regarding their floating point variables, where 6 equates to 6 decimals
   * of precision
   */
  public static final int RELAXED_HASH_CODE_SCALE = 6;

  /**
   * Add the passed in FD as a new unique FD if it is not already present based on relaxed hash code identification. When present return the currently registered FD instead of the
   * passed in one and the passed in FD is NOT added
   * 
   * @param fundamentalDiagram to add
   * @return registered fundamental diagram for the generated relaxed hash code
   */
  private FundamentalDiagram registerUniqueFundamentalDiagram(FundamentalDiagram fundamentalDiagram) {
    int relaxedHash = fundamentalDiagram.relaxedHashCode(RELAXED_HASH_CODE_SCALE);
    FundamentalDiagram usedFd = fundamentalDiagram;
    if (uniqueFundamentalDiagrams.containsKey(relaxedHash)) {
      usedFd = uniqueFundamentalDiagrams.get(relaxedHash);
    } else {
      uniqueFundamentalDiagrams.put(relaxedHash, usedFd);
    }
    return usedFd;
  }

  /**
   * Validate if a link segment has a registered FD, if not issue a warning
   * 
   * @param linkSegment to validate
   * @return fundamentalDiagram found (if any), otherwise null
   */
  private FundamentalDiagram getOrWarning(final MacroscopicLinkSegment linkSegment) {
    FundamentalDiagram foundFd = get(linkSegment);
    if (foundFd == null) {
      LOGGER.warning(String.format("IGNORE: Fundamental diagram absent for link segment %s to %.2f", linkSegment.getXmlId()));
    }
    return foundFd;
  }

  /**
   * Validate if a link segment type has a registered FD, if not issue a warning
   * 
   * @param linkSegmentType to validate
   * @return fundamentalDiagram found (if any), otherwise null
   */
  private FundamentalDiagram getOrWarning(final MacroscopicLinkSegmentType linkSegmentType) {
    FundamentalDiagram foundFd = get(linkSegmentType);
    if (foundFd == null) {
      LOGGER.warning(String.format("IGNORE: Fundamental diagram absent for link segment type %s to %.2f", linkSegmentType.getXmlId()));
    }
    return foundFd;
  }

  /**
   * Initialise the default available fundamental diagrams for the layer the component is registered on. This includes the fundamental diagrams for the link segment types and
   * possible anomalies for links where the physical link segment characteristics would overrule the link segment type defaults
   * 
   * @param parentNetworkLayer to initialise default fundamental diagrams for
   */
  protected abstract void initialiseDefaultFundamentalDiagramsForLayer(MacroscopicNetworkLayer parentNetworkLayer);

  /**
   * Register the given fundamental diagram for the link segment. This overrules the fundamental diagram that would be used based on the link segment's type. In case there already
   * exists an identical fundamental diagram (based on relaxed hashcode comparison), the link segment is assigned the already present fundamental diagram. The fundamental diagram
   * used for the link segment is returned, which is either the passed in one, or an already present functionally identical version
   * 
   * @param linkSegment        to use
   * @param fundamentalDiagram to register
   * @return used Fd for the link segment, can be different (but functionally equivalent) if registered Fd was already present for another link segment
   */
  protected FundamentalDiagram register(final MacroscopicLinkSegment linkSegment, final FundamentalDiagram fundamentalDiagram) {
    int relaxedHash = fundamentalDiagram.relaxedHashCode(RELAXED_HASH_CODE_SCALE);

    /* only proceed if it differs from the one already currently used */
    FundamentalDiagram currentFd = get(linkSegment);
    if (currentFd != null && currentFd.relaxedHashCode(RELAXED_HASH_CODE_SCALE) == relaxedHash) {
      return currentFd;
    }

    /* add to unique FDs if indeed unique */
    FundamentalDiagram usedFd = registerUniqueFundamentalDiagram(fundamentalDiagram);

    /* register for link segment */
    linkSegmentFundamentalDiagrams.put(linkSegment, usedFd);
    return usedFd;
  }

  /**
   * Register the given fundamental diagram for the link segment type. In case there already exists an identical fundamental diagram (based on relaxed hashcode comparison), the
   * link segment type is assigned the already present fundamental diagram. The fundamental diagram used for the link segment type is returned, which is either the passed in one,
   * or an already present functionally identical version
   *
   * @param linkSegmentType    to use
   * @param fundamentalDiagram to register
   * @return used Fd for the link segment type, can be different (but functionally equivalent) if registered Fd was already present for another link segment
   */
  protected FundamentalDiagram register(final MacroscopicLinkSegmentType linkSegmentType, final FundamentalDiagram fundamentalDiagram) {
    int relaxedHash = fundamentalDiagram.relaxedHashCode(RELAXED_HASH_CODE_SCALE);

    /* only proceed if it differs from the one already currently used */
    FundamentalDiagram currentFd = get(linkSegmentType);
    if (currentFd != null && currentFd.relaxedHashCode(RELAXED_HASH_CODE_SCALE) == relaxedHash) {
      return currentFd;
    }

    /* add to unique FDs if indeed unique */
    FundamentalDiagram usedFd = registerUniqueFundamentalDiagram(fundamentalDiagram);

    /* register for link segment type */
    linkSegmentTypeFundamentalDiagrams.put(linkSegmentType, usedFd);
    return usedFd;
  }

  /**
   * Base constructor
   * 
   * @param groupId token
   */
  public FundamentalDiagramComponent(final IdGroupingToken groupId) {
    super(groupId, FundamentalDiagramComponent.class);
    this.uniqueFundamentalDiagrams = new HashMap<>();
    this.linkSegmentFundamentalDiagrams = new HashMap<>();
    this.linkSegmentTypeFundamentalDiagrams = new HashMap<>();
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public FundamentalDiagramComponent(final FundamentalDiagramComponent other, boolean deepCopy) {
    super(other, deepCopy);

    this.uniqueFundamentalDiagrams = new HashMap<>();
    other.uniqueFundamentalDiagrams.entrySet().forEach( e ->
            uniqueFundamentalDiagrams.put( e.getKey(), deepCopy ? e.getValue().deepClone() : e.getValue()));

      this.linkSegmentFundamentalDiagrams = new HashMap<>();
      this.linkSegmentTypeFundamentalDiagrams = new HashMap<>();

      /* replace old references with deep copied new ones if needed */
      other.linkSegmentFundamentalDiagrams.entrySet().forEach( entry ->
              linkSegmentFundamentalDiagrams.put(
                      entry.getKey(),
                      deepCopy
                              ? linkSegmentFundamentalDiagrams.get(entry.getValue().relaxedHashCode(RELAXED_HASH_CODE_SCALE))
                              : entry.getValue()));

      other.linkSegmentTypeFundamentalDiagrams.entrySet().forEach( entry ->
              linkSegmentTypeFundamentalDiagrams.put(
                      entry.getKey(),
                      deepCopy
                              ? linkSegmentTypeFundamentalDiagrams.get(entry.getValue().relaxedHashCode(RELAXED_HASH_CODE_SCALE))
                              : entry.getValue()));
  }

  /**
   * Collect the fundamental diagram for the given link segment.
   * 
   * @param linkSegment to collect fundamental diagram for
   * @return related fundamental diagram, null if not present
   */
  public FundamentalDiagram get(final MacroscopicLinkSegment linkSegment) {
    FundamentalDiagram foundFd = linkSegmentFundamentalDiagrams.get(linkSegment);
    if (foundFd == null) {
      return get(linkSegment.getLinkSegmentType());
    }
    return foundFd;
  }

  /**
   * Collect the fundamental diagram for the given link segment type.
   * 
   * @param linkSegmentType to collect fundamental diagram for
   * @return related fundamental diagram, null if not present
   */
  public FundamentalDiagram get(final MacroscopicLinkSegmentType linkSegmentType) {
    return linkSegmentTypeFundamentalDiagrams.get(linkSegmentType);
  }

  /**
   * Set the capacity in pcu/h/lane to use for the Newell FD for a given link segment. This only impacts the backward wave speed used to keep the FD viable.
   *
   * @param linkSegment         the specified link segment
   * @param capacityPcuHourLane to use
   */
  public void setCapacityLinkSegmentPcuHourLane(final MacroscopicLinkSegment linkSegment, final double capacityPcuHourLane) {
    FundamentalDiagram foundFd = getOrWarning(linkSegment);
    FundamentalDiagram newFd = foundFd.shallowClone();
    newFd.setCapacityPcuHour(capacityPcuHourLane);
    register(linkSegment, newFd);
  }

  /**
   * Set the maximum density in pcu/km/lane to use for the Newell FD for a given link segment. This only impacts the backward wave speed used to keep the FD viable. one to change
   * the capacity.
   *
   * @param linkSegment         the specified link segment
   * @param maxDensityPcuKmLane to use
   */
  public void setMaximumDensityLinkSegmentPcuKmLane(final MacroscopicLinkSegment linkSegment, final double maxDensityPcuKmLane) {
    FundamentalDiagram foundFd = getOrWarning(linkSegment);
    FundamentalDiagram newFd = foundFd.shallowClone();
    newFd.setMaximumDensityPcuKmHour(maxDensityPcuKmLane);
    register(linkSegment, newFd);
  }

  /**
   * Set the capacity in pcu/h/lane to use for the Newell FD for a given link segment type. This only impacts the backward wave speed used to keep the FD viable.
   *
   * @param linkSegmentType     the specified link segment type
   * @param capacityPcuHourLane to use
   */
  public void setCapacityLinkSegmentTypePcuHourLane(final MacroscopicLinkSegmentType linkSegmentType, final double capacityPcuHourLane) {
    FundamentalDiagram foundFd = getOrWarning(linkSegmentType);
    FundamentalDiagram newFd = foundFd.shallowClone();
    newFd.setCapacityPcuHour(capacityPcuHourLane);
    register(linkSegmentType, newFd);
  }

  /**
   * Set the maximum density in pcu/km/lane to use for the Newell FD for a given link segment type. This only impacts the backward wave speed used to keep the FD viable. one to
   * change the capacity.
   *
   * @param linkSegmentType     the specified link segment type
   * @param maxDensityPcuKmLane to use
   */
  public void setMaximumDensityLinkSegmentTypePcuKmLane(final MacroscopicLinkSegmentType linkSegmentType, final double maxDensityPcuKmLane) {
    FundamentalDiagram foundFd = getOrWarning(linkSegmentType);
    FundamentalDiagram newFd = foundFd.shallowClone();
    newFd.setMaximumDensityPcuKmHour(maxDensityPcuKmLane);
    register(linkSegmentType, newFd);
  }

  /**
   * Method to collect all fundamental diagrams for the given link segments in a 1:1 fashion in a raw array based on the current setup of this component. The returned array is
   * indexed by the link segments linkSegmentId (not id). The returned array is a newly created array yet the fundamental diagrams contained in it are references to the fundamental
   * diagrams registered on this component. Further, it is also assumed that the provided link segments are indeed the segments (and types) on which the registered fundamental
   * diagrams on this component are based. If not, then this would result in undefined behaviour.
   * 
   * @param linkSegments to collect fundamental diagrams for
   * @return fundamental diagrams per link segment by linkSegmentId, if no fd is present, the entry remains null
   */
  public FundamentalDiagram[] asLinkSegmentIndexedArray(MacroscopicLinkSegments linkSegments) {
    FundamentalDiagram[] linkSegmentFundamentalDiagrams = new FundamentalDiagram[linkSegments.size()];
    for (MacroscopicLinkSegment linkSegment : linkSegments) {
      linkSegmentFundamentalDiagrams[(int) linkSegment.getLinkSegmentId()] = get(linkSegment);
    }
    return linkSegmentFundamentalDiagrams;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() {
    this.linkSegmentFundamentalDiagrams.clear();
    this.linkSegmentTypeFundamentalDiagrams.clear();
    this.uniqueFundamentalDiagrams.clear();
  }

  /**
   * The fundamental diagram component registers for the PopulateFundamentalDiagramEvent in order to initialise its default fundamental diagrams based on the network layer that it
   * is created for. Further user or builder overrides, can alter these subsequently at a later stage
   */
  @Override
  public PlanitComponentEventType[] getKnownSupportedEventTypes() {
    return new PlanitComponentEventType[] { PopulateFundamentalDiagramEvent.EVENT_TYPE };
  }

  /**
   * Registered for PopulateFundamentalDiagramEvent which allows the component to initialise all the default available Fds based on the network layer it is registered for.
   * Delegates to {@link #initialiseDefaultFundamentalDiagramsForLayer(MacroscopicNetworkLayer)} for concrete derived implementations to execute
   * 
   */
  @Override
  public void onPlanitComponentEvent(PlanitComponentEvent event) throws PlanItException {
    if (!(event.getType().equals(PopulateFundamentalDiagramEvent.EVENT_TYPE))) {
      return;
    }
    PopulateFundamentalDiagramEvent populateFdEvent = (PopulateFundamentalDiagramEvent) event;
    if (populateFdEvent.getFundamentalDiagramToPopulate() != this) {
      return;
    }
    initialiseDefaultFundamentalDiagramsForLayer(populateFdEvent.getParentNetworkLayer());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract FundamentalDiagramComponent shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract FundamentalDiagramComponent deepClone();

}
