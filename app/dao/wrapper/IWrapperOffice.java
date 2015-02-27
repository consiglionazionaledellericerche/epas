package dao.wrapper;

import models.Office;

public interface IWrapperOffice extends IWrapperModel<Office> {

	boolean isSeat();

	boolean isArea();

	boolean isInstitute();

}
