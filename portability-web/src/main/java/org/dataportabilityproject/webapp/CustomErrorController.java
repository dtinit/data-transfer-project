package org.dataportabilityproject.webapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;


/** Custom error controller that displays all error attributes. */
@Controller
public class CustomErrorController implements ErrorController {

  @Autowired
  private ErrorAttributes errorAttributes;

  @Override
  public String getErrorPath() {
    return "/error";
  }

  @RequestMapping(value = "/error")
  public String error(HttpServletRequest servletRequest, Model model) {
    Map<String, Object> attrs = errorAttributes.getErrorAttributes(new ServletRequestAttributes(servletRequest), false);
    model.addAttribute("attrs", attrs);
    return "error handling";
  }
}
