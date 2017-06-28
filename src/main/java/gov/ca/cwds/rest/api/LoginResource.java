package gov.ca.cwds.rest.api;

import gov.ca.cwds.service.LoginService;
import gov.ca.cwds.service.OauthLoginService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.core.*;

/**
 * Created by dmitry.rudenko on 5/22/2017.
 */
@RestController
public class LoginResource {
    @Autowired
    LoginService loginService;

    @GET
    @RequestMapping("/authn/login")
    @Transactional
    @ApiOperation(
            value = "Login. Applications should direct users to this endpoint for login.  When authentication complete, user will be redirected back to callback with auth 'token' as a query parameter",
            code = 200)
    public void login(@NotNull @Context final HttpServletResponse response,
                      @ApiParam(required = true, name = "callback",
                              value = "URL to send the user back to after authentication") @RequestParam("callback") @NotNull String callback,
                      @ApiParam(name = "sp_id",
                              value = "Service provider id") @RequestParam(name = "sp_id", required = false) String spId) throws Exception {
        String jwtToken = loginService.login(spId);
        response.sendRedirect(callback + "?token=" + jwtToken);
    }

    //back-end only!
    @GET
    @RequestMapping("/authn/validate")
    @ApiOperation(value = "Validate an authentication token", code = 200)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "authorized"),
            @ApiResponse(code = 401, message = "Unauthorized")})
    public String validateToken(@Context final HttpServletResponse response, @NotNull @ApiParam(required = true, name = "token",
            value = "The token to validate") @RequestParam("token") String token) {
        try {
            return loginService.validate(token);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return "Unauthorized";
        }

    }
}