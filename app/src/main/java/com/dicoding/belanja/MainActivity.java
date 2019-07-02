package com.dicoding.belanja;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.cloudant.sync.datastore.ConflictException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.util.Log;
import android.view.ActionMode;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Request;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.Response;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;





import org.json.JSONObject;

/**
 * Main list activity that configures and starts Cloudant sync.
 */
public class MainActivity extends AppCompatActivity{

    static final String LOG_TAG = "MainActivity";

    // Main data model object.
    private TasksModel sTasks;
    public TaskAdapter mTaskAdapter;
    private ListView listView;

    ActionMode mActionMode = null; // Holder for action bar interaction when an item is tapped.

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Core SDK must be initialized to interact with Bluemix Mobile services.
        BMSClient.getInstance().initialize(getApplicationContext(), BMSClient.REGION_SYDNEY);
        // Create Remote Cloudant database to sync with 
        Request request = new Request(getApplicationContext().getResources().getString(R.string.cloudantUrl1) + "/your_db_name", Request.PUT);
        
        // Must encode auth string to create db via REST request
        byte[] byteAuth = new byte[0];
        try {
            String byteString = getString(R.string.cloudantUsername1) + ":" + getString(R.string.cloudantPassword1);
            byteAuth = byteString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String base64Auth = Base64.encodeToString(byteAuth, Base64.NO_WRAP);

        HashMap headers = new HashMap();
        List<String> authorization = new ArrayList<>();
        authorization.add("Basic " + base64Auth);
        
        headers.put("authorization", authorization);
        
        request.setHeaders(headers);
        
        request.send(getApplicationContext(), new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                Log.i(MainActivity.LOG_TAG, "Database successfully created. " + response.toString());
            }

            // On failure, log errors
            @Override
            public void onFailure(Response response, Throwable throwable, JSONObject extendedInfo) {
                String errorMessage = "";

                if (response != null) {
                   errorMessage += response.getResponseText();
                }
                if (throwable != null) {
                    errorMessage += throwable.getMessage();
                }
                if (extendedInfo != null){
                    errorMessage += "EXTENDED_INFO: " + extendedInfo.toString() + "\n";
                }
                if (errorMessage.isEmpty()) {
                    errorMessage = "Request Failed With Unknown Error.";
                }

                if(!errorMessage.contains("file_exists")) {
                    Log.e(MainActivity.LOG_TAG, "DB creation failed with error: " + errorMessage);
                } else {
                    Log.i(MainActivity.LOG_TAG, "DB was created already.");
                }
            }
        });

        // Protect creation of static TasksModel singleton.
        if (sTasks == null) {
            // Model needs to stay in existence for lifetime of app.
            sTasks = new TasksModel(this.getApplicationContext());
        }

        // Register this activity as the listener to replication updates
        // while its active.
        sTasks.setReplicationListener(this);

        // Create button to create new tasks.
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showTaskDialog(R.string.new_task, -1);
                }
            });
        }

        listView = (ListView) findViewById(R.id.list);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> adapter,View v, int position, long lon){

                if(mActionMode != null) {
                    mActionMode.finish();
                }

                // Make the newly clicked item the currently selected one.
                listView.setItemChecked(position, true);
                mActionMode = listView.startActionMode(mActionModeCallback);
            }
        });

        
        
        
        
        
         
        // Load the Cloudant tasks from the model.
        this.reloadTasksFromModel();
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()");
        super.onDestroy();

        // Clear our reference as listener.
        sTasks.setReplicationListener(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        
        

        showProgressDialog(R.string.action_download, "Syncing with Cloudant");
        sTasks.startPullReplication();
    }

    // Creates default replication/settings menu in the title bar.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.todo, menu);
        return true;
    }

    // Handles interaction with the dropdown corner menu.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_download:
                showProgressDialog(R.string.action_download, "Pulling changes from Cloudant");
                sTasks.startPullReplication();
                return true;
            case R.id.action_upload:
                showProgressDialog(R.string.action_upload, "Pushing Changes to Cloudant");
                sTasks.startPushReplication();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Action mode handles the action bar interactions (update/delete buttons) created when a list item is tapped.
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    deleteTaskAt(listView.getCheckedItemPosition());
                    mode.finish();
                    return true;
                case R.id.action_update:
                    showTaskDialog(R.string.update_task, listView.getCheckedItemPosition());
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            listView.setItemChecked(listView.getCheckedItemPosition(), false);
            mActionMode = null;
        }
    };

    // Create new task and add to local Cloudant datastore and list adapter list.
    public void createNewTask(String desc) {
        Task t = new Task(desc);
        sTasks.createDocument(t);
        reloadTasksFromModel();
    }

    // Copies Local Cloudant Store into Array List for adapter to translate Tasks onto the List view.
    private void reloadTasksFromModel() {

        List<Task> tasks = sTasks.allTasks();

        // Sort list to show alphabetical order, top to bottom.
        Collections.sort(tasks, new Comparator<Task>() {
            @Override
            public int compare(Task task1, Task task2) {
                return task1.getDescription().compareToIgnoreCase(task2.getDescription());
            }
        });

        this.mTaskAdapter = new TaskAdapter(this, tasks);
        listView.setAdapter(this.mTaskAdapter);
    }

    // Update task at given location when update is confirmed.
    public void updateTaskAt(int position, String description) {
        try {
            Task t = (Task) mTaskAdapter.getItem(position);
            t.setDescription(description);
            sTasks.updateDocument(t);
            reloadTasksFromModel();
            Toast.makeText(MainActivity.this,
                    "Updated item : " + t.getDescription(),
                    Toast.LENGTH_SHORT).show();
        } catch (ConflictException e) {
            throw new RuntimeException(e);
        }
    }

    // Onclick check box function, similar to updateTaskAt but without the full reloadTaskFromModel call
    public void onCompleteCheckboxClicked(View view) {
        try {
            Task t = (Task) mTaskAdapter.getItem(view.getId());
            t.setCompleted(!t.isCompleted());
            t = sTasks.updateDocument(t);
            mTaskAdapter.set(view.getId(), t);
        } catch (ConflictException e) {
            throw new RuntimeException(e);
        }
    }

    // Delete task at given location when delete is pressed.
    private void deleteTaskAt(int position) {
        try {
            Task t = (Task) mTaskAdapter.getItem(position);
            sTasks.deleteDocument(t);
            mTaskAdapter.remove(position);
            Toast.makeText(MainActivity.this,
                    "Deleted item : " + t.getDescription(),
                    Toast.LENGTH_SHORT).show();
        } catch (ConflictException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Displays an ErrorDialog with error details and resolution.
     * @param errorTitle Error Title from values/strings.xml.
     * @param errorMessage Error Message either from values/strings.xml or response from server.
     * @param canContinue Whether the application can continue without needing to be rebuilt.
     */
    public void showErrorDialog(int errorTitle, String errorMessage, boolean canContinue) {
        DialogFragment newErrorFragment = ErrorDialogFragment.newInstance(errorTitle, errorMessage, canContinue);
        newErrorFragment.show(getFragmentManager(), "error");
    }

    /**
     * Displays a NewTaskDialog with options to set/edit the task string description.
     * @param title Dialog title banner.
     * @param taskPosition used to locate and update existing task, will be -1 for create.
     */
    public void showTaskDialog(int title, int taskPosition){
        DialogFragment newTaskFragment = TaskDialogFragment.newInstance(title, taskPosition);
        newTaskFragment.show(getFragmentManager(), "task");
    }

    /**
     * Displays the progress spinner while syncing local and remote datastores.
     * @param title Dialog title banner (Upload/Download).
     * @param message Tells the user what is happening (Pushing/Pulling data).
     */
    public void showProgressDialog(int title, String message){
        DialogFragment newProgressFragment = ProgressDialogFragment.newInstance(title, message);
        newProgressFragment.show(getFragmentManager(), "progress");
    }

    /**
     * Dismiss the dialog once progress is completed.
     */
    public void dismissDialog(){
        ((DialogFragment)getFragmentManager().findFragmentByTag("progress")).dismiss();
    }

    // Called when the user decides to interrupt push/pull replication.
    void stopReplication() {
        sTasks.stopAllReplications();
        dismissDialog();
        mTaskAdapter.notifyDataSetChanged();
    }

    /**
     * Called by TasksModel when it receives a replication complete callback.
     * TasksModel takes care of calling this on the main thread.
     */
    void replicationComplete() {
        reloadTasksFromModel();
        Toast.makeText(getApplicationContext(),
                R.string.replication_completed,
                Toast.LENGTH_LONG).show();
        dismissDialog();
    }

    /**
     * Called by TasksModel when it receives a replication error callback.
     * TasksModel takes care of calling this on the main thread.
     */
    void replicationError() {
        Log.i(LOG_TAG, getString(R.string.replication_error));
        reloadTasksFromModel();
        dismissDialog();
        showErrorDialog(R.string.replication_failed,getString(R.string.replication_error), false);
    }
}
