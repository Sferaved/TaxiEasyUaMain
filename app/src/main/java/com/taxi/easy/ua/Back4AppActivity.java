package com.taxi.easy.ua;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class Back4AppActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Saving your First data object on Back4App
        ParseObject person = new ParseObject("Person");
        person.put("name", "John Snow");
        person.put("age", 27);
        person.saveInBackground();

//Reading your First Data Object from Back4App
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Person");
        query.getInBackground("mhPFDlCahj", new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (e == null) {
                    // object will be your person
                } else {
                    // something went wrong
                }
            }
        });
    }
}
