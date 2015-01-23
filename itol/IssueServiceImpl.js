//load("nashorn:mozilla_compat.js");
//importPackage(com.wilutions.itol.db);

var MAX_PROJECTS = 20;
var MAX_USERS = 20;

var IOException = Java.type("java.io.IOException");
var Property = Java.type("com.wilutions.itol.db.Property");
var PropertyClass = Java.type("com.wilutions.itol.db.PropertyClass");
var PropertyClasses = Java.type("com.wilutions.itol.db.PropertyClasses");
var IdName = Java.type("com.wilutions.itol.db.IdName");
var JHttpClient = Java.type("com.wilutions.itol.db.HttpClient");
var HttpResponse = Java.type("com.wilutions.itol.db.HttpResponse");
var IssueUpdate = Java.type("com.wilutions.itol.db.IssueUpdate");
var Issue = Java.type("com.wilutions.itol.db.Issue");
var DescriptionHtmlEditor = Java.type("com.wilutions.itol.db.DescriptionHtmlEditor");

var Logger = Java.type("java.util.logging.Logger");
var log = Logger.getLogger("IssueServiceImpl.js");

function dump(name, obj) {
	var str = JSON.stringify(obj, null, 2);
	log.info(name + "=" + str);
}

var config = {

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

	url : "http://192.168.0.11",
	apiKey : "... see \"Redmine / My account / API access key\" ... ",
	projectNames : "",

	// Property IDs of configuration data.
	// IDs are member names to simplify function fromProperties
	PROPERTY_ID_URL : "url",
	PROPERTY_ID_API_KEY : "apiKey",
	PROPERTY_ID_PROJECT_NAMES : "projectNames",

	toProperties : function() {
		return [ new Property(this.PROPERTY_ID_URL, this.url),
				new Property(this.PROPERTY_ID_API_KEY, this.apiKey),
				new Property(this.PROPERTY_ID_PROJECT_NAMES, this.projectNames) ];
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

};

var httpClient = {

	send : function(method, headers, params, content) {
		var destUrl = config.url + params;
		this._addAuthHeader(headers);
		var response = JHttpClient.send(destUrl, method, headers, content);
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

	post : function(params, content) {
		var headers = [ "Content-Type: application/json" ];
		return JSON.parse(this.send("POST", headers, params, JSON
				.stringify(content)).content);
	},

	upload : function(params, file) {
		var headers = [ "Content-Type: application/octet-stream" ];
		return JSON.parse(this.send("POST", headers, params, file).content);
	},

	get : function(params) {
		var headers = [];
		return JSON.parse(this.send("GET", headers, params, null).content);
	},

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

	clear : function() {
		this.projects = {};
		this.user = {};
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

		var projectsResponse = httpClient.get("/projects.json?offset=" + offset
				+ "&limit=1000");
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
	var arrOfMemberships = httpClient.get("/projects/" + project.id
			+ "/memberships.json").memberships;
	for (var j = 0; j < arrOfMemberships.length && j < MAX_USERS; j++) {
		var membership = arrOfMemberships[j];
		var user = membership.user;
		project.members.push(user);
	}
	dump("project.members", project.members);
	log.info(")readProjectMembers");
}

function writeIssue(issue) {
	log.info("writeIssue(");
	dump("send", issue);
	var ret = httpClient.post("/issues.json", issue);
	dump("recv", ret);
	log.info(")writeIssue=");
	return ret;
}

function initialize() {
	config.valid = false;

	data.clear();

	readProjects(data);

	readCurrentUser(data);

	config.valid = true;
}

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

	// Initialize select list for some issue properties

	var propIssueType = propertyClasses.get(Property.ISSUE_TYPE);
	propIssueType.setSelectList([ new IdName(1, "Bug"),
			new IdName(2, "Feature Request"), new IdName(3, "Support") ]);

	var propPriority = propertyClasses.get(Property.PRIORITY);
	propPriority.setSelectList([ new IdName(1, "Low priority"),
			new IdName(2, "Normal priority"), new IdName(3, "High priority"),
			new IdName(4, "Urgent"), new IdName(5, "Immediate") ]);

	var propIssueStatus = propertyClasses.get(Property.STATE);
	propIssueStatus.setSelectList([ new IdName(1, "New issue"),
			new IdName(2, "In progress"), new IdName(3, "Resolved"),
			new IdName(4, "Feedback"), new IdName(5, "Closed"),
			new IdName(6, "Rejected") ]);

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
}

function getPropertyClasses() {
	return config.propertyClasses;
}

function getIssueTypes(issue) {
	return getPropertyClasses().get(Property.ISSUE_TYPE).getSelectList();
};

function getPriorities(issue) {
	return getPropertyClasses().get(Property.PRIORITY).getSelectList();
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
	var ret = [];
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
	} else {
		ret.push(new IdName(-1, "No members"));
	}
	return ret;
};

function getCurrentUser() {
	return data.user ? new IdName(parseInt(data.user.id), data.user.firstname
			+ " " + data.user.lastname) : new IdName(0, "");
};

function getIssueStates(iss) {
	return getPropertyClasses().get(Property.STATE).getSelectList();
};

function getDetails(issue) {

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

function createIssue() {
	config.checkValid();
	
	var iss = new Issue();

	iss.setType(1); // Bug
	iss.setPriority(2); // Normal priority
	iss.setState(1); // New issue

	var projects = getCategories(null);
	iss.setCategory(projects[0].getId());
	
	return iss;
};

function updateIssue(trackerIssue) {
	config.checkValid();

	var redmineIssue = {};
	toRedmineIssue(trackerIssue, redmineIssue);

	redmineIssue = writeIssue({
		issue : redmineIssue
	}).issue;

	toTrackerIssue(redmineIssue, trackerIssue);

	return trackerIssue;
};

function toRedmineIssue(trackerIssue, redmineIssue) {

	redmineIssue.project_id = "" + trackerIssue.getCategory();
	redmineIssue.tracker_id = "" + trackerIssue.getType();
	redmineIssue.status_id = "" + trackerIssue.getState();
	redmineIssue.priority_id = "" + trackerIssue.getPriority();
	redmineIssue.subject = "" + trackerIssue.getSubject();
	redmineIssue.description = "" + trackerIssue.getDescription();

	if (trackerIssue.getAssignee() >= 0) {
		redmineIssue.assigned_to_id = "" + trackerIssue.getAssignee();
	}

	if (trackerIssue.getMilestones().length) {
		redmineIssue.fixed_version_id = "" + trackerIssue.getMilestones()[0];
	}

	return redmineIssue;
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

function findFirstIssues(findInfo, idx, max) {
	// TODO Auto-generated method stub
	return null;
};

function findNextIssues(searchId, idx, max) {
	// TODO Auto-generated method stub
	return null;
};

function findCloseIssues(searchId) {
	// TODO Auto-generated method stub
};

function extractIssueIdFromMailSubject(subject) {
	return "1";
};

function injectIssueIdIntoMailSubject(subject, iss) {
	var ret = "[";
	switch (iss.getType()) {
	case "1":
		ret += "B";
		break;
	case "2":
		ret += "F";
		break;
	case "3":
		ret += "S";
		break;
	default:
		sbuf.append("U");
		break;
	}

	ret += "-" + iss.getId() + "]";
	ret += subject;

	return ret;
};

function readIssue(issueId) {
	return issues.get(issueId);
};

function readAttachment(attachmentId) {
	return null;
};

function writeAttachment(att) {
	return null;
};

function deleteAttachment(attachmentId) {
};

initializePropertyClasses();
