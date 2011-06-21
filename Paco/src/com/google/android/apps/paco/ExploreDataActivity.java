/*
* Copyright 2011 Google Inc. All Rights Reserved.
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance  with the License.  
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/


package com.google.android.apps.paco;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;


/**
 *
 */
public class ExploreDataActivity extends Activity {

  private static final int DATA_EXPERIMENT_OPTION = 3;
  private static final int STOP_EXPERIMENT_OPTION = 2;
  private static final int EDIT_EXPERIMENT_OPTION = 1;
  static final int JOIN_REQUEST_CODE = 1;
  static final int JOINED_EXPERIMENT = 1;
  
  private boolean showingJoinedExperiments;
  private Cursor cursor;
  private ExperimentProviderUtil experimentProviderUtil;
  private ListView list;
  private ProgressDialog  p;
  private ViewGroup mainLayout;
  public UserPreferences userPrefs;
  private int kindOfDataView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mainLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.explore_data, null);
    setContentView(mainLayout);
    
    Button chooseTrends = (Button)findViewById(R.id.TrendsButton);
    chooseTrends.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        gotoVarSelection(1);
        v.setVisibility(View.INVISIBLE);
      }     
      });
    
    Button chooseRelationships = (Button)findViewById(R.id.RelationshipsButton);
    chooseRelationships.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        gotoVarSelection(2);
        v.setVisibility(View.INVISIBLE);
      }     
      });
    
    Button chooseDistributions = (Button)findViewById(R.id.DistributionsButton);
    chooseDistributions.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        gotoVarSelection(3);
        v.setVisibility(View.INVISIBLE);
      }     
      });
    
  }
  
  protected void gotoVarSelection(int i){
    mainLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.variable_choices, null);
    setContentView(mainLayout);
    Intent intent = getIntent();
    intent.setData(ExperimentColumns.JOINED_EXPERIMENTS_CONTENT_URI);
    
    showingJoinedExperiments = intent.getData().equals(ExperimentColumns.JOINED_EXPERIMENTS_CONTENT_URI);
    
    userPrefs = new UserPreferences(this);
    
    list = (ListView)findViewById(R.id.find_experiments_list);
    

    
    experimentProviderUtil = new ExperimentProviderUtil(this);
    
    String selectionArgs = null;

    cursor = managedQuery(getIntent().getData(), 
        new String[] { ExperimentColumns._ID, ExperimentColumns.TITLE}, 
        selectionArgs, 
            null, null);
    
    SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, 
      android.R.layout.simple_list_item_1, cursor, 
      new String[] { ExperimentColumns.TITLE}, 
      new int[] { android.R.id.text1}) {
    
      };
      
      list.setAdapter(adapter);
      list.setOnItemClickListener(new OnItemClickListener() {
        
        public void onItemClick(AdapterView<?> listview, View textview, int position,
            long id) {
          Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
          
          String action = getIntent().getAction();
          if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
              // The caller is waiting for us to return an experiment selected by
              // the user.  The have clicked on one, so return it now.
              setResult(RESULT_OK, new Intent().setData(uri));
          } else {
              // Launch activity to view/edit or run the currently selected experiment
//            if (showingJoinedExperiments) {
//              if (position == 0) {
              Intent experimentIntent = new Intent(ExploreDataActivity.this, ExperimentExecutor.class);
              experimentIntent.setData(uri);
              startActivity(experimentIntent);
              finish();
//            } 
          }
        }
      });
  }


  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    setContentView(mainLayout);
  }
  
  public void test(){
    mainLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.find_experiments, null);
    setContentView(mainLayout);
  }
  

  protected void refreshList() {    
    new DownloadExperimentsTask(this, cursor, userPrefs, experimentProviderUtil, null).execute();
  }

}