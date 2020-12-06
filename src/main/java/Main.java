import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
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
    private final static String inputFilePath = "skratenaVerzia.xml";
    private static void parserRedirectov() {

        try {
            File myObj = new File(inputFilePath);
            FileWriter alternativeNames = new FileWriter("alternativeNames.txt");
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
                            int nameIndex = redirect.indexOf("|");
                            if(nameIndex != -1){
                                redirect = redirect.substring(0,nameIndex);
                            }
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
        int nameIndex = link.indexOf("|");
        if(nameIndex != -1){
            link = link.substring(0,nameIndex);
        }
        int hashtagIndex = link.indexOf("#");
        String pageTitle = link.substring(0,hashtagIndex);
        String sectionName = link.substring(hashtagIndex + 1);
        if(lastIndex != -1 && linkedPages.get(lastIndex).title.equals(pageTitle)) {
            boolean exists = false;
            for (String var : linkedPages.get(lastIndex).sections){
                if(var.equals(sectionName)){
                    exists = true;
                    break;
                }
            }
           if(!exists) {
               linkedPages.get(lastIndex).sections.add(sectionName);
               System.out.println(sectionName);
               counter++;
           }

            return lastIndex;
        } else {
            int c = 0;
            for (Page var : linkedPages) {
                if (var.title.equals(pageTitle)) {
                    boolean exists = false;
                    for (String section : var.sections){
                        if(var.equals(sectionName)){
                            exists = true;
                            break;
                        }
                    }
                    if(!exists) {
                        var.sections.add(sectionName);
                        System.out.println(sectionName);
                        counter++;
//                        System.out.println(counter);
                    }

                    return c;
                }
                c++;
            }
        }

        linkedPages.add(new Page(pageTitle,sectionName));
        return linkedPages.size() - 1;
    }
    private static void sections() {
        try {
            File myObj = new File(inputFilePath);
            Scanner myReader = new Scanner(myObj);
            String regex = "==.+==";
            Pattern pattern = Pattern.compile(regex);
            String sectionName;
            String text = "";
            Page linkedPage = null;
            boolean write = false;
            int index = -1;
//            JSONObject obj = new JSONObject();
            FileWriter jsonFile = new FileWriter("Sections.json");
            jsonFile.write('[');
            jsonFile.flush();
            ArrayList<JSONObject> active = new ArrayList<>();
            while (myReader.hasNextLine()) {
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
                        int aindex = -1;
                        if(sectionName.charAt(4) == '='){
                            aindex = 4;
                        } else if(sectionName.charAt(3) == '='){
                            aindex = 3;
                        } else if(sectionName.charAt(2) == '='){
                            aindex = 2;
                        } else if(sectionName.charAt(1) == '='){
                            aindex = 1;
                        }
//                        System.out.println(linkedPage.sections.size());
                        for (JSONObject obj: active) {
                            if ((int)obj.get("Level") >= aindex) {
                                if (write) {
                                    saveSections(jsonFile, obj);
                                    obj.replace("Level", -1);
//                                    write = false;
                                }

                            } else {
                                obj.replace("Text", obj.get("Text") + data + "\n");
                            }
                        }
                        active.removeIf(o -> (int)o.get("Level") == -1);

                        sectionName = sectionName.replace("=====", "").replace("====", "")
                                .replace("===", "").replace("==", "").trim();
                        for (String var : linkedPage.sections) {
                            if (var.equals(sectionName)) {
                                System.out.println("True");
                                write = true;
                                JSONObject obj = new JSONObject();
                                obj.put("Level", aindex);
                                obj.put("Name", linkedPage.title + "#" + sectionName);
                                obj.put("Text", "");
                                active.add(obj);
                                break;
                            }
                        }


                    } else if (write) {
                        if (data.startsWith("[[Kategória:") || data.contains("</text>")){
                            for (JSONObject obj: active) {
                                saveSections(jsonFile, obj);
                            }
                            active.clear();
                            write = false;
                        } else {
                            for (JSONObject obj: active) {
                                obj.replace("Text", obj.get("Text") + data + "\n");
                            }
                        }
                    }
                }

            }
            jsonFile.write(']');
            jsonFile.flush();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void saveSections(FileWriter file, JSONObject obj) {
        try {
            counter--;
            System.out.println(counter);
            System.out.println("tu som");
            file.write(obj.toJSONString() + ',');
            System.out.println(obj);
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws IOException, ParseException {

        Scanner in = new Scanner(System.in);

        if (!(new File("Sections.json")).exists()) {
            parserRedirectov();
            sections();
            Index.createIndex();
        }

        while (true) {
            System.out.println("Vložte odkaz('s' - to stop): ");
            String input = in.nextLine();

            if(input.equals("s"))
                break;

            JSONObject section = Index.getSection(input);
            System.out.println(section.get("Name"));
            System.out.println(section.get("Text"));
        }

    }
}

