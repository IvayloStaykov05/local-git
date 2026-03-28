package com.example.project.backend.model.enums;

public enum DocumentRole {
    OWNER,
    AUTHOR,
    REVIEWER,
    READER;

    public static DocumentRole stringToEnum(String string){
        string = string.toLowerCase();
        switch(string){
            case "owner":
                return DocumentRole.OWNER;
            case "author":
                return DocumentRole.AUTHOR;
            case "reviewer":
                return DocumentRole.REVIEWER;
            case "reader":
                return DocumentRole.READER;
            default:
                return null;
        }
    }
}


