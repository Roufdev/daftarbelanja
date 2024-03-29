package com.dicoding.belanja;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Constructs and displays an error message to the user through a Dialog. Includes an option to finish the
 * Activity if an error results in the application needing to be rebuilt to function.
 */
public class ErrorDialogFragment extends DialogFragment {

    /**
     * ErrorDialogFragment constructor.
     * @param title Title of the Alert, referenced by its resource id.
     * @param message Message contents of the Alert dialog.
     * @param canContinue Whether the application can continue without needing to be rebuilt.
     * @return The constructed ErrorDialogFragment.
     */
    public static ErrorDialogFragment newInstance(int title, String message, boolean canContinue) {
        ErrorDialogFragment frag = new ErrorDialogFragment();
        Bundle args = new Bundle();

        args.putInt("title", title);
        args.putString("message", message);
        args.putBoolean("canContinue", canContinue);

        frag.setArguments(args);
        frag.setCancelable(false);
        frag.setRetainInstance(true);
        
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt("title");
        String message = getArguments().getString("message");
        boolean canContinue = getArguments().getBoolean("canContinue");

        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message);

        // If the application still has some functionality, allow the user to dismiss the dialog.
        if (canContinue) {
            return adb.setPositiveButton(R.string.alert_dialog_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Do nothing and dismiss the dialog.
                                }
                            }
                    )
                    .create();
        } else {
            // If the application has to be rebuilt for anything to work, we will finish the Activity.
            return adb.setNeutralButton(R.string.alert_dialog_close,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    getActivity().finish();
                                }
                            }
                    )
                    .create();
        }
    }
    
    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();
        // Need this code to handle fragment rotation
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }
}
