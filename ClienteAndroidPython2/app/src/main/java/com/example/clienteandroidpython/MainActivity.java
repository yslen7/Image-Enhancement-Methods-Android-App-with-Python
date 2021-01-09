package com.example.clienteandroidpython;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.os.Build;
import android.provider.MediaStore;

import com.example.clienteandroidpython.Retrofit.IUploadAPI;
import com.example.clienteandroidpython.Retrofit.RetrofitClient;
import com.example.clienteandroidpython.Utils.Common;
import com.example.clienteandroidpython.Utils.IUploadCallbacks;
import com.example.clienteandroidpython.Utils.ProgressRequestBody;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.net.URISyntaxException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements IUploadCallbacks {

    private static final int PICK_FILE_REQUEST = 1000;
    private static final int PERMISSION_CODE = 1000;
    private static final int IMAGE_CAPTURE_CODE = 1001;
    private static final int WRITE_EXTERNAL_STORAGE_CODE = 1;

    IUploadAPI mService;
    Button btnUpload;
    ImageView imageView;
    Uri selectedFileUri;
    Button mCaptureBtn;
    ImageView mImageView;
    Uri image_uri;
    ProgressDialog dialog;
    EditText mInputEdit;
    String mText;

    private IUploadAPI getAPIUpload(){
        return RetrofitClient.getClient().create(IUploadAPI.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = findViewById(R.id.image_view);
        mCaptureBtn = findViewById(R.id.capture_image_btn);
        mInputEdit = findViewById(R.id.InputEdit);
        // Si damos click al botón de Tomar foto, se abre la cámara
        mCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                    PackageManager.PERMISSION_DENIED){
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permission, PERMISSION_CODE);
                    }
                    else {
                        openCamera();
                    }
                }
                else {
                    openCamera();
                }
            }
        });
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        Toast.makeText(MainActivity.this, "Permiso aceptado", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "Debes aceptar los permisos", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                    }
                }).check();
        //Se crea el mService
        mService = getAPIUpload();
        //InitView
        btnUpload = (Button)findViewById(R.id.btn_upload);
        imageView = (ImageView)findViewById(R.id.image_view);
        //Evento
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseFile();
            }
        });
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadFile();
            }
        });
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Nueva Imagen");
        values.put(MediaStore.Images.Media.DESCRIPTION, "De la cámara");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }

    //MÁS CÓDIGO REFERENTE A PERMISOS
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PERMISSION_CODE:{
                if (grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED){
                    openCamera();
                }
                else {
                    Toast.makeText(this, "Permiso denegado...", Toast.LENGTH_SHORT).show();
                }
            }
            case WRITE_EXTERNAL_STORAGE_CODE:{
                if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                }
                else {
                    Toast.makeText(this, "Se necesita permiso para almacenamiento", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    //Código para mostrar la imagen seleccionado en el imageview
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK)
        {
            imageView.setImageURI(image_uri);
            if(requestCode == PICK_FILE_REQUEST)
            {
                if(data != null){
                    selectedFileUri = data.getData();
                    if(selectedFileUri != null && !selectedFileUri.getPath().isEmpty())
                        imageView.setImageURI(selectedFileUri);
                    else
                        Toast.makeText(this, "No se encontró el archivo", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void uploadFile() {
        mText=mInputEdit.getText().toString().trim(); //.trim() quita el espacio antes y despues del texto
        if(selectedFileUri != null) {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMessage("Procesando...");
            dialog.setIndeterminate(false);
            dialog.setMax(100);
            dialog.setCancelable(false);
            dialog.show();

            File file = null;
            try {
                file = new File(Common.getFilePath(this, selectedFileUri));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            if (file != null) {
                ProgressRequestBody requestBody = new ProgressRequestBody(file, this);
                final RequestBody selectorPart = RequestBody.create(MediaType.parse("text/plain"), mText);
                final MultipartBody.Part ImagePart = MultipartBody.Part.createFormData("image", file.getName(), requestBody);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mService.uploadFile(selectorPart,ImagePart)
                                .enqueue(new Callback<String>() {
                                    @Override
                                    public void onResponse(Call<String> call, Response<String> response) {
                                        dialog.dismiss();
                                        String image_processed_link = new StringBuilder("http://192.168.1.72:5000/" +
                                                response.body().replace("\"", "")).toString();
                                        Picasso.get().load(image_processed_link)
                                                .into(imageView);

                                        Toast.makeText(MainActivity.this, "Detectado!!!", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure(Call<String> call, Throwable t) {
                                        dialog.dismiss();
                                        Toast.makeText(MainActivity.this, "" + t.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                }).start();
            }
        }
        else
        {
            Toast.makeText(this, "No se puede subir el archivo :(", Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent,PICK_FILE_REQUEST);
    }

    @Override
    public void onProgressUpdate(int percent) {
    }
}