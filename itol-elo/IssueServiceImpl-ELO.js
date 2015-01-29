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

	url : "http://srvpelo1:9090/ix-lldo_prod/ix",
	projectNames : "",
	msgFileType : ".msg",

	// Property IDs of configuration data.
	// IDs are member names to simplify function fromProperties
	PROPERTY_ID_URL : "url",
	PROPERTY_ID_PROJECT_NAMES : "projectNames",
	PROPERTY_ID_MSG_FILE_TYPE : "msgFileType",

	toProperties : function() {
		return [
				new Property(this.PROPERTY_ID_URL, this.url),
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
		} else {

		}
	}
	log.info(")getProjectUserNames=" + ret);
	return ret;
}

function isMyProject(sord) {
	log.info("isMyProject(" + sord);
	var isMyProject = false;
	var userNames = getProjectUserNames(sord);
	for (var i = 0; !isMyProject && i < userNames.length; i++) {
		isMyProject = isMe(userNames[i]);
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

function readProjectMembers(project) {
	log.info("readProjectMembers(project.id=" + project.id);
	checkValid();

	var userNames = getProjectUserNames(project.sord);
	var userNameObjs = conn.ix().getUserNames(userNames.sort(),
			CheckoutUsersC.BY_IDS);

	project.members = [];
	for (var i = 0; i < userNameObjs.length; i++) {
		var idn = new IdName(userNameObjs[i].id, userNameObjs[i].name);
		project.members.push(idn);
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

	var connFact = new IXConnFactory(config.url,
			"ELO Issue Tracker for Outlook", "1.0");
	conn = connFact.createSso(InetAddress.getLocalHost().getHostName());

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

	var propIssueStatus = propertyClasses.get(Property.STATE);
	propIssueStatus.setSelectList([ new IdName(1, "Neuer Eintrag"),
	// Field status_id seems to be ignored when creating a new issue.
	// Although it works on the web page.
	// new IdName(2, "In progress"), new IdName(3, "Resolved"),
	// new IdName(4, "Feedback"), new IdName(5, "Closed"),
	// new IdName(6, "Rejected")
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
	var ret = [ new IdName(-1, "Unassigned") ];
	var projectId = issue ? issue.getCategory() : 0;
	var project = data.todoProjects[projectId];
	if (!project) {
		project = data.ttsProjects[projectId];
	}
	log.info("project=" + project);
	if (project) {
		ret = project.members;
	}
	return ret;
};

function getCurrentUser() {
	return conn ? new IdName(conn.getUserId(), conn.getUserName())
			: new IdName(0, "");
};

function getIssueStates(iss) {
	return getPropertyClasses().get(Property.STATE).getSelectList();
};

function getDetails(issue) {

}

function getDescriptionHtmlEditor(issue) {
	return null;
}

function getShowIssueUrl(issueId) {
	return "";
}

function getMsgFileType() {
	return config.msgFileType;
}

// "<style>p.MsoTitle
// '{'font-size:26.0pt;font-family:\"Arial\";color:#17365D;'}'</style>" +
// "</head><body contenteditable=\"true\">" +
// "<p class=\"MsoTitle\">Problem/Fehler</p>" +
// "<hr/>" +

var descTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
		+ "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">"
		+ "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"de\">"
		+ "<head></head><body>\n"
		+ "<h1>Beschreibung / Reproduktionsschritte:</h1>\n"
		+ "<p><pre>{0}</pre></p>" + "<h1>Analyse:</h1>\n"
		+ "<p>&lt;Entwickler: Wie ist es zu diesem Problem gekommen?&gt;</p>\n"
		+ "<h1>Korrektur / Lösung:</h1>\n"
		+ "<p>&lt;Entwickler: Wie wurde das Problem gelöst&gt;</p>\n"
		+ "</body></html>\n";

function createIssue(subject, mailDescription) {
	config.checkValid();

	if (mailDescription.length > 200) {
		mailDescription = mailDescription.substring(0, 197) + "...";
	}
	var issueDescription = MessageFormat.format(descTemplate, mailDescription);

	var iss = new Issue();

	iss.setSubject(subject);
	iss.setDescription(issueDescription);
	iss.setType(ISSUE_TYPE_BUG); // Bug
	iss.setPriority(PRIORITY_B); // Normal priority
	iss.setState(1); // New issue

	var projects = getCategories(iss);
	if (projects) {
		iss.setCategory(projects[0].getId());
	}

	iss.setAssignee(conn.getUserId());

	return iss;
};

function saveHtmlDescriptionAsTempDoc(description) {
	try {
		var htmlFile = File.createTempFile("issue", ".html");
		var fos = new FileOutputStream(htmlFile);
		fos.write(0xEF);
		fos.write(0xBB);
		fos.write(0xBF);
		var wr = new OutputStreamWriter(fos, "UTF-8");
		wr.write(description.toCharArray());
		wr.close();

		var word = new Dispatch("Word.Application");
		var docs = word._get("Documents");
		var doc = docs._call("Open", htmlFile.getAbsolutePath());
		var docFile = File.createTempFile("issue", ".docx");

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
			CheckoutUsersC.BY_IDS).name;
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

function uploadAttachment(folder, name, file) {
	
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

	var folder = conn.ix().createSord(parentId, maskId, SordC.mbAll);

	if (isBug) {
		folder.name = issueId + ": " + issue.getSubject();
		folder.desc = "##*\r\n";
		setObjKeysForBug(folder, issue);
	} else {
		folder.name = issue.getSubject();
		setObjKeysForTodo(folder, issue);
	}
	
	folder.id = conn.ix().checkinSord(folder, SordC.mbAll, LockC.NO);
	log.info("folder=" + folder);

	try {
		var ed = conn.ix().createDoc(folder.id, "", "", EditInfoC.mbSordDocAtt);
		ed.sord.name = "Beschreibung";
		
		var dv = new DocVersion();
		dv.ext = ".docx";
		dv.pathId = folder.path;
		ed.document.docs = [dv];
		
		ed.document = conn.ix().checkinDocBegin(ed.document);
		var url = ed.document.docs[0].url;
		log.info("url=" + url);
		
		var docFile = saveHtmlDescriptionAsTempDoc(issue.getDescription());
		log.info("docFile=" + docFile);
		var uploadResult = conn.upload(url, docFile);
		log.info("uploadResult=" + uploadResult);
		ed.document.docs[0].uploadResult = uploadResult;
		
		var objId = conn.ix().checkinDocEnd(ed.sord, SordC.mbAll, ed.document, LockC.NO);
		log.info("objId=" + objId);
	}
	finally {
		if (docFile) {
			docFile["delete"]();
		}
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

function toTrackerIssue(eloIssue, trackerIssue) {

	trackerIssue.setId(eloIssue.id);

	trackerIssue.setCategory(eloIssue.project.id);
	trackerIssue.setType(eloIssue.tracker.id);
	trackerIssue.setState(eloIssue.status.id);
	trackerIssue.setPriority(eloIssue.priority.id);
	trackerIssue.setSubject(eloIssue.subject);
	trackerIssue.setDescription(eloIssue.description);

	// trackerIssue.setAssignee(eloIssue.assigned_to.id);

	if (eloIssue.fixed_version_id) {
		trackerIssue.setMilestones([ eloIssue.fixed_version_id ]);
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
				issueId = str.substring(p + 1);
			} else {
				issueId = str;
			}
		}
	}
	return issueId;
};

function injectIssueIdIntoMailSubject(subject, iss) {
	// var ret = "[ITOL-";
	// switch (iss.getType()) {
	// case "1":
	// ret += "B";
	// break;
	// case "2":
	// ret += "F";
	// break;
	// case "3":
	// ret += "S";
	// break;
	// default:
	// sbuf.append("U");
	// break;
	// }
	//
	// ret += "-" + iss.getId() + "]";
	// ret += subject;
	//
	// return ret;

	return subject;
};

function readIssue(issueId) {
	return issues.get(issueId);
};

function readAttachment(attachmentId) {
	return null;
};

function writeAttachment(trackerAttachment, progressCallback) {
	log.info("writeAttachment(" + trackerAttachment + ", progressCallback="
			+ progressCallback);
	var content = trackerAttachment.getStream();

	var uploadResult = httpClient.upload("/uploads.json", content,
			trackerAttachment.getContentLength(), progressCallback);
	dump(uploadResult);

	trackerAttachment.setId(uploadResult.upload.token);

	var eloAttachment = {};
	eloAttachment.token = uploadResult.upload.token;
	eloAttachment.filename = trackerAttachment.getFileName();
	eloAttachment.content_type = trackerAttachment.getContentType();

	if (progressCallback) {
		progressCallback.setFinished();
	}

	log.info(")writeAttachment=" + eloAttachment);
	return eloAttachment;
};

function deleteAttachment(attachmentId) {
};

initializePropertyClasses();
