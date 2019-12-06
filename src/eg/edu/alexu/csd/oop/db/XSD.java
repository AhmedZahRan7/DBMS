package eg.edu.alexu.csd.oop.db;

import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XSD {

    private static LinkedList<String> Labels=new LinkedList<>();
    private static LinkedList<Class> TYPES=new LinkedList<>();
    static void MakeXsd(String Path, Table tables)  {

        if(tables.getHeaders() == null || tables.getTypes()==null){
            System.out.println("Wrong table initialization");
            return;
        }

        File dbfile=new File(Path+".xsd");
        try {
            dbfile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileWriter writer;
        try {
            writer = new FileWriter(dbfile);
            StringBuilder str=new StringBuilder();
            String s=getBeginOfXsd(tables.getName());
            str.append(s);
            writer.write(s);
            s=getDataXsd(tables.getHeaders(),tables.getTypes());
            writer.write(s);


            s=getEndXsd();
            writer.write(s);

            writer.flush();
            writer.close();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    static void getXSD(String Path, String Table) throws Exception {

        Labels=new LinkedList<>();
        TYPES=new LinkedList<>();
        String str="";
        try {
            str= new String(Files.readAllBytes(Paths.get(new File(Path+  System.getProperty("file.separator") + Table +  System.getProperty("file.separator") + Table + ".xsd").getAbsolutePath())));

        }catch (Exception e ){
            throw new Exception("XSD FILE OF PATH "+ Path +" doesn't found");
        }

        Pattern pattern=Pattern.compile("(.*)(<xs:complexType name = 'record'>)(.*)");

        Matcher matcher=pattern.matcher(str);

        if (matcher.find())
        {
            int x=matcher.start();

            String s=new StringBuilder(str).delete(0,x).toString();

            pattern=Pattern.compile("(<xs:element)(.*)(/>)");

            matcher=pattern.matcher(s);

            while (matcher.find())
            {
                String LastString =matcher.group(2);

                Labels.add(GetName(LastString));
                TYPES.add(GetType(LastString));

            }
        }else throw new EOFException("ERROR FORMAT XSD FILE");
    }



    static String[] GetNames(){
        String[] s =new String[Labels.size()];
        int i=0;
        for(String str:Labels){
            s[i++]=str;
        }
        return s;
    }

    static Class[] GetTypes(){
        Class[] s =new Class[TYPES.size()];
        int i=0;
        for(Class cls:TYPES){
            s[i++]=cls;
        }
        return s;
    }

    private static Class GetType(String lastString) throws EOFException {
        Pattern pattern=Pattern.compile("(\")(.*?)(\")");
        Matcher matcher =pattern.matcher(lastString);
        int count=0;
        while (matcher.find())
        {
            if(count==0){count++;continue;}

            return GetClass(new StringBuilder(matcher.group(2)).delete(0,3).toString());
        }
        throw new EOFException("ERROR FORMAT XSD FILE");
    }

    private static Class GetClass(String group) throws EOFException {
        switch (group)
        {
            case "string":return String.class;
            case "int":return Integer.class;
            case "boolean":return Boolean.class;
        }
        System.out.println(group);
        throw new EOFException("ERROR FORMAT XSD FILE");
    }


    private static String GetName(String lastString) throws EOFException
    {
        Pattern pattern=Pattern.compile("(\")(.*?)(\")");
        Matcher matcher =pattern.matcher(lastString);
        if(matcher.find())
        {
            return (matcher.group(2));
        }
        throw new EOFException("ERROR FORMAT XSD FILE");
    }

    private static String getEndXsd()
    {
        return  "      </xs:sequence>\n </xs:complexType> \n</xs:schema>" ;
    }

    private static String getDataXsd(String[] labels, Class[] types) throws Exception
    {
        StringBuilder stringBuilder =new StringBuilder();
        for(int i=0;i<labels.length;i++)
        {
            String s=getRecord(labels[i],types[i]);
            stringBuilder.append(s);
        }
        return stringBuilder.toString();
    }

    private  static String getRecord(String label, Class type) throws Exception
    {
        return "        <xs:element name =\"" + label + "\" type =\"" + getclass(type) + "\"/>\n";
    }

    private static  String getclass(Class type) throws Exception
    {

        if(type==String.class)return "xs:string";
        if(type==Integer.class||type==int.class)return "xs:int";
        if(type==Boolean.class||type==boolean.class)return "xs:boolean";
        throw new Exception(type+"not supported type");
    }
    private  static  String getBeginOfXsd( String table)
    {
        return "<?xml version = \"1.0\"?>\n   " + "<xs:schema xmlns:xs = \"http://www.w3.org/2001/XMLSchema\">\n" +
                "   <xs:element name = '" + table + "'>\n" +
                "         <xs:complexType>\n              <xs:sequence>\n" +
                "                  <xs:element name = 'record' type = 'record'   minOccurs='0' maxOccurs = 'unbounded' />\n " +
                "             </xs:sequence>\n" + "         </xs:complexType>\n" + "   </xs:element>\n\n" +
                "<xs:complexType name = 'record'>\n" + "      <xs:sequence>\n";

    }
}