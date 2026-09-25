package org.goplanit.test.network;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.goplanit.converter.idmapping.NetworkIdMapper;
import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.network.layer.macroscopic.intersection.IntersectionsImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.IdMapperType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.macroscopic.intersection.Intersection;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionControlType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionType;
import org.goplanit.utils.network.layer.macroscopic.intersection.IntersectionUtils;
import org.goplanit.utils.network.layer.physical.Node;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the intersections memory model: member nodes, control type, kinds, approach and internal segments, turns
 * restricted by the layer's banned movements, the container's lookup by member node, and that intersections leave the
 * network itself untouched
 *
 * @author markr
 */
public class IntersectionsTest {

  private MacroscopicNetworkLayer layer;
  private MacroscopicLinkSegmentType road;
  private List<Node> nodes;
  private IntersectionsImpl intersections;

  @BeforeEach
  public void setUp() {
    IdGenerator.reset();
    var network = new MacroscopicNetwork(IdGroupingToken.collectGlobalToken());
    layer = network.getTransportLayers().getFactory().registerNew();
    road = layer.getLinkSegmentTypes().getFactory().registerNew("road", 1800, 180);
    nodes = new ArrayList<>();
    intersections = (IntersectionsImpl) layer.getIntersections();
  }

  @AfterEach
  public void tearDown() {
    IdGenerator.reset();
  }

  private void createNodes(int count) {
    for (int i = 0; i < count; ++i) {
      nodes.add(layer.getNodes().getFactory().registerNew());
    }
  }

  /** connect a and b with a segment in each direction */
  private void twoWay(int a, int b) {
    var link = layer.getLinks().getFactory().registerNew(nodes.get(a), nodes.get(b), 1, true);
    layer.getLinkSegments().getFactory().registerNew(link, true, true).setLinkSegmentType(road);
    layer.getLinkSegments().getFactory().registerNew(link, false, true).setLinkSegmentType(road);
  }

  /** the registered link segment running from node a to node b */
  private MacroscopicLinkSegment segment(int a, int b) {
    return layer.getLinkSegments().stream().filter(
        ls -> ls.getUpstreamVertex() == nodes.get(a) && ls.getDownstreamVertex() == nodes.get(b)).findFirst().orElseThrow();
  }

  /** centre node 0 with arms to nodes 1 to 4, node 5 next to the centre and node 6 beyond it, all links two-way */
  private void createJunctionArea() {
    createNodes(7);
    for (int arm = 1; arm <= 4; ++arm) {
      twoWay(0, arm);
    }
    twoWay(0, 5);
    twoWay(5, 6);
  }

  /** a signalised junction at node 0, not registered */
  private Intersection junctionAtCentre() {
    return intersections.getFactory().create(nodes.get(0), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);
  }

  /** junction over nodes 0 and 5: approaches from 1 and 6, internal both ways */
  private Intersection twoNodeJunction() {
    var junction = junctionAtCentre();
    junction.addMemberNode(nodes.get(5));
    junction.addApproachSegment(segment(1, 0));
    junction.addApproachSegment(segment(6, 5));
    junction.addInternalSegment(segment(0, 5));
    junction.addInternalSegment(segment(5, 0));
    return junction;
  }

  // MEMBER NODES

  @Test
  public void nodeInTwoIntersectionsIsReported() {
    createJunctionArea();
    var first = intersections.getFactory().registerNew(
        nodes.get(0), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);
    var second = intersections.getFactory().create(
        nodes.get(1), IntersectionControlType.UNSIGNALISED, IntersectionType.JUNCTION);
    assertTrue(second.addMemberNode(nodes.get(0)));
    intersections.getFactory().register(second);

    var shared = IntersectionUtils.findNodesInMoreThanOneIntersection(intersections);
    assertEquals(1, shared.size());
    assertTrue(shared.get(nodes.get(0)).contains(first) && shared.get(nodes.get(0)).contains(second));
  }

  @Test
  public void addingStartOfApproachAsMemberIsRefused() {
    createJunctionArea();
    var junction = junctionAtCentre();
    junction.addApproachSegment(segment(1, 0));

    assertFalse(junction.addMemberNode(nodes.get(1)));
    assertEquals(List.of(nodes.get(0)), junction.getMemberNodes());
  }

  @Test
  public void removingMemberNodeWithReferencesRemovesThem() {
    createJunctionArea();
    var junction = twoNodeJunction();

    assertTrue(junction.removeMemberNode(nodes.get(5), true));
    assertEquals(List.of(nodes.get(0)), junction.getMemberNodes());
    assertEquals(List.of(segment(1, 0)), junction.getApproachSegments());
    assertTrue(junction.getInternalSegments().isEmpty());
  }

  @Test
  public void removingOnlyMemberNodeKeepsReferences() {
    createJunctionArea();
    var junction = twoNodeJunction();

    assertTrue(junction.removeMemberNode(nodes.get(5), false));
    assertEquals(List.of(nodes.get(0)), junction.getMemberNodes());
    assertEquals(2, junction.getApproachSegments().size());
    assertEquals(2, junction.getInternalSegments().size());
  }

  // CONTROL TYPE

  @Test
  public void nodeOutsideAnyIntersectionIsNotSignalised() {
    createJunctionArea();
    intersections.getFactory().registerNew(nodes.get(0), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);

    assertTrue(intersections.isSignalised(nodes.get(0)));
    assertFalse(intersections.isSignalised(nodes.get(1)));
  }

  @Test
  public void changingControlTypeKeepsTheRest() {
    createJunctionArea();
    var junction = twoNodeJunction();
    intersections.getFactory().register(junction);

    junction.setControlType(IntersectionControlType.UNSIGNALISED);
    assertEquals(IntersectionControlType.UNSIGNALISED, junction.getControlType());
    assertFalse(intersections.isSignalised(nodes.get(0)));
    assertEquals(List.of(nodes.get(0), nodes.get(5)), junction.getMemberNodes());
    assertEquals(2, junction.getApproachSegments().size());
    assertEquals(2, junction.getInternalSegments().size());
  }

  // KINDS

  @Test
  public void bothKindsEachOnce() {
    createJunctionArea();
    var junction = junctionAtCentre();

    assertTrue(junction.addType(IntersectionType.CROSSING));
    assertFalse(junction.addType(IntersectionType.JUNCTION));
    assertEquals(EnumSet.of(IntersectionType.JUNCTION, IntersectionType.CROSSING), junction.getTypes());
  }

  // APPROACHES

  @Test
  public void approachThatDoesNotEnterIsRefused() {
    createJunctionArea();
    var junction = junctionAtCentre();

    assertFalse(junction.addApproachSegment(segment(0, 1)));   // starts at a member
    assertFalse(junction.addApproachSegment(segment(5, 6)));   // does not end at a member
    assertTrue(junction.getApproachSegments().isEmpty());
  }

  @Test
  public void segmentIsAnApproachOnce() {
    createJunctionArea();
    var junction = junctionAtCentre();

    assertTrue(junction.addApproachSegment(segment(1, 0)));
    assertFalse(junction.addApproachSegment(segment(1, 0)));
    assertEquals(1, junction.getApproachSegments().size());
    assertSame(segment(1, 0), junction.getApproachSegment(segment(1, 0).getId()));
  }

  // TURNS

  @Test
  public void banFromApproachIsFoundWithoutChangingTheIntersection() {
    createJunctionArea();
    var junction = twoNodeJunction();
    intersections.getFactory().register(junction);
    var ban = layer.getBannedMovements().getFactory().registerNew(segment(1, 0), segment(0, 2));
    layer.getBannedMovements().getFactory().registerNew(segment(2, 0), segment(0, 3));   // not from an approach

    var bansByApproachSegment =
        IntersectionUtils.findBannedMovementsByApproachSegment(intersections, layer.getBannedMovements());
    assertEquals(1, bansByApproachSegment.size());
    assertEquals(List.of(ban), bansByApproachSegment.get(segment(1, 0)));

    var bansByIntersection =
        IntersectionUtils.findBannedMovementsByIntersection(intersections, layer.getBannedMovements());
    assertEquals(List.of(ban), bansByIntersection.get(junction));
    assertEquals(2, junction.getApproachSegments().size());
  }

  // INTERNAL SEGMENTS

  @Test
  public void internalSegmentWithOutsideEndIsRefused() {
    createJunctionArea();
    var junction = junctionAtCentre();

    assertFalse(junction.addInternalSegment(segment(0, 1)));
    assertTrue(junction.getInternalSegments().isEmpty());
  }

  @Test
  public void unlistedSegmentBetweenMembersIsNeitherInternalNorApproach() {
    createJunctionArea();
    var junction = junctionAtCentre();
    junction.addMemberNode(nodes.get(5));

    assertNull(junction.getInternalSegment(segment(0, 5).getId()));
    assertNull(junction.getApproachSegment(segment(0, 5).getId()));
  }

  // LOOKUP BY MEMBER NODE

  @Test
  public void registeredIntersectionIsFoundByEachMember() {
    createJunctionArea();
    var junction = twoNodeJunction();
    intersections.getFactory().register(junction);

    assertSame(junction, intersections.getByMemberNode(nodes.get(0)));
    assertSame(junction, intersections.getByMemberNode(nodes.get(5)));
    assertNull(intersections.getByMemberNode(nodes.get(1)));
  }

  @Test
  public void memberAddedAfterRegistrationIsNotFound() {
    createJunctionArea();
    var junction = intersections.getFactory().registerNew(
        nodes.get(0), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);

    assertTrue(junction.addMemberNode(nodes.get(5)));
    assertNull(intersections.getByMemberNode(nodes.get(5)));
  }

  @Test
  public void registeredIntersectionIsFoundBySegmentUntilRemoved() {
    createJunctionArea();
    var junction = twoNodeJunction();
    intersections.getFactory().register(junction);

    assertSame(junction, intersections.getBySegment(segment(1, 0)));   // approach
    assertSame(junction, intersections.getBySegment(segment(0, 5)));   // internal
    assertNull(intersections.getBySegment(segment(0, 1)));             // leaves it

    intersections.remove(junction);
    assertNull(intersections.getBySegment(segment(1, 0)));
  }

  @Test
  public void removedIntersectionIsNoLongerFound() {
    createJunctionArea();
    var junction = intersections.getFactory().registerNew(
        nodes.get(0), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);

    intersections.remove(junction);
    assertNull(intersections.getByMemberNode(nodes.get(0)));
    assertFalse(intersections.isSignalised(nodes.get(0)));
  }

  // LAYER

  @Test
  public void newLayerHasEmptyIntersections() {
    createJunctionArea();

    assertTrue(layer.getIntersections().isEmpty());
    assertFalse(layer.hasIntersections());
    assertFalse(layer.isSignalised(nodes.get(0)));
  }

  @Test
  public void layerReportsSignalisedMemberNode() {
    createJunctionArea();
    intersections.getFactory().registerNew(nodes.get(0), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);

    assertTrue(layer.hasIntersections());
    assertTrue(layer.isSignalised(nodes.get(0)));
    assertFalse(layer.isSignalised(nodes.get(1)));
  }

  @Test
  public void recreatingLayerIdsKeepsReferencesAndLookups() {
    createJunctionArea();
    var gone = intersections.getFactory().registerNew(
        nodes.get(3), IntersectionControlType.UNSIGNALISED, IntersectionType.JUNCTION);
    var junction = twoNodeJunction();
    intersections.getFactory().register(junction);
    intersections.remove(gone);                                   // leaves a gap in the ids
    var approachSegment = segment(1, 0);

    layer.recreateManagedIds(IdGroupingToken.collectGlobalToken());

    assertEquals(0, junction.getId());                            // contiguous again
    assertSame(junction, intersections.getByMemberNode(nodes.get(0)));
    assertSame(junction, intersections.getBySegment(approachSegment));
    assertSame(approachSegment, junction.getApproachSegment(approachSegment.getId()));
    assertTrue(junction.removeApproachSegment(approachSegment));
  }

  @Test
  public void resettingLayerEmptiesIntersections() {
    createJunctionArea();
    intersections.getFactory().registerNew(nodes.get(0), IntersectionControlType.SIGNALISED, IntersectionType.JUNCTION);

    layer.resetChildManagedIdEntities();

    assertTrue(layer.getIntersections().isEmpty());
    assertNull(intersections.getByMemberNode(nodes.get(0)));
  }

  // COPYING

  @Test
  public void deepCopyOfLayerRefersOnlyToCopiedEntities() {
    createJunctionArea();
    var junction = twoNodeJunction();
    junction.addType(IntersectionType.CROSSING);
    junction.setExternalId("osm:123");
    intersections.getFactory().register(junction);

    var copy = layer.deepClone();

    assertEquals(1, copy.getIntersections().size());
    var copied = copy.getIntersections().getFirst();
    assertNotSame(junction, copied);
    assertEquals("osm:123", copied.getExternalId());
    assertEquals(junction.getTypes(), copied.getTypes());
    assertEquals(junction.getControlType(), copied.getControlType());

    /* every reference is the copy's own entity, checked by identity since a copy equals its original by id */
    assertEquals(2, copied.getMemberNodes().size());
    for (var node : copied.getMemberNodes()) {
      assertSame(copy.getNodes().get(node.getId()), node);
    }
    var copiedSegments = new ArrayList<>(copied.getApproachSegments());
    copiedSegments.addAll(copied.getInternalSegments());
    assertEquals(4, copiedSegments.size());
    for (var segment : copiedSegments) {
      assertSame(copy.getLinkSegments().get(segment.getId()), segment);
    }

    /* the copy's lookups find the copy through the copy's own entities, and not through the original's */
    var copiedIntersections = (IntersectionsImpl) copy.getIntersections();
    assertSame(copied, copiedIntersections.getByMemberNode(copy.getNodes().get(nodes.get(0).getId())));
    assertSame(copied, copiedIntersections.getBySegment(copy.getLinkSegments().get(segment(1, 0).getId())));
    assertNull(copiedIntersections.getByMemberNode(nodes.get(0)));

    /* the original is untouched */
    assertSame(nodes.get(0), junction.getMemberNodes().get(0));
    assertSame(junction, intersections.getByMemberNode(nodes.get(0)));
  }

  @Test
  public void shallowCopyOfLayerSharesIntersections() {
    createJunctionArea();
    var junction = twoNodeJunction();
    intersections.getFactory().register(junction);

    var copy = layer.shallowClone();

    assertSame(junction, copy.getIntersections().getFirst());
    assertSame(junction, copy.getIntersections().getByMemberNode(nodes.get(0)));
  }

  // IDENTITY

  @Test
  public void externalIdIsKept() {
    createJunctionArea();
    var junction = junctionAtCentre();
    junction.setExternalId("osm:123");

    assertEquals("osm:123", intersections.getFactory().register(junction).getExternalId());
  }

  @Test
  public void idMapperMapsIntersectionsByEachIdType() {
    createJunctionArea();
    var junction = intersections.getFactory().register(junctionAtCentre());
    junction.setXmlId("x1");
    junction.setExternalId("osm:1");

    assertEquals("x1", new NetworkIdMapper(IdMapperType.XML).getIntersectionIdMapper().apply(junction));
    assertEquals("osm:1", new NetworkIdMapper(IdMapperType.EXTERNAL_ID).getIntersectionIdMapper().apply(junction));
    assertEquals(String.valueOf(junction.getId()),
        new NetworkIdMapper(IdMapperType.ID).getIntersectionIdMapper().apply(junction));
  }

  // DESCRIPTIVE ONLY

  @Test
  public void intersectionsLeaveTheNetworkUnchanged() {
    createJunctionArea();
    var segmentsBefore = new ArrayList<>(layer.getLinkSegments().toCollection());
    int linksBefore = layer.getLinks().size();

    intersections.getFactory().register(twoNodeJunction());

    assertEquals(linksBefore, layer.getLinks().size());
    assertEquals(segmentsBefore.size(), layer.getLinkSegments().size());
    assertEquals(1, layer.getLinkSegmentTypes().size());
    for (var linkSegment : segmentsBefore) {
      assertSame(linkSegment, layer.getLinkSegments().get(linkSegment.getId()));
      assertSame(road, linkSegment.getLinkSegmentType());
    }
  }
}
