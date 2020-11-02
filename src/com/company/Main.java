package com.company;
import org.json.simple.JSONObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileWriter;

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
    private static int counter = 0;
    private static void parserRedirectov() {

        try {
            File myObj = new File("skwiki-latest-pages-articles.xml");
            FileWriter alternativeNames = new FileWriter("alternativeNames.json");
            Scanner myReader = new Scanner(myObj);
            String title = "";
            int lastIndex = -1;
            boolean isRedirect = false;
            String redirect = null;
            String regex = "(?!\\[\\[[^#]*\\]\\])\\[\\[[^\\|]*#[^\\]]+]]";
            while (myReader.hasNextLine()) {
                counter++;
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
                            alternativeNames.write(title + "|" + redirect + "\n");
                            alternativeNames.flush();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
        //linkedPages.forEach(page -> System.out.println(page.title));
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
        System.out.println(counter);
        linkedPages.add(new Page(pageTitle,sectionName));
        return linkedPages.size() - 1;
    }
    private static void sections() {
        try {
            File myObj = new File("skwiki-latest-pages-articles.xml");
            Scanner myReader = new Scanner(myObj);
            String regex = "==.+==";
            Pattern pattern = Pattern.compile(regex);
            String sectionName;
            String text = "";
            Page linkedPage = null;
            boolean write = false;
            JSONObject obj = new JSONObject();
            FileWriter jsonFile = new FileWriter("Sections.json");
            while (myReader.hasNextLine()) {
                counter--;
                String data = myReader.nextLine();
                Matcher matcher = pattern.matcher(data);
                if (data.contains("<title>")) {
                    linkedPage = null;
                    String title = data.replace("<title>", "")
                            .replace("</title>", "").trim();
                    for (Page var : linkedPages) {
                        if (var.title.equals(title)) {
                            linkedPage = var;
                            break;
                        }
                    }
                }
                if (linkedPage != null) {
                    if (matcher.find()) {
                        sectionName = matcher.group();

                        if(sectionName.charAt(2) != '=') {
                            sectionName = sectionName.replace("==", "").trim();
                            if (write){
                                obj.put("Text", text);
                                saveSections(jsonFile, obj);
                                text = "";
                            }
                            write = false;
                            for (String var : linkedPage.sections) {
                                if (var.equals(sectionName)) {
                                    write = true;
                                    obj = new JSONObject();
                                    obj.put("Name", linkedPage.title + "#" + sectionName);
                                    break;
                                }
                            }
                        }
                    } else if (write) {
                        if (data.startsWith("[[Kategória:") || data.contains("</text>")){
                            write = false;
                            obj.put("Text", text);
                            saveSections(jsonFile, obj);
                            text = "";
                        } else {
                            text += data;
                        }
                    }
                }

            }
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void saveSections(FileWriter file, JSONObject obj) {
        try {
            System.out.println(counter);
            file.write(obj.toJSONString());
            System.out.println(obj);
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        parserRedirectov();
        sections();
    }
}

