/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */

/**
 * Maximum number of projects to be read. This value constraints the number of
 * combo box items in the UI.
 */
var MAX_PROJECTS = 50;

/**
 * Maximum number of users per project. This value constraints the number of
 * combo box items in the UI.
 */
var MAX_USERS = 50;

/**
 * Import requried Java classes
 */
var IOException = Java.type("java.io.IOException");
var Property = Java.type("com.wilutions.itol.db.Property");
var PropertyClass = Java.type("com.wilutions.itol.db.PropertyClass");
var PropertyClasses = Java.type("com.wilutions.itol.db.PropertyClasses");
var IdName = Java.type("com.wilutions.itol.db.IdName");
var JHttpClient = Java.type("com.wilutions.itol.db.HttpClient");
var HttpResponse = Java.type("com.wilutions.itol.db.HttpResponse");
var IssueUpdate = Java.type("com.wilutions.itol.db.IssueUpdate");
var Issue = Java.type("com.wilutions.itol.db.Issue");
var DescriptionHtmlEditor = Java
		.type("com.wilutions.itol.db.DescriptionHtmlEditor");
var Logger = Java.type("java.util.logging.Logger");
var log = Logger.getLogger("IssueServiceImpl.js");

/**
 * Dump JavaScript objects into the log file.
 * 
 * @param name
 *            String, used as title.
 * @param obj
 *            Object to be dumped.
 */
function dump(name, obj) {
	var str = JSON.stringify(obj, null, 2);
	log.info(name + "=" + str);
}

/**
 * Configuration values. This values can be edited in the configuration page
 * "Issue Tracker" in the backstage view of Outlook's main window. If you want
 * to add a new configuration property, follow the instructions in the lines
 * marked with MY_CONFIG_PROPERTY.
 */
var config = {

	/**
	 * Redmine URL
	 */
	url : "http://192.168.0.11",

	/**
	 * API key for authentication.
	 */
	apiKey : "... see \"Redmine / My account / API access key\" ... ",

	/**
	 * Comma separated list of project names. Only projects listed here are
	 * shown in the UI.
	 */
	projectNames : "",

	/**
	 * Attach mail encoded in this format.
	 */
	msgFileType : ".msg",

	/**
	 * MY_CONFIG_PROPERTY Add your property here. Properties can be of type
	 * string or boolean. A list of possible values can be assigned to a string
	 * property definition. In this case, a combo box is associated to the
	 * property. The syntax for declaring a property is: property-name =
	 * "property-value" comma
	 */
	// my_config_property = "a default value",

	// Property IDs of configuration data.
	// IDs are member names to simplify function fromProperties
	PROPERTY_ID_URL : "url",
	PROPERTY_ID_API_KEY : "apiKey",
	PROPERTY_ID_PROJECT_NAMES : "projectNames",
	PROPERTY_ID_MSG_FILE_TYPE : "msgFileType",

	/**
	 * MY_CONFIG_PROPERTY Define a property ID. Currently, use the property name
	 * as ID. Syntax: property-id-variable colon "property-name" comma
	 */
	// PROPERTY_ID_MY_CONFIG_PROPERTY : "my_config_property",
	/**
	 * Collect the properties in an array. The configuration view calls this
	 * function to receive the property values.
	 */
	toProperties : function() {
		return [
				// Add your property here:
				// new Property(this.PROPERTY_ID_MY_CONFIG_PROPERTY,
				// this.my_config_property),
				new Property(this.PROPERTY_ID_URL, this.url),
				new Property(this.PROPERTY_ID_API_KEY, this.apiKey),
				new Property(this.PROPERTY_ID_PROJECT_NAMES, this.projectNames),
				new Property(this.PROPERTY_ID_MSG_FILE_TYPE, this.msgFileType) ];
	},

	fromProperties : function(props) {
		for (var i = 0; i < props.length; i++) {
			var memberName = props[i].id + "";
			this[memberName] = props[i].value;
		}

		// strip trailing / from URL
		if (this.url && this.url.indexOf("/", this.url.length - 1) != -1) {
			this.url = this.url.substring(0, this.url.length - 1);
		}
	},

	/**
	 * This property is set as true, if initialization succeeds. All
	 * IssueService functions have to check this member.
	 */
	valid : false,

	/**
	 * Throw exception if configuration is invalid.
	 */
	checkValid : function() {
		if (!this.valid) {
			throw new IOException(
					"Initialization failed. Check configuration properties on backstage view.");
		}
	},

};

/**
 * Execute HTTP requests. JavaScript wrapper around the Java class JHttpClient.
 */
var httpClient = {

	/**
	 * Send a request.
	 * 
	 * @param method
	 *            Either "POST", "GET", "PUT", "DELETE".
	 * @param header
	 *            Array of headers, each header in form "header-name :
	 *            header-value", e.g. ["Content-Type: application/json"]
	 * @return response text.
	 * @throws IOException
	 *             on error status (HTTP != 2xx)
	 */
	send : function(method, headers, params, content, progressCallback) {
		var destUrl = config.url + params;
		this._addAuthHeader(headers);
		var response = JHttpClient.send(destUrl, method, headers, content,
				progressCallback ? progressCallback : null);
		if (response.status < 200 || response.status >= 300) {
			var msg = "";
			if (response.status) {
				msg += "HTTP Status " + response.status;
			}
			if (response.errorMessage) {
				if (msg) {
					msg += ", ";
				}
				msg += response.errorMessage;
			}
			if (response.content) {
				if (msg) {
					msg += ", ";
				}
				msg += response.content;
			}
			throw new IOException(msg);
		}
		return response;
	},

	/**
	 * Send POST request to transmit a JSON object.
	 * 
	 * @param params
	 *            URL parameters, e.g. "/issues.json"
	 * @param content
	 *            JSON object to be posted
	 * @param progressCallback
	 *            Listener object to watch progress.
	 * @return Server response as string, usually in JSON format.
	 */
	post : function(params, content, progressCallback) {
		var headers = [ "Content-Type: application/json" ];
		var jsonStr = JSON.stringify(content);
		var ret = this.send("POST", headers, params, jsonStr, progressCallback);
		return JSON.parse(ret.content);
	},

	/**
	 * Send PUT request to transmit a JSON object.
	 * 
	 * @param params
	 *            URL parameters, e.g. "/issues/1234.json"
	 * @param content
	 *            JSON object to be posted
	 * @param progressCallback
	 *            Listener object to watch progress.
	 */
	put : function(params, content, progressCallback) {
		var headers = [ "Content-Type: application/json" ];
		var jsonStr = JSON.stringify(content);
		this.send("PUT", headers, params, jsonStr, progressCallback);
	},

	/**
	 * Send POST request to upload a file.
	 * 
	 * @param URL
	 *            parameters, e.g. "/uploads.json"
	 * @param content
	 *            File content, a java.io.InputStream
	 * @param contentLength
	 *            Content length
	 * @param progressCallback
	 *            Listener object to watch progress.
	 * @return Server response as string, usually in JSON format.
	 */
	upload : function(params, content, contentLength, progressCallback) {
		var headers = [ "Content-Type: application/octet-stream" ];
		if (contentLength) {
			headers.push("Content-Length: " + contentLength);
		}
		var ret = this.send("POST", headers, params, content, progressCallback);
		return JSON.parse(ret.content);
	},

	/**
	 * Send GET request to receive a JSON object.
	 * 
	 * @param params
	 *            URL parameters, e.g. "/projects.json?offset=..."
	 * @return Server response as string, usually in JSON format.
	 */
	get : function(params) {
		var headers = [];
		return JSON.parse(this.send("GET", headers, params, null).content);
	},

	/**
	 * Add the Redmine API key in header X-Redmine-API-Key for authentication.
	 */
	_addAuthHeader : function(headers) {
		if (config.apiKey) {
			headers.push("X-Redmine-API-Key: " + config.apiKey);
		} else if (config.userName) {
			var auth = JHttpClient.makeBasicAuthenticationHeader(
					config.userName, config.userPwd);
			headers.push("Authorization: Basic " + auth);
		}
	}
};

/**
 * Cache of frequently used data.
 */
var data = {

	/**
	 * Map of projects. Key: project ID, value: project.
	 * this.projects[.].members contains an array of project members
	 * (assignees). this.projects[.].versions contains an array of versions
	 * (milestones).
	 */
	projects : {},

	/**
	 * Current user.
	 */
	user : {},

	/**
	 * Array of tracker IdName objects
	 */
	trackers : [],

	/**
	 * Array of priority IdName objects
	 */
	priorities : [],

	/**
	 * Default priority ID.
	 */
	defaultPriority : 0,

	/**
	 * Array of status IdName objects
	 */
	statuses : [],

	clear : function() {
		this.projects = {};
		this.user = {};
		this.trackers = [];
		this.priorities = [];
		this.defaultPriority = 0;
	}

}

function arrayIndexOf(arr, elm) {
	for (var j = 0; j < arr.length; j++) {
		if (elm == arr[j]) {
			return j;
		}
	}
	return -1;
}

function readProjects(data) {
	log.info("readProjects(");

	var projectNames = [];
	log.info("config.projectNames=" + config.projectNames);
	if (config.projectNames) {
		projectNames = config.projectNames.split(",");
		if (projectNames.length != 0) {
			log.info("Look for this projects=" + projectNames);
		}
	}

	var projectCount = 0;
	var offset = 0;
	while (projectCount < MAX_PROJECTS) {

		var projectsResponse = httpClient.get("/projects.json?"
				+ "include=trackers&" + "offset=" + offset + "&limit="
				+ (MAX_PROJECTS - offset));
		var arrOfProjects = projectsResponse.projects;
		if (arrOfProjects.length == 0) {
			break;
		}

		for (var i = 0; i < arrOfProjects.length && projectCount < MAX_PROJECTS; i++) {
			var project = arrOfProjects[i];
			log.info("Found project, id=" + project.id + ", name="
					+ project.name);
			if (projectNames.length == 0
					|| arrayIndexOf(projectNames, project.name) != -1) {
				data.projects[project.id] = project;
				dump("Add project to select list", project);
				projectCount++;
			}
		}

		offset += arrOfProjects.length;
	}
	log.info(")readProjects");
};

function readCurrentUser(data) {
	log.info("readCurrentUser(");
	data.user = httpClient.get("/users/current.json").user;
	dump("user ", data.user);
	log.info(")readCurrentUser");
};

function readProjectVersions(project) {
	log.info("readProjectVersions(project.id=" + project.id);
	project.versions = [];
	var arrOfVersions = httpClient.get("/projects/" + project.id
			+ "/versions.json").versions;
	for (var j = 0; j < arrOfVersions.length; j++) {
		var version = arrOfVersions[j];
		project.versions.push(version);
	}
	dump("project.versions", project.versions);
	log.info(")readProjectVersions");
}

function readProjectMembers(project) {
	log.info("readProjectMembers(project.id=" + project.id);
	project.members = [];
	var offset = 0;
	while (project.members.length < MAX_USERS) {

		var arrOfMemberships = httpClient.get("/projects/" + project.id
				+ "/memberships.json?" + "offset=" + offset + "&limit="
				+ (MAX_USERS - offset)).memberships;

		if (arrOfMemberships.length == 0) {
			break;
		}

		dump("arrOfMemberships", arrOfMemberships);

		for (var j = 0; j < arrOfMemberships.length && j < MAX_USERS; j++) {
			var membership = arrOfMemberships[j];
			var user = membership.user;
			if (user) { // see issue #9, membership.user might be undefined
				project.members.push(user);
			}
		}
		offset += arrOfMemberships.length;
	}
	dump("project.members", project.members);
	log.info(")readProjectMembers");
}

function writeIssue(issueParam, progressCallback) {
	log.info("writeIssue(");
	dump("send", issueParam);
	var ret = null;

	var pgIssue = null;
	if (progressCallback) {
		if (progressCallback.isCancelled())
			return;
		pgIssue = progressCallback.createChild("Write issue");
	}

	var issueId = issueParam.issue.id;
	var isUpdate = !!issueId;
	log.info("issueId=" + issueId + ", isUpdate=" + isUpdate);

	if (isUpdate) {
		ret = httpClient.put("/issues/" + issueId + ".json", issueParam,
				pgIssue);
	} else {
		ret = httpClient.post("/issues.json", issueParam, pgIssue);
	}

	dump("recv", ret);
	log.info(")writeIssue=");
	return ret;
}

function initialize() {
	config.valid = false;

	data.clear();

	readProjects(data);

	readCurrentUser(data);

	readTrackers(data);

	readPriorities(data);
	
	readStatuses(data);

	config.valid = true;
}

function readTrackers(data) {
	log.info("readTrackers(");
	var trackersResponse = httpClient.get("/trackers.json");
	dump("trackersResponse", trackersResponse);
	for (var i = 0; i < trackersResponse.trackers.length; i++) {
		var tracker = trackersResponse.trackers[i];
		data.trackers.push(new IdName(tracker.id, tracker.name));
	}

	log.info(")readTrackers");
}

function readPriorities(data) {
	log.info("readPriorities(");
	var prioritiesResponse = httpClient
			.get("/enumerations/issue_priorities.json");
	dump("prioritiesResponse", prioritiesResponse);
	data.priorities = [];
	for (var i = 0; i < prioritiesResponse.issue_priorities.length; i++) {
		var priority = prioritiesResponse.issue_priorities[i];
		data.priorities.push(new IdName(priority.id, priority.name));
		if (priority.is_default) {
			data.defaultPriority = priority.id;
		}
	}
	log.info(")readPriorities");

}

function readStatuses(data) {
	log.info("readStatuses(");
	var statusesResponse = httpClient.get("/issue_statuses.json");
	dump("statusesResponse", statusesResponse);
	data.statuses = [];
	for (var i = 0; i < statusesResponse.issue_statuses.length; i++) {
		var status = statusesResponse.issue_statuses[i];
		data.statuses.push(new IdName(status.id, status.name));
	}
	if (!data.statuses) {
		data.statuses = [ new IdName(1, "New issue") ];
	}
	log.info(")readStatuses");
}

/**
 * Initialize property classes. In config.propertyClasses, a type definition is
 * stored for each property. The UI interprets the type definition and assigns
 * an appropriate UI control.
 */
function initializePropertyClasses() {

	var propertyClasses = PropertyClasses.getDefault();

	// Configuration properties

	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_URL,
			"Redmine URL");
	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_API_KEY,
			"API key");
	propertyClasses.add(PropertyClass.TYPE_STRING,
			config.PROPERTY_ID_PROJECT_NAMES,
			"Projects (optional, comma separated)");
	propertyClasses.add(PropertyClass.TYPE_STRING,
			config.PROPERTY_ID_MSG_FILE_TYPE, "Attach mail as");

	// propertyClass.add(PropertyClass.TYPE_STRING,
	// config.PROPERTY_ID_MY_CONFIG_PROPERTY,
	// "my_config_property label"

	// Initialize select list for some issue properties

	var propMsgFileType = propertyClasses.get(config.PROPERTY_ID_MSG_FILE_TYPE);
	propMsgFileType.setSelectList([ new IdName(".msg", "Outlook (.msg)"),
			new IdName(".mhtml", "MIME HTML (.mhtml)"),
			new IdName(".rtf", "Rich Text Format (.rtf)") ]);

	// Rename "Milestones" to "Version"
	var propMilestones = propertyClasses.get(Property.MILESTONES);
	propMilestones.setName("Versions");
	
	config.propertyClasses = propertyClasses;
}

function getConfig() {
	return config.toProperties();
}

function setConfig(configProperties) {
	if (configProperties) {
		config.fromProperties(configProperties);
		initialize();
	}
};

function getPropertyClasses() {
	return config.propertyClasses;
};

function getPropertyClass(propertyId, issue) {
	var ret = getPropertyClasses().getCopy(propertyId);
	switch(propertyId) {
	case Property.ISSUE_TYPE:
		ret.selectList = getIssueTypes(issue);
		break;
	case Property.PRIORITY:
		ret.selectList = data.priorities;
		break;
	case Property.CATEGORY:
		ret.selectList = getCategories(issue);
		break;
	case Property.MILESTONES:
		ret.selectList = getMilestones(issue);
		break;
	case Property.ASSIGNEE:
		ret.selectList = getAssignees(issue);
		break;
	case Property.STATUS:
		ret.selectList = data.statuses;
		break;
	default:
		break;
	}
	return ret;
}

function getIssueTypes(issue) {
	var ret = [];
	var projectId = issue ? issue.getCategory() : -1;
	var project = data.projects[projectId];
	log.info("project=" + project);
	if (project) {
		if (!project.trackers) {
			var projectResponse = httpClient.get("/projects/" + projectId
					+ ".json?include=trackers");
			project.trackers = projectResponse.project.trackers;
			dump("Added project.trackers", project.trackers);
		}
		if (project.trackers) {
			for (var i = 0; i < project.trackers.length; i++) {
				var idn = new IdName(project.trackers[i].id,
						project.trackers[i].name);
				ret.push(idn);
			}
		}
	}
	return ret;
};

function getCategories(issue) {
	var ret = [];
	for ( var projectId in data.projects) {
		var project = data.projects[projectId];
		var idn = new IdName(projectId, project.name);
		ret.push(idn);
	}
	return ret;
};

function getMilestones(issue) {
	var ret = [];
	var projectId = issue ? issue.getCategory() : 0;
	var project = data.projects[projectId];
	log.info("project=" + project);
	if (project) {

		if (typeof project.versions === "undefined") {
			readProjectVersions(project);
		}

		for (var i = 0; i < project.versions.length; i++) {
			var version = project.versions[i];
			if (version.status == "open") {
				ret.push(new IdName(version.id, version.name));
			}
		}
	}
	return ret;
};

function getAssignees(issue) {
	var ret = [ new IdName(-1, "Unassigned") ];
	var projectId = issue ? issue.getCategory() : 0;
	var project = data.projects[projectId];
	log.info("project=" + project);
	if (project) {

		if (typeof project.members === "undefined") {
			readProjectMembers(project);
		}

		for (var i = 0; i < project.members.length; i++) {
			var member = project.members[i];
			ret.push(new IdName(member.id, member.name));
		}
	}
	return ret;
};

function getCurrentUser() {
	return data.user ? new IdName(parseInt(data.user.id), data.user.firstname
			+ " " + data.user.lastname) : new IdName(0, "");
};

function getDescriptionTextEditor(issue) {
	return null;
}

function getDescriptionHtmlEditor(issue) {
	var htmlTemplate = "<html>"
			+ "<head>"
			+ "<script src=\"REDMINE_URL/javascripts/jstoolbar/jstoolbar-textile.min.js?1420968012\" type=\"text/javascript\">"
			+ "</script>"
			+ "<script src=\"REDMINE_URL/javascripts/jstoolbar/lang/jstoolbar-en.js?1420968012\" type=\"text/javascript\">"
			+ "</script>"
			+ "<link href=\"REDMINE_URL/stylesheets/jstoolbar.css?1420968012\" media=\"screen\" rel=\"stylesheet\" type=\"text/css\" />"
			+ "<script src=\"REDMINE_URL/javascripts/context_menu.js?1420968012\" type=\"text/javascript\">"
			+ "</script>"
			+ "<link href=\"REDMINE_URL/stylesheets/context_menu.css?1420968012\" media=\"screen\" rel=\"stylesheet\" type=\"text/css\" />"
			+ "</head>"
			+ "<body>"
			+ "<textarea class=\"wiki-edit\" style=\"width:100%;height:90%\" cols=\"60\" id=\"issue_notes\" name=\"issue[notes]\" rows=\"10\">ISSUE_DESCRIPTION"
			+ "</textarea>"
			+ "<script type=\"text/javascript\">"
			+ "var wikiToolbar = new jsToolBar(document.getElementById('issue_notes'));"
			+ "wikiToolbar.setHelpLink('/help/en/wiki_syntax.html');"
			+ "wikiToolbar.draw();" + "</script>" + "</body>" + "</html>";

	var html = htmlTemplate.replace(/REDMINE_URL/g, config.url);
	html = html.replace("ISSUE_DESCRIPTION", issue.getDescription());

	var editor = new DescriptionHtmlEditor();
	editor.htmlContent = html;
	editor.elementId = "issue_notes";

	return editor;
}

function getShowIssueUrl(issueId) {
	return config.url + "/issues/" + issueId;
}

function getMsgFileType() {
	return config.msgFileType;
}

function createIssue(subject, description) {
	config.checkValid();

	subject = stripIssueIdFromMailSubject(subject);

	// strip RE:, Fwd:, AW:, WG: ...
	subject = stripReFwdFromSubject(subject);

	var iss = new Issue();

	iss.setSubject(subject);
	iss.setDescription(description);
	iss.setType(1); // Bug
	iss.setPriority(data.defaultPriority); // Normal priority
	iss.setState(1); // New issue
	iss.setAssignee(-1);
	
	

	var projects = getCategories(null);
	iss.setCategory(projects[0].getId());

	return iss;
};

function updateIssue(trackerIssue, progressCallback) {
	log.info("updateIssue(trackerIssue=" + trackerIssue + ", progressCallback="
			+ progressCallback);
	config.checkValid();

	var redmineIssue = {};
	toRedmineIssue(trackerIssue, redmineIssue, progressCallback);

	var issueParam = {
		issue : redmineIssue
	};
	var issueReturn = writeIssue(issueParam, progressCallback);

	// Convert redmineIssue to trackerIssue,
	// issueReturn is null when updating an exisiting issue.
	if (issueReturn) {
		var redmineIssue = issueReturn.issue;
		toTrackerIssue(redmineIssue, trackerIssue);
	}

	log.info(")updateIssue=" + trackerIssue);
	return trackerIssue;
};

function toRedmineIssue(trackerIssue, redmineIssue, progressCallback) {
	log.info("toRedmineIssue(trackerIssue=" + trackerIssue + ", redmineIssue="
			+ redmineIssue + ", progressCallback=" + progressCallback);

	redmineIssue.id = trackerIssue.getId();
	redmineIssue.project_id = parseInt(trackerIssue.getCategory());
	redmineIssue.tracker_id = parseInt(trackerIssue.getType());
	redmineIssue.status_id = parseInt(trackerIssue.getState());
	redmineIssue.priority_id = parseInt(trackerIssue.getPriority());
	redmineIssue.subject = "" + trackerIssue.getSubject();

	if (redmineIssue.id) {
		// Update issue: set notes
		redmineIssue.notes = "" + trackerIssue.getDescription();
	} else {
		// New issue: set description
		redmineIssue.description = "" + trackerIssue.getDescription();
	}

	if (trackerIssue.getAssignee().length() != 0) {
		var userId = parseInt(trackerIssue.getAssignee());
		if (userId >= 0) {
			redmineIssue.assigned_to_id = userId;
		}
	}

	if (trackerIssue.getMilestones().length) {
		redmineIssue.fixed_version_id = parseInt(trackerIssue.getMilestones()[0]);
	}

	redmineIssue.uploads = [];
	try {
		for (var i = 0; i < trackerIssue.getAttachments().size(); i++) {
			var trackerAttachment = trackerIssue.getAttachments().get(i);
			var pgUpload = null;
			if (progressCallback) {
				if (progressCallback.isCancelled())
					break;
				var str = "Upload attachment "
						+ trackerAttachment.getFileName();
				str += ", "
						+ makeAttachmentSizeString(trackerAttachment
								.getContentLength());
				pgUpload = progressCallback.createChild(str);
				pgUpload.setTotal(trackerAttachment.getContentLength());
			}
			redmineAttachment = writeAttachment(trackerAttachment, pgUpload);
			redmineIssue.uploads.push(redmineAttachment);
		}
	} catch (ex) {
		for (var i = 0; i < redmineIssue.uploads.length; i++) {
			var token = redmineIssue.uploads[i];
			try {
				deleteAttachment(token);
			} catch (ex2) {
			}
		}
	}

	log.info(")toRedmineIssue=" + redmineIssue);
	return redmineIssue;
}

function makeAttachmentSizeString(contentLength) {
	var dims = [ "Bytes", "KB", "MB", "GB", "TB" ];
	var dimIdx = 0;
	var c = contentLength;
	for (var i = 0; i < dims.length; i++) {
		var nb = c;
		c = Math.floor(c / 1000);
		if (c == 0) {
			dimIdx = i;
			break;
		}
	}
	return nb + dims[dimIdx];
}

function toTrackerIssue(redmineIssue, trackerIssue) {

	trackerIssue.setId(redmineIssue.id);

	trackerIssue.setCategory(redmineIssue.project.id);
	trackerIssue.setType(redmineIssue.tracker.id);
	trackerIssue.setState(redmineIssue.status.id);
	trackerIssue.setPriority(redmineIssue.priority.id);
	trackerIssue.setSubject(redmineIssue.subject);
	trackerIssue.setDescription(redmineIssue.description);

	// trackerIssue.setAssignee(redmineIssue.assigned_to.id);

	if (redmineIssue.fixed_version_id) {
		trackerIssue.setMilestones([ redmineIssue.fixed_version_id ]);
	}
}

function validateIssue(iss) {
	var ret = iss;
	return ret;
};

function extractIssueIdFromMailSubject(subject) {
	var issueId = "";
	var startTag = "[R-";
	var p = subject.indexOf(startTag);
	if (p >= 0) {
		var q = subject.indexOf("]", p);
		if (q >= 0) {
			issueId = subject.substring(p + startTag.length, q);
		}
	}
	return issueId;
};

function stripOneIssueIdFromMailSubject(subject) {
	log.info("stripOneIssueIdFromMailSubject(" + subject);
	var ret = subject;
	var startTag = "[R-";
	var p = subject.indexOf(startTag);
	if (p >= 0) {
		var q = subject.indexOf("]", p);
		if (q >= 0) {
			ret = subject.substring(q + 1).trim();
		}
	}
	log.info(")stripOneIssueIdFromMailSubject=" + ret);
	return ret;
};

function stripIssueIdFromMailSubject(subject) {
	var ret = stripOneIssueIdFromMailSubject(subject);
	while (ret != subject) {
		subject = ret;
		ret = stripOneIssueIdFromMailSubject(subject);
	}
	return ret;
}

function stripReFwdFromSubject(subject) {
	var p = "";
	while (p != subject) {
		p = subject;
		subject = stripFirstReFwdFromSubject(p);
	}
	return subject;
}

function stripFirstReFwdFromSubject(subject) {
	subject = subject.trim();
	var s = subject.toLowerCase();
	var p = s.indexOf(":");
	if (p < 4) {
		subject = subject.substring(p + 1);
	}
	return subject;
}

function injectIssueIdIntoMailSubject(subject, iss) {
	var ret = "[R-" + iss.getId() + "] ";
	ret += subject;
	return ret;
};

function writeAttachment(trackerAttachment, progressCallback) {
	log.info("writeAttachment(" + trackerAttachment + ", progressCallback="
			+ progressCallback);
	var content = trackerAttachment.getStream();

	var uploadResult = httpClient.upload("/uploads.json", content,
			trackerAttachment.getContentLength(), progressCallback);
	dump(uploadResult);

	trackerAttachment.setId(uploadResult.upload.token);

	var redmineAttachment = {};
	redmineAttachment.token = uploadResult.upload.token;
	redmineAttachment.filename = trackerAttachment.getFileName();
	redmineAttachment.content_type = trackerAttachment.getContentType();

	if (progressCallback) {
		progressCallback.setFinished();
	}

	log.info(")writeAttachment=" + redmineAttachment);
	return redmineAttachment;
};

initializePropertyClasses();
