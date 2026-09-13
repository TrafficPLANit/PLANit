package org.goplanit.test.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.goplanit.network.MacroscopicNetworkUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.junit.jupiter.api.Test;

/** Tests synchronisation of banned movements with standalone link-segment removal. */
public class BannedMovementModifierTest {

  @Test
  public void removeEdgeSegmentRemovesOnlyReferencingBannedMovements() {
    var network = MacroscopicNetworkUtils.createSimpleGrid(
        IdGenerator.createIdGroupingToken("banned-movement-modifier-test"), 3, 3);
    var layer = network.getTransportLayers().getFirst();

    var removedSegment = layer.getLinkSegments().get(0);
    var otherSegmentA = layer.getLinkSegments().get(1);
    var otherSegmentB = layer.getLinkSegments().get(2);
    var movements = layer.getBannedMovements();

    var bannedOnIncomingSegment = movements.getFactory().registerNew(removedSegment, otherSegmentA);
    var bannedOnOutgoingSegment = movements.getFactory().registerNew(otherSegmentA, removedSegment);
    var unrelatedBannedMovement = movements.getFactory().registerNew(otherSegmentA, otherSegmentB);

    layer.getLayerModifier().removeEdgeSegment((MacroscopicLinkSegment) removedSegment);

    assertEquals(1, movements.size());
    assertFalse(movements.containsValue(bannedOnIncomingSegment));
    assertFalse(movements.containsValue(bannedOnOutgoingSegment));
    assertTrue(movements.containsValue(unrelatedBannedMovement));
  }
}
