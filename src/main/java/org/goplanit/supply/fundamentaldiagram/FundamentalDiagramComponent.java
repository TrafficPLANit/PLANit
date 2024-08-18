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
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegments;

/**
 * Fundamental diagram traffic component. Here we track the relation between a macroscopic link segment and its
 * fundamental diagram. To minimise memory usage only unique fundamental diagrams are retained based on their
 * relaxed hash codes. If a duplicate it registered it is simply discarded and the related link segment is attached
 * to the already present fundamental diagram in the pool
 * <p>
 *   Any overrides using PLANit network entities directly are to be invoked  AFTER the initialisation of the FDs
 *   and from within PLANit itself (that is what they are available). Any settings carried over from a user directly
 *   (via a configurator) are to be considered separate and as part of the initialisation (not yet implemented).
 *   These two should interactions should be kept separate.
 * </p>
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
   * Add the passed in FD as a new unique FD if it is not already present based on relaxed hash code identification.
   * When present return the currently registered FD instead of the passed in one and the passed in FD is NOT added
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
      LOGGER.warning(String.format("IGNORE: Fundamental diagram absent for link segment %s to %.2f",
              linkSegment.getXmlId()));
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
      LOGGER.warning(String.format("IGNORE: Fundamental diagram absent for link segment type %s to %.2f",
              linkSegmentType.getXmlId()));
    }
    return foundFd;
  }

  /**
   * Factory method based on link segment type for derived components to implement
   *
   * @param lsType to use
   * @param mode mode to use
   * @return created FD
   */
  protected abstract FundamentalDiagram createFundamentalDiagramByLinkSegmentType(
          MacroscopicLinkSegmentType lsType, Mode mode);

  /**
   /**
   * Populate the fundamental diagram component with FDs for each link segment type. We do not generate any specifically
   * for a link segment unless the physical speed limit of the link is more restrictive than the one posted in the type.
   * This however is rare and will also trigger a warning to the user. The user can then further change the used Fds via
   * the configurator if desired.
   * <p>
   * It will generate all fundamental diagrams per macroscopic link segment type. In case the fundamental diagrams can
   * be constructed based solely on physical characteristics, i.e., free speed, we do so. If the registered link segment
   * types explicitly require a particular capacity to be used than the estimated capacity of the FD is overruled with
   * the one of the type (or if the FD cannot derive its capacity from physical characteristics, in which case either
   * the set or default capacity of the link segment type is used).
   * </p>
   *
   * @param parentNetworkLayer to use
   */
  protected void initialiseDefaultFundamentalDiagramsForLayer(MacroscopicNetworkLayer parentNetworkLayer){
    Mode mode = parentNetworkLayer.getFirstSupportedMode();

    /* link types */
    for (MacroscopicLinkSegmentType lsType : parentNetworkLayer.getLinkSegmentTypes()) {
      if (!lsType.isModeAllowed(mode)) {
        continue;
      }

      /* create local */
      var createdFd = createFundamentalDiagramByLinkSegmentType(lsType, mode);

      /* register */
      register(lsType, createdFd);
    }

    /*
     * check if the speed limit of the link differs from the link segment type mode's limit, Only override the FD for
     * the link if the speed limit is MORE restrictive, if it is not, the link segment type is more restrictive and
     * is to be used. It is unlikely that the link speed is more restrictive, so log this as well so the user can
     * decide if it is to be altered
     */
    for (MacroscopicLinkSegment linkSegment : parentNetworkLayer.getLinkSegments()) {
      updateFundamentalDiagramForLinkIfRestricted(linkSegment, mode);
    }
  }

  /**
   * check if the speed limit of the link differs from the link segment type mode's limit, Only override the FD for
   * the link if the speed limit is MORE restrictive, if it is not, the link segment type is more restrictive and
   * is to be used. It is unlikely that the link speed is more restrictive, so log this as well so the user can
   * decide if it is to be altered
   *
   * @param linkSegment to check
   * @param mode to check
   */
  protected void updateFundamentalDiagramForLinkIfRestricted(
          MacroscopicLinkSegment linkSegment, Mode mode) {

    if (Precision.smaller(linkSegment.getPhysicalSpeedLimitKmH(), linkSegment.getLinkSegmentType().getMaximumSpeedKmH(mode))) {
      LOGGER.warning(String.format(
              "Physical speed limit (%.2f) on link segment %s is more restrictive than the speed limit (%.2f) of the applied link segment type %s",
              linkSegment.getPhysicalSpeedLimitKmH(), linkSegment.getXmlId(), linkSegment.getLinkSegmentType().getMaximumSpeedKmH(mode),
              linkSegment.getLinkSegmentType().getXmlId()));

      /* update FD */
      double modeSpeedLimit = linkSegment.getModelledSpeedLimitKmH(mode);
      LOGGER.info(String.format("Overwriting fundamental diagram used on link segment %s, restricting free flow speed to %.2f",
              linkSegment.getXmlId(), modeSpeedLimit));

      var newFd = get(linkSegment).deepClone();
      newFd.setMaximumSpeedKmHour(modeSpeedLimit);

      /* register */
      register(linkSegment, newFd);
    }
  }

  /**
   * Register the given fundamental diagram for the link segment. This overrules the fundamental diagram that would be
   * used based on the link segment's type. In case there already exists an identical fundamental diagram (based on
   * relaxed hashcode comparison), the link segment is assigned the already present fundamental diagram. The fundamental diagram
   * used for the link segment is returned, which is either the passed in one, or an already present functionally identical version
   * 
   * @param linkSegment        to use
   * @param fundamentalDiagram to register
   * @return used Fd for the link segment, can be different (but functionally equivalent) if registered Fd was already
   *   present for another link segment
   */
  protected FundamentalDiagram register(
          final MacroscopicLinkSegment linkSegment, final FundamentalDiagram fundamentalDiagram) {
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
   * Register the given fundamental diagram for the link segment type. In case there already exists an identical
   * fundamental diagram (based on relaxed hashcode comparison), the link segment type is assigned the already
   * present fundamental diagram. The fundamental diagram used for the link segment type is returned, which is
   * either the passed in one, or an already present functionally identical version
   *
   * @param linkSegmentType    to use
   * @param fundamentalDiagram to register
   * @return used Fd for the link segment type, can be different (but functionally equivalent) if registered Fd was
   *  already present for another link segment
   */
  protected FundamentalDiagram register(
          final MacroscopicLinkSegmentType linkSegmentType, final FundamentalDiagram fundamentalDiagram) {
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
   * precision scale to apply for the relaxed hash code to compare fundamental diagrams on being identical regarding
   * their floating point variables, where 6 equates to 6 decimals of precision
   */
  public static final int RELAXED_HASH_CODE_SCALE = 6;

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
    other.uniqueFundamentalDiagrams.forEach((key1, value1) ->
            uniqueFundamentalDiagrams.put(key1, deepCopy ? value1.deepClone() : value1));

      this.linkSegmentFundamentalDiagrams = new HashMap<>();
      this.linkSegmentTypeFundamentalDiagrams = new HashMap<>();

      /* replace old references with deep copied new ones if needed */
      other.linkSegmentFundamentalDiagrams.forEach((key, value) -> linkSegmentFundamentalDiagrams.put(
              key,
              deepCopy
                      ? uniqueFundamentalDiagrams.get(value.relaxedHashCode(RELAXED_HASH_CODE_SCALE))
                      : value));

      other.linkSegmentTypeFundamentalDiagrams.forEach((key, value) -> linkSegmentTypeFundamentalDiagrams.put(
              key,
              deepCopy
                      ? uniqueFundamentalDiagrams.get(value.relaxedHashCode(RELAXED_HASH_CODE_SCALE))
                      : value));
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
   * Set the capacity in pcu/h/lane to use for the Newell FD for a given link segment.
   *
   * @param linkSegment         the specified link segment
   * @param capacityPcuHourLane to use
   */
  public void setCapacityLinkSegmentPcuHourLane(
          final MacroscopicLinkSegment linkSegment, final double capacityPcuHourLane) {
    FundamentalDiagram foundFd = getOrWarning(linkSegment);
    FundamentalDiagram newFd = foundFd.shallowClone();
    newFd.setCapacityPcuHour(capacityPcuHourLane);
    register(linkSegment, newFd);
  }

  /**
   * Set the maximum density in pcu/km/lane to use for the Newell FD for a given link segment.
   *
   * @param linkSegment         the specified link segment
   * @param maxDensityPcuKmLane to use
   */
  public void setMaximumDensityLinkSegmentPcuKmLane(
          final MacroscopicLinkSegment linkSegment, final double maxDensityPcuKmLane) {
    FundamentalDiagram foundFd = getOrWarning(linkSegment);
    FundamentalDiagram newFd = foundFd.shallowClone();
    newFd.setMaximumDensityPcuKmHour(maxDensityPcuKmLane);
    register(linkSegment, newFd);
  }

  /**
   * Set the capacity in pcu/h/lane to use for the Newell FD for a given link segment type.
   *
   * @param linkSegmentType     the specified link segment type
   * @param capacityPcuHourLane to use
   */
  public void setCapacityLinkSegmentTypePcuHourLane(
          final MacroscopicLinkSegmentType linkSegmentType, final double capacityPcuHourLane) {
    FundamentalDiagram foundFd = getOrWarning(linkSegmentType);
    FundamentalDiagram newFd = foundFd.shallowClone();
    newFd.setCapacityPcuHour(capacityPcuHourLane);
    register(linkSegmentType, newFd);
  }

  /**
   * Set the maximum density in pcu/km/lane to use for the Newell FD for a given link segment type.
   *
   * @param linkSegmentType     the specified link segment type
   * @param maxDensityPcuKmLane to use
   */
  public void setMaximumDensityLinkSegmentTypePcuKmLane(
          final MacroscopicLinkSegmentType linkSegmentType, final double maxDensityPcuKmLane) {
    FundamentalDiagram foundFd = getOrWarning(linkSegmentType);
    FundamentalDiagram newFd = foundFd.shallowClone();
    newFd.setMaximumDensityPcuKmHour(maxDensityPcuKmLane);
    register(linkSegmentType, newFd);
  }

  /**
   * Method to collect all fundamental diagrams for the given link segments in a 1:1 fashion in a raw array based on the
   * current setup of this component. The returned array is indexed by the link segments linkSegmentId (not id).
   * The returned array is a newly created array yet the fundamental diagrams contained in it are references to the fundamental
   * diagrams registered on this component. Further, it is also assumed that the provided link segments are indeed the
   * segments (and types) on which the registered fundamental diagrams on this component are based. If not, then this
   * would result in undefined behaviour.
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
   * The fundamental diagram component registers for the PopulateFundamentalDiagramEvent in order to initialise its
   * default fundamental diagrams based on the network layer that it is created for. Further user or builder overrides,
   * can alter these subsequently at a later stage
   *
   * @return supported event types
   */
  @Override
  public PlanitComponentEventType[] getKnownSupportedEventTypes() {
    return new PlanitComponentEventType[] { PopulateFundamentalDiagramEvent.EVENT_TYPE };
  }

  /**
   * Registered for PopulateFundamentalDiagramEvent which allows the component to initialise all the default available
   * Fds based on the network layer it is registered for. Delegates to
   * {@link #initialiseDefaultFundamentalDiagramsForLayer(MacroscopicNetworkLayer)} for concrete derived
   * implementations to execute
   *
   * @param event to process
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
