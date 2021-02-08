package com.orbitsoftlabs.carplatetfcl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bilal.androidthingscameralib.InitializeCamera;
import com.bilal.androidthingscameralib.OnPictureAvailableListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.orbitsoftlabs.carplatetfcl.env.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

//lib_support/java/ClassifierQuantizedMobilenet
//change getModelPath() for model filename and getLabelPath() for label filename

//Twilio
//Change accountSid, authToken and twilioSender for a new configuration from Twilio Dashboard

public class ClassifierNew extends AppCompatActivity implements OnPictureAvailableListener {
    private ImageView capturedImage;

    private InitializeCamera initializeCamera;

    protected TextView recognitionTextView,
            recognition1TextView,
            recognition2TextView,
            recognitionValueTextView,
            recognition1ValueTextView,
            recognition2ValueTextView;

    protected ImageView bottomSheetArrowImageView;

    private TextView recognizedText;

    private Classifier.Model model = Classifier.Model.QUANTIZED_MOBILENET;
    private Classifier.Device device = Classifier.Device.CPU;
    private int numThreads = -1;

    String accountSid = "AC133749800da1f0cba449304dba159494";
    String authToken = "d36c512d87765bec1d140781dcd9e265";
    String twilioSender = "+14804626379";

    private static final Logger LOGGER = new Logger();

    private Classifier classifier;
    private FirebaseVisionTextRecognizer recognizer;
    private FirebaseVisionImage image;
    ArrayList<String> plateToBeCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tfe_ic_activity_camera);

        capturedImage = findViewById(R.id.capturedImage);

        initializeCamera =  new InitializeCamera(this, this,
                800,800,10);

        bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);
        bottomSheetArrowImageView.setVisibility(View.GONE);
        recognizedText = findViewById(R.id.recognized_text);

        recognitionTextView = findViewById(R.id.detected_item);
        recognitionValueTextView = findViewById(R.id.detected_item_value);
        recognition1TextView = findViewById(R.id.detected_item1);
        recognition1ValueTextView = findViewById(R.id.detected_item1_value);
        recognition2TextView = findViewById(R.id.detected_item2);
        recognition2ValueTextView = findViewById(R.id.detected_item2_value);

        plateToBeCheck = new ArrayList<>();

        recognizer = FirebaseVision.getInstance().getCloudTextRecognizer();

        recreateClassifier(model, device, numThreads);
        if (classifier == null) {
            LOGGER.e("No classifier on preview!");
            return;
        } else{
            capture();
        }
    }

    @Override
    public void onPictureAvailable(byte[] imageBytes) {

        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        capturedImage.setImageBitmap(bitmap);

        if (classifier != null) {

            final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap, 0);
            image = FirebaseVisionImage.fromBitmap(bitmap);

            recognizer.processImage(image)
                            .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                @Override
                                public void onSuccess(FirebaseVisionText visionText) {
                                    String resultText = visionText.getText();

                                    showResultsInBottomSheet(results, resultText);
                                }
                            })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Task failed with an exception
                                            // ...
                                            ClassifierNew.this.recreate();
                                        }
                                    });
        }
    }

    private void capture(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initializeCamera.captureImage();
            }
        }, 0);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Make sure to release the camera resources.
        if (initializeCamera != null) {
            initializeCamera.releaseCameraResources();
        }

        try {

        } catch (Exception e) {

        }
    }

    private void sendSMS(String ACCOUNT_SID, String AUTH_TOKEN, String plate ){

        //plateToBeCheck.add(plate);

        String plateToFormat = plate.replace("[","").replace("]","");
        //String[] recognized_plate = String.valueOf(plateToFormat).split("\\r?\\n");
        String plate_last_char = plateToFormat.substring(plateToFormat.length() - 3);

        String plateNoSymbols = plate.replaceAll(" ","").replaceAll("[^A-Za-z0-9]","");

        String new_plate = plateNoSymbols.substring(plateNoSymbols.length() - 3);
        if (!plateToBeCheck.contains(new_plate)) {
            plateToBeCheck.add(new_plate);

            Log.d("Plate", plate + " not contains " + plate_last_char);

            Log.d("Initial Plate Array", plate + ", " + plate_last_char);
            Log.d("Plate Array", String.valueOf(plateToBeCheck).replace("[","")
                    .replace("]",""));

            // String checked_plate = String.valueOf(plateToBeCheck).replace("[","").replace("]","");

            String body = plate+ " is violating Provincial Ordinance No. 164";
            String from = twilioSender;
            String to = "+639772671389";

            String base64EncodedCredentials = "Basic " + Base64.encodeToString(
                    (ACCOUNT_SID + ":" + AUTH_TOKEN ).getBytes(), Base64.NO_WRAP
            );

            Log.d("SMS", "Sending...");
            Map<String, String> smsData = new HashMap<>();
            smsData.put("From", from);
            smsData.put("To", to);
            smsData.put("Body", body);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.twilio.com/2010-04-01/")
                    .build();
            TwilioApi api = retrofit.create(TwilioApi.class);

            api.sendMessage(ACCOUNT_SID, base64EncodedCredentials, smsData).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) Log.d("SMS", "onResponse->success");
                    else Log.d("SMS", "onResponse->failure");
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    //Log.d("TAG", "onFailure");
                }
            });

        } if (plateToBeCheck.contains(plateNoSymbols)){
            //plateToBeCheck.clear();
            //plateToBeCheck.add(plate);
            Log.d("Plate Array", "Array already contains " + plate + ", " + plate_last_char);
        }
    }

    private void showResultsInBottomSheet(List<Classifier.Recognition> results, String recognized_text) {
        if (results != null && results.size() >= 3) {
            Classifier.Recognition recognition = results.get(0);
            String new_recognized_text = recognized_text.replace("-","")
                    .replace(":","").replace(",","")
                    .replace(".","").replace(";","");
            recognizedText.setText("");
            // && results.get(0).getTitle().equals("private automobile")
            //&& !results.get(0).getTitle().equals("cant detect")
            if(!recognized_text.equals("") && results.get(0).getTitle().equals("automobiles private ")){

                recognizedText.setText(recognized_text);
                String[] recognized_texts = new_recognized_text.split("\\r?\\n");
                String recTextLast = recognized_texts[0].substring(String.valueOf(recognized_texts[0]).length() - 1);

                DateFormat day = new SimpleDateFormat("EEE");
                String currentDay = day.format(Calendar.getInstance().getTime());

                DateFormat time = new SimpleDateFormat("kk");
                //String currentTime = time.format(Calendar.getInstance().getTime());
                int currentTime = Integer.parseInt(time.format(Calendar.getInstance().getTime()));

                //Log.d("Plate Number: ", new_recognized_text);
                //Monday
                if(currentDay.equals("Mon")){

                    if (recTextLast.equals("1") || recTextLast.equals("2")){
                        if (currentTime >= 07 && currentTime <= 10){
                            sendSMS(accountSid,authToken, recognized_texts[0]);
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Active Hour"), Toast.LENGTH_SHORT).show();
                        } else if (currentTime >= 15 && currentTime <= 19){
                            sendSMS(accountSid,authToken, recognized_texts[0]);
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Active Hour"), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Window Hour"), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                //Tuesday
                if(currentDay.equals("Tue")){

                    if (recTextLast.equals("3") || recTextLast.equals("4")){
                        if (currentTime >= 07 && currentTime <= 10){
                            sendSMS(accountSid,authToken, recognized_texts[0]);
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Active Hour"), Toast.LENGTH_SHORT).show();
                        } else if (currentTime >= 15 && currentTime <= 19){
                            sendSMS(accountSid,authToken, recognized_texts[0]);
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Active Hour"), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Window Hour"), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                //Wednesday
                if(currentDay.equals("Wed")){

                    if (recTextLast.equals("5") || recTextLast.equals("6")){
                        if (currentTime >= 07 && currentTime <= 10){
                            sendSMS(accountSid,authToken, recognized_texts[0]);
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Active Hour"), Toast.LENGTH_SHORT).show();
                        } else if (currentTime >= 15 && currentTime <= 19){
                            sendSMS(accountSid,authToken, recognized_texts[0]);
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Active Hour"), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Window Hour"), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                //Thursday
                if(currentDay.equals("Thu")){

                    if (recTextLast.equals("7") || recTextLast.equals("8")){
                        if (currentTime >= 07 && currentTime <= 10){
                            sendSMS(accountSid,authToken, recognized_texts[0]);
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Active Hour"), Toast.LENGTH_SHORT).show();
                        } else if (currentTime >= 15 && currentTime <= 19){
                            sendSMS(accountSid,authToken, recognized_texts[0]);
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Active Hour"), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Window Hour"), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                //Friday
                if(currentDay.equals("Fri")){

                    if (recTextLast.equals("9") || recTextLast.equals("0")){
                        if (currentTime >= 07 && currentTime <= 10){
                            sendSMS(accountSid,authToken, recognized_texts[0]);
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Active Hour"), Toast.LENGTH_SHORT).show();
                        } else if (currentTime >= 15 && currentTime <= 19){
                            sendSMS(accountSid,authToken, recognized_texts[0]);
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Active Hour"), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " Window Hour"), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                //Saturday & Sunday
                if(currentDay.equals("Sat") || currentDay.equals("Sun")){
                    Toast.makeText(getApplicationContext(), String.valueOf(currentDay + " " + currentTime + " No Coding"), Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(getApplicationContext(), "No Plate Number Detected or the plate detected is exempted from coding scheme.", Toast.LENGTH_SHORT).show();
            }
            if (recognition != null) {
                if (recognition.getTitle() != null)
                    recognitionTextView.setText(recognition.getTitle());
                //Toast.makeText(getApplicationContext(), recognition.getTitle(), Toast.LENGTH_LONG).show();
                if (recognition.getConfidence() != null)
                    recognitionValueTextView.setText(
                            String.format("%.2f", (10000 * recognition.getConfidence())) + "%");
            }

            Classifier.Recognition recognition1 = results.get(1);
            if (recognition1 != null) {
                if (recognition1.getTitle() != null) recognition1TextView.setText(recognition1.getTitle());
                //if (recognition1.getConfidence() != null)
                    //recognition1ValueTextView.setText(String.format("%.1f", (10000 * recognition1.getConfidence())) + "%");
            }

            Classifier.Recognition recognition2 = results.get(2);
            if (recognition2 != null) {
                if (recognition2.getTitle() != null) recognition2TextView.setText(recognition2.getTitle());
                //if (recognition2.getConfidence() != null)
                    //recognition2ValueTextView.setText(String.format("%.1f", (10000 * recognition2.getConfidence())) + "%");
            }
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //capturedImage.setVisibility(View.INVISIBLE);
                capture();
            }
        }, 0);
    }

    private void recreateClassifier(Classifier.Model model, Classifier.Device device, int numThreads) {
        if (classifier != null) {
            //LOGGER.d("Closing classifier.");
            classifier.close();
            classifier = null;
        }
        if (device == Classifier.Device.GPU
                && (model == Classifier.Model.QUANTIZED_MOBILENET || model == Classifier.Model.QUANTIZED_EFFICIENTNET)) {
            //LOGGER.d("Not creating classifier: GPU doesn't support quantized models.");
            runOnUiThread(
                    () -> {
                        Toast.makeText(this, R.string.tfe_ic_gpu_quant_error, Toast.LENGTH_LONG).show();
                    });
            return;
        }
        try {
            //LOGGER.d("Creating classifier (model=%s, device=%s, numThreads=%d)", model, device, numThreads);
            classifier = Classifier.create(this, model, device, numThreads);
        } catch (IOException | IllegalArgumentException e) {
            //LOGGER.e(e, "Failed to create classifier.");
            runOnUiThread(
                    () -> {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            return;
        }

    }
}

