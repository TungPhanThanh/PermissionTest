package com.tungpt.processmanager.model;

import java.util.ArrayList;

public class ListUrl {
    private static ArrayList<String> sListUrl;

    public static void setListUrl(ArrayList<String> ListUrl){
        sListUrl = ListUrl;
    }
    public static ArrayList<String> getsListUrl(){
        return sListUrl;
    }
}
