package com.tungpt.processmanager.model;

import java.util.ArrayList;

public class ListProcess {
    private static ArrayList<String> sListProcess;

    public static void setListProcess(ArrayList<String> listProcess){
        sListProcess = listProcess;
    }
    public static ArrayList<String> getsListProcess(){
        return sListProcess;
    }
}
