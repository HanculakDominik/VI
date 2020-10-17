package com.company;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//regex: (?!\[\[[^#]*\]\])\[\[[^\|]*#[^\]]*]]
public class Main {
    private static void parserRedirectov() {

        try {
            File myObj = new File("skratenaVerzia.xml");
            Scanner myReader = new Scanner(myObj);
            String title = "";
            boolean isRedirect = false;
            String redirect = null;
            String regex = "(?!\\[\\[[^#]*\\]\\])\\[\\[[^\\|]*#[^\\]]+]]";
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                if(data.contains("<title>")) {
                    title = data;
                    isRedirect = false;
                    redirect = null;
                }
                isRedirect = data.contains("<redirect title=") || isRedirect;

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(data);

                while (matcher.find()) {
                    if (matcher.group().indexOf("#") == 2) {
                        title = title.replace("<title>", "")
                                .replace("</title>", "").trim();
                        System.out.println(title + "::" + matcher.group());
                    } else if (isRedirect) {
                        if(redirect == null) {
                             title = title.replace("<title>", "")
                                    .replace("</title>", "").trim();
                            redirect = matcher.group();
                            System.out.println(title + "++" + redirect);
                        }
                    } else {
                        System.out.println(matcher.group());
                    }


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

