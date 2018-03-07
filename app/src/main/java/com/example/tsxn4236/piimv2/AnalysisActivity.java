package com.example.tsxn4236.piimv2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.*;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;

import static org.bytedeco.javacpp.opencv_highgui.imread;


public class AnalysisActivity extends Activity implements View.OnClickListener{

    private Tools tools = new Tools();
    private GlobalVariable globalVariable = new GlobalVariable();

    private ImageView imageView;

    protected Button webButton;
    private int groupeKNN=0;

    private ArrayList<String> resources_ID_WEB = new ArrayList<>();
    private Intent i;

    private RequestQueue queue;
    private ProgressBar bar;

    public Intent getI() {
        return i;
    }

    public void setI(Intent i) {
        this.i = i;
    }

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);
        queue = Volley.newRequestQueue(this);
        init();
        imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.load));
        new ThreadAnalysis().execute("");

    }

    public void onClick(View v) {
        switch(v.getId()) {
            // Si l'identifiant de la vue est celui du bouton capture
            case R.id.returnButton:
                Toast.makeText(this, "Main", Toast.LENGTH_SHORT).show();
                Intent intentResult = new Intent(AnalysisActivity.this, MainActivity.class);
                startActivity(intentResult);
                finish();
                break;
            // Si l'identifiant de la vue est celui du bouton photo library
            case R.id.webButton:
                Toast.makeText(this, "Web", Toast.LENGTH_SHORT).show();
                Intent openURL = new Intent(android.content.Intent.ACTION_VIEW);
                openURL.setData(Uri.parse(resources_ID_WEB.get(groupeKNN)));
                startActivity(openURL);
                break;
        }
    }

    private void setPicture() {

        Mat imageLC = null;
        Intent t = getI();
        tools.logAnalysis("t: "+ t);
        String sPushPicture= getI().getStringExtra(GlobalVariable.TURI);
        String uriYML= getI().getStringExtra(GlobalVariable.URIYML);
        String uriJSON= getI().getStringExtra(GlobalVariable.URIJSON);

        try {
            imageLC = OpenImRead(sPushPicture);
        } catch (Exception e) {
            tools.logAnalysis("Image LC pushPicture pas charge ");
            e.printStackTrace();
        }

        tools.logAnalysis(" imageLC:  "+ imageLC);
        tools.logAnalysis(" uriYML: "+uriYML );
        tools.logAnalysis(" uriJson:  "+uriJSON );

        File tfileYML= new File(uriYML);
        String filePathYML = tfileYML.getPath();
        Mat vocabulary = vocab(filePathYML);

        //create SIFT feature point extracter
        final opencv_nonfree.SIFT detector;
        detector = new opencv_nonfree.SIFT(SiftVariables.nFeatures, SiftVariables.nOctaveLayers, SiftVariables.contrastThreshold, SiftVariables.edgeThreshold, SiftVariables.sigma);

        //create a matcher with FlannBased Euclidien distance (possible also with BruteForce-Hamming)
        final opencv_features2d.FlannBasedMatcher matcher;
        matcher = new opencv_features2d.FlannBasedMatcher();

        //create BoF (or BoW) descriptor extractor
        final opencv_features2d.BOWImgDescriptorExtractor bowide;
        bowide = new opencv_features2d.BOWImgDescriptorExtractor(detector.asDescriptorExtractor(), matcher);

        //Set the dictionary with the vocabulary we created in the first step
        bowide.setVocabulary(vocabulary);

        File tfileJSON= new File(uriJSON);
        //String filePathJSON = tfileJSON.getPath();

        try{
            FileInputStream fis= new FileInputStream(tfileJSON);
            BufferedReader is=new BufferedReader(new InputStreamReader(fis));
            String SJson=is.readLine();
            tools.logAnalysis("JsonComplet: "+SJson+"\n" );
            JSONObject json= new JSONObject(SJson);
            int classNumber = json.getJSONArray("brands").length();
            bar.setMax(classNumber);
            tools.logAnalysis(":Number Class : "+classNumber );
            //String[] class_names; class_names = new String[classNumber];
            String[][] class_names;
            class_names = new String[classNumber][4];

            for(int i=0;i<json.getJSONArray("brands").length(); i++) {

                resources_ID_WEB.add(json.getJSONArray("brands").getJSONObject(i).getString("url"));
               // class_names[i]=json.getJSONArray("brands").getJSONObject(i).getString("brandname");
                //class_names[i]= json.getJSONArray("brands").getJSONObject(i).getString("classifier").substring(0, json.getJSONArray("brands").getJSONObject(i).getString("classifier").lastIndexOf('.'));
                class_names[i][0]= Integer.toString(i);
                class_names[i][1]=json.getJSONArray("brands").getJSONObject(i).getString("classifier").substring(0, json.getJSONArray("brands").getJSONObject(i).getString("classifier").lastIndexOf('.'));
                class_names[i][2]=json.getJSONArray("brands").getJSONObject(i).getString("url");
                class_names[i][3]=json.getJSONArray("brands").getJSONObject(i).getJSONArray("images").getString(0);
                tools.logAnalysis("XmlClasse: "+class_names[i][3]);
            }


            final opencv_ml.CvSVM[] classifiers;
            classifiers = new opencv_ml.CvSVM[classNumber];
            for (int i = 0 ; i < classNumber ; i++) {
                tools.logAnalysis("ClassNumbers: "+i+"/"+ classNumber);
                //open the file to write the resultant descriptor
                classifiers[i] = new opencv_ml.CvSVM();
                //System.out.println("class "+classifiers[i].get_support_vector_count());
                classifiers[i].load(getCacheDir() +File.separator+ class_names[i][1]+ ".xml");
            }
            tools.logAnalysis("classifier:  "+classifiers[0].get_support_vector_count() );

            Mat response_hist = new Mat();
            opencv_features2d.KeyPoint keypoints = new opencv_features2d.KeyPoint();
            Mat inputDescriptors = new Mat();

            detector.detectAndCompute(imageLC, Mat.EMPTY, keypoints, inputDescriptors);
            bowide.compute(imageLC, keypoints, response_hist);

            // Finding best match
            float minf = Float.MAX_VALUE;
            String bestMatch = null;

            // loop for all classes
            for (int i = 0; i < classNumber; i++) {
                tools.logAnalysis("Dans Loop for all classes");
                // classifier prediction based on reconstructed histogram
                float res = 0;
                try {
                    res = classifiers[i].predict(response_hist, true);
                    tools.logAnalysis("res: "+res);


                } catch (Exception e) {
                    tools.logAnalysis("erreur res");
                    e.printStackTrace();
                }
                //System.out.println(class_names[i] + " is " + res);
                if (res < minf) {
                    minf = res;
                    bestMatch = class_names[i][1];
                    tools.logAnalysis(" l'image actuelle: " + bestMatch );

                }
            }
            for(int i = 0; i < class_names.length; i++) {
                tools.logAnalysis("classLoad: "+getCacheDir() +File.separator+ class_names[i][1]+ ".xml");
                globalVariable.incCptProgressBar();
                bar.setProgress(globalVariable.getCptProgressBar());
                if ( class_names[i][1].equals( bestMatch )) {
                    groupeKNN=Integer.parseInt(class_names[i][0]);
                    tools.logAnalysis("groupeKinn "+groupeKNN);
                   // break;
                }
            }
            RequestImage(GlobalVariable.urlServer+"train-images/"+class_names[groupeKNN][3]);
            tools.logAnalysis(" l'image  predicted as " + bestMatch );
            globalVariable.setBestmatch(bestMatch);

        }
            catch (Exception e){
            e.printStackTrace();

        }
        tools.logAnalysis("Result : "+groupeKNN );// Print du resultat, le groupe auquel l'image appartient.
    }

    private Mat vocab(String path){

            final Mat vocabulary;
            tools.logAnalysis(" vocab read vocabulary from file...");
            Loader.load(opencv_core.class);

            opencv_core.CvFileStorage storage = opencv_core.cvOpenFileStorage(path, null, opencv_core.CV_STORAGE_READ);
            tools.logAnalysis("vocab storage"+storage);
            Pointer p = opencv_core.cvReadByName(storage, null, "vocabulary", opencv_core.cvAttrList());
            opencv_core.CvMat cvMat = new opencv_core.CvMat(p);
            vocabulary = new opencv_core.Mat(cvMat);

            tools.logAnalysis("vocabulary loaded " + vocabulary.rows() + " x " + vocabulary.cols());
            tools.logAnalysis("vocabulary "+vocabulary);
        return vocabulary;
    }

    private Mat OpenImRead (String nameFile){
        Mat test = null;
        tools.logAnalysis(" pathOpenImRead: :"+nameFile);
        try {
            test = imread(nameFile);
            tools.logAnalysis("imread file: "+nameFile+" \nMat: "+ test);
        } catch (Exception e) {
            e.printStackTrace();
           tools.logAnalysis("imread file ko");
        }
        return test;
    }

    private void RequestImage(String url){
        ImageRequest imageRequest = new ImageRequest(url,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {

                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG,100,bos);
                        String filePath = getApplicationContext().getCacheDir().toString() + "/" + "predictedimage";
                        byte[] bitmapData = bos.toByteArray();
                        try {
                            FileOutputStream output = new FileOutputStream(filePath);
                            output.write(bitmapData);
                            output.close();
                            output.flush();
                            tools.logAnalysis("predictImagefilePathOnReponse: "+ filePath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                },
                0, 0, ImageView.ScaleType.CENTER_CROP, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        tools.logAnalysis( "load error");
                    }
                }
        );
        queue.add(imageRequest);
        //mRequestQueue.add(stringRequest);
    }

    private void viewImagePredicted(){
        TextView textView;
        textView = findViewById(R.id.viewMark);
        textView.setText(globalVariable.getBestmatch());
        Uri myUri = Uri.parse(this.getCacheDir() + "/" + "predictedimage");
        imageView.setImageURI( myUri);
        tools.logAnalysis("PredictedImage: "+ myUri.getPath()+"\nPredictImage to String: "+ myUri.toString());
    }

    public void testview(){

        Uri myUri = Uri.parse(this.getCacheDir() + "/" + "pushpicture");
        imageView.setImageURI( myUri);
    }

    private void init(){

        Button returnButton; // creation des variables de la gestions des boutons
        returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(this);

        webButton = findViewById(R.id.webButton);
        webButton.setOnClickListener(this);

        imageView = findViewById(R.id.analysisPicture);
        imageView.setOnClickListener(this);

        bar = findViewById(R.id.progressBar);
        bar.setVisibility(View.VISIBLE);
        setI(getIntent());
    }
	
    private class ThreadAnalysis extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params){
            try {
                setPicture();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.interrupted();
            }
            tools.logAnalysis("fin du thread");
            return "doInBackground Finish";
        }
        @Override
        protected void onPostExecute(String result) {
            viewImagePredicted();
            tools.logAnalysis("fin du Post Execute");
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
										   
    }

}