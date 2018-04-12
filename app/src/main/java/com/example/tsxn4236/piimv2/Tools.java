package com.example.tsxn4236.piimv2;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.*;

public class Tools {
    /*
    Classe pour l'ensembles des oyutils aidant au debug et au run
     */

    private static final String TAG = Tools.class.getName();

    public void logFullFile(File file){
        logg("Name: "+file.getName()
                +"\nPath: "+file.getAbsolutePath()
                +"\nExist: "+file.exists()
                +"\nlength: "+ file.length()
        );
    }

    public void logg (String txt){
        Log.i(TAG,"#@ "+txt);
    }

    public void logFullBitmap(Bitmap bitmap){
        logg(   "Bitmap "
                +"\nName: " + bitmap.toString()
                +"\nhauteur: " + bitmap.getHeight()
                +"\nlargeur: " + bitmap.getWidth()
        );
    }

    public void logAnalysis(String txt){
        Log.i(TAG,"#@Analysis "+txt);
    }

    public void logMain(String txt){
        Log.i(TAG,"#@Main "+txt);
    }


    public class ExtendTools extends AppCompatActivity{
        Context context;
        public Context getContext() {
            return context;
        }

        public void setContext(Context context) {
            this.context = context;
        }


    }
}


