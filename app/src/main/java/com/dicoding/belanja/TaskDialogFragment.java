package com.dicoding.belanja;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Handles dialog fragment creation for Task interaction (Create and Update).
 */
public class TaskDialogFragment extends DialogFragment {

    /**
     * TaskDialogFragment constructor.
     * @param title Title of the Alert, referenced by its resource id.
     * @param taskPosition Lets the dialog know which task needs to be updated. Will be -1 when creating a new task.
     * @return The constructed TaskDialogFragment.
     */
    public static TaskDialogFragment newInstance(int title, int taskPosition) {
        TaskDialogFragment frag = new TaskDialogFragment();
        Bundle args = new Bundle();

        args.putInt("title", title);
        args.putInt("taskPosition", taskPosition);

        frag.setArguments(args);
        return frag;
    }

    // Create Task dialog with appropriate properties and interaction; Update vs Create.
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt("title");
        final int taskPosition = getArguments().getInt("taskPosition");

        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_new_task, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(title);
        final EditText description = (EditText) v.findViewById(R.id.new_task_desc);

        if(taskPosition >= 0){
            description.setHint(((Task) ((MainActivity) getActivity()).mTaskAdapter.getItem(taskPosition)).getDescription());

            // Check description is present, if so add a task otherwise show an error.
            DialogInterface.OnClickListener positiveClick = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    if (description.getText().length() > 0) {
                        ((MainActivity) getActivity()).updateTaskAt(taskPosition, description.getText().toString());
                        description.getText().clear();
                    } else {
                        Toast.makeText(getActivity(),
                                R.string.task_not_updated,
                                Toast.LENGTH_LONG).show();
                    }
                }
            };

            builder.setView(v).setPositiveButton(R.string.update, positiveClick);
        } else {
            DialogInterface.OnClickListener positiveClick = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    if (description.getText().length() > 0) {
                        ((MainActivity) getActivity()).createNewTask(description.getText().toString());
                        description.getText().clear();
                    } else {
                        Toast.makeText(getActivity(),
                                R.string.task_not_created,
                                Toast.LENGTH_LONG).show();
                    }
                }
            };

            builder.setView(v).setPositiveButton(R.string.create, positiveClick);
        }

        // Create onClick listener for cancel button.
        DialogInterface.OnClickListener negativeClick = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        };
        builder.setNegativeButton(R.string.cancel, negativeClick);

        final AlertDialog taskDialog = builder.create();

        // Enable "Create"/"Update" button when the description has some characters.
        final TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                Button b = taskDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                b.setEnabled(description.getText().length() > 0);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        // Set listener for create button configured above.
        taskDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = taskDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                b.setEnabled(description.getText().length() > 0);
                description.addTextChangedListener(textWatcher);
            }
        });

        return taskDialog;
    }
}
