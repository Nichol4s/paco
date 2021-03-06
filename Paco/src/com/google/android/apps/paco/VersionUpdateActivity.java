package com.google.android.apps.paco;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class VersionUpdateActivity extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.updates_check);
      if (VersionChecker.checkForUpdate(this)) {
        Toast.makeText(this, "Update Found", Toast.LENGTH_SHORT).show();
        Button installButton = (Button) findViewById(R.id.InstallButton);
        installButton.setVisibility(android.view.View.VISIBLE);
        installButton.setOnClickListener(new OnClickListener() {
          public void onClick(View arg0) {
            update();
          }
        });
      } else {
        Toast.makeText(this, "This version is current.", Toast.LENGTH_SHORT).show();
        finish();
      }
    }

    private void update() {
      String url = "https://" + new UserPreferences(this).getServerAddress() + "/paco.apk";
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse(url));
      startActivity(i);
      finish();
    }

  }
