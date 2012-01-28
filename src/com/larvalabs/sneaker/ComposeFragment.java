package com.larvalabs.sneaker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.Calendar;

/**
 * @author John Watkinson
 */
public class ComposeFragment extends Fragment {

    private static final int SELECT_IMAGE = 1;

    private byte[] image = null;
    private Button attachButton = null;
    private ImageView attachImage = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        image = null;
        View view = inflater.inflate(R.layout.compose_entry, container, false);
        final EditText editText = (EditText) view.findViewById(R.id.compose_entry);
        final View okButton = view.findViewById(R.id.compose_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String text = editText.getText().toString();
                Post post = new Post(text, image, Constants.POINTS_STAR, Status.STARRED, Calendar.getInstance().getTime());
                Database.getDB().addPost(post);
                getActivity().finish();
            }
        });
        final View cancelButton = view.findViewById(R.id.compose_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getFragmentManager().popBackStack();
                getActivity().finish();
            }
        });
        attachButton = (Button) view.findViewById(R.id.compose_attach);
        attachButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Intent intent = new Intent(Intent.ACTION_GET_CONTENT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
                startActivityForResult(intent, SELECT_IMAGE);
            }
        });
        attachImage = (ImageView) view.findViewById(R.id.compose_image);
        attachImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                image = null;
                attachButton.setVisibility(View.VISIBLE);
                attachImage.setVisibility(View.GONE);
            }
        });
        return view;
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
                Bitmap bitmap = Util.decodeBitmap(path, 800);
                image = Util.bitmapToBytes(bitmap);
                attachImage.setImageBitmap(bitmap);
                attachButton.setVisibility(View.GONE);
                attachImage.setVisibility(View.VISIBLE);
            }
        }
    }

    private String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getActivity().managedQuery(uri, projection, null, null, null);
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
}
