package com.util.updater;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class CommandExecuter {
    
    //Run a command asynchronously, outside of the JVM on Windows
    public String executeCommandWindows(String command) {
        try {
            Runtime.getRuntime().exec(command).waitFor();
        } catch (IOException e) {
            return(e.getMessage());
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
            return(e.getMessage());
        }
        return "";
    }

    //Roundabout way to execute a command separate from the JVM on linux
    public String executeCommandLinux(String command) {

        //Write the command to the shell script
        try(FileWriter writer = new FileWriter(new File(System.getProperty("user.dir") + "/Update.sh"))){
            writer.write(command);
        }
        catch(IOException ex){
            //Catch any IO errors - should not ever be a problem
            Alert errorAlert = new Alert(AlertType.ERROR, "Error updating, please report this.");
            errorAlert.setHeaderText(ex.getLocalizedMessage());
            errorAlert.showAndWait();
            return("Exception thrown in executeCommandLinux(); Stacktrace: " + ex.getStackTrace());
        }

        // nohup detaches the script from the JVM process itself
        ProcessBuilder processBuilder = new ProcessBuilder().command("nohup", "sh", "Update.sh");

        try {
            //Set the process to start in the dir that the .jar is located
            processBuilder.directory(new File(System.getProperty("user.dir")));
            //Don't shoot errors - log them instead
            processBuilder.redirectErrorStream(false);
            //Create and start the process
            Process updateProcess = processBuilder.start();
            //Waitfor prevents pre-emptive termination
            updateProcess.waitFor();
    
        } catch (IOException e) {
            return("Error thrown from processBuilder in executeCommandLinux(); Stacktrace: " + e.getStackTrace());
        }
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
            return("Exception in executeCommandLinux(); Stacktrace: " + ex.getStackTrace());
        }
        return "";
    }

    public String executeCommandMacOS(String command){
        try {
            Runtime.getRuntime().exec(
                new String[]{"/bin/zsh", "-c", command}
            ).waitFor();
        } catch (IOException e) {
            return(e.getMessage());
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
            return(e.getMessage());
        }
        return "";
    }
}
