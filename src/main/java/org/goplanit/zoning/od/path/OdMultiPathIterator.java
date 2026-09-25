package org.goplanit.zoning.od.path;

import org.goplanit.utils.zoning.zonetozone.ZoneToZoneDataIterator;
import org.goplanit.utils.path.ManagedDirectedPath;

import java.util.List;

/**
 * Iterator for directed multi-paths tracked by od
 * 
 * @author markr
 * @param <T> type of path
 * @param <U> type of list of paths
 */
public interface OdMultiPathIterator<T extends ManagedDirectedPath, U extends List<T>>
    extends ZoneToZoneDataIterator<U> {

}
