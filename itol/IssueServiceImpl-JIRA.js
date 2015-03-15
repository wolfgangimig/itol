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
var MAX_PROJECTS = 1000;

/**
 * Maximum number of users per project. This value constraints the number of
 * combo box items in the UI.
 */
var MAX_USERS = 1000;

/**
 * Name and description of the ITOL configuration project.
 */
var ITOL_CONFIG_NAME = "Issue Tracker for Microsoft Outlook and JIRA Configuration";
var ITOL_CONFIG_DESC = "This project stores the configuration data for the "
		+ "Issue Tracker Addin for Microsoft Outlook and JIRA. " + "Last update was at:";
var ITOL_CONFIG_DESC_TAG_BEGIN = "<pre>ENCRYPTED_DATA_BEGIN\n";
var ITOL_CONFIG_DESC_TAG_END = "\nENCRYPTED_DATA_END</pre>";

/**
 * Import requried Java classes
 */
var IOException = Java.type("java.io.IOException");
var URLEncoder = Java.type("java.net.URLEncoder");
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
	 * JIRA URL
	 */
	url : "http://192.168.0.18:8080",

	/**
	 * User name
	 */
	userName : "JIRA login name",

	/**
	 * User password
	 */
	userPwd : "",

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
	PROPERTY_ID_USER_NAME : "userName",
	PROPERTY_ID_USER_PASSWORD : "userPwd",
	PROPERTY_ID_MSG_FILE_TYPE : "msgFileType",

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
		new Property(this.PROPERTY_ID_URL, this.url), new Property(this.PROPERTY_ID_USER_NAME, this.userName),
				new Property(this.PROPERTY_ID_USER_PASSWORD, this.userPwd),
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
		var restDir = "/rest/api/2";
		var destUrl = config.url + restDir + params;
		this._addAuthHeader(headers);
		var response = JHttpClient.send(destUrl, method, headers, content, progressCallback ? progressCallback : null);
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
	 * Add BASIC authentication header
	 */
	_addAuthHeader : function(headers) {
		if (config.userName) {
			var pwd = config.userPwd;
			var auth = JHttpClient.makeBasicAuthenticationHeader(config.userName, pwd);
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
	 * this.projects[.].issueTypes is a map of issue types.
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
	 * Default priority ID.
	 */
	defaultPriority : 3, // Major

	/**
	 * Custom fields definitions.
	 */
	custom_fields : [],

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

	var response = httpClient.get("/issue/createmeta?expand=projects.issuetypes.fields");
	ddump("projects", response.projects);
	
	data.projects = {};

	var projectCount = 0;
	var arrOfProjects = response.projects;
	for (var i = 0; i < arrOfProjects.length && projectCount < MAX_PROJECTS; i++) {
		var project = arrOfProjects[i];
		data.projects[project.id] = project;
		
		// Make issueTypes map from issuetypes array
		project.issueTypes = {};
		for (var t = 0; t < project.issuetypes.length; t++) {
			var issueType = project.issuetypes[t];
			project.issueTypes[issueType.id] = issueType;
		}
		delete project.issuetypes;
		
		if (islinfo) log.log(Level.INFO, "project.id=" + project.id + ", .name=" + project.name);
		projectCount++;
	}

	if (islfine) log.log(Level.FINE, ")readProjects");
};

function readCurrentUser(data) {
	if (islfine) log.log(Level.FINE, "readCurrentUser(");
	data.user = httpClient.get("/myself");
	ddump("user ", data.user);
	if (islinfo) log.log(Level.INFO, "data.user.key=" + data.user.key + ", .name=" + data.user.name);
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

function writeIssue(issueParam, progressCallback) {
	if (islfine) log.log(Level.FINE, "writeIssue(");
	ddump("send", issueParam);
	var ret = null;

	var pgIssue = null;
	if (progressCallback) {
		if (progressCallback.isCancelled()) return;
		pgIssue = progressCallback.createChild("Write issue");
	}

	var issueId = issueParam.fields.id;
	var isUpdate = !!issueId;
	if (islfine) log.log(Level.FINE, "issueId=" + issueId + ", isUpdate=" + isUpdate);

	if (isUpdate) {
		httpClient.put("/issue/" + issueId, issueParam, pgIssue);
	}
	else {
		var iss = httpClient.post("/issue", issueParam, pgIssue);
		issueId = iss.id;
	}
	
	ret = httpClient.get("/issue/" + issueId);

	ddump("recv", ret);
	if (islfine) log.log(Level.FINE, ")writeIssue=");
	return ret;
}

function initialize() {
	if (islfine) log.log(Level.FINE, "initialize(");
	config.valid = false;

	if (islinfo) {
		log.log(Level.INFO, "config.url=" + config.url);
		log.log(Level.INFO, "config.userName=" + config.userName);
		log.log(Level.INFO, "config.msgFileType=" + config.msgFileType);
	}

	data.clear();

	if (islinfo) log.log(Level.INFO, "readProjects");
	readProjects(data);

	if (islinfo) log.log(Level.INFO, "readCurrentUser");
	readCurrentUser(data);
	
	config.valid = true;
	if (islinfo) log.log(Level.INFO, "initialized");

	if (islfine) log.log(Level.FINE, ")initialize");
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

	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_URL, "JIRA URL");
	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_USER_NAME, "User name");
	propertyClasses.add(PropertyClass.TYPE_PASSWORD, config.PROPERTY_ID_USER_PASSWORD, "Password");
	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_MSG_FILE_TYPE, "Attach mail as");
	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_USE_CUSTOM_FIELDS, "Use custom fields", "0", [
			new IdName("0", "No"), new IdName("1", "Yes") ]);

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
		initialize();
	}
};

function getPropertyClasses() {
	return config.propertyClasses;
};

function getPropertyClass(propertyId, issue) {
	var ret = getPropertyClasses().getCopy(propertyId);
	switch (propertyId) {
	case Property.ISSUE_TYPE:
		ret.selectList = getIssueTypes(issue);
		break;
	case Property.PRIORITY:
		ret.selectList = getIssuePriorities(issue);
		break;
	case Property.PROJECT:
		ret.selectList = getProjectsIdNames(issue);
		break;
	case Property.ASSIGNEE:
		ret.selectList = getAssignees(issue);
		break;
	case Property.STATUS:
		ret.selectList = getIssueStatuses(issue);
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

function getIssueTypes(issue) {
	if (islfine) log.log(Level.FINE, "getIssueTypes(");
	var ret = [];
	var project = getIssueProject(issue);
	if (islfine) log.log(Level.FINE, "project=" + project);
	if (project && project.issueTypes) {
		for ( var typeId in project.issueTypes) {
			var issueType = project.issueTypes[typeId];
			if (issueType) {
				var idn = new IdName(typeId, issueType.name);
				ret.push(idn);
				if (islfine) log.log(Level.FINE, "issueType=" + idn);
			}
		}
	}
	if (islfine) log.log(Level.FINE, ")getIssueTypes");
	return ret;
};

function getIssuePriorities(issue) {
	if (islfine) log.log(Level.FINE, "getIssuePriorities(");
	var ret = [];
	var project = getIssueProject(issue);
	if (islfine) log.log(Level.FINE, "project=" + project + ", issue.type=" + issue.type);
	if (project && project.issueTypes) {
		if (islfine) log.log(Level.FINE, "for typeId...");
		for ( var typeId in project.issueTypes) {
			if (islfine) log.log(Level.FINE, "typeId=" + typeId);
			var issueType = project.issueTypes[typeId];
			if (islfine) log.log(Level.FINE, "issueType=" + issueType + ", issueType.id=" + issueType.id);
			if (issueType.id == issue.type) {
				if (islfine) log.log(Level.FINE, "issueType.fields=" + issueType);
				if (issueType.fields) {
					var priority = issueType.fields.priority;
					if (islfine) log.log(Level.FINE, "issueType.fields.priority=" + priority + ", issueType.fields.allowedValues=" + priority.allowedValues);
					if (priority && priority.allowedValues) {
						for (var prioIdx = 0; prioIdx < priority.allowedValues.length; prioIdx++) {
							var prioValue = priority.allowedValues[prioIdx];
							var idn = new IdName(prioValue.id, prioValue.name);
							ret.push(idn);
							if (islfine) log.log(Level.FINE, "prioValue=" + idn);
						}
					}
				}
			}
		}
	}
	if (islfine) log.log(Level.FINE, ")getIssuePriorities");
	return ret;
}

function getIssueStatuses(issue) {
	if (islfine) log.log(Level.FINE, "getIssueStatuses(");
	var ret = [];
	if (issue.id) {
		var project = getIssueProject(issue);
		var typeId = issue ? issue.type : 0;
		if (project && project.issueTypes && typeId) {
			var issueType = project.issueTypes[typeId];
			if (issueType) {
				for (var statusId in issueType.statuses) {
					var status = issueType.statuses[statusId];
					var idn = new IdName(statusId, status.name);
					ret.push(idn);
					if (islfine) log.log(Level.FINE, "status=" + idn);
				}
			}
		}
	}
	else {
		ret = [new IdName(-1, "New")];
	}
	if (islfine) log.log(Level.FINE, ")getIssueStatuses");
	return ret;
};

function getProjectsIdNames(issue) {
	if (islfine) log.log(Level.FINE, "getProjectIdNames(" + issue);
	var ret = [];
	// Project association of an existing issue cannot be changed.
	// This is my experience, although the JIRA documentation
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
	if (project && project.versions) {
		for (var versionId in project.versions) {
			var version = project.versions[versionId];
			ret.push(new IdName(version.id, version.name));
		}
	}
	return ret;
};

function getCategories(issue) {
	var ret = [];
	return ret;
};

function getIssueProjectMemberships(issue) {
	if (islfine) log.log(Level.FINE, "getIssueProjectMemberships(");
	var ret = [];
	if (islfine) log.log(Level.FINE, ")getIssueProjectMemberships=#" + ret.length);
	return ret;
}

function getAssignees(issue) {
	var ret = [];
	var issueId = issue ? issue.id : 0;
	return ret;
};



function getPropertyAutoCompletion(propId, issue, filter) {
	if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "getPropertyAutoCompletion(propId=" + propId + ", filter=" + filter);
	var ret = null;
	
	if (propId == Property.ASSIGNEE) {
		
		if (issue != null && filter != null) {
			
			var url = "/user/assignable/search?";
			
			var userName = URLEncoder.encode(filter, "UTF-8");
			url += "username=" + userName;
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "userName=" + userName);
			
			var project = getIssueProject(issue);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "project=" + project);
			
			if (project) {
				url += "&project=" + project.key;
			}

			var issueKey = null;
			if (issueKey) {
				url += "&issueKey=" + issueKey;
			}
			
			url += "&startAt=0";
			url += "&maxResults=10";
			
			var response = httpClient.get(url);
			ddump("assignees", response);
			
			ret = [];
			for (var i = 0; i < response.length; i++) {
				var user = response[i];
				ret.push(new IdName(user.key, user.displayName));
			}

		}
		else {
			// Just a check whether auto completion is supported.
			// Return an empty array in this case.
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "auto completion supported");
			ret = [];
		}
	}
	else {
		// Auto complete is not supported, return null.
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "auto completion unsupported");
		ret = null;
	}
	
	if (log.isLoggable(Level.FINE)) log.log(Level.FINE, ")getPropertyAutoCompletion");
	return ret;
}

function getCurrentUser() {
	return data.user ? new IdName(parseInt(data.user.id), data.user.displayName) : new IdName(
			0, "");
};

function getDescriptionTextEditor(issue) {
	return null;
}

function getHtmlEditor(issue, propertyId) {
	return null;
}

function getShowIssueUrl(issueId) {
	return getIssueHistoryUrl(issueId);
}

function getMsgFileType() {
	return config.msgFileType;
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

	subject = stripIssueIdFromMailSubject(subject);
	if (islfine) log.log(Level.FINE, "subject without issue ID=" + subject);

	// strip RE:, Fwd:, AW:, WG: ...
	subject = stripReFwdFromSubject(subject);
	if (islfine) log.log(Level.FINE, "subject without RE, FWD, ... =" + subject);

	var issue = new Issue();
	var defaultProps = makeDefaultProperties(defaultIssueAsString);
	ddump("defaultProps", defaultProps);

	issue.setPriority(data.defaultPriority); 
	issue.setStatus(-1); // New issue

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

	var JIRAIssue = toJIRAIssue(trackerIssue, modifiedProperties, progressCallback);

	JIRAIssue = writeIssue(JIRAIssue, progressCallback);

	// Convert JIRAIssue to trackerIssue,
	// issueReturn is null when updating an exisiting issue.
	if (JIRAIssue) {
		toTrackerIssue(JIRAIssue, trackerIssue);
	}

	// Delete the Property.NOTES which is only used to add notes.
	// Existing notes cannot be edited with ITOL.
	trackerIssue.setPropertyString(Property.NOTES, "");

	if (islfine) log.log(Level.FINE, ")updateIssue=" + trackerIssue);
	return trackerIssue;
};

function toJIRAIssue(trackerIssue, modifiedProperties,  progressCallback) {
	if (islfine) log.log(Level.FINE, "toJIRAIssue(trackerIssue=" + trackerIssue
			+ ", progressCallback=" + progressCallback);
	
	var JIRAIssue = {};
	var fields = JIRAIssue.fields = {};
	
	if (trackerIssue.id) {
		//JIRAIssue.fields.id = trackerIssue.getId();
	}
	
	fields.project = {};
	fields.project.id = ""+trackerIssue.project;
	
	fields.issuetype = {}; 
	fields.issuetype.id = ""+trackerIssue.type;
	
//	JIRAIssue.status_id = parseInt(trackerIssue.getStatus());
//	JIRAIssue.priority_id = parseInt(trackerIssue.getPriority());
	
	fields.summary = "" + trackerIssue.getSubject();
	fields.description = "" + trackerIssue.getDescription();
	
	
//	JIRAIssue.comment = [{}];
//	JIRAIssue.comment[0].add = "" + trackerIssue.getPropertyString(Property.NOTES, "");
//
//	// Assignee
//	JIRAIssue.assigned_to_id = "";
//	if (trackerIssue.getAssignee().length() != 0) {
//		var userId = parseInt(trackerIssue.getAssignee());
//		if (userId >= 0) {
//			JIRAIssue.assigned_to_id = userId;
//		}
//	}
//
//	// JIRA specific properties
//	var propertyIds = [ config.PROPERTY_ID_ISSUE_CATEGORY, config.PROPERTY_ID_FIXED_VERSION,
//			config.PROPERTY_ID_START_DATE, config.PROPERTY_ID_DUE_DATE, config.PROPERTY_ID_ESTIMATED_HOURS,
//			config.PROPERTY_ID_DONE_RATIO ];
//	for (var i = 0; i < propertyIds.length; i++) {
//		var propId = propertyIds[i];
//		var propValue = getIssuePropertyValue(trackerIssue, propId);
//		if (islfine) log.log(Level.FINE, "propId=" + propId + ", propValue=" + propValue);
//		JIRAIssue[propId] = propValue;
//	}
//
//	// Custom properties
//	if (data.custom_fields) {
//		JIRAIssue.custom_fields = [];
//		for (var i = 0; i < data.custom_fields.length; i++) {
//			var cfield = data.custom_fields[i];
//			var propId = makeCustomFieldPropertyId(cfield);
//			var propValue = getIssuePropertyValue(trackerIssue, propId);
//			if (islfine) log.log(Level.FINE, "custom propId=" + propId + ", propValue=" + propValue);
//			setJIRAIssueCustomField(JIRAIssue, cfield.id, propId, propValue);
//		}
//	}
//
//	JIRAIssue.uploads = [];
//	try {
//		for (var i = 0; i < trackerIssue.getAttachments().size(); i++) {
//			var trackerAttachment = trackerIssue.getAttachments().get(i);
//			if (islfine) log.log(Level.FINE, "trackerAttachment=" + trackerAttachment);
//
//			// Upload only new attachments
//			if (trackerAttachment.getId().isEmpty()) {
//
//				// Create inner progress callback
//				var pgUpload = null;
//				if (progressCallback) {
//					if (progressCallback.isCancelled()) break;
//					var str = "Upload attachment " + trackerAttachment.getFileName();
//					str += ", " + makeAttachmentSizeString(trackerAttachment.getContentLength());
//					pgUpload = progressCallback.createChild(str);
//					pgUpload.setTotal(trackerAttachment.getContentLength());
//				}
//
//				// Upload
//				JIRAAttachment = writeAttachment(trackerAttachment, pgUpload);
//				JIRAIssue.uploads.push(JIRAAttachment);
//			}
//			else if (trackerAttachment.isDeleted()) {
//				deleteAttachment(trackerAttachment.getId());
//			}
//		}
//	}
//	catch (ex) {
//
//		// Remove so far uploaded attachments
//		for (var i = 0; i < JIRAIssue.uploads.length; i++) {
//			var token = JIRAIssue.uploads[i];
//			try {
//				deleteAttachment(token);
//			}
//			catch (ex2) {
//			}
//		}
//	}

	if (islfine) log.log(Level.FINE, ")toJIRAIssue");
	return JIRAIssue;
}

function setJIRAIssueCustomField(JIRAIssue, fieldId, propId, propValue) {
	if (islfine) log.log(Level.FINE, "setJIRAIssueCustomField(");
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
		JIRAIssue.custom_fields.push(obj);
		if (islfine) log.log(Level.FINE, "set field=" + JSON.stringify(obj));
	}
	if (islfine) log.log(Level.FINE, ")setJIRAIssueCustomField");
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
	if (islfine) log.log(Level.FINE, "stripOneIssueIdFromMailSubject(" + subject);
	var ret = subject;

	var startTag = "[R-";
	var p = subject.indexOf(startTag);
	if (p >= 0) {
		if (islfine) log.log(Level.FINE, "found issue ID at " + p);
		var q = subject.indexOf("]", p);
		if (q >= 0) {
			ret = subject.substring(q + 1).trim();
			if (islfine) log.log(Level.FINE, "removed issue ID at " + p);
		}
	}
	ret = ret.trim();
	if (islfine) log.log(Level.FINE, ")stripOneIssueIdFromMailSubject=" + ret);
	return ret;
};

function stripIssueIdFromMailSubject(subject) {
	var ret = stripOneIssueIdFromMailSubject(subject);
	while (ret != subject) {
		subject = ret;
		ret = stripOneIssueIdFromMailSubject(subject);
	}
	ret = ret.trim();
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
	var ret = stripIssueIdFromMailSubject("" + subject);

	if (iss && iss.id && iss.id.length) {
		ret = "[R-" + iss.getId() + "] ";
		ret += subject;
	}
	else {
		var p = subject.indexOf("[R-");
		if (p >= 0) {
			var q = subject.indexOf("]", p + 3);
			if (q >= 0) {
				ret = subject.substring(0, p);
				ret += subject.substring(q + 1);
			}
		}
	}

	ret = ret.trim();
	return ret;
};

function writeAttachment(trackerAttachment, progressCallback) {
	if (islfine) log.log(Level.FINE, "writeAttachment(" + trackerAttachment + ", progressCallback=" + progressCallback);
	var content = trackerAttachment.getStream();

	var uploadResult = httpClient.upload("/uploads.json", content, trackerAttachment.getContentLength(),
			progressCallback);
	ddump(uploadResult);

	trackerAttachment.setId(uploadResult.upload.token);

	var JIRAAttachment = {};
	JIRAAttachment.token = uploadResult.upload.token;
	JIRAAttachment.filename = trackerAttachment.getFileName();
	JIRAAttachment.content_type = trackerAttachment.getContentType();

	if (progressCallback) {
		progressCallback.setFinished();
	}

	if (islfine) log.log(Level.FINE, ")writeAttachment=" + JIRAAttachment);
	return JIRAAttachment;
};

function readIssue(issueId) {
	if (islfine) log.log(Level.FINE, "readIssue(" + issueId);
	var response = httpClient.get("/issues/" + issueId + ".json?"
			+ "include=children,attachments,relations,changesets,journals,watchers");
	ddump("issue", response);

	var JIRAIssue = response.issue;
	var trackerIssue = new Issue();
	toTrackerIssue(JIRAIssue, trackerIssue);

	if (islfine) log.log(Level.FINE, ")readIssue");
	return trackerIssue;
}

function toTrackerIssue(JIRAIssue, issue) {
	if (islfine) log.log(Level.FINE, "toTrackerIssue(" + JIRAIssue.id);
	
	var fields = JIRAIssue.fields;

	// Standard properties
	issue.id = JIRAIssue.id;
	issue.subject = fields.summary;
	issue.description = fields.description;
	if (islfine) log.log(Level.FINE, "issue.subject=" + issue.subject);

	if (JIRAIssue.project) {
		issue.project = fields.project.id;
		if (islfine) log.log(Level.FINE, "issue.project=" + issue.project);
	}
	
//	if (JIRAIssue.tracker) {
//		issue.type = JIRAIssue.tracker.id;
//		if (islfine) log.log(Level.FINE, "issue.type=" + issue.type);
//	}
//	if (JIRAIssue.status) {
//		issue.status = JIRAIssue.status.id;
//		if (islfine) log.log(Level.FINE, "issue.status=" + issue.status);
//	}
//	if (JIRAIssue.priority) {
//		issue.priority = JIRAIssue.priority.id;
//		if (islfine) log.log(Level.FINE, "issue.priority=" + issue.priority);
//	}
//	if (JIRAIssue.assigned_to) {
//		issue.assignee = JIRAIssue.assigned_to.id;
//	}
//	else {
//		issue.assignee = -1;
//	}
//	if (islfine) log.log(Level.FINE, "issue.assignee=" + issue.assignee);
//
//	if (JIRAIssue.category) {
//		setIssuePropertyValue(issue, config.PROPERTY_ID_ISSUE_CATEGORY, JIRAIssue.category.id);
//	}
//	if (JIRAIssue.fixed_version) {
//		setIssuePropertyValue(issue, config.PROPERTY_ID_FIXED_VERSION, JIRAIssue.fixed_version.id);
//	}
//
//	// JIRA specific properties
//	var propertyIds = [ config.PROPERTY_ID_START_DATE, config.PROPERTY_ID_DUE_DATE, config.PROPERTY_ID_ESTIMATED_HOURS,
//			config.PROPERTY_ID_DONE_RATIO ];
//	for (var i = 0; i < propertyIds.length; i++) {
//		var propId = propertyIds[i];
//		var propValue = JIRAIssue[propId];
//		if (islfine) log.log(Level.FINE, "propId=" + propId + ", propValue=" + propValue);
//		setIssuePropertyValue(issue, propertyIds[i], propValue);
//	}
//
//	// Custom properties
//	if (JIRAIssue.custom_fields) {
//		if (islfine) log.log(Level.FINE, "#custom_fields=" + JIRAIssue.custom_fields.length);
//		for (var i = 0; i < JIRAIssue.custom_fields.length; i++) {
//			var propId = makeCustomFieldPropertyId(JIRAIssue.custom_fields[i]);
//			var propValue = JIRAIssue.custom_fields[i].value;
//			if (islfine) log.log(Level.FINE, "custom propId=" + propId + ", propValue=" + propValue);
//			setIssuePropertyValue(issue, propId, propValue);
//		}
//	}
//
//	// Attachments
//	if (JIRAIssue.attachments) {
//		if (islfine) log.log(Level.FINE, "#attachments=" + JIRAIssue.attachments.length);
//		var trackerAttachments = [];
//		for (var i = 0; i < JIRAIssue.attachments.length; i++) {
//			var ra = JIRAIssue.attachments[i];
//			var ta = new Attachment();
//			ta.id = ra.id;
//			ta.subject = ra.description;
//			ta.contentType = ra.content_type;
//			ta.fileName = ra.filename;
//			ta.contentLength = ra.filesize;
//			ta.url = ra.content_url;
//			if (islfine) log.log(Level.FINE, "attachment id=" + ta.id + ", file=" + ta.fileName);
//			trackerAttachments.push(ta);
//		}
//		issue.attachments = trackerAttachments;
//	}

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
