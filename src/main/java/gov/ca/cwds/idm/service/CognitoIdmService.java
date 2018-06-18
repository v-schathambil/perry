package gov.ca.cwds.idm.service;

import com.amazonaws.services.cognitoidp.model.UserType;
import gov.ca.cwds.PerryProperties;
import gov.ca.cwds.data.persistence.auth.CwsOffice;
import gov.ca.cwds.data.persistence.auth.StaffPerson;
import gov.ca.cwds.idm.dto.UpdateUserDto;
import gov.ca.cwds.idm.dto.User;
import gov.ca.cwds.idm.dto.UsersSearchParameter;
import gov.ca.cwds.idm.util.UsersSearchParametersUtil;
import gov.ca.cwds.rest.api.domain.PerryException;
import gov.ca.cwds.rest.api.domain.UserValidationException;
import gov.ca.cwds.rest.api.domain.auth.GovernmentEntityType;
import gov.ca.cwds.service.CwsUserInfoService;
import gov.ca.cwds.service.dto.CwsUserInfo;
import gov.ca.cwds.service.scripts.IdmMappingScript;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.script.ScriptException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Profile("idm")
public class CognitoIdmService implements IdmService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CognitoIdmService.class);
  static final String RACFID_ATTRIBUTE = "CUSTOM:RACFID";

  @Autowired CognitoServiceFacade cognitoService;

  @Autowired CwsUserInfoService cwsUserInfoService;

  @Autowired private PerryProperties configuration;

  @Override
  public List<User> getUsers(String lastName) {
    Collection<UserType> cognitoUsers =
        cognitoService.search(UsersSearchParametersUtil.composeSearchParameter(lastName));

    Map<String, String> userNameToRacfId = new HashMap<>(cognitoUsers.size());
    for (UserType user : cognitoUsers) {
      userNameToRacfId.put(user.getUsername(), getRACFId(user));
    }

    Map<String, CwsUserInfo> idToCmsUser =
        cwsUserInfoService
            .findUsers(userNameToRacfId.values())
            .stream()
            .collect(
                Collectors.toMap(
                    CwsUserInfo::getRacfId,
                    e -> e,
                    (user1, user2) -> {
                      LOGGER.warn(
                          "UserAuthorization - duplicate UserId for RACFid: {}", user1.getRacfId());
                      return user1;
                    }));

    IdmMappingScript mapping = configuration.getIdentityManager().getIdmMapping();
    return cognitoUsers
        .stream()
        .map(
            e -> {
              try {
                return mapping.map(e, idToCmsUser.get(userNameToRacfId.get(e.getUsername())));
              } catch (ScriptException ex) {
                LOGGER.error("Error running the IdmMappingScript");
                throw new PerryException(ex.getMessage(), ex);
              }
            })
        .collect(Collectors.toList());
  }

  @Override
  public User findUser(String id) {
    UserType cognitoUser = cognitoService.getById(id);
    return enrichCognitoUser(cognitoUser);
  }

  @Override
  public void updateUser(String id, UpdateUserDto updateUserDto) {
    cognitoService.updateUser(id, updateUserDto);
  }

  @Override
  public User checkUser(String racfId, String email) {
    CwsUserInfo cwsUser = getCwsUserByRacfId(racfId);
    if (cwsUser == null) {
      String message = MessageFormat.format("No user with RACFID {0} found in CWSCMS", racfId);
      LOGGER.info(message);
      throw new UserValidationException(message);
    }
    Collection<UserType> cognitoUsers =
        cognitoService.search(
            UsersSearchParameter.SearchParameterBuilder.aSearchParameters()
                .withEmail(email)
                .build());

    if (!CollectionUtils.isEmpty(cognitoUsers)) {
      String message =
          MessageFormat.format("User with email {0} is already present in Cognito", email);
      LOGGER.info(message);
      throw new UserValidationException(message);
    }
    return composeUser(cwsUser, email);
  }

  private User composeUser(CwsUserInfo cwsUser, String email) {
    User user = new User();
    user.setEmail(email);
    user.setRacfid(cwsUser.getRacfId());
    enrichDataFromCwsOffice(cwsUser.getCwsOffice(), user);
    enrichDataFromStaffPerson(cwsUser.getStaffPerson(), user);
    return user;
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
      user.setOffice(office.getCwsOfficeName());
      Optional.ofNullable(office.getPrimaryPhoneNumber())
          .ifPresent(e -> user.setPhoneNumber(e.toString()));
      Optional.ofNullable(office.getPrimaryPhoneExtensionNumber())
          .ifPresent(e -> user.setPhoneExtensionNumber(e.toString()));
      Optional.ofNullable(office.getGovernmentEntityType())
          .ifPresent(
              x -> user.setCountyName((GovernmentEntityType.findBySysId(x)).getDescription()));
    }
  }

  private User enrichCognitoUser(UserType cognitoUser) {
    String racfId = getRACFId(cognitoUser);
    CwsUserInfo cwsUser = getCwsUserByRacfId(racfId);
    try {
      return configuration.getIdentityManager().getIdmMapping().map(cognitoUser, cwsUser);
    } catch (ScriptException e) {
      LOGGER.error("Error running the IdmMappingScript");
      throw new PerryException(e.getMessage(), e);
    }
  }

  private CwsUserInfo getCwsUserByRacfId(String racfId) {
    CwsUserInfo cwsUser = null;
    if (racfId != null) {
      List<CwsUserInfo> users = cwsUserInfoService.findUsers(Collections.singletonList(racfId));
      if (!CollectionUtils.isEmpty(users)) {
        cwsUser = users.get(0);
      }
    }
    return cwsUser;
  }

  static String getRACFId(UserType user) {
    return CognitoUtils.getAttributeValue(user, RACFID_ATTRIBUTE);
  }
}
