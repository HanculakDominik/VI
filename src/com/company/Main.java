package com.company;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//regex: (?!\[\[[^#]*\]\])\[\[[^\|]*#[^\]]*]]
class Page{
    public String title;
    public ArrayList<String> sections;
    public Page(String title, String section){
        this.title = title;
        this.sections = new ArrayList<>();
        this.sections.add(section);
    }
}
public class Main {
    private static ArrayList<Page> linkedPages = new ArrayList<>();
    private static void parserRedirectov() {

        try {
            File myObj = new File("skratenaVerzia.xml");
            Scanner myReader = new Scanner(myObj);
            String title = "";
            int lastIndex = -1;
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
                    String found = matcher.group().substring(0,matcher.group().length() -2).substring(2);
                    if (found.indexOf("#") == 0) {
                        title = title.replace("<title>", "")
                                .replace("</title>", "").trim();
                        found = title + found;
                        lastIndex = saveLink(found,lastIndex);
                    } else if (isRedirect) {
                        if(redirect == null) {
                             title = title.replace("<title>", "")
                                    .replace("</title>", "").trim();
                            redirect = found;
                            lastIndex = saveLink(found,lastIndex);
                        }
                    } else {
                        lastIndex = saveLink(found,lastIndex);
                    }


                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        linkedPages.forEach(page -> System.out.println(page.title));
        System.out.println(linkedPages.size());
    }

    private static int saveLink(String link, int lastIndex){
        int hashtagIndex = link.indexOf("#");
        String pageTitle = link.substring(0,hashtagIndex);
        String sectionName = link.substring(hashtagIndex + 1);
        if(lastIndex != -1 && linkedPages.get(lastIndex).title.equals(pageTitle)) {
            linkedPages.get(lastIndex).sections.add(sectionName);
            return lastIndex;
        } else {
            int c = 0;
            for (Page var : linkedPages) {
                if (var.title.equals(pageTitle)) {
                    var.sections.add(sectionName);
                    return c;
                }
                c++;
            }
        }
        linkedPages.add(new Page(pageTitle,sectionName));
        return linkedPages.size() - 1;
    }
    public static void main(String[] args) {
        parserRedirectov();
    }
}

