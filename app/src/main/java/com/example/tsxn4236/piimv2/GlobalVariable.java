package com.example.tsxn4236.piimv2;

public class GlobalVariable {


    public static final int REQUEST_PHOTO_LIB = 1;
    public static final int REQUEST_IMAGE_CAPTURE = 2;
    public static final int REQUEST_ANALYSIS = 3;
    public final static String TURI = "TURI";
    public final static String URIYML = "URIYML";
    public final static String URIJSON = "URIJSON";
    public final static String urlServer = "http://www-rech.telecom-lille.fr/nonfreesift/";


    private boolean isAlreadyDownload = false;
    private int cptFinal=2;
    private int cptProgressBar = 0;

    private boolean isClickable = false;

    private String nameVocabulary;

    private String bestmatch;


    public int getCptProgressBar() {
        return cptProgressBar;
    }

    public void incCptProgressBar(){
        cptProgressBar++;
    }

    public void decCptFinal(){
        cptFinal--;
    }

    public int getCptFinal() {
        return cptFinal;
    }

    public void setCptFinal(int cptFinal2) {
        this.cptFinal = this.cptFinal + cptFinal2;
    }

    public boolean isAlreadyDownload() {
        return isAlreadyDownload;
    }

    public void setAlreadyDownload(boolean alreadyDownload) {
        isAlreadyDownload = alreadyDownload;
    }

    public String getBestmatch() {
        return bestmatch;
    }

    public void setBestmatch(String bestmatch) {
        this.bestmatch = bestmatch;
    }

    public String getNameVocabulary() {
        return nameVocabulary;
    }

    public void setNameVocabulary(String nameVocabulary) {
        this.nameVocabulary = nameVocabulary;
    }

    public boolean isClickable() {
        return isClickable;
    }

    public void setClickable(boolean clickable) {
        isClickable = clickable;
    }

}

