//load("nashorn:mozilla_compat.js");
//importPackage(com.wilutions.itol.db);

var MAX_PROJECTS = 20;
var MAX_USERS = 20;

var ISSUE_TYPE_BUG = 1;
var ISSUE_TYPE_TODO = 2;

var PRIORITY_A = 1;
var PRIORITY_B = 2;
var PRIORITY_C = 3;

var DOCMASK_TODO = "ToDoItem";
var DOCMASK_BUG = "TrackItem";

// maskBug=[61,(AB1EABAD-016E-4E83-EC68-CD3AE01DCE61),TrackItem,#lines=15]
// line.key=EFS_PROD, line.name=Produkt
// line.key=EFS_KAT, line.name=Kategorie
// line.key=EFS_OPEN, line.name=Geöffnet
// line.key=EFS_DONE, line.name=Erledigt
// line.key=EFS_CLOSED, line.name=Geschlossen
// line.key=EFS_VEROPEN, line.name=Version aufgefallen
// line.key=EFS_VERCLOSED, line.name=Version erledigt
// line.key=EFS_NR, line.name=Tracking Nr.
// line.key=EFS_TYPE, line.name=Aufgabentyp
// line.key=EFS_QSUSER, line.name=QS Verantwortlicher
// line.key=EFS_USER, line.name=Ausführender
// line.key=EFS_COMMENT, line.name=Letzter Kommentar
// line.key=EFS_MOVETO, line.name=Übertragen an
// line.key=EFS_CHECKBY, line.name=Prüfung starten
// line.key=EFS_STATECHG, line.name=Zeitstempel
// maskTodo=[60,(5B0E5986-5B28-71A4-6C66-3F4DF61C9526),ToDoItem,#lines=22]
// line.key=TODOOWNER, line.name=Einreicher
// line.key=TODOSTATUS, line.name=Status
// line.key=TODOW1, line.name=Bearbeiter 1
// line.key=TODOW2, line.name=Aktiviert durch
// line.key=TODOW3, line.name=Beended durch
// line.key=TODODONE, line.name=Abgeschlossen
// line.key=TODOVOTE, line.name=Vote
// line.key=TODOSTART, line.name=Start in Arbeit
// line.key=TODOEXTERN, line.name=Extern
// line.key=TODOPRIO, line.name=Priorität
// line.key=TODOPMANF, line.name=Status Anf
// line.key=TODOPMUMS, line.name=Status Ums
// line.key=TODOPMRBEZ, line.name=Release Bez
// line.key=TODOPMAUFW, line.name=Aufwand
// line.key=TODOPMPRIO, line.name=Priorität
// line.key=TODOPMTHEMA, line.name=Thema
// line.key=TODOPMMOD, line.name=Module
// line.key=TODOPMREAL, line.name=Realisierung
// line.key=TODOPMVOTE, line.name=Voting
// line.key=TODOID, line.name=Item ID
// line.key=TODOPROJ, line.name=Projekt
// line.key=TODOPMTYPE, line.name=PM Typ

var MessageFormat = Java.type("java.text.MessageFormat");
var IOException = Java.type("java.io.IOException");
var File = Java.type("java.io.File");
var FileOutputStream = Java.type("java.io.FileOutputStream");
var OutputStreamWriter = Java.type("java.io.OutputStreamWriter");
var Integer = Java.type("java.lang.Integer");
var JavaDate = Java.type("java.util.Date");
var System = Java.type("java.lang.System");
var FileInputStream = Java.type("java.io.FileInputStream");

var Property = Java.type("com.wilutions.itol.db.Property");
var PropertyClass = Java.type("com.wilutions.itol.db.PropertyClass");
var PropertyClasses = Java.type("com.wilutions.itol.db.PropertyClasses");
var IdName = Java.type("com.wilutions.itol.db.IdName");
var JHttpClient = Java.type("com.wilutions.itol.db.HttpClient");
var HttpResponse = Java.type("com.wilutions.itol.db.HttpResponse");
var IssueUpdate = Java.type("com.wilutions.itol.db.IssueUpdate");
var Attachment = Java.type("com.wilutions.itol.db.Attachment");
var Issue = Java.type("com.wilutions.itol.db.Issue");
var IssueHtmlEditor = Java
		.type("com.wilutions.itol.db.IssueHtmlEditor");
var IssueHtmlEditor = Java
		.type("com.wilutions.itol.db.IssueHtmlEditor");

var InetAddress = Java.type("java.net.InetAddress");
var IXConnFactory = Java.type("de.elo.ix.client.IXConnFactory");
var IXConnection = Java.type("de.elo.ix.client.IXConnection");
var FindInfo = Java.type("de.elo.ix.client.FindInfo");
var FindChildren = Java.type("de.elo.ix.client.FindChildren");
var FindResult = Java.type("de.elo.ix.client.FindResult");
var Sord = Java.type("de.elo.ix.client.Sord");
var SordC = Java.type("de.elo.ix.client.SordC");
var CheckoutUsersC = Java.type("de.elo.ix.client.CheckoutUsersC");
var LockC = Java.type("de.elo.ix.client.LockC");
var DocMaskC = Java.type("de.elo.ix.client.DocMaskC");
var Document = Java.type("de.elo.ix.client.Document");
var DocVersion = Java.type("de.elo.ix.client.DocVersion");
var EditInfo = Java.type("de.elo.ix.client.EditInfo");
var EditInfoC = Java.type("de.elo.ix.client.EditInfoC");
var ClientInfo = Java.type("de.elo.ix.client.ClientInfo");
var UserInfoC = Java.type("de.elo.ix.client.UserInfoC");

var Dispatch = Java.type("com.wilutions.com.Dispatch");

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

	url : "http://srvpelo1:6080/ix-lldo_prod/ix",
	credentials : "",
	projectNames : "",
	msgFileType : ".msg",

	// Property IDs of configuration data.
	// IDs are member names to simplify function fromProperties
	PROPERTY_ID_URL : "url",
	PROPERTY_ID_PROJECT_NAMES : "projectNames",
	PROPERTY_ID_MSG_FILE_TYPE : "msgFileType",
	PROPERTY_ID_CREDENTIALS : "credentials",

	toProperties : function() {
		return [
				new Property(this.PROPERTY_ID_URL, this.url),
				new Property(this.PROPERTY_ID_CREDENTIALS, this.credentials),
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

};

// IX-Connection, created in initialize()
var conn = null;

function checkValid() {
	if (conn == null) {
		throw new IOException("Not connected to Indexserver");
	}
}

var data = {

	/**
	 * Map of projects. Key: project ID, value: project.
	 * this.projects[.].members contains an array of project members
	 * (assignees). this.projects[.].versions contains an array of versions
	 * (milestones).
	 */
	todoProjects : {},
	ttsProjects : {},

	/**
	 * Array of IdName objects for users.
	 */
	users : [],
	
	clear : function() {
		this.todoProjects = {};
		this.ttsProjects = {};
		this.users = [];
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

function getSordsInFolder(parentId) {
	var ret = [];
	var fi = new FindInfo();
	fi.findChildren = new FindChildren();
	fi.findChildren.parentId = parentId;
	var fr = conn.ix().findFirstSords(fi, 100, SordC.mbAll);
	while (true) {
		for (var i = 0; i < fr.sords.length; i++) {
			ret.push(fr.sords[i]);
		}
		if (!fr.moreResults) {
			break;
		}
		fr = conn.ix().findNextSords(fi.searchId, ret.length, 100, SordC.mbAll);
	}
	conn.ix().findClose(fi.searchId);
	return ret;
}

function isMe(userName) {
	log.info("isMe(" + userName);
	var myUserInfo = conn.getLoginResult().getUser();
	ret = myUserInfo.name == userName;
	for (var i = 0; !ret && i < myUserInfo.userProps.length; i++) {
		var s = myUserInfo.userProps[i];
		ret = userName == s;
		log.info("s=" + s + "==" + userName);
	}
	log.info(")isMe=" + ret);
	return ret;
}

function getProjectUserNames(sord) {
	log.info("getProjectUserNames(" + sord + ", desc=" + sord.desc);
	var ret = [];
	var descLines = sord.desc.split("\n");
	for (var lineIdx = 0; lineIdx < descLines.length; lineIdx++) {
		var desc = descLines[lineIdx].trim();
		var idx = -1;
		if (desc.indexOf("users=") == 0) {
			idx = 6;
		} else if (desc.indexOf("user=") == 0) {
			idx = 5;
		}

		if (idx >= 0) {
			var userList = desc.substring(idx);
			ret = userList.split("|");
			break;
		}
	}
	ret = ret.sort();
	log.info(")getProjectUserNames=" + ret);
	return ret;
}

function getProjectUserNamesQS(sord) {
	log.info("getProjectUserNamesQS(" + sord + ", desc=" + sord.desc);
	var ret = [];
	var descLines = sord.desc.split("\n");
	for (var lineIdx = 0; lineIdx < descLines.length; lineIdx++) {
		var desc = descLines[lineIdx].trim();
		var idx = -1;
		if (desc.indexOf("qsuser=") == 0) {
			idx = 7;
			var userList = desc.substring(idx);
			ret = userList.split("|");
			break;
		}
	}
	ret = ret.sort();
	log.info(")getProjectUserNamesQS=" + ret);
	return ret;
}

function isMyProject(sord) {
	log.info("isMyProject(" + sord);
	var isMyProject = false;
	var userNames = getProjectUserNames(sord);
	var userNamesQS = getProjectUserNamesQS(sord);
	var projectUsers = userNames.concat(userNamesQS);
	for (var i = 0; !isMyProject && i < projectUsers.length; i++) {
		isMyProject = isMe(projectUsers[i]);
	}
	log.info(")isMyProject=" + isMyProject);
	return isMyProject;
}

function makeProjectFromSord(sord) {
	var project = {
		"id" : sord.id,
		"name" : ("" + sord.name),
		"sord" : sord
	};
	readProjectMembers(project);

	return project;
}

function readProjects(data) {
	log.info("readProjects(");

	var myUserInfo = conn.getLoginResult().getUser();

	var todoProjectSords = getSordsInFolder("ARCPATH:/Administration/ToDoConfig/Project");

	data.todoProjects = {};
	for (var i = 0; i < todoProjectSords.length; i++) {
		var sord = todoProjectSords[i];
		var myproj = isMyProject(sord);
		log.info("sord=" + sord + ", isMyProject=" + myproj + ", desc="
				+ sord.desc);
		if (myproj) {
			log.info("add todo project=" + sord);
			data.todoProjects["" + sord.getId()] = makeProjectFromSord(sord);
		}
	}

	var ttsProjectSords = getSordsInFolder("ARCPATH:/Administration/ttsConfig");

	data.ttsProjects = {};
	for (var i = 0; i < ttsProjectSords.length; i++) {
		var sord = ttsProjectSords[i];
		if (sord.name == "Java Server") {
			continue;
		}

		var myproj = isMyProject(sord);
		log.info("sord=" + sord + ", isMyProject=" + myproj + ", desc="
				+ sord.desc);
		if (myproj) {
			log.info("add tts project=" + sord);
			data.ttsProjects["" + sord.getId()] = makeProjectFromSord(sord);
		}
	}

	var maskBug = conn.ix().checkoutDocMask(DOCMASK_BUG, DocMaskC.mbAll,
			LockC.NO);
	log.info("maskBug=" + maskBug);
	for (var i = 0; i < maskBug.lines.length; i++) {
		var line = maskBug.lines[i];
		log.info("  line.key=" + line.key + ", line.name=" + line.name);
	}

	var maskTodo = conn.ix().checkoutDocMask(DOCMASK_TODO, DocMaskC.mbAll,
			LockC.NO);
	log.info("maskTodo=" + maskTodo);
	for (var i = 0; i < maskTodo.lines.length; i++) {
		var line = maskTodo.lines[i];
		log.info("  line.key=" + line.key + ", line.name=" + line.name);
	}

	log.info(")readProjects");
};

function readProjectVersions(project) {
	log.info("readProjectVersions(project.id=" + project.id);
	project.versions = [ "1.0", "2.0" ];
	log.info(")readProjectVersions");
}

function getUserIdName(userName) {
	var ret = null;
	userName = userName.toLowerCase();
	for (var i = 0; i < data.users.length; i++) {
		var idn = data.users[i];
		if (idn.name.toLowerCase().equals(userName)) {
			ret = idn;
		}
	}	
	return ret;
}

function readProjectMembers(project) {
	log.info("readProjectMembers(project.id=" + project.id);
	checkValid();

	var userNames = getProjectUserNames(project.sord);

	project.members = [];
	for (var i = 0; i < userNames.length; i++) {
		var idn = getUserIdName(userNames[i]);
		if (idn) {
			project.members.push(idn);
		}
	}

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
		} catch (ex) {
		}
	}

	var computerName = InetAddress.getLocalHost().getHostName();
	var connFact = new IXConnFactory(config.url,
			"ELO Issue Tracker for Outlook", "1.0");
	
	
	if (config.credentials) {
		var p = config.credentials.indexOf(";");
		if (p >= 0) {
			var userName = config.credentials.substring(0, p);
			var password = config.credentials.substring(p+1);
			conn = connFact.create(userName, password, computerName, "");
		}
		else {
			var ci = new ClientInfo();
			ci.ticket = config.credentials;
			conn = connFact.createFromTicket(ci);
		}
	} else {
		conn = connFact.createSso(computerName);
	}
	
	var userInfos = conn.ix().checkoutUsers(null, CheckoutUsersC.ALL_USERS, LockC.NO);
	for (var i = 0; i < userInfos.length; i++) {
		var idn = new IdName(userInfos[i].id, userInfos[i].name);
		data.users.push(idn);
		var idn = new IdName(userInfos[i].id, userInfos[i].userProps[UserInfoC.PROP_NAME_OS]);
		data.users.push(idn);
		log.info("user id=" + idn.id + ", name=" + idn.name);
	}

	readProjects(data);

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
			config.PROPERTY_ID_MSG_FILE_TYPE, "Attach mail as");
	propertyClasses.add(PropertyClass.TYPE_STRING,
			config.PROPERTY_ID_CREDENTIALS, "Credentials");

	var propMsgFileType = propertyClasses.get(config.PROPERTY_ID_MSG_FILE_TYPE);
	propMsgFileType.setSelectList([ new IdName(".msg", "Outlook (.msg)"),
			new IdName(".mhtml", "MIME HTML (.mhtml)"),
			new IdName(".rtf", "Rich Text Format (.rtf)") ]);

	// Initialize select list for some issue properties

	var propIssueType = propertyClasses.get(Property.ISSUE_TYPE);
	propIssueType.setSelectList([ new IdName(ISSUE_TYPE_BUG, "Bug"),
			new IdName(ISSUE_TYPE_TODO, "Todo") ]);

	var propPriority = propertyClasses.get(Property.PRIORITY);
	propPriority.setSelectList([ new IdName(PRIORITY_A, "Priorität A"),
			new IdName(PRIORITY_B, "Priorität B"),
			new IdName(PRIORITY_C, "Prioriät C") ]);

	var propIssueStatus = propertyClasses.get(Property.STATUS);
	propIssueStatus.setSelectList([ new IdName(1, "Neuer Eintrag"),
	// Field status_id seems to be ignored when creating a new issue.
	// Although it works on the web page.
	// new IdName(2, "In progress"), new IdName(3, "Resolved"),
	// new IdName(4, "Feedback"), new IdName(5, "Closed"),
	// new IdName(6, "Rejected")
	]);

//	var propMilestones = propertyClasses.get(Property.MILESTONES);
//	propMilestones.setName("Versionen");

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
		ret.selectList = getPriorities(issue);
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
		ret.selectList = getIssueStatuses(issue);
		break;
	default:
		break;
	}
	return ret;
}

function getPropertyDisplayOrder() {
	var propertyIds = [ Property.ASSIGNEE ];
	return propertyIds;
}

function getIssueTypes(issue) {
	return getPropertyClasses().get(Property.ISSUE_TYPE).getSelectList();
};

function getPriorities(issue) {
	return getPropertyClasses().get(Property.PRIORITY).getSelectList();
};

function getCategories(issue) {
	log.info("getCategories(");
	var ret = [];
	var projects = issue.getType() == ISSUE_TYPE_BUG ? data.ttsProjects
			: data.todoProjects;
	for ( var projectId in projects) {
		log.info("projectId=" + projectId);
		var project = projects[projectId];
		var idn = new IdName(projectId, project.name);
		ret.push(idn);
	}
	log.info(")getCategories=" + ret);
	return ret;
};

function getMilestones(issue) {
	var ret = [];
	return ret;
};

function getAssignees(issue) {
	log.info("getAssignees(");
	var ret = [ new IdName(-1, "Unassigned") ];
	var projectId = issue ? issue.getCategory() : 0;
	log.info("projectId=" + projectId);
	var project = data.todoProjects[projectId];
	if (!project) {
		project = data.ttsProjects[projectId];
	}
	log.info("project=" + project);
	if (project) {
		ret = project.members;
	}
	log.info(")getAssignees=" + ret);
	return ret;
};

function getCurrentUser() {
	return conn ? new IdName(conn.getUserId(), conn.getUserName())
			: new IdName(0, "");
};

function getIssueStatuses(iss) {
	return getPropertyClasses().get(Property.STATUS).getSelectList();
};

function getHtmlEditor(issue, propertyId) {
	return null;
}

function getDescriptionTextEditor(issue) {
	// var isBug = issue.getType() == ISSUE_TYPE_BUG;
	// return new DescriptionTextEditor(issue.getDescription());
	return null;
}

function getObjIdExprForIssueId(issueId) {
	var isBug = issueId.indexOf("TTS") == 0;
	var objId = "OKEY:";
	objId += isBug ? "EFS_NR" : "TODOID";
	objId += "=" + issueId
	return objId;
}

function getShowIssueUrl(issueId) {
	var objId = getObjIdExprForIssueId(issueId);
	var folder = conn.ix().checkoutSord(objId, SordC.mbLean,
			LockC.NO);

	var arc = conn.ix().checkoutSord("1", SordC.mbMin, LockC.NO);

	// EP
	// Alldo_prod
	// G(D2DBEEE1-F1B7-8B54-E8AD-EC49502365A0)
	// I5442908
	// WTOP
	// T
	var CR = "\r\n";
	var ecd = "EP" + CR + "A" + arc.name + CR + "G" + folder.guid + CR + "I"
			+ folder.id + CR + "WTOP" + CR + "T" + CR;

	folder.desc = ecd; // trick: brauche einen java.lang.String für write()

	var tempEcd = File.createTempFile(issueId, ".ecd");
	var wr = new OutputStreamWriter(new FileOutputStream(tempEcd));
	wr.write(folder.desc.toCharArray());
	wr.close();

	var fname = tempEcd.getAbsolutePath();
	fname = fname.replace(/\\\\/, "/");
	return "file:////" + fname;
}

function getMsgFileType() {
	return config.msgFileType;
}

// "<style>p.MsoTitle
// '{'font-size:26.0pt;font-family:\"Arial\";color:#17365D;'}'</style>" +
// "</head><body contenteditable=\"true\">" +
// "<p class=\"MsoTitle\">Problem/Fehler</p>" +
// "<hr>" +

var descTemplateBug = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
		+ "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">"
		+ "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"de\">"
		+ "<head></head><body>\n"
		+ "<h1>Beschreibung / Reproduktionsschritte:</h1>\n" + "<hr>\n"
		+ "<p><br/><pre>{0}</pre><br/></p>" + "<hr>\n" + "<h1>Analyse:</h1>\n"
		+ "<p>&lt;Entwickler: Wie ist es zu diesem Problem gekommen?&gt;</p>\n"
		+ "<h1>Korrektur / Lösung:</h1>\n"
		+ "<p>&lt;Entwickler: Wie wurde das Problem gelöst&gt;</p>\n"
		+ "</body></html>\n";

var descTemplateTodo = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
		+ "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">"
		+ "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"de\">"
		+ "<head></head><body>\n" + "<h1>Beschreibung</h1>\n" + "<hr>\n"
		+ "<p><br/><pre>{0}</pre><br/></p>" + "<hr>\n" + "</body></html>\n";

function getIssueDescriptionFromMailBody(issueType, mailbody) {
	var isBug = issueType == ISSUE_TYPE_BUG;
	var descTemplate = isBug ? descTemplateBug : descTemplateTodo;

	if (mailbody.length > 200) {
		mailbody = mailbody.substring(0, 197) + "...";
	}

	var issueDescription = MessageFormat.format(descTemplate, mailbody);
	return issueDescription;
};

function createIssue(subject, mailDescription) {
	config.checkValid();

	subject = stripIssueIdFromSubject(subject);

	if (subject.indexOf("AW:") == 0) {
		subject = subject.substring(3).trim();
	} else if (subject.indexOf("WG:") == 0) {
		subject = subject.substring(3).trim();
	}

	var issueDescription = getIssueDescriptionFromMailBody(ISSUE_TYPE_BUG,
			mailDescription);

	var iss = new Issue();

	iss.setSubject(subject);
	iss.setDescription(issueDescription);
	iss.setType(ISSUE_TYPE_BUG); // Bug
	iss.setPriority(PRIORITY_B); // Normal priority
	iss.setStatus(1); // New issue

	var projects = getCategories(iss);
	if (projects) {
		iss.setCategory(projects[0].getId());
	}

	iss.setAssignee(conn.getUserId());

	return iss;
};

function saveHtmlDescriptionAsTempDoc(description, progressCallback) {

	var pgSave = null;
	if (progressCallback) {
		if (progressCallback.isCancelled())
			return null;
		var str = "Save description";
		pgSave = progressCallback.createChild(str);
		pgSave.setTotal(10000);
	}

	try {
		var htmlFile = File.createTempFile("issue", ".html");
		var fos = new FileOutputStream(htmlFile);
		fos.write(0xEF);
		fos.write(0xBB);
		fos.write(0xBF);
		var wr = new OutputStreamWriter(fos, "UTF-8");
		wr.write(description.toCharArray());
		wr.close();
		if (pgSave) {
			if (pgSave.isCancelled())
				return null;
			pgSave.setProgress(1000);
		}

		var word = new Dispatch("Word.Application");
		pgSave.setProgress(3000);
		var docs = word._get("Documents");
		var doc = docs._call("Open", htmlFile.getAbsolutePath());
		var docFile = File.createTempFile("issue", ".docx");
		if (pgSave) {
			if (pgSave.isCancelled())
				return null;
			pgSave.setProgress(6000);
		}

		// Format Nummern
		// https://msdn.microsoft.com/en-us/library/bb238158%28v=office.12%29.aspx
		var wdFormatXMLDocument = 12;
		doc._call("SaveAs", docFile.getAbsolutePath(), new Integer(
				wdFormatXMLDocument));

	} finally {
		if (htmlFile) {
			htmlFile["delete"]();
		}
		if (doc) {
			try {
				doc._call("Close");
			} catch (ignore) {
			}
		}
		if (word) {
			try {
				word._call("Quit");
			} catch (ignore) {
			}
		}
		if (pgSave) {
			pgSave.setFinished();
		}
	}

	return docFile;
}

function getIssueId(issueType) {
	var trackerId = "";
	var isBug = issueType == ISSUE_TYPE_BUG;
	var counterName = isBug ? "TTS_ID" : "PMREQID";

	var id = conn.ix().checkoutCounters([ counterName ], true, LockC.NO)[0].value;

	if (isBug) {
		var sid = "00000" + id;
		if (sid.length > 6) {
			sid = sid.substring(sid.length - 6);
		}
		trackerId = "TTS" + sid;
	} else {
		trackerId = "RID" + id;
	}

	return trackerId;
}

function setObjKeysForTodo(folder, issue) {
	log.info("setObjKeysForTodo(" + folder.name);

	var project = data.todoProjects[issue.getCategory()];

	var assigneeName = conn.ix().getUserNames([ issue.getAssignee() ],
			CheckoutUsersC.BY_IDS)[0].name;
	log.info("assigneeName=" + assigneeName);

	for (var i = 0; i < folder.objKeys.length; i++) {
		var okey = folder.objKeys[i];
		log.info("objkeys=" + okey);
		var value = "";
		switch (okey.name) {
		case "TODOPROJ":
			value = project.name;
			break;
		case "TODOSTATUS":
			value = getPrioAsString(issue);
			break;
		case "TODOID":
			value = issue.getId();
			break;
		case "TODOW1":
			value = assigneeName;
			break;
		}

		if (value) {
			okey.data = [ value ];
			log.info("objKey[" + okey.name + "]=" + value);
		}

	}

	log.info(")setObjKeysForTodo");
}

function getIssuePropsFromObjKeys(folder, issue) {
	log.info("getIssuePropsFromObjKeys(" + folder.name);
	if (folder && folder.objKeys) {
		for (var i = 0; i < folder.objKeys.length; i++) {
			var okey = folder.objKeys[i];
			var value = (okey.data && okey.data.lenth) ? okey.data[0] : null;
			if (value) {
				switch (okey.name) {
				case "TODOPROJ":
					for (var projectId in data.todoProjects) {
						var project = data.todoProjects[projectId];
						if (project && project.name && project.name == value) {
							issue.setCategory(projectId);
							break;
						}
					}
					break;
				case "TODOSTATUS":
					issue.setPriority(getPriorityIdFromName(value));
					break;
				case "TODOID":
					issue.setId(value);
					break;
				case "TODOW1":
					for (var u = 0; u < data.users.length; u++) {
						var idn = data.users[u];
						if (value.equals(idn.name)) {
							issue.setAssignee(idn.id);
							break;
						}
					}
					break;
				}
			}
		}
	}
	log.info(")getIssuePropsFromObjKeys");
}

function getPrioAsString(issue) {
	log.info("getPrioAsString(type=" + issue.getType() + ", prio="
			+ issue.getPriority());
	var value = "";
	var prio = parseInt(issue.getPriority());
	if (issue.getType() == ISSUE_TYPE_BUG) {
		switch (prio) {
		case PRIORITY_A:
			value = "Prio A: sofort zu bearbeiten";
			break;
		case PRIORITY_B:
			value = "Prio B: zum nächsten Release";
			break;
		case PRIORITY_C:
			value = "Prio C: ohne festen Termin";
			break;
		}
	} else {
		switch (prio) {
		case PRIORITY_A:
			value = "Prio A";
			break;
		case PRIORITY_B:
			value = "Prio B";
			break;
		case PRIORITY_C:
			value = "Prio C";
			break;
		}
	}
	log.info(")getPrioAsString=" + value);
	return value;
}

function getPriorityIdFromName(prioName) {
	var ret = PRIORITY_B;
	if (prioName.indexOf("Prio A") >= 0) {
		ret = PRIORITY_A;
	}
	else if (prioName.indexOf("Prio B") >= 0) {
		ret = PRIORITY_B;
	}
	else if (prioName.indexOf("Prio C") >= 0) {
		ret = PRIORITY_C;
	}
	return ret;
}

function setObjKeysForBug(folder, issue) {
	log.info("setObjKeysForBug(" + folder.name);

	var project = data.ttsProjects[issue.getCategory()];

	var assigneeName = conn.ix().getUserNames([ issue.getAssignee() ],
			CheckoutUsersC.BY_IDS)[0].name;
	log.info("assigneeName=" + assigneeName);

	for (var i = 0; i < folder.objKeys.length; i++) {
		var okey = folder.objKeys[i];
		var value = "";
		log.info("okey.name=" + okey.name);
		switch (okey.name) {
		case "EFS_PROD": // line.name=Produkt
			value = project.name;
			break;
		case "EFS_KAT": // line.name=Kategorie
			value = getPrioAsString(issue);
			break;
		case "EFS_OPEN": // line.name=Geöffnet
			value = conn.dateToIso(new JavaDate(System.currentTimeMillis()));
			break;
		case "EFS_DONE": // line.name=Erledigt
			break;
		case "EFS_CLOSED": // line.name=Geschlossen
			break;
		case "EFS_VEROPEN": // line.name=Version aufgefallen
			break;
		case "EFS_VERCLOSED": // line.name=Version erledigt
			break;
		case "EFS_NR": // line.name=Tracking Nr.
			value = issue.getId();
			break;
		case "EFS_TYPE": // line.name=Aufgabentyp
			break;
		case "EFS_QSUSER": // line.name=QS Verantwortlicher
			break;
		case "EFS_USER": // line.name=Ausführender
			value = assigneeName;
			break;
		case "EFS_COMMENT": // line.name=Letzter Kommentar
			break;
		case "EFS_MOVETO": // line.name=Übertragen an
			break;
		case "EFS_CHECKBY": // line.name=Prüfung starten
			break;
		case "EFS_STATECHG": // line.name=Zeitstempel
			break;
		}

		if (value) {
			okey.data = [ value ];
			log.info("objKey[" + okey.name + "]=" + value);
		}
	}

	log.info(")setObjKeysForBug");
}

function getIssueParentId(issue) {
	log.info("getIssueParentId(");

	var isBug = issue.getType() == ISSUE_TYPE_BUG;
	var prioName = getPrioAsString(issue);

	var projects = isBug ? data.ttsProjects : data.todoProjects;
	var project = projects[issue.getCategory()];
	dump("project", project);

	var parentId = "";
	if (isBug) {
		parentId = "ARCPATH:/07.  Entwicklung/TTS/" + project.name + "/"
				+ prioName;
	} else {
		parentId = "ARCPATH:/07.  Entwicklung/Public/ToDo/" + project.name
				+ "/" + prioName;
	}

	log.info(")getIssueParentId=" + parentId);
	return parentId;
}

function getFileExt(fname) {
	var ext = "";
	var p = fname.lastIndexOf(".");
	if (p >= 0) {
		ext = fname.substring(p);
	}
	return ext;
}

function getFileNameWithoutExt(fname) {
	var p = fname.lastIndexOf(".");
	if (p >= 0) {
		fname = fname.substring(0, p);
	}
	return fname;
}

function uploadAttachment(parentId, attachment, progressCallback) {
	log.info("uploadAttachment(parentId=" + parentId + ", attachment="
			+ attachment);

	var fileSize = attachment.getContentLength();

	var pgUpload = null;
	if (progressCallback) {
		if (progressCallback.isCancelled())
			return null;
		var str = "Upload attachment " + attachment.getFileName();
		str += ", " + makeAttachmentSizeString(fileSize);
		pgUpload = progressCallback.createChild(str);
		pgUpload.setTotal(fileSize);
	}

	try {
		var ed = conn.ix().createDoc(parentId, "", "", EditInfoC.mbSordDocAtt);
		ed.sord.name = getFileNameWithoutExt(attachment.getFileName());

		var dv = new DocVersion();
		dv.ext = getFileExt(attachment.getFileName());
		dv.pathId = ed.sord.path;
		ed.document.docs = [ dv ];

		ed.document = conn.ix().checkinDocBegin(ed.document);
		var url = ed.document.docs[0].url;
		log.info("url=" + url);

		if (pgUpload) {
			if (pgUpload.isCancelled())
				return null;
			pgUpload.setProgress(fileSize / 3);
		}

		var uploadResult = conn.upload(url, attachment.getStream(), fileSize,
				attachment.getContentType());
		log.info("uploadResult=" + uploadResult);
		ed.document.docs[0].uploadResult = uploadResult;
		pgUpload.setProgress(2 * fileSize / 3);

		var objId = conn.ix().checkinDocEnd(ed.sord, SordC.mbAll, ed.document,
				LockC.NO);
		log.info("objId=" + objId);
	} finally {
		if (pgUpload) {
			pgUpload.setFinished();
		}
	}

	log.info(")uploadAttachment=" + objId);
	return objId;
}

function getTodoDescFromIssue(issue) {
	log.info("getTodoDescFromIssue(");
	var ret = "Beschreibung s. Dokument";

	var desc = issue.getDescription();
	log.info("desc=" + desc);

	var p = desc.indexOf("<hr>");
	log.info("hr at " + p);
	if (p >= 0) {
		p += 5;
		var q = desc.indexOf("<hr>", p);
		log.info("hr at " + q);
		if (q >= 0) {
			var str = desc.substring(p, q);
			log.info("str=" + str);
			// http://stackoverflow.com/questions/822452/strip-html-from-text-javascript
			str = str.replace(/<(?:.|\n)*?>/gm, '');
			ret = str;
		}
	}
	log.info(")getTodoDescFromIssue=" + ret);
	return ret;
}

function updateIssue(issue, progressCallback) {
	log.info("updateIssue(issue=" + issue + ", progressCallback="
			+ progressCallback);
	config.checkValid();

	var isBug = issue.getType() == ISSUE_TYPE_BUG;
	var maskId = isBug ? DOCMASK_BUG : DOCMASK_TODO;

	var issueId = getIssueId(issue.getType());
	issue.setId(issueId);
	log.info("issueId=" + issueId);

	var parentId = getIssueParentId(issue);
	log.info("parentId=" + parentId);

	try {

		var folder = conn.ix().createSord(parentId, maskId, SordC.mbAll);

		if (isBug) {
			folder.name = issueId + ": " + issue.getSubject();
			folder.desc = "##*\r\n";
			setObjKeysForBug(folder, issue);
		} else {
			folder.name = issue.getSubject();
			folder.desc = getTodoDescFromIssue(issue);
			setObjKeysForTodo(folder, issue);
		}

		folder.id = conn.ix().checkinSord(folder, SordC.mbAll, LockC.NO);
		log.info("folder=" + folder);

		var docFile = saveHtmlDescriptionAsTempDoc(issue.getDescription(),
				progressCallback);
		var descriptionAttachment = new Attachment();
		descriptionAttachment.setFileName("Beschreibung.docx");
		descriptionAttachment.setStream(new FileInputStream(docFile));
		descriptionAttachment.setContentType("application/docx");
		descriptionAttachment.setContentLength(docFile.length());
		uploadAttachment(folder.id, descriptionAttachment, progressCallback);

		for (var i = 0; i < issue.getAttachments().size(); i++) {
			var attachment = issue.getAttachments().get(i);
			uploadAttachment(folder.id, attachment, progressCallback);
		}
	} catch (ex) {
		if (folder && folder.id) {
			try {
				conn.ix().deleteSord("", folder.id, LockC.NO, null);
			} catch (ignore) {
			}
		}
		throw ex;
	} finally {
	}

	return issue;
};

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
	log.info("validateIssue(");
	var ret = iss;
	if (iss.getType() == ISSUE_TYPE_TODO) {
		var description = iss.getDescription();
		var wasBug = description
				.indexOf("<h1>Beschreibung / Reproduktionsschritte:</h1>") >= 0;
		log.info("issue.type=" + iss.getType() + ", wasBug=" + wasBug);
		if (wasBug) {
			description = getIssueDescriptionFromMailBody(ISSUE_TYPE_TODO, "");
			iss.setDescription(description);
		}
	} else {
		var description = iss.getDescription();
		var wasTodo = description.indexOf("<h1>Beschreibung</h1>") >= 0;
		log.info("issue.type=" + iss.getType() + ", wasTodo=" + wasTodo);
		if (wasTodo) {
			description = getIssueDescriptionFromMailBody(ISSUE_TYPE_BUG, "");
			iss.setDescription(description);
		}
	}
	log.info(")validateIssue");
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
	var startTag = "[TTS";
	var p = subject.indexOf(startTag);
	if (p < 0) {
		startTag = "[RID";
		p = subject.indexOf(startTag);
	}
	if (p >= 0) {
		var q = subject.indexOf("]", p);
		if (q >= 0) {
			issueId = subject.substring(p + 1, q);
		}
	}
	return issueId;
};

function stripFirstIssueIdFromSubject(subject) {
	var startTag = "[TTS";
	var p = subject.indexOf(startTag);
	if (p < 0) {
		startTag = "[RID";
		p = subject.indexOf(startTag);
	}
	if (p >= 0) {
		var q = subject.indexOf("]", p);
		if (q >= 0) {
			subject = subject.substring(q + 1).trim();
		}
	}
	return subject;
}

function stripIssueIdFromSubject(mailSubject) {
	var subject = stripFirstIssueIdFromSubject(mailSubject);
	while (subject != mailSubject) {
		mailSubject = subject;
		subject = stripFirstIssueIdFromSubject(subject);
	}
	return subject;
}

function injectIssueIdIntoMailSubject(mailSubject, iss) {
	var subject = stripIssueIdFromSubject(mailSubject);
	subject = "[" + iss.getId() + "] " + subject.trim();
	return subject;
};

//maskBug=[61,(AB1EABAD-016E-4E83-EC68-CD3AE01DCE61),TrackItem,#lines=15]
//line.key=EFS_PROD, line.name=Produkt
//line.key=EFS_KAT, line.name=Kategorie
//line.key=EFS_OPEN, line.name=Geöffnet
//line.key=EFS_DONE, line.name=Erledigt
//line.key=EFS_CLOSED, line.name=Geschlossen
//line.key=EFS_VEROPEN, line.name=Version aufgefallen
//line.key=EFS_VERCLOSED, line.name=Version erledigt
//line.key=EFS_NR, line.name=Tracking Nr.
//line.key=EFS_TYPE, line.name=Aufgabentyp
//line.key=EFS_QSUSER, line.name=QS Verantwortlicher
//line.key=EFS_USER, line.name=Ausführender
//line.key=EFS_COMMENT, line.name=Letzter Kommentar
//line.key=EFS_MOVETO, line.name=Übertragen an
//line.key=EFS_CHECKBY, line.name=Prüfung starten
//line.key=EFS_STATECHG, line.name=Zeitstempel
//maskTodo=[60,(5B0E5986-5B28-71A4-6C66-3F4DF61C9526),ToDoItem,#lines=22]
//line.key=TODOOWNER, line.name=Einreicher
//line.key=TODOSTATUS, line.name=Status
//line.key=TODOW1, line.name=Bearbeiter 1
//line.key=TODOW2, line.name=Aktiviert durch
//line.key=TODOW3, line.name=Beended durch
//line.key=TODODONE, line.name=Abgeschlossen
//line.key=TODOVOTE, line.name=Vote
//line.key=TODOSTART, line.name=Start in Arbeit
//line.key=TODOEXTERN, line.name=Extern
//line.key=TODOPRIO, line.name=Priorität
//line.key=TODOPMANF, line.name=Status Anf
//line.key=TODOPMUMS, line.name=Status Ums
//line.key=TODOPMRBEZ, line.name=Release Bez
//line.key=TODOPMAUFW, line.name=Aufwand
//line.key=TODOPMPRIO, line.name=Priorität
//line.key=TODOPMTHEMA, line.name=Thema
//line.key=TODOPMMOD, line.name=Module
//line.key=TODOPMREAL, line.name=Realisierung
//line.key=TODOPMVOTE, line.name=Voting
//line.key=TODOID, line.name=Item ID
//line.key=TODOPROJ, line.name=Projekt
//line.key=TODOPMTYPE, line.name=PM Typ

function readIssue(issueId) {
	log.info("readIssue(" + issueId);
	var issue = new Issue();
	
	var objId = getObjIdExprForIssueId(issueId);
	var folder = conn.ix().checkoutSord(objId, SordC.mbAll, LockC.NO);

	var isBug = issueId.indexOf("TTS") == 0;
	
	
	
	
	
	log.info(")readIssue=" + issue);
	return issue;
};

function getIssueHistoryUrl(issueId) {
	return config.url;
}

initializePropertyClasses();
