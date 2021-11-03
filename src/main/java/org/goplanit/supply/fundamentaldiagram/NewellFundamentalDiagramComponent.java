package org.goplanit.supply.fundamentaldiagram;

import java.util.logging.Logger;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

/**
 * Newell fundamental diagram traffic component
 *
 * @author markr
 *
 */
public class NewellFundamentalDiagramComponent extends FundamentalDiagramComponent {

  /** generated UID */
  private static final long serialVersionUID = -3166623064510413929L;

  /**
   * Logger to use
   */
  private static final Logger LOGGER = Logger.getLogger(NewellFundamentalDiagramComponent.class.getCanonicalName());

  /**
   * Populate the fundamental diagram component with FDs for each link segment type. We do not generate any specifically for a link segment unless the physical speed limit of the
   * link is more restrictive than the one posted in the type. This however is rare and will also trigger a warning to the user. The user can then further change the used Fds via
   * the configurator if desired.
   * <p>
   * It will generate all fundamental diagrams per macroscopic link segment type. In case the fundamental diagrams can be constructed based solely on physical characteristics,
   * i.e., free speed, we do so. If the registered link segment types explicitly require a particular capacity to be used than the estimated capacity of the FD is overruled with
   * the one of the type (or if the FD cannot derive its capacity from physical characteristics, in which case either the set or default capacity of the link segment type is used).
   * 
   * @param fundamentalDiagramToPopulate the fundamental diagram component tracking all used Fds in the network and by whom they are used
   * @param parentNetworkLayer           to use
   */
  @Override
  protected void initialiseDefaultFundamentalDiagramsForLayer(MacroscopicNetworkLayer parentNetworkLayer) {
    /* NEWELL */
    Mode mode = parentNetworkLayer.getFirstSupportedMode();

    /* link types */
    for (MacroscopicLinkSegmentType linkSegmentType : parentNetworkLayer.getLinkSegmentTypes()) {
      if (linkSegmentType.isModeAllowed(mode)) {

        /* FD */
        NewellFundamentalDiagram newellFd = null;
        double modeSpeed = linkSegmentType.getMaximumSpeedKmH(mode);
        if (!linkSegmentType.isExplicitMaximumDensityPerLaneSet() && !linkSegmentType.isExplicitCapacityPerLaneSet()) {
          /* use free speed to create Newell FD with inferred capacity */
          newellFd = new NewellFundamentalDiagram(modeSpeed);
        } else if (!linkSegmentType.isExplicitMaximumDensityPerLaneSet()) {
          /* capacity is explicitly overwritten, so use that as well, use default for jam density */
          newellFd = new NewellFundamentalDiagram(modeSpeed, linkSegmentType.getExplicitCapacityPerLane(), linkSegmentType.getExplicitMaximumDensityPerLaneOrDefault());
        } else {
          /* only jam density set, use that */
          newellFd = new NewellFundamentalDiagram(modeSpeed, linkSegmentType.getExplicitMaximumDensityPerLane());
        }

        /* register */
        register(linkSegmentType, newellFd);

      } else {
        LOGGER.info(String.format("IGNORE: Macroscopic link segment type %s has no modes used by the assignment", linkSegmentType.getXmlId()));
      }
    }

    /*
     * check if the speed limit of the link differs from the link segment type mode's limit, Only override the FD for the link if the speed limit is MORE restrictive, if it is not,
     * the link segment type is more restrictive and is to be used. It is unlikely that the link speed is more restrictive, so log this as well so the user can decide if it is to
     * be altered
     */
    for (MacroscopicLinkSegment linkSegment : parentNetworkLayer.getLinkSegments()) {
      if (Precision.isSmaller(linkSegment.getPhysicalSpeedLimitKmH(), linkSegment.getLinkSegmentType().getMaximumSpeedKmH(mode))) {
        LOGGER.warning(String.format("Physical speed limit (%.2f) on link segment %s is more restrictive than the speed limit (%.2f) of the applied link segment type %s",
            linkSegment.getPhysicalSpeedLimitKmH(), linkSegment.getXmlId(), linkSegment.getLinkSegmentType().getMaximumSpeedKmH(mode)));

        /* updated FD */
        double modeSpeedLimit = linkSegment.getModelledSpeedLimitKmH(mode);
        LOGGER.info(String.format("Overwriting fundamental diagram used on link segment %s, restricting free flow speed to %.2f", linkSegment.getXmlId(), modeSpeedLimit));
        NewellFundamentalDiagram oldFd = (NewellFundamentalDiagram) get(linkSegment);
        NewellFundamentalDiagram newFd = oldFd.clone();
        newFd.setMaximumSpeedKmHour(modeSpeedLimit);

        /* register */
        register(linkSegment, newFd);
      }
    }
  }

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  public NewellFundamentalDiagramComponent(final IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  public NewellFundamentalDiagramComponent(final NewellFundamentalDiagramComponent other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NewellFundamentalDiagramComponent clone() {
    return new NewellFundamentalDiagramComponent(this);
  }

  /**
   * Register the given Newell fundamental diagram for the link segment. This overrules the fundamental diagram that would be used based on the link segment's type. In case there
   * already exists an identical fundamental diagram (based on relaxed hashcode comparison), the link segment is assigned the already present fundamental diagram. The fundamental
   * diagram used for the link segment is returned, which is either the passed in one, or an already present functionally identical version
   * 
   * @param linkSegment        to use
   * @param fundamentalDiagram to register
   * @return used Fd for the link segment, can be different (but functionally equivalent) if registered Fd was already present for another link segment
   */
  public FundamentalDiagram register(final MacroscopicLinkSegment linkSegment, final NewellFundamentalDiagram fundamentalDiagram) {
    return super.register(linkSegment, fundamentalDiagram);
  }

  /**
   * Register the given Newell fundamental diagram for the link segment type. In case there already exists an identical fundamental diagram (based on relaxed hashcode comparison),
   * the link segment type is assigned the already present fundamental diagram. The fundamental diagram used for the link segment type is returned, which is either the passed in
   * one, or an already present functionally identical version
   * 
   * @param linkSegmentType    to use
   * @param fundamentalDiagram to register
   * @return used Fd for the link segment type, can be different (but functionally equivalent) if registered Fd was already present for another link segment
   */
  public FundamentalDiagram register(final MacroscopicLinkSegmentType linkSegmentType, final NewellFundamentalDiagram fundamentalDiagram) {
    return super.register(linkSegmentType, fundamentalDiagram);
  }

}
