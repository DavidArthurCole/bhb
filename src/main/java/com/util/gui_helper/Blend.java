package com.util.gui_helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Blend {

    private Blend(){}

    private static String padWithZeros(int inputInt){
        return String.format("%1$" + 2 + "s", Integer.toHexString(inputInt)).replace(' ', '0');
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

        Integer[] componentsOne = new Integer[]{
            hexR(hexOne),
            hexG(hexOne),
            hexB(hexOne)
        };

        Integer[] compDif = new Integer[]{
            hexR(hexTwo) - hexR(hexOne),
            hexG(hexTwo) - hexG(hexOne),
            hexB(hexTwo) - hexB(hexOne)
        };

        //Loop through each step
        for(float j = 0; j <= (input.length() - 1); ++j){
            float gainPercent = (j / (input.length() - 1));
            output.append("&#" + ( 
                padWithZeros((int)(componentsOne[0] + (gainPercent * compDif[0]))) +
                padWithZeros((int)(componentsOne[1] + (gainPercent * compDif[1]))) +
                padWithZeros((int)(componentsOne[2] + (gainPercent * compDif[2])))
            ).toUpperCase() + input.charAt((int)j));
        }
        
        return output.toString();
    }

    //Splitters to return R G and B values (as Integers) of a hex string
    private static Integer hexR(String hex){
        return Integer.parseInt(hex.substring(0, 2), 16);
    }

    private static Integer hexG(String hex){
        return Integer.parseInt(hex.substring(2, 4), 16);
    }

    private static Integer hexB(String hex){
        return Integer.parseInt(hex.substring(4, 6), 16);
    }

    public static String blendMain(int howManyCodes, String input, String[] codeArray, boolean rightJustified){

        //New builder
        StringBuilder output = new StringBuilder();
        List<Integer> splitLengths = findSplitLengths(input, (howManyCodes - 1));
        String[] splits = determineSplits(rightJustified, splitLengths, input);

        for(int i = 0, codeIndex = 0; i < splits.length; i++, codeIndex++){
            if(i != (splits.length -1)) splits[i] = splits[i] + splits[i + 1].substring(0, 1);

            String addendum = blendTwo(codeArray[codeIndex], codeArray[codeIndex + 1], splits[i]);
            output.append(i != (splits.length - 1) ? addendum.substring(0, addendum.length() - 9) : addendum);
        }

        return output.toString();
    }
}
