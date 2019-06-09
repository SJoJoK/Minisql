import CATALOGMANAGER.Attribute;
import CATALOGMANAGER.CatalogManager;
import CATALOGMANAGER.NumType;
import CATALOGMANAGER.Table;
import INDEXMANAGER.Index;
import INDEXMANAGER.IndexManager;
import RECORDMANAGER.Condition;
import RECORDMANAGER.RecordManager;
import RECORDMANAGER.TableRow;

import java.io.*;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Interpreter {

    private static boolean nestLock = false; //not permit to use nesting sql file execution

    public Interpreter() {

    }

    public static void main(String[] args) {
        try {
            // add API initialize in here
            API api = new API();
            System.out.println("Weclome to minisql~");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            interpret(reader);
        } catch (IOException e) {
            System.out.println("RunTime error: IO exception occurs");
        }

    }

    private static void interpret(BufferedReader reader) throws IOException {
        String restState = ""; //rest staement after ';' in last line

        while (true) { //read for each statment
            int index;
            String line;
            StringBuilder statement = new StringBuilder();
            if (restState.contains(";")) { // resetLine contains whole statement
                index = restState.indexOf(";");
                statement.append(restState.substring(0, index));
                restState = restState.substring(index + 1);
            } else {
                statement.append(restState); //add rest line
                statement.append(" ");
                System.out.print("-->");
                while (true) {  //read whole statement until ';'
                    line = reader.readLine();
                    if (line == null) { //read the file tail
                        reader.close();
                        String lastState = statement.toString().trim();
                        if(!lastState.equals("")) {
                            System.out.println("Can't deal with last statement: ");
                            System.out.println(lastState);
                        }
                        return;
                    } else if (line.contains(";")) { //last line
                        index = line.indexOf(";");
                        statement.append(line.substring(0, index));
                        restState = line.substring(index + 1); //set reset statement
                        break;
                    } else {
                        statement.append(line);
                        statement.append(" ");
                        System.out.print("-->"); //next line
                    }
                }
            }

            //after get the whole statement
            String[] tokens = statement.toString().trim().split(" ");
            String result = statement.toString().trim().replaceAll("\\s+", " ");

            if (tokens.length == 1 && tokens[0].equals("")) {
                System.out.println("No statement specified");
                continue;
            }
            switch (tokens[0]) { //match keyword
                case "create":
                    if (tokens.length == 1) {
                        System.out.println("Syntax error: Can't find create object");
                    } else {
                        switch (tokens[1]) {
                            case "table":
                                parse_create_table(result);
                                break;
                            case "index":
                                parse_create_index(result);
                                break;
                            default:
                                System.out.println("Syntax error: Can't identify " + tokens[1]);
                                break;
                        }
                    }
                    break;
                case "drop":
                    if (tokens.length == 1) {
                        System.out.println("Syntax error: Can't find drop object");
                    } else {
                        switch (tokens[1]) {
                            case "table":
                                parse_drop_table(result);
                                break;
                            case "index":
                                parse_drop_index(result);
                                break;
                            default:
                                System.out.println("Syntax error: Can't identify " + tokens[1]);
                                break;
                        }
                    }
                    break;
                case "select":
                    parse_select(result);
                    break;
                case "insert":
                    parse_insert(result);
                    break;
                case "delete":
                    parse_delete(result);
                    break;
                case "quit":
                    parse_quit(result, reader);
                    break;
                case "exefile":
                    parse_sql_file(result);
                    break;
                default:
                    System.out.println("Syntax error: Can't identify " + tokens[0]);
            }
        }
    }

    private static void parse_create_table(String statement) {
        System.out.println("In parse_create_table..");
        statement = statement.replaceAll(" *\\( *", " (").replaceAll(" *\\) *", ") ");
        statement = statement.replaceAll(" *, *", ",");
        statement = statement.trim();

        statement = statement.replaceAll("^create table","").trim(); //skip create table keyword

        int startIndex, endIndex;
        if (statement.equals("")) { //no statement after create table
            System.out.println("Syntax error: Must specify a table name");
            return;
        }

        endIndex = statement.indexOf(" ");
        if (endIndex == -1) { //no statement after create table xxx
            System.out.println("Syntax error: Can't find attribute definition");
            return;
        }

        String tableName = statement.substring(0, endIndex); //get table name

      /*
        if (CatalogManager.get_table(tableName) != null) { //check whether it exists
            System.out.println("Runtime error: Table " + tableName + "already existed");
            return;
        }*/

        startIndex = endIndex + 1; //start index of '('
        if (!statement.substring(startIndex).matches("^\\(.*\\)$")) { //check brackets
            System.out.println("Syntax error: Can't not find the definition brackets in table " + tableName);
            return;
        }

        int length;
        String[] attrsDefine, attrParas;
        String attrName, attrType, attrLength = "", primaryName = "";
        boolean attrUnique;
        Attribute attribute;
        Vector<Attribute> attrVec = new Vector<>();

        attrsDefine = statement.substring(startIndex + 1).split(","); //get each attribute definition

        for (int i = 0; i < attrsDefine.length; i++) { //for each attribute
            if (i == attrsDefine.length - 1) { //last line
                attrParas = attrsDefine[i].trim().substring(0, attrsDefine[i].length() - 1).split(" "); //remove last ')'
            } else {
                attrParas = attrsDefine[i].trim().split(" ");
            } //split each attribute in parameters: name, type,（length) (unique)

            if (attrParas[0].equals("")) { //empty
                System.out.println("Syntax error: Empty attribute in table " + tableName);
                return;
            } else if (attrParas[0].equals("primary")) { //primary key definition
                if (attrParas.length != 3 || !attrParas[1].equals("key")) { //not as primary key xxxx
                    System.out.println("Syntax error: Error definition of primary key in table " + tableName);
                    return;
                }
                if (!attrParas[2].matches("^\\(.*\\)$")) { //not as primary key (xxxx)
                    System.out.println("Syntax error: Error definition of primary key in table " + tableName);
                    return;
                }
                if (!primaryName.equals("")) { //already set primary key
                    System.out.println("Logical error: Redefinition of primary key in table " + tableName);
                    return;
                }
                primaryName = attrParas[2].substring(1, attrParas[2].length() - 1); //set primary key
            } else { //ordinary definition
                if (attrParas.length == 1) { //only attribute name
                    System.out.println("Syntax error: Incompleted definition in attribute " + attrParas[0]);
                    return;
                }
                attrName = attrParas[0]; //get attribute name
                attrType = attrParas[1]; //get attribute type

                for (int j = 0; j < attrVec.size(); j++) { //check whether name redefines
                    if (attrName.equals(attrVec.get(j).attributeName)) {
                        System.out.println("Logical error: Redefinition in attribute " + attrParas[0]);
                        return;
                    }
                }

                if (attrType.equals("int") || attrType.equals("float")) { //check type
                    endIndex = 2; //expected end index
                } else if (attrType.equals("char")) {
                    if (attrParas.length == 2) { //no char length
                        System.out.println("Syntax error: Must specify char length in " + attrParas[0]);
                        return;
                    }
                    if (!attrParas[2].matches("^\\(.*\\)$")) { //not in char (x) form
                        System.out.println("Syntax error: Wrong definition of char length in " + attrParas[0]);
                        return;
                    }
                    attrLength = attrParas[2].substring(1, attrParas[2].length() - 1); //get length
                    try {
                        length = Integer.parseInt(attrLength); //check the length
                    } catch (NumberFormatException e) {
                        System.out.println("Type error: The char length in " + attrParas[0] + " dosen't match a int type or overflow ");
                        return;
                    }
                    if (length < 1 || length > 255) {
                        System.out.println("Type error: The char length in " + attrParas[0] + " must be in [1,255] ");
                        return;
                    }
                    endIndex = 3; //expected end index
                } else { //unmatched type
                    System.out.println("Syntax error: Error attribute type " + attrType + " in " + attrParas[0]);
                    return;
                }

                if (attrParas.length == endIndex) { //check unique constraint
                    attrUnique = false;
                } else if (attrParas.length == endIndex + 1 && attrParas[endIndex].equals("unique")) {  //unique
                    attrUnique = true;
                } else { //wrong definition
                    System.out.println("Syntax error: Error constraint definition in " + attrParas[0]);
                    return;
                }

                if (attrType.equals("char")) { //generate attribute
                    attribute = new Attribute(attrName, NumType.valueOf(attrType.toUpperCase()), Integer.parseInt(attrLength), attrUnique);
                } else {
                    attribute = new Attribute(attrName, NumType.valueOf(attrType.toUpperCase()), attrUnique);
                }
                attrVec.add(attribute);
            }
        }
        if (primaryName.equals("")) { //check whether set the primary key
            System.out.println("Syntax error: Not specified primiary key in table " + tableName);
            return;
        }
        for (int j = 0; j < attrVec.size(); j++) {
            if (primaryName.equals(attrVec.get(j).attributeName)) { //check primary key matches one attribute
                Table table = new Table(tableName, primaryName, attrVec);
                System.out.println("Try to create the following table:");
                System.out.println("Table name: " + tableName);
                System.out.println("Primary key name: " + primaryName);
                System.out.println("Attribute:");
                for (int k = 0; k < attrVec.size(); k++) {
                    System.out.print("Name: " + attrVec.get(k).attributeName + " ");
                    System.out.print("Type: " + attrVec.get(k).type + " ");
                    System.out.println("Unique: " + attrVec.get(k).isUnique + " ");
                }
                if (API.create_table(tableName, table)) {
                    System.out.println("Create table successfully");
                }
                return;
            }
        }
        System.out.println("Logical error: No attribute name matches the primary key name");
    }

    private static void parse_drop_table(String statement) {
        System.out.println("In parse_drop_table..");
        String[] tokens = statement.split(" ");
        if (tokens.length == 2) {
            System.out.println("Syntax error: Not specify table name");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Syntax error: Extra parameters in drop table");
            return;
        }

        String tableName = tokens[2]; //get table name
     /*  if(CatalogManager.get_table(tableName) == null) {
            System.out.println("RunTime error: Table " + tableName + " dosen't exist");
            return;
        }*/

        System.out.println("Try to drop the following table:");
        System.out.println("Table name: " + tableName);
        if (API.drop_table(tableName)) {
            System.out.println("Drop table successfully");
        }
    }

    private static void parse_create_index(String statement) {
        System.out.println("In parse_create_index..");
        statement = statement.replaceAll("\\s+", " ");
        statement = statement.replaceAll(" *\\( *", " (").replaceAll(" *\\) *", ") ");
        statement = statement.trim();

        String[] tokens = statement.split(" ");

        if (tokens.length == 2) {
            System.out.println("Syntax error: Not specify index name");
            return;
        }

        String indexName = tokens[2]; //get index name
        if (tokens.length == 3 || !tokens[3].equals("on")) {
            System.out.println("Syntax error: Must add keyword 'on' after index name " + indexName);
            return;
        }
        if (tokens.length == 4) {
            System.out.println("Syntax error: Not specify table name");
            return;
        }

        String tableName = tokens[4]; //get table name
        if (tokens.length == 5 ) {
            System.out.println("Syntax error: Not specify attribute name in table " + tableName);
            return;
        }

        String attrName = tokens[5];
        if (!attrName.matches("^\\(.*\\)$")) { //not as (xxx) form
            System.out.println("Syntax error: Error in specifiy attribute name " + attrName);
            return;
        }

        attrName = attrName.substring(1, attrName.length() - 1); //extract attribute name
        if (tokens.length != 6) {
            System.out.println("Syntax error: Extra parameters in create index");
            return;
        }

    /*
        if (CatalogManager.get_table(tableName) == null) {
            System.out.println("Runtime error: Table " + tableName +" dosen't exist");
            return;
        }
        if (CatalogManager.get_attribute_index(tableName, attrName) == -1){
            System.out.println("Runtime error: The attribute " + attrName + " doesn't exist");
            return;
        }
        if (CatalogManager.is_index_key(tableName, attrName)) {
            System.out.println("Runtime error: The attribute " + attrName +
                    " already has index " + CatalogManager.get_index_name(tableName, attrName));
            return;
        }
        if (CatalogManager.is_unique(tableName , attrName)) {
            System.out.println("Runtime error: The attribute " + attrName + "is not unique");
            return;
        } */


        Index index = new Index (indexName, tableName, attrName);
        System.out.println("Try to create index");
        System.out.println("Index name: " + indexName);
        System.out.println("Table name: " + tableName);
        System.out.println("Attribute name: " + attrName);
        if (API.create_index(index)) {
            System.out.println("Create index successfully");
        }
    }

    private static void parse_drop_index(String statement) {
        System.out.println("In parse_drop_index..");
        String[] tokens = statement.split(" ");

        if (tokens.length == 2) {
            System.out.println("Syntax error: Not specify index name");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Syntax error: Extra parameters in drop index");
            return;
        }

        String indexName = tokens[2]; //get table name

        System.out.println("Try to drop the following index:");
        System.out.println("Index name: " + indexName);

        //need to check whether it's an index?
        if (API.drop_index(indexName)) {
            System.out.println("Drop index successfully");
        }


    }

    private static void parse_select(String statement) {
        //System.out.println("In parse_select..");
        //select ... from ... where ...
        String attrStr = Utils.substring(statement, "select ", " from");
        String tabStr = Utils.substring(statement, "from ", " where");
        String conStr = Utils.substring(statement, "where ", "");
        Vector<Condition> conditions;
        Vector<String> attrNames;
        if (attrStr.equals("")) {
            System.out.println("Syntax error: Can not find key word 'from' or lack of blank before from!");
            return;
        }
        if (attrStr.trim().equals("*")) {
            //select all attributes
            if (tabStr.equals("")) {  // select * from [];
                tabStr = Utils.substring(statement, "from ", "");
                Vector<TableRow> ret = API.select(tabStr, new Vector<>(), new Vector<>());
                for (int i = 0; i < ret.size(); i++) Utils.printRow(ret.get(i));
            } else { //select * from [] where [];
                String[] conSet = conStr.split(" *and *");
                //get condition vector
                try {
                    conditions = Utils.create_conditon(conSet);
                    Vector<TableRow> ret = API.select(tabStr, new Vector<>(), conditions);
                    for (int i = 0; i < ret.size(); i++) Utils.printRow(ret.get(i));
                } catch (Exception e) {
                    System.out.print(e.getMessage());
                }
            }
        } else {
            attrNames = Utils.convert(attrStr.split(" *, *")); //get attributes list
            if (tabStr.equals("")) {  //select [attr] from [];
                tabStr = Utils.substring(statement, "from ", "");
                Vector<TableRow> ret = API.select(tabStr, attrNames, new Vector<>());
                for (int i = 0; i < ret.size(); i++) Utils.printRow(ret.get(i));
            } else { //select [attr] from [table] where
                String[] conSet = conStr.split(" *and *");
                //get condition vector
                try {
                    conditions = Utils.create_conditon(conSet);
                    Vector<TableRow> ret =  API.select(tabStr, attrNames, conditions);
                    for (int i = 0; i < ret.size(); i++) Utils.printRow(ret.get(i));
                } catch (Exception e) {
                    System.out.print(e.getMessage());
                }
            }
        }
    }



    private static void parse_insert(String statement) {
        System.out.println("In parse_insert..");
        statement = statement.replaceAll(" *\\( *", " (").replaceAll(" *\\) *", ") ");
        statement = statement.replaceAll(" *, *", ",");
        statement = statement.trim();
        statement = statement.replaceAll("^insert","").trim();  //skip insert keyword

        int startIndex, endIndex;
        if (statement.equals("")) {
            System.out.println("Syntax error: Must add keyword 'into' after insert ");
            return;
        }

        endIndex = statement.indexOf(" "); //check into keyword
        if (endIndex == -1) {
            System.out.println("Syntax error: Not specfiy the table name");
            return;
        }
        if (!statement.substring(0, endIndex).equals("into")) {
            System.out.println("Syntax error: Must add keyword 'into' after insert ");
            return;
        }

        startIndex = endIndex + 1;
        endIndex = statement.indexOf(" ", startIndex); //check table name
        if (endIndex == -1) {
            System.out.println("Syntax error: Not specfiy the insert value");
            return;
        }

        String tableName = statement.substring(startIndex, endIndex); //get table name

        /*
        if (CatalogManager.get_table(tableName) == null) {
            System.out.println("RunTime error: The table " + tableName +" dosen't exist");
            return;
        }*/

        startIndex = endIndex + 1;
        endIndex = statement.indexOf(" ", startIndex); //check values keyword
        if (endIndex == -1) {
            System.out.println("Syntax error: Not specfiy the insert value");
            return;
        }
        if (!statement.substring(startIndex, endIndex).equals("values")) {
            System.out.println("Syntax error: Must add keyword 'values' after table " + tableName);
            return;
        }

        startIndex = endIndex + 1;
        if (!statement.substring(startIndex).matches("^\\(.*\\)$")) { //check brackets
            System.out.println("Syntax error: Can't not find the insert brackets in table " + tableName);
            return;
        }

        String[] valueParas = statement.substring(startIndex + 1).split(","); //get attribute tokens
        TableRow tableRow = new TableRow();
/*
        if(valueParas.length != CatalogManager.get_attribute_num(tableName)) {
            System.out.println("Logical error: Number of attribute mismatch");
        }*/

        for (int i = 0;i < valueParas.length;i++) {
            if (i == valueParas.length - 1) { //last attribute
                valueParas[i] = valueParas[i].substring(0, valueParas[i].length() - 1);
            }
            if (valueParas[i].equals("")) { //empty attribute
                System.out.println("Syntax error: Empty attribute value in insert value");
                return;
            }
            if (valueParas[i].matches("^\".*\"$") || valueParas[i].matches("^\'.*\'$")) { // extract from '' or " "
                valueParas[i] = valueParas[i].substring(1, valueParas[i].length() - 1);
            } else if(!valueParas[i].matches("^[^\"\'].*[^\"\']$")) { // quotation mismatch
                System.out.println("Syntax error: Quotation mismatch in " + valueParas[i]);
                return;
            }

 /*         String type = CatalogManager.get_type(tableName, i);
            switch (type) { //check type
                case "INT":
                    try {
                        Integer.parseInt(valueParas[i]);
                    } catch (NumberFormatException e) {
                        System.out.println("Type error: " + valueParas[i] + " dosen't match int type or overflow");
                        return;
                    }
                    break;
                case "FLOAT":
                    try {
                        Float.parseFloat(valueParas[i]);
                    } catch (NumberFormatException e) {
                        System.out.println("Type error: " + valueParas[i] + " dosen't match float type or overflow");
                        return;
                    }
                case "CHAR":
                    if(CatalogManager.get_length(tableName,i) < valueParas[i].length()) {
                        System.out.println("Type error: The char number" + valueParas[i] +
                                " must be limited in " + CatalogManager.get_length(tableName,i) + "bytes");
                        return;
                    }
                default:
                    System.out.println("RunTime error: Can't identify the type");
                    return;
            }*/
            tableRow.add_attribute_value(valueParas[i]); //add to table row
        }
        System.out.println("Try to insert");
        System.out.println("Table name: " + tableName);
        System.out.println("Attribute value:");
        for(int i = 0;i < tableRow.get_attribute_size();i++) {
            System.out.println(tableRow.get_attribute_value(i)+" ");
        }

        if (API.insert_row(tableName,tableRow)) {
            System.out.println("Insert successfully");
        }
    }

    private static void parse_delete(String statement) {
        //delete from [tabName] where []
        int num = 0;
        String tabStr = Utils.substring(statement, "from ", " where").trim();
        String conStr = Utils.substring(statement, "where ", "").trim();
        Vector<Condition> conditions;
        Vector<String> attrNames;
        if (tabStr.equals("")) {  //delete from ...
            tabStr = Utils.substring(statement, "from ", "").trim();
            num = API.delete_row(tabStr, new Vector<>());
            System.out.println("Query ok! " + num + "row(s) deleted");
        } else {  //delete from ... where ...
            String[] conSet = conStr.split(" *and *");
            //get condition vector
            try {
                conditions = Utils.create_conditon(conSet);
                API.delete_row(tabStr, conditions);
            } catch (Exception e) {
                System.out.print(e.getMessage());
            }
        }
    }


    private static void parse_quit(String statement, BufferedReader reader) {
        System.out.println("In parse_quit\n");
        String[] tokens = statement.split(" ");

        if (tokens.length != 1) {
            System.out.println("Syntax error: Extra parameters in quit");
            return;
        }
        System.out.println("Try to quit program");

        try{
            CatalogManager.store_catalog();
            RecordManager.store_record();
            reader.close();
            System.exit(0);
        } catch (IOException e) {
            System.out.println("RunTime error: can't close the input stream");
        }
    }

    private static void parse_sql_file(String statement) {
        System.out.println("In parse_sql_file\n");
        String[] tokens = statement.split(" ");

        if (tokens.length != 2) {
            System.out.println("Syntax error: Extra parameters in sql file execution");
            return;
        }

        String fileName = tokens[1];
        try {
            BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
            if (nestLock) { //first enter in sql file execution
                System.out.println("Can't use nested file execution");
            } else {
                nestLock = true; //lock, avoid nested execution
                System.out.println("Try to execute sql file:");
                interpret(fileReader);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Can't find the file");
        } catch (IOException e) {
            System.out.println("RunTime error: IO exception occurs");
        } finally {
            nestLock = false; //unlock
        }
    }


}

class Utils {

    public static final int NONEXIST = -1;
    public static final String[] OPERATOR = {"=", "<>", "<", ">", "<=", ">="};

    public static String substring(String str, String start, String end) {
        String regex = start + "(.*)" + end;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) return matcher.group(1);
        else return "";
    }

    public static <T> Vector<T> convert(T[] array) {
        Vector<T> v = new Vector<>();
        for (int i = 0; i < array.length; i++) v.add(array[i]);
        return v;
    }

    //ab <> 'c' | cab ="fabd"  | k=5  | char= '53' | int = 2
    public static Vector<Condition> create_conditon(String[] conSet) throws Exception {
        Vector<Condition> c = new Vector<>();
        for (int i = 0; i < conSet.length; i++) {
            int index = contains(conSet[i], OPERATOR);
            if (index == NONEXIST) throw new Exception("Syntax error: Invalid conditions " + conSet[i]);
            String attr = substring(conSet[i], "", OPERATOR[index]).trim();
            String value = substring(conSet[i], OPERATOR[index], "").trim().replace("\'", "").replace("\"", "");
            c.add(new Condition(attr, OPERATOR[index], value));
        }
        return c;
    }

    public static boolean check_type(String attr, boolean flag) {
        return true;
    }

    public static int contains(String str, String[] reg) {
        for (int i = 0; i < reg.length; i++) {
            if (str.contains(reg[i])) return i;
        }
        return NONEXIST;
    }

    public static void printRow(TableRow row) {
        for (int i = 0; i < row.get_attribute_size(); i++) {
            System.out.print(row.get_attribute_value(i) + "\t");
        }
        System.out.println();
    }

}
