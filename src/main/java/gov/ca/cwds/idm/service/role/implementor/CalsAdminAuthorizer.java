package gov.ca.cwds.idm.service.role.implementor;

import static gov.ca.cwds.service.messages.MessageCode.CALS_ADMIN_CANNOT_VIEW_NON_CALS_USER;

import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.service.authorization.UserRolesService;

@SuppressWarnings({"common-java:DuplicatedBlocks"})
//it's not a duplication but super class method invocation where common code is extracted
class CalsAdminAuthorizer extends AbstractAdminActionsAuthorizer {

  CalsAdminAuthorizer(User user) {
    super(user);
  }

  @Override
  public void checkCanViewUser() {
    if (!UserRolesService.isCalsExternalWorker(getUser())) {
      throwAuthorizationException(CALS_ADMIN_CANNOT_VIEW_NON_CALS_USER, getUser().getId());
    }
  }

  @Override
  public void checkCanCreateUser() {
    unsufficientRoleError();
  }

  @Override
  public void checkCanUpdateUser() {
    unsufficientRoleError();
  }

  @Override
  public void checkCanResendInvitationMessage() {
    unsufficientRoleError();
  }

  @Override
  public void checkCanEditRoles() {
    unsufficientRoleError();
  }
}
