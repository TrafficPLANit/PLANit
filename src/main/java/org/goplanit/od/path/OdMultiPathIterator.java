package org.goplanit.od.path;

import org.goplanit.utils.od.OdDataIterator;
import org.goplanit.utils.path.ManagedDirectedPath;

import java.util.Collection;
import java.util.List;

/**
 * Iterator for directed multi-paths tracked by od
 * 
 * @author markr
 *
 */
public interface OdMultiPathIterator<T extends ManagedDirectedPath, U extends List<T>> extends OdDataIterator<U> {

}
