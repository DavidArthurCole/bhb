package com;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Blend {

    private Blend(){}

    private static String padWithZeros(String inputString){
        return String.format("%1$" + 2 + "s", inputString).replace(' ', '0');
    }

    public static List<Integer> findSplitLengths(String word, int numSplits){

        int len = word.length();

        //Store the length each substring should be
        List<Integer> solution = new ArrayList<>();

        int roughDivision = (int) Math.ceil( (double) len/numSplits); // the length of each divided word
        int remainingLetters = word.length();
        
        boolean reduced = false; // flag to see if I've already reduced the size of the sub-words

        for (int i = 0; i < numSplits; ++i) {

            
            int x = (roughDivision-1)*(numSplits-(i)); // see next comment
            // checks to see if a reduced word length * remaining splits exactly equals remaining letters
            if (!reduced && x == remainingLetters) {
                roughDivision -= 1;
                reduced = true;
            }

            solution.add(roughDivision); 
            remainingLetters -= roughDivision;
        }

        return solution;
    }

    public static String[] determineSplits(boolean rightJustified, List<Integer> splitLengths, String input){

        String[] result = new String[splitLengths.size()];
        if(rightJustified) Collections.reverse(splitLengths);
        int index = 0;
        for(int i = 0; i < splitLengths.size(); ++i){
            result[i] = input.substring(index, index+splitLengths.get(i));
            index+=splitLengths.get(i);
        }

        return result;
    }

    public static String blendTwo(String hexOne, String hexTwo, String input){

        //Output will be appended over time
        StringBuilder output = new StringBuilder();

        //Loop through each step
        for(float j = 0; j <= (input.length() - 1); ++j){
            output.append("&#" 
            + (padWithZeros(Integer.toHexString(Integer.parseInt(hexOne.substring(0, 2),16) + 
                (int)((j / (input.length() - 1)) * (Integer.parseInt(hexTwo.substring(0, 2),16) - Integer.parseInt(hexOne.substring(0, 2),16)))))
            + padWithZeros(Integer.toHexString(Integer.parseInt(hexOne.substring(2, 4),16) + 
                (int)((j / (input.length() - 1)) * (Integer.parseInt(hexTwo.substring(2, 4),16) - Integer.parseInt(hexOne.substring(2, 4),16)))))
            + padWithZeros(Integer.toHexString(Integer.parseInt(hexOne.substring(4, 6),16) + 
                (int)((j / (input.length() - 1)) * (Integer.parseInt(hexTwo.substring(4, 6),16) - Integer.parseInt(hexOne.substring(4, 6),16)))))).toUpperCase()
            + input.charAt((int)j));
        }

        return output.toString();
    }

    public static String blendMain(int howManyCodes, String input, String[] codeArray, boolean rightJustified){


        //New builder
        StringBuilder output = new StringBuilder();
        int codeIndex = 0;
        List<Integer> splitLengths = findSplitLengths(input, (howManyCodes - 1));
        String[] splits = determineSplits(rightJustified, splitLengths, input);

        for(int i = 0; i < splits.length; i++ ){
            if(i != (splits.length -1)) splits[i] = splits[i] + splits[i + 1].substring(0, 1);

            String addendum = blendTwo(codeArray[codeIndex], codeArray[codeIndex + 1], splits[i]);
            output.append(i != (splits.length - 1) ? addendum.substring(0, addendum.length() - 9) : addendum);;
            codeIndex++;
        }

        return output.toString();
    }
}
