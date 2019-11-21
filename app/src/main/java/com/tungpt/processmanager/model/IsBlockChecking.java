package com.tungpt.processmanager.model;

public class IsBlockChecking {
    private static boolean isChecking = false;
    private static boolean isCheckingProcess = false;

    public static void setIsChecking(boolean checking){
        isChecking = checking;
    }

    public static boolean getChecking(){
        return isChecking;
    }

    public static void setIsCheckingProcess(boolean checkingProcess){
        isCheckingProcess = checkingProcess;
    }

    public static boolean getCheckingProcess(){
        return isCheckingProcess;
    }
}
