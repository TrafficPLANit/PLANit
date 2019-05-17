package org.planit.interactor;

/**
 * Link Volume Accessor interface
 * 
 * @author markr
 *
 */
public interface LinkVolumeAccessor extends InteractorAccessor {

    /**
     * Get the LinkVolumeAccessee class - default implementation
     * 
     * @return class which implements InteractorAccessee interface
     */
    @Override
    default Class<? extends InteractorAccessee> getRequestedAccessee() {
        return LinkVolumeAccessee.class;
    }

}
