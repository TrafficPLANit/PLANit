package org.goplanit.network.layer.macroscopic.intersection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.goplanit.utils.containers.ListUtils;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdAbleUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionSegment;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * What both kinds of intersection share: the member nodes forming a junction or crossing, how it is controlled, its
 * kinds, and its approaches: the link segments entering it, each with whether it has a signal of its own and, when set,
 * its green ratio and saturation flow factor. Its methods change only this intersection; keeping it consistent with the
 * rest of the layer, and with the container's index, is up to the layer modifier
 *
 * @author markr
 */
public abstract class IntersectionImpl extends ExternalIdAbleImpl implements Intersection {

  /** generated UID */
  private static final long serialVersionUID = 4112560832261209945L;

  /** member nodes, held in a list since entities hash by id and recreating ids changes ids in place */
  private final List<Node> memberNodes;

  /** how the intersection is controlled */
  private IntersectionControlType controlType;

  /** kinds of the intersection, each at most once */
  private final Set<IntersectionType> types;

  /** approaches, one for each link segment entering the intersection, with a signal of its own or not, in the order
   * added */
  private final List<IntersectionSegmentImpl> approaches;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param memberNodes of the intersection
   * @param controlType of the intersection
   * @param types kinds of the intersection, at least one
   */
  protected IntersectionImpl(
      final IdGroupingToken groupId, final Collection<Node> memberNodes, final IntersectionControlType controlType,
      final Collection<IntersectionType> types) {
    super(IdGenerator.generateId(groupId, INTERSECTION_ID_CLASS));
    this.memberNodes = new ArrayList<>(memberNodes);
    this.controlType = controlType;
    this.types = EnumSet.copyOf(types);
    this.approaches = new ArrayList<>();
  }

  /**
   * Copy constructor. Shallow and deep copies alike hold their own collections and their own copies of the approaches,
   * referring to the same nodes and segments; re-pointing those to copied nodes and segments is up to the copy of the
   * layer
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected IntersectionImpl(final IntersectionImpl other, boolean deepCopy) {
    super(other);
    this.memberNodes = new ArrayList<>(other.memberNodes);
    this.controlType = other.controlType;
    this.types = EnumSet.copyOf(other.types);
    this.approaches = new ArrayList<>(other.approaches.size());
    other.approaches.forEach(approach -> approaches.add(approach.deepClone()));
  }

  /**
   * Remove what a kind of intersection holds on top of what all intersections hold, for a member node just removed
   * together with its dependent references. The approach segments ending at the node are already removed
   *
   * @param node the member node removed
   */
  protected void removeReferencesOfRemovedMemberNode(Node node) {
    /* nothing on top for an intersection that holds approaches only */
  }

  /**
   * Remove what a kind of intersection holds on top of what all intersections hold, for an approach segment just
   * removed
   *
   * @param approachSegment the approach segment removed
   */
  protected void removeReferencesOfRemovedApproachSegment(MacroscopicLinkSegment approachSegment) {
    /* nothing on top for an intersection that holds approaches only */
  }

  /**
   * Add a member node, after the kind of intersection has checked it may
   *
   * @param node to add
   * @return true when added
   */
  protected boolean addToMemberNodes(Node node) {
    return memberNodes.add(node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Node> getMemberNodes() {
    return Collections.unmodifiableList(memberNodes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getMemberNode(long nodeId) {
    return IdAbleUtils.findById(memberNodes, nodeId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeMemberNode(Node node, boolean removeDependentReferences) {
    if (!memberNodes.removeIf(member -> member == node)) {
      return false;
    }
    if (removeDependentReferences) {
      approaches.removeIf(approach -> approach.getLinkSegment().getDownstreamVertex() == node);
      removeReferencesOfRemovedMemberNode(node);
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionControlType getControlType() {
    return controlType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setControlType(IntersectionControlType controlType) {
    this.controlType = controlType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<IntersectionType> getTypes() {
    return Collections.unmodifiableSet(types);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addType(IntersectionType type) {
    return types.add(type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<IntersectionSegment> getApproaches() {
    return Collections.unmodifiableList(approaches);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionSegment getApproach(MacroscopicLinkSegment entrySegment) {
    return IntersectionSegmentUtils.findByLinkSegment(approaches, entrySegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionSegment addNewApproach(MacroscopicLinkSegment entrySegment, boolean hasOwnSignal) {
    if (!IntersectionUtils.isSegmentEnteringFromOutsideThatIsNotAnApproach(this, entrySegment)) {
      return null;
    }
    var approach = IntersectionSegmentFactory.create(entrySegment, hasOwnSignal);
    approaches.add(approach);
    return approach;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeApproachSegment(MacroscopicLinkSegment segment) {
    removeReferencesOfRemovedApproachSegment(segment);
    return IntersectionSegmentUtils.removeByLinkSegment(approaches, segment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean setEntrySegmentOnApproach(
      MacroscopicLinkSegment approachSegment, MacroscopicLinkSegment entrySegment) {
    var approach = IntersectionSegmentUtils.findByLinkSegment(approaches, approachSegment);
    if (approach == null || !IntersectionUtils.isSegmentEnteringFromOutsideThatIsNotAnApproach(this, entrySegment)) {
      return false;
    }
    approach.setLinkSegment(entrySegment);
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionSegment swapApproachToNewSignalOrUnsignalised(
      MacroscopicLinkSegment approachSegment, boolean hasOwnSignal) {
    return IntersectionSegmentUtils.swapToSignalOrUnsignalised(approaches, approachSegment, hasOwnSignal);
  }

  /**
   * Update the member nodes based on the mapping provided, e.g. to their copies after a deep copy of the layer
   *
   * @param nodeToNodeMapping should contain the member node as currently used and then the value is the new node to
   *                          replace it
   * @param removeMissingMappings when true a member node without a mapping is removed, otherwise it is left in-tact
   */
  protected void updateMemberNodeMapping(Function<Node, Node> nodeToNodeMapping, boolean removeMissingMappings) {
    ListUtils.updateMapping(memberNodes, nodeToNodeMapping, removeMissingMappings);
  }

  /**
   * Update the segments based on the mapping provided, e.g. to their copies after a deep copy of the layer
   *
   * @param segmentToSegmentMapping should contain the segment as currently used and then the value is the new segment
   *                                to replace it
   * @param removeMissingMappings when true an approach whose segment has no mapping is removed, otherwise it is left
   *                              in-tact
   */
  protected void updateSegmentMapping(
      Function<MacroscopicLinkSegment, MacroscopicLinkSegment> segmentToSegmentMapping, boolean removeMissingMappings) {
    // each approach updates its own link segment; one without a mapping is removed when asked
    approaches.removeIf(
        approach -> !approach.updateLinkSegmentMapping(segmentToSegmentMapping) && removeMissingMappings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    setId(IdGenerator.generateId(tokenId, getIdClass()));
    return getId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract IntersectionImpl shallowClone();

  /**
   * {@inheritDoc}
   */
  @Override
  public abstract IntersectionImpl deepClone();
}
