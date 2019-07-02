package com.dicoding.belanja;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

/**
 * Shows Cloudant sync progress; Download and Upload dialogs.
 */
public class ProgressDialogFragment extends DialogFragment {

    /**
     * ProgressDialogFragment constructor.
     * @param title Title of the Alert, referenced by its resource id.
     * @param message Message contents of the Alert dialog.
     * @return The constructed ProgressDialogFragment.
     */
    public static ProgressDialogFragment newInstance(int title, String message) {
        ProgressDialogFragment frag = new ProgressDialogFragment();
        Bundle args = new Bundle();

        args.putInt("title", title);
        args.putString("message", message);

        frag.setArguments(args);
        return frag;
    }

    // Create progress dialog with appropriate properties and interaction; Upload vs Download.
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        int title = getArguments().getInt("title");
        String message = getArguments().getString("message");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(title).setMessage(message);

        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_loading, null);

        // Create onclick listener for cancel button to stop replication.
        DialogInterface.OnClickListener negativeClick = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((MainActivity) getActivity()).stopReplication();
            }
        };

        // Create keylistener to indicate replication is still running when the back button is pressed.
        DialogInterface.OnKeyListener keyListener = new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    Toast.makeText(getActivity(),
                            R.string.replication_running, Toast.LENGTH_LONG).show();
                    return true;
                }
                return false;
            }
        };

        // Set above listeners to the dialog being created.
        builder.setView(view).setNegativeButton("Stop", negativeClick).setOnKeyListener(keyListener);

        return builder.create();
    }
}
