/*
* Copyright 2011 Google Inc. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance  with the License.  
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package com.google.sampling.experiential.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.common.collect.Lists;
import com.google.paco.shared.model.ExperimentDAO;
import com.google.sampling.experiential.datastore.JsonConverter;

/**
 * Servlet that answers requests for experiments.
 * 
 * Only used by the Android client to get the current list of experiments.
 * 
 * @author Bob Evans
 *
 */
@SuppressWarnings("serial")
public class ExperimentServlet extends HttpServlet {

  public static final Logger log = Logger.getLogger(ExperimentServlet.class.getName());
  public static final String DEV_HOST = "<Your machine name here>";
  private UserService userService;

  
  @Override
  // Bob: I didn't spend a whole lot of time cleaning this up because the isShortLoad part is temporary.  
  // What's your opinion?  Should I clean it up anyway so it's easier to follow for whoever changes the
  // iOS code?
  // Bob: Right now caching is messed up in this method because the full list of short experiments and
  // the full list of long experiments shares the same cache.  Shouldn't be a problem in general since
  // no app should be making both short and old-type experiment requests, but something to be aware of
  // while this code is in a transition state.
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    resp.setContentType("application/json;charset=UTF-8");
    String lastModParam = req.getParameter("lastModification");     // Bob: this is unused.  Should we remove it? -Priya
    String tz = req.getParameter("tz");

    boolean isShortLoad = false;
    boolean isFullExpLoad = false;
    List<Long> experimentIds = Lists.newArrayList();
    if (req.getParameter("short") != null) {
      isShortLoad = true;
    } else {
      String expStr = req.getParameter("id");
      if (expStr != null && !expStr.isEmpty()) {
        String[] strIds = expStr.split("[, ]+");
        for (String id : strIds) {
          Long experimentId = extractExperimentId(id);
          if (!experimentId.equals(new Long(-1))) {
            System.out.println("adding experiment id "  + experimentId);
            experimentIds.add(experimentId);
          }
        }
        isFullExpLoad = !(experimentIds.isEmpty());   // PRIYA
      }
    }
    
    User user = getWhoFromLogin();
    
    if (user == null) {
      resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
    } else {
      
      String pacoVersion = req.getHeader("paco.version");
      if (pacoVersion != null) {
        log.info("Paco version of request = " + pacoVersion);
      }
      
      ExperimentCacheHelper cacheHelper = ExperimentCacheHelper.getInstance();
      String experimentsJson = cacheHelper.getExperimentsJsonForUser(user.getUserId());    // PRIYA
      if (experimentsJson != null && !isFullExpLoad) {   // PRIYA
        log.info("Got cached experiments for " + user.getEmail());
      }
      
      List<ExperimentDAO> experiments;      // PRIYA
      if (experimentsJson == null || isFullExpLoad) { // PRIYA
        
        log.info("No cached experiments for " + user.getEmail());
        experiments = cacheHelper.getJoinableExperiments(tz);
        log.info("joinable experiments " + ((experiments != null) ? Integer.toString(experiments.size()) : "none"));
        String email = getEmailOfUser(req, user);
        List<ExperimentDAO> availableExperiments = null;
        if (experiments == null) {
          experiments = Lists.newArrayList();
          availableExperiments = experiments;        
        } else {
          availableExperiments = ExperimentRetriever.getSortedExperimentsAvailableToUser(experiments, email);        
        }
        ExperimentRetriever.removeSensitiveFields(availableExperiments);
        
        if (isShortLoad) {
          experimentsJson = JsonConverter.shortJsonify(availableExperiments);
          cacheHelper.putExperimentJsonForUser(user.getUserId(), experimentsJson); 
        } else if (isFullExpLoad) {     // PRIYA
          experimentsJson = loadExperiments(experimentIds, availableExperiments);
          // Do not put in the cache.
        } else {
          experimentsJson = JsonConverter.jsonify(availableExperiments);
          cacheHelper.putExperimentJsonForUser(user.getUserId(), experimentsJson); 
        }       
      }    
      resp.getWriter().println(scriptBust(experimentsJson));
    }
  }
  
  private Long extractExperimentId(String expStr) {
    try {
      Long experimentId = Long.parseLong(expStr, 10);
      return experimentId;      // PRIYA
    } catch (NumberFormatException e) {
      log.severe("Invalid experiment id " + expStr + " sent to server.");
      return new Long(-1);
    }
  }
  
  private String loadExperiments(List<Long> experimentIds, List<ExperimentDAO> availableExperiments) {
    List<ExperimentDAO> experiments = Lists.newArrayList();
    for (ExperimentDAO experiment : availableExperiments) {
      if (experimentIds.contains(experiment.getId())) {           // PRIYA - this could be super slow.  Maybe use hash set.
        experiments.add(experiment);
      }
    }
    if (experiments.isEmpty()) {
      log.severe("Experiment id's " + experimentIds + " are all invalid.  No experiments were fetched from server.");
      return null;
    }
    return JsonConverter.jsonify(experiments); // PRIYA - change on android side;
  }

  
  private String scriptBust(String experimentsJson) {
    // TODO add scriptbusting prefix to this and client code.
    return experimentsJson;
  }


  private String getEmailOfUser(HttpServletRequest req, User user) {
    String email = user != null ? user.getEmail() : null;
    if (email == null && isDevInstance(req)) {
      email = "<put your email here to test in developor mode>";
    }
    if (email == null) {
      throw new IllegalArgumentException("You must login!");
    }
    return email.toLowerCase();
  }

  private User getWhoFromLogin() {
    UserService userService = UserServiceFactory.getUserService();
    return userService.getCurrentUser();
  }
  
  public static boolean isDevInstance(HttpServletRequest req) {
    try {
      return DEV_HOST.equals(InetAddress.getLocalHost().toString());
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    return false;
  }

}
