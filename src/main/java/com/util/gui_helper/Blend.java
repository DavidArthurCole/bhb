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

        List<Integer> solution = new ArrayList<>();

        int roughDivision = (int) Math.ceil((double)(word.length())/numSplits); // the length of each divided word
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

    public static List<String> determineSplits(boolean rightJustified, List<Integer> splitLengths, String input){

        List<String> result = new ArrayList<>(splitLengths.size());
        if(rightJustified) Collections.reverse(splitLengths);
        int index = 0;
        for(int i = 0; i < splitLengths.size(); ++i){
            result.add(i, input.substring(index, index+splitLengths.get(i)));
            index+=splitLengths.get(i);
        }

        return result;
    }

    public static String blendTwo(String hexOne, String hexTwo, String input){

        //Output will be appended over time
        StringBuilder output = new StringBuilder();

        List<Integer> componentsOne = new ArrayList<>(List.of(hexR(hexOne), hexG(hexOne), hexB(hexOne)));
        List<Integer> compDif = new ArrayList<>(List.of(hexR(hexTwo) - hexR(hexOne), hexG(hexTwo) - hexG(hexOne), hexB(hexTwo) - hexB(hexOne)));

        //Loop through each step
        for(float j = 0; j <= (input.length() - 1); ++j){
            float gainPercent = (j / (input.length() - 1));
            output.append("&#" + ( 
                padWithZeros((int)(componentsOne.get(0) + (gainPercent * compDif.get(0)))) +
                padWithZeros((int)(componentsOne.get(1) + (gainPercent * compDif.get(1)))) +
                padWithZeros((int)(componentsOne.get(2) + (gainPercent * compDif.get(2))))
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

    public static String blendMain(int howManyCodes, String input, List<String> codeList, boolean rightJustified){

        //New builder
        StringBuilder output = new StringBuilder();
        List<Integer> splitLengths = findSplitLengths(input, (howManyCodes - 1));
        List<String> splits = new ArrayList<>(determineSplits(rightJustified, splitLengths, input));

        for(int i = 0, codeIndex = 0; i < splits.size(); i++, codeIndex++){
            if(i != (splits.size() -1)) splits.set(i, splits.get(i) + splits.get(i + 1).substring(0, 1));

            String addendum = blendTwo(codeList.get(codeIndex), codeList.get(codeIndex + 1), splits.get(i));
            output.append(i != (splits.size() - 1) ? addendum.substring(0, addendum.length() - 9) : addendum);
        }

        return output.toString();
    }
}
