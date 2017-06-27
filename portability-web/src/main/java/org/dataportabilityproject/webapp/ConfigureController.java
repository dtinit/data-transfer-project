package org.dataportabilityproject.webapp;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.webapp.job.JobManager;
import org.dataportabilityproject.webapp.job.PortabilityJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/** Controller to process the configuration submitted via the form. */
@RestController
public class ConfigureController {
  @Autowired
  private ServiceProviderRegistry serviceProviderRegistry;
  @Autowired
  private JobManager jobManager;

  /** Configures the job and kicks off the auth flow. */
  @CrossOrigin(origins = "http://localhost:3000")
  @RequestMapping(path="/configure", method = RequestMethod.POST)
  @ResponseBody
  public String configure(HttpServletRequest request,
      @CookieValue(value = "token", required = false) String token) throws Exception {

    // Set a cookie the first time through
    // TODO: Move to interceptor
    if (Strings.isNullOrEmpty(token)) {
      String newToken = jobManager.createNewUserjob();
      Cookie cookie = new Cookie("token", newToken);
      System.out.println("Set the cookie for token: " + newToken);
      // TODO: response.addCookie(cookie);
    } else {
      System.out.println("Found existing cookie,  token: " + token);
    }

    return "TO BE IMPLEMENTED";
  }
}
