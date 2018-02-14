package com.antonio.camera;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.greysonparrelli.permiso.Permiso;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private ImageView imagen;
    private Button btn;
    private final int TRAER_DE_GALERÍA = 10;
    private final int TRAER_DE_CAMARA = 20;

    private final String HACER_FOTOGRAFÍA = "Hacer fotografía";
    private final String CARGAR_IMAGEN_GALERIA = "Cargar imagen de galería";
    private final String CANCELAR = "Cancelar";

    private final String NUEVA_CARPETA_RAIZ="misImagenesAppCamera/";
    private final String RUTA_IMAGEN=NUEVA_CARPETA_RAIZ+"misFotos";
    private String path;
    private boolean permisoCamare=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inicializarControles();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gestionarImagen();
            }
        });

        //GESTIONAMOS LOS PERMISOS CON LA LIBRERÍA PERMISOS.... https://github.com/greysonp/permiso
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Permiso.getInstance().setActivity(this);
            solicitarPermisos();
        }
    }

    private void inicializarControles() {
        //Se quitan los Casting....y se hace otro commit
        imagen = findViewById(R.id.imgImagen);
        btn =  findViewById(R.id.btnImagen);
    }

    private void gestionarImagen() {

        //solicitarPermisos();
        //Gestionamos las opciones creando un array....
        //final CharSequence[] opciones = {"Hacer fotografía", "Cargar imagen de galería", "Cancelar"};
        final CharSequence[] opciones = {HACER_FOTOGRAFÍA, CARGAR_IMAGEN_GALERIA, CANCELAR};

        //En el AlertDialog incluimos las opciones...
        final AlertDialog.Builder alertOpciones = new AlertDialog.Builder(MainActivity.this);
        alertOpciones.setTitle(R.string.elegir_opcion);
        alertOpciones.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (opciones[i].equals(CARGAR_IMAGEN_GALERIA)) {

                    // 1- Traer las imágenes de la Galería o de otros directorios :Intent.ACTION_PICK-----ACTION_GET_CONTENT--Slo carga desde la app Photos
                    /*Intent intent = new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/");
                    startActivityForResult(intent.createChooser(intent, "Selecciona una aplicación para realizar la acción"), TRAER_DE_GALERÍA);*/

                   //Trae solo imágenes de la Galeria;
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, TRAER_DE_GALERÍA);



                }else if (opciones[i].equals(HACER_FOTOGRAFÍA)){

                    hacerFotografia();

                }else dialogInterface.dismiss();

            }
        });

        alertOpciones.show();


    }

    private void hacerFotografia() {

        if (permisoCamare) {


        File fileImagen = new File(Environment.getExternalStorageDirectory(), RUTA_IMAGEN);
        boolean isCreada = fileImagen.exists();
        String nombreImagen = "";
        if (!isCreada) {
            isCreada = fileImagen.mkdirs();
        }

        if (isCreada) {
            nombreImagen = (System.currentTimeMillis() / 1000) + ".jpg";
        }

        //Ruta de almacenamiento
        path = Environment.getExternalStorageDirectory() +
                File.separator + RUTA_IMAGEN + File.separator + nombreImagen;

        File imagen = new File(path);


        /*Si targetSdkVersion es 24 o superior*/
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String authorities = getApplicationContext().getPackageName() + ".provider";
            Uri imageUri = FileProvider.getUriForFile(this, authorities, imagen);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        }
        //ANTES DE ANDROID 7
        else {
            //intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(fileImagen));
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imagen));
        }

        startActivityForResult(intent, TRAER_DE_CAMARA);
    }else {
            Toast.makeText(this, "Verifique los permisos de la app.", Toast.LENGTH_SHORT).show();
            solicitarPermisosManualmente();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            switch (requestCode) {

                case TRAER_DE_GALERÍA:

                    Uri miPath = data.getData();
                    imagen.setImageURI(miPath);
                    break;

                case TRAER_DE_CAMARA:
                    //Permitimos que la foto se guarde en la galería...
                    MediaScannerConnection.scanFile(this, new String[]{path}, null,
                            new MediaScannerConnection.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.i("Ruta de almacenamiento","Path: "+path);
                                }
                            });

                    Bitmap bitmap= BitmapFactory.decodeFile(path);
                    imagen.setImageBitmap(bitmap);

                    break;

            }


        }
    }


    private void solicitarPermisos() {


        Permiso.getInstance().
                requestPermissions(new Permiso.IOnPermissionResult() {
                                       @Override
                                       public void onPermissionResult(Permiso.ResultSet resultSet) {
                                           //PERMISO PARA LEER/ESCRIBIR EN EXTERNAL STORAGE
                                           if (resultSet.isPermissionGranted(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                               //Toast.makeText(Activity_chats.this, "Permiso concedido", Toast.LENGTH_SHORT).show();
                                               btn.setEnabled(true);
                                               File carpetaImagenes = new File(Environment.getExternalStorageDirectory(), RUTA_IMAGEN);
                                               if(!carpetaImagenes.exists()){
                                                   carpetaImagenes.mkdirs();
                                               }

                                           } else if (resultSet.isPermissionPermanentlyDenied(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                               Toast.makeText(MainActivity.this, "Se ha denegado permanentemente el permiso para escribir en la tarjeta SD", Toast.LENGTH_SHORT).show();
                                               btn.setEnabled(false);
                                           } else {
                                               Toast.makeText(MainActivity.this, "Permiso denegado para el acceso a los archivos de imagen", Toast.LENGTH_SHORT).show();
                                              // btnAdjuntar.setEnabled(false);
                                               //solicitarPermisosManualmente();
                                           }

                                           //PERMISO PARA EL MANEJO DE LA CÁMARA
                                           if (resultSet.isPermissionGranted(Manifest.permission.CAMERA)) {
                                               permisoCamare=true;
                                               File carpetaFotos = new File(Environment.getExternalStorageDirectory(), RUTA_IMAGEN);
                                               if(!carpetaFotos.exists()){
                                                   carpetaFotos.mkdirs();
                                               }

                                           } else if (resultSet.isPermissionPermanentlyDenied(Manifest.permission.CAMERA)) {
                                               Toast.makeText(MainActivity.this, "Se ha denegado permanentemente el permiso para manejar la cámara", Toast.LENGTH_SHORT).show();
                                               //btnHacerFoto.setEnabled(false);
                                               solicitarPermisosManualmente();
                                           } else {
                                               Toast.makeText(MainActivity.this, "Permiso denegado para el manejo de la cámara", Toast.LENGTH_SHORT).show();
                                              // btnHacerFoto.setEnabled(false);
                                           }


                                       }

                                       @Override
                                       public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                                           Permiso.getInstance().showRationaleInDialog("Los permisos están desactivados", "Debes aceptar los permisos solicitados para el correcto funcionamiento de la App", null, callback);
                                       }
                                   },
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

         Permiso.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults);

    }



    private void solicitarPermisosManualmente() {

        final CharSequence[] opciones = {"Si", "No"};
        final android.support.v7.app.AlertDialog.Builder alertOpciones = new android.support.v7.app.AlertDialog.Builder(MainActivity.this);
        alertOpciones.setTitle("¿Desea configurar los permisos de forma manual?");
        alertOpciones.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (opciones[i].equals("Si")) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "Los permisos no fueron aceptados. La aplicación no funcionará correctamente.", Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();
                }
            }
        });
        alertOpciones.show();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Permiso.getInstance().setActivity(this);
    }
}
