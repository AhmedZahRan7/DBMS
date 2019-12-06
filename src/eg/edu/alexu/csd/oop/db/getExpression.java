package eg.edu.alexu.csd.oop.db;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class getExpression {
    private LinkedList< Pair <String , Pair< Integer, Integer> > > Strings=new LinkedList<>();
    private LinkedList< Pair <String , Pair< Integer, Integer> > > Container=new LinkedList<>();
    private LinkedList <Object> Expresstion=new LinkedList<>();
    private char[] AllowedChar ={'+','-','*','/','(',')','<','>','='};
    private String[] AllowedStrings ={"and","or","not"};
    private String str;

    Object[] GetExpression(String str) throws SQLException {
        this.str=str;
        DuplicateSpaces();
        GetStrings();
        GetObjects();
        int indexOfStrings=0;
        int indexOfContainers=0;
        for(int i=0;i<Strings.size()+Container.size();i++){
            if(indexOfStrings<Strings.size()){
                if(indexOfContainers>=Container.size()||Strings.get(indexOfStrings).second.first<Container.get(indexOfContainers).second.first){
                    Expresstion.add(Strings.get(indexOfStrings++).first);
                    continue;
                }
            }
            Expresstion.add(FindType(Container.get(indexOfContainers++).first));
        }

        Object[] objects =new Object[Expresstion.size()];
        for(int i=0;i<Expresstion.size();i++){
            objects[i]=Expresstion.get(i);
        }
        return objects;
    }

    private Object FindType(String first) throws SQLException {
        if(isInteger(first)){
            return Integer.parseInt( first );
        }
        if(first.length()==1){
            if(iSALLOWEDCHAR(first.charAt(0))){
                return first.charAt(0);
            }
            throw new SQLException("\"" + first + "\"" + " isn't a column in the table ");
        }
        if(AlOWSTRING(first)){
            return first;
        }
        if(IsBool(first)){
            return Boolean.parseBoolean(first.toLowerCase());
        }
        throw new SQLException("\"" + first + "\"" + " isn't a column in the table ");
    }

    private boolean IsBool(String first) {
        return first.equalsIgnoreCase("true")||first.equalsIgnoreCase("false");
    }

    private boolean AlOWSTRING(String first) {
        for(String s :AllowedStrings){
            if(first.equalsIgnoreCase(s))return true;
        }
        return false;
    }

    private boolean iSALLOWEDCHAR(char c) {
        for(char c1:AllowedChar){
            if(c==c1)return true;
        }
        return false;
    }

    private boolean isInteger(String first) {
        return first.matches("^(-)?[0-9]+$");
    }

    private void DuplicateSpaces() {
        StringBuilder s= new StringBuilder(" ");
        for(int i=0;i<str.length();i++){
            if(str.charAt(i)!=' '){
                s.append(str.charAt(i));
                continue;
            }
            s.append("  ");
        }
        s.append(" ");
        str= s.toString();
    }

    private void GetObjects() {
        Pattern pattern =Pattern.compile("([\\s][^\"\\s]+?[\\s])");
        Matcher matcher=pattern.matcher(str);
        while (matcher.find()){
            String s = new StringBuilder(matcher.group(1)).delete(0,1).delete(matcher.group(1).length()-2,matcher.group(1).length()-1).toString();
            if(OVERLAPPING(matcher.start(),matcher.end()))continue;
            Pair<String , Pair< Integer , Integer>> p= new Pair<>();
            Pair<Integer , Integer> pSecond = new Pair<>(matcher.start(), matcher.end());
            p.first=s;
            p.second=pSecond;
            Container.add(p);
        }

    }

    private void GetStrings() {
        Pattern pattern =Pattern.compile("(\".*?[\"])");
        Matcher matcher=pattern.matcher(str);
        while (matcher.find()){
            String s = matcher.group(1);
            Pair<String , Pair< Integer , Integer>> p= new Pair<>();
            Pair<Integer , Integer> pSecond = new Pair<>(matcher.start(), matcher.end());
            p.first=s;
            p.second=pSecond;
            Strings.add(p);
        }

    }

    private boolean OVERLAPPING(int start, int end) {
        for(Pair pair :Strings ){
            if(OverLap(start,end, (Pair)pair.second))return true;
        }
        return false;
    }

    private boolean OverLap(int start, int end, Pair p ) {
        if(start<(int)p.first&&end<(int)p.second)return false;
        return start <= (int) p.first || end <= (int) p.second;
    }


    private static class  Pair < a , b > {
        a first;
        b second;
        Pair(a first , b second){
            this.first=first;
            this.second=second;
        }
        Pair (){ }
        @Override
        public String toString() {
            String s ="First " +this.first;
            s+=" --- ";
            s+="Second " +this.second;
            return s;
        }
    }
}