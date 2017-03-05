package com.wilutions.itol;

import java.util.Arrays;
import java.util.List;

public class About3rdPartyLib {
	
	private String name;
	private String license;
	private String licenseUrl;
	
	public About3rdPartyLib() {
	}
	
	public About3rdPartyLib(String name, String license, String licenseUrl) {
		this.setName(name);
		this.setLicense(license);
		this.licenseUrl = licenseUrl;
	}
	
	void showLicense() {
		IssueApplication.showDocument(licenseUrl);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	static final List<About3rdPartyLib> LIBS = Arrays.asList(
			new About3rdPartyLib("GSON", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
			new About3rdPartyLib("ControlsFX", "BSD-3-Clause", "https://opensource.org/licenses/BSD-3-Clause"),
			new About3rdPartyLib("CenterDevice JavaFxSVG","BSD-3-Clause", "https://opensource.org/licenses/BSD-3-Clause"),
			new About3rdPartyLib("JXLayer","BSD-2-Clause", "https://opensource.org/licenses/BSD-2-Clause"),
			new About3rdPartyLib("SwingX","LGPL", "https://www.gnu.org/copyleft/lesser.html")
			);

}
