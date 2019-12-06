package eg.edu.alexu.csd.oop.db;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

public class DB implements Database {
    private volatile static DB obj;
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private String path;
    private LinkedList<Table> tables = new LinkedList<>();
    private Mark mark = new Mark();
    private List<Object> data;
    private String[] actualHeaders;
    private Class[] actualTypes;
    private String directoryOfDatabases = "DataBasesDirectory";
    private String selectedTableName;
    private Class[] selectedTypes;
    private String[] selectedHeaders;

    public Class[] getSelectedTypes() {
        return selectedTypes;
    }

    public String[] getSelectedHeaders() {
        return selectedHeaders;
    }

    public DB() {

    }

    /**
     * to use sigelton oop-pattern
     */
    public static DB getInstance()
    {
        if (obj == null)
        {
            // To make thread safe
            synchronized (DB.class)
            {
                // check again as multiple threads
                // can reach above step
                if (obj==null)
                    obj = new DB();
            }
        }
        return obj;
    }


    @Override
    public String createDatabase(String databaseName, boolean dropIfExists) {

//        File direct = new File(directoryOfDatabases);
//        if (!direct.exists()) direct.mkdirs();
//        databaseName = directoryOfDatabases + System.getProperty("file.separator") + databaseName;
        File file = new File(databaseName);
        boolean exist = file.exists();
        if (exist && !dropIfExists) { //if exist and not drop >> use it
            this.path = databaseName;
            try {
                tables = ReadTables.ReadTables(databaseName);
            } catch (Exception e) {
                System.out.println(ANSI_RED + e.getMessage() + ANSI_RESET);
            }
            return databaseName;
        } else if (exist) { //if exist and drop >> drop the old then create new one and use it
            dropDirectory(databaseName);
            createDirectory(databaseName);
            tables = new LinkedList<>();
            this.path = databaseName;
            return databaseName;
        }
        if (createDirectory(databaseName)) { //if not exists create it
            this.path = databaseName;
            tables = new LinkedList<>();
            return databaseName;
        } else return databaseName;
    }

    private void dropDirectory(String path) { //this function can delete a directory and all its contents
        File oldData = new File(path);
        String[] entries = oldData.list();
        if (entries != null) {
            for (String s : entries) {
                dropDirectory(oldData.getPath() + System.getProperty("file.separator") + s);
            }
        }
//        else {
//            if (!path.contains(".xml") && !path.contains(".xsd"))
//                System.out.println(ANSI_RED + "Data base doesn't exist!!" + ANSI_RESET);
//        }
        oldData.delete();
    }

    private boolean createDirectory(String path) { //this function creates a directory witt this path
        File directory = new File(path);
        return directory.mkdirs();
    }

    @Override
    public boolean executeStructureQuery(String query) throws SQLException {
        query = query.replaceAll("\'", "\"");
        if (query.matches("(?i)^create .+")) { //starts with create
            LinkedList<Object> resultOfQuery = Parser.parseCreate(query);
            if (resultOfQuery == null || (resultOfQuery.size() != 3 && resultOfQuery.size() != 5)) {
                throw new SQLException("Wrong create command");
            }
            if ((Boolean) resultOfQuery.get(0)) { //create DataBase
                String dataName = (String) resultOfQuery.get(1);
                Boolean drop = (Boolean) resultOfQuery.get(2);
                createDatabase(dataName, drop);
            } else { //create a table
                if (path == null) {
                    throw new SQLException("no database initialized");
                }
                String tableName = (String) resultOfQuery.get(1);
                File tablePath = new File(path + System.getProperty("file.separator") + tableName);
                if (tablePath.exists()) {
                    System.out.println("table is already existed");
                    return false;
                }
                String[] namesOfColumns = (String[]) resultOfQuery.get(3);
                String[] types = (String[]) resultOfQuery.get(4);
                if (tableName == null || namesOfColumns[0] == null || types[0] == null) {
                    System.out.println(ANSI_RED + "Wrong table initialization!!" + ANSI_RESET);
                    return false;
                }
                Table t = new Table(tableName, namesOfColumns, types);
                addTable(t);
                XML.convertIntoXml(path, t);
            }
        } else if (query.matches("(?i)^drop.+")) { //starts with drop
            LinkedList<Object> resultOfQuery = Parser.parseDrop(query);
            if (resultOfQuery == null || resultOfQuery.size() != 2) {
                throw new SQLException("wrong query");
            }
            if ((Boolean) resultOfQuery.get(0)) { //drop DataBase
                String dataName = (String) resultOfQuery.get(1);
                dropDirectory( dataName);
                this.tables = new LinkedList<>();
                this.path = null;

            } else { //drop a table
                String tableName = (String) resultOfQuery.get(1);
                Table t = getTable(tableName);
                if (t == null) {
                    throw new SQLException("table name not exist!");
                }
                removeTable(t);
                dropDirectory(path + System.getProperty("file.separator") + tableName);
            }
        }
        return true;
    }

    @Override
    public Object[][] executeQuery(String query) throws SQLException {
        query = query.replaceAll("\'", "\"");
        LinkedList<Object> result = Parser.parseSelect(query);
        if (result == null) {
            throw new SQLException("wrong input format");
        }
        LinkedList<String> columns = new LinkedList<>(Arrays.asList((String[]) result.get(0)));
        String tableName = (String) result.get(1);
        String condition = (String) result.get(2);
        LinkedList<String> headers = new LinkedList<>(Arrays.asList(getTable(tableName).getHeaders()));
        if (columns.getFirst().equals("*")) {
            columns = headers;
        }
        // when ahmed finished it
        LinkedList<Object[]> references = new LinkedList<>();
        if (!condition.equals("")) {
            Mark mark = new Mark();
            try {
                references = mark.getData(condition, getTable(tableName));
            } catch (Exception e) {
                System.out.println(ANSI_RED + e.getMessage() + ANSI_RESET);
                return null;
            }
        } else {
            references.addAll(getTable(tableName).getTable());
        }
        Object[][] arr = new Object[references.size()][columns.size()];
        LinkedList<Integer> columnsIndexes = new LinkedList<>();
        for (String column : columns) {
            columnsIndexes.add(headers.indexOf(column));
        } // now i have the indexes of the columns to be selected
        selectedTableName = tableName;
        this.selectedHeaders = columns.toArray(new String[columns.size()]);
        this.selectedTypes = new Class[columns.size()];
        ArrayList<String> originalHeaders = new ArrayList<>(Arrays.asList(getSelectedTable().getHeaders()));
        ArrayList<Class> originalTypes = new ArrayList<>(Arrays.asList(getSelectedTable().getTypes()));
        int h = 0;
        for (String column : columns) {
            int index = originalHeaders.indexOf(column);
            this.selectedTypes[h++] = originalTypes.get(index);
        }
        int k = 0;
        for (int j = 0; j < headers.size(); j++) {
            if (columnsIndexes.contains(j)) { // j is a column to be selected
                int i = 0;
                for (Object record : references) { // i is an index of a record selected
                    arr[i++][k] = ((Object[]) record)[j];
                }
                k++;
            }
        }
        return arr;
    }

    /**
     * This method like a dirctor, it makes tasks distributions...
     * @param query
     *        the query needed to be excuted.
     * @return int
     *        it returns number of operated cols and -1 if no col hasn't inserted or deleted...
     */
    @Override
    public int executeUpdateQuery(String query) throws Exception {
        query = query.replaceAll("\'", "\"");
        if(query.matches("(?i)^\\s*INSERT\\s+INTO\\s.+$"))
            return this.excuteInsert(query);
        if(query.matches("(?i)^\\s*DELETE\\s+FROM\\s.+$"))
            return this.excuteDelete(query);
        if(query.matches("(?i)^\\s*UPDATE\\s+.+$"))
            return this.excuteUpdate(query);
        return 0;
    }

    /**
     * @param
     *      query of type string the query
     * @return
     *        Integer define the modified row in the table
     * this method takes the query and then get the main data to be inserted
     * checks if the table already exits
     * checks if the number of scheme objects equals the insertions objects
     * checks data types of the the insertions
     * rearrange the data to make it right to according to the scheme
     */
    private int excuteInsert(String query) throws SQLException, Exception{


        data = Parser.parseInsert(query);
        if(data == null) {
            System.out.println(ANSI_RED + "Bad Input!!" + ANSI_RESET);
            return 0;
        }
        Table operateOnTable = this.existance(data.get(0).toString());

        if(operateOnTable != null) {
            String[] headers = (String[])data.get(1);
            if(headers==null){
                headers=operateOnTable.getHeaders();
            }
            toLowerCaseConverter(headers, headers.length);
            String[] values = (String[])data.get(2);
            if(headers.length != values.length) {
                System.out.println(ANSI_RED + "Bad Input!!" + ANSI_RESET);
                return 0;
            }
            actualHeaders = operateOnTable.getHeaders();
            toLowerCaseConverter(actualHeaders, actualHeaders.length);
            actualTypes = operateOnTable.getTypes();
            if(this.containsTheseHeaders(headers, values)) {
                String[] strings = this.rearrange(headers, values);
                operateOnTable.addX(convertToObjects(strings));
                XML.convertIntoXml(this.path, operateOnTable);
                return 1;
            }
            else {
                System.out.println(ANSI_RED + "Bad Input!!" + ANSI_RESET);
                return 0;
            }
        } else {
            System.out.println(ANSI_RED + "The table is still not being created!!" + ANSI_RESET);
            return 0;
        }
    }

    private Object[] convertToObjects(String[] str) throws Exception {
        Object[] obj = new Object[str.length];
        int i = 0;
        for(String temp : str) {
            if(temp == null)
                throw new Exception("Bad Input!!");
            if(temp.matches("^-?[0-9]+$")){
                obj[i++] = Integer.parseInt(temp);
            }
            else if(temp.matches("(?i).*true|false.*"))
                obj[i++] = Boolean.parseBoolean(temp);
            else
                obj[i++] = temp;
        }
        return obj;
    }

    private int excuteDelete(String query) throws Exception {
        data = Parser.parseDelete(query);
        if(data == null) {
            System.out.println(ANSI_RED + "Bad Input!!" + ANSI_RESET);
            return 0;
        }
        Table deleteFromTable = this.existance(data.get(0).toString());
        if(deleteFromTable != null) {
            if(deleteFromTable.getTable().size() == 0) {
                throw new Exception("This operation is denied for an empty table!!");
            }
            if(data.get(1).equals("")) {
                int holeSize = deleteFromTable.getTable().size();
                deleteFromTable.emptyTheTable();
                try {
                    XML.convertIntoXml(this.path, deleteFromTable);
                } catch(SQLException e) {}
                return holeSize;
            } else {
                LinkedList<Object[]> deletedRows = mark.getData(data.get(1).toString(), deleteFromTable);
                for(Object[] O : deletedRows) deleteFromTable.removeRecord(O);
                try {
                    XML.convertIntoXml(this.path, deleteFromTable);
                } catch(SQLException e) {}
                return deletedRows.size();
            }
        } else
            System.out.println(ANSI_RED + "The table is still not being created" + ANSI_RESET);
        return 0;
    }

    private int excuteUpdate(String query) throws Exception {
        LinkedList<Object[]> markedList = new LinkedList<>();
        data = Parser.parseUpdate(query);
        if(data == null) {
            System.out.println(ANSI_RED + "Bad Input!!" + ANSI_RESET);
            return 0;
        }
        Table updatedTable = this.existance(data.get(0).toString());
        if(updatedTable != null) {
            if(updatedTable.getTable().size() == 0) {
                return 0;
            }
            String[] cols = (String[])data.get(2);
            String[] newValues = (String[])data.get(3);
            actualHeaders = updatedTable.getHeaders();
            actualTypes = updatedTable.getTypes();
            toLowerCaseConverter(cols,cols.length);
            toLowerCaseConverter(actualHeaders,actualHeaders.length);
            if(cols.length != newValues.length) {
                System.out.println(ANSI_RED + "Bad Input!!" + ANSI_RESET);
                return 0;
            }
            if(this.containsTheseHeaders(cols, newValues)) {
                if(data.get(1).toString() != "") {
                    markedList = mark.getData(data.get(1).toString(), updatedTable);
                    for(Object[] iterate : markedList) {
                        Object[] newRowToBeExclusivelyAdded = this.createNewRows(iterate, cols, newValues);
                        updatedTable.updateRecord(iterate, newRowToBeExclusivelyAdded);
                        try {
                            XML.convertIntoXml(this.path, updatedTable);
                        } catch(SQLException e) {}
                    }
                }
                else {
                    updatedTable.updateHoleTable(this.convertToObjects(newValues), this.getIndexes(cols));
                    try {
                        XML.convertIntoXml(this.path, updatedTable);
                    } catch(SQLException e) {}
                    return updatedTable.getSize();
                }
            } else {
                System.out.println(ANSI_RED + "Bad Input!!" + ANSI_RESET);
                return 0;
            }
        } else
            throw new SQLException("The table is still not being created!!");
        return markedList.size();
    }

    private Object[] createNewRows(final Object[] oldy, String[] cols, String[] newValues) throws Exception {
        Object[] news = convertToObjects(newValues);
        LinkedList<String> actualHeadersAsList = new LinkedList<>(Arrays.asList(actualHeaders));
        Object[] newie = new Object[oldy.length];
        System.arraycopy(oldy, 0, newie, 0, oldy.length);
        for(int i = 0, index; i < cols.length; ++i) {
            index = actualHeadersAsList.indexOf(cols[i]);
            newie[index] = news[i];
        }
        return newie;
    }

    private int[] getIndexes(String[] cols) {
        LinkedList<String> actualHeadersAsList = new LinkedList<String>(Arrays.asList(actualHeaders));
        int[] indexes = new int[cols.length];
        for(int i = 0, j = 0; i < cols.length; ++i) {
            if(actualHeadersAsList.contains(cols[i]))
                indexes[j++] = actualHeadersAsList.indexOf(cols[i]) + 1;
        }
        return indexes;
    }

    private Table existance(String tableName) {
        return this.getTable(tableName);
    }

    private Boolean containsTheseHeaders(String[] headers, String[] values) {
        LinkedList<String> actualHeadersAsList = new LinkedList<String>(Arrays.asList(actualHeaders));
        int i;
        for(i = 0; i < headers.length; ++i) {
            if(actualHeadersAsList.contains(headers[i])) {
                int index = actualHeadersAsList.indexOf(headers[i]);
                if(actualTypes[index].toString().matches("(?i)class\\s+java\\.lang\\.Integer")) {
                    if(values[i].matches("^-?[0-9]+$")){
                        continue;
                    }
                    else {
                        System.out.println(ANSI_RED + "Not Matched Types!!" + ANSI_RESET);
                        return false;
                    }
                }
                else if(actualTypes[index].toString().matches("(?i)class\\s+java\\.lang\\.Boolean")) {
                    if(values[i].matches("(?i).*(false)|(true).*")) {
                        continue;
                    }
                    else {
                        System.out.println(ANSI_RED + "Not Matched Types!!" + ANSI_RESET);
                        return false;
                    }
                }
            } else
                return false;
        }
        return i == headers.length;
    }

    private String[] rearrange(String[] headers, String[] values) {
        LinkedList<String> headersList = new LinkedList<String>(Arrays.asList(headers));

        String[] dummy = new String[actualHeaders.length];
        for(int i = 0; i < headersList.size(); ++i){
            int index = Arrays.asList(actualHeaders).indexOf((headersList.get(i)));
            if(index != -1)
                dummy[index] = values[i];
        }
        return dummy;
    }

    public Table getSelectedTable() {
        for (Table table : tables) {
            if (table.getName().equals(this.selectedTableName)) {
                return table;
            }
        }
        return null;
    }

    private Table getTable(String tableName){
        for (Table table : tables) {
            if (table.getName().equalsIgnoreCase(tableName)) {
                return table;
            }
        }
        return null;
    }

    String schema(){
        StringBuilder stringBuilder = new StringBuilder("");
        Iterator<Table> tableIterator = tables.listIterator();
        while (tableIterator.hasNext()){
            stringBuilder.append(tableIterator.next());
            if (tableIterator.hasNext()){
                stringBuilder.append('\n');
            }
        }
        return stringBuilder.toString();
    }

    private void addTable(Table table){
        this.tables.add(table);
    }

    private void removeTable (Table table){
        this.tables.remove(table);
    }

    private void toLowerCaseConverter(String[] arr, int length) {
        for(int i = 0; i < length; ++i)
            arr[i] = arr[i].toLowerCase();
    }

}