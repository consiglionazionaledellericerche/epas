package dao.wrapper;

import models.base.BaseModel;


public interface IWrapperModel<T extends BaseModel> {

  T getValue();

}
