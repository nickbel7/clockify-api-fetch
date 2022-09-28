# clockify-api-fetch

### Service that fetces data from clockify api and stores it to an SQL database

THIS IS A JAVA PROJECT

To COMPILE the java file (class file will be generated) : 
javac --release 8 -cp lib/* App.java

To put the class file into a jar (with Manifest file) : 
jar cfm ClockifyBridge.jar Manifest.txt App.class

To run the jar :
java -jar ClockifyBridge.jar

To get data from api (cmd command) :
curl -H "content-type: application/json" -H "X-Api-Key: X4X1zK2D+VZFRN0w" -X GET https://api.clockify.me/api/v1/user

*add "| jq" for json code parsing in cmd

Parameters (ini file)

[DBConnection]
ServerName=
DatabaseName=
UserName=
Password=

[Params]
APIKey=
ClockID=
HoursBefore=

-workspaceId


INFOP_CLOCKIFY table :
-CLK_ID -> (ini parameter)
-EMAIL -> userEmail
-TRAPRE_DATE -> startDate (ex. 08/11/2020)
-TRAPRE_TIME -> startTime (ex. 15:19)
-FUNC -> 'A' for start 'B' for end
-CARDNO -> NULL
-IN_DATE -> current date and time (ex. 08/11/2020 15:19:39)
-COMMENTS -> NULL
-FLAG_UPD -> 0


Usefull resources : 
MAVEN project manual
https://maven.apache.org/guides/getting-started/

JSON manipulation in java : 
https://www.baeldung.com/java-org-json

Insert data to SQL table with jdbc :
https://alvinalexander.com/java/edu/pj/jdbc/jdbc0002/

Create JAR file from .java
https://www.baeldung.com/java-create-jar

