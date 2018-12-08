package com.bnr.que_snitch;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class Crime {
    private UUID mId;
    private String mTitle;
    private String mDate;
    private boolean mSolved;
    private boolean mRequiresPolice;
    private String mSuspect;
    private Date mDateRaw;

    public Crime(){
        this(UUID.randomUUID());
        //deleted for db
        //mId = UUID.randomUUID();
        //mDate = DateFormat.getDateInstance().format(new Date());
    }
    public Crime(UUID id){
        mId = id;
        mDate = DateFormat.getDateInstance().format(new Date());
    }

    public boolean setRequiresPolice() {
        return mRequiresPolice;
    }
    public void getRequiresPolice(boolean requiresPolice) {
        mRequiresPolice = requiresPolice;
    }

    public UUID getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }
    public void setTitle(String title) {
        mTitle = title;
    }

    String getSuspect(){return mSuspect;}
    public void setSuspect(String suspect){mSuspect = suspect;}

    public void setDate(String date) {
        mDate = date;
    }
    public void setRawDate(Date date) {
        mDateRaw = date;
    }
    public String getDate() {
        return mDate;
    }
    public Date getRawDate() {
        return mDateRaw;
    }

    public boolean isSolved() {
        return mSolved;
    }
    public void setSolved(boolean solved) {
        mSolved = solved;
    }

    public String getPhotoFilename() {
        return "IMG_" + getId().toString() + ".jpg";
    }

}
