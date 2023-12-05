package org.goplanit.od.path;

import org.goplanit.utils.od.OdData;
import org.goplanit.utils.path.ManagedDirectedPath;

import java.util.Collection;

/**
 * A container class for Origin-Destination paths, where each OD may have more than one path specified. OdPaths should be Idable. We leave it to the concrete implementations to determine what type of container is used, this is only a
 * placeholder to signify that it concerns a container for paths that is uniquely identifiable
 * 
 * @author markr
 *
 */
public interface OdMultiPaths<T extends Collection<? extends ManagedDirectedPath>> extends OdData<T> {

}
