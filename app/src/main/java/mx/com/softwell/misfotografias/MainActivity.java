package mx.com.softwell.misfotografias;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.app.usage.StorageStatsManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import mx.com.softwell.misfotografias.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final int RC_GALLERY = 21;
    private static final int RC_CAMARA = 22;

    private static final int RP_CAMARA = 121;
    private static final int RP_STORAGE = 122;

    private static final String IMAGE_DIRECTORY = "/MyPhotoApp";
    private static final String MY_PHOTO = "my_photo";

    private static final String PATH_PROFILE = "profile";
    private static final String PATH_PHOTO_URL = "photoUrl";

    private TextView lblMessage;
    private ActivityMainBinding binding;

    private StorageReference storageReference;
    private DatabaseReference databaseReference;

    private String currentPhotoPath;
    private Uri photoSelectedUri;

    private BottomNavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                case R.id.navigation_gallery:
                    lblMessage.setText("Galería");
                    fromGallery();
                    return true;
                case R.id.navigation_camera:
                    lblMessage.setText("Cámara");
                    //fromCamara();
                    dispatchTakePictureIntent();
                    return true;
            }
            return false;
        }
    };



    private Object DiskCacheStrategy;
    private Object StorageReference;
    private Object Glide;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        lblMessage = findViewById(R.id.lblMessage);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

        ConfigFirebase();

        binding.btnUpload.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("WrongConstant")
            @Override
            public void onClick( View view) {
                if(photoSelectedUri == null){
                    Snackbar.make(binding.container, R.string.main_message_url_null, BaseTransientBottomBar.ANIMATION_MODE_FADE);
                    return;
                }
                StorageReference profileReference = storageReference.child(PATH_PROFILE);
                StorageReference photoReference = profileReference.child(MY_PHOTO);
                photoReference.putFile(photoSelectedUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Snackbar.make(binding.container, R.string.main_message_question_upload_success, BaseTransientBottomBar.LENGTH_LONG).show();
                        taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                savedPhotoUrl(task.getResult());
                            }
                        });

                        binding.btnDelete.setVisibility(View.VISIBLE);
                        binding.lblMessage.setText(R.string.main_message_done);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                       Snackbar.make(binding.container, R.string.main_message_upload_error, BaseTransientBottomBar.LENGTH_LONG).show();
                    }
                });
                
            }
        });

        binding.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                storageReference.child(PATH_PROFILE).child(MY_PHOTO).delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                               databaseReference.removeValue();
                               binding.imgPhoto.setImageBitmap(null);
                               binding.btnDelete.setVisibility(View.GONE);
                               Snackbar.make(binding.container, R.string.main_message_delete, BaseTransientBottomBar.LENGTH_LONG).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(binding.container, R.string.main_mesage_error_delete, BaseTransientBottomBar.LENGTH_LONG).show();
                    }
                });
            }
        });
        configPhotoProfile();
    }

    private void savedPhotoUrl(Uri downloadUri) {
        databaseReference.setValue(downloadUri.toString());
    }

    private void ConfigFirebase() {
        StorageReference = FirebaseStorage.getInstance().getReference();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference().child(PATH_PROFILE).child(PATH_PHOTO_URL);
    }

    private void configPhotoProfile() {
        storageReference.child(PATH_PROFILE).child(MY_PHOTO).getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        RequestOptions options = new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE).centerCrop();

                        Glide.with(MainActivity.this)
                                .load(uri)
                                .placeholder(R.drawable.loader)
                                .error(R.drawable.ic_error)
                                .applay(options)
                                .into(binding.imgPhoto);
                        binding.btnDelete.setVisibility(View.VISIBLE);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                binding.btnDelete.setVisibility(View.GONE);
                Snackbar.make(binding.container, R.string.main_message_error_notfount,BaseTransientBottomBar.LENGTH_LONG).show();
            }
        });

        }


    private void fromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, RC_GALLERY);
    }

    private void fromCamara() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, RC_CAMARA);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager())!= null){
            File photoFile;
            photoFile = createImageFile();
            if(photoFile!=null){
                Uri photoUri = FileProvider.getUriForFile(this,
                        "mx.com.softwell.misfotografias", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoUri);
                startActivityForResult(takePictureIntent, RC_CAMARA);
            }
        }
    }

    private File createImageFile() {
        final String timeStamp = new SimpleDateFormat("dd-MM-yyyy_HHmms", Locale.ROOT)
                .format(new Date());
        final String imageFileName = MY_PHOTO + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = null;
        try {
            image = File.createTempFile(imageFileName, "jpg", storageDir);
            currentPhotoPath = image.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    @Override
    protected  void onActivityResult(int requestCode, int resultCode, @Nullable Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            switch (requestCode){
                case RC_GALLERY:
                    if(data!=null){
                        photoSelectedUri = data.getData();
                        try {
                            Bitmap bitmap =MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoSelectedUri);
                            binding.imgPhoto.setImageBitmap(bitmap);
                            binding.btnDelete.setVisibility(View.GONE);
                            binding.lblMessage.setText(R.string.main_message_question_upload);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case RC_CAMARA:
                   // Bundle extras = data.getExtras();
                    //Bitmap bitmap =(Bitmap)extras.get("data");
                    photoSelectedUri = addPicGallery();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),
                                photoSelectedUri);
                        binding.imgPhoto.setImageBitmap(bitmap);
                        binding.btnDelete.setVisibility(View.GONE);
                        binding.lblMessage.setText(R.string.main_message_question_upload);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;

                    }

            }
        }

    private Uri addPicGallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        currentPhotoPath = null;
        return  contentUri;
    }
}
