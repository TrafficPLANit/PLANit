package org.planit.od.path;

import org.planit.od.OdData;
import org.planit.utils.path.DirectedPath;

/**
 * A container class for Origin-Destination paths. OdPaths should be Idable. We leave it to the concrete implementations to determine what type of container is used, this is only a
 * placeholder to signify that it concerns a container for paths that is uniquely identifiable
 * 
 * @author markr
 *
 */
public interface OdPaths extends OdData<DirectedPath> {

}
