package com.gorups.grocery;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.gorups.db.TodoTable;
import com.gorups.db.ItemMasterTable;
import com.gorups.grocery.R;
import com.gorups.grocery.content.MyTodoContentProvider;
import com.gorups.grocery.content.MasterList_CP;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import android.view.View.OnClickListener;
import android.widget.TextView;

/*
 * TodoDetailActivity allows to enter a new todo item
 * or to change an existing
 */
public class TodoDetailActivity extends Activity implements OnClickListener {
	private Spinner mCategory;
	private EditText mTitleText;
	private EditText mBodyText;
	private Uri todoUri;
	private Uri todoUri_ML;
	private byte is_save;
//	private Button scanBtn;
	
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		is_save = 0;
		setContentView(R.layout.todo_edit);
		mCategory = (Spinner) findViewById(R.id.category);
		mTitleText = (EditText) findViewById(R.id.todo_edit_summary);
		mBodyText = (EditText) findViewById(R.id.todo_edit_description);
		Button confirmButton = (Button) findViewById(R.id.todo_edit_button);
		Button cancelButton = (Button) findViewById(R.id.todo_cancel_button);
		Button scanBtn = (Button) findViewById(R.id.btnScan);
		// listen for clicks
		scanBtn.setOnClickListener(this);

		// check from the saved Instance
		todoUri = (bundle == null) ? null : (Uri) bundle
				.getParcelable(MyTodoContentProvider.CONTENT_ITEM_TYPE);
		Bundle extras = getIntent().getExtras();
		todoUri_ML = (bundle == null) ? null : (Uri) bundle
				.getParcelable(MasterList_CP.CONTENT_ITEM_TYPE);
		
		// Or passed from the other activity
		if (extras != null) {
			todoUri = extras
					.getParcelable(MyTodoContentProvider.CONTENT_ITEM_TYPE);
			todoUri_ML = extras
					.getParcelable(MasterList_CP.CONTENT_ITEM_TYPE);
			
			fillData(todoUri);
		}

		confirmButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				if (TextUtils.isEmpty(mTitleText.getText().toString())) {
					makeToast();
				} else {
					is_save = 1;
					setResult(RESULT_OK);
					finish();
				}
			}
		});

		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {

				is_save = 0;
				setResult(RESULT_OK);
				finish();

			}
		});

	}

	public void onClick(View v) {
		// check for scan button
		if (v.getId() == R.id.btnScan) {
			// instantiate ZXing integration class
			IntentIntegrator scanIntegrator = new IntentIntegrator(this);
			// start scanning
			scanIntegrator.initiateScan();
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// retrieve result of scanning - instantiate ZXing object
		IntentResult scanningResult = IntentIntegrator.parseActivityResult(
				requestCode, resultCode, intent);
		// check we have a valid result
		if (scanningResult != null) {
			// get content from Intent Result
			String scanContent = scanningResult.getContents();
			// get format name of data scanned
			String scanFormat = scanningResult.getFormatName();
			// output to UI
			// formatTxt.setText("FORMAT: "+scanFormat);
			mTitleText.setText("" + scanContent);
		} else {
			// invalid scan data or scan canceled
			Toast toast = Toast.makeText(getApplicationContext(),
					"No scan data received!", Toast.LENGTH_SHORT);
			toast.show();
		}
	}

	private void fillData(Uri uri) {
		String[] projection = { TodoTable.COLUMN_SUMMARY,
				TodoTable.COLUMN_DESCRIPTION, TodoTable.COLUMN_CATEGORY };
		Cursor cursor = getContentResolver().query(uri, projection, null, null,
				null);
		String[] projection_ML = { ItemMasterTable.COLUMN_CODE,
				ItemMasterTable.COLUMN_DESCRIPTION, ItemMasterTable.COLUMN_CATEGORY };
		Cursor cursor2 = getContentResolver().query(uri, projection_ML, null, null,
				null);
		
		if (cursor != null) {
			cursor.moveToFirst();
			String category = cursor.getString(cursor
					.getColumnIndexOrThrow(TodoTable.COLUMN_CATEGORY));
			for (int i = 0; i < mCategory.getCount(); i++) {
				String s = (String) mCategory.getItemAtPosition(i);
				if (s.equalsIgnoreCase(category)) {
					mCategory.setSelection(i);
				}
			}
			mTitleText.setText(cursor.getString(cursor
					.getColumnIndexOrThrow(TodoTable.COLUMN_SUMMARY)));
			mBodyText.setText(cursor.getString(cursor
					.getColumnIndexOrThrow(TodoTable.COLUMN_DESCRIPTION)));
			// always close the cursor
			cursor.close();
		}
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState();
		outState.putParcelable(MyTodoContentProvider.CONTENT_ITEM_TYPE, todoUri);
		outState.putParcelable(MasterList_CP.CONTENT_ITEM_TYPE, todoUri_ML);
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveState();
	}

	private void saveState() {
		if (is_save == 1)

		{
			String category = (String) mCategory.getSelectedItem();
			String summary = mTitleText.getText().toString();
			String description = mBodyText.getText().toString();
			// only save if either summary or description
			// is available
			if (description.length() == 0 && summary.length() == 0) {
				return;
			}
			// Grocery list
			ContentValues values = new ContentValues();
			values.put(TodoTable.COLUMN_CATEGORY, category);
			values.put(TodoTable.COLUMN_SUMMARY, summary);
			values.put(TodoTable.COLUMN_DESCRIPTION, description);
			if (todoUri == null) {
				// New todo
				todoUri = getContentResolver().insert(
						MyTodoContentProvider.CONTENT_URI, values);
			} else {
				// Update todo
				getContentResolver().update(todoUri, values, null, null);
			}
			// Item Master List
			ContentValues values_mas = new ContentValues();
			values_mas.put(ItemMasterTable.COLUMN_CODE, summary);
			//values_mas.put(ItemMasterTable.COLUMN_CATEGORY, );
			values_mas.put(ItemMasterTable.COLUMN_DESCRIPTION, description);
			if (todoUri_ML == null) {
				// New todo
				todoUri_ML = getContentResolver().insert(
						MasterList_CP.CONTENT_URI, values_mas);
			} else {
				// Update todo
				getContentResolver().update(todoUri_ML, values_mas, null, null);
			}
			
		}
	}

	private void makeToast() {
		Toast.makeText(TodoDetailActivity.this, "Please maintain a summary",
				Toast.LENGTH_LONG).show();
	}
}
