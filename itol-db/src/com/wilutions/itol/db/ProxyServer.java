package com.wilutions.itol.db;

import java.io.Serializable;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.logging.Logger;

/**
 * Proxy server configuration and initialization.
 *
 */
public class ProxyServer implements Serializable {
	
	private static final long serialVersionUID = 797602390235105742L;
	private final static Logger log = Logger.getLogger("ProxyServer");
	
	private String host;
	private boolean enabled; 
	private int port;
	private String userName = System.getProperty("user.name"); 
	private String encryptedPassword;
	
	public ProxyServer() {
	}
	
	protected void copyFrom(ProxyServer rhs) {
		this.host = rhs.host;
		this.enabled = rhs.enabled;
		this.port = rhs.port;
		this.userName = rhs.userName;
		this.encryptedPassword = rhs.encryptedPassword;
	}
	
	
	protected void unsetUserRelatedValues() {
		setUserName(null);
		setEncryptedPassword(null);
	}

	public void init() {
		
/*
 * http://www.pcwelt.de/ratgeber/Mit-Squid-als-Proxy-Server-schneller-im-Netzwerk-surfen-5896242.html
 * https://blog.mafr.de/2013/06/16/setting-up-a-web-proxy-with-squid/
 * 
 	
 	1. Installieren:
 	
 	1.1. Squid installieren
 	
 	sudo apt install squid
 	
 	1.2. Apache Hilfsmittel installieren, damit Kennwort-Datenbank erstellt werden kann
 	
 	sudo apt-get install apache2-utils 
 	
 	
 	2. Konfigurieren
 	
 	2.1. Original-Konfigurationsdatei sichern
 	
 	sudo cp /etc/squid/squid.conf /etc/squid/squid.conf.original
 	
 	2.2. Zugriffsrechte auf Konfigdatei für alle
 	
 	sudo chmod ugo+rwx ./squid.conf
 	
 	2.3. Zugriffsrechte auf Logdateien
 	
 	sudo chmod ugo+rwx /var/log/squid/access.log
 	sudo chmod ugo+rwx /var/log/squid/cache.log
 	
 	2.4. User für Basic-Authentication erstellen

 	Mit Basic-Authentication kann man sich über Java nicht mit dem Proxy verbinden. 
 	Deshalb besser 2.5.

 	sudo htpasswd -c /etc/squid/passwords squiduser
 	> squidpwd
 	
 	2.5. Digest Authentication
 	
 	cd /etc/squid
 	sudo touch passwd
 	sudo chown proxy.proxy passwd
 	sudo chmod 640 passwd
 	sudo htdigest -c /etc/squid/passwd SquidRealm squiduser
 	> squidpwd
 	
 	Prüfen:
 	sudo /usr/lib/squid/digest_file_auth /etc/squid/passwd
 	>"squiduser":"SquidRealm"
 	OK...
 	CTRL-D
 	
 	2.6. Konfigurationsdatei 

 	auth_param digest program /usr/lib/squid/digest_file_auth -c /etc/squid/passwd
 	auth_param digest realm SquidRealm
 	auth_param digest children 2

	acl auth_users proxy_auth REQUIRED
	
	http_access allow auth_users
	http_access deny all

 	http_port 3128
 	http_port 3129 intercept
 	

 	3. Squid neu starten
 	
 	sudo /etc/init.d/squid restart
 	
 	
 	4. Proxy im Windows einrichten über Chrome
 	
 	Einstellungen - Suche nach "proxy" - Windows-Einstellungen öffnen - LAN-Einstellungen
 	Proxyserver anhaken, Adresse und Port eingeben
 	
 	5. Java Programm
 	
 	Kommandozeilenparameter -Djava.net.useSystemProxies=true
 	
 	Authenticator s.u., User=squiduser, Pwd=squidpwd
 		
 */
		
		if (enabled) {
			log.info("Initialize proxy: host=" + host + ", proxyPort=" + port + ", proxyUser=" + userName);
			
			Authenticator.setDefault(new Authenticator() {
			    @Override
			    protected PasswordAuthentication getPasswordAuthentication() {
			    	PasswordAuthentication ret = null;
			        if (getRequestorType() == RequestorType.PROXY) {
			        	
			        	String requestingHost = getRequestingHost();
			        	int requestingPort = getRequestingPort();
			            String prot = getRequestingProtocol().toLowerCase();
			            
			            log.info("Proxy authentication: requestingHost=" + requestingHost + ", requestingProtocol=" + prot + ", requestingPort=" + requestingPort 
			            		+ ", host=" + host + ", port=" + port + ", proxyUser=" + userName);
						
			            String password = PasswordEncryption.decrypt(encryptedPassword);
			            if (host.isEmpty()) {
		                    ret = new PasswordAuthentication(userName, password.toCharArray());
			            }
			            else {
			            	if (requestingHost.equalsIgnoreCase(host)) {
				                if (port == requestingPort) {
				                    ret = new PasswordAuthentication(userName, password.toCharArray());
				                }
				            }
			            }
			        }
			        return ret;
			    }
			});

			if (host.isEmpty()) {
				// Use system proxies.
				// Check whether the program was started with this option.
				// It can only be set on the command line.
				String useSystemProxiesStr = System.getProperty("java.net.useSystemProxies");
				if (useSystemProxiesStr == null || useSystemProxiesStr.isEmpty()) {
					String msg = "Command line option -Djava.net.useSystemProxies=true has to be passed in order to use system proxies.";
					System.err.println(msg);
					log.warning(msg);
				}
			}
			else {
				System.setProperty("http.host", host); 
				System.setProperty("http.proxyPort", Integer.toString(port));
				System.setProperty("https.host", host); 
				System.setProperty("https.proxyPort", Integer.toString(port));
			}
			
		}
		else {
			log.info("Proxy not used.");
			System.setProperty("http.host", ""); 
			System.setProperty("http.proxyPort", "0");
			System.setProperty("https.proxyHost", ""); 
			System.setProperty("https.proxyPort", "0"); 
		}
		
	}

	public String getHost() {
		return Default.value(host);
	}

	public void setHost(String host) {
		this.host = host;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUserName() {
		return Default.value(userName);
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getEncryptedPassword() {
		return Default.value(encryptedPassword);
	}

	public void setEncryptedPassword(String password) {
		this.encryptedPassword = password;
	}


}
