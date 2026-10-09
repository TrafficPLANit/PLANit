package org.goplanit.network.layer.macroscopic.intersection.modifier.event.handler;

import java.util.logging.Logger;

import org.goplanit.event.handler.SyncXmlIdToIdHandler;
import org.goplanit.network.layer.macroscopic.intersection.modifier.event.RecreatedIntersectionsManagedIdsEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.intersection.modifier.event.IntersectionModifierListener;

/**
 * Sync the intersections' XML ids to their internal ids. Listens to #RecreatedIntersectionsManagedIdsEvent
 *
 * @author markr
 */
public class SyncXmlIdToIdIntersectionsHandler extends SyncXmlIdToIdHandler implements IntersectionModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SyncXmlIdToIdIntersectionsHandler.class.getCanonicalName());

  /**
   * Default constructor
   */
  public SyncXmlIdToIdIntersectionsHandler() {
    super(RecreatedIntersectionsManagedIdsEvent.EVENT_TYPE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onIntersectionModifierEvent(IntersectionModificationEvent event) {
    if (!event.getType().equals(RecreatedIntersectionsManagedIdsEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports RecreatedIntersectionsManagedIdsEvent events",
          SyncXmlIdToIdIntersectionsHandler.class.getName()));
      return;
    }
    ((RecreatedIntersectionsManagedIdsEvent) event).getIntersections().forEach(super::syncXmlIdToInternalId);
  }
}
