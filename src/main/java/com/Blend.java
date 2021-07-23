package com;

import java.util.Scanner;

public class Blend {

    private Blend(){}

    protected static Scanner scanner = new Scanner(System.in);

    public static String padWithZeros(String inputString){
        return String.format("%1$" + 2 + "s", inputString).replace(' ', '0');
    }

    public static String blendTwo(String hexOne, String hexTwo, String input){

        //Output will be appended over time
        StringBuilder output = new StringBuilder();

        //Gets RGB values in integer form
        float hexOneRVal = Integer.parseInt(hexOne.substring(0, 2),16);
        float hexOneGVal = Integer.parseInt(hexOne.substring(2, 4),16);
        float hexOneBVal = Integer.parseInt(hexOne.substring(4, 6),16);

        float hexTwoRVal = Integer.parseInt(hexTwo.substring(0, 2),16);
        float hexTwoGVal = Integer.parseInt(hexTwo.substring(2, 4),16);
        float hexTwoBVal = Integer.parseInt(hexTwo.substring(4, 6),16);

        //Loop through each step
        for(float j = 0; j <= (input.length() - 1); ++j){

            //Calculate what percentage of the color should be faded
            float percent = (j / (input.length() - 1));

            //Combine into one
            StringBuilder hexToAdd = new StringBuilder(
               padWithZeros(Integer.toHexString((int)hexOneRVal + Math.round(percent * (hexTwoRVal - hexOneRVal))))
             + padWithZeros(Integer.toHexString((int)hexOneGVal + Math.round(percent * (hexTwoGVal - hexOneGVal))))
             + padWithZeros(Integer.toHexString((int)hexOneBVal + Math.round(percent * (hexTwoBVal - hexOneBVal)))));

            output.append("&#" + hexToAdd.toString().toUpperCase() + Character.toString(input.charAt((int)j)));
        }

        return output.toString();
    }

    public static String blendMain(int howManyCodes, String input, String[] codeArray){

        //New builder
        StringBuilder output = new StringBuilder();

        //This is easy - Blend the colors, return it
        if(howManyCodes == 2) return(blendTwo(codeArray[0], codeArray[1], input));
        //This is not - Figure out if it's odd or even, blend from there, recursively
        else blendHard(howManyCodes, input, codeArray, output);

        return output.toString();
    }

    public static void blendHard(int howManyCodes, String input, String[] codeArray, StringBuilder output){

        //Create a new array to hold separated pieces
        String[] inputSeparated = new String[5];

        //If the midpoint number is even
        if((howManyCodes - 2) % 2 == 0) blendEven(howManyCodes - 2, 0, (input.length() - 1), inputSeparated, input);
        //If midpoint number is odd
        else blendOdd(howManyCodes - 2, 0, (input.length() - 1), inputSeparated, input);

        //Goes through and concatenates blended pieces together into one output
        for (int i = 0; i <= 4; i++) {
            if (inputSeparated[i] != null && !inputSeparated[i].equals("")) {
                if (i == 0) {
                    output.append(blendTwo(codeArray[i], codeArray[i + 1], inputSeparated[i]));
                }
                else {
                    String temp = blendTwo(codeArray[i], codeArray[i + 1], inputSeparated[i]);
                    output.append(temp.substring(9, temp.length()));
                }
            }
        }
    }

    public static void blendEven(int midpoints, int start, int end, String[] inputSeparated, String input){

        int midpoint1;
        int midpoint2;
        int midpoint3;
        int midpoint4;

        if(midpoints == 2){
            midpoint1 = start + 2;
            midpoint2 = end - 2;

            while(((midpoint2 - midpoint1) - midpoint1) >= 2) {
                midpoint1 += 1;
                midpoint2 -= 1;
            }

            inputSeparated[0] = input.substring(start, midpoint1 + 1);
            inputSeparated[1] = input.substring(midpoint1, midpoint2 + 1);
            inputSeparated[2] = input.substring(midpoint2, end + 1);
        }
        else if(midpoints == 4){
            midpoint1 = start + 2;
            midpoint2 = midpoint1 + 2;
            midpoint4 = end - 2;
            midpoint3 = midpoint4 - 2;

            while(Math.abs((midpoint2 - midpoint1) - (midpoint3 - midpoint2)) >= 4){
                midpoint1 += 1;
                midpoint2 += 2;
                midpoint4 -= 1;
                midpoint3 -= 2;
            }

            inputSeparated[0] = input.substring(start, midpoint1 + 1);
            inputSeparated[1] = input.substring(midpoint1, midpoint2 + 1);
            inputSeparated[2] = input.substring(midpoint2, midpoint3 + 1);
            inputSeparated[3] = input.substring(midpoint3, midpoint4 + 1);
            inputSeparated[4] = input.substring(midpoint4, end + 1);
        }
    }

    public static void blendOdd(int midpoints, int start, int end, String[] inputSeparated, String input){
        int midpoint1;
        int midpoint2;
        int midpoint3;

        if(midpoints == 1){
            if (input.length() % 2 == 1) {
                midpoint1 = end / 2;
            }
            else {
                midpoint1 = (end + 1) / 2;
            }

            inputSeparated[0] = input.substring(start, midpoint1 + 1);
            inputSeparated[1] = input.substring(midpoint1, end + 1);
        }
        else if(midpoints == 3){
            //Calculate center midpoint (midpoint2)
            if (input.length() % 2 == 1) {
                midpoint2 = end / 2;
            }
            else {
                midpoint2 = (end + 1) / 2;
            }

            //Calculate midpoint1
            if (midpoint2 % 2 == 0) {
                midpoint1 = (midpoint2 / 2);
            }
            else {
                midpoint1 = ((midpoint2 + 1) / 2);
            }

            //Calculate midpoint3
            if ((end - midpoint2) % 2 == 1) {
                midpoint3 = (end - (midpoint2 / 2));
            }
            else {
                midpoint3 = (end - ((midpoint2 + 1) / 2));
            }

            inputSeparated[0] = input.substring(start, midpoint1 + 1);
            inputSeparated[1] = input.substring(midpoint1, midpoint2 + 1);
            inputSeparated[2] = input.substring(midpoint2, midpoint3 + 1);
            inputSeparated[3] = input.substring(midpoint3, end + 1);
        }
    }

    public static boolean isNumeric(String str) { 
        try {  
          Integer.parseInt(str);  
          return true;
        } 
        catch(NumberFormatException e){  
          return false;  
        }  
    }

    public static boolean isHexOk(String hex){
        if(hex.length() != 6) return false;
        //Starts a counter for how many valid chars there are
        int okCount = 0;
        //For each character in hex     //For each valid hex char               //If it's a valid hex character, increment
        for(int i = 0; i <= 5; i++) for(int j = 0; j <= 15; j++) if(hex.toUpperCase().charAt(i) == ("0123456789ABCDEF".charAt(j))) okCount++;
        //If it's 6, good, else, bad
        return okCount == 6;
    }

}
