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

var IXConnFactory = Java.type("de.elo.ix.client.IXConnFactory");
var IXConnection = Java.type("de.elo.ix.client.IXConnection");

var Logger = Java.type("java.util.logging.Logger");
var log = Logger.getLogger("IssueServiceImpl-ELO.js");

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

	url : "http://srvpelo1:9090/ix-lldo_prod/ix",
	projectNames : "",
	msgFileType : ".msg",

	// Property IDs of configuration data.
	// IDs are member names to simplify function fromProperties
	PROPERTY_ID_URL : "url",
	PROPERTY_ID_PROJECT_NAMES : "projectNames",
	PROPERTY_ID_MSG_FILE_TYPE : "msgFileType",

	toProperties : function() {
		return [ new Property(this.PROPERTY_ID_URL, this.url),
				new Property(this.PROPERTY_ID_PROJECT_NAMES, this.projectNames),
				new Property(this.PROPERTY_ID_MSG_FILE_TYPE, this.msgFileType)];
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

// IX-Connection, created in initialize()
var conn = null;


var data = {

	/**
	 * Map of projects. Key: project ID, value: project.
	 * this.projects[.].members contains an array of project members
	 * (assignees). this.projects[.].versions contains an array of versions
	 * (milestones).
	 */
	projects : {},

	clear : function() {
		this.projects = {};
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

	data.projects = {};
	data.projects["pid1"] = { "name" : "projekt1" };
	data.projects["pid2"] = { "name" : "projekt2" };
	
	log.info(")readProjects");
};

function readProjectVersions(project) {
	log.info("readProjectVersions(project.id=" + project.id);
	project.versions = ["1.0", "2.0"];
	log.info(")readProjectVersions");
}

function readProjectMembers(project) {
	log.info("readProjectMembers(project.id=" + project.id);
	project.members = [
			{"id" : 1, "name" : "Alpha" },
			{"id" : 2, "name" : "Beta" }
	];
	
	dump("project.members", project.members);
	log.info(")readProjectMembers");
}

function writeIssue(issue, progressCallback) {
	log.info("writeIssue(");
	dump("send", issue);
	log.info(")writeIssue=");
	return ret;
}

function initialize() {
	config.valid = false;

	data.clear();
	
	if (conn) {
		try {
			conn.logout();
		}
		catch (ex) {}
	}
	
	var connFact = new IXConnFactory(url, "ELO Issue Tracker for Outlook", "1.0");
	conn = connFact.createSSO();

	readProjects(data);

	readCurrentUser(data);

	config.valid = true;
}

function initializePropertyClasses() {

	var propertyClasses = PropertyClasses.getDefault();

	// Configuration properties

	propertyClasses.add(PropertyClass.TYPE_STRING, config.PROPERTY_ID_URL,
			"IX URL");
	propertyClasses.add(PropertyClass.TYPE_STRING,
			config.PROPERTY_ID_PROJECT_NAMES,
			"Projects (optional, comma separated)");
	propertyClasses.add(PropertyClass.TYPE_STRING,
			config.PROPERTY_ID_MSG_FILE_TYPE,
			"Attach mail as");
	
	var propMsgFileType = propertyClasses.get(config.PROPERTY_ID_MSG_FILE_TYPE);
	propMsgFileType.setSelectList([ new IdName(".msg", "Outlook (.msg)"),
	                  			new IdName(".mhtml", "MIME HTML (.mhtml)"),
	                  			new IdName(".rtf", "Rich Text Format (.rtf)") ]);

	// Initialize select list for some issue properties

	var propIssueType = propertyClasses.get(Property.ISSUE_TYPE);
	propIssueType.setSelectList([ new IdName(1, "Bug"), new IdName(2, "Todo")]);

	var propPriority = propertyClasses.get(Property.PRIORITY);
	propPriority.setSelectList([ new IdName(1, "Priorität A"),
			new IdName(2, "Priorität B"), new IdName(3, "Prioriät C")]);

	var propIssueStatus = propertyClasses.get(Property.STATE);
	propIssueStatus.setSelectList([ new IdName(1, "Neuer Eintrag"),
	        // Field status_id seems to be ignored when creating a new issue.  
	        // Although it works on the web page.
//			new IdName(2, "In progress"), new IdName(3, "Resolved"),
//			new IdName(4, "Feedback"), new IdName(5, "Closed"),
//			new IdName(6, "Rejected") 
	]);

	var propMilestones = propertyClasses.get(Property.MILESTONES);
	propMilestones.setName("Versionen");

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
	var ret = [new IdName(-1, "Unassigned")];
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

function getIssueStates(iss) {
	return getPropertyClasses().get(Property.STATE).getSelectList();
};

function getDetails(issue) {

}

function getDescriptionHtmlEditor(issue) {
	return "";
}

function getShowIssueUrl(issueId) {
	return "";
}

function getMsgFileType() {
	return config.msgFileType;
}

function createIssue() {
	config.checkValid();
	
	var iss = new Issue();

	iss.setType(1); // Bug
	iss.setPriority(2); // Normal priority
	iss.setState(1); // New issue
	iss.setAssignee(-1);

	var projects = getCategories(null);
	iss.setCategory(projects[0].getId());
	
	return iss;
};

function updateIssue(trackerIssue, progressCallback) {
	log.info("updateIssue(trackerIssue=" + trackerIssue + ", progressCallback=" + progressCallback);
	config.checkValid();

	var redmineIssue = {};
	toRedmineIssue(trackerIssue, redmineIssue, progressCallback);

	var issueParam = {	issue : redmineIssue };
	redmineIssue = writeIssue(issueParam, progressCallback).issue;

	toTrackerIssue(redmineIssue, trackerIssue);

	log.info(")updateIssue=" + trackerIssue);
	return trackerIssue;
};

function toRedmineIssue(trackerIssue, redmineIssue, progressCallback) {
	log.info("toRedmineIssue(trackerIssue=" + trackerIssue + ", redmineIssue=" + redmineIssue + ", progressCallback=" + progressCallback);
	redmineIssue.project_id = parseInt(trackerIssue.getCategory());
	redmineIssue.tracker_id = parseInt(trackerIssue.getType());
	redmineIssue.status_id = parseInt(trackerIssue.getState());
	redmineIssue.priority_id = parseInt(trackerIssue.getPriority());
	redmineIssue.subject = "" + trackerIssue.getSubject();
	redmineIssue.description = "" + trackerIssue.getDescription();

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
				if (progressCallback.isCancelled()) break;
				var str = "Upload attachment " + trackerAttachment.getFileName();
				str += ", " + makeAttachmentSizeString(trackerAttachment.getContentLength());
				pgUpload = progressCallback.createChild(str);
				pgUpload.setTotal(trackerAttachment.getContentLength());
			}
			redmineAttachment = writeAttachment(trackerAttachment, pgUpload);
			redmineIssue.uploads.push(redmineAttachment);
		}
	}
	catch (ex) {
		for (var i = 0; i < redmineIssue.uploads.length; i++) {
			var token = redmineIssue.uploads[i];
			try {
				deleteAttachment(token);
			}
			catch (ex2) {}
		}
	}

	log.info(")toRedmineIssue=" + redmineIssue);
	return redmineIssue;
}

function makeAttachmentSizeString(contentLength) {
	var dims = ["Bytes", "KB", "MB", "GB", "TB"];
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
	var issueId = "";
	var startTag = "[ITOL-";
	var p = subject.indexOf(startTag);
	if (p == 0) {
		var q = subject.indexOf("]");
		if (q >= 0) {
			var str = subject.substring(p + startTag.length, q);
			p = str.indexOf("-");
			if (p >= 0) {
				issueId = str.substring(p+1);
			}
			else {
				issueId = str;
			}
		}
	}
	return issueId;
};

function injectIssueIdIntoMailSubject(subject, iss) {
	var ret = "[ITOL-";
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

function writeAttachment(trackerAttachment, progressCallback) {
	log.info("writeAttachment(" + trackerAttachment + ", progressCallback=" + progressCallback);
	var content = trackerAttachment.getStream();
	
	var uploadResult =  httpClient.upload("/uploads.json", content, trackerAttachment.getContentLength(), progressCallback);
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

function deleteAttachment(attachmentId) {
};

initializePropertyClasses();
