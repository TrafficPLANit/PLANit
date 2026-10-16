package org.goplanit.network.layer.macroscopic;

import org.goplanit.utils.misc.binning.BinnedCount;
import org.goplanit.utils.mode.Mode;

import java.util.ArrayList;
import java.util.List;

/**
 * What restricting a single mode's access to its connected subnetworks came to, see
 * {@link MacroscopicNetworkLayerUtils#restrictModeAccessToConnectedSubNetworks}, as a value so the caller decides what
 * to log.
 *
 * @author markr
 */
public class ModeAccessRestrictionResult {

  /** the mode whose access was judged */
  private final Mode mode;

  /** number of link segments the mode lost access to */
  private final int withdrawn;

  /** number of link segments that kept the mode's access only because they were protected */
  private final int protectedSegments;

  /** number of subnetworks the mode was found to have */
  private final int subNetworksFound;

  /** number of those subnetworks that kept the mode's access */
  private final int subNetworksRetained;

  /** number of vertices in the mode's largest subnetwork */
  private final int largestSubNetworkSize;

  /** number of discarded subnetworks per size bin */
  private final BinnedCount<Integer> discardedSubNetworks;

  /** number of link segments the mode lost access to, per size bin of the subnetwork they belonged to, binned the
   * same way as the discarded subnetworks */
  private final BinnedCount<Integer> withdrawnLinkSegments;

  /**
   * Constructor
   *
   * @param mode whose access was judged
   * @param withdrawn number of link segments the mode lost access to
   * @param protectedSegments number of link segments that kept access only because they were protected
   * @param subNetworksFound number of subnetworks the mode was found to have
   * @param subNetworksRetained number of those subnetworks that kept the mode's access
   * @param largestSubNetworkSize number of vertices in the mode's largest subnetwork
   * @param discardedSubNetworks number of discarded subnetworks per size bin
   * @param withdrawnLinkSegments number of link segments access was withdrawn on per size bin, binned the same way
   */
  ModeAccessRestrictionResult(Mode mode, int withdrawn, int protectedSegments, int subNetworksFound,
      int subNetworksRetained, int largestSubNetworkSize, BinnedCount<Integer> discardedSubNetworks,
      BinnedCount<Integer> withdrawnLinkSegments) {
    this.discardedSubNetworks = discardedSubNetworks;
    this.withdrawnLinkSegments = withdrawnLinkSegments;
    this.mode = mode;
    this.withdrawn = withdrawn;
    this.protectedSegments = protectedSegments;
    this.subNetworksFound = subNetworksFound;
    this.subNetworksRetained = subNetworksRetained;
    this.largestSubNetworkSize = largestSubNetworkSize;
  }

  /**
   * Number of subnetworks this mode was found to have
   *
   * @return count
   */
  public int getSubNetworksFound() {
    return subNetworksFound;
  }

  /**
   * Number of subnetworks that kept the mode's access
   *
   * @return count
   */
  public int getSubNetworksRetained() {
    return subNetworksRetained;
  }

  /**
   * Number of vertices in the mode's largest subnetwork
   *
   * @return count
   */
  public int getLargestSubNetworkSize() {
    return largestSubNetworkSize;
  }

  /**
   * How many subnetworks were discarded, by the size of the subnetwork.
   * <p>
   * Reported because the totals alone mislead when choosing a size threshold: a mode can have thousands of
   * subnetworks discarded while barely any link segment loses access, most of them being a single vertex with
   * no traversable segment between anything. Where the link segments sit, see
   * {@link #getWithdrawnLinkSegmentCounts()}, is what a threshold actually decides.
   * </p>
   *
   * @return counts per size bin
   */
  public BinnedCount<Integer> getDiscardedSubNetworkCounts() {
    return discardedSubNetworks;
  }

  /**
   * How many link segments lost the mode's access, by the size of the subnetwork they belonged to
   *
   * @return counts per size bin
   */
  public BinnedCount<Integer> getWithdrawnLinkSegmentCounts() {
    return withdrawnLinkSegments;
  }

  /**
   * The size distribution rendered a line per bin, for a caller to log indented beneath its own header. Bins
   * without anything in them are left out, and the labels are padded so the numbers line up.
   *
   * @return one line per non-empty size bin, empty when nothing was discarded
   */
  public List<String> getSizeBinSummaryLines() {
    var sizeBins = discardedSubNetworks.getConfiguration();
    int labelWidth = 0;
    for (int index = 0; index < sizeBins.size(); ++index) {
      if (discardedSubNetworks.getCount(index) > 0 || withdrawnLinkSegments.getCount(index) > 0) {
        labelWidth = Math.max(labelWidth, sizeBins.getBin(index).getLabel().length());
      }
    }

    var lines = new ArrayList<String>();
    for (int index = 0; index < sizeBins.size(); ++index) {
      final long discarded = discardedSubNetworks.getCount(index);
      final long withdrawnSegments = withdrawnLinkSegments.getCount(index);
      if (discarded == 0 && withdrawnSegments == 0) {
        continue;
      }
      lines.add(String.format("size %-" + labelWidth + "s : %d subnetworks discarded, %d link segments withdrawn",
          sizeBins.getBin(index).getLabel(), discarded, withdrawnSegments));
    }
    return lines;
  }

  /**
   * The mode this concerns
   *
   * @return mode
   */
  public Mode getMode() {
    return mode;
  }

  /**
   * Number of link segments the mode lost access to
   *
   * @return count
   */
  public int getWithdrawn() {
    return withdrawn;
  }

  /**
   * Number of link segments that kept access despite not qualifying, because they were protected
   *
   * @return count
   */
  public int getProtected() {
    return protectedSegments;
  }

  /**
   * Verify whether anything changed
   *
   * @return true when nothing was withdrawn
   */
  public boolean isEmpty() {
    return withdrawn == 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return String.format(
        "%s: %d subnetworks found, %d kept (largest %d vertices), %d discarded, access withdrawn on %d link " +
            "segments%s",
        mode.getName(), subNetworksFound, subNetworksRetained, largestSubNetworkSize,
        subNetworksFound - subNetworksRetained, withdrawn,
        protectedSegments > 0 ? String.format(", %d protected", protectedSegments) : "");
  }
}
