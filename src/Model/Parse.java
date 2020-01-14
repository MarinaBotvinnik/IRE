
package Model;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class that in charge of parsing texts.
 * The class contains a list of stop_words, List of months, and an Indexer. 
 */
public class Parse {

    private HashSet<String> stopWords;
    private ConcurrentHashMap<String, String> months;
    private Indexer indexer;

    /**
     * Constructor of the class.
     * The method initializes the list of the stop words, and initializes the Indexer with the proper stem value
     *
     * @param stem - true if stem needed, false otherwise
     */
    public Parse(boolean stem) {
        stopWords = new HashSet<>();
        try {
            BufferedReader buffer = new BufferedReader(new InputStreamReader(new FileInputStream("Resource/stop_words.txt"), "UTF-8"));
            String st;
            //add all the stop-words in all their ways
            while ((st = buffer.readLine()) != null) {
                if (!st.equals("between") && !st.equals("and") && !st.equals("may") && !st.equals("m") && !st.equals("b") && !st.equals("t")) {
                    stopWords.add(st);
                    stopWords.add(st.toUpperCase());
                    stopWords.add(st.substring(0, 1).toUpperCase() + st.substring(1));
                }
            }
            stopWords.add("<P>");
            stopWords.add("</P>");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        months = new ConcurrentHashMap<>();
        indexer = new Indexer(stem);
    }

    public String parseQuery(String text,boolean isStem){
        StringBuilder parsed= new StringBuilder();
        String[] splitText = deleteStopWords(text);
        int textLength = splitText.length;
        for (int i = 0; i < textLength; i++) {
            String termTxt = splitText[i];
            if(isStem){
                Stemmer stemmer = new Stemmer();
                stemmer.add(termTxt.toCharArray(), termTxt.length());
                stemmer.stem();
                termTxt = stemmer.toString();
            }
            if (termTxt.length() > 1 && termTxt.charAt(0) == '.' && Character.isDigit(termTxt.charAt(1))) {
                termTxt = 0 + termTxt;
                splitText[i] = 0 + termTxt;
            }
            if (termTxt.length() == 0)
                continue;
            ////if the word is a PURE NUMBER
            if (isNum(splitText[i])) {
                ////if the word is Kg - (skip 1 word ahead + check if there is i+1 word)
                if (i + 1 < textLength && (splitText[i + 1].equals("kg") || splitText[i + 1].equals("Kg") || splitText[i + 1].equals("KG") || splitText[i + 1].equals("kilogram")
                        || splitText[i + 1].equals("Kilogram") || splitText[i + 1].equals("kilograms") || splitText[i + 1].equals("Kilograms")
                        || splitText[i + 1].equals("Kgs") || splitText[i + 1].equals("kgs"))) {
                    termTxt = splitText[i] + " kg";
                    parsed.append(" ").append(termTxt);
                    i++;
                    continue;
                }
                if (i + 1 < textLength && (splitText[i + 1].equals("gr") || splitText[i + 1].equals("gram") || splitText[i + 1].equals("Gram") || splitText[i + 1].equals("GRAM")
                        || splitText[i + 1].equals("grams") || splitText[i + 1].equals("Grams") || splitText[i + 1].equals("GRAMS"))) {
                    termTxt = splitText[i] + " gr";
                    parsed.append(" ").append(termTxt);
                    i++;
                    continue;
                }
                if (i + 1 < textLength && (splitText[i + 1].equals("ton") || splitText[i + 1].equals("Ton") || splitText[i + 1].equals("TON") || splitText[i + 1].equals("tons")
                        || splitText[i + 1].equals("Tons") || splitText[i + 1].equals("TONS"))) {
                    termTxt = splitText[i] + "K kg";
                    parsed.append(" ").append(termTxt);
                    i++;
                    continue;
                }
                ////if the word is Km - (skip 1 word ahead + check if there is i+1 word)
                if (i + 1 < textLength && (splitText[i + 1].equals("km") || splitText[i + 1].equals("Km") || splitText[i + 1].equals("KM") || splitText[i + 1].equals("kilometer")
                        || splitText[i + 1].equals("Kilometer") || splitText[i + 1].equals("kilometers") || splitText[i + 1].equals("Kilometers"))) {
                    termTxt = splitText[i] + "K meters";
                    parsed.append(" ").append(termTxt);
                    i++;
                    continue;
                }

                ////if the word is meter - (skip 1 word ahead + check if there is i+1 word)
                if (i + 1 < textLength && (splitText[i + 1].equals("meter") || splitText[i + 1].equals("Meter") || splitText[i + 1].equals("Meters") || splitText[i + 1].equals("meters"))) {
                    termTxt = splitText[i] + " meters";
                    parsed.append(" ").append(termTxt);
                    i++;
                    continue;
                }

                if (i + 1 < textLength && (splitText[i + 1].equals("Centimeter") || splitText[i + 1].equals("centimeter") || splitText[i + 1].equals("Centimeters")
                        || splitText[i + 1].equals("centimeters") || splitText[i + 1].equals("cm"))) {
                    termTxt = splitText[i] + " cm";
                    parsed.append(" ").append(termTxt);
                    i++;
                    continue;
                }
                ////if the number is PERCENT  - (skip 1 word ahead + check if there is i+1 word)
                if (i + 1 < textLength && (splitText[i + 1].equals("percent") || splitText[i + 1].equals("percentage") || splitText[i + 1].equals("%"))) {
                    termTxt = splitText[i] + "%";
                    parsed.append(" ").append(termTxt);
                    i++;
                    continue;
                }

                ////if the words of type PRICE M\B\T U.S DOLLARS - (skip 3 word ahead + check if there is i+3 word)
                if (i + 3 < textLength && (splitText[i + 2].equals("U.S") && (splitText[i + 3].equals("dollars") || splitText[i + 3].equals("Dollars")))) {
                    termTxt = getAmount(splitText, i, termTxt);
                    parsed.append(" ").append(termTxt);
                    i += 3;
                    continue;
                }

                // if the word is of type price m/bn Dollars
                if (i + 2 < textLength && (splitText[i + 2].equals("dollars") || splitText[i + 2].equals("Dollars"))) {
                    if (splitText[i + 1].equals("m") || splitText[i + 1].equals("M"))
                        termTxt = splitText[i] + "M Dollars";
                    if (splitText[i + 1].equals("bn") || splitText[i + 1].equals("BN")) {
                        double newVal = Double.parseDouble(splitText[i]);
                        newVal = newVal * 1000;
                        termTxt = newVal + "M Dollars";
                    }
                    parsed.append(" ").append(termTxt);
                    i += 2;
                    continue;
                }

                // if the word is of type PRICE DOLLARS
                if (i + 1 < textLength && (splitText[i + 1].equals("Dollars") || splitText[i + 1].equals("dollars"))) {
                    String price = termNum(Double.parseDouble(splitText[i]));
                    if (price.charAt(price.length() - 1) == 'B') {
                        double newVal = Double.parseDouble(splitText[i]);
                        newVal = newVal / 1000;
                        termTxt = newVal + "M Dollars";
                    } else {
                        termTxt = price + " Dollars";
                    }
                    parsed.append(" ").append(termTxt);
                    i++;
                    continue;
                }

                //if there is a FRACTION after the number
                if (i + 1 < textLength && isFraction(splitText[i + 1])) {
                    if (i + 2 < textLength && (splitText[i + 2].equals("dollars") || splitText[i + 2].equals("Dollars"))) {
                        termTxt = splitText[i] + " " + splitText[i + 1] + " Dollars";
                        i += 2;
                    } else {
                        termTxt = splitText[i] + " " + splitText[i + 1];
                        i++;
                    }
                    parsed.append(" ").append(termTxt);
                    continue;
                }

                //if the number is part of a date
                if (i + 1 < textLength && isMonth(splitText[i + 1])) {
                    int month = (int) Double.parseDouble(splitText[i]);
                    if (month < 10)
                        termTxt = monthNum(splitText[i + 1]) + "-0" + splitText[i];
                    else
                        termTxt = monthNum(splitText[i + 1]) + "-" + splitText[i];
                    parsed.append(" ").append(termTxt);
                    continue;
                }
                String termNum = termNum(Double.parseDouble(splitText[i]));
                parsed.append(" ").append(termNum);
            }
            //its a word or its a number with something attached to this
            else {
                if (splitText[i].charAt(0) == '$') { //if a $ is attached in the beginning
                    termTxt = getDollars(splitText, textLength, i, termTxt);
                    parsed.append(" ").append(termTxt);
                    i++;
                    continue;
                }

                //if a % is attached in the beginning
                if (splitText[i].charAt(splitText[i].length() - 1) == '%') {
                    termTxt = splitText[i];
                    parsed.append(" ").append(termTxt);
                    continue;
                }

                //the term is a month
                if (i + 1 < textLength && isMonth(splitText[i]) && isNum(splitText[i + 1])) {
                    int datePart = (int) Double.parseDouble(splitText[i + 1]);
                    //checking if the number indicates a day
                    if (datePart <= 31) {
                        if (datePart < 10)
                            termTxt = monthNum(splitText[i]) + "-0" + splitText[i + 1];
                        else
                            termTxt = monthNum(splitText[i]) + "-" + splitText[i + 1];
                    }
                    //else, then the number indicates years
                    else
                        termTxt = splitText[i + 1] + "-" + monthNum(splitText[i]);
                    parsed.append(" ").append(termTxt);
                    i++;
                    continue;
                }

                //if its the type WORD - WORD or WORD - WORD - WORD
                if (splitText[i].indexOf("-") >= 0 && !termTxt.equals("--") && !termTxt.equals("---")) {
                    String[] numbers = splitToNumbers(splitText[i]);
                    if (numbers[0].equals("true")) {
                        parsed.append(" ").append(numbers[1]);
                        parsed.append(" ").append(numbers[2]);
                        continue;
                    }
                    if (!numbers[0].equals("false")) {
                        termTxt = numbers[1] + " " + numbers[0];
                        parsed.append(" ").append(termTxt);
                        continue;
                    } else {
                        termTxt = termTxt.replace(".", "");
                        parsed.append(" ").append(termTxt);
                        continue;
                    }

                }

                //if its the type BETWEEN NUMBER AND NUMBER
                if (i + 3 < textLength && (splitText[i].equals("Between") || splitText[i].equals("between")) && isNum(splitText[i + 1]) && splitText[i + 2].equals("and") && isNum(splitText[i + 3])) {
                    if (splitText[i + 1].charAt(0) == '.') {
                        splitText[i + 1] = 0 + splitText[i + 1];
                    }
                    if (splitText[i + 3].charAt(0) == '.') {
                        splitText[i + 3] = 0 + splitText[i + 3];
                    }
                    parsed.append(" ").append(splitText[i]).append(" ").append(splitText[i + 1]).append(" ").append(splitText[i + 2]).append(" ").append(splitText[i + 3]);
                    parsed.append(" ").append(splitText[i + 1]);
                    parsed.append(" ").append(splitText[i + 3]);
                    i += 3;
                }
                //it is a REGULAR WORD - the dictionary will save it correctly
                else {
                    termTxt = splitText[i].replace(".", "");
                    termTxt = termTxt.replace("/", "");
                    if (termTxt.length() > 1 && !termTxt.equals("--") && !termTxt.equals("---") && !termTxt.equals("and") && !termTxt.equals("And") && !termTxt.equals("AND")) {
                        parsed.append(" ").append(termTxt);
                    }
                }
            }
        }
        return parsed.toString();

    }

    private String getAmount(String[] splitText, int i, String termTxt) {
        if (splitText[i + 1].equals("million") || splitText[i + 1].equals("Million") || splitText[i + 1].equals("M") || splitText[i + 1].equals("m")) {
            termTxt = splitText[i] + "M Dollars";
        }
        if (splitText[i + 1].equals("billion") || splitText[i + 1].equals("Billion") || splitText[i + 1].equals("bn")) {
            double num = Double.parseDouble(splitText[i]);
            num = num * 1000;
            termTxt = num + "M Dollars";
        }
        if (splitText[i + 1].equals("trillion") || splitText[i + 1].equals("Trillion") || splitText[i + 1].equals("tn")) {
            double num = Double.parseDouble(splitText[i]);
            num = num * 1000000;
            termTxt = num + "M Dollars";
        }
        return termTxt;
    }

    /**
     * Method that gets a text and the DOCNO of the text and parses the text.
     * it deletes all the stop words, and the unnecessary punctuations.
     * It go throw every word , and makes the necessary changes to create a term, then, it sends the word to the indexer
     *
     * @param text
     * @param docNo
     */

    public void parse(String text, String docNo) {
        int entityIndexer = 0;
        Document document = new Document(docNo);
        String[] splitText = deleteStopWords(text);
        int textLength = splitText.length;
        for (int i = 0; i < textLength; i++) {
            String termTxt = splitText[i];
            if (termTxt.length() > 1 && termTxt.charAt(0) == '.' && Character.isDigit(termTxt.charAt(1))) {
                termTxt = 0 + termTxt;
                splitText[i] = 0 + termTxt;
            }
            if (termTxt.length() == 0)
                continue;
            ////if the word is a PURE NUMBER
            if (isNum(splitText[i])) {
                ////if the word is Kg - (skip 1 word ahead + check if there is i+1 word)
                if (i + 1 < textLength && (splitText[i + 1].equals("kg") || splitText[i + 1].equals("Kg") || splitText[i + 1].equals("KG") || splitText[i + 1].equals("kilogram")
                        || splitText[i + 1].equals("Kilogram") || splitText[i + 1].equals("kilograms") || splitText[i + 1].equals("Kilograms")
                        || splitText[i + 1].equals("Kgs") || splitText[i + 1].equals("kgs"))) {
                    termTxt = splitText[i] + " kg";
                    addTermToIndx(termTxt, docNo, i);
                    addTermToDoc(document, termTxt);
                    i++;
                    continue;
                }
                if (i + 1 < textLength && (splitText[i + 1].equals("gr") || splitText[i + 1].equals("gram") || splitText[i + 1].equals("Gram") || splitText[i + 1].equals("GRAM")
                        || splitText[i + 1].equals("grams") || splitText[i + 1].equals("Grams") || splitText[i + 1].equals("GRAMS"))) {
                    termTxt = splitText[i] + " gr";
                    addTermToDoc(document, termTxt);
                    addTermToIndx(termTxt, docNo, i);
                    i++;
                    continue;
                }
                if (i + 1 < textLength && (splitText[i + 1].equals("ton") || splitText[i + 1].equals("Ton") || splitText[i + 1].equals("TON") || splitText[i + 1].equals("tons")
                        || splitText[i + 1].equals("Tons") || splitText[i + 1].equals("TONS"))) {
                    termTxt = splitText[i] + "K kg";
                    addTermToDoc(document, termTxt);
                    addTermToIndx(termTxt, docNo, i);
                    i++;
                    continue;
                }
                ////if the word is Km - (skip 1 word ahead + check if there is i+1 word)
                if (i + 1 < textLength && (splitText[i + 1].equals("km") || splitText[i + 1].equals("Km") || splitText[i + 1].equals("KM") || splitText[i + 1].equals("kilometer")
                        || splitText[i + 1].equals("Kilometer") || splitText[i + 1].equals("kilometers") || splitText[i + 1].equals("Kilometers"))) {
                    termTxt = splitText[i] + "K meters";
                    addTermToDoc(document, termTxt);
                    addTermToIndx(termTxt, docNo, i);
                    i++;
                    continue;
                }

                ////if the word is meter - (skip 1 word ahead + check if there is i+1 word)
                if (i + 1 < textLength && (splitText[i + 1].equals("meter") || splitText[i + 1].equals("Meter") || splitText[i + 1].equals("Meters") || splitText[i + 1].equals("meters"))) {
                    termTxt = splitText[i] + " meters";
                    addTermToDoc(document, termTxt);
                    addTermToIndx(termTxt, docNo, i);
                    i++;
                    continue;
                }

                if (i + 1 < textLength && (splitText[i + 1].equals("Centimeter") || splitText[i + 1].equals("centimeter") || splitText[i + 1].equals("Centimeters")
                        || splitText[i + 1].equals("centimeters") || splitText[i + 1].equals("cm"))) {
                    termTxt = splitText[i] + " cm";
                    addTermToDoc(document, termTxt);
                    addTermToIndx(termTxt, docNo, i);
                    i++;
                    continue;
                }
                ////if the number is PERCENT  - (skip 1 word ahead + check if there is i+1 word)
                if (i + 1 < textLength && (splitText[i + 1].equals("percent") || splitText[i + 1].equals("percentage") || splitText[i + 1].equals("%"))) {
                    termTxt = splitText[i] + "%";
                    addTermToDoc(document, termTxt);
                    addTermToIndx(termTxt, docNo, i);
                    i++;
                    continue;
                }

                ////if the words of type PRICE M\B\T U.S DOLLARS - (skip 3 word ahead + check if there is i+3 word)
                if (i + 3 < textLength && (splitText[i + 2].equals("U.S") && (splitText[i + 3].equals("dollars") || splitText[i + 3].equals("Dollars")))) {
                    termTxt = getAmount(splitText, i, termTxt);
                    addTermToDoc(document, termTxt);
                    addTermToIndx(termTxt, docNo, i);
                    i += 3;
                    continue;
                }

                // if the word is of type price m/bn Dollars
                if (i + 2 < textLength && (splitText[i + 2].equals("dollars") || splitText[i + 2].equals("Dollars"))) {
                    if (splitText[i + 1].equals("m") || splitText[i + 1].equals("M"))
                        termTxt = splitText[i] + "M Dollars";
                    if (splitText[i + 1].equals("bn") || splitText[i + 1].equals("BN")) {
                        double newVal = Double.parseDouble(splitText[i]);
                        newVal = newVal * 1000;
                        termTxt = newVal + "M Dollars";
                    }
                    addTermToDoc(document, termTxt);
                    addTermToIndx(termTxt, docNo, i);
                    i += 2;
                    continue;
                }

                // if the word is of type PRICE DOLLARS
                if (i + 1 < textLength && (splitText[i + 1].equals("Dollars") || splitText[i + 1].equals("dollars"))) {
                    String price = termNum(Double.parseDouble(splitText[i]));
                    if (price.charAt(price.length() - 1) == 'B') {
                        double newVal = Double.parseDouble(splitText[i]);
                        newVal = newVal / 1000;
                        termTxt = newVal + "M Dollars";
                    } else {
                        termTxt = price + " Dollars";
                    }
                    addTermToDoc(document, termTxt);
                    addTermToIndx(termTxt, docNo, i);
                    i++;
                    continue;
                }

                //if there is a FRACTION after the number
                if (i + 1 < textLength && isFraction(splitText[i + 1])) {
                    if (i + 2 < textLength && (splitText[i + 2].equals("dollars") || splitText[i + 2].equals("Dollars"))) {
                        termTxt = splitText[i] + " " + splitText[i + 1] + " Dollars";
                        i += 2;
                    } else {
                        termTxt = splitText[i] + " " + splitText[i + 1];
                        i++;
                    }
                    addTermToDoc(document, termTxt);
                    addTermToIndx(termTxt, docNo, i);
                    continue;
                }

                //if the number is part of a date
                if (i + 1 < textLength && isMonth(splitText[i + 1])) {
                    int month = (int) Double.parseDouble(splitText[i]);
                    if (month < 10)
                        termTxt = monthNum(splitText[i + 1]) + "-0" + splitText[i];
                    else
                        termTxt = monthNum(splitText[i + 1]) + "-" + splitText[i];
                    addTermToDoc(document, termTxt);
                    addTermToIndx(termTxt, docNo, i);
                    continue;
                }
                String termNum = termNum(Double.parseDouble(splitText[i]));
                addTermToDoc(document, termNum);
                addTermToIndx(termNum, docNo, i);
            }
            //its a word or its a number with something attached to this
            else {
                //DOR'S POTENTIALS
                if (Character.isUpperCase(splitText[i].charAt(0)) && i >= entityIndexer) {
                    int j = 1;
                    String entity = splitText[i];
                    while ((i + j < textLength) && Character.isUpperCase(splitText[i + j].charAt(0)) && j <= 3) {
                        entity += " " + splitText[i + j];
                        if (indexer.addEntToDic(entity, docNo, i)) {
                            entity = splitText[i].replace(".", "");
                            entity = entity.replace("/", "");
                            addTermToDoc(document, entity);
                        }
                        j++;
                    }
                    entityIndexer += i + j;
                }

                if (splitText[i].charAt(0) == '$') { //if a $ is attached in the beginning
                    termTxt = getDollars(splitText, textLength, i, termTxt);
                    addTermToDoc(document, termTxt);
                    addTermToIndx(termTxt, docNo, i);
                    i++;
                    continue;
                }

                //if a % is attached in the beginning
                if (splitText[i].charAt(splitText[i].length() - 1) == '%') {
                    termTxt = splitText[i];
                    addTermToDoc(document, termTxt);
                    addTermToIndx(termTxt, docNo, i);
                    continue;
                }

                //the term is a month
                if (i + 1 < textLength && isMonth(splitText[i]) && isNum(splitText[i + 1])) {
                    int datePart = (int) Double.parseDouble(splitText[i + 1]);
                    //checking if the number indicates a day
                    if (datePart <= 31) {
                        if (datePart < 10)
                            termTxt = monthNum(splitText[i]) + "-0" + splitText[i + 1];
                        else
                            termTxt = monthNum(splitText[i]) + "-" + splitText[i + 1];
                    }
                    //else, then the number indicates years
                    else
                        termTxt = splitText[i + 1] + "-" + monthNum(splitText[i]);
                    addTermToDoc(document, termTxt);
                    addTermToIndx(termTxt, docNo, i);
                    i++;
                    continue;
                }

                //if its the type WORD - WORD or WORD - WORD - WORD
                if (splitText[i].indexOf("-") >= 0 && !termTxt.equals("--") && !termTxt.contains("---")) {
                    String[] numbers = splitToNumbers(splitText[i]);
                    if (numbers[0].equals("true")) {
                        addTermToDoc(document, numbers[1]);
                        addTermToIndx(numbers[1], docNo, i);
                        addTermToDoc(document, numbers[2]);
                        addTermToIndx(numbers[2], docNo, i);
                        continue;
                    }
                    if (!numbers[0].equals("false")) {
                        termTxt = numbers[1] + " " + numbers[0];
                        addTermToDoc(document, termTxt);
                        addTermToIndx(termTxt, docNo, i);
                        continue;
                    } else {
                        termTxt = termTxt.replace(".", "");
                        addTermToDoc(document, termTxt);
                        addTermToIndx(splitText[i], docNo, i);
                        continue;
                    }

                }

                //if its the type BETWEEN NUMBER AND NUMBER
                if (i + 3 < textLength && (splitText[i].equals("Between") || splitText[i].equals("between")) && isNum(splitText[i + 1]) && splitText[i + 2].equals("and") && isNum(splitText[i + 3])) {
                    if (splitText[i + 1].charAt(0) == '.') {
                        splitText[i + 1] = 0 + splitText[i + 1];
                    }
                    if (splitText[i + 3].charAt(0) == '.') {
                        splitText[i + 3] = 0 + splitText[i + 3];
                    }
                    addTermToDoc(document, splitText[i] + " " + splitText[i + 1] + " " + splitText[i + 2] + " " + splitText[i + 3]);
                    addTermToIndx(splitText[i] + " " + splitText[i + 1] + " " + splitText[i + 2] + " " + splitText[i + 3], docNo, i);
                    addTermToDoc(document, splitText[i + 1]);
                    addTermToIndx(splitText[i + 1], docNo, i);
                    addTermToDoc(document, splitText[i + 3]);
                    addTermToIndx(splitText[i + 3], docNo, i);
                    i += 3;
                }
                //it is a REGULAR WORD - the dictionary will save it correctly
                else {
                    termTxt = splitText[i].replace(".", "");
                    termTxt = termTxt.replace("/", "");
                    if (termTxt.length() > 1 && !termTxt.equals("--") && !termTxt.contains("---") && !termTxt.equals("and") && !termTxt.equals("And") && !termTxt.equals("AND")) {
                        addTermToDoc(document, termTxt);
                        addTermToIndx(termTxt, docNo, i);
                    }
                }
            }
        }
        document.closeDoc();
        indexer.addDocToDic(document);
    }

    private String getDollars(String[] splitText, int textLength, int i, String termTxt) {
        String value = splitText[i].substring(1);
        if (value.length() > 1 && value.charAt(0) == '.' && Character.isDigit(value.charAt(1))) {
            value = 0 + value;
        }
        if (isNum(value)) {
            if (i + 1 < textLength && (splitText[i + 1].equals("million") || splitText[i + 1].equals("Million") || splitText[i + 1].equals("Million.")))
                termTxt = value + "M Dollars";
            else if (i + 1 < textLength && (splitText[i + 1].equals("billion") || splitText[i + 1].equals("Billion"))) {
                double newVal = Double.parseDouble(value);
                newVal = newVal * 1000;
                termTxt = newVal + "M Dollars";
            } else {
                double checkVal = Double.parseDouble(value);
                if (checkVal < 1000000)
                    termTxt = value + " Dollars";
                else {
                    checkVal = checkVal / 1000000;
                    termTxt = checkVal + "M Dollars";
                }
            }
        }
        return termTxt;
    }

    /**
     * Method sends a request to the indexer to reset the system
     */
    public void closeParser() {
        indexer.closeIndexer();
    }

    /**
     * Method that sends a request to the indexer to uploads the correct dictionary from the parameters
     *
     * @param stem - Stemming/noStemming dictionary
     * @param path - the path to the posting files
     * @return - the dictionary of the terms.
     */
    public LinkedHashMap<String, String> upload(boolean stem, String path) {
        return indexer.uploadDictionary(stem, path);
    }

    /**
     * set the path to write the posting by sending the value to the Indexer.
     *
     * @param path - where the posting files will be saved
     */
    public void setIndexerPath(String path) {
        indexer.setPath(path);
    }

    /**
     * gets the number of the documents in the corpus
     *
     * @return number of the documents in the corpus
     */
    public int getNumOfDocs() {
        return indexer.getNumOfDocs();
    }

    /**
     * gets the number of the terms in the corpus
     *
     * @return number of the terms in the corpus
     */
    public int getNumOfTerm() {
        return indexer.getNumOfTerm();
    }

    /**
     * sends a command to erase all the necessary data to the Indexer.
     */
    public void reset() {
        indexer.reset();
    }

    /**
     * method gets a string with '-' char, then splits it to words and checks what their kind.
     * it will put in the first cell
     * true - if they are numbers
     * gr/kg - if the words are Weight
     * cm/meter - if the words are Distance
     * false - all the other options.
     * then it puts the words in the next cells
     *
     * @param str - String from the format word-word or word-word-word
     * @return an array of splited strings
     */
    private String[] splitToNumbers(String str) {
        String[] splited = new String[3];
        int place = str.indexOf('-');
        String first = str.substring(0, place);
        String sec = str.substring(place + 1);
        if (isNum(first) && isNum(sec)) {
            splited[0] = "true";
            if (first.charAt(0) == '.')
                first = 0 + first;
            if (sec.charAt(0) == '.')
                sec = 0 + sec;
            splited[1] = first;
            splited[2] = sec;
        } else if (isNum(first) && (sec.equals("grams") || sec.equals("gram") || sec.equals("Grams") || sec.equals("Gram") || sec.equals("gr") || sec.equals("Gr"))) {
            splited[0] = "gr";
            splited[1] = first;
        } else if (isNum(first) && (sec.equals("kilograms") || sec.equals("kilogram") || sec.equals("Kilograms") || sec.equals("Kilogram") || sec.equals("kg") || sec.equals("Kg") || sec.equals("KG"))) {
            splited[0] = "kg";
            splited[1] = first;
        } else if (isNum(first) && (sec.equals("ton") || sec.equals("Ton") || sec.equals("TON") || sec.equals("tons") || sec.equals("Tons") || sec.equals("TONS"))) {
            splited[0] = "kg";
            splited[1] = first + "K";
        } else if (isNum(first) && (sec.equals("centimeters") || sec.equals("Centimeters") || sec.equals("centimeter") || sec.equals("Centimeter") || sec.equals("Cm") || sec.equals("cm") || sec.equals("CM"))) {
            splited[0] = "cm";
            splited[1] = first;
        } else if (isNum(first) && (sec.equals("kilometers") || sec.equals("kilometer") || sec.equals("Kilometer") || sec.equals("Kilometers") || sec.equals("km") || sec.equals("Km"))) {
            splited[0] = "meters";
            splited[1] = first + "K";
        } else if (isNum(first) && (sec.equals("meters") || sec.equals("meter"))) {
            splited[0] = "meters";
            splited[1] = first;
        } else splited[0] = "false";
        return splited;
    }

    /**
     * Method gets a string and checks if its number/number - a.k.a a fraction.
     *
     * @param txt - a word
     * @return - true if the word is a fraction, false otherwise
     */
    private boolean isFraction(String txt) {
        int place = txt.indexOf('/');
        if (place >= 0) {
            String first = txt.substring(0, place);
            String sec = txt.substring(place + 1);
            return isNum(sec) && isNum(first);
        }
        return false;
    }

    /**
     * Method gets a string and checks if its a number
     *
     * @param str - a word
     * @return - true if the word is a number, false otherwise
     */
    private boolean isNum(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Method gets a string and checks if its a month, also, it initialize the months list.
     *
     * @param str - a word
     * @return - true if the word is a month, false otherwise
     */
    private boolean isMonth(String str) {
        if (months.isEmpty()) {
            synchronized (this) {
                String[] months = {"January", "JANUARY", "Jan", "JAN", "january",
                        "February", "FEBRUARY", "Feb", "FEB", "february",
                        "March", "MARCH", "Mar", "MAR", "march",
                        "April", "APRIL", "Apr", "april", "APR",
                        "May", "MAY", "may",
                        "June", "JUNE", "Jun", "JUN", "june",
                        "July", "JULY", "Jul", "JUL", "july",
                        "August", "AUGUST", "Aug", "august", "AUG",
                        "September", "SEPTEMBER", "Sep", "SEP", "september",
                        "October", "OCTOBER", "Oct", "OCT", "october",
                        "November", "NOVEMBER", "Nov", "NOV", "november",
                        "December", "DECEMBER", "Dec", "DEC", "december"};
                for (String month : months) {
                    this.months.put(month, month.substring(0, 3).toLowerCase());
                }
            }
        }
        return months.containsKey(str);
    }

    /**
     * Method gets a month and return a string that describes this month.
     *
     * @param str - a word that is a month (from the month list)
     * @return - A string that is a number of this month.
     */
    private String monthNum(String str) {
        if (months.get(str).equals("jan"))
            return "01";
        if (months.get(str).equals("feb"))
            return "02";
        if (months.get(str).equals("mar"))
            return "03";
        if (months.get(str).equals("apr"))
            return "04";
        if (months.get(str).equals("may"))
            return "05";
        if (months.get(str).equals("jun"))
            return "06";
        if (months.get(str).equals("jul"))
            return "07";
        if (months.get(str).equals("aug"))
            return "08";
        if (months.get(str).equals("sep"))
            return "09";
        if (months.get(str).equals("oct"))
            return "10";
        if (months.get(str).equals("nov"))
            return "11";
        return "12";
    }

    /**
     * this function takes a number and turn it to a term type of writing to a number
     * for example: 134785 = 134.785K
     *
     * @param num
     * @return a string with the correct typo for the number.
     */
    private String termNum(double num) {
        String termNum;
        if (num < 1000) {
            num = Math.floor(num * 1000) / 1000;
            termNum = "" + num;
        } else if (num >= 1000 && num < 1000000) {
            num = num / 1000;
            num = Math.floor(num * 1000) / 1000;
            termNum = "" + num + "K";
        } else if (num >= 1000000 && num < 1000000000) {
            num = num / 1000000;
            num = Math.floor(num * 1000) / 1000;
            termNum = "" + num + "M";
        } else {
            num = num / 1000000000;
            num = Math.floor(num * 1000) / 1000;
            termNum = "" + num + "B";
        }
        return termNum;
    }

    /**
     * This function deletes all the stopwords and the punctuations from the text before parsing
     *
     * @param text - the whole text
     * @return - an array of final words of the text
     */
    private String[] deleteStopWords(String text) {
        text = text.replaceAll("[,:(){}<>`~*?|&@#=+;!'\"]", "").replaceAll("[\\[\\]]", "");
        String[] splitTxt = text.split("\\s+");
        List<String> l = new ArrayList<>();
        for (int i = 0; i < splitTxt.length; i++) {
            if (!stopWords.contains(splitTxt[i])) {
                l.add(splitTxt[i]);
            }
        }
        String[] newText = new String[l.size()];
        for (int i = 0; i < newText.length; i++) {
            newText[i] = l.get(i);
        }
        return newText;
    }

    /**
     * Method that sends a term to the indexer by its name, DOCNO, and position
     *
     * @param name     - the word itself
     * @param docId    - the Doc id
     * @param position - the word # in the doc.
     */
    private void addTermToIndx(String name, String docId, int position) {
        indexer.addTermToDic(name, docId, position);
    }

    /**
     * Method that sends a term to the Document by its name and DOCNO
     * @param doc - the Doc Id
     * @param termName - the word itself
     */
    private void addTermToDoc(Document doc, String termName){doc.addTerm(termName);}

    /**
     *  Method set the stem value by sending it to the indexer
     * @param isStem
     */
    public void setIndexerStem(boolean isStem){
        indexer.setStem(isStem);
    }

    public void write(){
        this.indexer.write();
    }

//    public HashMap<String,String> getTermDic(boolean stem,String path){
//       return indexer.getTermDic(stem,path);
//    }
    public HashMap<String,String> getTermDicWithoutUpload(){
        return indexer.getTermDicWithoutUpload();
    }


    private LinkedHashMap<String, Integer> getTopFive(HashMap<String, Integer> allEntities) {
        // Create a list from elements of HashMap
        List<HashMap.Entry<String, Integer> > list = new LinkedList<>(allEntities.entrySet());

        // Sort the list
        list.sort((o1, o2) -> -(o1.getValue()).compareTo(o2.getValue()));

        // put data from sorted list to hashmap (the first 50)
        LinkedHashMap<String, Integer> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> doc : list) {
            if(temp.size()<6) {
                temp.put(doc.getKey(), doc.getValue());
            }
        }

        return temp;
    }
}