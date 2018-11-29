package com.pbluedotsoft.actimeattack;

/**
 * Created by daniel on 27/11/18.
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

public class TrackConfigDialog extends DialogFragment {

    /**
     * Declaring the interface, to invoke a callback function in the implementing activity class
     */
    DialogPositiveListener dialogPositiveListener;


    /**
     * An interface to be implemented in the hosting activity for "OK" button click listener
     */
    interface DialogPositiveListener {
        void onPositiveClick(String selectedItem);
    }


    /**
     * This is a callback method executed when this fragment is attached to an activity.
     * This function ensures that, the hosting activity implements the interface
     * AlertPositiveListener
     */
    public void onAttach(android.app.Activity activity) {
        super.onAttach(activity);
        try {
            dialogPositiveListener = (DialogPositiveListener) activity;
        } catch (ClassCastException e) {
            // The hosting activity does not implemented the interface AlertPositiveListener
            throw new ClassCastException(activity.toString() + " must implement " +
                    "AlertPositiveListener");
        }
    }


    /**
     * This is the OK button listener for the alert dialog,
     * which in turn invokes the method onPositiveClick(position)
     * of the hosting activity which is supposed to implement it
     */
    OnClickListener positiveListener = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            AlertDialog alert = (AlertDialog) dialog;
            int position = alert.getListView().getCheckedItemPosition();
            String selectedItem = alert.getListView().getItemAtPosition(position).toString();
            dialogPositiveListener.onPositiveClick(selectedItem);
        }
    };


    /**
     * This is a callback method which will be executed
     * on creating this fragment
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Getting the arguments passed to this fragment
        Bundle bundle = getArguments();
        int position = bundle.getInt("position");
        String[] trackConfig = bundle.getStringArray("trackConfig");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Choose track layout");
        builder.setSingleChoiceItems(trackConfig, position, null);
        builder.setPositiveButton("OK", positiveListener);
//        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        return dialog;
    }
}
