Issue Tracker Addin for Microsoft Outlook and Redmine
Copyright 2015 Wolfgang Imig
http://www.wilutions.com/joa/itol.html


1. Abstract

ITOL creates issues for Redmine from within Microsoft Outlook. 
It seamlessly integrates with the Outlook explorer and mail inspector 
window and allows you to enter the issue properties and add additional
file attachments.


2. Preferences
  - Windows 7 or later
  - Outlook 2010 or 2013, Outlook Express is not supported.
  - Redmine 2.6.1 (only tested with this version)


3. Usage Notes

This program creates a project with identifier "itol-configuration".
Its description is used to store the definition of the 
custom properties for any user. Since Redmine allows only administrators
to read custom property definitions - maybe because select lists could
contain secrets - they are stored in an encrypted format.
Since every user can modify the JavaScript interface, everyone can 
access the decrypted information if she or he is really interested in.
Thus, ITOL should not be used in environments, where custom properties
contain secrets.

The first user that starts ITOL should be an administrator. This 
allows to create the "itol-configuration" project. The display name of the project
is "Issue Tracker for Microsoft Outlook and Redmine Configuration". It can be 
changed as well as the project's parent. ITOL only tries to find the project
by its identifier "itol-configuration" (which must not be changed). 
  
  
4. FAQ

4.1. Drag & Drop of mails into attachments list

Currently, it is not possible to drag mails into the attachment list. Outlook
uses a special data format that Java does not recognize. Though I have a 
solution for this problem for ordinary Java programs (see www.wilutions.com/outldd),  
it does not work with JavaFX packaged applications. 
I am still working on this problem.

 
5. Licenses

ITOL is licensed under the MIT License, see file "MIT License.txt".

Icons downloaded from
http://www.iconarchive.com/show/childish-icons-by-double-j-design.html
license: http://creativecommons.org/licenses/by/4.0/
Backlink to designer: http://www.doublejdesign.co.uk 