package de.checkerce.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FileReader {
    public static String readFile(String filename) {
        List<String> lines = new ArrayList<String>();
        try {
            File myObj = new File(filename);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                lines.add(data);
            }
            String data = String.join("\n", lines);
            myReader.close();
            return data;
        } catch (FileNotFoundException e) {
            System.out.println("Error: File not Found");
            e.printStackTrace();
        }
        return null;
    }
}
