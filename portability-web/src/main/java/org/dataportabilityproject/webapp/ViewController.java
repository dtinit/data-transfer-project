package org.dataportabilityproject.webapp;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/** Controller to redirect all paths to the angular view */
@Controller
public class ViewController {


  /** Maps angular routes paths back to angular */
  @RequestMapping({ "/home", "/export.*", "/import.*", "/copy.*", "/demo.*", "/next.*", "/simplelogin.*"})
  public String index(HttpServletRequest request) {
    System.out.println("Redirecting to home after receiving: " + request.getRequestURI());
    return "forward:/index.html";
  }
}
