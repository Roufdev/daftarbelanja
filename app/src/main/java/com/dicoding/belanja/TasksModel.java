package com.dicoding.belanja;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.cloudant.sync.datastore.ConflictException;
import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.datastore.DatastoreNotCreatedException;
import com.cloudant.sync.datastore.DocumentBodyFactory;
import com.cloudant.sync.datastore.DocumentException;
import com.cloudant.sync.datastore.DocumentRevision;
import com.cloudant.sync.event.Subscribe;
import com.cloudant.sync.notifications.ReplicationCompleted;
import com.cloudant.sync.notifications.ReplicationErrored;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorBuilder;

import java.util.ArrayList;
import java.util.List;


/**
 * Handles creating the Cloudant datastore(s) and remote replication.
 * This is where the bulk of the Cloudant store code lives.
 * Shows you how to create, replicate, and perform all CRUD operations across remote and local Cloudant datastores.
 */
class TasksModel {

    private static final String LOG_TAG = "TasksModel";

    // Local datastore to CRUD tasks and update with remote Bluemix datastore.
    private java.net.URI cloudantUri;
    private Datastore ds;
    private DatastoreManager manager;

    // Replicators used to Push and/or pull data to and/or from remote datastore on Bluemix.
    private Replicator mPushReplicator;
    private Replicator mPullReplicator;

    private Context mContext;

    private final Handler mHandler;
    private MainActivity mListener;

    TasksModel(Context context) {

        mContext = context;

        // Create a DatastoreManager using application internal storage path
        java.io.File path = getApplicationContext().getDir("datastores", android.content.Context.MODE_PRIVATE);
        manager = DatastoreManager.getInstance(path);

        try {
            cloudantUri = new java.net.URI(getApplicationContext().getResources().getString(R.string.cloudantUrl1) + "/your_db_name");
            // Create the Datastore object you'll use in all of your Cloudant operations:
            ds = manager.openDatastore("my_datastore");

            // At this point, you may wish to create pull and push replicators for bi-directional sync.  It is up
            // to you, the developer, to program the interaction between the Cloudant database and the mobile application.
            // A simple example follows.  The example does not include a countdown latch, does not show how to
            // subscribe to sync events, and ignores sync errors.

            // Create and run the pull replicator
            //com.cloudant.sync.replication.Replicator pullReplicator = com.cloudant.sync.replication.ReplicatorBuilder.pull().from(cloudantUri).to(ds).build();
            //pullReplicator.start();
            // Create and run the push replicator
            //com.cloudant.sync.replication.Replicator pushReplicator = com.cloudant.sync.replication.ReplicatorBuilder.push().to(cloudantUri).from(ds).build();
            //pushReplicator.start();

        } catch (java.net.URISyntaxException e) {
            android.util.Log.e("TAG", e.getMessage(), e);
        } catch (DatastoreNotCreatedException e) {
            android.util.Log.e("TAG", e.getMessage(), e);
        }

        // Set up the replicator objects from the app's settings.
        this.loadReplicationSettings();

        // Allow us to switch code called by the ReplicationListener into
        // the main thread so the UI can update safely.
        this.mHandler = new Handler(Looper.getMainLooper());

    }

    private Context getApplicationContext(){
        return mContext;
    }

    /**
     * Sets the listener for replication callbacks as a weak reference.
     * @param listener {@link MainActivity} to receive callbacks.
     */
    void setReplicationListener(MainActivity listener) {
        this.mListener = listener;
    }

    /**
     * Creates a task, assigning an ID.
     * @param task task to create.
     * @return new revision of the document.
     */
    Task createDocument(Task task) {
        DocumentRevision rev = new DocumentRevision();
        rev.setBody(DocumentBodyFactory.create(task.asMap()));
        try {
            DocumentRevision created = this.ds.createDocumentFromRevision(rev);
            return Task.fromRevision(created);
        } catch (DocumentException de) {
            return null;
        }
    }

    /**
     * Updates a Task document within the datastore.
     * @param task task to update.
     * @return the updated revision of the Task.
     * @throws ConflictException if the task passed in has a rev which doesn't
     *      match the current rev in the datastore.
     */
    Task updateDocument(Task task) throws ConflictException {
        DocumentRevision rev = task.getDocumentRevision();
        rev.setBody(DocumentBodyFactory.create(task.asMap()));
        try {
            DocumentRevision updated = this.ds.updateDocumentFromRevision(rev);
            return Task.fromRevision(updated);
        } catch (DocumentException de) {
            return null;
        }
    }

    /**
     * Deletes a Task document within the datastore.
     * @param task task to delete
     * @throws ConflictException if the task passed in has a rev which doesn't
     *      match the current rev in the datastore.
     */
    void deleteDocument(Task task) throws ConflictException {
        this.ds.deleteDocumentFromRevision(task.getDocumentRevision());
    }

    /**
     * Returns all {@code Task} documents in the datastore.
     * @return a List<Task> object of size taskCount.
     */
    List<Task> allTasks() {
        int nDocs = this.ds.getDocumentCount();
        List<DocumentRevision> all = this.ds.getAllDocuments(0, nDocs, true);
        List<Task> tasks = new ArrayList<>();

        // Filter all documents down to those of type Task.
        for(DocumentRevision rev : all) {
            Task t = Task.fromRevision(rev);
            if (t != null) {
                tasks.add(t);
            }
        }

        return tasks;
    }

    /**
     * Stops running replications.
     *
     * The stop() methods stops the replications asynchronously, see the
     * replicator docs for more information.
     */
    void stopAllReplications() {
        if (this.mPullReplicator != null) {
            this.mPullReplicator.stop();
        }
        if (this.mPushReplicator != null) {
            this.mPushReplicator.stop();
        }
    }

    /**
     * Starts the configured push replication to Cloudant remote datastore on Bluemix.
     */
    void startPushReplication() {
        if (this.mPushReplicator != null) {
            this.mPushReplicator.start();
        } else {
            throw new RuntimeException("Push replication not set up correctly");
        }
    }

    /**
     * Starts the configured pull replication from Cloudant remote datastore on Bluemix.
     */
    void startPullReplication() {
        if (this.mPullReplicator != null) {
            this.mPullReplicator.start();
        } else {
            throw new RuntimeException("Push replication not set up correctly");
        }
    }

    /**
     * Create and register Pull and Push Replicators to sync data with the remote Cloudant datastore.
     */
    private void loadReplicationSettings() {

        mPullReplicator = ReplicatorBuilder.pull().to(ds).from(cloudantUri).build();
        mPushReplicator = ReplicatorBuilder.push().from(ds).to(cloudantUri).build();

        // Registers this class to listen to replication events, complete and error functions below.
        mPushReplicator.getEventBus().register(this);
        mPullReplicator.getEventBus().register(this);

    }

    /**
     * Calls the MainActivity's replicationComplete method on the main thread,
     * as the complete() callback will probably come from a replicator worker
     * thread.
     */
    @Subscribe
    public void complete(ReplicationCompleted rc) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.replicationComplete();
                }
            }
        });
    }

    /**
     * Calls the MainActivity's replicationComplete method on the main thread,
     * as the error() callback will probably come from a replicator worker
     * thread.
     */
    @Subscribe
    public void error(ReplicationErrored re) {
        Log.e(LOG_TAG, "Replication error:", re.errorInfo.getException());

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.replicationError();
                }
            }
        });
    }
}
