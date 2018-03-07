package com.example.tsxn4236.piimv2;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import com.soundcloud.android.crop.Crop;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Tools tools = new Tools();
    private GlobalVariable globalVariable = new GlobalVariable();
    private FileTreatments fileTreatments = new FileTreatments();



    private Button analysisButton;
    private ImageView imageViewMain;
    private Intent analysisIntent;
    private RequestQueue queue;

    /*

    globalVariable.isClickable et validateAnalysis

     */






    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // set du layout main
        initScreen();
        analysisIntent = new Intent(MainActivity.this, AnalysisActivity.class);
        queue = Volley.newRequestQueue(this);

        new Traitment().execute("");

        //delai pour le téléchargement des fichiers
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == GlobalVariable.REQUEST_PHOTO_LIB ) {
            validateAnalysis();
            getpicture(data, 1);
        }
        else if(resultCode == RESULT_OK && requestCode == GlobalVariable.REQUEST_IMAGE_CAPTURE ){
            validateAnalysis();
            getpicture(data, 2);
        }
        else if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK ) {
            validateAnalysis();
            beginCrop(data.getData());
        } else if (requestCode == Crop.REQUEST_CROP) {
            validateAnalysis();
            handleCrop(resultCode, data);
        }
    }

    public void onClick(View v) {
        switch(v.getId()) {
            // Si l'identifiant de la vue est celui du bouton capture
            case R.id.captureButton:
                tools.logMain("OnClick Capture button");
                startCaptureActivity();
                break;
            case R.id.libraryButton:
                tools.logMain("OnClick library burtton");
                startPhotoLibraryActivity();
                break;
            case R.id.analysisButton:
                tools.logMain("OnClick analysis button");
                startAnalysisActivity();
                break;
        }
    }

    private void startPhotoLibraryActivity() {
        Intent photoLibIntent = new Intent();
        photoLibIntent.setType("image/*");
        photoLibIntent.setAction(Intent.ACTION_GET_CONTENT);
        try {
            startActivityForResult(photoLibIntent, GlobalVariable.REQUEST_PHOTO_LIB);
        } catch (Exception e) {
            Toast.makeText(this, "L'application photo n'est pas disponible", Toast.LENGTH_LONG).show();
        }
    }

    private void startCaptureActivity(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                //creer le fichier image et le stock en local
                fileTreatments.setImageFile(this.getCacheDir());
                photoFile = fileTreatments.getImageFile();
                tools.logMain("Image creer");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                //photoFile.toURI();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFile.toURI());
                tools.logFullFile(photoFile);
                tools.logMain("photoFile.toUri"+photoFile.toURI()+" \nphotoFileLength: "+photoFile.length());
                startActivityForResult(takePictureIntent, GlobalVariable.REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void startAnalysisActivity(){ startActivityForResult(analysisIntent,GlobalVariable.REQUEST_ANALYSIS); }

    private void caseURL(String url){
        File extract = new File(url);
        int posPoint = extract.getName().lastIndexOf('.');
        String extention = extract.getName().substring(posPoint + 1);
        tools.logMain("@@REST basename "+extention);
        switch(extention) {
            case "json":
                RequestJSON(url,getCacheDir().toString());
                break;
            case "yml":
                RequestSTRING(url);
                break;
            default:
                tools.logMain("erreur!!!");
        }
    }

    private void RequestJSON(String url, String path) {
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject json) {
                        try {
                            globalVariable.setNameVocabulary(json.getString("vocabulary"));
                            globalVariable.setCptFinal(json.getJSONArray("brands").length());
                            caseURL(GlobalVariable.urlServer+globalVariable.getNameVocabulary());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        tools.logMain("@@REST json"+"Response is: ok");
                        try {
                            try {
                                String tmp;
                                File file = new File(getApplicationContext().getCacheDir().toString()+File.separator, "json");
                                tmp = fileTreatments.writeInFile(file,json.toString());
                                tools.logMain("tmpJson: "+ tmp);
                                analysisIntent.putExtra(GlobalVariable.URIJSON,tmp);
                                checkRequestFull();
                            }
                            catch (IOException e)
                            {
                                tools.logMain( "File write failed: " + e.toString());
                            }

                            for(int i=0;i<json.getJSONArray("brands").length(); i++){

                                final String name =json.getJSONArray("brands").getJSONObject(i).getString("classifier");
                                String urlClassifier =GlobalVariable.urlServer+"classifiers/"+json.getJSONArray("brands").getJSONObject(i).getString("classifier");
                                setClassifier(urlClassifier,name);
                            }
                        } catch (Exception e) {
                            tools.logMain( "NOK ");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        tools.logMain("REST That didn't work!");
                        Toast.makeText(getApplicationContext(),"Erreur pas d'internet, merci de relancer l'appli",Toast.LENGTH_SHORT).show();
                    }
                }
        );
        queue.add(jsonRequest);
    }

    private void RequestSTRING(String url){
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        tools.logMain("Response is: ok");
                        try
                        {
                            String tmp;
                            File file = new File(getApplicationContext().getCacheDir().toString()+File.separator, "vocabulary");
                            tmp = fileTreatments.writeInFile(file,response);
                            tools.logMain("tmpYML: "+ tmp);
                            analysisIntent.putExtra(GlobalVariable.URIYML,tmp);
                            tools.logFullFile(file);
                            checkRequestFull();
                        }
                        catch (IOException e)
                        {
                            tools.logMain("File write failed: " + e.toString());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        tools.logMain("@@REST That didn't work!");
                        Toast.makeText(getApplicationContext(),"Erreur pas d'internet, merci de relancer l'appli",Toast.LENGTH_SHORT).show();
                    }
                }
        );
        queue.add(stringRequest);
    }

    private void checkRequestFull(){

        globalVariable.decCptFinal();
        if (globalVariable.getCptFinal()==0){
            tools.logMain("CptFinal = 0, tout les fichiers sont dl");
        }
        else{
            tools.logMain("Il reste des fichiers à dl: "+globalVariable.getCptFinal());
        }
    }

    private void beginCrop(Uri source) {
        Uri destination = Uri.fromFile(new File(getCacheDir(), "aftercrop"));
        Crop.of(source, destination).asSquare().start(this);
    }

    private void handleCrop(int resultCode, Intent result) {
        tools.logMain("crop "+resultCode+" "+ RESULT_OK);
        if (resultCode == RESULT_OK) {
            imageViewMain.setImageURI(Crop.getOutput(result));
            try {
                Uri imageUri = Crop.getOutput(result);
                tools.logMain("crop "+imageUri);
                // Flux pour lire les donnÃ©es de la carte SD
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                // obtention d'une image Bitmap
                Bitmap imageUP = BitmapFactory.decodeStream(inputStream);
                tools.logMain("crop "+imageUP);
                String tmp;
                tmp = fileTreatments.creatBitmapByRessources("pushpicture",0,imageUP,getResources(),this.getCacheDir());
                analysisIntent.putExtra(GlobalVariable.TURI,tmp);
                Uri view = Uri.fromFile(new File(getCacheDir(), "pushpicture"));
                imageViewMain.setImageURI(view);
            } catch (IOException e) {

                e.printStackTrace();
            }
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void getpicture(Intent data,int LibOrCap){

        if(LibOrCap==1){ //library

            Uri imageUri = data.getData();
            InputStream inputStream;
            try {
                inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap imageUP = BitmapFactory.decodeStream(inputStream);
                tools.logMain("height: "+ imageUP.getHeight()+" weight: "+ imageUP.getWidth() );
                imageViewMain.setImageBitmap(imageUP);
                String tmp;
                tmp = fileTreatments.creatBitmapByRessources("beforecrop",0,imageUP,getResources(),this.getCacheDir());
                analysisIntent.putExtra(GlobalVariable.TURI,tmp);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                // Message Ã  l'utilisateur
                Toast.makeText(this, "Unable to open image", Toast.LENGTH_LONG).show();
            }catch(IOException e){e.printStackTrace();}

        }else if(LibOrCap==2){ //capture
            tools.logMain("crop capture ok");
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            try {
                String tmp;
                tmp =fileTreatments.creatBitmapByRessources("beforecrop",0,imageBitmap,getResources(),this.getCacheDir());
                analysisIntent.putExtra(GlobalVariable.TURI,tmp);

            }catch(IOException e){e.printStackTrace();}
        }
        Uri source = Uri.fromFile(new File(getCacheDir()+"/beforecrop"));
        beginCrop(source);
    }

    private void validateAnalysis(){
        if (globalVariable.isClickable() ==true ) {
            analysisButton.setClickable(true);
            analysisButton.setBackgroundColor(Color.GREEN);
        }
    }

    private void initScreen(){
         Button captureButton;
         Button libraryButton;
        captureButton = (Button) findViewById(R.id.captureButton);
        captureButton.setOnClickListener(this);

        libraryButton = (Button) findViewById(R.id.libraryButton);
        libraryButton.setOnClickListener(this);

        analysisButton = (Button) findViewById(R.id.analysisButton);
        analysisButton.setOnClickListener(this);
        analysisButton.setClickable(false);
        analysisButton.setBackgroundColor(Color.DKGRAY);

        imageViewMain = (ImageView) findViewById(R.id.mainPicture);

    }

    private void setClassifier(String url, final String name){

        tools.logMain( "setClassifier " + "URL: " +url + " name: "+name);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try
                        {
                            String tmp;
                            File file = new File(getApplicationContext().getCacheDir().toString()+File.separator, name);
                            tmp = fileTreatments.writeInFile(file,response);
                            tools.logMain("tmpXML: "+ tmp);
                            tools.logMain("setClassifier File.getAb: "+ file.getAbsolutePath());
                        }
                        catch (IOException e)
                        {
                            tools.logMain( "File write failed: " + e.toString());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(),"Erreur pas d'internet, merci de relancer l'appli",Toast.LENGTH_SHORT).show();
                    }
                }
        );
        queue.add(stringRequest);

    }

    private class Traitment extends AsyncTask<String, Void, String> {
        GlobalVariable globalVariable = new GlobalVariable();

        @Override
        protected String doInBackground(String... params){
            try {
                Thread.sleep(1000);
                if(globalVariable.isAlreadyDownload()== false){
                    globalVariable.setAlreadyDownload(true);
                    caseURL(GlobalVariable.urlServer+"index.json");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.interrupted();
            }
            tools.logMain("fin du thread");
            return "end of doItBackGround";
        }

        @Override
        protected void onPostExecute(String result) {
            tools.logMain(" fin du Post Execute");
            //Change le bouton en cliquable
            globalVariable.setClickable(true);
            //validateAnalysis();
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}

    }
}