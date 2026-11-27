package org.goplanit.network.layer.macroscopic.tollroad.modifier.event.handler;

import java.util.logging.Logger;

import org.goplanit.event.handler.SyncXmlIdToIdHandler;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.RecreatedTollRoadsManagedIdsEvent;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.TollRoadModificationEvent;
import org.goplanit.utils.network.layer.macroscopic.tollroad.modifier.event.TollRoadModifierListener;

/**
 * Sync the toll roads' XML ids to their internal ids. Listens to #RecreatedTollRoadsManagedIdsEvent
 * <p>
 * When a network is renumbered before it is written, for example after dangling parts are removed, every entity's XML
 * id is set to its new id, so the file's ids are unique and run from zero. Without this, a toll road would keep the XML
 * id it was read with among link segments that were all renumbered. Its name and external id, such as the id of the
 * road in the data it was built from, still say which road it is.
 * </p>
 *
 * @author markr
 */
public class SyncXmlIdToIdTollRoadsHandler extends SyncXmlIdToIdHandler implements TollRoadModifierListener {

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(SyncXmlIdToIdTollRoadsHandler.class.getCanonicalName());

  /**
   * Default constructor
   */
  public SyncXmlIdToIdTollRoadsHandler() {
    super(RecreatedTollRoadsManagedIdsEvent.EVENT_TYPE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onTollRoadModifierEvent(TollRoadModificationEvent event) {
    if (!event.getType().equals(RecreatedTollRoadsManagedIdsEvent.EVENT_TYPE)) {
      LOGGER.warning(String.format("%s only supports RecreatedTollRoadsManagedIdsEvent events",
          SyncXmlIdToIdTollRoadsHandler.class.getName()));
      return;
    }
    ((RecreatedTollRoadsManagedIdsEvent) event).getTollRoads().forEach(super::syncXmlIdToInternalId);
  }
}
