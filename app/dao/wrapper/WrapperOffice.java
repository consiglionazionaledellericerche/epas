package dao.wrapper;

import java.util.List;

import manager.OfficeManager;
import models.Office;
import models.Person;
import models.Role;
import models.UsersRolesOffices;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.OfficeDao;
import dao.PersonDao;
import dao.RoleDao;
import dao.wrapper.function.WrapperModelFunctionFactory;

/**
 * @author alessandro
 *
 */
public class WrapperOffice implements IWrapperOffice {

	private final Office value;
	private final RoleDao roleDao;

	@Inject
	WrapperOffice(@Assisted Office office, RoleDao roleDao) {
		value = office;
		this.roleDao = roleDao;
	}

	@Override
	public Office getValue() {
		return value;
	}


	/**
	 * Gli amministratori dell'office.
	 * @return
	 */
	public List<Person> getPersonnelAdmin() {

		Role roleAdmin = roleDao.getRoleByName(Role.PERSONNEL_ADMIN);
		List<Person> personList = Lists.newArrayList();
		for(UsersRolesOffices uro : this.value.usersRolesOffices) {

			if(uro.office.id.equals(this.value.id) && uro.role.id.equals(roleAdmin.id) 
					&& uro.user.person != null)
				personList.add(uro.user.person);
		}
		return personList;
	}

	/**
	 * I mini amministratori dell'office.
	 * @return
	 */
	public List<Person> getPersonnelAdminMini() {

		Role roleAdminMini = roleDao.getRoleByName(Role.PERSONNEL_ADMIN_MINI);
		List<Person> personList = Lists.newArrayList();
		for(UsersRolesOffices uro : this.value.usersRolesOffices) {

			if(uro.office.id.equals(this.value.id) && uro.role.id.equals(roleAdminMini.id) 
					&& uro.user.person != null)
				personList.add(uro.user.person);
		}
		return personList;
	}

}
