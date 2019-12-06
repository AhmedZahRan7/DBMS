package eg.edu.alexu.csd.oop.db;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Scanner;

public class Parser {
    private volatile static Parser obj;

    public static Parser getInstance() throws Exception {
        if (obj == null)
        {
            // To make thread safe
            synchronized (Parser.class)
            {
                // check again as multiple threads
                // can reach above step
                if (obj==null)
                    obj = new Parser();
            }
        }
        return obj;
    }
    private Parser() throws Exception {
        run();
    }

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_PURPLE = "\u001B[45m";
    private static DB dummyDataBase = DB.getInstance();
    private Scanner scanner = new Scanner(System.in);

    private void run() throws Exception{
        // to make ctrl + l
        // for screen clearing
        System.out.print("\033[H\033[2J");
        System.out.flush();

        String command;
        do {
            System.out.print(ANSI_PURPLE + "Death_Team >>> " + ANSI_RESET);
            command = scanner.nextLine();
            command = command.trim();
            try{
                String ans = this.parse(command.replaceAll(";", ""));
                if(ans == null)
                    continue;
                else if(ans.equals("Bad Input!! " + "\n"))
                    System.out.print(ANSI_RED + ans + ANSI_RESET);
                else
                    System.out.print(ANSI_BLUE + ans + ANSI_RESET);
            } catch (Exception e){
                System.out.println(ANSI_RED + e.getMessage() + ANSI_RESET);
            }
        } while (!command.equalsIgnoreCase(".quit"));
    }

    // takes the query as a parameter, and returns the message back to run(); to show it to the console
    private String parse(String query) throws Exception {
        // for databases creation, for example "create database database1;"
        if (query.matches("(?i)^\\s*(CREATE|Drop)\\s+.+$")) {
            // we are dealing with create
            dummyDataBase.executeStructureQuery(query);
            return "";
        } else if (query.matches("(?i)^\\s*INSERT\\s+INTO\\s+.+$") ||
                query.matches("(?i)^\\s*DELETE\\s+FROM\\s+.+$") ||
                query.matches("(?i)^\\s*UPDATE\\s+.+$")) {
            // we are dealing with execute structure query statement
            dummyDataBase.executeUpdateQuery(query);
            return "";
        } else if (query.matches("(?i)^\\s*SELECT\\s+.+\\s+FROM\\s.+$")){
            Object[][] selectedTable = dummyDataBase.executeQuery(query);
            if(selectedTable == null)
                return null;
            StringBuilder stringBuilder = new StringBuilder();
            for (Object[] objects : selectedTable) {
                for (int j = 0; j < objects.length; j++) {
                    stringBuilder.append(objects[j]);
                    if (j < objects.length-1) {
                        stringBuilder.append('|');
                    }
                }
                stringBuilder.append('\n');
            }
            return stringBuilder.toString();
        } else if (query.matches("(?i)^\\.quit\\s*$")){
            return "thanks for enjoying our DBMS\n";
        } else if (query.matches("(?i)^\\.schema\\s*$")) {
            return dummyDataBase.schema();
        }
        return "Bad Input!! " + "\n";
    }

    /*
     * takes the query as a parameter
     * returns a linked list of objects
     * at index = 0, a Boolean Object it is true if it is to create a database, and false if to create a table
     * at index = 1, a String object to tell you the name of database or the name of the table
     * at index = 2, an Boolean Object, is useless in case of create table, if true then drop the existing database
     *           with the same name if false then don't drop it.
     * at index = 3, an object of an array of strings that holds the names of each column.
     * at index = 4, an object of an array of classes that holds that types of each column.
     * put in your mind that you have to use type cast when dealing with the return value
     * like : "Boolean value = (Object[]) Parser.parseCreate(query).get(2);" (Boolean not boolean)
     * returns null if the query doesn't match
     */
    public static LinkedList<Object> parseCreate(String query) throws SQLException {
        if (query.matches("(?i)^\\s*CREATE.+$")) {
            LinkedList<Object> result = new LinkedList<>();
            String query1 = query;
            query1 = query1.replaceAll("(?i)^\\s*CREATE\\s+", "");
            if (query1.matches("(?i)^\\s*DATABASE\\s+.+")) {
                // we are dealing with data base
                result.add(true);
                query1 = query1.replaceAll("(?i)^\\s*DATABASE\\s+", "");
                String dataBaseName = "";
                boolean dropIfExist = false;
                if (query1.matches("^\\s*.+\\s*$")) {
                    // for databases creation, for example "create database database_name1;"
                    dataBaseName = query1.replaceAll("\\s*$", "");
                } else if (query1.matches("(?i)^\\s*.+\\s+DROP\\s+IF\\s+EXIST\\s*$")) {
                    // for example "create database drop IF NOT EXIST batabase1;"
                    dataBaseName = query1.replaceAll("(?i)\\s+DROP\\s+IF\\s+EXIST\\s*$", "");
                    dropIfExist = true;
                } else
                    System.out.println(ANSI_RED + "Bad Input!!" + ANSI_RESET);
                result.add(dataBaseName);
                result.add(dropIfExist);
                return result;
            } else if (query1.matches("(?i)^\\s*TABLE\\s+.+")) { // done
                // we are dealing with table
                // example: "create table table_name values ( names varchar, phone int, email varchar);"
                query1=query1.replace("(" ," (");
                if(!query1.matches(".+\\)\\s*$") || query1.matches("(?i)^\\s*TABLE\\s+\\w+\\s*$")) return null;
                result.add(false);
                String tableName = query1;
                tableName = tableName.replaceAll("(?i)^\\s*TABLE\\s+", "").replaceAll("(?i)\\s*[(].+\\s*$", "");
                result.add(tableName);
                result.add(false);
                String[] dataStyle = query1.replaceAll("(?i)^\\s*TABLE\\s*\\w+\\s*[(]\\s*", "").replaceAll("(?i)\\s*[)]\\s*$", "").split(",");
                // dataStyle will look like "[name varchar, phone int, email varchar]"
                String[] headers = new String[dataStyle.length];
                String[] types = new String[dataStyle.length];
                if (validateTableStyle(dataStyle)) {
                    for (int i = 0; i < dataStyle.length; i++) {
                        String[] temp = dataStyle[i].replaceAll("^\\s+", "").replaceAll("\\s+$", "").split("\\s+");
                        headers[i] = temp[0];
                        if (temp[1].equalsIgnoreCase("varchar")) {
                            types[i] = "String";
                        } else if (temp[1].equalsIgnoreCase("int")) {
                            types[i] = "Integer";
                        } else if (temp[1].equalsIgnoreCase("bool")) {
                            types[i] = "Boolean";
                        }
                    }
                }
                result.add(headers);
                result.add(types);
                return result;
            }
        }
        return null;
    }

    /*
     * takes the query as a parameter
     * returns a linked list of objects
     * at index = 0, a Boolean Object it is true if it is to drop a database, and false if to drop a table
     * at index = 1, a String object to tell you the name of database or the name of the table
     * put in your mind that you have to use type cast when dealing with the return value
     * like : "Boolean value = (Object[]) Parser.parseDrop(query).get(0);" (Boolean not boolean)
     * returns null if the query doesn't match
     */
    static LinkedList<Object> parseDrop(String query) throws SQLException {
        if (query.matches("(?i)^\\s*DROP\\s+.+\\s*$")) {
            LinkedList<Object> result = new LinkedList<>();
            String query1 = query;
            query1 = query1.replaceAll("(?i)^\\s*Drop\\s+", "");
            if (query1.matches("(?i)^\\s*DATABASE.+$")) {
                // we are dealing with database dropping
                // example: "DROP DATABASE database_name;"
                result.add(true);
                String dataBaseName = query1.replaceAll("(?i)^\\s*DATABASE\\s+", "").replaceAll("(?i)\\s*$", "");
                result.add(dataBaseName);
            } else if (query1.matches("(?i)^\\s*TABLE\\s+\\w+\\s*$")) {
                // we are dealing with table dropping
                // example: "DROP TABLE table_name;"
                result.add(false);
                String tableName = query1.replaceAll("(?i)^\\s*TABLE\\s+", "").replaceAll("(?i)\\s*$", "");
                result.add(tableName);
            }
            return result;
        }
        return null;
    }

    /*
     * takes the query as its parameter
     * returns a linkedlist of objects
     * at index = 0, there is a string object that contains the name of the table.
     * at index = 1, there is an array of strings object that contains the columns.
     * at index = 2, there is an object of an array of objects that contains the data.
     * put in your mind that you have to use type cast when dealing with the return value
     * like : "String value = (Object[]) Parser.parseInsert(query).get(2);"
     * returns null if the query doesn't match
     */
    static LinkedList<Object> parseInsert(String query) {
        // example: "insert into table_name (column1 ,column2, column3) values (value1, value2, value3);"
        if (query.matches("(?i)^\\s*INSERT\\s+INTO\\s+\\w+\\s*[(].*[)]\\s*VALUES\\s*[(].+[)]\\s*$")) {
            String query1 = query;
            String tableName = query1.replaceAll("(?i)^\\s*INSERT\\s+INTO\\s+", "").replaceAll("(?i)\\s*[(].*[)]\\s*VALUES\\s*[(].+[)]\\s*$", "");
            query1 = query;
            String[] headers = removeSpaces(query1.replaceAll("(?i)^\\s*INSERT\\s+INTO\\s+\\w+\\s*[(]\\s*", "").replaceAll("(?i)[)]\\s*VALUES\\s*[(].+[)]\\s*$", "").split(","));
            // headers are for example: "column1,column2,column3".
            query1 = query;
            String[] values = removeSpaces(query1.replaceAll("(?i)^\\s*INSERT\\s+INTO\\s+\\w+\\s*[(].*[)]\\s*VALUES\\s*[(]\\s*", "").replaceAll("(?i)\\s*[)]\\s*$", "").split(","));
            // values are for example: "value1,value2,value3".
            LinkedList<Object> result = new LinkedList<>();
            result.add(tableName);
            result.add(headers);
            result.add(values);
            return result;
        }else if (query.matches("(?i)^\\s*INSERT\\s+INTO\\s+\\w+\\s+VALUES\\s*[(].+[)]\\s*$")){
            String query1 = query;
            String tableName = query1.replaceAll("(?i)^\\s*INSERT\\s+INTO\\s+", "").replaceAll("(?i)\\s+VALUES\\s*[(].+[)]\\s*$", "");
            query=query.replaceAll(".*\\(","").replaceAll("\\)","");
            String[] values =query.split(",");
            for(int i=0;i<values.length;i++) values[i]=values[i].trim();
            LinkedList<Object> result = new LinkedList<>();
            result.add(tableName);
            result.add(null);
            result.add(values);
            return result;

        }
        return null;
    }

    /*
     * takes the query as its parameter
     * returns a linkedlist of objects
     * at index = 0, there is an array object that tells you which columns you want to select,
     *          unless you get and array whose first element is *(string) then select all the columns.
     * at index = 1, there is a string object that tells you the name of the table.
     * at index = 2, there is a string object that contains the condition after where,
     *          and if the object is an empty string then no condition.
     * put in your mind that you have to use type cast when dealing with the return value
     * like : "String value = (Object[]) Parser.parseInsert(query).get(2);"
     * returns null if the query doesn't match
     */
    static LinkedList<Object> parseSelect(String query) {
        if (query.matches("(?i)^\\s*SELECT\\s+.+\\s+FROM\\s+.+\\s*$")) {
            LinkedList<Object> result = new LinkedList<>();
            // don't know if it contains where or not
            query = query.replaceAll("(?i)^\\s*SELECT\\s+", "").replaceAll("(?i)\\s*$", "");
            String query1 = query;
            String requiredColumns = query1.replaceAll("(?i)\\s+FROM.+$", "");
            if (requiredColumns.matches("^\\s*[*]\\s*$")) {
                // meaning we "SELECT * FORM table_name;".
                result.add(new String[]{"*"});
            } else {
                // something like "column1, column2".
                result.add(removeSpaces(requiredColumns.split(",")));
            }
            if (query.matches("(?i)^.+WHERE.+$")) {
                // contains where
                String tableName = query1.replaceAll("(?i)^.+FROM\\s+", "").replaceAll("(?i)\\s+WHERE.+$", "");
                result.add(tableName);
                String condition = query1.replaceAll("(?i)^.+FROM\\s+.+WHERE\\s+", "").replaceAll("'","\"");
                result.add(condition);
            } else {
                // doesn't contain where
                String tableName = query1.replaceAll("(?i)^.+FROM\\s+", "");
                result.add(tableName);
                result.add("");
            }
            return result;
        }
        return null;
    }

    /*
     * takes the query as its parameter
     * returns a linkedlist of objects
     * at index = 0, there is a object of a string of the name of the table.
     * at index = 1, there is a string object that contains the condition after where,
     *          and if the object is an empty string then no condition.
     * put in your mind that you have to use type cast when dealing with the return value
     * like : "String value = (Object[]) Parser.parseInsert(query).get(2);"
     * returns null if the query doesn't match
     */
    static LinkedList<Object> parseDelete(String query) {
        if (query.matches("(?i)^\\s*DELETE\\s+FROM\\s+.+$")) {
            LinkedList<Object> result = new LinkedList<>();
            String query1 = query.replaceAll("(?i)^\\s*DELETE\\s+FROM\\s+", "").replaceAll("\\s*$","");
            String tableName = "";
            String condition = "";
            if (query1.matches("(?i)^\\w+\\s*WHERE\\s+.+\\s*$")) {
                tableName = query1.replaceAll("(?i)^\\s+WHERE.+$", "").replaceAll("(?i)\\s*where.+\\s*$","");
                condition = query1.replaceAll("(?i)^\\s*\\w+\\s+WHERE\\s+", "");
            } else if (query1.matches("(?i)^\\s*\\w+\\s*$")){
                tableName = query1;
                condition = "";
            }
            result.add(tableName);
            result.add(condition.replaceAll("'","\"")); // because Mark doesn't deal with single quotes in it
            return result;
        }
        return null;
    }

    /*
     * takes the query as its parameter
     * returns a linkedlist of objects
     * at index = 0, there is a object of a string of the name of the table.
     * at index = 1, there is a string object that contains the condition after where,
     *          and if the object is an empty string then no condition.
     * at index = 2, there is an object of an array that contains the columns that we need to set the data at.
     * at index = 3, there is an object of an array that contains the new values that we are ganna set.
     * put in your mind that you have to use type cast when dealing with the return value
     * like : "String value = (Object[]) Parser.parseInsert(query).get(2);"
     * returns null if the query doesn't match
     */
    static LinkedList<Object> parseUpdate(String query) {
        if (query.matches("(?i)^\\s*UPDATE\\s+\\w+\\s+SET\\s+.+$")){
            LinkedList<Object> result = new LinkedList<>();
            String query1 = query.replaceAll("(?i)^\\s*UPDATE\\s+",""); // remove update keyword
            String tableName = query1.replaceAll("(?i)\\s+SET\\s+.+$","");
            query1 = query1.replaceAll("(?i)^\\s*\\w+\\s*SET\\s*",""); // remove table name and set Keyword
            String[] setters = new String[20];
            String condition = null;
            if (query1.matches("(?i)^.+\\s+WHERE\\s+.+\\s*$")) {
                // contains where
                condition = query1.replaceAll("(?i)^\\s*.+\\s+WHERE\\s+", "").replaceAll("\\s*$", "");
                setters = query1.replaceAll("\\s+WHERE\\s+.+\\s*$", "").split(",");
            } else if (query1.matches("(?i)^.+\\s*$")){
                // doesn't contain where
                condition = "";
                setters = query1.split(",");
            }
            String[] columns = new String[setters.length];
            String[] newValues = new String[setters.length];
            for (int i = 0; i < setters.length; i++) {
                String[] arr = setters[i].split("=");
                columns[i] = arr[0];
                newValues[i] = arr[1];
            }
            columns = removeSpaces(columns);
            newValues = removeSpaces(newValues);
            newValues[newValues.length-1] = newValues[newValues.length-1].replaceAll("where.+$", "");
            result.add(tableName);
            result.add(condition);
            result.add(columns);
            result.add(newValues);
            return result;
        }
        return null;
    }

    private static boolean validateTableStyle(String[] tableStyle) {
        boolean valid = true;
        for (String s : tableStyle) {
            if (!s.matches("(?i)^\\s*\\w+\\s+(varchar|int|bool)\\s*")) {
                valid = false;
                break;
            }
        }
        return valid;
    }

    private static String[] removeSpaces(String[] array) {
        String[] tempArray = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            tempArray[i] = array[i].replaceAll("\\s+", "").replaceAll("'","\"");
        }
        return tempArray;
    }


}