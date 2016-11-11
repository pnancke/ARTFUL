package fmdir;

import java.io.*;
import java.sql.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.Date;

/**
 * Created by kev_s on 08.11.2016.
 */
public class DatabaseTools {
    public static final String driverName= "org.sqlite.JDBC";
    public static int id = 0;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    /**
     * Fill the database
     * @param translations
     * @param wordFreq
     * @param year
     * @param language
     */
    public static void fillDatabase(ArrayList<Translation> translations, HashMap<String, Integer> wordFreq, String year, String language) {
        try {
            Class.forName(driverName).newInstance();
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + Main.prop.getProperty("dbPath"));
            connection.setAutoCommit(false);

            try {
                long updateStart = System.currentTimeMillis();
                PreparedStatement insert = connection.prepareStatement("insert into words (id, w_id, word, language, located_in," + year + ") values (?1, ?2, ?3, ?4, ?5, ?6);");

                PreparedStatement query = connection.prepareStatement("SELECT id FROM words WHERE word = ?1 and language='" + language + "';");

                PreparedStatement update = connection.prepareStatement("update words set " + year + " = ?1 WHERE id= ?2");

                int searched = 0;
                int found = 0;
                for (Translation translation: translations) {
                    searched++;

                    //TODO: REPLACE PRIMARY KEY WITH (word,language)
                    //then: https://coderanch.com/t/538298/databases/Checking-record-exists
                    //or: http://stackoverflow.com/a/8818028
                    //or: http://stackoverflow.com/a/20817124
                    //or maybe: SELECT count(*) FROM foos WHERE bar = 'bar'


                    int w_id = translation.id;
                    String word = translation.citylabel;
//                    String language = translation.language;
                    String locatedIn = translation.locatedIn;


                    if (wordFreq.containsKey(word)) {
                        found++;
                        query.setString(1, word);
                        ResultSet rs = query.executeQuery();
                        if (rs.next()) {
                            update.setInt(1, wordFreq.get(word));
                            update.setInt(2, rs.getInt(1));
                            update.addBatch();
                        } else {
                            id++;
                            insert.setInt(1, id);
                            insert.setInt(2, w_id);
                            insert.setString(3, word);
                            insert.setString(4, language);
                            insert.setString(5, locatedIn);
                            insert.setInt(6, wordFreq.get(word));
                            insert.addBatch();
                        }
                    }
                    insert.executeBatch();
                    update.executeBatch();
                    connection.commit();
                }

                long commitStart = System.currentTimeMillis();
                System.out.println(found + " matches found");
                System.out.println(searched + " words queried, compared, and written to DB in\t" + (float)(commitStart-updateStart)/1000 + " seconds");
//                insert.executeBatch();
//                update.executeBatch();
//                connection.commit();
//                System.out.println(searched + " word frequencies written in \t" + (float)(System.currentTimeMillis()-commitStart)/1000 + " seconds");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(ANSI_YELLOW + "ERROR. Rolling back changes." + ANSI_RESET);
                connection.rollback();
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fill the database
     * @param wordFreq
     * @param year
     * @param language
     */
    public static void writeAllWordFrequencies(HashMap<String, Integer> wordFreq, String year, String language) {
        try {
            Class.forName(driverName).newInstance();
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + Main.prop.getProperty("dbPath"));
            connection.setAutoCommit(false);

            try {
                PreparedStatement insert = connection.prepareStatement("insert into words (id, w_id, word, language, located_in," + year + ") values (?1, ?2, ?3, ?4, ?5, ?6);");

                int found = 0;
                for (Object kvPair : wordFreq.entrySet()) {

                    Map.Entry pair = (Map.Entry) kvPair;
                    String word = pair.getKey().toString();
                    int freq = (int) pair.getValue();

                    found++;
                    id++;
                    insert.setInt(1, id);
                    insert.setInt(2, 0);
                    insert.setString(3, word);
                    insert.setString(4, language);
                    insert.setString(5, null);
                    insert.setInt(6, freq);
                    insert.addBatch();
                }

                long commitStart = System.currentTimeMillis();
                insert.executeBatch();
                connection.commit();
                System.out.println(found + " word frequencies added in\t" + (float)(System.currentTimeMillis()-commitStart)/1000 + " seconds");
            } catch (Exception e) {
//                e.printStackTrace();
                System.out.println(ANSI_YELLOW + "ERROR. Rolling back changes." + ANSI_RESET);
                connection.rollback();
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateWordFrequencies(HashMap<String, Integer> wordFreq, String year, String language) {
        try {
            Class.forName(driverName).newInstance();
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + Main.prop.getProperty("dbPath"));
            connection.setAutoCommit(false);

            try {
                long updateStart = System.currentTimeMillis();

                Statement s = connection.createStatement();
                ResultSet rs = s.executeQuery("SELECT word, language, " + year + " FROM words WHERE language='" + language + "';");
                PreparedStatement update = connection.prepareStatement("update words set " + year + " = ?1 WHERE word = ?2 and language = ?3");

                int searched = 0;
                int found = 0;
                while (rs.next() && searched<3){
                    searched++;

                    String word = rs.getString(1);

                    if (wordFreq.containsKey(word)) {
                        found++;

                        update.setInt(1, wordFreq.get(word));
                        update.setString(2, word);
                        update.setString(3, language);
                        update.addBatch();
                    }
                }

                long commitStart = System.currentTimeMillis();
                System.out.println(searched + " words queried and compared in\t" + (float)(commitStart-updateStart)/1000 + " seconds");
                update.executeBatch();
                connection.commit();
                System.out.println(found + " word frequencies updated in\t" + (float)(System.currentTimeMillis()-commitStart)/1000 + " seconds");
            } catch (Exception e) {
//                e.printStackTrace();
                System.out.println(ANSI_YELLOW + "ERROR. Rolling back changes." + ANSI_RESET);
                connection.rollback();
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * DEBUG function
     * @param rowCount
     */
    public static void TESTprintDB(int rowCount) {
        try {
            Class.forName(driverName).newInstance();
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + Main.prop.getProperty("dbPath"));

            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM words;");
            int columnCount = rs.getMetaData().getColumnCount();

            int temp = 0;
            while (rs.next() && temp<rowCount){
                temp++;
                String allColumns = "";
                for (int i=1; i<=columnCount; i++) allColumns += rs.getString(i) + "\t";
                System.out.println(allColumns);
            }
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteAllRows(String dbName) {
        System.out.println("Deleting table contents: " + dbName);
        try {
            Class.forName(driverName).newInstance();
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + Main.prop.getProperty("dbPath"));

            Statement s = connection.createStatement();

            String query = "DELETE FROM " + dbName;
            int deletedRows=s.executeUpdate(query);
            if(deletedRows>0){
                System.out.println("Deleted all rows in the table successfully...");
            }else{
                System.out.println("Table already empty.");
            }
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printCount() {
        try {
            Class.forName(driverName).newInstance();
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + Main.prop.getProperty("dbPath"));
            ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM words;");
            System.out.println(ANSI_CYAN + "DatabaseTools records: " + NumberFormat.getNumberInstance(Locale.US).format(rs.getInt(1)) + " rows" + ANSI_RESET);
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
