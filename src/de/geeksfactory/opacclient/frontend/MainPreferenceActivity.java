package de.geeksfactory.opacclient.frontend;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import com.actionbarsherlock.view.MenuItem;

import de.geeksfactory.opacclient.R;
import de.geeksfactory.opacclient.reminder.ReminderCheckService;
import de.geeksfactory.opacclient.storage.MetaDataSource;
import de.geeksfactory.opacclient.storage.SQLMetaDataSource;

public class MainPreferenceActivity extends OpacPreferenceActivity {
	@Override
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		addPreferencesFromResource(R.xml.settings);

		Preference assistant = findPreference("accounts");
		assistant.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				Intent intent = new Intent(MainPreferenceActivity.this,
						AccountListActivity.class);
				startActivity(intent);
				return false;
			}
		});

		Preference meta = findPreference("meta_clear");
		meta.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference arg0) {
				MetaDataSource data = new SQLMetaDataSource(
						MainPreferenceActivity.this);
				data.open();
				data.clearMeta();
				data.close();
				Intent i = new Intent(MainPreferenceActivity.this,
						ReminderCheckService.class);
				startService(i);
				return false;
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		}
		return super.onOptionsItemSelected(item);
	}
}
