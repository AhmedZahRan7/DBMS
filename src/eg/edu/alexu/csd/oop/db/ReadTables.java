package eg.edu.alexu.csd.oop.db;

import java.io.File;
import java.util.LinkedList;


/**
 * to call this class use it
 *  Table []t =ReadTables.ReadTables("");
 *  it return LinkedLIST of tables
 */
public class ReadTables {

    static LinkedList<Table> ReadTables(String Path) throws Exception {
        String[] tablesName=null;
        tablesName  =new File(Path).list();
        if (tablesName == null)return new LinkedList<>();
        LinkedList<Table> tables = new LinkedList<>();
        for(int i=0;i<tablesName.length;i++){
            tables.add(MakeTable(tablesName[i],Path));
        }
        return tables;
    }

    private static Table MakeTable(String tableName,String Path) throws Exception {
        XSD.getXSD(Path,tableName);
        String[] headers=XSD.GetNames();
        Class[] types=XSD.GetTypes();
        String[] StringTypes=GetString(types);
        LinkedList<Object[]> table=XML.convertFromXml(Path,tableName,types);
        Table table1=new Table(tableName,headers,StringTypes) ;
        table1.addAll(table);
        return table1;
    }

    private static String[] GetString(Class[] types) throws Exception {
        int i=0;
        String[] str =new String[types.length];
        for(Class c :types){
            if(c==String.class){
                str[i++]="String";
            }else if(c==Integer.class||c==int.class){
                str[i++]="Integer";
            }else if(c==Boolean.class||c==boolean.class){
                str[i++]="boolean";
            }else {
                throw new Exception("type "+c+" is not supported");
            }
        }
        return str;
    }

}
