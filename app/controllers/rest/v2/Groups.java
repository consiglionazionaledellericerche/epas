package controllers.rest.v2;

import cnr.sync.dto.v2.GroupShowDto;
import cnr.sync.dto.v2.GroupShowTerseDto;
import com.google.gson.GsonBuilder;
import controllers.Resecure;
import dao.GroupDao;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With(Resecure.class)
public class Groups extends Controller {

  @Inject
  static GroupDao groupDao;
  @Inject 
  static SecurityRules rules;
  @Inject
  static GsonBuilder gsonBuilder;
  
  public static void list(Long id, String code, String codeId) {
    val office = Offices.getOfficeFromRequest(id, code, codeId);
    rules.checkIfPermitted(office);
    
    val list = 
        office.groups.stream().map(group -> GroupShowTerseDto.build(group))
          .collect(Collectors.toSet());
    renderJSON(gsonBuilder.create().toJson(list));
  }
  
  public static void show(Long id) {
    val group = groupDao.byId(id).orNull();
    notFoundIfNull(group);
    rules.checkIfPermitted(group.office);
    renderJSON(gsonBuilder.create().toJson(GroupShowDto.build(group)));
  }
  
  public static void create() {
    todo();
  }
  
  public static void update() {
    todo();
  }
  
  public static void delete() {
    todo();
  }
}
