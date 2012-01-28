package com.larvalabs.sneaker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.*;
import android.os.Process;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * @author John Watkinson
 */
public class SummaryFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>, PostStatusListener {

    public static final int ACTIVITY_REQUEST_COMPOSE = 1;
    public static final int ACTIVITY_REQUEST_DETAIL = 2;
    public static final int ACTIVITY_REQUEST_SYNC = 3;

    private int curCheckPosition = -1;
    private CursorAdapter adapter;
    private String search;
    private Button searchButton;

    private HandlerThread decoderThread;
    private Handler decoderHandler;
    private HashMap<ImageView, Integer> imagesToLoad = new HashMap<ImageView, Integer>();
    private boolean isScrolling = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        decoderThread = new HandlerThread("Bitmap Decoder Thread", Thread.NORM_PRIORITY-1);
        decoderThread.start();
        decoderHandler = new Handler(decoderThread.getLooper());

//        android.os.Process.setThreadPriority(decoderThread.getThreadId(), Process.THREAD_PRIORITY_BACKGROUND);

        String[] columns = {Database.COL_TEXT};
        int[] to = {R.id.text_entry};
//        setEmptyText(getString(R.string.summary_loading));
        setEmptyText(getString(R.string.summary_no_posts));
        final int maxImageWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getActivity().getResources().getDisplayMetrics());
        final int maxImageHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 360, getActivity().getResources().getDisplayMetrics());
        final int dividerHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getActivity().getResources().getDisplayMetrics());
        final int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getActivity().getResources().getDisplayMetrics());

        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_FLING || scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    isScrolling = true;
                    Util.debug("IMAGE: Scrolling started.");
                } else if (scrollState == SCROLL_STATE_IDLE) {
                    Util.debug("IMAGE: Scrolling stopped.");
                    isScrolling = false;
                    fillImageViews();
                }
            }

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//                Util.debug("*** On scroll");
            }
        });

        getListView().setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (isScrolling) {
                        Util.debug("IMAGE: Scrolling aborted!");
                        isScrolling = false;
                        fillImageViews();
                    }
                }
                return false;
            }
        });

//        adapter = new SimpleCursorAdapter(getActivity(), R.layout.summary_entry, null, columns, to, 0);
        adapter = new CursorAdapter(getActivity(), null) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                View view = getActivity().getLayoutInflater().inflate(R.layout.main_entry, parent, false);
                bindView(view, context, cursor);
                return view;
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                // todo - Use "View Holder" approach, which is an insane manual way of avoiding these slow findViewById calls:
                // http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/view/List14.html
                ImageView statusButton = (ImageView) view.findViewById(R.id.main_entry_status);
                final ImageView image = (ImageView) view.findViewById(R.id.main_entry_image);
                TextView text = (TextView) view.findViewById(R.id.main_entry_text);
                TextView date = (TextView) view.findViewById(R.id.main_entry_date);
                int hasImage = cursor.getInt(cursor.getColumnIndex(Database.COL_HAS_IMAGE));
                int itemId = cursor.getInt(cursor.getColumnIndex(Database.COL_ID));
                final int status = cursor.getInt(cursor.getColumnIndex(Database.COL_STATUS));
                final int score = cursor.getInt(cursor.getColumnIndex(Database.COL_SCORE));
                if (status == Status.STARRED.ordinal()) {
                    statusButton.setBackgroundResource(R.drawable.status_button_star);
                } else {
                    statusButton.setBackgroundResource(Constants.getPointResource(score));
                }
                final String key = cursor.getString(cursor.getColumnIndex(Database.COL_KEY));
                statusButton.setTag(key);
                statusButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Util.debug("STATUS BUTTON CLICKED: " + v.getTag());
                        final AlertDialog dialog = PostStatusDialog.createPostStatusDialog(getActivity(), key, status, SummaryFragment.this);
                        dialog.show();
                    }
                });
//                final LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) image.getLayoutParams();
                if (hasImage > 0) {
                    if (isScrolling) {
                        Util.debug("IMAGE: queueing " + cursor.getPosition());
                        image.setImageBitmap(null);
                        image.setBackgroundResource(R.drawable.gray_border);
                        image.setVisibility(View.VISIBLE);
                        imagesToLoad.put(image, itemId);
                    } else {
                        Util.debug("IMAGE: directly loading " + cursor.getPosition());
                        final byte[] imageData = Database.getDB().getImageData(itemId);
                        loadImageDirectly(image, imageData);
                    }
                } else {
                    image.setVisibility(View.GONE);
                    image.setImageBitmap(null);
                }
                String entryText = cursor.getString(cursor.getColumnIndex(Database.COL_TEXT));
                text.setText(entryText);
                text.setMaxLines(3);
                long millis = cursor.getLong(cursor.getColumnIndex(Database.COL_DATE));
                Date dateObj = new Date(millis);
                String dateText = Util.formatDate(dateObj);
                date.setText(dateText);
            }

        };
        getListView().addHeaderView(new View(getActivity()), null, false);
        getListView().addFooterView(new View(getActivity()), null, false);
        setListAdapter(adapter);
        getListView().setFooterDividersEnabled(true);
        getListView().setHeaderDividersEnabled(true);
        getListView().setCacheColorHint(0xFFD0D0D0);
        getListView().setVerticalFadingEdgeEnabled(false);
        getListView().setDivider(null);
        getLoaderManager().initLoader(0, null, this);
        // Wire up buttons
        final View panicButton = getActivity().findViewById(R.id.main_panic_button);
        panicButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Util.handlePanic(getActivity());
            }
        });
        final View composeButton = getActivity().findViewById(R.id.main_compose_button);
        composeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(getActivity(), ComposeActivity.class);
                startActivityForResult(intent, ACTIVITY_REQUEST_COMPOSE);
            }
        });
        final View syncButton = getActivity().findViewById(R.id.main_sync_button);
        syncButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(getActivity(), SyncActivity.class);
                startActivityForResult(intent, ACTIVITY_REQUEST_SYNC);
            }
        });
        searchButton = (Button) getActivity().findViewById(R.id.search_button);
        final EditText searchText = (EditText) getActivity().findViewById(R.id.search_text);
        searchText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                String text = s.toString();
                doSearch(text);
                setSearchButtonDrawable(text);
            }
        });
//                text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                        if (actionId == 1000) {
//                            final View next = text.focusSearch(View.FOCUS_FORWARD);
//                            if (next != null) {
//                                next.requestFocus();
//                            }
//                            return true;
//                        }
//                        return false;
//                    }
//                });
        searchText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (search == null || search.length() == 0) {
                    searchText.requestFocus();
                } else {
                    searchText.setText(null);
                    setSearchButtonDrawable(null);
                }
            }
        });

        if (savedInstanceState != null) {
            // Restore last state for checked position.
            curCheckPosition = savedInstanceState.getInt("curChoice", 0);
        }
        ((Sneaker) getActivity()).setFragment(this);
    }

    private void fillImageViews() {
        final int startIndex = getListView().getFirstVisiblePosition();
        final int endIndex = getListView().getLastVisiblePosition();
        

        ArrayList<ImageView> imageViews = new ArrayList<ImageView>(imagesToLoad.keySet());
        Util.debug("*** Loading images for " + imageViews.size() + " views");
        for (final ImageView image : imageViews) {
            final int id = imagesToLoad.get(image);
            decoderHandler.post(new Runnable() {
                public void run() {
                    final byte[] imageData = Database.getDB().getImageData(id);
                    loadImageDirectly(image, imageData);
                }
            });
            imagesToLoad.remove(image);
        }
        Util.debug("*** Now " + imagesToLoad.size() + " images in loader queue.");
    }

    private void loadImageDirectly(final ImageView image, byte[] imageData) {
        int width = image.getWidth();
        if (width == 0) {
            // Just a big default width to use when doing loading before layout
            width = 400;
        }
        final Bitmap bitmap = Util.bytesToBitmapWithSampling(imageData, width);
        image.post(new Runnable() {
            public void run() {
                image.setImageBitmap(bitmap);
//                                        image.setAdjustViewBounds(true);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setBackgroundResource(R.drawable.gray_border);
//                                        layoutParams.setMargins(0, 0, padding, 0);
            }
        });
    }

    private void setSearchButtonDrawable(String text) {
        if (text == null || text.length() == 0) {
            searchButton.setBackgroundResource(R.drawable.search_button);
        } else {
            searchButton.setBackgroundResource(R.drawable.search_clear_button);
        }
    }

    public void reload() {
        getLoaderManager().restartLoader(0, null, SummaryFragment.this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Util.debug("ACTIVITY RESULT");
        if (Util.getAndClearChangeFlag(getActivity())) {
            Util.debug("--> RELOAD");
            getLoaderManager().restartLoader(0, null, this);
        }
//        if (requestCode == ACTIVITY_REQUEST_COMPOSE) {
//            Util.debug("--> COMPOSE");
//            getLoaderManager().restartLoader(0, null, this);
//        } else if (requestCode == ACTIVITY_REQUEST_DETAIL) {
//            Util.debug("--> DETAILS");
//            getLoaderManager().restartLoader(0, null, this);
//        } else if (requestCode == ACTIVITY_REQUEST_SYNC) {
//            Util.debug("--> SYNC");
//            getLoaderManager().restartLoader(0, null, this);
//        }
    }

    public void statusChanged(String key, Status status) {
        Database.getDB().setPostStatus(key, status);
        // Just reload everything for now
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Menu turned off
        //inflater.inflate(R.menu.summary_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Assuming "Compose"
        if (item.getItemId() == R.id.menu_compose) {
            Intent intent = new Intent();
            intent.setClass(getActivity(), ComposeActivity.class);
            startActivityForResult(intent, ACTIVITY_REQUEST_COMPOSE);
            return true;
        } else if (item.getItemId() == R.id.menu_sync) {
            Intent intent = new Intent();
            intent.setClass(getActivity(), BluetoothSync.class);
            startActivityForResult(intent, ACTIVITY_REQUEST_SYNC);
            return true;
        } else if (item.getItemId() == R.id.menu_clear) {
            Database.getDB().deleteAll();
            Toast.makeText(getActivity(), "Deleted Everything", Toast.LENGTH_SHORT).show();
            getLoaderManager().restartLoader(0, null, this);
        }
        return false;
    }

    public void doSearch(String text) {
        this.search = text;
        Util.debug(" --- Searching: '" + text + "'.");
        getLoaderManager().restartLoader(0, null, this);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        // todo - revisit what the ideal ordering would be.
        return Util.createContentLoader(search, getActivity(), false);
    }

    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor.isAfterLast()) {
            //setEmptyText(getString(R.string.summary_no_posts));
        }
        adapter.changeCursor(cursor);
    }

    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (!getActivity().isFinishing()) {
            setEmptyText(getString(R.string.summary_no_posts));
//            setEmptyText(getString(R.string.summary_loading));
        }
        adapter.changeCursor(null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curChoice", curCheckPosition);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // The header is insanely the first position in the list.
        showDetails(position - 1);
    }

    /**
     * Helper function to show the details of a selected item, either by
     * displaying a fragment in-place in the current UI, or starting a
     * whole new activity in which it is displayed.
     */
    void showDetails(int index) {
        curCheckPosition = index;
        Util.debug("At index: " + index);
        long id = adapter.getItemId(index);
        Util.debug("Item touched: " + id);
        // Otherwise we need to launch a new activity to display
        // the dialog fragment with selected text.
        Intent intent = new Intent();
        intent.setClass(getActivity(), DetailsActivity.class);
        intent.putExtra(DetailsFragment.ARG_INDEX, index);
        intent.putExtra(DetailsFragment.ARG_SEARCH, search);
        startActivityForResult(intent, ACTIVITY_REQUEST_DETAIL);
    }
}
