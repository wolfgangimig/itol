pushd "D:\git\itol\itol-elo"
set CP=D:\git\itol\itol-elo\bin
set CP=%CP%;D:\git\itol\itol\bin
set CP=%CP%;D:\git\itol\itol-db\bin
set CP=%CP%;D:\git\joa\java\lib
set CP=%CP%;D:\git\joa\java\lib\joa.jar
set CP=%CP%;D:\git\itol\itol-elo\lib\commons-codec-1.6.jar
set CP=%CP%;D:\git\itol\itol-elo\lib\commons-logging-1.1.1.jar
set CP=%CP%;D:\git\itol\itol-elo\lib\EloixClient.jar
set CP=%CP%;D:\git\itol\itol-elo\lib\javautils-1.2.jar
set CP=%CP%;D:\git\itol\itol-elo\lib\log4j-1.2.17.jar
"C:\Program Files\Java\jre1.8.0_20\bin\java.exe" -classpath "%CP%" de.elo.itol.ELOIssueApplication
