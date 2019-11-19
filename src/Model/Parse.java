package Model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class Parse {

    private HashMap<String, Term> terms;
    private ArrayList<String> stopWords;

    public Parse(){
        terms = new HashMap<>();
        if (stopWords == null) {
            stopWords = new ArrayList<>();
            try{
                BufferedReader buffer = new BufferedReader(new FileReader("Resource/stop_words"));
                String st;
                while (( st = buffer.readLine()) != null){
                    stopWords.add(st);
                }
            }catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void parse(String text) {
        String[] splitText = text.split("\\s+");
        for(int i=0; i< splitText.length; i++){
            String term="";
            // if this is a stop word than we dont need to turn it into a term
            if(isStopWord(splitText[i])){
                continue;
            }
            //if this is a word

            //if this is a number and nothing is attached to it
            if(isNum(splitText[i])){
                String termNum =termNum(Double.parseDouble(splitText[i]));
                if(splitText[i+1].equals("percent")|| splitText[i+1].equals("percentage") || splitText[i+1].equals("%")){
                    term = termNum+"%";
                }
            }
        }

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

    private boolean isStopWord(String word){
        String lowerCaseWord = word.toLowerCase();
        if(stopWords.contains(lowerCaseWord) && !lowerCaseWord.equals("between") ){
            return true;
        }
        return false;
    }
}
