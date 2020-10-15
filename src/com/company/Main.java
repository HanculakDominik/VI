package com.company;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//regex: (?!\[\[[^#]*\]\])\[\[[^\|]*#[^\]]*]]
public class Main {

    static void parserRedirectov() {
        try {
            File myObj = new File("skratenaVerzia.xml");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String regex = "(?!\\[\\[[^#]*\\]\\])\\[\\[[^\\|]*#[^\\]]*]]";

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher
                        = pattern
                        .matcher(data);

                while (matcher.find()) {
                    System.out.println(matcher.group());
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        parserRedirectov();
    }
}

