package gov.ca.cwds.idm.service.diff;

public final class BooleanDiff extends BaseDiff<Boolean> {

  public BooleanDiff(final Boolean oldValue, final Boolean newValue) {
    super(oldValue, newValue);
  }

  @Override
  String toStringValue(Boolean value) {
    return String.valueOf(value);
  }
}