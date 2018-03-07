package com.example.tsxn4236.piimv2;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import java.io.*;

public class FileTreatments {
    private File imageFile;
    private Bitmap resizedBitmap;


    //GlobalVariable globalVariable = new GlobalVariable();
    private Tools tools = new Tools();
    GlobalVariable globalVariable = new GlobalVariable();

    public File getImageFile() {
        tools.logg("imageFile :"+imageFile.length());
        return imageFile;
    }

    public void setImageFile(File path) throws IOException {
        imageFile = null;
        imageFile = new File(path + "/"+"pushpicture");
        tools.logFullFile(imageFile);
    }

    public void setResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);
        // "RECREATE" THE NEW BITMAP
        resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        tools.logFullBitmap(resizedBitmap);
    }

    public Bitmap getResizedBitmap(){
        return resizedBitmap;
    }

    public String writeInFile(File file, String json) throws IOException{
        FileOutputStream fOut = new FileOutputStream(file);
        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
        myOutWriter.append(json.toString());
        myOutWriter.close();
        fOut.flush();
        fOut.close();
        tools.logg("##path "+file.getAbsolutePath());
        return file.getAbsolutePath();

    }

    public String creatBitmapByRessources(String nameFile, int drawableId, Bitmap btm, Resources res,File cacheDir) throws IOException {
        FileOutputStream output;
        Bitmap nfiles;
        String filePath;
        nfiles=btm;
        if(drawableId!=0){
            nfiles = BitmapFactory.decodeResource(res,drawableId);
       }
        if( nfiles.getHeight()> 3000 || nfiles.getWidth() > 2000){
            setResizedBitmap(nfiles,nfiles.getHeight()/2,nfiles.getWidth()/2);
            nfiles = getResizedBitmap();
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        tools.logg("## " + nameFile + nfiles);
        nfiles.compress(Bitmap.CompressFormat.JPEG,100,bos);
        filePath = cacheDir + "/" + nameFile;
        byte[] bitmapData = bos.toByteArray();
        try {
            output = new FileOutputStream(filePath);
            output.write(bitmapData);
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tools.logg("##nameToString: "+ filePath.toString());
        return filePath;
    }


}
