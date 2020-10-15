package com.company;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//regex: (?!\[\[[^#]*\]\])\[\[[^\|]*#[^\]]*]]
public class Main {
    static String title;
    static void parserRedirectov() {
        try {
            File myObj = new File("skratenaVerzia.xml");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                if(data.contains("<title>")) {
                    title = data;
                }
                String regex = "(?!\\[\\[[^#]*\\]\\])\\[\\[[^\\|]*#[^\\]]+]]";

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher
                        = pattern
                        .matcher(data);

                while (matcher.find()) {

                    if(matcher.group().indexOf("#") == 2) {
                        title = title.replace("<title>", "")
                                .replace("</title>", "").trim();
                        System.out.println(title);
                    }
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

