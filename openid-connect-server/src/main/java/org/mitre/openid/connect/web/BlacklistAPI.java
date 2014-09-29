/*******************************************************************************
 * Copyright 2014 The MITRE Corporation
 *   and the MIT Kerberos and Internet Trust Consortium
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/**
 * 
 */
package org.mitre.openid.connect.web;

import java.security.Principal;
import java.util.Collection;

import org.mitre.openid.connect.model.BlacklistedSite;
import org.mitre.openid.connect.service.BlacklistedSiteService;
import org.mitre.openid.connect.view.HttpCodeView;
import org.mitre.openid.connect.view.JsonEntityView;
import org.mitre.openid.connect.view.JsonErrorView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * @author jricher
 *
 */
@Controller
@RequestMapping("/api/blacklist")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class BlacklistAPI {

	@Autowired
	private BlacklistedSiteService blacklistService;

	private static Logger logger = LoggerFactory.getLogger(BlacklistAPI.class);

	private Gson gson = new Gson();
	private JsonParser parser = new JsonParser();

	/**
	 * Get a list of all blacklisted sites
	 * @param m
	 * @return
	 */
	@RequestMapping(method = RequestMethod.GET, produces = "application/json")
	public String getAllBlacklistedSites(ModelMap m) {

		Collection<BlacklistedSite> all = blacklistService.getAll();

		m.put("entity", all);

		return JsonEntityView.VIEWNAME;
	}

	/**
	 * Create a new blacklisted site
	 * @param jsonString
	 * @param m
	 * @param p
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
	public String addNewBlacklistedSite(@RequestBody String jsonString, ModelMap m, Principal p) {

		JsonObject json;

		BlacklistedSite blacklist = null;

		try {

			json = parser.parse(jsonString).getAsJsonObject();
			blacklist = gson.fromJson(json, BlacklistedSite.class);
			BlacklistedSite newBlacklist = blacklistService.saveNew(blacklist);
			m.put("entity", newBlacklist);

		}
		catch (JsonSyntaxException e) {
			logger.error("addNewBlacklistedSite failed due to JsonSyntaxException: ", e);
			m.put("code", HttpStatus.BAD_REQUEST);
			m.put("errorMessage", "Could not save new blacklisted site. The server encountered a JSON syntax exception. Contact a system administrator for assistance.");
			return JsonErrorView.VIEWNAME;
		} catch (IllegalStateException e) {
			logger.error("addNewBlacklistedSite failed due to IllegalStateException", e);
			m.put("code", HttpStatus.BAD_REQUEST);
			m.put("errorMessage", "Could not save new blacklisted site. The server encountered an IllegalStateException. Refresh and try again - if the problem persists, contact a system administrator for assistance.");
			return JsonErrorView.VIEWNAME;
		}

		return JsonEntityView.VIEWNAME;

	}

	/**
	 * Update an existing blacklisted site
	 */
	@RequestMapping(value="/{id}", method = RequestMethod.PUT, consumes = "application/json", produces = "application/json")
	public String updateBlacklistedSite(@PathVariable("id") Long id, @RequestBody String jsonString, ModelMap m, Principal p) {

		JsonObject json;

		BlacklistedSite blacklist = null;

		try {

			json = parser.parse(jsonString).getAsJsonObject();
			blacklist = gson.fromJson(json, BlacklistedSite.class);

		}
		catch (JsonSyntaxException e) {
			logger.error("updateBlacklistedSite failed due to JsonSyntaxException", e);
			m.put("code", HttpStatus.BAD_REQUEST);
			m.put("errorMessage", "Could not update blacklisted site. The server encountered a JSON syntax exception. Contact a system administrator for assistance.");
			return JsonErrorView.VIEWNAME;
		} catch (IllegalStateException e) {
			logger.error("updateBlacklistedSite failed due to IllegalStateException", e);
			m.put("code", HttpStatus.BAD_REQUEST);
			m.put("errorMessage", "Could not update blacklisted site. The server encountered an IllegalStateException. Refresh and try again - if the problem persists, contact a system administrator for assistance.");
			return JsonErrorView.VIEWNAME;
		}


		BlacklistedSite oldBlacklist = blacklistService.getById(id);

		if (oldBlacklist == null) {
			logger.error("updateBlacklistedSite failed; blacklist with id " + id + " could not be found");
			m.put("code", HttpStatus.NOT_FOUND);
			m.put("errorMessage", "Could not update blacklisted site. The requested blacklist with id " + id + "could not be found.");
			return JsonErrorView.VIEWNAME;
		} else {

			BlacklistedSite newBlacklist = blacklistService.update(oldBlacklist, blacklist);

			m.put("entity", newBlacklist);

			return JsonEntityView.VIEWNAME;
		}
	}

	/**
	 * Delete a blacklisted site
	 * 
	 */
	@RequestMapping(value="/{id}", method = RequestMethod.DELETE)
	public String deleteBlacklistedSite(@PathVariable("id") Long id, ModelMap m) {
		BlacklistedSite blacklist = blacklistService.getById(id);

		if (blacklist == null) {
			logger.error("deleteBlacklistedSite failed; blacklist with id " + id + " could not be found");
			m.put("errorMessage", "Could not delete bladklist. The requested bladklist with id " + id + " could not be found.");
			return JsonErrorView.VIEWNAME;
		} else {
			m.put("code", HttpStatus.OK);
			blacklistService.remove(blacklist);
		}

		return HttpCodeView.VIEWNAME;
	}

	/**
	 * Get a single blacklisted site
	 */
	@RequestMapping(value="/{id}", method = RequestMethod.GET, produces = "application/json")
	public String getBlacklistedSite(@PathVariable("id") Long id, ModelMap m) {
		BlacklistedSite blacklist = blacklistService.getById(id);
		if (blacklist == null) {
			logger.error("getBlacklistedSite failed; blacklist with id " + id + " could not be found");
			m.put("code", HttpStatus.NOT_FOUND);
			m.put("errorMessage", "Could not delete bladklist. The requested bladklist with id " + id + " could not be found.");
			return JsonErrorView.VIEWNAME;
		} else {

			m.put("entity", blacklist);

			return JsonEntityView.VIEWNAME;
		}

	}
}
