package dao.wrapper;

import models.base.BaseModel;

/**
 * @author marco
 */
public interface IWrapperModel<T extends BaseModel> {

  T getValue();

}
