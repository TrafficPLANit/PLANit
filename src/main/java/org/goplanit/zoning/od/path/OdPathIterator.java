package org.goplanit.zoning.od.path;

import org.goplanit.utils.zoning.zonetozone.ZoneToZoneDataIterator;
import org.goplanit.utils.path.ManagedDirectedPath;

/**
 * Iterator for managed directed paths tracked by od
 * 
 * @author markr
 *
 */
public interface OdPathIterator<T extends ManagedDirectedPath> extends ZoneToZoneDataIterator<T> {

}
