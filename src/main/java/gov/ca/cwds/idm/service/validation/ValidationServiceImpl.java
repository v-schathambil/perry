package gov.ca.cwds.idm.service.validation;

import static gov.ca.cwds.config.api.idm.Roles.COUNTY_ADMIN;
import static gov.ca.cwds.config.api.idm.Roles.OFFICE_ADMIN;
import static gov.ca.cwds.idm.service.authorization.UserRolesService.getStrongestAdminRole;
import static gov.ca.cwds.idm.service.cognito.util.CognitoUsersSearchCriteriaUtil.composeToGetFirstPageByEmail;
import static gov.ca.cwds.idm.service.cognito.util.CognitoUsersSearchCriteriaUtil.composeToGetFirstPageByRacfId;
import static gov.ca.cwds.service.messages.MessageCode.ACTIVE_USER_WITH_RAFCID_EXISTS_IN_IDM;
import static gov.ca.cwds.service.messages.MessageCode.NOT_AUTHORIZED_TO_ADD_USER_FOR_OTHER_COUNTY;
import static gov.ca.cwds.service.messages.MessageCode.NOT_AUTHORIZED_TO_ADD_USER_FOR_OTHER_OFFICE;
import static gov.ca.cwds.service.messages.MessageCode.NO_USER_WITH_RACFID_IN_CWSCMS;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_TO_REMOVE_ALL_ROLES;
import static gov.ca.cwds.service.messages.MessageCode.UNABLE_UPDATE_UNALLOWED_ROLES;
import static gov.ca.cwds.service.messages.MessageCode.USER_WITH_EMAIL_EXISTS_IN_IDM;
import static gov.ca.cwds.util.CurrentAuthenticatedUserUtil.getCurrentUser;
import static gov.ca.cwds.util.Utils.toUpperCase;

import com.amazonaws.services.cognitoidp.model.UserType;
import gov.ca.cwds.UniversalUserToken;
import gov.ca.cwds.data.persistence.auth.CwsOffice;
import gov.ca.cwds.data.persistence.auth.StaffPerson;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UserUpdate;
import gov.ca.cwds.idm.service.MappingService;
import gov.ca.cwds.idm.service.authorization.AuthorizationService;
import gov.ca.cwds.idm.service.cognito.CognitoServiceFacade;
import gov.ca.cwds.idm.service.cognito.util.CognitoUtils;
import gov.ca.cwds.idm.service.role.implementor.AdminRoleImplementorFactory;
import gov.ca.cwds.rest.api.domain.UserIdmValidationException;
import gov.ca.cwds.rest.api.domain.auth.GovernmentEntityType;
import gov.ca.cwds.service.CwsUserInfoService;
import gov.ca.cwds.service.dto.CwsUserInfo;
import gov.ca.cwds.service.messages.MessageCode;
import gov.ca.cwds.service.messages.MessagesService;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Profile("idm")
public class ValidationServiceImpl implements ValidationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationServiceImpl.class);

  private MappingService mappingService;

  private CwsUserInfoService cwsUserInfoService;

  private MessagesService messagesService;

  private CognitoServiceFacade cognitoServiceFacade;

  private AuthorizationService authorizeService;

  private AdminRoleImplementorFactory adminRoleImplementorFactory;

  @Override
  public User validateUserCreate(UniversalUserToken admin, User user) {
    String racfId = user.getRacfid();
    if (StringUtils.isNotBlank(racfId)) {
      return validateRacfidUserCreate(user);
    } else {
        return user;
    }
  }

  @Override
  public void validateUpdateUser(UniversalUserToken admin, UserType existedCognitoUser, UserUpdate updateUserDto) {
    validateByNewUserRoles(admin, updateUserDto);
    validateActivateUser(existedCognitoUser, updateUserDto);
  }

  private User validateRacfidUserCreate(User user) {
    String racfId = user.getRacfid();

    CwsUserInfo cwsUser = validateActiveUserExistsInCws(racfId);
    validateEmailDoesNotExistInCognito(user.getEmail());
    validateRacfidDoesNotExistInCognito(racfId);

    enrichUserByCwsData(user, cwsUser);

    validateByCreateAuthorizationRules(user);
    return user;
  }

  private void validateByCreateAuthorizationRules(User user) {
    Optional<MessageCode> authorizationError = buildAuthorizationError();
    if (!authorizeService.canCreateUser(user) && authorizationError.isPresent()) {
      throwValidationException(authorizationError.get(), user.getCountyName());
    }
  }

  private void validateRacfidDoesNotExistInCognito(String racfId) {
    if (isActiveRacfIdPresent(racfId)) {
      throwValidationException(ACTIVE_USER_WITH_RAFCID_EXISTS_IN_IDM, racfId);
    }
  }

  private void validateEmailDoesNotExistInCognito(String email) {
    if (userWithEmailExistsInCognito(email)) {
      throwValidationException(USER_WITH_EMAIL_EXISTS_IN_IDM, email);
    }
  }

  private CwsUserInfo validateActiveUserExistsInCws(String racfId) {
    CwsUserInfo cwsUser = cwsUserInfoService.getCwsUserByRacfId(racfId);
    if (cwsUser == null) {
      throwValidationException(NO_USER_WITH_RACFID_IN_CWSCMS, racfId);
    }
    return cwsUser;
  }

  private void validateByNewUserRoles(UniversalUserToken admin, UserUpdate updateUserDto) {
    Collection<String> newUserRoles = updateUserDto.getRoles();

    if (newUserRoles == null) {
      return;
    }

    if (newUserRoles.isEmpty()) {
      throwValidationException(UNABLE_TO_REMOVE_ALL_ROLES);
    }

    Collection<String> allowedRoles = adminRoleImplementorFactory.getPossibleUserRoles(admin);
    if (!allowedRoles.containsAll(newUserRoles)) {
      throwValidationException(UNABLE_UPDATE_UNALLOWED_ROLES, newUserRoles, allowedRoles);
    }
  }

  private void enrichUserByCwsData(User user, CwsUserInfo cwsUser) {
    user.setRacfid(cwsUser.getRacfId());
    enrichDataFromCwsOffice(cwsUser.getCwsOffice(), user);
    enrichDataFromStaffPerson(cwsUser.getStaffPerson(), user);
  }

  private void enrichDataFromStaffPerson(StaffPerson staffPerson, final User user) {
    if (staffPerson != null) {
      user.setFirstName(staffPerson.getFirstName());
      user.setLastName(staffPerson.getLastName());
      user.setEndDate(staffPerson.getEndDate());
      user.setStartDate(staffPerson.getStartDate());
    }
  }

  private void enrichDataFromCwsOffice(CwsOffice office, final User user) {
    if (office != null) {
      user.setOfficeId(office.getOfficeId());
      Optional.ofNullable(office.getPrimaryPhoneNumber())
          .ifPresent(e -> user.setPhoneNumber(e.toString()));
      Optional.ofNullable(office.getPrimaryPhoneExtensionNumber())
          .ifPresent(user::setPhoneExtensionNumber);
      Optional.ofNullable(office.getGovernmentEntityType())
          .ifPresent(
              x -> user.setCountyName((GovernmentEntityType.findBySysId(x)).getDescription()));
    }
  }

  private Optional<MessageCode> buildAuthorizationError() {
    switch (getStrongestAdminRole((getCurrentUser()))) {
      case COUNTY_ADMIN:
        return Optional.of(NOT_AUTHORIZED_TO_ADD_USER_FOR_OTHER_COUNTY);
      case OFFICE_ADMIN:
        return Optional.of(NOT_AUTHORIZED_TO_ADD_USER_FOR_OTHER_OFFICE);
      default:
        return Optional.empty();
    }
  }

  private boolean isActiveRacfIdPresent(String racfId) {
    Collection<UserType> cognitoUsersByRacfId =
        cognitoServiceFacade.searchAllPages(composeToGetFirstPageByRacfId(toUpperCase(racfId)));
    return !CollectionUtils.isEmpty(cognitoUsersByRacfId)
        && isActiveUserPresent(cognitoUsersByRacfId);
  }

  private boolean userWithEmailExistsInCognito(String email) {
    Collection<UserType> cognitoUsers =
        cognitoServiceFacade.searchPage(composeToGetFirstPageByEmail(email)).getUsers();
    return !CollectionUtils.isEmpty(cognitoUsers);
  }

  private void throwValidationException(MessageCode messageCode, Object... args) {
    String msg = messagesService.getTechMessage(messageCode, args);
    String userMsg = messagesService.getUserMessage(messageCode, args);
    LOGGER.error(msg);
    throw new UserIdmValidationException(msg, userMsg, messageCode);
  }

  private void validateActivateUser(UserType existedCognitoUser, UserUpdate updateUserDto) {
    if (!canChangeToEnableActiveStatus(updateUserDto.getEnabled(),
        existedCognitoUser.getEnabled())) {
      return;
    }
    String racfId = CognitoUtils.getRACFId(existedCognitoUser);
    if (StringUtils.isNotBlank(racfId)) {
      validateActivateUser(racfId);
    }
  }

  private static boolean canChangeToEnableActiveStatus(Boolean newEnabled, Boolean currentEnabled) {
    return newEnabled != null && !newEnabled.equals(currentEnabled) && newEnabled;
  }

  void validateActivateUser(String racfId) {
    validateActiveUserExistsInCws(racfId);
    validateRacfidDoesNotExistInCognito(racfId);
  }

  private static boolean isActiveUserPresent(Collection<UserType> cognitoUsers) {
    return cognitoUsers
        .stream()
        .anyMatch(userType -> Objects.equals(userType.getEnabled(), Boolean.TRUE));
  }

  @Autowired
  public void setMappingService(MappingService mappingService) {
    this.mappingService = mappingService;
  }

  @Autowired
  public void setCwsUserInfoService(CwsUserInfoService cwsUserInfoService) {
    this.cwsUserInfoService = cwsUserInfoService;
  }

  @Autowired
  public void setMessagesService(MessagesService messagesService) {
    this.messagesService = messagesService;
  }

  @Autowired
  public void setCognitoServiceFacade(
      CognitoServiceFacade cognitoServiceFacade) {
    this.cognitoServiceFacade = cognitoServiceFacade;
  }

  @Autowired
  public void setAuthorizeService(
      AuthorizationService authorizeService) {
    this.authorizeService = authorizeService;
  }

  @Autowired
  public void setAdminRoleImplementorFactory(
      AdminRoleImplementorFactory adminRoleImplementorFactory) {
    this.adminRoleImplementorFactory = adminRoleImplementorFactory;
  }
}
