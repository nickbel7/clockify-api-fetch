import java.io.File;
import org.ini4j.*;

import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
// import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONObject;
import org.json.JSONArray;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

public class App
{
    public static void main( String[] args ) throws Exception
    {

        Wini ini = new Wini(new File("ClockifyBridge.ini"));
        String serverName = ini.get("DBConnection", "ServerName");
        String databaseName = ini.get("DBConnection", "DatabaseName");
        String apiKey = ini.get("Params", "APIKey");
        String clockId = ini.get("Params", "ClockID");
        int daysBefore = ini.get("Params", "DaysBefore", int.class);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String currentDateTime = dtf.format(now);
        // System.out.println(currentDateTime);

        ////////////
        // DATA RETRIEVAL FROM API
        ///////////
        System.out.println("Starting...");
        //API KEY & WORKSPACE ID
        String workspaceId = "5f85bf02df6d623f428770c4";

        // Create url object
        URL obj = new URL("https://api.clockify.me/api/v1/workspaces/"+workspaceId+"/users/");
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("X-Api-Key", apiKey);
        con.setRequestProperty("content-type", "application/json");
        int responseCode = con.getResponseCode();
        System.out.println("\n Sending 'GET' request to URL...");
        System.out.println("Response Code : " + responseCode);
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //READ JSON response
        System.out.println("Result after Reading JSON Response");
        JSONArray usersArray = new JSONArray(response.toString());

        // DATE and TIME that denote START time for returned entries
        Calendar cal = Calendar.getInstance();
        int offsetDays = daysBefore;
        cal.add(Calendar.DATE, -offsetDays);

        // TIME AND DATE PARSERS
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");   //DATE & TIME format for API
        // df.setTimeZone(TimeZone.getTimeZone("UTC"));
        // System.out.println(Calendar.getInstance().getTimeZone());
        SimpleDateFormat dfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm");   //DATE & TIME format for DB
        // SimpleDateFormat dfTime = new SimpleDateFormat("HH:mm");
        String startDate = df.format(cal.getTime());            //sets default Stard DATE & TIME
        df.setTimeZone(TimeZone.getTimeZone("UTC"));            //sets format to UTC time
        System.out.println("Fetching Records Starting from (Date & Time) : " + startDate);
        // String startDate;

        //INITIALIZE CONNECTION TO DATABASE
        Connection conn = null;
        String url = "jdbc:sqlserver://"+ serverName +";databaseName="+ databaseName +";integratedSecurity=true";

        // Establish the connection
        System.out.println("Connecting to database...");
        
        //Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        conn = DriverManager.getConnection(url);
        System.out.println("Connected to database successfully...");

        //LOOPS THROUGHT THE USERS AND GETS THEIR TIME ENTRIES
        try {
            for (int i=0 ; i<usersArray.length() ; i++) {
                JSONObject singleUser = usersArray.getJSONObject(i);
                String userId = singleUser.get("id").toString();
                String userName = singleUser.get("name").toString();
                String userEmail = singleUser.get("email").toString();
                System.out.println(userName + ": " + userEmail + " -> " + userId);

                //GET LAST ENTRY DATE AND TIME FROM DATABASE FOR APPROPRIATE USER
                try {
                    Statement stmt = null;
                    String query = "SELECT TOP(1) "
                                   + "TRAPRE_DATE,"
                                   + "TRAPRE_TIME, "
                                   + "FUNC "
                                   + "from INFOP_CLOCKIFY "
                                   + "WHERE EMAIL LIKE '" + userEmail + "' "
                                   + "ORDER BY TRAPRE_DATE DESC, TRAPRE_TIME DESC";
                    try {
                      stmt = conn.createStatement();
                      ResultSet rs = stmt.executeQuery(query);
                      while (rs.next()) {
                        String tDate = rs.getString("TRAPRE_DATE").substring(0, 10); //Get Date from DB
                        String tTime = rs.getString("TRAPRE_TIME");                  //Get Time from DB

                        Date lastEntryDate = dfDateTime.parse(tDate +" "+tTime);     //Unify Date and Time into one Date variable
                        df.setTimeZone(TimeZone.getTimeZone("Europe/Athens"));
                        startDate = df.format(lastEntryDate);
                        df.setTimeZone(TimeZone.getTimeZone("UTC"));                 //Format Date for API
                        System.out.println(startDate);
                      }
                    } catch (SQLException e) {
                        throw new Error("Problem", e);
                    } finally {
                        if (stmt != null) { stmt.close(); }
                    }

                } catch (SQLException e) {
                    throw new Error("Problem", e);
                }

                URL timeEntriesObject = new URL("https://api.clockify.me/api/v1/workspaces/"+workspaceId+"/user/"+userId+"/time-entries?start="+startDate);
                HttpURLConnection con2 = (HttpURLConnection) timeEntriesObject.openConnection();
                con2.setRequestMethod("GET");
                con2.setRequestProperty("X-Api-Key", apiKey);
                con2.setRequestProperty("content-type", "application/json");
                //Response from GET request
                BufferedReader in2 = new BufferedReader(new InputStreamReader(con2.getInputStream()));
                String inputLine2;
                //Convert response to JSON format -> timeEntriesResponse
                StringBuffer timeEntriesResponse = new StringBuffer();
                while ((inputLine2 = in2.readLine()) != null) {
                    timeEntriesResponse.append(inputLine2);
                }
                in2.close();

                // DateFormat simpleDf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a z", Locale.ENGLISH);
                //SIMPLE DATE & TIME PARSERS FOR SQL QUERY
                DateFormat simpleDfDate = new SimpleDateFormat("yyyy-MM-dd");
                DateFormat simpleDfTime = new SimpleDateFormat("HH:mm");

                JSONArray timeEntriesArray = new JSONArray(timeEntriesResponse.toString());
                System.out.println(timeEntriesArray.length());
                for (int j=0 ; j<timeEntriesArray.length() ; j++) {
                    JSONObject timeEntry = timeEntriesArray.getJSONObject(timeEntriesArray.length() - 1 - j);
                    String startTime = timeEntry.getJSONObject("timeInterval").get("start").toString();
                    String endTime = timeEntry.getJSONObject("timeInterval").get("end").toString();

                    Date startTimeParsed = df.parse(startTime);
                    // String startTimeString = simpleDf.format(startTimeParsed).toString();
                    // System.out.println("Start: " + simpleDf.format(startTimeParsed).toString());
                    System.out.println("StartDate: " + simpleDfDate.format(startTimeParsed).toString());
                    System.out.println("StartTime: " + simpleDfTime.format(startTimeParsed).toString());
                    Date endTimeParsed = (endTime=="null" ? null : df.parse(endTime));
                    // String endTimeString = simpleDf.format(endTimeParsed).toString();
                    // System.out.println(endTime=="null" ? "Still working" : "End: " + simpleDf.format(endTimeParsed).toString());

                    ////////////
                    // LOAD DATA IN SQL DATABASE
                    ///////////
                    //QUERY FOR START TIME ENTRIES
                    Statement stmtStart = null;
                    // System.out.println(simpleDfDate.format(startTimeParsed)+ " "+ simpleDfTime.format(startTimeParsed));
                    String queryStart = "IF NOT EXISTS ("
                                    + "SELECT 'X' FROM INFOP_CLOCKIFY WHERE EMAIL = '" + userEmail
                                    + "' AND TRAPRE_DATE = '" + simpleDfDate.format(startTimeParsed)
                                    + "' AND TRAPRE_TIME = '" + simpleDfTime.format(startTimeParsed)
                                    + "' AND FUNC = 'A' )"
                                    + "INSERT INTO INFOP_CLOCKIFY "
                                    + "(CLK_ID, EMAIL, TRAPRE_DATE, TRAPRE_TIME, FUNC, CARDNO, IN_DATE, COMMENTS, FLAG_UPD) "
                                    // + "VALUES (999, 'nikolas.bellos@gmail.com', '2020-11-09', '15:15', 'A', NULL, '2020-11-09 15:19:39', NULL, 0) "
                                    + "VALUES (" + clockId + ", '" + userEmail + "', '" + simpleDfDate.format(startTimeParsed) + "', '" + simpleDfTime.format(startTimeParsed) + "', 'A', NULL, '" + currentDateTime + "', NULL, 0) ";
                    try {
                        stmtStart = conn.createStatement();
                        stmtStart.executeUpdate(queryStart);
                        System.out.println("Inserted records into the table...");
                    } catch (SQLException e) {
                        throw new Error("Problem (Start entry)", e);
                    } finally {
                        if (stmtStart != null) { stmtStart.close(); }
                    }

                    //QUERY FOR END TIME ENTRIES
                    if (endTime != "null") {
                        Statement stmtEnd = null;
                        String queryEnd = "IF NOT EXISTS ("
                                        + "SELECT 'X' FROM INFOP_CLOCKIFY WHERE EMAIL = '" + userEmail
                                        + "' AND TRAPRE_DATE = '" + simpleDfDate.format(endTimeParsed)
                                        + "' AND TRAPRE_TIME = '" + simpleDfTime.format(endTimeParsed)
                                        + "' AND FUNC = 'B' )"
                                        + "INSERT INTO INFOP_CLOCKIFY "
                                        + "(CLK_ID, EMAIL, TRAPRE_DATE, TRAPRE_TIME, FUNC, CARDNO, IN_DATE, COMMENTS, FLAG_UPD) "
                                        // + "VALUES (999, 'nikolas.bellos@gmail.com', '09/11/2020', '15:15', 'A', NULL, '08/11/2020 15:19:39', NULL, 0) "
                                        + "VALUES (" + clockId + ", '" + userEmail + "', '" + simpleDfDate.format(endTimeParsed) + "', '" + simpleDfTime.format(endTimeParsed) + "', 'B', NULL, '" + currentDateTime + "', NULL, 0) ";
                        try {
                            stmtEnd = conn.createStatement();
                            stmtEnd.executeUpdate(queryEnd);
                            System.out.println("Inserted records into the table...");
                        } catch (SQLException e) {
                            throw new Error("Problem (End entry)", e);
                        } finally {
                            if (stmtEnd != null) { stmtEnd.close(); }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new Error("Problem", e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }

    }

}
