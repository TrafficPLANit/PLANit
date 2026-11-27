package org.goplanit.test.zoning;

import org.goplanit.network.MacroscopicNetwork;
import org.goplanit.test.LayerTestBase;
import org.goplanit.utils.graph.directed.DirectedVertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.zoning.connectoid.OdConnectoid;
import org.goplanit.utils.zoning.connectoid.TransferConnectoid;
import org.goplanit.zoning.Zoning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Finding the connectoids of a zoning at their access node: found at their node, followed to a new node when moved
 * while registered, found nowhere when moved after their removal, found in copies, and not found by another node with
 * the same id
 *
 * <pre>
 *   0        1        2      three nodes on a line; a transfer connectoid and an OD connectoid at node 1
 * </pre>
 */
public class ConnectoidsByAccessNodeTest extends LayerTestBase {

  /** zoning holding the connectoids */
  private Zoning zoning;

  /** the transfer connectoid at node 1 */
  private TransferConnectoid transferConnectoid;

  /** the OD connectoid at node 1 */
  private OdConnectoid odConnectoid;

  /** One layer with three nodes, and a zoning with a transfer and an OD connectoid at node 1 */
  @BeforeEach
  public void setUp() {
    var token = IdGroupingToken.collectGlobalToken();
    var network = new MacroscopicNetwork(token);
    var car = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);
    layer = network.getTransportLayers().getFactory().registerNew();
    layer.registerSupportedModes(List.of(car));
    createNodesOnLine(3);
    zoning = new Zoning(token, layer.getLayerIdGroupingToken());
    transferConnectoid = zoning.getTransferConnectoids().getFactory().registerNew(nodes.get(1));
    odConnectoid = zoning.getOdConnectoids().getFactory().registerNew(nodes.get(1));
  }

  /** Each connectoid is found at its node, and nothing at a node without connectoids */
  @Test
  public void connectoidFoundAtItsNode() {
    assertEquals(List.of(transferConnectoid), zoning.getTransferConnectoids().getByAccessNode(nodes.get(1)));
    assertEquals(List.of(odConnectoid), zoning.getOdConnectoids().getByAccessNode(nodes.get(1)));
    assertTrue(zoning.getTransferConnectoids().getByAccessNode(nodes.get(0)).isEmpty());
  }

  /** A registered connectoid moved to another node is found there only */
  @Test
  public void movedRegisteredConnectoidFoundAtNewNodeOnly() {
    transferConnectoid.setReferenceVertex(nodes.get(2));
    assertEquals(List.of(transferConnectoid), zoning.getTransferConnectoids().getByAccessNode(nodes.get(2)));
    assertTrue(zoning.getTransferConnectoids().getByAccessNode(nodes.get(1)).isEmpty());
  }

  /** A connectoid moved after its removal is found nowhere in the container it left */
  @Test
  public void connectoidMovedAfterRemovalFoundNowhere() {
    zoning.getTransferConnectoids().remove(transferConnectoid);
    transferConnectoid.setReferenceVertex(nodes.get(2));
    assertTrue(zoning.getTransferConnectoids().getByAccessNode(nodes.get(1)).isEmpty());
    assertTrue(zoning.getTransferConnectoids().getByAccessNode(nodes.get(2)).isEmpty());
  }

  /** A shallow copy shares the connectoid, and both containers follow it when it moves */
  @Test
  public void shallowCopyFollowsSharedConnectoid() {
    var copy = zoning.getTransferConnectoids().shallowClone();
    transferConnectoid.setReferenceVertex(nodes.get(2));
    assertEquals(List.of(transferConnectoid), copy.getByAccessNode(nodes.get(2)));
    assertEquals(List.of(transferConnectoid), zoning.getTransferConnectoids().getByAccessNode(nodes.get(2)));
  }

  /** A deep copy finds its own copy of the connectoid and follows it when it moves, the original staying put */
  @Test
  public void deepCopyFindsAndFollowsItsOwnConnectoid() {
    var copy = zoning.getTransferConnectoids().deepClone();
    var copied = copy.get(transferConnectoid.getId());
    assertNotSame(transferConnectoid, copied);
    assertEquals(List.of(copied), copy.getByAccessNode(nodes.get(1)));

    copied.setReferenceVertex(nodes.get(2));
    assertEquals(List.of(copied), copy.getByAccessNode(nodes.get(2)));
    assertEquals(List.of(transferConnectoid), zoning.getTransferConnectoids().getByAccessNode(nodes.get(1)));
  }

  /** Another node object with the same id as the connectoid's node finds nothing */
  @Test
  public void otherNodeWithSameIdFindsNothing() {
    DirectedVertex sameId = nodes.get(1).shallowClone();
    assertEquals(nodes.get(1).getId(), sameId.getId());
    assertTrue(zoning.getTransferConnectoids().getByAccessNode(sameId).isEmpty());
  }
}
