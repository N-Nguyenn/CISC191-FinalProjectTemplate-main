package edu.sdccd.cisc191.template;
import java.sql.*;
import java.util.ArrayList;
import java.io.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Random;
import java.util.function.*;

public class Main {
    //setting up the database 'cps' with the root username and password
    public static ArrayList readFileData(String filePath) throws Exception {
        String url = "jdbc:mysql://localhost:3306/cps";
        String username = "nick";
        String password = "Coastal1!";
        Connection conn = DriverManager.getConnection(url, username, password);
        //connects to the database
        ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();
        //reads a txt file and puts data from the txt file into the 2d ArrayList
        File file = new File(filePath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String temp;
        while ((temp = br.readLine()) != null) {
            int i = temp.indexOf(",");
            String userName = temp.substring(0, i);
            String passWord = temp.substring(i + 1, temp.length() - 1);
            ArrayList<String> tempList = new ArrayList<String>();
            tempList.add(userName);
            tempList.add(passWord);
            list.add(tempList);
        }
        //calling the sort method and alphabetically sorting the username
        sortCredentials(list);
        //makes sure passwords are strong by making sure no passwords are less than 6 digits
        validatePassword((list));

        //adds all of the data from the 2d arraylist into the table of users in the database
        for (int i = 0; i < list.size(); i++) {
            try {
                String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, list.get(i).get(0));
                stmt.setString(2, list.get(i).get(1));
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Unable to insert rows into table");
                e.printStackTrace();
                break;
            }
        }
        return list;
    }

    //creates a ConcurrentSort2D object called sort and then sorts the list
    private static void sortCredentials(ArrayList list) throws Exception {
        ConcurrentSort2D sort = new ConcurrentSort2D();
        sort.sort2D(list);
    }

    //streams through the arraylist and also uses lambda function to check the length of the passwords
    //if password length is less than 6, generate a new random password in cps
    public static void validatePassword(ArrayList<ArrayList<String>> list) {
        //random and alpha are used to generate new passwords
        Random random = new Random();
        String alpha = "abcdefghijklmnopqrstuvwxyz1234567889";
        //lambda function to optimze code
        //stream through the password column to check password requirements
        list.stream().forEach(column -> {
            Function<String, Integer> getLength = n -> n.length();
            if(checkLength(column.get(1), getLength)) {
                String temp = "";
                for(int i = 0; i < 6; i++){
                    temp = temp + alpha.charAt(random.nextInt(36));
                }
                column.remove(1);
                column.add(temp);
            }
        });
    }

    //checks if password length is less than 6 , return a boolean
    public static boolean checkLength(String str, Function<String, Integer> getLength){
        if(getLength.apply(str) < 6) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws SQLException, Exception, ClassNotFoundException {
        // Connect to the database
        String url = "jdbc:mysql://localhost:3306/cps";
        String username = "nick";
        String password = "Coastal1!";
        Connection conn = DriverManager.getConnection(url, username, password);

        //populates the table from the .txt file
        //Checks if the "users" table exists if it doesn't exist, create a new users table
        //If it does exist, clean the table everytime so that the list gets updated
        try
        {
            String sql = "SHOW TABLES LIKE 'users'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                String refresher = "TRUNCATE TABLE users";
                stmt.execute(refresher); //table is now clean
                sql = "CREATE TABLE users (id INT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(50), password VARCHAR(50))";
                readFileData("C:\\Users\\nicka\\Documents\\CISC 191\\Coding\\CISC191-FinalProjectTemplate-main(Dec)\\Common\\src\\main\\java\\edu\\sdccd\\cisc191\\template\\users.txt");
                rs = stmt.executeQuery("SELECT * FROM users");
                while (rs.next()) {
                    for (int i = 0; i < 20; i++) {
                        System.out.print("-");
                    }
                    System.out.println();
                    int id = rs.getInt("id");
                    username = rs.getString("username");
                    password = rs.getString("password");
                    System.out.println(id + "  - " + username + " - " + password);
                    for (int i = 0; i < 20; i++) {
                        System.out.print("-");
                    }
                }
            } else {
                sql = "CREATE TABLE users (id INT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(50), password VARCHAR(50))";
                readFileData("C:\\Users\\nicka\\Documents\\CISC 191\\Coding\\CISC191-FinalProjectTemplate-main(Dec)\\Common\\src\\main\\java\\edu\\sdccd\\cisc191\\template\\users.txt");
                rs = stmt.executeQuery("SELECT * FROM users");
                while (rs.next()) {
                    for (int i = 0; i < 20; i++) {
                        System.out.print("-");
                    }
                    System.out.println();
                    int id = rs.getInt("id");
                    username = rs.getString("username");
                    password = rs.getString("password");
                    System.out.println(id + "  - " + username + " - " + password);
                    for (int i = 0; i < 20; i++) {
                        System.out.print("-");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        // Close the connection
        try {
            conn.close();
        } catch (SQLException e) {
            System.out.println("Unable to close connection");
            e.printStackTrace();
        }
    }
}

//concurrent mergesort with a limited number of threads
class ConcurrentSort2D {
    private static final int NUM_THREADS = 4;  // number of threads to use
    private ArrayList<ArrayList<String>> list;
    public void sort2D(ArrayList<ArrayList<String>> list) throws Exception {
        this.list = list;
        // Base case: if the list is empty or has only one element, it is already sorted
        if (list.size() <= 1) {
            return;
        }
        // Divide the list into two halves
        int mid = list.size() / 2;
        ArrayList<ArrayList<String>> left = new ArrayList<>(list.subList(0, mid));
        ArrayList<ArrayList<String>> right = new ArrayList<>(list.subList(mid, list.size()));
        // Recursively sort the two halves in separate threads
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        Callable<Void> leftSort = () -> {
            sort2D(left);
            return null;
        };
        Callable<Void> rightSort = () -> {
            sort2D(right);
            return null;
        };
        Future<Void> leftResult = executor.submit(leftSort);
        Future<Void> rightResult = executor.submit(rightSort);
        leftResult.get();
        rightResult.get();
        executor.shutdown();
        // Merge the sorted halves back together
        merge2D(list, left, right);
    }

    //merges the two arrays passed in, sorting the list
    private static void merge2D(ArrayList<ArrayList<String>> result, ArrayList<ArrayList<String>> left, ArrayList<ArrayList<String>> right) {
        // Indexes for the result, left, and right lists
        int i = 0, j = 0, k = 0;
        //Compare the elements at the current indexes of the left and right lists, and add the smaller one to the result
        while (i < left.size() && j < right.size()) {
            if (left.get(i).get(0).compareTo(right.get(j).get(0)) < 0) {
                result.set(k, left.get(i));
                i++;
            } else {
                result.set(k, right.get(j));
                j++;
            }
            k++;
        }
        // Add any remaining elements from the left list
        while (i < left.size()) {
            result.set(k, left.get(i));
            i++;
            k++;
        }
        // Add any remaining elements from the right list
        while (j < right.size()) {
            result.set(k, right.get(j));
            j++;
            k++;
        }
    }
}
