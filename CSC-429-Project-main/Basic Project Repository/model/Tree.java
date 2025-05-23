package model;

import java.util.Objects;
import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;
import java.sql.*;
import exception.InvalidPrimaryKeyException;

import static com.sun.corba.se.impl.util.Utility.printStackTrace;

public class Tree extends EntityBase {
    private static String tableName = "tree";
    protected Properties persistentState;
    protected Properties dependencies;
    private String updateStatusMessage = "";

    public Tree() {
        super(tableName);
        setDependencies();
        persistentState = new Properties();
    }

    public Tree(String barcode) throws InvalidPrimaryKeyException {
        super(tableName);

        String query = "SELECT * FROM " + tableName + " WHERE Barcode = '" + barcode + "'";
        Vector<Properties> dataRetrieved = getSelectQueryResult(query);

        if (dataRetrieved != null) {
            int size = dataRetrieved.size();
            if (size != 1) {
                throw new InvalidPrimaryKeyException("Multiple trees found for barcode: " + barcode);
            } else {
                Properties retrievedTreeData = dataRetrieved.elementAt(0);
                persistentState = new Properties();

                Enumeration<?> allKeys = retrievedTreeData.propertyNames();
                while (allKeys.hasMoreElements()) {
                    String nextKey = (String) allKeys.nextElement();
                    String nextValue = retrievedTreeData.getProperty(nextKey);
                    if (nextValue != null) {
                        persistentState.setProperty(nextKey, nextValue);
                    }
                }
            }
        } else {
            throw new InvalidPrimaryKeyException("No tree found for barcode: " + barcode);
        }
    }

    public Tree(Properties props) {
        super(tableName);
        setDependencies();

        persistentState = new Properties();
        Enumeration<?> allKeys = props.propertyNames();
        while (allKeys.hasMoreElements()) {
            String nextKey = (String) allKeys.nextElement();
            String nextValue = props.getProperty(nextKey);
            if (nextValue != null) {
                persistentState.setProperty(nextKey, nextValue);
            }
        }
    }

    private void setDependencies() {
        dependencies = new Properties();
        myRegistry.setDependencies(dependencies);
    }

    public Object getState(String key) {
        if (key.equals("UpdateStatusMessage")) {
            return updateStatusMessage;
        }
        return persistentState.getProperty(key);
    }

    public void stateChangeRequest(String key, Object value) {
        persistentState.setProperty(key, value.toString());
        myRegistry.updateSubscribers(key, this);
    }

    public Vector<String> getEntryListView() {
        Vector<String> v = new Vector<>();
        v.addElement(persistentState.getProperty("Barcode"));
        v.addElement(persistentState.getProperty("Tree_Type"));
        v.addElement(persistentState.getProperty("Notes"));
        v.addElement(persistentState.getProperty("Status"));
        v.addElement(persistentState.getProperty("DateStatusUpdated"));
        return v;
    }

    public void updateStateInDatabase() {
        try {
            try {
                insertPersistentState(mySchema, persistentState);
                updateStatusMessage = "New Tree with Barcode " + persistentState.getProperty("Barcode") + " inserted successfully!";
                System.out.println("Insert successful.");
            } catch (SQLException ex) {
                System.out.println("Insert failed, attempting update...");

                try {
                    Properties whereClause = new Properties();
                    whereClause.setProperty("Barcode", persistentState.getProperty("Barcode"));
                    updatePersistentState(mySchema, persistentState, whereClause);
                    updateStatusMessage = "Tree data for Barcode " + persistentState.getProperty("Barcode") + " updated successfully!";
                    System.out.println("Update successful.");
                } catch (Exception updateException) {
                    System.out.println("Update also failed.");
                    throw new Exception("Failed to insert or update Tree with Barcode " + persistentState.getProperty("Barcode"), updateException);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//    public void updateStateInDatabase() {
//        try
//        {
//           // insertPersistentState(mySchema, persistentState);
//             if (persistentState.getProperty("Barcode") != null) { //INSERTING
//                System.out.println("should be inserting");
//                insertPersistentState(mySchema, persistentState); // THIS USED TO BE insertPersistentState DONT FORGET FOR DEBUG
//
//                Properties whereClause = new Properties();
//                whereClause.setProperty("Barcode", persistentState.getProperty("Barcode"));
//            updatePersistentState(mySchema, persistentState, whereClause);
//
//
//               // updateStatusMessage = "Scout data for new Tree : " +  persistentState.getProperty("Barcode")
//                       // + "installed successfully in database!";
//
//            } else { //IF THERE IS NO BARCODE//were updating instead of inserting
//                {
//                    System.out.println("startofupdatestateindatabase section1");
//
//                    Properties whereClause = new Properties();
//                    whereClause.setProperty("Barcode", persistentState.getProperty("Barcode"));
//                    updatePersistentState(mySchema, persistentState, whereClause);
//                    updateStatusMessage = "Tree data for Barcode: " + persistentState.getProperty("Barcode") + " updated successfully!";
//                    System.out.println("endofupdatestateindatabase section1");
//                }
//
//               // insertPersistentState(mySchema, persistentState);
//                //updateStatusMessage = "New Tree added successfully!";
//            }
//        }
//        catch (SQLException ex)
//        {
//            System.out.println("Error inserting/updating Tree data!");
//            updateStatusMessage = "Error updating Tree data!";
//        }
//    }

    public void processNewTree(Properties p) {
        //persistentState = new Properties();
        //SECTION THAT REFERENCES THE FK/TABLE FOR TREETYPE
        String query = "SELECT * FROM tree_type WHERE BarcodePrefix = '" + p.getProperty("Tree_Type") + "'";
        Vector result = getSelectQueryResult(query);
        String treeTypeID = ((Properties) result.firstElement()).getProperty("ID");


        persistentState.setProperty("Barcode", p.getProperty("Barcode"));
        persistentState.setProperty("Tree_Type", treeTypeID); //WORKING LINE
        persistentState.setProperty("Notes", p.getProperty("Notes"));
        persistentState.setProperty("Status", p.getProperty("Status"));
        persistentState.setProperty("DateStatusUpdated", p.getProperty("DateStatusUpdated"));

        try {
            updateStateInDatabase();
        } catch (Exception ex) {
            System.out.println("Failed to add TreeType to the database :(");
            ex.printStackTrace();
        }
    }

    public void deleteTree(Properties p){
        String query = "SELECT Status FROM tree WHERE Barcode = '" + p.getProperty("Barcode") + "'";
        Vector result = getSelectQueryResult(query);
        String statusTree = ((Properties) result.firstElement()).getProperty("Status");

        if (Objects.equals(statusTree, "Sold") || Objects.equals(statusTree, "Damaged")){
            System.out.println("Can't remove a sold or damage tree");
            updateStatusMessage = "Can't remove a sold or damage tree";
            displayErrorMessage("Can't remove a sold or damage tree");
            //printStackTrace();
        }else{
            String query1 = "DELETE FROM tree WHERE Barcode = '" + p.getProperty("Barcode") +"'";
            getSelectQueryResult(query1);
        }



    }

    public void displayErrorMessage(String message)
    {
        System.out.println("displayErrorMessage called with message: " + message);
        Tree statusLog = null;
        statusLog.displayErrorMessage(message);
    }

    protected void initializeSchema(String tableName) {
        if (mySchema == null) {
            mySchema = getSchemaInfo(tableName);
        }
    }

}
