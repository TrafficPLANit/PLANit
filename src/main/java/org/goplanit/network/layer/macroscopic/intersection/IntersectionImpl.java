package org.goplanit.network.layer.macroscopic.intersection;

import java.util.ArrayList;
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
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.physical.Node;

/**
 * An intersection: the member nodes forming a junction or crossing, how it is controlled, and the link segments
 * entering it and lying inside it. Its methods change only this intersection; keeping it consistent with
 * the rest of the layer, and with the container's index, is up to the layer modifier
 *
 * @author markr
 */
public class IntersectionImpl extends ExternalIdAbleImpl implements Intersection {

  /** generated UID */
  private static final long serialVersionUID = 4112560832261209945L;

  /** member nodes, held in a list since entities hash by id and recreating ids changes ids in place */
  private final List<Node> memberNodes;

  /** how the intersection is controlled */
  private IntersectionControlType controlType;

  /** kinds of the intersection, each at most once */
  private final Set<IntersectionType> types;

  /** link segments entering the intersection, each at most once */
  private final List<MacroscopicLinkSegment> approachSegments;

  /** link segments lying inside the intersection, running between member nodes */
  private final List<MacroscopicLinkSegment> internalSegments;

  /**
   * Constructor
   *
   * @param groupId contiguous id generation within this group for instances of this class
   * @param node first member node
   * @param controlType of the intersection
   * @param type first kind of the intersection
   */
  protected IntersectionImpl(
      final IdGroupingToken groupId, final Node node, final IntersectionControlType controlType,
      final IntersectionType type) {
    super(IdGenerator.generateId(groupId, INTERSECTION_ID_CLASS));
    this.memberNodes = new ArrayList<>(1);
    this.memberNodes.add(node);
    this.controlType = controlType;
    this.types = EnumSet.of(type);
    this.approachSegments = new ArrayList<>();
    this.internalSegments = new ArrayList<>(0);
  }

  /**
   * Copy constructor. Shallow and deep copies alike hold their own collections, referring to the same nodes and
   * segments; re-pointing those to copied nodes and segments is up to the copy of the layer
   *
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected IntersectionImpl(final IntersectionImpl other, boolean deepCopy) {
    super(other);
    this.memberNodes = new ArrayList<>(other.memberNodes);
    this.controlType = other.controlType;
    this.types = EnumSet.copyOf(other.types);
    this.approachSegments = new ArrayList<>(other.approachSegments);
    this.internalSegments = new ArrayList<>(other.internalSegments);
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
  public boolean addMemberNode(Node node) {
    if (hasMemberNode(node) || approachSegments.stream().anyMatch(segment -> segment.getUpstreamVertex() == node)) {
      return false;
    }
    return memberNodes.add(node);
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
      approachSegments.removeIf(segment -> segment.getDownstreamVertex() == node);
      internalSegments.removeIf(segment -> segment.getUpstreamVertex() == node || segment.getDownstreamVertex() == node);
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
  public List<MacroscopicLinkSegment> getApproachSegments() {
    return Collections.unmodifiableList(approachSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegment getApproachSegment(long segmentId) {
    return IdAbleUtils.findById(approachSegments, segmentId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addApproachSegment(MacroscopicLinkSegment segment) {
    if (approachSegments.stream().anyMatch(held -> held == segment) || !hasMemberNode(segment.getDownstreamVertex())
        || hasMemberNode(segment.getUpstreamVertex())) {
      return false;
    }
    return approachSegments.add(segment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeApproachSegment(MacroscopicLinkSegment segment) {
    return approachSegments.removeIf(held -> held == segment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MacroscopicLinkSegment> getInternalSegments() {
    return Collections.unmodifiableList(internalSegments);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MacroscopicLinkSegment getInternalSegment(long segmentId) {
    return IdAbleUtils.findById(internalSegments, segmentId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean addInternalSegment(MacroscopicLinkSegment segment) {
    if (internalSegments.stream().anyMatch(held -> held == segment) || !hasMemberNode(segment.getUpstreamVertex())
        || !hasMemberNode(segment.getDownstreamVertex())) {
      return false;
    }
    return internalSegments.add(segment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeInternalSegment(MacroscopicLinkSegment segment) {
    return internalSegments.removeIf(held -> held == segment);
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
   * Update the approach and internal segments based on the mapping provided, e.g. to their copies after a deep copy of
   * the layer
   *
   * @param segmentToSegmentMapping should contain the segment as currently used and then the value is the new segment
   *                                to replace it
   * @param removeMissingMappings when true a segment without a mapping is removed, otherwise it is left in-tact
   */
  protected void updateSegmentMapping(
      Function<MacroscopicLinkSegment, MacroscopicLinkSegment> segmentToSegmentMapping, boolean removeMissingMappings) {
    ListUtils.updateMapping(approachSegments, segmentToSegmentMapping, removeMissingMappings);
    ListUtils.updateMapping(internalSegments, segmentToSegmentMapping, removeMissingMappings);
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
  public IntersectionImpl shallowClone() {
    return new IntersectionImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IntersectionImpl deepClone() {
    return new IntersectionImpl(this, true);
  }
}
