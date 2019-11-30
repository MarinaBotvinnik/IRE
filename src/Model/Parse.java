package Model;

import javax.print.Doc;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Parse {

    private ArrayList<String> stopWords;
    private HashMap<String,String> months;
    private HashMap<String,String> potentials;

    public Parse(){
        if (stopWords == null) {
            stopWords = new ArrayList<>();
            try{
                BufferedReader buffer = new BufferedReader(new InputStreamReader(new FileInputStream("resource/stop_words"),"UTF-8"));
                String st;
                //add all the stop-words in all their ways
                while (( st = buffer.readLine()) != null){
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
    }

    public void parse(Dictionary dictionary,String text, String docNo) {
        Document document = new Document(docNo);
        String[] splitText = deleteStopWords(text);
        for(int i=0; i< splitText.length; i++){
            String termTxt="";
            ////if the word is a PURE NUMBER
            if(isNum(splitText[i])){
                ////if the word is Kg
                if(splitText[i+1].equals("kg") || splitText[i+1].equals("Kg")|| splitText[i+1].equals("KG")||splitText[i+1].equals("kilogram")
                        ||splitText[i+1].equals("Kilogram")||splitText[i+1].equals("kilograms") ||splitText[i+1].equals("Kilograms")){
                    termTxt = splitText[i] + "Kg";
                    document.addFrequency(termTxt);
                    document.addPosotion(termTxt,i);
                    dictionary.addTerm(termTxt,docNo);
                    continue;
                }
                ////if the word is Km
                if(splitText[i+1].equals("km") || splitText[i+1].equals("Km")|| splitText[i+1].equals("KM")||splitText[i+1].equals("kilometer")
                        ||splitText[i+1].equals("Kilometer")||splitText[i+1].equals("kilometers") ||splitText[i+1].equals("Kilometers")){
                    termTxt = Double.parseDouble(splitText[i])*10000 + "meters";
                    dictionary.addTerm(termTxt,docNo);
                    continue;
                }
                ////if the word is meter
                if(splitText[i+1].equals("meter") || splitText[i+1].equals("Meter") || splitText[i+1].equals("Meters")){
                    termTxt = splitText[i] + " meters";
                    dictionary.addTerm(termTxt,docNo);
                    continue;
                }
                ////if the number is PERCENT
                if(splitText[i+1].equals("percent")|| splitText[i+1].equals("percentage") || splitText[i+1].equals("%")){
                    termTxt = splitText[i]+"%";
                    document.addFrequency(termTxt);
                    document.addPosotion(termTxt,i);
                    dictionary.addTerm(termTxt,docNo);
                    continue;
                }
                ////if the words of type PRICE M\B\T U.S DOLLARS
                if(splitText[i+2].equals("U.S") && (splitText[i+3].equals("dollars") || splitText[i+3].equals("Dollars")))
                {
                    if(splitText[i + 1].equals("million") || splitText[i + 1].equals("Million") || splitText[i + 1].equals("M")||splitText[i + 1].equals("m") ) {
                        termTxt = splitText[i] + "M Dollars";
                    }
                    if(splitText[i + 1].equals("billion") ||  splitText[i + 1].equals("Billion") || splitText[i + 1].equals("bn")){
                        double num = Double.parseDouble(splitText[i]);
                        num = num*1000;
                        termTxt = num + "M Dollars";
                    }
                    if(splitText[i + 1].equals("trillion") ||  splitText[i + 1].equals("Tillion") || splitText[i + 1].equals("tn")){
                        double num = Double.parseDouble(splitText[i]);
                        num = num*1000000;
                        termTxt = num + "M Dollars";
                    }
                    document.addFrequency(termTxt);
                    document.addPosotion(termTxt,i);
                    dictionary.addTerm(termTxt,docNo);
                    continue;
                }
                // if the word is of type price m/bn Dollars
                if(splitText[i+2].equals("dollars") || splitText[i+2].equals("Dollars")){
                    if(splitText[i+1].equals("m") || splitText.equals("M"))
                        termTxt = splitText[i] + "M Dollars";
                    if( splitText[i+1].equals("bn") || splitText[i+1].equals("BN")){
                        double newVal = Double.parseDouble(splitText[i]);
                        newVal = newVal*1000;
                        termTxt = newVal + "M Dollars";
                    }
                    document.addFrequency(termTxt);
                    document.addPosotion(termTxt,i);
                    dictionary.addTerm(termTxt,docNo);
                    continue;
                }
                // if the word is of type PRICE DOLLARS
                if(splitText[i+1].equals("Dollars") || splitText[i+1].equals("dollars")){
                    String price = termNum(Double.parseDouble(splitText[i]));
                    if(price.charAt(price.length()-1) == 'B'){
                        double newVal = Double.parseDouble(splitText[i]);
                        newVal = newVal/1000;
                        termTxt = newVal + "M Dollars";
                    }
                    else{
                        termTxt = price + " Dollars";
                    }
                    document.addFrequency(termTxt);
                    document.addPosotion(termTxt,i);
                    dictionary.addTerm(termTxt,docNo);
                    continue;
                }
                //if there is a FRACTION after the number
                if(isFraction(splitText[i+1])){
                    if(splitText[i+2].equals("dollars") || splitText[i+2].equals("Dollars")) {
                        termTxt = splitText[i] + " " + splitText[i + 1] + " Dollars";
                    }
                    else
                        termTxt = splitText[i]+ " "+splitText[i+1];
                    document.addFrequency(termTxt);
                    document.addPosotion(termTxt,i);
                    dictionary.addTerm(termTxt,docNo);
                    continue;
                }
                //if the number is part of a date
                if(isMonth(splitText[i+1])){
                    if(Integer.parseInt(splitText[i])<10)
                        termTxt= monthNum(splitText[i+1])+"-0"+splitText[i];
                    else
                        termTxt= monthNum(splitText[i+1])+"-"+splitText[i];
                    document.addFrequency(termTxt);
                    document.addPosotion(termTxt,i);
                    dictionary.addTerm(termTxt,docNo);
                    continue;
                }
                //no need to save the number, it was already saved as a date
                if(isMonth(splitText[i-1])){
                    continue;
                }
                String termNum =termNum(Double.parseDouble(splitText[i]));
                document.addFrequency(termTxt);
                document.addPosotion(termTxt,i);
                dictionary.addTerm(termNum,docNo);
                continue;
            }
            else{ //its a word or its a number with something attached to this
                if(Character.isUpperCase(splitText[i].charAt(0))){
                    int j = 1;
                    String entity=splitText[i];
                    while (Character.isUpperCase(splitText[i + j].charAt(0))) {
                        entity=" "+splitText[i+j];
                        j++;
                    }
                    if (potentials.containsKey(entity) && !docNo.equals(potentials.get(entity)) && j>1){
                        document.addFrequency(termTxt);
                        document.addPosotion(termTxt,i);
                        dictionary.addTerm(entity,docNo);
                        dictionary.addTerm(entity, potentials.get(entity));
                    }
                    else{
                        potentials.put(entity,docNo);
                    }
                }
                if(splitText[i].charAt(0)=='$'){ //if a $ is attached in the beginning
                    String value = splitText[i].substring(1);
                    if(splitText[i+1].equals("million") || splitText[i+1].equals("Million"))
                        termTxt = value + "M Dollars";
                    else if(splitText[i+1].equals("billion") || splitText[i+1].equals("Billion")){
                        double newVal = Double.parseDouble(splitText[i].substring(1));
                        newVal = newVal*1000;
                        termTxt = newVal + "M Dollars";
                    }
                    else{
                        double checkVal = Double.parseDouble(splitText[i]);
                        if(checkVal < 1000000)
                            termTxt = value + " Dollars";
                        else{
                            checkVal = checkVal/1000000;
                            termTxt = checkVal + "M Dollars";
                        }
                    }
                    document.addFrequency(termTxt);
                    document.addPosotion(termTxt,i);
                    dictionary.addTerm(termTxt,docNo);
                    continue;
                }
                if(splitText[i].charAt(splitText[i].length()-1)=='%'){ //if a % is attached in the beginning
                    termTxt = splitText[i];
                    document.addFrequency(termTxt);
                    document.addPosotion(termTxt,i);
                    dictionary.addTerm(termTxt,docNo);
                    continue;
                }
                //the term is a month
                if(isMonth(splitText[i]) && isNum(splitText[i+1])){
                        //checking if the number indicates a day
                    if(Integer.parseInt(splitText[i+1])<=31){
                        if(Integer.parseInt(splitText[i+1])<10)
                            termTxt= monthNum(splitText[i])+"-0"+splitText[i+1];
                        else
                            termTxt= monthNum(splitText[i])+"-"+splitText[i+1];
                    }
                    //else, then the number indicates years
                    else
                        termTxt=splitText[i+1]+"-"+ monthNum(splitText[i]);
                    document.addFrequency(termTxt);
                    document.addPosotion(termTxt,i);
                    dictionary.addTerm(termTxt,docNo);
                    continue;
                }
                //if its the type WORD - WORD or WORD - WORD - WORD
                if(splitText[i].indexOf("-")>=0){
                    String [] numbers = splitToNumbers(splitText[i]);
                    if(numbers[0].equals("true")){
                        document.addFrequency(numbers[1]);
                        document.addPosotion(numbers[1],i);
                        document.addFrequency(numbers[2]);
                        document.addPosotion(numbers[2],i);
                        dictionary.addTerm(numbers[1],docNo);
                        dictionary.addTerm(numbers[2],docNo);
                    }
                    if(numbers[0].equals("kg")){
                        termTxt = numbers[1]+ "Kg";
                        dictionary.addTerm(termTxt,docNo);
                    }
                    if(numbers[0].equals("km")){
                        termTxt = numbers[1]+ "Km";
                        dictionary.addTerm(termTxt,docNo);
                    }
                    if(numbers[0].equals("meters")){
                        termTxt = numbers[1]+ " meters";
                        dictionary.addTerm(termTxt,docNo);
                    }
                    if(numbers[0].equals("true")|| numbers[0].equals("false")) {
                        document.addFrequency(splitText[i]);
                        document.addPosotion(splitText[i], i);
                        dictionary.addTerm(splitText[i], docNo);
                    }
                }
                //if ots the type BETWEEN NUMBER AND NUMBER
                if((splitText[i].equals("Between") || splitText[i].equals("between"))&& isNum(splitText[i+1]) && splitText[i+2].equals("and") && isNum(splitText[i+3])){
                    document.addFrequency(splitText[i]+" "+splitText[i+1]+ " "+ splitText[i+2] + " "+ splitText[i+3]);
                    document.addPosotion(splitText[i]+" "+splitText[i+1]+ " "+ splitText[i+2] + " "+ splitText[i+3], i);
                    dictionary.addTerm(splitText[i]+" "+splitText[i+1]+ " "+ splitText[i+2] + " "+ splitText[i+3],docNo);
                    document.addFrequency(splitText[i+1]);
                    document.addPosotion(splitText[i+1], i);
                    dictionary.addTerm(splitText[i+1],docNo);
                    document.addFrequency(splitText[i+3]);
                    document.addPosotion(splitText[i+3], i);
                    dictionary.addTerm(splitText[i+3],docNo);
                }
                //it is a REGULAR WORD - the dictionary will save it correctly
               else{
                   document.addFrequency(splitText[i]);
                   document.addPosotion(splitText[i], i);
                   dictionary.saveCorrectly(splitText[i],docNo);
                }
            }
        }

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
       else if(isNum(first) && (sec.equals("kilograms") || sec.equals("kilogram"))){
           splited[0] = "kg";
           splited[1]= first;
        }
       else if(isNum(first) && (sec.equals("kilometers") || sec.equals("kilometer"))){
           splited[0] = "km";
           splited[1]= first;
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

    private String monthNum(String str){ // add all lower case / and short all upper case
       if(months.get(str).equals("jan")) //maybe this way its quicker
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
        String[] splitTxt = text.split(" ");
        int empty=0;
        for(int i=0; i<splitTxt.length; i++){
            if(stopWords.contains(splitTxt[i]) || !splitTxt[i].equals("between") || !splitTxt[i].equals("Between") ||!splitTxt[i].equals("and") ){
                splitTxt[i]= null;
                empty++;
            }
        }
        String[] newText = new String[splitTxt.length - empty];
        int place=0;
        for(int i=0; i< splitTxt.length; i++){
            if(!splitTxt[i].equals(null)){
                newText[place] = splitTxt[i];
                place++;
            }
        }
        return newText;
    }
}
