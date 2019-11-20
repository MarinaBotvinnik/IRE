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
