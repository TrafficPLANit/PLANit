package org.planit.interactor;

public interface LinkVolumeAccessor extends InteractorAccessor {

	@Override
	default Class<? extends InteractorAccessee> getRequestedAccessee() {
		return LinkVolumeAccessee.class;
	}
	
}
