package gov.ca.cwds.idm.service;


import gov.ca.cwds.idm.event.AuditEvent;
import gov.ca.cwds.idm.service.cognito.AuditProperties;
import gov.ca.cwds.util.CurrentAuthenticatedUserUtil;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Profile("idm")
public class AuditEventIndexService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuditEventIndexService.class);

  private static final String DORA_URL = "doraUrl";
  private static final String ES_AUDIT_INDEX = "esUserIndex";
  private static final String ES_AUDIT_TYPE = "esUserType";
  private static final String ID = "id";
  private static final String SSO_TOKEN = "ssoToken";

  private static final String CREATE_URL_TEMPLATE =
      "{"
          + DORA_URL
          + "}/dora/{"
          + ES_AUDIT_INDEX
          + "}/{"
          + ES_AUDIT_TYPE
          + "}/{"
          + ID
          + "}/_create?token={"
          + SSO_TOKEN
          + "}";

  @Autowired
  private AuditProperties auditProperties;

  @Autowired
  private IndexRestSender restSender;

  @Async("auditLogTaskExecutor")
  public <T extends AuditEvent> void sendAuditEventToEsIndex(T event) {
    String eventId = event.getId();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<AuditEvent> requestEntity = new HttpEntity<>(event, headers);

    Map<String, String> params = new HashMap<>();
    params.put(DORA_URL, auditProperties.getDoraUrl());
    params.put(ES_AUDIT_INDEX, auditProperties.getIndex());
    params.put(ES_AUDIT_TYPE, auditProperties.getType());
    params.put(ID, eventId);
    params.put(SSO_TOKEN, getSsoToken());
    ResponseEntity<String> response;
    try {
      response = restSender.send(CREATE_URL_TEMPLATE, requestEntity, params);

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(
            "Audit record, id:{} was successfully stored in Elastic Search index, Dora response string is:{}",
            eventId,
            response.getBody());
      }
    } catch (Exception e) {
      LOGGER.error("Error while storing the audit event {} for user {}.",
          event.getEventType(),
          event.getUserLogin());
      throw e;
    }
  }

  protected String getSsoToken() {
    return CurrentAuthenticatedUserUtil.getSsoToken();
  }


  public void setAuditProperties(AuditProperties auditProperties) {
    this.auditProperties = auditProperties;
  }

  public void setRestSender(IndexRestSender restSender) {
    this.restSender = restSender;
  }

}
