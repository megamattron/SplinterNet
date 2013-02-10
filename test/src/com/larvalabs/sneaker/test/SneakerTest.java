/*
 * This is an example test project created in Eclipse to test NotePad which is a sample 
 * project located in AndroidSDK/samples/android-11/NotePad
 * 
 * 
 * You can run these test cases either on the emulator or on device. Right click
 * the test project and select Run As --> Run As Android JUnit Test
 * 
 * @author Renas Reda, renas.reda@jayway.com
 * 
 */

package com.larvalabs.sneaker.test;

import android.nfc.Tag;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.jayway.android.robotium.solo.Solo;
import com.larvalabs.sneaker.*;
import com.larvalabs.sneaker.R;


public class SneakerTest extends ActivityInstrumentationTestCase2<Sneaker>{

	private Solo solo;
    private static final String TAG = "SplinterNetTests";

    public SneakerTest() {
		super(Sneaker.class);

	}
	
	@Override
	public void setUp() throws Exception {
		//setUp() is run before a test case is started. 
		//This is where the solo object is created.
		solo = new Solo(getInstrumentation(), getActivity());
        Log.d(TAG, "Resetting database.");
        solo.clickOnView(solo.getView(R.id.main_panic_button));
        solo.clickOnButton("Yes, Delete");
	}
	
	@Override
	public void tearDown() throws Exception {
		//tearDown() is run after a test case has finished. 
		//finishOpenedActivities() will finish all the activities that have been opened during the test execution.
		solo.finishOpenedActivities();
	}


	public void testAddTextEntry() throws Exception {
        String testEntryText1 = "Here is a test entry 1.";
        String testEntryText2 = "Here is a test entry 2.";
        {
            solo.clickOnView(solo.getView(R.id.main_compose_button));
            solo.enterText(0, testEntryText1);
            solo.clickOnView(solo.getView(R.id.compose_done));
            assertTrue(solo.searchText(testEntryText1));
        }
        {
            solo.clickOnView(solo.getView(R.id.main_compose_button));
            solo.enterText(0, testEntryText2);
            solo.clickOnView(solo.getView(R.id.compose_done));
            assertTrue(solo.searchText(testEntryText1));
            assertTrue(solo.searchText(testEntryText2));
        }

	}

}
