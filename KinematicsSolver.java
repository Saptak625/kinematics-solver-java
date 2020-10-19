package com.saptakdas.physics.kinematics;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.*;

public class KinematicsSolver {
    private static Scanner sc=new Scanner(System.in);
    private static String[] fields={"t", "s", "u", "v", "a"};
    private static ScriptEngineManager manager = new ScriptEngineManager();
    private static ScriptEngine engine = manager.getEngineByName("js");

    public static void main(String[] args) {
        //Get input
        System.out.println("This is a kinematics solver. Restrictions of this include only working in one dimension and constant acceleration. Please enter all field values using same units throughout.");
        System.out.println("Enter all given fields.\n");
        HashMap<String, Double> inputs = allInputs();
        String unknown;
        while (true) {
            System.out.println("\nEnter the unknown field.\n");
            System.out.print("Field name: ");
            unknown = sc.next();
            if (Arrays.asList(fields).contains(unknown)) {
                if (inputs.get(unknown) == null) {
                    break;
                } else {
                    System.out.println("Field is already filled. Try again.");
                }
            } else {
                System.out.println("Field was not found. Try again.");
            }
        }

        //Solve
        //Getting equation system.
        LinkedList<Object> allKeys = new LinkedList<>(Arrays.asList((Object[]) inputs.keySet().toArray()));
        allKeys.add(unknown);
        String equationSystem="";
        for(String combination: new String[] {"vuat", "suat", "vuas", "suvt", "savt"}){
            int numOfMatched=0;
            for(Object key : allKeys) {
                if(combination.contains((String) key)) {
                    numOfMatched++;
                }
            }
            if(numOfMatched==4) {
                equationSystem=combination;
                break;
            }
        }
        if(equationSystem==""){
            System.out.println("No one step equation was found.");
            System.exit(0);
        }
        System.out.println(equationSystem);

        //Get proper equationSet
        Hashtable<String, Hashtable<String, String[]>> kinematicsEquations=initializeEquations();

        //GUESS
        //Givens
        System.out.println("\nG:");
        for(String givenFieldName: inputs.keySet()){
            System.out.println("-->"+givenFieldName+"="+inputs.get(givenFieldName));
        }

        //U
        System.out.println("\nU:");
        System.out.println("-->"+unknown+"=?");

        //E
        System.out.println("\nE:");
        Object[] keysSorted=allKeys.toArray();
        Arrays.sort(keysSorted);
        if (Arrays.equals(keysSorted, new Object[]{"a", "s", "t", "v"})) {
            System.out.println("Formula is derived by substituting s=((u+v)/2)*t into v=u+at for u.");
        }
        String equationStart=equationSystem.toCharArray()[0]+"="+kinematicsEquations.get(equationSystem).get(Character.toString(equationSystem.toCharArray()[0]))[0]+(kinematicsEquations.get(equationSystem).get(Character.toString(equationSystem.toCharArray()[0])).length>1? " and "+equationSystem.toCharArray()[0]+"="+kinematicsEquations.get(equationSystem).get(Character.toString(equationSystem.toCharArray()[0]))[1]: "");
        System.out.println("Equation: "+ equationStart);
        if(!Character.toString(equationSystem.toCharArray()[0]).equals(unknown)){
            System.out.println("Rearranging equation in terms of "+unknown+".");
            String equationRevised=unknown+"="+kinematicsEquations.get(equationSystem).get(unknown)[0]+(kinematicsEquations.get(equationSystem).get(unknown).length>1? " and "+unknown+"="+kinematicsEquations.get(equationSystem).get(unknown)[1]: "");
            System.out.println("Rearranged Equation: "+ equationRevised);
        }

        //S
        System.out.println("\nS:\nSubstituting...");
        String[] finalEquations=kinematicsEquations.get(equationSystem).get(unknown);
        StringBuilder substitutedEquation= new StringBuilder();
        for(int i=0; i<finalEquations.length; i++){
            if(i>0){
                substitutedEquation.append(" and ");
            }
            String equationSubstitute=finalEquations[i];
            equationSubstitute=equationSubstitute.replace("Math.sqrt", "q");
            for(String givenField: inputs.keySet()){
                equationSubstitute=equationSubstitute.replace(givenField, "("+inputs.get(givenField).toString()+")");
            }
            equationSubstitute=equationSubstitute.replace("q", "Math.sqrt");
            substitutedEquation.append(unknown).append("=").append(equationSubstitute);
        }
        System.out.println("Substituted Equation: "+ substitutedEquation);

        //S
        System.out.println("\nS:\nSolving...");
        try {
            //Setting all known values
            for(String key: inputs.keySet()) {
                engine.eval(key+"="+inputs.get(key));
            }
            //Use correct equation
            StringBuilder finalAnswer=new StringBuilder();
            LinkedList<Double> answers=new LinkedList<>();
            for(int i=0; i<finalEquations.length; i++) {
                boolean contains=false;
                for(double value: answers){
                    if(value==(double) engine.eval(finalEquations[i])){
                        contains=true;
                        break;
                    }
                }
                if(!contains) {
                    if (i > 0) {
                        finalAnswer.append(" and ");
                    }
                    finalAnswer.append(unknown).append("=").append(Double.valueOf(engine.eval(finalEquations[i]).toString()));
                    answers.addLast((double) engine.eval(finalEquations[i]));
                }
            }
            System.out.println("\nAnswer: "+finalAnswer);
            if(answers.size()>1) {
                System.out.println("One answer is likely an extraneous solution.");
                if (unknown.equals("t"))
                    System.out.println("Predicted Answer: t=" + (answers.get(0) >= 0 ? answers.get(0).toString() + (answers.get(1) >= 0 ? " and t=" + answers.get(1) : "") : (answers.get(1) >= 0 ? answers.get(1) : "No Answers?")));
            }
        } catch (ScriptException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static Hashtable<String, Hashtable<String, String[]>> initializeEquations(){
        LinkedList<Hashtable<String, String[]>> equations=new LinkedList<>();
        String[][] unknownVariable={{"v", "u", "a", "t"}, {"s", "u", "a", "t"}, {"v", "u", "a", "s"}, {"s", "u", "v", "t"}, {"s", "a", "v", "t"}};
        String[][][] equationsArr={{{"u+(a*t)"}, {"v-(a*t)"}, {"(v-u)/t"}, {"(v-u)/a"}}, {{"(u*t)+((a*t*t)/2)"}, {"(s/t)-((a*t)/2)"}, {"((2*s)/(t*t))-((2*u)/t)"}, {"(-u+Math.sqrt((u*u)+(2*a*s)))/a", "(-u-Math.sqrt((u*u)+(2*a*s)))/a"}}, {{"Math.sqrt((u*u)+(2*a*s))", "-Math.sqrt((u*u)+(2*a*s))"}, {"Math.sqrt((v*v)-(2*a*s))", "-Math.sqrt((v*v)-(2*a*s))"}, {"((v*v)-(u*u))/(2*s)"}, {"((v*v)-(u*u))/(2*a)"}}, {{"((u+v)/2)*t"}, {"((2*s)/t)-v"}, {"((2*s)/t)-u"}, {"(2*s)/(u+v)"}}, {{"(v*t)-((a*t*t)/2)"}, {"((2*v)/t)-((2*s)/(t*t))"}, {"(s/t)+((a*t)/2)"}, {"((2*v)+Math.sqrt((4*v*v)-(8*a*s)))/(2*a)", "((2*v)-Math.sqrt((4*v*v)-(8*a*s)))/(2*a)"}}};
        for(int i = 0; i < unknownVariable.length; i++){
            equations.addLast(new Hashtable<>());
        }
        for(int i=0; i<unknownVariable.length; i++){
            for(int j=0; j<unknownVariable[i].length; j++) {
                equations.get(i).put(unknownVariable[i][j], equationsArr[i][j]);
            }
        }
        Hashtable<String, Hashtable<String, String[]>> kinematicsEquations=new Hashtable<>();
        for(int i=0; i<equations.size(); i++){
            kinematicsEquations.put(new String[] {"vuat", "suat", "vuas", "suvt", "savt"}[i], equations.get(i));
        }
        return kinematicsEquations;
    }

    public static double input(String variable){
        String value;
        while(true) {
            System.out.print(variable+": ");
            value = sc.next();
            try{
                Double.parseDouble(value);
                break;
            }catch(Exception e){
                System.out.println("Please enter a number!");
            }
        }
        return Double.parseDouble(value);
    }

    public static HashMap<String, Double> allInputs(){
        HashMap<String, Double> inputs=new HashMap<>();
        int numOfFieldsEntered=0;
        while(numOfFieldsEntered<3){
            System.out.print("Enter "+(3-numOfFieldsEntered)+" more field"+(4-numOfFieldsEntered==1? "": "s")+": ");
            String fieldName=sc.next();
            if(Arrays.asList(fields).contains(fieldName)){
                if(inputs.get(fieldName)==null) {
                    inputs.put(fieldName, input(fieldName));
                    numOfFieldsEntered++;
                }else{
                    System.out.println("Field is already filled.");
                }
            }else{
                System.out.println("Field was not found. Try again.");
            }
        }
        return inputs;
    }
}