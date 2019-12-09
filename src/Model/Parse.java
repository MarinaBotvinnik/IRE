package Model;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Parse {

    private HashSet<String> stopWords;
    private HashMap<String,String> months;
    private HashMap<String,String> potentials;
    private Indexer indexer;

    public Parse(){
        if (stopWords == null) {
            stopWords = new HashSet<>();
            try{
                BufferedReader buffer = new BufferedReader(new InputStreamReader(new FileInputStream("Resource/stop_words.txt"),"UTF-8"));
                String st;
                //add all the stop-words in all their ways
                while (( st = buffer.readLine()) != null){
                    if(!st.equals("between") && !st.equals("and") && !st.equals("may") &&!st.equals("m")&& !st.equals("b")&&!st.equals("t"))
                    stopWords.add(st);
                    stopWords.add(st.toUpperCase());
                    stopWords.add(st.substring(0,1).toUpperCase()+st.substring(1));
                }
            }catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        potentials = new HashMap<>();
        months = new HashMap<>();
        indexer = new Indexer();
    }

    public void parse(Dictionary dictionary,String text, String docNo) {
        Document document = new Document(docNo);
        String[] splitText = deleteStopWords(text);
        int textLength = splitText.length;
        for(int i=0; i< textLength; i++){
            String termTxt=splitText[i];
            if(termTxt.length()==0)
                continue;
            ////if the word is a PURE NUMBER
            if(isNum(splitText[i])){
                ////if the word is Kg - (skip 1 word ahead + check if there is i+1 word)
                if(i+1<textLength &&(splitText[i+1].equals("kg") || splitText[i+1].equals("Kg")|| splitText[i+1].equals("KG")||splitText[i+1].equals("kilogram")
                        ||splitText[i+1].equals("Kilogram")||splitText[i+1].equals("kilograms") ||splitText[i+1].equals("Kilograms")
                        ||splitText[i+1].equals("Kgs") ||splitText[i+1].equals("kgs"))){
                    termTxt = splitText[i] + " kg";
                    addTermToIndx(termTxt,docNo,i,false);
                    i++;
                    continue;
                }
                if(i+1<textLength &&(splitText[i+1].equals("gr") || splitText[i+1].equals("gram")|| splitText[i+1].equals("Gram")||splitText[i+1].equals("GRAM")
                        ||splitText[i+1].equals("grams")||splitText[i+1].equals("Grams") ||splitText[i+1].equals("GRAMS"))){
                    termTxt = splitText[i] + " gr";
                    addTermToIndx(termTxt,docNo,i,false);
                    i++;
                    continue;
                }
                if(i+1<textLength &&(splitText[i+1].equals("ton") || splitText[i+1].equals("Ton")|| splitText[i+1].equals("TON")||splitText[i+1].equals("tons")
                        ||splitText[i+1].equals("Tons")||splitText[i+1].equals("TONS"))){
                    termTxt = splitText[i] + "K kg";
                    addTermToIndx(termTxt,docNo,i,false);
                    i++;
                    continue;
                }
                ////if the word is Km - (skip 1 word ahead + check if there is i+1 word)
                if(i+1<textLength && (splitText[i+1].equals("km") || splitText[i+1].equals("Km")|| splitText[i+1].equals("KM")||splitText[i+1].equals("kilometer")
                        ||splitText[i+1].equals("Kilometer")||splitText[i+1].equals("kilometers") ||splitText[i+1].equals("Kilometers"))){
                    termTxt = splitText[i] + "K meters";
                    addTermToIndx(termTxt,docNo,i,false);
                    i++;
                    continue;
                }

                ////if the word is meter - (skip 1 word ahead + check if there is i+1 word)
                if( i+1<textLength && (splitText[i+1].equals("meter") || splitText[i+1].equals("Meter") || splitText[i+1].equals("Meters") || splitText[i+1].equals("meters"))){
                    termTxt = splitText[i] + " meters";
                    addTermToIndx(termTxt,docNo,i,false);
                    i++;
                    continue;
                }

                if( i+1<textLength && (splitText[i+1].equals("Centimeter") || splitText[i+1].equals("centimeter") || splitText[i+1].equals("Centimeters")
                        || splitText[i+1].equals("centimeters") || splitText[i+1].equals("cm"))){
                    termTxt = splitText[i] + " cm";
                    addTermToIndx(termTxt,docNo,i,false);
                    i++;
                    continue;
                }
                ////if the number is PERCENT  - (skip 1 word ahead + check if there is i+1 word)
                if(i+1<textLength && (splitText[i+1].equals("percent")|| splitText[i+1].equals("percentage") || splitText[i+1].equals("%"))){
                    termTxt = splitText[i]+"%";
                    addTermToIndx(termTxt,docNo,i,false);
                    i++;
                    continue;
                }

                ////if the words of type PRICE M\B\T U.S DOLLARS - (skip 3 word ahead + check if there is i+3 word)
                if( i+3<textLength && (splitText[i+2].equals("U.S") && (splitText[i+3].equals("dollars") || splitText[i+3].equals("Dollars"))))
                {
                    if(splitText[i + 1].equals("million") || splitText[i + 1].equals("Million") || splitText[i + 1].equals("M")||splitText[i + 1].equals("m") ) {
                        termTxt = splitText[i] + "M Dollars";
                    }
                    if(splitText[i + 1].equals("billion") ||  splitText[i + 1].equals("Billion") || splitText[i + 1].equals("bn")){
                        double num = Double.parseDouble(splitText[i]);
                        num = num*1000;
                        termTxt = num + "M Dollars";
                    }
                    if(splitText[i + 1].equals("trillion") ||  splitText[i + 1].equals("Trillion") || splitText[i + 1].equals("tn")){
                        double num = Double.parseDouble(splitText[i]);
                        num = num*1000000;
                        termTxt = num + "M Dollars";
                    }
                    addTermToIndx(termTxt,docNo,i,false);
                    i+=3;
                    continue;
                }

                // if the word is of type price m/bn Dollars
                if(i+2<textLength && (splitText[i+2].equals("dollars") || splitText[i+2].equals("Dollars"))){
                    if(splitText[i+1].equals("m") || splitText.equals("M"))
                        termTxt = splitText[i] + "M Dollars";
                    if( splitText[i+1].equals("bn") || splitText[i+1].equals("BN")){
                        double newVal = Double.parseDouble(splitText[i]);
                        newVal = newVal*1000;
                        termTxt = newVal + "M Dollars";
                    }
                    addTermToIndx(termTxt,docNo,i,false);
                    i+=2;
                    continue;
                }

                // if the word is of type PRICE DOLLARS
                if(i+1<textLength && (splitText[i+1].equals("Dollars") || splitText[i+1].equals("dollars"))){
                    String price = termNum(Double.parseDouble(splitText[i]));
                    if(price.charAt(price.length()-1) == 'B'){
                        double newVal = Double.parseDouble(splitText[i]);
                        newVal = newVal/1000;
                        termTxt = newVal + "M Dollars";
                    }
                    else{
                        termTxt = price + " Dollars";
                    }
                    addTermToIndx(termTxt,docNo,i,false);
                    i++;
                    continue;
                }

                //if there is a FRACTION after the number
                if(i+1<textLength && isFraction(splitText[i+1])){
                    if(i+2<textLength && (splitText[i+2].equals("dollars") || splitText[i+2].equals("Dollars"))) {
                        termTxt = splitText[i] + " " + splitText[i + 1] + " Dollars";
                        i+=2;
                    }
                    else {
                        termTxt = splitText[i] + " " + splitText[i + 1];
                        i++;
                    }
                    addTermToIndx(termTxt,docNo,i,false);
                    continue;
                }

                //if the number is part of a date
                if(i+1<textLength && isMonth(splitText[i+1])){
                    int month = (int)Double.parseDouble(splitText[i]);
                    if(month<10)
                        termTxt= monthNum(splitText[i+1])+"-0"+splitText[i];
                    else
                        termTxt= monthNum(splitText[i+1])+"-"+splitText[i];
                    dictionary.addTerm(termTxt,docNo);
                    continue;
                }
                String termNum =termNum(Double.parseDouble(splitText[i]));
                addTermToIndx(termTxt,docNo,i,false);
                continue;
            }
            //its a word or its a number with something attached to this
            else{
                //DOR'S POTENTIALS
                if(Character.isUpperCase(splitText[i].charAt(0))){
                    int j = 1;
                    String entity=splitText[i];
                    while ((i+j<textLength) && Character.isUpperCase(splitText[i + j].charAt(0))) {
                        entity=" "+splitText[i+j];
                        j++;
                    }
                    if (potentials.containsKey(entity) && !docNo.equals(potentials.get(entity)) && j>1){
                        dictionary.addTerm(entity,docNo);
                        dictionary.addTerm(entity, potentials.get(entity));
                    }
                    else{
                        potentials.put(entity,docNo);
                    }
                }

                if(splitText[i].charAt(0)=='$') { //if a $ is attached in the beginning
                    String value = splitText[i].substring(1);
                    if (isNum(value)) {
                        if (i + 1 < textLength && (splitText[i + 1].equals("million") || splitText[i + 1].equals("Million") || splitText[i + 1].equals("Million.")))
                            termTxt = value + "M Dollars";
                        else if (i + 1 < textLength &&(splitText[i + 1].equals("billion") || splitText[i + 1].equals("Billion"))) {
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
                    addTermToIndx(termTxt,docNo,i,false);
                    i++;
                    continue;
                }

                //if a % is attached in the beginning
                if(splitText[i].charAt(splitText[i].length()-1)=='%'){
                    termTxt = splitText[i];
                    addTermToIndx(termTxt,docNo,i,false);
                    continue;
                }

                //the term is a month
                if(i+1<textLength && isMonth(splitText[i]) && isNum(splitText[i+1])){
                    int datePart = (int)Double.parseDouble(splitText[i+1]);
                        //checking if the number indicates a day
                    if(datePart<=31){
                        if(datePart<10)
                            termTxt= monthNum(splitText[i])+"-0"+splitText[i+1];
                        else
                            termTxt= monthNum(splitText[i])+"-"+splitText[i+1];
                    }
                    //else, then the number indicates years
                    else
                        termTxt=splitText[i+1]+"-"+ monthNum(splitText[i]);
                    addTermToIndx(termTxt,docNo,i,false);
                    i++;
                    continue;
                }

                //if its the type WORD - WORD or WORD - WORD - WORD
                if(splitText[i].indexOf("-")>=0){
                    String [] numbers = splitToNumbers(splitText[i]);
                    if(numbers[0].equals("true")) {
                        addTermToIndx(numbers[1],docNo,i,false);
                        addTermToIndx(numbers[2],docNo,i,false);
                    }
                    if(!numbers[0].equals("true")&& !numbers[0].equals("false")) {
                        termTxt = numbers[1] + " " + numbers[0];
                        addTermToIndx(termTxt,docNo,i,false);
                    }
                    else addTermToIndx(splitText[i],docNo,i,false);
                        continue;
                    }

                //if its the type BETWEEN NUMBER AND NUMBER
                if(i+3<textLength && (splitText[i].equals("Between") || splitText[i].equals("between"))&& isNum(splitText[i+1]) && splitText[i+2].equals("and") && isNum(splitText[i+3])){
                    addTermToIndx(splitText[i]+" "+splitText[i+1]+ " "+ splitText[i+2] + " "+ splitText[i+3],docNo,i,false);
                    addTermToIndx(splitText[i+1],docNo,i,false);
                    addTermToIndx(splitText[i+3],docNo,i,false);
                    i+=3;
                    continue;
                }
                //it is a REGULAR WORD - the dictionary will save it correctly
               else{
                   termTxt=splitText[i].replace(".","");
                   addTermToIndx(termTxt,docNo,i,false);
                }
            }
        }
        document.closeDoc();
        indexer.addDocToDic(document);
    }

    private String[] splitToNumbers (String str){
       String [] splited = new String[3];
       int place = str.indexOf('-');
       String first = str.substring(0,place);
       String sec = str.substring(place+1);
       if(isNum(first) && isNum(sec)){
           splited[0] = "true";
           splited[1]= first;
           splited[2] = sec;
       }
       else if(isNum(first) && (sec.equals("grams") || sec.equals("gram") || sec.equals("Grams") || sec.equals("Gram")|| sec.equals("gr")||sec.equals("Gr"))){
           splited[0] = "gr";
           splited[1]= first;
       }
       else if(isNum(first) && (sec.equals("kilograms") || sec.equals("kilogram") || sec.equals("Kilograms") || sec.equals("Kilogram")|| sec.equals("kg")||sec.equals("Kg")||sec.equals("KG"))){
           splited[0] = "kg";
           splited[1]= first;
       }
       else if(isNum(first) && (sec.equals("ton") || sec.equals("Ton") || sec.equals("TON") || sec.equals("tons")|| sec.equals("Tons")||sec.equals("TONS"))){
           splited[0] = "kg";
           splited[1]= first +"K";
       }
       else if(isNum(first) && (sec.equals("centimeters") || sec.equals("Centimeters") || sec.equals("centimeter") || sec.equals("Centimeter")|| sec.equals("Cm")||sec.equals("cm")||sec.equals("CM"))){
           splited[0] = "cm";
           splited[1]= first;
       }
       else if(isNum(first) && (sec.equals("kilometers") || sec.equals("kilometer")|| sec.equals("Kilometer")|| sec.equals("Kilometers")|| sec.equals("km")|| sec.equals("Km"))){
           splited[0] = "meters";
           splited[1]= first + "K";
       }
       else if(isNum(first) && (sec.equals("meters") || sec.equals("meter"))){
           splited[0] = "meters";
           splited[1]= first;
       }
       else splited[0]= "false";
       return splited;
    }

    private boolean isFraction(String txt){
        int place = txt.indexOf('/');
        if(place >=0){
            String first = txt.substring(0,place);
            String sec = txt.substring(place+1);
            if(isNum(sec) && isNum(first))
                return true;
        }
        return false;
    }

    private boolean isNum(String str){
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }

    private boolean isMonth(String str){
        if(months.isEmpty())
        {
            String[] months={"January", "JANUARY", "Jan","JAN","january",
                    "February", "FEBRUARY", "Feb","FEB","february",
                    "March", "MARCH", "Mar","MAR","march",
                    "April", "APRIL", "Apr","april","APR",
                    "May", "MAY","may",
                    "June", "JUNE", "Jun","JUN","june",
                    "July", "JULY", "Jul","JUL","july",
                    "August", "AUGUST", "Aug","august","AUG",
                    "September", "SEPTEMBER", "Sep","SEP","september",
                    "October", "OCTOBER", "Oct","OCT","october",
                    "November", "NOVEMBER", "Nov","NOV","november",
                    "December", "DECEMBER", "Dec","DEC","december"};
            for(String month : months){
               this.months.put(month,month.substring(0,3).toLowerCase());
            }
        }
        if(months.containsKey(str))
            return true;
        return false;
    }

    private String monthNum(String str){
       if(months.get(str).equals("jan"))
           return "01";
        if(months.get(str).equals("feb"))
            return "02";
        if(months.get(str).equals("mar"))
            return "03";
        if(months.get(str).equals("apr"))
            return "04";
        if(months.get(str).equals("may"))
            return "05";
        if(months.get(str).equals("jun"))
            return "06";
        if(months.get(str).equals("jul"))
            return "07";
        if(months.get(str).equals("aug"))
            return "08";
        if(months.get(str).equals("sep"))
            return "09";
        if(months.get(str).equals("oct"))
            return "10";
        if(months.get(str).equals("nov"))
            return "11";
        return "12";
    }

    /**
     * this function takes a number and turn it to a term type of writing to a number
     * for example: 134785 = 134.785K
     * @param num
     * @return a string with the correct typo for the number.
     */
    private String termNum(double num){
        String termNum;
        if(num<1000){
            num = Math.floor(num*1000 )/1000;
            termNum = ""+num;
        }
        else if(num>=1000 && num<1000000){
            num = num/1000;
            num = Math.floor(num*1000 )/1000;
            termNum = ""+num+"K";
        }
        else if( num>=1000000 && num < 1000000000){
            num = num/1000000;
            num = Math.floor(num*1000 )/1000;
            termNum = ""+num+"M";
        }
        else{
            num = num/1000000000;
            num = Math.floor(num*1000 )/1000;
            termNum = ""+num+"B";
        }
        return termNum;
    }

    /**
     * This function deletes all the stopwords from the text before parsing
     * @param text
     * @return
     */
    private String[] deleteStopWords (String text){
        text=text.replaceAll("[,:(){}*'\"]", "").replaceAll("\\[|\\]", "");
        String[] splitTxt = text.split("\\s+");
        List<String> l = new ArrayList<>();
        for(int i=0; i<splitTxt.length; i++){
            if(!stopWords.contains(splitTxt[i])) {
                l.add(splitTxt[i]);
            }
        }
        String[] newText = new String[l.size()];
        for (int i=0; i<newText.length;i++){
            newText[i]=l.get(i);
        }
        return newText;
    }

    private void addTermToIndx(String name,String docId,int position, boolean isEntity){
        Term term = new Term(name,docId,position,isEntity);
        indexer.addTermToDic(term);
    }
}
