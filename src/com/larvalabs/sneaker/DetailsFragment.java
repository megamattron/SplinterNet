package com.larvalabs.sneaker;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.HashMap;

/**
 * @author John Watkinson
 */
public class DetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, PostStatusListener {

    public static final String ARG_INDEX = "index";
    public static final String ARG_SEARCH = "search";

    private Cursor cursor = null;
    private Button nextButton;
    private Button previousButton;
    private Post post;
    private Button statusButton;

    private HashMap<String, Status> statusMap = new HashMap<String, Status>();

    /**
     * Create a new instance of DetailsFragment, initialized with the id of the data to show.
     */
    public static DetailsFragment newInstance(int index, String search) {
        DetailsFragment f = new DetailsFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();

        args.putInt(ARG_INDEX, index);
        args.putString(ARG_SEARCH, search);

        f.setArguments(args);

        return f;
    }

    public int getPostIndex() {
        return getArguments().getInt(ARG_INDEX);
//        return getActivity().getIntent().getIntExtra(ARG_INDEX, 0);
    }

    public void setPostIndex(int index) {
        getArguments().putInt(ARG_INDEX, index);
    }

    public String getSearch() {
        return getArguments().getString(ARG_SEARCH);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            // We have different layouts, and in one of them this
            // fragment's containing frame doesn't exist.  The fragment
            // may still be created from its saved state, but there is
            // no reason to try to create its view hierarchy because it
            // won't be displayed.  Note this is not needed -- we could
            // just run the code below, where we would create and return
            // the view hierarchy; it would just never be used.
            return null;
        }
        View view = inflater.inflate(R.layout.detail_layout, container, false);
        // Enable buttons
        final Button panicButton = (Button) view.findViewById(R.id.detail_panic);
        panicButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Util.handlePanic(getActivity());
            }
        });
        final Button listButton = (Button) view.findViewById(R.id.detail_list);
        listButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        statusButton = (Button) view.findViewById(R.id.detail_status);
        statusButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (post != null) {
                    String key = post.getKey();
                    Status status = statusMap.get(post.getKey());
                    if (status == null) {
                        status = post.getStatus();
                    }
                    final AlertDialog dialog = PostStatusDialog.createPostStatusDialog(getActivity(), key, status.ordinal(), DetailsFragment.this);
                    dialog.show();
                }
            }
        });
        previousButton = (Button) view.findViewById(R.id.detail_previous);
        previousButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int index = getPostIndex();
                if (index > 0) {
                    index--;
                    setPostIndex(index);
                    loadPost();
                }
            }
        });
        nextButton = (Button) view.findViewById(R.id.detail_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int index = getPostIndex();
                setPostIndex(index + 1);
                loadPost();
            }
        });
        return view;
    }

    public void statusChanged(String key, Status status) {
        Util.setChangeFlag(getActivity());
        statusMap.put(key, status);
        Database.getDB().setPostStatus(key, status);
        if (status == Status.DELETED) {
            getActivity().finish();
        } else {
            loadPost();
        }
    }

    private void populateView(View view, Post post) {
        final ImageView imageView = (ImageView) view.findViewById(R.id.detail_image);
        final TextView dateView = (TextView) view.findViewById(R.id.detail_date);
        final TextView textView = (TextView) view.findViewById(R.id.detail_text);
        if (post.getImageData() == null) {
            imageView.setVisibility(View.GONE);
        } else {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(Util.bytesToBitmap(post.getImageData()));
            imageView.setAdjustViewBounds(true);
        }
        String dateText = Util.formatDate(post.getDate());
        dateView.setText(dateText);
        textView.setText(post.getText());
        Status status = statusMap.get(post.getKey());
        if (status == null) {
            status = post.getStatus();
        }
        if (status == Status.STARRED) {
            statusButton.setBackgroundResource(R.drawable.status_button);
        } else {
            statusButton.setBackgroundResource(R.drawable.status_circle_button);
        }
    }

    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return Util.createContentLoader(getSearch(), getActivity(), true);
    }

    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        this.cursor = cursor;
        statusMap = new HashMap<String, Status>();
        loadPost();
    }

    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        cursor = null;
    }

    private void loadPost() {
        if (cursor != null) {
            int index = getPostIndex();
            Util.debug("Post index: " + index);
            cursor.moveToPosition(index);
            if (cursor.isAfterLast()) {
                setPostIndex(index - 1);
            } else {
                post = Database.getPostFromCursor(cursor);
                if (post != null) {
                    View view = getView();
                    populateView(view, post);
                }
                if (cursor.isLast()) {
                    nextButton.setBackgroundResource(R.drawable.next_off_but);
                } else {
                    nextButton.setBackgroundResource(R.drawable.next_but);

                }
                if (cursor.isFirst()) {
                    previousButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.previous_off_but));
                } else {
                    previousButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.previous_but));
                }
            }
        }
    }
}
