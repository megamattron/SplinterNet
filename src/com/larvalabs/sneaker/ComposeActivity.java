package com.larvalabs.sneaker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.*;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * @author John Watkinson
 */
public class ComposeActivity extends Activity {

    private static final int SELECT_IMAGE = 1;
    private static final int CAPTURE_IMAGE = 2;

    private static final String SAVE_IMAGE = "image";

    private byte[] image = null;
    private Button attachButton = null;
    private ImageView attachImage = null;
    private View imageParent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compose_layout);
        final EditText editText = (EditText) findViewById(R.id.compose_entry);
        final Button panicButton = (Button) findViewById(R.id.compose_panic);
        panicButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Util.handlePanic(ComposeActivity.this);
            }
        });
        final View okButton = findViewById(R.id.compose_done);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String text = editText.getText().toString();
                Post post = new Post(text, image, 100, Status.STARRED, Calendar.getInstance().getTime());
                Database.getDB().addPost(post);
                Util.setChangeFlag(ComposeActivity.this);
                finish();
            }
        });
        final View cancelButton = findViewById(R.id.compose_delete);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ComposeActivity.this);
                AlertDialog dialog = builder.setMessage(R.string.abort_compose)
                        .setCancelable(false)
                        .setPositiveButton(R.string.abort_yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).setNegativeButton(R.string.remove_image_no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).create();
                dialog.show();
            }
        });
        attachButton = (Button) findViewById(R.id.compose_camera);
        registerForContextMenu(attachButton);
        attachButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                attachButton.showContextMenu();
                //Intent intent = new Intent(Intent.ACTION_GET_CONTENT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            }
        });
        imageParent = findViewById(R.id.compose_photo_container);
        attachImage = (ImageView) findViewById(R.id.compose_photo);
        attachImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.dialog_remove_image, null, false);
                ImageView imageView = (ImageView) layout.findViewById(R.id.image);
                imageView.setImageBitmap(Util.bytesToBitmap(image));
                AlertDialog.Builder builder = new AlertDialog.Builder(ComposeActivity.this);
                builder.setView(layout);
                builder.setPositiveButton(R.string.remove_image_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        image = null;
                        attachButton.setVisibility(View.VISIBLE);
                        imageParent.setVisibility(View.GONE);
                        dialog.cancel();
                    }
                });
                builder.setNegativeButton(R.string.remove_image_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        // Hack to work around the 100-year bug in Android where it won't show the soft keyboard at appropriate time.
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        // Set image if it is there
        if (savedInstanceState != null) {
            image = savedInstanceState.getByteArray(SAVE_IMAGE);
            if (image != null) {
                setImageData(Util.bytesToBitmap(image));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (image != null) {
            outState.putByteArray(SAVE_IMAGE, image);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_IMAGE) {
                Uri selectedImageUri = data.getData();

                //OI FILE Manager
                String fileManagerString = selectedImageUri.getPath();

                //MEDIA GALLERY
                String selectedImagePath = getPath(selectedImageUri);
                
                String path;
                if (selectedImagePath != null) {
                    path = selectedImagePath;
                } else {
                    path = fileManagerString;
                }
                Util.debug("Imagepath: '" + path + "'.");
                
                String scrubPath = scrubExif(path);
                if (path.equals(scrubPath)){
                Util.error("Warning: was not able to scrub", null);
                }
                
                Bitmap bitmap = Util.decodeBitmap(path, 800);
                setImageData(bitmap);
            } else if (requestCode == CAPTURE_IMAGE) {
            	//Not using i.getData() as it crashes on some devices for picture taking. 

            	Bitmap bitmap = (Bitmap) data.getExtras().get("data"); 
            	
            	
            	// TODO Saving directly as bitmap doesn't set exif data, code left just in case.
/*
            	String path = Environment.getExternalStorageDirectory().toString();
            	OutputStream fOut = null;
            	File file = new File(path, "TEMP_IMAGE.jpg");
            	
            	path = file.getAbsolutePath();				// Set path as new image
            	
            	try {
					fOut = new FileOutputStream(file);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

            	bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fOut); //Save image to path
            	
            	try {
					fOut.flush();
					fOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

            	try {
					MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	
            	                
                String scrubPath = scrubExif(path);
                
                if (path.equals(scrubPath)){
                Util.error("Warning: was not able to scrub", null);
                }
//              Bitmap bitmap = Util.decodeBitmap(path, 800);
                

            	
                String scrubPath = scrubExif(path);		//Scrub exif
                
                if (path.equals(scrubPath)){
                Util.error("Warning: was not able to scrub", null);
                }
                else
*/
                	setImageData(bitmap);
                
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.camera_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.take_photo:
            	
                Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, CAPTURE_IMAGE);
                
                return true;
            case R.id.choose_photo:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
                startActivityForResult(intent, SELECT_IMAGE);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void setImageData(Bitmap bitmap) {
        image = Util.bitmapToBytes(bitmap);
        attachImage.setImageBitmap(bitmap);
        attachButton.setVisibility(View.GONE);
        imageParent.setVisibility(View.VISIBLE);
    }

    private String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            // THIS CAN HAPPEN IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else {
            return null;
        }
    }
    
    private String scrubExif(String path){
        // TODO Gracefully handle .jpg, .jpeg, .jpe .jif, .jfif, .jfi
    	String originalpath = path;
    	String scrubbedPath = null;
        if (path.contains(".jpg")){
        	scrubbedPath = ExifHandler.scrubExif(path);
        }
        
        if (scrubbedPath != null){
        	path = scrubbedPath;
        }
        try {
        	//TODO Remove Test for Exif, need to show user exif data 
            ExifInterface e = ExifHandler.getExif(originalpath);
			ExifInterface a = ExifHandler.getExif(scrubbedPath);	
			
			Util.error("Original File", null);
			Util.error(ExifHandler.printExif(e), null);
			Util.error("Scrubbed file", null);
			Util.error(ExifHandler.printExif(a), null);
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        return path;
    }

}
