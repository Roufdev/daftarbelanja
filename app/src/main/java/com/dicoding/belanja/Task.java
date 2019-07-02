package com.dicoding.belanja;

import com.cloudant.sync.datastore.DocumentRevision;

import java.util.HashMap;
import java.util.Map;

/*
 * Object representing a task.
 *
 * As well as acting as a value object, this class also has a reference to the original
 * DocumentRevision, which will be valid if the Task was fetched from the database, or else null
 * (eg for Tasks which have been created but not yet saved to the database).
 *
 * fromRevision() and asMap() act as helpers to map to and from JSON - in a real application
 * something more complex like an object mapper might be used.
 */

class Task {

    private Task() {}

    Task(String desc) {
        this.setDescription(desc);
        this.setCompleted(false);
        this.setType(DOC_TYPE);
    }

    // This is the revision number in the database representing this task.
    private DocumentRevision rev;
    DocumentRevision getDocumentRevision() {
        return rev;
    }

    // Doc type needed to identify and group collective types in the datastore.
    private static final String DOC_TYPE = "com.cloudant.sync.example.task";
    private String type = DOC_TYPE;
    private void setType(String type) {
        this.type = type;
    }

    // Variable for the check mark completed flag.
    private boolean completed;
    boolean isCompleted() {
        return this.completed;
    }
    void setCompleted(boolean completed) {
        this.completed = completed;
    }

    // Main task text.
    private String description;
    String getDescription() {
        return this.description;
    }
    void setDescription(String desc) {
        this.description = desc;
    }

    @Override
    public String toString() {
        return "{ desc: " + getDescription() + ", completed: " + isCompleted() + "}";
    }

    // Create task object based on doc revision from datastore.
    static Task fromRevision(DocumentRevision rev) {
        Task t = new Task();
        t.rev = rev;

        Map<String, Object> map = rev.asMap();
        if(map.containsKey("type") && map.get("type").equals(Task.DOC_TYPE)) {
            t.setType((String) map.get("type"));
            t.setCompleted((Boolean) map.get("completed"));
            t.setDescription((String) map.get("description"));
            return t;
        }
        return null;
    }

    // Return task as a Hash Map for easy consumption by replicators and datastores.
    Map<String, Object> asMap() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("completed", completed);
        map.put("description", description);
        return map;
    }

}
