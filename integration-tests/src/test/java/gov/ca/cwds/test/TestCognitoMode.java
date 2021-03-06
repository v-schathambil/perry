package gov.ca.cwds.test;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.serenitybdd.junit.runners.SerenityParameterizedRunner;
import net.thucydides.core.annotations.Steps;
import net.thucydides.junit.annotations.Concurrent;
import net.thucydides.junit.annotations.TestData;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

@RunWith(SerenityParameterizedRunner.class)
@Concurrent
public class TestCognitoMode {

  public static final String USERNAME_XPATH = "(//input[@name='username'])[2]";
  public static final String PASSWORD_XPATH = "(//input[@name='password'])[2]";
  public static final String SUBMIT_XPATH = "(//form[@name='cognitoSignInForm'])[2]";

  private WebDriver driver;
  private final TestDataBean testDataBean;

  @Steps
  private LoginSteps loginSteps;

  public TestCognitoMode(TestDataBean testDataBean) {
    this.testDataBean = testDataBean;
  }

  @TestData
  public static Collection<Object[]> testData() {
    //TODO: separate data per thread
    int threadsCount = Integer.parseInt(System.getProperty("perry.threads.count", "1"));
    return IntStream.range(0, threadsCount).boxed().map(i -> {
      Object[] item = new TestDataBean[1];
      item[0] = new TestDataBean();
      return item;
    }).collect(Collectors.toList());
  }

  @BeforeClass
  public static void setupClass() {
    WebDriverManager.chromedriver().setup();
  }

  @Before
  public void init() throws MalformedURLException {
    if (StringUtils.isEmpty(testDataBean.getGridUrl())) {
      driver = new HtmlUnitDriver();
    } else {
      driver = new RemoteWebDriver(new URL(testDataBean.getGridUrl()), DesiredCapabilities.chrome());
    }
    loginSteps.setDriver(driver);
  }

  @After
  public void teardown() {
    if (driver != null) {
      driver.quit();
    }
  }

  @Test
  public void testCognitoMode() throws Exception {
    loginSteps.goToPerryLoginUrl(testDataBean.getUrl() + "/authn/login?callback=" + testDataBean.getUrl() + "/demo-sp.html");
    loginSteps.isElementPresentXpath(USERNAME_XPATH);
    loginSteps.typeXpath(USERNAME_XPATH, testDataBean.getUsername());
    loginSteps.typeXpath(PASSWORD_XPATH, testDataBean.getPassword());
    loginSteps.clickXpath(SUBMIT_XPATH);
    String accessCode = loginSteps.waitForAccessCodeParameter();
    String perryToken = loginSteps.mapAccessCode(testDataBean.getUrl() + "/authn/token?accessCode=" + accessCode);
    for (int i = 0; i<Integer.parseInt(testDataBean.getValidateRepeatCount()); i++) {
      String jsonToken = loginSteps.validateToken(testDataBean.getUrl() + "/authn/validate?token=" + perryToken);
      loginSteps.validateTokenContent(testDataBean.getJson(), jsonToken);
    }
  }
}