package eg.edu.alexu.csd.oop.db;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;

public class XML {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";

    static boolean convertIntoXml(String Path, Table table) throws SQLException {

        File file =new File(Path);
        if(!file.exists()){
            throw new SQLException(Path+"  ISN'T FOUND");
        }
        file=new File(Path+System.getProperty("file.separator")+table.getName());

        if(!file.exists()){
            file.mkdir();
            XSD.MakeXsd(Path+System.getProperty("file.separator")+table.getName()+System.getProperty("file.separator")+table.getName(),table);
        }
        LinkedList<Object[]> data =table.getTable();
        String TableName =table.getName();
        String []Labels=table.getHeaders();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return false;
        }
        Document doc = dBuilder.newDocument();
        Element rootElement = doc.createElement(table.getName());
        doc.appendChild(rootElement);

        for (Object[] objects : data) {
            Element row = doc.createElement("record");
            rootElement.appendChild(row);
            int numberOfColumn = 0;
            for (Object object : objects) {
                Element element = doc.createElement(Labels[numberOfColumn]);
                element.appendChild(doc.createTextNode(String.valueOf(object)));
                row.appendChild(element);
                numberOfColumn++;
            }
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            return false;
        }
        DOMSource source = new DOMSource(doc);

        StreamResult result = new StreamResult(Path+System.getProperty("file.separator")+ TableName + System.getProperty("file.separator") + TableName + ".xml");
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    static LinkedList<Object[]> convertFromXml(String Path, String table, Class[] types) throws Exception {
        // validate the data before loading it

        String s=Path+System.getProperty("file.separator")+table+System.getProperty("file.separator")+table;

        boolean isValid = validateXMLSchema(s+ ".xsd",s+".xml");
        if(! isValid) {
            throw new Exception("XML ISN'T VALID");
        }


        LinkedList<Object[]> data = new LinkedList<>();
        try {
            File file = new File(Path+System.getProperty("file.separator") + table + System.getProperty("file.separator") + table + ".xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("record");
            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);
                Object[] row = new Object[types.length];
                Element eElement = (Element) nNode;
                NodeList nodeList =eElement.getChildNodes();
                for (int j = 0; j < eElement.getChildNodes().getLength(); j++) {
                    String str = nodeList.item(j).getTextContent();
                    if (types[j] == String.class) {
                        row[j]=str;
                    } else if (types[j] == int.class||types[j] == Integer.class) {
                        row[j]=(Integer.parseInt(str));
                    } else if (types[j] == boolean.class||types[j] == Boolean.class) {
                        row[j]=(Boolean.parseBoolean(str));
                    } else {
                        throw new Exception("not supported type");
                    }

                }
                data.add(row);
            }
        } catch (Exception e) {
            System.out.println(ANSI_RED + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        }
        return data;
    }

    private static boolean validateXMLSchema(String xsdPath, String xmlPath){
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new File(xsdPath));
            Validator validator;
            validator = schema.newValidator();
            validator.validate(new StreamSource(new File(xmlPath)));
        } catch (IOException e){
            System.out.println(ANSI_RED  + "Exception: " + e.getMessage() + ANSI_RESET);
            return false;
        }catch(SAXException e1){
            System.out.println(ANSI_RED + "SAX Exception: "+e1.getMessage() + ANSI_RESET);
            return false;
        }

        return true;

    }

}