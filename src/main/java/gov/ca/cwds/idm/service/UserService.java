package gov.ca.cwds.idm.service;

import static gov.ca.cwds.config.TokenServiceConfiguration.TOKEN_TRANSACTION_MANAGER;

import gov.ca.cwds.idm.service.cognito.CognitoServiceFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("idm")
public class UserService {

  private CognitoServiceFacade cognitoServiceFacade;

  private NsUserService nsUserService;

  private AuditEventService auditEventService;

  /**
   * @return true if User attributes (in Cognito and database) were really updated, false otherwise
   */
  @Transactional(value = TOKEN_TRANSACTION_MANAGER)
  public boolean updateUserAttributes(UserUpdateRequest userUpdateRequest) {
    userUpdateRequest.getAuditEvents().forEach(auditEventService::saveAuditEventsToDb);
    boolean isDatabaseUpdated = nsUserService.update(userUpdateRequest);
    boolean isCognitoUpdated = cognitoServiceFacade.updateUserAttributes(userUpdateRequest);
    return (isDatabaseUpdated || isCognitoUpdated);
  }

  @Autowired
  public void setCognitoServiceFacade(CognitoServiceFacade cognitoServiceFacade) {
    this.cognitoServiceFacade = cognitoServiceFacade;
  }

  @Autowired
  public void setNsUserService(NsUserService nsUserService) {
    this.nsUserService = nsUserService;
  }

  @Autowired
  public void setAuditEventService(AuditEventService auditEventService) {
    this.auditEventService = auditEventService;
  }

}
