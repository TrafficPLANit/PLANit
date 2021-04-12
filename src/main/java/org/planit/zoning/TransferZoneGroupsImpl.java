package org.planit.zoning;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.planit.utils.zoning.TransferZoneGroup;
import org.planit.utils.zoning.TransferZoneGroups;

/**
 * Container for transfer zone groups where each transfer zone group logically groups multiple transfer zones together. Practically this can be used to represent public transport
 * stations for example
 * 
 * @author markr
 *
 */
public class TransferZoneGroupsImpl implements TransferZoneGroups {

  /**
   * Map storing all the transfer zone groups
   */
  protected Map<Long, TransferZoneGroup> transferZoneGroupsMap = new TreeMap<Long, TransferZoneGroup>();

  /** zoning builder to use */
  protected final ZoningBuilder zoningBuilder;

  /**
   * recreate the mapping such that all the keys used for each transfer zone group reflect their internal id. To be called whenever the ids of transfer zone groups are changed
   */
  protected void updateIdMapping() {
    Map<Long, TransferZoneGroup> updatedMap = new HashMap<Long, TransferZoneGroup>(transferZoneGroupsMap.size());
    transferZoneGroupsMap.forEach((oldId, group) -> updatedMap.put(group.getId(), group));
    transferZoneGroupsMap.clear();
    transferZoneGroupsMap = updatedMap;
  }

  /**
   * Constructor
   * 
   * @param zoningBuilder to use
   */
  public TransferZoneGroupsImpl(ZoningBuilder zoningBuilder) {
    this.zoningBuilder = zoningBuilder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<TransferZoneGroup> iterator() {
    return transferZoneGroupsMap.values().iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneGroup register(TransferZoneGroup transferZoneGroup) {
    return transferZoneGroupsMap.put(transferZoneGroup.getId(), transferZoneGroup);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneGroup registerNew() {
    TransferZoneGroup transferZoneGroup = createNew();
    register(transferZoneGroup);
    return transferZoneGroup;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneGroup createNew() {
    return zoningBuilder.createTransferZoneGroup();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneGroup remove(TransferZoneGroup transferZone) {
    return transferZoneGroupsMap.remove(transferZone.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasTransferZoneGroup(TransferZoneGroup transferZoneGroup) {
    return transferZoneGroupsMap.containsKey(transferZoneGroup.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransferZoneGroup get(long transferZoneGroupId) {
    return transferZoneGroupsMap.get(transferZoneGroupId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return transferZoneGroupsMap.size();
  }

}
