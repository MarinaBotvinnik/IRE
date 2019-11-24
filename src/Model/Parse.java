package Model;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Parse {

    private ArrayList<String> stopWords;

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
    }

    public void parse(Dictionary dictionary,String text) {
        String[] splitText = deleteStopWords(text);

        for(int i=0; i< splitText.length; i++){
            String termTxt="";
            ////if the word is a PURE NUMBER
            if(isNum(splitText[i])){
                ////if the number is PERCENT
                if(splitText[i+1].equals("percent")|| splitText[i+1].equals("percentage") || splitText[i+1].equals("%")){
                    termTxt = splitText[i]+"%";
                    dictionary.addTerm(termTxt,""); //CHANGE THE VALUE
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
                    dictionary.addTerm(termTxt,"");
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
                    dictionary.addTerm(termTxt,"");
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
                    dictionary.addTerm(termTxt,"");
                    continue;
                }
                //if there is a FRACTION after the number
                if(isFraction(splitText[i+1])){
                    if(splitText[i+2].equals("dollars") || splitText[i+2].equals("Dollars")) {
                        termTxt = splitText[i] + " " + splitText[i + 1] + " Dollars";
                    }
                    else
                        termTxt = splitText[i]+ " "+splitText[i+1];
                    dictionary.addTerm(termTxt,"");
                    continue;
                }
                //if the number is part of a date
                if(isMounth(splitText[i+1])){
                    if(Integer.parseInt(splitText[i])<10)
                        termTxt=mounthNum(splitText[i+1])+"-0"+splitText[i];
                    else
                        termTxt=mounthNum(splitText[i+1])+"-"+splitText[i];
                    dictionary.addTerm(termTxt,"");
                    continue;
                }
                //no need to save the number, it was already saved as a date
                if(isMounth(splitText[i-1])){
                    continue;
                }
                String termNum =termNum(Double.parseDouble(splitText[i]));
                dictionary.addTerm(termNum,"");
                continue;
            }
            else{ //its a word or its a number with something attached to this
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
                    dictionary.addTerm(termTxt,"");
                    continue;
                }
                if(splitText[i].charAt(splitText[i].length()-1)=='%'){ //if a % is attached in the beginning
                    termTxt = splitText[i];
                    dictionary.addTerm(termTxt,"");
                    continue;
                }
                //the term is a mounth
                if(isMounth(splitText[i]) && isNum(splitText[i+1])){
                        //checking if the number indicates a day
                    if(Integer.parseInt(splitText[i+1])<=31){
                        if(Integer.parseInt(splitText[i+1])<10)
                            termTxt=mounthNum(splitText[i])+"-0"+splitText[i+1];
                        else
                            termTxt=mounthNum(splitText[i])+"-"+splitText[i+1];
                    }
                    //else, then the number indicates years
                    else
                        termTxt=splitText[i+1]+"-"+mounthNum(splitText[i]);
                    dictionary.addTerm(termTxt,"");
                    continue;
                }
            }
        }

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
            double num = Double.parseDouble(str);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }

    private boolean isMounth(String str){
        String[] mounths={"January", "JANUARY", "Jan", "February", "FEBRUARY", "Feb", "March", "MARCH", "Mar", "April", "APRIL", "Apr", "May", "MAY", "June", "JUNE", "Jun", "July", "JULY", "Jul", "August", "AUGUST", "Aug", "September", "SEPTEMBER", "Sep", "October", "OCTOBER", "Oct", "November", "NOVEMBER", "Nov", "December", "DECEMBER", "Dec"};
        for(String month : mounths){
            if(str.equals(month))
                return true;
        }
        return false;
    }

    private String mounthNum(String str){
        if(str.equals("January") || str.equals("JANUARY") || str.equals("Jan"))
            return "01";
        if(str.equals("February") || str.equals("FEBRUARY") || str.equals("Feb"))
            return "02";
        if(str.equals("March") || str.equals("MARCH") || str.equals("Mar"))
            return "03";
        if(str.equals("April") || str.equals("APRIL") || str.equals("Apr"))
            return "04";
        if(str.equals("May") || str.equals("MAY"))
            return "05";
        if(str.equals("June") || str.equals("JUNE") || str.equals("Jun"))
            return "06";
        if(str.equals("July") || str.equals("JULY") || str.equals("Jul"))
            return "07";
        if(str.equals("August") || str.equals("AUGUST") || str.equals("Aug"))
            return "08";
        if(str.equals("September") || str.equals("SEPTEMBER") || str.equals("Sep"))
            return "09";
        if(str.equals("October") || str.equals("OCTOBER") || str.equals("Oct"))
            return "10";
        if(str.equals("November") || str.equals("NOVEMBER") || str.equals("Nov"))
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
            if(stopWords.contains(splitTxt[i]) || !splitTxt[i].equals("between") || !splitTxt[i].equals("Between")){
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
