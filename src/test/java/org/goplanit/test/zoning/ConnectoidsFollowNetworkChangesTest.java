package org.goplanit.test.zoning;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;
import org.goplanit.zoning.Zoning;
import org.goplanit.utils.zoning.modifier.event.RemoveConnectoidEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A zoning made on a network keeps its connectoids in step with the changes made through the modifier of each layer:
 * from then on, also after the other listeners of a layer are removed, until detached, and the same for its copies
 *
 * <pre>
 *   0        1        2      three nodes on a line, no links; a transfer connectoid at node 1, made on the network
 * </pre>
 */
public class ConnectoidsFollowNetworkChangesTest extends LayerTestBase {

  /** the network the zoning is made on */
  private MacroscopicNetwork network;

  /** the zoning made on the network */
  private Zoning zoning;

  /** the transfer connectoid at node 1 */
  private TransferConnectoid connectoidAtNode1;

  /**
   * Remove a node of the layer through its modifier
   *
   * @param node index of the node to remove
   */
  private void removeNode(int node) {
    layer.getLayerModifier().removeVertex(nodes.get(node));
  }

  /** One layer with three nodes, and a zoning made on the network with a transfer connectoid at node 1 */
  @BeforeEach
  public void setUp() {
    var token = IdGroupingToken.collectGlobalToken();
    network = new MacroscopicNetwork(token);
    var bus = network.getModes().getFactory().registerNew(PredefinedModeType.BUS);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(bus));
    createNodesOnLine(3);
    zoning = new Zoning(token, network);
    connectoidAtNode1 = zoning.getTransferConnectoids().getFactory().registerNew(nodes.get(1));
  }

  /** The zoning follows the layer also after the other listeners of the layer are removed */
  @Test
  public void followingSurvivesClearingTheOtherListeners() {
    layer.getLayerModifier().removeAllNonInternalListeners();

    removeNode(1);

    assertTrue(zoning.getTransferConnectoids().isEmpty());
  }

  /** A zoning detached from the network no longer follows it */
  @Test
  public void detachedZoningNoLongerFollows() {
    zoning.detachFrom(network);

    removeNode(1);

    assertEquals(List.of(connectoidAtNode1), List.copyOf(zoning.getTransferConnectoids().toCollection()));
  }

  /** A deep copy follows the network its original follows */
  @Test
  public void copyFollowsItsOriginalsNetwork() {
    var copy = zoning.deepClone();

    removeNode(1);

    assertTrue(copy.getTransferConnectoids().isEmpty());
    assertTrue(zoning.getTransferConnectoids().isEmpty());
  }

  /** A copy detached from the network keeps its connectoid, while the original loses its own */
  @Test
  public void detachedCopyNoLongerFollows() {
    var copy = zoning.deepClone();
    copy.detachFrom(network);

    removeNode(1);

    assertEquals(1, copy.getTransferConnectoids().size());
    assertTrue(zoning.getTransferConnectoids().isEmpty());
  }

  /** An OD connectoid at a removed node is removed */
  @Test
  public void odConnectoidAtRemovedNodeIsRemoved() {
    var odZone = zoning.getOdZones().getFactory().registerNew();
    zoning.getOdConnectoids().getFactory().registerNewWithUndirectedEntry(odZone, nodes.get(2));

    removeNode(2);

    assertTrue(zoning.getOdConnectoids().isEmpty());
  }

  /** A transfer connectoid at a removed node is removed whole, with one connectoid removed event naming it */
  @Test
  public void transferConnectoidAtRemovedNodeIsRemovedWithOneEvent() {
    var recorder = new ZoningLossEventRecorder(zoning);

    removeNode(1);

    assertTrue(zoning.getTransferConnectoids().isEmpty());
    assertEquals(List.of(RemoveConnectoidEvent.EVENT_TYPE), recorder.getHeardTypes());
    assertSame(connectoidAtNode1, ((RemoveConnectoidEvent) recorder.getHeard().get(0)).getRemovedConnectoid());
  }

  /** A zoning made on a network before it has layers, as a reader does before reading the network, follows the layers
   * added since once attached again */
  @Test
  public void zoningMadeBeforeItsLayersFollowsThemOnceAttachedAgain() {
    var token = IdGroupingToken.collectGlobalToken();
    var laterNetwork = new MacroscopicNetwork(token);
    var earlyZoning = new Zoning(token, laterNetwork);
    var laterLayer = laterNetwork.getTransportLayers().getFactory().registerNew();
    var laterNode = laterLayer.getNodes().getFactory().registerNew();
    earlyZoning.getTransferConnectoids().getFactory().registerNew(laterNode);

    earlyZoning.attachTo(laterNetwork);
    laterLayer.getLayerModifier().removeVertex(laterNode);

    assertTrue(earlyZoning.getTransferConnectoids().isEmpty());
  }

  /** A node removed in another layer of the network leaves the connectoid alone */
  @Test
  public void nodeRemovedInAnotherLayerLeavesConnectoid() {
    var otherLayer = network.getTransportLayers().getFactory().registerNew();
    var otherNode = otherLayer.getNodes().getFactory().registerNew();
    zoning.detachFrom(network);
    zoning.attachTo(network);

    otherLayer.getLayerModifier().removeVertex(otherNode);

    assertEquals(1, zoning.getTransferConnectoids().size());
  }

  /** A connectoid moved to another node is followed there, and no longer at its old node */
  @Test
  public void movedConnectoidIsFollowedAtItsNewNode() {
    connectoidAtNode1.setReferenceVertex(nodes.get(2));

    removeNode(1);
    assertEquals(1, zoning.getTransferConnectoids().size());

    removeNode(2);
    assertTrue(zoning.getTransferConnectoids().isEmpty());
  }

  /** Removing a connectoid with its node leaves the ids of the others as they are */
  @Test
  public void removingConnectoidsDoesNotRenumberTheOthers() {
    var atNode0 = zoning.getTransferConnectoids().getFactory().registerNew(nodes.get(0));
    var atNode2 = zoning.getTransferConnectoids().getFactory().registerNew(nodes.get(2));
    long idAtNode0 = atNode0.getId();
    long idAtNode2 = atNode2.getId();

    removeNode(1);

    assertEquals(idAtNode0, atNode0.getId());
    assertEquals(idAtNode2, atNode2.getId());
  }
}
