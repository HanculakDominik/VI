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

class Page {
    public String title;
    public ArrayList<String> sections;

    public Page(String title, String section) {
        this.title = title;
        this.sections = new ArrayList<>();
        this.sections.add(section);
    }
}

public class Main {
    private static ArrayList<Page> linkedPages = new ArrayList<>();
    private final static String inputFilePath = "data/skwiki-latest-pages-articles.xml";

    /**
     * This method parses links and redirects from file(inputFilePath).
     */
    private static void linkParser() {

        try {
            File myObj = new File(inputFilePath);
            FileWriter alternativeNames = new FileWriter("AlternativeNames.json");
            alternativeNames.write('[');
            alternativeNames.flush();
            Scanner myReader = new Scanner(myObj);
            String title = "";
            int lastIndex = -1;
            boolean isRedirect = false;
            String redirect = null;
            String regex = "(?!\\[\\[[^#]*\\]\\])\\[\\[[^\\|\\[]*#[^\\]]+\\]\\]";
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                if (data.contains("<title>")) {
                    title = data;
                    isRedirect = false;
                    redirect = null;
                }
                isRedirect = data.contains("<redirect title=") || isRedirect;

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(data);

                while (matcher.find()) {
                    String found = matcher.group().substring(0, matcher.group().length() - 2).substring(2);
                    if (found.indexOf("#") == 0) {
                        /*
                        Parsing in page links (anchors)
                         */
                        title = title.replace("<title>", "")
                                .replace("</title>", "").trim();
                        found = title + found;
                        lastIndex = saveLink(found, lastIndex);
                    } else if (isRedirect) {
                        /*
                        Parsing redirects
                         */
                        if (redirect == null) {
                            title = title.replace("<title>", "")
                                    .replace("</title>", "").trim();
                            redirect = found;
                            int nameIndex = redirect.indexOf("|");
                            if (nameIndex != -1) {
                                redirect = redirect.substring(0, nameIndex);
                            }
                            JSONObject obj = new JSONObject();
                            obj.put("PageName", title);
                            int hashtagIndex = redirect.indexOf("#");
                            obj.put("RePageName", redirect.substring(0, hashtagIndex));
                            obj.put("ReSectionName", redirect.substring(hashtagIndex + 1));
                            alternativeNames.write(obj.toJSONString() + ',');
                            alternativeNames.flush();
                            lastIndex = saveLink(found, lastIndex);
                        }
                    } else {
                        /*
                        Saving basic links (page_name#section_name)
                         */
                        lastIndex = saveLink(found, lastIndex);
                    }
                }
            }
            alternativeNames.write(']');
            alternativeNames.flush();
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method saves Links to array linkedPages in memory.
     *
     * @param link      Link to save
     * @param lastIndex Index of last saved link for faster saving
     * @return Index of saved link
     */
    private static int saveLink(String link, int lastIndex) {
        int nameIndex = link.indexOf("|");
        if (nameIndex != -1) {
            link = link.substring(0, nameIndex);
        }
        int hashtagIndex = link.indexOf("#");
        String pageTitle = link.substring(0, hashtagIndex);
        String sectionName = link.substring(hashtagIndex + 1);
        if (lastIndex != -1 && linkedPages.get(lastIndex).title.equals(pageTitle)) {
            /*
            Saves link if page is same as page previously saved for faster saving
             */
            boolean exists = false;
            for (String var : linkedPages.get(lastIndex).sections) {
                if (var.equals(sectionName)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                linkedPages.get(lastIndex).sections.add(sectionName);
            }
            return lastIndex;

        } else {
            /*
            Saves link if link with same page name has been already saved
             */
            int index = 0;
            for (Page var : linkedPages) {
                if (var.title.equals(pageTitle)) {
                    boolean exists = false;
                    for (String section : var.sections) {
                        if (section.equals(sectionName)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        var.sections.add(sectionName);
                    }

                    return index;
                }
                index++;
            }
        }

        linkedPages.add(new Page(pageTitle, sectionName));
        return linkedPages.size() - 1;
    }

    /**
     * This method parses sections
     * from file(inputFilePath) and compares them to links.
     */
    private static void sectionParser() {
        try {
            File myObj = new File(inputFilePath);
            Scanner myReader = new Scanner(myObj);
            String regex = "==.+==";
            Pattern pattern = Pattern.compile(regex);
            String sectionName;
            Page linkedPage = null;
            boolean write = false;
            FileWriter jsonFile = new FileWriter("Sections.json");
            jsonFile.write('[');
            jsonFile.flush();
            ArrayList<JSONObject> active = new ArrayList<>();
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                Matcher matcher = pattern.matcher(data);
                if (data.contains("<title>")) {
                    /*
                    Matching current page with linked pages
                    to check if sections from page could be parsed
                     */
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
                        if (sectionName.charAt(4) == '=') {
                            aindex = 4;
                        } else if (sectionName.charAt(3) == '=') {
                            aindex = 3;
                        } else if (sectionName.charAt(2) == '=') {
                            aindex = 2;
                        } else if (sectionName.charAt(1) == '=') {
                            aindex = 1;
                        }

                        for (JSONObject obj : active) {
                            if ((int) obj.get("Level") >= aindex) {
                                /*
                                If section level is smaller or same as current read headline,
                                section is saved
                                 */
                                if (write) {
                                    saveSections(jsonFile, obj);
                                    obj.replace("Level", -1);
                                }

                            } else {
                                obj.replace("Text", obj.get("Text") + data + "\n");
                            }
                        }
                        active.removeIf(o -> (int) o.get("Level") == -1);

                        sectionName = sectionName.replace("=====", "").replace("====", "")
                                .replace("===", "").replace("==", "").trim();
                        for (String var : linkedPage.sections) {
                            if (var.equals(sectionName)) {
                                write = true;
                                JSONObject obj = new JSONObject();
                                obj.put("Level", aindex);
                                obj.put("PageName", linkedPage.title);
                                obj.put("SectionName", sectionName);
                                obj.put("Text", "");
                                active.add(obj);
                                break;
                            }
                        }

                    } else if (write) {
                        if (data.startsWith("[[Kategória:") || data.contains("</text>")) {
                            /*
                            If end of page is read all active sections are saved
                             */
                            for (JSONObject obj : active) {
                                saveSections(jsonFile, obj);
                            }
                            active.clear();
                            write = false;
                        } else {
                            for (JSONObject obj : active) {
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

    /**
     * This method saves section in json format to file.
     *
     * @param writer Writer which writes to defined file
     * @param obj    JSONObject to write to file
     */
    private static void saveSections(FileWriter writer, JSONObject obj) {
        try {
            writer.write(obj.toJSONString() + ',');
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main method with UI functionality.
     *
     * @param args Unused
     * @throws IOException    Exceptions from index manipulation
     * @throws ParseException Exceptions from json parser
     */
    public static void main(String[] args) throws IOException, ParseException {

        Scanner in = new Scanner(System.in);

        if (!(new File("Sections.json")).exists()) {
            linkParser();
            sectionParser();
            Index.createIndex("Sections.json", "documents");
            Index.createIndex("AlternativeNames.json", "alt_names");
        }
        boolean running = true;
        while (running) {
            System.out.println("\n---------- Menu ----------\n" +
                    "0 - Koniec\n" +
                    "1 - Hľadať podla názvu stránky\n" +
                    "2 - Hľadať podľa názvu sekcie\n" +
                    "3 - Hľadať podľa názvu stránky a sekcie\n" +
                    "4 - Štatistiky");
            String input = in.nextLine();
            try {
                switch (Integer.parseInt(input)) {
                    case 0: {
                        running = false;
                        break;
                    }
                    case 1: {
                        System.out.println("Zadajte názov stránky:");
                        input = in.nextLine();
                        ArrayList<JSONObject> objects = Index.getSection(input, null, "documents");
                        if (objects.size() != 0) {
                            System.out.println("\nNázvy sekcií, ktoré sa nachádzajú na stránke:");
                            for (JSONObject o : objects) {
                                System.out.println(o.get("SectionName"));
                            }
                        } else {
                            objects = Index.getSection(input, null, "alt_names");
                            for (JSONObject o : objects) {
                                ArrayList<JSONObject> documents = Index.getSection((String) o.get("RePageName"),
                                        (String) o.get("ReSectionName"), "documents");
                                for (JSONObject doc : documents) {
                                    System.out.println("----------- " + doc.get("PageName")
                                            + "#" + doc.get("SectionName") + " -----------\n");
                                    System.out.println(doc.get("Text"));
                                }
                            }
                        }
                        break;
                    }
                    case 2: {
                        System.out.println("Zadajte názov sekcie:");
                        input = in.nextLine();
                        ArrayList<JSONObject> objects = Index.getSection(null, input, "documents");
                        System.out.println("\nNázvy stránok, ktoré obsahujú sekciu:");
                        for (JSONObject o : objects) {
                            System.out.println(o.get("PageName"));
                        }
                        break;
                    }
                    case 3: {
                        System.out.println("Zadajte názov stránky:");
                        String pageName = in.nextLine();
                        System.out.println("Zadajte názov sekcie:");
                        String sectionName = in.nextLine();
                        ArrayList<JSONObject> objects = Index.getSection(pageName, sectionName, "documents");
                        for (JSONObject o : objects) {
                            System.out.println("-----------" + o.get("PageName")
                                    + "#" + o.get("SectionName") + "-----------");
                            System.out.println(o.get("Text"));
                        }
                        break;
                    }
                    case 4: {

                        ArrayList<String> terms = Index.getStatistics("PageName");
                        System.out.println("Top 10 názvov stránok (Podľa počtu výskytov):");
                        for (int i = 0; i < terms.size(); i++) {
                            System.out.println((i + 1) + ". " + terms.get(i));
                        }

                        System.out.println("\n");

                        terms = Index.getStatistics("SectionName");
                        System.out.println("Top 10 názvov sekcií (Podľa počtu výskytov):");
                        for (int i = 0; i < terms.size(); i++) {
                            System.out.println((i + 1) + ". " + terms.get(i));
                        }

                        System.out.println("\n");

                        terms = Index.getStatistics("Level");
                        System.out.println("Počet výskytov úrovni nadpisov (1-4)");
                        for (int i = 0; i < terms.size(); i++) {
                            System.out.println((i + 1) + ". " + terms.get(i));
                        }
                        break;
                    }

                }
            } catch (NumberFormatException e) {
                System.out.println("Prosím vložte číslo");
            }

        }

    }
}

