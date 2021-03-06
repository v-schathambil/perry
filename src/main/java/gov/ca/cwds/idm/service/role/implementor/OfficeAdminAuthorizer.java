package gov.ca.cwds.idm.service.role.implementor;

import static gov.ca.cwds.service.messages.MessageCode.NOT_AUTHORIZED_TO_ADD_USER_FOR_OTHER_OFFICE;
import static gov.ca.cwds.service.messages.MessageCode.NOT_SUPER_ADMIN_CANNOT_UPDATE_USERS_WITH_SUPER_ADMIN_ROLE;
import static gov.ca.cwds.service.messages.MessageCode.NOT_SUPER_ADMIN_CANNOT_VIEW_USERS_WITH_SUPER_ADMIN_ROLE;
import static gov.ca.cwds.service.messages.MessageCode.OFFICE_ADMIN_CANNOT_RESEND_INVITATION_FOR_USER_FROM_OTHER_OFFICE;
import static gov.ca.cwds.service.messages.MessageCode.OFFICE_ADMIN_CANNOT_UPDATE_COUNTY_ADMIN;
import static gov.ca.cwds.service.messages.MessageCode.OFFICE_ADMIN_CANNOT_UPDATE_STATE_ADMIN;
import static gov.ca.cwds.service.messages.MessageCode.OFFICE_ADMIN_CANNOT_UPDATE_USER_FROM_OTHER_OFFICE;
import static gov.ca.cwds.service.messages.MessageCode.OFFICE_ADMIN_CANNOT_VIEW_USERS_WITH_CALS_EXTERNAL_WORKER_ROLE;
import static gov.ca.cwds.service.messages.MessageCode.OFFICE_ADMIN_CANNOT_VIEW_USER_FROM_OTHER_COUNTY;

import gov.ca.cwds.idm.dto.User;

class OfficeAdminAuthorizer extends AbstractAdminActionsAuthorizer {

  OfficeAdminAuthorizer(User user) {
    super(user);
  }

  @Override
  public void checkCanViewUser() {
    checkAdminAndUserInTheSameCounty(
        OFFICE_ADMIN_CANNOT_VIEW_USER_FROM_OTHER_COUNTY, getUser().getId());
    checkUserIsNotCalsExternalWorker(
        OFFICE_ADMIN_CANNOT_VIEW_USERS_WITH_CALS_EXTERNAL_WORKER_ROLE, getUser().getId());
    checkUserIsNotSuperAdmin(NOT_SUPER_ADMIN_CANNOT_VIEW_USERS_WITH_SUPER_ADMIN_ROLE);
  }

  @Override
  public void checkCanCreateUser() {
    checkAdminAndUserInTheSameOffice(NOT_AUTHORIZED_TO_ADD_USER_FOR_OTHER_OFFICE);
  }

  @Override
  public void checkCanUpdateUser() {
    checkAdminAndUserInTheSameOffice(OFFICE_ADMIN_CANNOT_UPDATE_USER_FROM_OTHER_OFFICE);
    checkUserIsNotCountyAdmin(OFFICE_ADMIN_CANNOT_UPDATE_COUNTY_ADMIN);
    checkUserIsNotStateAdmin(OFFICE_ADMIN_CANNOT_UPDATE_STATE_ADMIN);
    checkUserIsNotSuperAdmin(NOT_SUPER_ADMIN_CANNOT_UPDATE_USERS_WITH_SUPER_ADMIN_ROLE);
  }

  @Override
  public void checkCanResendInvitationMessage() {
    checkAdminAndUserInTheSameOffice(OFFICE_ADMIN_CANNOT_RESEND_INVITATION_FOR_USER_FROM_OTHER_OFFICE);
  }
}
