package com.bnr.que_snitch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CrimeFragment extends Fragment {
    private Crime mCrime;
    private EditText mTitle;
    private Button mButton;
    private CheckBox mBox;
    private static final String ARG_CRIME_ID = "ARG_CRIME_ID";
    private static final String DIALOG_DATE = "DialogDate";
    private static final int REQUEST_DATE = 0;
    private static final int REQUEST_CONTACT = 1;
    private static final int REQUEST_PHOTO = 2;

    private ImageButton takePic;
    private ImageView showPic;
    private Button chooseSuspect;
    private Button sendCrimeReportButton;
    private File mPhotoFile;
    private Callbacks mCallbacks;

    //required interface for hosting activities
    public interface Callbacks{
        void onCrimeUpdated(Crime crime);
    }

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        mCallbacks = (Callbacks)context;
    }

    @Override
    public void onDetach(){
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        UUID crimeId = (UUID) getArguments().getSerializable(ARG_CRIME_ID);
        mCrime = CrimeLab.get(getActivity()).getCrime(crimeId);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_crime,container,false);

        mButton = v.findViewById(R.id.crime_date);
        updateDate(mCrime.getDate());
        //mButton.setEnabled(false);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager manager = getFragmentManager();
                try {
                    DateFormat format =
                            new SimpleDateFormat("MMMM d,yyyy", Locale.ENGLISH);
                    Date theDate = format.parse(mCrime.getDate());
                    DatePickerFrag dialog = DatePickerFrag.newInstance(theDate);
                    dialog.setTargetFragment(CrimeFragment.this,REQUEST_DATE);
                    dialog.show(manager,DIALOG_DATE);;
                } catch (ParseException pe){
                    pe.printStackTrace();
                }
            }
        });
        mBox = v.findViewById(R.id.crime_solved);
        mBox.setChecked(mCrime.isSolved());
        mBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mCrime.setSolved(b);
                updateCrime();
            }
        });

        mTitle = v.findViewById(R.id.crime_title);
        mTitle.setText(mCrime.getTitle());
        mTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                mCrime.setTitle(s.toString());
                updateCrime();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        sendCrimeReportButton = v.findViewById(R.id.send_crime_report_button);
        takePic = v.findViewById(R.id.take_pic);
        chooseSuspect = v.findViewById(R.id.choose_suspect_button);
        showPic= v.findViewById(R.id.show_pic);

        sendCrimeReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //THIS TYPE OF IMPLICIT INTENT WILL ALWAYS WORK CORRECTLY
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_TEXT,getCrimeReport());
                i.putExtra(Intent.EXTRA_SUBJECT,getString(R.string.crime_report_subject));
                //Optional intent chooser that customizes the title
                i = Intent.createChooser(i,getString(R.string.send_report));
                startActivity(i);
            }
        });

        final Intent pickContact = new Intent(Intent.ACTION_PICK,ContactsContract.Contacts.CONTENT_URI);

        //THIS TYPE OF IMPLICIT INTENT WILL CRASH IF THERE IS NO APP THAT CAN HANDLE THE ACTION
        //PACKAGE MANAGER TO HANDLE THIS!
        chooseSuspect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(pickContact,REQUEST_CONTACT);
            }
        });

        chooseSuspect.setText((mCrime.getSuspect() != null)?mCrime.getSuspect():getString(R.string.choose_suspect_button));

        PackageManager packageManager = getActivity().getPackageManager();
        if (packageManager.resolveActivity(pickContact,PackageManager.MATCH_DEFAULT_ONLY) == null){
            chooseSuspect.setEnabled(false);
        }

        mPhotoFile = CrimeLab.get(getActivity()).getPhotoFile(mCrime);

        final Intent captureImageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        boolean canTakePhoto = mPhotoFile != null && captureImageIntent.resolveActivity(packageManager) != null;
        takePic.setEnabled(canTakePhoto);
        takePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = FileProvider.getUriForFile(getActivity(),
                        "com.bnr.android.que-snitch.file_provider",mPhotoFile);

                captureImageIntent.putExtra(MediaStore.EXTRA_OUTPUT,uri);

                List<ResolveInfo> cameraActivities =
                        getActivity().getPackageManager().queryIntentActivities(
                                captureImageIntent, PackageManager.MATCH_DEFAULT_ONLY);

                for(ResolveInfo act : cameraActivities){
                    getActivity().grantUriPermission(
                            act.activityInfo.toString(),uri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }

                startActivityForResult(captureImageIntent,REQUEST_PHOTO);
            }
        });

        updatePhotoView();

        return v;
    }

    public static CrimeFragment newInstance(UUID crimeId){
        Bundle args = new Bundle();
        args.putSerializable(ARG_CRIME_ID, crimeId);

        CrimeFragment fragment = new CrimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onPause(){
        super.onPause();

        CrimeLab.get(getActivity()).updateCrime(mCrime);
    }

    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data){
        if (resultCode != Activity.RESULT_OK){
            return;
        }

        if (requestCode == REQUEST_DATE){
            Date date = (Date) data.getSerializableExtra(DatePickerFrag.EXTRA_DATE);

            mCrime.setRawDate(date);
            mCrime.setDate(DateFormat.getDateInstance().format(date));
            updateCrime();
            updateDate(date.toString());
        } else if (requestCode == REQUEST_CONTACT && data != null){
            Uri contactUri = data.getData();
            //specify which fields you want your query to return values for
            String[] queryFields = new String[]{ContactsContract.Contacts.DISPLAY_NAME};
            //Perform your query -> the contactURi is like a "where" clause here
            Cursor c = getActivity().getContentResolver()
                    .query(contactUri,queryFields,null,null,null);

            try{
                //Double check that you actually got results
                if (c.getCount() == 0){
                    return;
                }
                //Pull out the first column of the first row of data
                //that is your suspect's name
                c.moveToFirst();
                String suspect = c.getString(0);
                mCrime.setSuspect(suspect);
                updateCrime();
                chooseSuspect.setText(suspect);
            } finally{
                c.close();
            }
        } else if (requestCode == REQUEST_PHOTO){
            Uri uri = FileProvider.getUriForFile(getActivity(),
                    "com.bnr.android.que-snitch.file_provider",mPhotoFile);

            getActivity().revokeUriPermission(uri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            updateCrime();
            updatePhotoView();
        }


    }

    private void updateDate(String date) {
        mButton.setText(date);
    }

    private String getCrimeReport(){
        String solvedString = (mCrime.isSolved())
                ?getString(R.string.crime_report_solved)
                :getString(R.string.crime_report_unsolved);

        String dateFormat = "EEE, MMM dd";
        String dateString =mCrime.getDate();
        String suspect = mCrime.getSuspect();

        suspect = (suspect == null)
                ?getString(R.string.crime_report_no_suspect)
                :getString(R.string.crime_report_suspect,suspect);

        String report = getString(R.string.crime_report,mCrime.getTitle(),dateString,solvedString,suspect);
        return report;
    }

    private void updatePhotoView(){
        if(mPhotoFile == null || !mPhotoFile.exists()){
            showPic.setImageDrawable(getResources().getDrawable(R.drawable.x));
        } else {
            Bitmap bitmap = PictureUtils.getScaledBitmap(mPhotoFile.getPath(),getActivity());
            showPic.setImageBitmap(bitmap);
        }
    }

    private void updateCrime(){
        CrimeLab.get(getActivity()).updateCrime(mCrime);
        mCallbacks.onCrimeUpdated(mCrime);
    }
}
