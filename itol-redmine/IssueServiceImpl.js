/*
    Copyright (c) 2015 Wolfgang Imig
    
    This file is part of the library "JOA Issue Tracker for Microsoft Outlook".

    This file must be used according to the terms of   
      
      MIT License, http://opensource.org/licenses/MIT

 */

// TODO: Names of fields used in all projects:
// Add one line for each field in format ... "fieldname" : true,
var customFieldsInAllProjects = {
		"Text1 with leerzeichen" : true,
		"MultiList" : true,
};

// TODO: Field to project relationship.
// Add one line for each relation in format ... "fieldname in projectname" : true,
var mapCustomFieldsToProjects = {
		"field1 in project1" : true,
		"field2 in project1" : true,
		"field1 in project2" : true,
		"Float1 Fließkomma in project1" : true,
};


// TODO: Names of roles whose members cannot be assignees.
// Add one line for each role, that does NOT have checked the option 
// "Issues can be assigned to this role". 
// Format ... "rolename" : true, 
var dontAssignIssuesToThisRoles = {
     "ExampleRoleNameNotAssignee" : true,
};

/**
 * Maximum number of projects to be read. This value constraints the number of
 * combo box items in the UI.
 */
var MAX_PROJECTS = 1000;

/**
 * Maximum number of users per project. This value constraints the number of
 * combo box items in the UI.
 */
var MAX_USERS = 1000;

/**
 * Name and description of the ITOL configuration project.
 */
var ITOL_CONFIG_NAME = "Issue Tracker for Microsoft Outlook and Redmine Configuration";
var ITOL_CONFIG_DESC = "This project stores the configuration data for the "
		+ "Issue Tracker Addin for Microsoft Outlook and Redmine. " + "Last update was at:";
var ITOL_CONFIG_DESC_TAG_BEGIN = "<pre>ENCRYPTED_DATA_BEGIN\n";
var ITOL_CONFIG_DESC_TAG_END = "\nENCRYPTED_DATA_END</pre>";

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
var Attachment = Java.type("com.wilutions.itol.db.Attachment");
var IssueHtmlEditor = Java.type("com.wilutions.itol.db.IssueHtmlEditor");
var PasswordEncryption = Java.type("com.wilutions.itol.db.PasswordEncryption");
var Logger = Java.type("java.util.logging.Logger");
var Level = Java.type("java.util.logging.Level");
var log = Logger.getLogger("IssueServiceImpl.js");

var islfine = log.isLoggable(Level.FINE);
var islinfo = log.isLoggable(Level.INFO);
var islwarn = log.isLoggable(Level.WARNING);
var islsevere = log.isLoggable(Level.SEVERE);

/**
 * ddump JavaScript objects into the log file.
 * 
 * @param name
 *            String, used as title.
 * @param obj
 *            Object to be dumped.
 */
function ddump(name, obj) {
	if (log.isLoggable(Level.FINE)) {
		var str = JSON.stringify(obj, null, 2);
		log.log(Level.FINE, name + "=" + str);
	}
}
function idump(name, obj) {
	if (log.isLoggable(Level.INFO)) {
		var str = JSON.stringify(obj, null, 2);
		log.log(Level.INFO, name + "=" + str);
	}

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
	url : "URL to Redmine service, e.g. http://192.168.0.11",

	/**
	 * API key for authentication.
	 */
	apiKey : "... see \"Redmine / My account / API access key\" ... ",

	/**
	 * Attach mail encoded in this format.
	 */
	msgFileType : ".msg",

	/**
	 * ITOL configuration project. This project is used to store global
	 * configuration options for ITOL. When an administrative connection is
	 * established, the configuration options are updated. Non-administrative
	 * connections read the configuration.
	 */
	configProjectIdentifier : "itol-configuration",

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

	// Property IDs of issue properties
	PROPERTY_ID_START_DATE : "start_date",
	PROPERTY_ID_DUE_DATE : "due_date",
	PROPERTY_ID_ESTIMATED_HOURS : "estimated_hours",
	PROPERTY_ID_SPENT_HOURS : "spent_hours",
	PROPERTY_ID_DONE_RATIO : "done_ratio",
	PROPERTY_ID_ISSUE_CATEGORY : "category_id",
	PROPERTY_ID_FIXED_VERSION : "fixed_version_id",

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
		new Property(this.PROPERTY_ID_URL, this.url), new Property(this.PROPERTY_ID_API_KEY, this.apiKey)
		];
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
		if (!this.valid) { throw new IOException(
				"Initialization failed. Check configuration properties on backstage view."); }
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
		var response = JHttpClient.send(destUrl, method, headers, content, progressCallback ? progressCallback : null);
		
		if (response.status < 200 || response.status > 299) {
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
		}
		else if (config.userName) {
			var auth = JHttpClient.makeBasicAuthenticationHeader(config.userName, config.userPwd);
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
	 * this.projects[.].memberships contains an array of project memberships
	 * (assignees). this.projects[.].versions contains an array of versions
	 * (milestones). this.projects[.].custom_fields contains an array of custom
	 * fields.
	 */
	projects : {},

	/**
	 * Current user.
	 */
	user : {},

	/**
	 * Is current user an administrator. This is detected in
	 * readOrUpdateConfigurationProject()
	 */
	isAdmin : false,

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

	/**
	 * Custom fields definitions.
	 */
	custom_fields : [],
	
	/**
	 * Map of roles, Key: Role ID, Value : role object.
	 */
	roles : {},

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
		if (elm == arr[j]) { return j; }
	}
	return -1;
}

function readProjects(data) {
	if (islfine) log.log(Level.FINE, "readProjects(");

	var projectCount = 0;
	var offset = 0;
	while (projectCount < MAX_PROJECTS) {

		var projectsResponse = httpClient.get("/projects.json?" + "include=trackers,issue_categories,enabled_modules&"
				+ "offset=" + offset + "&limit=100");
		var arrOfProjects = projectsResponse.projects;
		if (arrOfProjects.length == 0) {
			break;
		}

		for (var i = 0; i < arrOfProjects.length && projectCount < MAX_PROJECTS; i++) {
			var project = arrOfProjects[i];
			data.projects[project.id] = project;
			ddump("project", project);
			if (islinfo) log.log(Level.INFO, "project.id=" + project.id + ", .name=" + project.name);
			projectCount++;
		}

		offset += arrOfProjects.length;
	}

	// Add parent project names to project names.
	for ( var projectId in data.projects) {
		var project = data.projects[projectId];
		var name = project.name;
		var p = project;
		while (p && p.parent) {
			name = p.parent.name + " » " + name;
			p = data.projects[p.parent.id];
		}
		project.name = name;
		if (islfine) log.log(Level.FINE, "project.id=" + project.id + ", name=" + project.name);
	}

	if (islfine) log.log(Level.FINE, ")readProjects");
};

function readCurrentUser(data) {
	if (islfine) log.log(Level.FINE, "readCurrentUser(");
	data.user = httpClient.get("/users/current.json?include=memberships,groups").user;
	if (islinfo) log.log(Level.INFO, "me.id=" + data.user.id + ", .name=" + data.user.name);
	ddump("user ", data.user);
	if (islfine) log.log(Level.FINE, ")readCurrentUser");
};

function readProjectVersions(project) {
	if (islfine) log.log(Level.FINE, "readProjectVersions(project.id=" + project.id);
	project.versions = [];
	var arrOfVersions = httpClient.get("/projects/" + project.id + "/versions.json").versions;
	for (var j = 0; j < arrOfVersions.length; j++) {
		var version = arrOfVersions[j];
		project.versions.push(version);
		if (islinfo) log.log(Level.INFO, "version: project.id=" + project.id + ", version.id=" + version.id
				+ ", .name=" + version.name);
	}
	ddump("project.versions", project.versions);
	if (islfine) log.log(Level.FINE, ")readProjectVersions");
	return project.versions;
}

function readProjectIssueCategories(project) {
	if (islfine) log.log(Level.FINE, "readProjectIssueCategories(project.id=" + project.id);
	project.issue_categories = [];
	var arr = httpClient.get("/projects/" + project.id + "/issue_categories.json").issue_categories;
	for (var j = 0; j < arr.length; j++) {
		var category = arr[j];
		project.issue_categories.push(category);
		if (islinfo) log.log(Level.INFO, "categorie: project.id=" + project.id + ", category.id=" + category.id
				+ ", .name=" + category.name);
	}
	ddump("project.issue_categories", project.issue_categories);
	if (islfine) log.log(Level.FINE, ")readProjectIssueCategories");
	return project.issue_categories;
}

function readProjectMembers(project) {
	if (islfine) log.log(Level.FINE, "readProjectMembers(project.id=" + project.id);
	project.memberships = [];
	var offset = 0;
	while (project.memberships.length < MAX_USERS) {

		var arrOfMemberships = httpClient.get("/projects/" + project.id + "/memberships.json?" + "offset=" + offset
				+ "&limit=100").memberships;

		if (arrOfMemberships.length == 0) {
			break;
		}

		ddump("arrOfMemberships", arrOfMemberships);

		for (var i = 0; i < arrOfMemberships.length; i++) {
			var membership = arrOfMemberships[i];
			// see issue #9, user might be missing
			if (membership.user) {
				project.memberships.push(membership);
				if (islinfo) log.log(Level.INFO, "member: project.id=" + project.id + ", user.id=" + membership.user.id
						+ ", .name=" + membership.user.name);
			}
		}

		offset += arrOfMemberships.length;
	}
	ddump("project.memberships", project.memberships);
	if (islfine) log.log(Level.FINE, ")readProjectMembers");
}

function writeIssue(issueParam, progressCallback) {
	if (islfine) log.log(Level.FINE, "writeIssue(");
	ddump("send", issueParam);
	var ret = null;

	var pgIssue = null;
	if (progressCallback) {
		if (progressCallback.isCancelled()) return;
		pgIssue = progressCallback.createChild("Write issue");
	}

	var issueId = issueParam.issue.id;
	var isUpdate = !!issueId;
	if (islfine) log.log(Level.FINE, "issueId=" + issueId + ", isUpdate=" + isUpdate);

	if (isUpdate) {
		ret = httpClient.put("/issues/" + issueId + ".json", issueParam, pgIssue);
	}
	else {
		ret = httpClient.post("/issues.json", issueParam, pgIssue);
	}

	ddump("recv", ret);
	if (islfine) log.log(Level.FINE, ")writeIssue=");
	return ret;
}

/**
 * Read or update ITOL configuration. The configuration data is stored as an
 * encrypted blob in the description of the ITOL confguration project.
 */
function readOrUpdateConfigurationProject() {
	if (islfine) log.log(Level.FINE, "readOrUpdateConfigurationProject(");

	// Read configuration project.
	var configProject = null;
	try {
		var response = httpClient.get("/projects/" + config.configProjectIdentifier + ".json");
		configProject = response.project;
		idump("configProject", configProject);

		// Encrypt configuration data
		var configDesc = configProject.description;
		var p = configDesc.indexOf(ITOL_CONFIG_DESC_TAG_BEGIN);
		if (p >= 0) {
			configDesc = configDesc.substring(p + ITOL_CONFIG_DESC_TAG_BEGIN.length);
		}
		p = configDesc.indexOf(ITOL_CONFIG_DESC_TAG_END);
		if (p >= 0) {
			configDesc = configDesc.substring(0, p);
		}
		configDesc = PasswordEncryption.decrypt(configDesc);
		var configData = JSON.parse(configDesc);
		data.custom_fields = configData.custom_fields;
		ddump("data.custom_fields", data.custom_fields);

		// Create property classes for custom fields
		var propertyClasses = getPropertyClasses();
		for (var i = 0; i < data.custom_fields.length; i++) {
			var cfield = data.custom_fields[i];
			var pclass = makePropertyClassForCustomField(cfield);
			if (pclass) {
				propertyClasses.add(pclass);
			}
		}

	}
	catch (ex) {
		if (islfine) log.log(Level.FINE, "Configuration project not found, try to crate it. ", ex);
	}

	// Try to update or create config project
	var isNew = !configProject;
	var customFields = null;
	try {

		// Read custom fields.
		// Only admins are allowed to. Ordinary users will jump to catch(ex)
		customFields = readCustomFields(data);

		// Put custom fields into a configuration object.
		// Convert it to JSON.
		var configData = {
			"custom_fields" : customFields
		};
		var configDesc = JSON.stringify(configData);

		// Encrypt the configuration object and wrap it into
		// BEGIN and END.
		configDesc = PasswordEncryption.encrypt(configDesc);
		configDesc = ITOL_CONFIG_DESC + " " + (new Date()).toISOString() + ". " + ITOL_CONFIG_DESC_TAG_BEGIN
				+ configDesc + ITOL_CONFIG_DESC_TAG_END;

		// Create a project object and assign the encrypted configuraion
		// as project description.
		configProject = configProject || {
			"name" : ITOL_CONFIG_NAME,
			"identifier" : config.configProjectIdentifier,
			"is_public" : true,
			"enabled_module_names" : []
		};
		configProject.description = configDesc;
		ddump("update configProject", configProject);

		// Create or update the project
		var projectRequest = {
			"project" : configProject
		};
		if (isNew) {
			ret = httpClient.post("/projects.json", projectRequest);
		}
		else {
			ret = httpClient.put("/projects/" + config.configProjectIdentifier + ".json", projectRequest);
		}

		// Memorize that current user is an administrator
		data.isAdmin = true;

	}
	catch (ex) {
		if (islfine) log.log(Level.FINE, "Failed to read custom fields, I am not an administrator.");

		// Memorize that current user is not an administrator
		data.isAdmin = false;

		if (isNew) {
			var msg = "Cannot read ITOL configuration project. "
					+ "The first login has to be made with an administrator account. "
					+ "Thereby, the configuration project is created. Details: " + ex.toString();
			log.log(Level.WARNING, msg);
			// throw new IOException(msg);
		}
	}

	if (islfine) log.log(Level.FINE, "data.isAdmin=" + data.isAdmin);

	if (islfine) log.log(Level.FINE, ")readOrUpdateConfigurationProject");
}

function initialize() {
	if (islfine) log.log(Level.FINE, "initialize(");
	config.valid = false;

	if (islinfo) {
		log.log(Level.INFO, "config.url=" + config.url);
		log.log(Level.INFO, "config.apiKey set=" + (config.apiKey && config.apiKey.length));
		log.log(Level.INFO, "config.msgFileType=" + config.msgFileType);
	}

	data.clear();
	
	if (!config.url || !config.url.toLowerCase().startsWith("http")) {
		throw new IOException("Invalid Redmine URL"); 
	}
	
	if (islinfo) log.log(Level.INFO, "readCurrentUser");
	readCurrentUser(data);
	
	if (islinfo) log.log(Level.INFO, "readOrUpdateConfigurationProject");
	readOrUpdateConfigurationProject();

	if (islinfo) log.log(Level.INFO, "readProjects");
	readProjects(data);

	if (islinfo) log.log(Level.INFO, "readTrackers");
	readTrackers(data);

	if (islinfo) log.log(Level.INFO, "readPriorities");
	readPriorities(data);

	if (islinfo) log.log(Level.INFO, "readStatuses");
	readStatuses(data);
	
	if (islinfo) log.log(Level.INFO, "readRoles");
	readRoles(data);

	config.valid = true;
	if (islinfo) log.log(Level.INFO, "initialized");

	if (islfine) log.log(Level.FINE, ")initialize");
}

function readTrackers(data) {
	if (islfine) log.log(Level.FINE, "readTrackers(");
	var trackersResponse = httpClient.get("/trackers.json");
	ddump("trackersResponse", trackersResponse);
	for (var i = 0; i < trackersResponse.trackers.length; i++) {
		var tracker = trackersResponse.trackers[i];
		if (islinfo) log.log(Level.INFO, "tracker.id=" + tracker.id + ", .name=" + tracker.name);
		data.trackers.push(new IdName(tracker.id, tracker.name));
	}

	if (islfine) log.log(Level.FINE, ")readTrackers");
}

function readPriorities(data) {
	if (islfine) log.log(Level.FINE, "readPriorities(");
	var prioritiesResponse = httpClient.get("/enumerations/issue_priorities.json");
	ddump("prioritiesResponse", prioritiesResponse);
	data.priorities = [];
	for (var i = 0; i < prioritiesResponse.issue_priorities.length; i++) {
		var priority = prioritiesResponse.issue_priorities[i];
		if (islinfo) log.log(Level.INFO, "priority.id=" + priority.id + ", .name=" + priority.name);
		data.priorities.push(new IdName(priority.id, priority.name));
		if (priority.is_default) {
			data.defaultPriority = priority.id;
		}
	}
	if (islfine) log.log(Level.FINE, ")readPriorities");

}

function readStatuses(data) {
	if (islfine) log.log(Level.FINE, "readStatuses(");
	var statusesResponse = httpClient.get("/issue_statuses.json");
	ddump("statusesResponse", statusesResponse);
	data.statuses = [];
	for (var i = 0; i < statusesResponse.issue_statuses.length; i++) {
		var status = statusesResponse.issue_statuses[i];
		if (islinfo) log.log(Level.INFO, "status.id=" + status.id + ", .name=" + status.name);
		data.statuses.push(new IdName(status.id, status.name));
	}
	if (!data.statuses) {
		data.statuses = [ new IdName(1, "New issue") ];
	}
	if (islfine) log.log(Level.FINE, ")readStatuses");
}

function readCustomFields(data) {
	if (islfine) log.log(Level.FINE, "readCustomFields(");
	var response = httpClient.get("/custom_fields.json");
	ddump("response", response);
	data.custom_fields = [];
	var propertyClasses = getPropertyClasses();

	for (var i = 0; i < response.custom_fields.length; i++) {
		var cfield = response.custom_fields[i];
		if (cfield.customized_type == "issue") {
			var pclass = makePropertyClassForCustomField(cfield);
			if (pclass) {
				propertyClasses.add(pclass);
				data.custom_fields.push(cfield);
				if (islinfo) log.log(Level.INFO, "custom_field.id=" + cfield.id + ", .name=" + cfield.name);
			}
		}
	}

	if (islfine) log.log(Level.FINE, ")readCustomFields");
	return data.custom_fields;
}

function readRoles(data) {
	if (islfine) log.log(Level.FINE, "readRoles(");
	var rolesResponse = httpClient.get("/roles.json");
	ddump("rolesResponse", rolesResponse);
	data.roles = {};
	for (var i = 0; i < rolesResponse.roles.length; i++) {
		var role = rolesResponse.roles[i];
		
		role = httpClient.get("/roles/" + role.id + ".json").role;
		data.roles[role.id] = role;
		
		role.canAssigneIssuesToThisRole = !dontAssignIssuesToThisRoles[role.name];
		
		if (islinfo) log.log(Level.INFO, "role.id=" + role.id + ", .name=" + role.name + ", .canAssigneIssuesToThisRole=" + role.canAssigneIssuesToThisRole);
	}
	if (islfine) log.log(Level.FINE, ")readRoles");
}

function makeCustomFieldPropertyId(cfield) {
	return "custom_field_" + cfield.id;
}

function isCustomFieldPropertyId(propId) {
	return propId.indexOf("custom_field_") == 0;
}

function makePropertyType(cfield) {
	var type = null;
	switch (cfield.field_format) {
	case "bool":
		type = PropertyClass.TYPE_BOOL;
		break;
	case "date":
		type = PropertyClass.TYPE_ISO_DATE;
		break;
	case "list":
		type = cfield.multiple ? PropertyClass.TYPE_STRING_LIST : PropertyClass.TYPE_STRING;
		break;
	case "string":
		type = PropertyClass.TYPE_STRING;
		break;

	case "float":
	case "int":
	case "text":
	case "link":
		type = PropertyClass.TYPE_STRING;
		break;

	// unsupported:
	case "version":
	case "user":
		type = null;
		break;
	}
	return type;
}

function makePropertyClassForCustomField(cfield) {
	if (islfine) log.log(Level.FINE, "makePropertyClassForCustomField(" + cfield.name);
	var ret = null;

	var type = makePropertyType(cfield);
	if (islfine) log.log(Level.FINE, "type=" + type);
	if (type) {

		// Define Property ID for this custom field.
		// That ID is also added to the cfield object to be able to use
		// it in getPropertyDisplayOrder().
		var id = cfield.propertyId = makeCustomFieldPropertyId(cfield);
		if (islfine) log.log(Level.FINE, "id=" + id);

		var name = cfield.name;
		var defaultValue = cfield.default_value ? cfield.default_value : null;
		if (islfine) log.log(Level.FINE, "defaultValue=" + defaultValue);

		var selectList = [];
		if (cfield.possible_values) {
			for (var i = 0; i < cfield.possible_values.length; i++) {
				var pvalue = cfield.possible_values[i];
				selectList.push(new IdName("" + pvalue.value));
			}
		}
		if (islfine) log.log(Level.FINE, "selectList#=" + selectList.length);

		ret = new PropertyClass(type, id, name, defaultValue, selectList);
	}
	else {
		if (islwarn) log.log(Level.WARNING, "Unsupported: field.name=" + cfield.name + ", field_format="
				+ cfield.field_format);
	}

	if (islfine) log.log(Level.FINE, ")makePropertyClassForCustomField=" + ret);
	return ret;
}

/**
 * Initialize property classes. In config.propertyClasses, a type definition is
 * stored for each property. The UI interprets the type definition and assigns
 * an appropriate UI control.
 */
function initializePropertyClasses() {

	var propertyClasses = PropertyClasses.getDefault();

	// ---------------------------
	// Configuration properties

	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_URL, "Redmine URL");
	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_API_KEY, "API key");
	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_PROJECT_NAMES,
			"Projects (optional, comma separated)");
	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_MSG_FILE_TYPE, "Attach mail as");

	// propertyClass.add(PropertyClass.TYPE_STRING,
	// config.PROPERTY_ID_MY_CONFIG_PROPERTY,
	// "my_config_property label"

	// ---------------------------
	// Property classes for issues

	propertyClasses.add(PropertyClass.TYPE_ISO_DATE, config.PROPERTY_ID_START_DATE, "Start Date", new Date()
			.toISOString());

	propertyClasses.add(PropertyClass.TYPE_ISO_DATE, config.PROPERTY_ID_DUE_DATE, "Due Date");

	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_ESTIMATED_HOURS, "Estimated hours");

	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_SPENT_HOURS, "Spent hours");

	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_DONE_RATIO, "% Done", "0", [
			new IdName("0", "0 %"), new IdName("10", "10 %"), new IdName("20", "20 %"), new IdName("30", "30 %"),
			new IdName("40", "40 %"), new IdName("50", "50 %"), new IdName("60", "60 %"), new IdName("70", "70 %"),
			new IdName("80", "80 %"), new IdName("90", "90 %"), new IdName("100", "100 %"), ]);

	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_ISSUE_CATEGORY, "Category", "");

	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_FIXED_VERSION, "Version", "");

	// -----------------------------------------------
	// Initialize select list for some issue properties

	var propMsgFileType = propertyClasses.get(config.PROPERTY_ID_MSG_FILE_TYPE);
	propMsgFileType.setSelectList([ new IdName(".msg", "Outlook (.msg)"), new IdName(".mhtml", "MIME HTML (.mhtml)"),
			new IdName(".rtf", "Rich Text Format (.rtf)") ]);

	config.propertyClasses = propertyClasses;
}

function getConfig() {
	return config.toProperties();
}

function setConfig(configProperties) {
	if (configProperties) {
		config.fromProperties(configProperties);
	}
};

function getPropertyClasses() {
	return config.propertyClasses;
};

function getPropertyClass(propertyId, issue) {
	var ret = getPropertyClasses().getCopy(propertyId);
	idump("propertyClass " + propertyId, ret);
	if (!ret) return null;
	switch (propertyId) {
	case Property.ISSUE_TYPE:
		ret.selectList = getIssueTypes(issue);
		break;
	case Property.PRIORITY:
		ret.selectList = data.priorities;
		break;
	case Property.PROJECT:
		ret.selectList = getProjectsIdNames(issue);
		break;
	case Property.ASSIGNEE:
		ret.selectList = getAssignees(issue);
		break;
	case Property.STATUS:
		ret.selectList = data.statuses;
		break;
	case config.PROPERTY_ID_ISSUE_CATEGORY:
		ret.selectList = getCategories(issue);
		break;
	case config.PROPERTY_ID_FIXED_VERSION:
		ret.selectList = getVersions(issue);
		break;
	default:
		break;
	}
	return ret;
}

function getPropertyAutoCompletion(propertyId, issue, filter) {
	return null;
}

function getIssueTypes(issue) {
	var ret = [];
	var projectId = issue ? issue.getProject() : -1;
	var project = data.projects[projectId];
	if (islfine) log.log(Level.FINE, "project=" + project);
	if (project) {
		if (!project.trackers) {
			var projectResponse = httpClient.get("/projects/" + projectId + ".json?include=trackers");
			project.trackers = projectResponse.project.trackers;
			ddump("Added project.trackers", project.trackers);
		}
		if (project.trackers) {
			for (var i = 0; i < project.trackers.length; i++) {
				var idn = new IdName(project.trackers[i].id, project.trackers[i].name);
				ret.push(idn);
			}
		}
	}
	return ret;
};

function getProjectsIdNames(issue) {
	if (islfine) log.log(Level.FINE, "getProjectIdNames(" + issue);
	var ret = [];
	// Project association of an existing issue cannot be changed.
	// This is my experience, although the Redmine documentation
	// says that it is possible.
	if (issue && issue.id && issue.id.length) {
		var project = getIssueProject(issue);
		ret = [ new IdName(project.id, project.name) ];
	}
	else {
		ret = getAllProjectsIdNamesWithIssueTracking();
	}

	if (islfine) log.log(Level.FINE, ")getProjectIdNames=#" + ret.length);
	return ret;
}

function getAllProjectsIdNamesWithIssueTracking() {
	if (islfine) log.log(Level.FINE, "getAllProjectsIdNamesWithIssueTracking(");

	var ret = [];

	// Collect project IDs an names into IdName array.
	for ( var projectId in data.projects) {
		var project = data.projects[projectId];
		if (islfine) log.log(Level.FINE, "project.id=" + projectId + " .enabled_modules="
				+ JSON.stringify(project.enabled_modules));

		// Skip projects without issue tracking
		var isIssueProject = !project.enabled_modules;
		if (project.enabled_modules) {
			for (var m = 0; !isIssueProject && m < project.enabled_modules.length; m++) {
				var module = project.enabled_modules[m];
				isIssueProject = module.name == "issue_tracking";
			}
		}
		if (!isIssueProject) {
			if (islinfo) log.log(Level.INFO, "project.id=" + projectId + ", .name=" + project.name
					+ " without issue tracking.");
			continue;
		}

		var idn = new IdName(project.id, project.name);
		if (islfine) log.log(Level.FINE, "add project.id=" + idn.id + ", name=" + idn.name);
		ret.push(idn);
	}

	// Sort by name
	ret.sort(compareIdNameByName);

	if (islfine) log.log(Level.FINE, ")getAllProjectsIdNamesWithIssueTracking");
	return ret;
};

function compareIdNameByName(lhs, rhs) {
	return lhs.name.compareTo(rhs.name);
}

function getIssueProject(issue) {
	if (islfine) log.log(Level.FINE, "getIssueProject(");
	var projectId = issue ? issue.getProject() : 0;
	var project = data.projects[projectId];
	if (islfine) log.log(Level.FINE, ")getIssueProject=" + project);
	return project;
}

function getVersions(issue) {
	var ret = [];
	var project = getIssueProject(issue);
	if (islfine) log.log(Level.FINE, "project=" + project);
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

function getCategories(issue) {
	var ret = [];
	var project = getIssueProject(issue);
	if (islfine) log.log(Level.FINE, "project=" + project);
	if (project) {

		if (typeof project.issue_categories === "undefined") {
			readProjectIssueCategories(project);
		}

		for (var i = 0; i < project.issue_categories.length; i++) {
			var cat = project.issue_categories[i];
			ret.push(new IdName(cat.id, cat.name));
		}
	}
	return ret;
};

function getIssueProjectMemberships(issue) {
	if (islfine) log.log(Level.FINE, "getIssueProjectMemberships(");
	var ret = [];
	var project = getIssueProject(issue);
	if (islfine) log.log(Level.FINE, "project=" + project);
	if (project) {

		if (typeof project.memberships === "undefined") {
			readProjectMembers(project);
		}

		ret = project.memberships;
	}
	if (islfine) log.log(Level.FINE, ")getIssueProjectMemberships=#" + ret.length);
	return ret;
}

function getAssignees(issue) {
	if (islfine) log.log(Level.FINE, "getAssignees(" + issue);
	
	var ret = [ new IdName(-1, "Unassigned") ];
	
	var memberships = getIssueProjectMemberships(issue);
	for (var i = 0; i < memberships.length; i++) {
		
		ddump("project.memberships[" + i + "]", memberships[i]);
		
		var canAssignIssues = true;
		var roles = memberships[i].roles;
		if (roles) {
			for (var j = 0; j < roles.length && canAssignIssues; j++) {
				var roleId = roles[j].id;
				var role = data.roles[roleId];
				if (islfine) log.log(Level.FINE, "role.name=" + role.name + ", canAssigneIssuesToThisRole=" + role.canAssigneIssuesToThisRole);
				
				canAssignIssues &= role.canAssigneIssuesToThisRole;
			}
		}
		
		if (canAssignIssues) {
			var member = memberships[i].user;
			ret.push(new IdName(member.id, member.name));
			if (islfine) log.log(Level.FINE, "add member=" + member.name);
		}
	}
	
	if (islfine) log.log(Level.FINE, ")getAssignees=" + ret);
	return ret;
};

function getCurrentUser() {
	return data.user ? new IdName(parseInt(data.user.id), data.user.firstname + " " + data.user.lastname) : new IdName(
			0, "");
};

function getDescriptionTextEditor(issue) {
	return null;
}

function getHtmlEditor(issue, propertyId) {
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
			+ "<textarea class=\"wiki-edit\" style=\"width:100%;height:90%\" cols=\"60\" id=\"issue_notes\" name=\"issue[notes]\" rows=\"10\">"
			+ "ISSUE_DESCRIPTION"
			+ "</textarea>" + "<script type=\"text/javascript\">"
			+ "var wikiToolbar = new jsToolBar(document.getElementById('issue_notes'));"
			+ "wikiToolbar.setHelpLink('/help/en/wiki_syntax.html');" + "wikiToolbar.draw();" + "</script>" + "</body>"
			+ "</html>";

	var html = htmlTemplate.replace(/REDMINE_URL/g, config.url);
	var text = "";
	if (issue) {
		text = issue.getPropertyString(propertyId, "");
	}
	html = html.replace("ISSUE_DESCRIPTION", text);

	var editor = new IssueHtmlEditor();
	editor.htmlContent = html;
	editor.elementId = "issue_notes";

	return editor;
}

function getShowIssueUrl(issueId) {
	return getIssueHistoryUrl(issueId);
}

function getPropertyDisplayOrder(issue) {
	if (islfine) log.log(Level.FINE, "getPropertyDisplayOrder(");
	var propertyIds = [ Property.ASSIGNEE ];

	var categories = getCategories(issue);
	if (categories && categories.length) {
		if (islfine) log.log(Level.FINE, "has categories");
		propertyIds.push(config.PROPERTY_ID_ISSUE_CATEGORY);
	}

	var versions = getVersions(issue);
	if (versions && versions.length) {
		if (islfine) log.log(Level.FINE, "has versions");
		propertyIds.push(config.PROPERTY_ID_FIXED_VERSION);
	}

	propertyIds.push(config.PROPERTY_ID_START_DATE, config.PROPERTY_ID_DUE_DATE, config.PROPERTY_ID_ESTIMATED_HOURS,
			config.PROPERTY_ID_DONE_RATIO);

	// Add custom fields appropriate for the issue
	if (data.custom_fields) {
		for (var i = 0; i < data.custom_fields.length; i++) {
			var cfield = data.custom_fields[i];
			if (!isCustomFieldForProject(cfield, issue)) continue;
			if (isCustomFieldForIssueType(cfield, issue)) {
				if (isCustomFieldForCurrentUser(cfield, issue)) {
					propertyIds.push(cfield.propertyId);
					if (islfine) log.log(Level.FINE, "add custom field=" + cfield.name);
				}
			}
		}
	}

	if (islfine) log.log(Level.FINE, ")getPropertyDisplayOrder=" + propertyIds);
	return propertyIds;
}

function isCustomFieldForProject(cfield, issue) {
	var ret = customFieldsInAllProjects[cfield.name];
	if (!ret) {
		var project = getIssueProject(issue);
		if (project) {
			ret = mapCustomFieldsToProjects[cfield.name + " in " + project.name];
		}
	}
	return ret;
}

function isCustomFieldForIssueType(cfield, issue) {
	if (islfine) log.log(Level.FINE, "isCustomFieldForIssueType(" + cfield.name + ", issueType=" + issue.getType());
	var ret = false;
	if (cfield.trackers) {
		for (var t = 0; !ret && t < cfield.trackers.length; t++) {
			ret = cfield.trackers[t].id == issue.getType();
		}
	}
	if (islfine) log.log(Level.FINE, ")isCustomFieldForIssueType=" + ret);
	return ret;
}

function isCustomFieldForCurrentUser(cfield, issue) {
	if (islfine) log.log(Level.FINE, "isCustomFieldForCurrentUser(" + cfield.name);
	var ret = false;

	if (!data.isAdmin && cfield.roles && cfield.roles.length && data.user) {

		// Field is available for users with this roles:
		var fieldRoleIds = [];
		for (var i = 0; i < cfield.roles.length; i++) {
			fieldRoleIds.push(cfield.roles[i].id);
		}
		if (islfine) log.log(Level.FINE, "fieldRoleIds=" + JSON.stringify(fieldRoleIds));

		// The roles the current user plays in the issue's project
		var roles = getCurrentUsersRolesForIssueProject(issue);

		// Intersect field and user roles.
		for (var j = 0; !ret && j < roles.length; j++) {
			ret = fieldRoleIds.indexOf(roles[j].id);
			if (ret) {
				if (islfine) log.log(Level.FINE, "found role " + roles[j].id);
			}
		}

	}
	else {
		ret = true;
	}

	if (islfine) log.log(Level.FINE, ")isCustomFieldForCurrentUser=" + ret);
	return ret;
}

function getCurrentUsersRolesForIssueProject(issue) {
	if (islfine) log.log(Level.FINE, "getCurrentUsersRolesInProject(");
	var roles = [];
	var memberships = getIssueProjectMemberships(issue);
	for (var i = 0; i < memberships.length; i++) {
		var user = memberships[i].user;
		if (user.id == data.user.id) {
			roles = memberships[i].roles;
		}
	}
	if (islfine) log.log(Level.FINE, ")getCurrentUsersRolesInProject=" + JSON.stringify(roles));
	return roles;
}

function getDefaultIssueAsString(issue) {
	var defaultProps = {};
	if (issue) {
		defaultProps.project = issue.project;
		defaultProps.type = issue.type;
		defaultProps.assignee = issue.assignee;
	}
	else {
		defaultProps = makeDefaultProperties("");
	}

	return JSON.stringify(defaultProps);
}

function makeDefaultProperties(defaultIssueAsString) {
	var defaultProps = {};
	if (defaultIssueAsString && defaultIssueAsString.length()) {
		defaultProps = JSON.parse(defaultIssueAsString);
	}
	else {
		var projects = getProjectsIdNames(null);
		if (projects && projects.length) {
			defaultProps.project = projects[0].getId();
		}
		defaultProps.type = 1;
		defaultProps.assignee = -1; // unassigned
	}
	return defaultProps;
}

function createIssue(subject, description, defaultIssueAsString) {
	if (islfine) log.log(Level.FINE, "createIssue(");

	config.checkValid();

	var issue = new Issue();
	var defaultProps = makeDefaultProperties(defaultIssueAsString);
	ddump("defaultProps", defaultProps);

	issue.setPriority(data.defaultPriority); // Normal priority
	issue.setStatus(1); // New issue

	issue.setProject(defaultProps.project);
	issue.setType(defaultProps.type);
	issue.setAssignee(defaultProps.assignee);

	issue.setSubject(subject);
	issue.setDescription(description);

	var propIds = getPropertyDisplayOrder(issue);
	ddump("propIds", propIds);

	for (var i = 0; i < propIds.length; i++) {
		var pclass = getPropertyClass(propIds[i], issue);
		if (pclass) {
			var value = pclass.getDefaultValue();
			if (value) {
				issue.setPropertyValue(propIds[i], value);
			}
		}
	}

	if (islfine) log.log(Level.FINE, ")createIssue");
	return issue;
};

function updateIssue(trackerIssue, modifiedProperties, progressCallback) {
	if (islfine) log.log(Level.FINE, "updateIssue(trackerIssue=" + trackerIssue + ", progressCallback="
			+ progressCallback);
	config.checkValid();

	var redmineIssue = {};
	toRedmineIssue(trackerIssue, modifiedProperties, redmineIssue, progressCallback);

	var issueParam = {
		issue : redmineIssue
	};
	var issueReturn = writeIssue(issueParam, progressCallback);

	// New or updated issue ID. (issueReturn is null for an updated issue)
	var issueId = issueReturn ? issueReturn.issue.id : trackerIssue.id;

	// Read the created or updated issue.
	// This returns also the updated attachment URLs.
	trackerIssue = readIssue(issueId);

	if (islfine) log.log(Level.FINE, ")updateIssue=" + trackerIssue);
	return trackerIssue;
};

function toRedmineIssue(trackerIssue, modifiedProperties, redmineIssue, progressCallback) {
	if (islfine) log.log(Level.FINE, "toRedmineIssue(trackerIssue=" + trackerIssue + ", redmineIssue=" + redmineIssue
			+ ", progressCallback=" + progressCallback);

	redmineIssue.id = trackerIssue.getId();
	redmineIssue.project_id = parseInt(trackerIssue.getProject());
	redmineIssue.tracker_id = parseInt(trackerIssue.getType());
	redmineIssue.status_id = parseInt(trackerIssue.getStatus());
	redmineIssue.priority_id = parseInt(trackerIssue.getPriority());
	redmineIssue.subject = "" + trackerIssue.getSubject();
	redmineIssue.description = "" + trackerIssue.getDescription();
	redmineIssue.notes = "" + trackerIssue.getPropertyString(Property.NOTES, "");

	// Assignee
	redmineIssue.assigned_to_id = "";
	if (trackerIssue.getAssignee().length() != 0) {
		var userId = parseInt(trackerIssue.getAssignee());
		if (userId >= 0) {
			redmineIssue.assigned_to_id = userId;
		}
	}

	// Redmine specific properties
	var propertyIds = [ config.PROPERTY_ID_ISSUE_CATEGORY, config.PROPERTY_ID_FIXED_VERSION,
			config.PROPERTY_ID_START_DATE, config.PROPERTY_ID_DUE_DATE, config.PROPERTY_ID_ESTIMATED_HOURS,
			config.PROPERTY_ID_DONE_RATIO ];
	for (var i = 0; i < propertyIds.length; i++) {
		var propId = propertyIds[i];
		var propValue = getIssuePropertyValue(trackerIssue, propId);
		if (islfine) log.log(Level.FINE, "propId=" + propId + ", propValue=" + propValue);
		redmineIssue[propId] = propValue;
	}

	// Custom properties
	if (data.custom_fields && data.custom_fields.length) {
		redmineIssue.custom_fields = [];
		for (var i = 0; i < data.custom_fields.length; i++) {
			var cfield = data.custom_fields[i];
			var propId = makeCustomFieldPropertyId(cfield);
			var propValue = getIssuePropertyValue(trackerIssue, propId);
			if (islfine) log.log(Level.FINE, "custom propId=" + propId + ", propValue=" + propValue);
			setRedmineIssueCustomField(redmineIssue, cfield.id, propId, propValue);
		}
	}

	redmineIssue.uploads = [];
	try {
		for (var i = 0; i < trackerIssue.getAttachments().size(); i++) {
			var trackerAttachment = trackerIssue.getAttachments().get(i);
			if (islfine) log.log(Level.FINE, "trackerAttachment=" + trackerAttachment);

			// Upload only new attachments
			if (trackerAttachment.getId().isEmpty()) {

				// Create inner progress callback
				var pgUpload = null;
				if (progressCallback) {
					if (progressCallback.isCancelled()) break;
					var str = "Upload attachment " + trackerAttachment.getFileName();
					str += ", " + makeAttachmentSizeString(trackerAttachment.getContentLength());
					pgUpload = progressCallback.createChild(str);
					pgUpload.setTotal(trackerAttachment.getContentLength());
				}

				// Upload
				redmineAttachment = writeAttachment(trackerAttachment, pgUpload);
				redmineIssue.uploads.push(redmineAttachment);
			}
			else if (trackerAttachment.isDeleted()) {
				deleteAttachment(trackerAttachment.getId());
			}
		}
	}
	catch (ex) {

		// Remove so far uploaded attachments
		for (var i = 0; i < redmineIssue.uploads.length; i++) {
			var token = redmineIssue.uploads[i];
			try {
				deleteAttachment(token);
			}
			catch (ex2) {
			}
		}
	}

	if (islfine) log.log(Level.FINE, ")toRedmineIssue=" + redmineIssue);
	return redmineIssue;
}

function setRedmineIssueCustomField(redmineIssue, fieldId, propId, propValue) {
	if (islfine) log.log(Level.FINE, "setRedmineIssueCustomField(");
	if (typeof propValue != "undefined") {
		var type = -1;
		var pclass = config.propertyClasses.get(propId);
		if (!pclass) {
			if (islsevere) log.log(Level.SEVERE, "Missing property class for property ID=" + propId);
			return;
		}
		type = pclass.type;
		if (islfine) log.log(Level.FINE, "propertyClass=" + pclass + ", type=" + type);
		switch (type) {
		case PropertyClass.TYPE_BOOL:
			fieldValue = (!!propValue) ? 1 : 0;
			break;
		case PropertyClass.TYPE_STRING_LIST:
		case PropertyClass.TYPE_STRING:
		case PropertyClass.TYPE_ISO_DATE:
			fieldValue = propValue;
			break;
		}

		var obj = {
			"id" : fieldId,
			"value" : fieldValue
		};
		redmineIssue.custom_fields.push(obj);
		if (islfine) log.log(Level.FINE, "set field=" + JSON.stringify(obj));
	}
	if (islfine) log.log(Level.FINE, ")setRedmineIssueCustomField");
}

function getIssuePropertyValue(issue, propId) {
	if (islfine) log.log(Level.FINE, "getIssuePropertyValue(" + propId);
	var ret = null;
	var pclass = config.propertyClasses.get(propId);
	if (!pclass) {
		if (islsevere) log.log(Level.SEVERE, "Missing property class for property ID=" + propId);
		return ret;
	}
	var type = pclass.type;
	if (islfine) log.log(Level.FINE, "propertyClass=" + pclass + ", type=" + type);
	switch (type) {
	case PropertyClass.TYPE_STRING_LIST: {
		ret = [];
		var list = issue.getPropertyStringList(propId, null);
		if (islfine) log.log(Level.FINE, "list=" + list);
		if (list) {
			for (var i = 0; i < list.size(); i++) {
				ret.push(list.get(i));
			}
		}
		break;
	}
	case PropertyClass.TYPE_BOOL: {
		ret = !!issue.getPropertyBoolean(propId, false);
		break;
	}
	case PropertyClass.TYPE_STRING:
	case PropertyClass.TYPE_ISO_DATE:
		ret = issue.getPropertyString(propId, "");
		break;
	default:
		if (islsevere) log.log(Level.SEVERE, "Unknown property type=" + type);
	}

	if (islfine) log.log(Level.FINE, ")getIssuePropertyValue=" + ret);
	return ret;
}

function deleteAttachment(attId) {
	log.warn("Removing attachments is not supported.");
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

function validateIssue(iss) {
	var ret = iss;
	return ret;
};

function writeAttachment(trackerAttachment, progressCallback) {
	if (islfine) log.log(Level.FINE, "writeAttachment(" + trackerAttachment + ", progressCallback=" + progressCallback);
	var content = trackerAttachment.getStream();

	var uploadResult = httpClient.upload("/uploads.json", content, trackerAttachment.getContentLength(),
			progressCallback);
	ddump(uploadResult);

	trackerAttachment.setId(uploadResult.upload.token);

	var redmineAttachment = {};
	redmineAttachment.token = uploadResult.upload.token;
	redmineAttachment.filename = trackerAttachment.getFileName();
	redmineAttachment.content_type = trackerAttachment.getContentType();

	if (progressCallback) {
		progressCallback.setFinished();
	}

	if (islfine) log.log(Level.FINE, ")writeAttachment=" + redmineAttachment);
	return redmineAttachment;
};

function readIssue(issueId) {
	if (islfine) log.log(Level.FINE, "readIssue(" + issueId);
	var response = httpClient.get("/issues/" + issueId + ".json?"
			+ "include=children,attachments,relations,changesets,journals,watchers");
	ddump("issue", response);

	var redmineIssue = response.issue;
	var trackerIssue = new Issue();
	toTrackerIssue(redmineIssue, trackerIssue);

	if (islfine) log.log(Level.FINE, ")readIssue");
	return trackerIssue;
}

function toTrackerIssue(redmineIssue, issue) {
	if (islfine) log.log(Level.FINE, "toTrackerIssue(" + redmineIssue.id);

	// Standard properties
	issue.id = redmineIssue.id;
	issue.subject = redmineIssue.subject;
	issue.description = redmineIssue.description;
	if (islfine) log.log(Level.FINE, "issue.subject=" + issue.subject);

	if (redmineIssue.project) {
		issue.project = redmineIssue.project.id;
		if (islfine) log.log(Level.FINE, "issue.project=" + issue.project);
	}
	if (redmineIssue.tracker) {
		issue.type = redmineIssue.tracker.id;
		if (islfine) log.log(Level.FINE, "issue.type=" + issue.type);
	}
	if (redmineIssue.status) {
		issue.status = redmineIssue.status.id;
		if (islfine) log.log(Level.FINE, "issue.status=" + issue.status);
	}
	if (redmineIssue.priority) {
		issue.priority = redmineIssue.priority.id;
		if (islfine) log.log(Level.FINE, "issue.priority=" + issue.priority);
	}
	if (redmineIssue.assigned_to) {
		issue.assignee = redmineIssue.assigned_to.id;
	}
	else {
		issue.assignee = -1;
	}
	if (islfine) log.log(Level.FINE, "issue.assignee=" + issue.assignee);

	if (redmineIssue.category) {
		setIssuePropertyValue(issue, config.PROPERTY_ID_ISSUE_CATEGORY, redmineIssue.category.id);
	}
	if (redmineIssue.fixed_version) {
		setIssuePropertyValue(issue, config.PROPERTY_ID_FIXED_VERSION, redmineIssue.fixed_version.id);
	}

	// Redmine specific properties
	var propertyIds = [ config.PROPERTY_ID_START_DATE, config.PROPERTY_ID_DUE_DATE, config.PROPERTY_ID_ESTIMATED_HOURS,
			config.PROPERTY_ID_DONE_RATIO ];
	for (var i = 0; i < propertyIds.length; i++) {
		var propId = propertyIds[i];
		var propValue = redmineIssue[propId];
		if (islfine) log.log(Level.FINE, "propId=" + propId + ", propValue=" + propValue);
		setIssuePropertyValue(issue, propertyIds[i], propValue);
	}

	// Custom properties
	if (redmineIssue.custom_fields) {
		if (islfine) log.log(Level.FINE, "#custom_fields=" + redmineIssue.custom_fields.length);
		for (var i = 0; i < redmineIssue.custom_fields.length; i++) {
			var propId = makeCustomFieldPropertyId(redmineIssue.custom_fields[i]);
			var propValue = redmineIssue.custom_fields[i].value;
			if (islfine) log.log(Level.FINE, "custom propId=" + propId + ", propValue=" + propValue);
			setIssuePropertyValue(issue, propId, propValue);
		}
	}

	// Attachments
	if (redmineIssue.attachments) {
		if (islfine) log.log(Level.FINE, "#attachments=" + redmineIssue.attachments.length);
		var trackerAttachments = [];
		for (var i = 0; i < redmineIssue.attachments.length; i++) {
			var ra = redmineIssue.attachments[i];
			var ta = new Attachment();
			ta.id = ra.id;
			ta.subject = ra.description;
			ta.contentType = ra.content_type;
			ta.fileName = ra.filename;
			ta.contentLength = ra.filesize;
			ta.url = ra.content_url;
			if (islfine) log.log(Level.FINE, "attachment id=" + ta.id + ", file=" + ta.fileName);
			trackerAttachments.push(ta);
		}
		issue.attachments = trackerAttachments;
	}

	if (islfine) log.log(Level.FINE, ")toTrackerIssue");
}

function setIssuePropertyValue(issue, propId, propValue) {
	if (islfine) log.log(Level.FINE, "setIssuePropertyValue(");
	if (typeof propValue != "undefined") {
		var type = -1;
		var pclass = config.propertyClasses.get(propId);
		if (!pclass) {
			if (islsevere) log.log(Level.SEVERE, "Missing property class for property ID=" + propId);
			return;
		}
		type = pclass.type;
		if (islfine) log.log(Level.FINE, "propertyClass=" + pclass + ", type=" + type + ", value=" + propValue);
		switch (type) {
		case PropertyClass.TYPE_STRING_LIST:
			issue.setPropertyStringList(propId, propValue ? propValue : []);
			break;
		case PropertyClass.TYPE_BOOL:
			issue.setPropertyBoolean(propId, propValue != 0);
			break;
		case PropertyClass.TYPE_STRING:
		case PropertyClass.TYPE_ISO_DATE:
			issue.setPropertyString(propId, propValue);
			break;
		}
	}
	if (islfine) log.log(Level.FINE, ")setIssuePropertyValue");
}

function getIssueHistoryUrl(issueId) {
	return config.url + "/issues/" + issueId + "?key=" + config.apiKey;
}

initializePropertyClasses();
