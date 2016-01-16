package com.wilutions.redmineaddin;

import java.util.ResourceBundle;

import com.wilutions.com.AsyncResult;
import com.wilutions.com.CoClass;
import com.wilutions.itol.Globals;
import com.wilutions.itol.ItolAddin;
import com.wilutions.joa.DeclAddin;
import com.wilutions.joa.LoadBehavior;
import com.wilutions.joa.OfficeApplication;
import com.wilutions.joa.outlook.ex.Wrapper;
import com.wilutions.mslib.office.IRibbonControl;

@CoClass(progId = "ItolRedmineAddin.Class", guid = "{c65f0704-7e6b-463a-8372-03718f3ff93e}")
@DeclAddin(application = OfficeApplication.Outlook, loadBehavior = LoadBehavior.LoadOnStart, friendlyName = "Issue Tracker Addin for Redmine", description = "Issue Tracker Addin for Microsoft Outlook and Redmine")
public class RedmineAddin extends ItolAddin {

	private ResourceBundle resb = Globals.getResourceBundle();

	public RedmineAddin() {
	}
	void x() {}

	@Override
	public String GetCustomUI(String ribbonId) {
		return super.GetCustomUI(ribbonId);
	}
	
//	@Override
//	protected void onConnect(Wrapper context, AsyncResult<Boolean> asyncResult) {
//		DlgConnect dlg = new DlgConnect();
//		Object owner = context.getWrappedObject();
//		dlg.showAsync(owner, asyncResult);
//	}

}
