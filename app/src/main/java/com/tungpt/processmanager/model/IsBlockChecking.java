package com.tungpt.processmanager.model;

public class IsBlockChecking {
    private static boolean isChecking = false;

    public static void setIsChecking(boolean checking){
        isChecking = checking;
    }

    public static boolean getChecking(){
        return isChecking;
    }
}
