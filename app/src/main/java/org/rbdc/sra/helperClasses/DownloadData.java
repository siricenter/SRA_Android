package org.rbdc.sra.helperClasses;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shaded.fasterxml.jackson.core.JsonFactory;
import com.shaded.fasterxml.jackson.core.type.TypeReference;
import com.shaded.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.rbdc.sra.Dashboard;
import org.rbdc.sra.objects.Area;
import org.rbdc.sra.objects.AreaLogin;
import org.rbdc.sra.objects.CountryLogin;
import org.rbdc.sra.objects.FoodItem;
import org.rbdc.sra.objects.Household;
import org.rbdc.sra.objects.LoginObject;
import org.rbdc.sra.objects.Note;
import org.rbdc.sra.objects.QuestionSet;
import org.rbdc.sra.objects.RegionLogin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DownloadData {

    private static DownloadData instance = null;
    private static String organization = null;
    private static int passes = 0;
    private static ArrayList<Area> areas = null;



    public static void setOrganization(String organization) {
        DownloadData.organization = organization;
    }

    protected DownloadData() {
        // Exists only to defeat instantiation.
    }

    public static DownloadData getInstance() {
        if(instance == null) {
            instance = new DownloadData();
        }
        return instance;
    }

    public static void downloadGoToDash(LoginObject info, final Context activity){
        Firebase base = new Firebase("https://intense-inferno-7741.firebaseio.com/organizations/" + organization + "/resources/");
        final int number = info.getSiteLogin().getAreaCount();
        areas = new ArrayList<>();
        for (CountryLogin country : info.getSiteLogin().getCountries()){
            for(RegionLogin regions : country.getRegions()){
                for(AreaLogin area : regions.getAreas()){
                    // For each area belonging to the user, look through the resources node
                    // for child nodes that have areas with the same name
                    String id = capitalizeFirstLetter(area.getName());
                    Query query = base.orderByChild("area").equalTo(id);
                    Log.i("link: ", query.getRef().toString());
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            passes = 1;
                            Area currentArea = new Area();
                            for(DataSnapshot child : dataSnapshot.getChildren()){
                                Household household = child.getValue(Household.class);
                                Log.i("counts: ","" + passes);

                                Log.i("household downloaded: ", household.getHouseholdID());

                                //Check to see if Area has already been created, if so add the
                                //household to that area
                                if(passes > 1) {
//                                    for (Area area1 : CRUDFlinger.getAreas()){
//                                        if (area1.getName().equals(household.getArea())) {
//                                            area1.addHousehold(household);
//                                        } else {
//                                            createArea(household);
//                                        }
//                                    }
                                    currentArea.addHousehold(household);
                                    System.out.println("Adding " + household.getName() + " to " + currentArea.getName());

                                }else{
                                    currentArea = createArea(household);
                                    System.out.println("Adding " + household.getName() + " to new area");
                                    CRUDFlinger.getRegion().addArea(currentArea);
                                }

                                passes++;

                            }
                            CRUDFlinger.saveRegion();
                            //CRUDFlinger.getRegion().getAreas().addAll(areas);
                            //areas.clear();
                            if(CRUDFlinger.getAreas().size() == number){
                                Intent intent = new Intent(activity,Dashboard.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                activity.startActivity(intent);
                            }

                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                            Toast.makeText(activity,firebaseError.getMessage(),Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }
    }

    public static void downloadToSync(LoginObject info, final Context activity){
        Firebase base = new Firebase("https://intense-inferno-7741.firebaseio.com/organizations/" + organization + "/resources/");
        areas = new ArrayList<>();
        for (CountryLogin country : info.getSiteLogin().getCountries()){
            for(RegionLogin regions : country.getRegions()){
                for(AreaLogin area : regions.getAreas()){
                    String id = capitalizeFirstLetter(area.getName());
                    Query query = base.orderByChild("area").startAt(id).endAt(id);
                    query.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for(DataSnapshot child : dataSnapshot.getChildren()){
                                Household household = child.getValue(Household.class);
                                if(CRUDFlinger.getTempAreas().size() > 0){
                                    for(Area area : CRUDFlinger.getTempRegion().getAreas()){
                                        if(area.getName().equals(household.getArea())){
                                            area.addHousehold(household);
                                        }else{
                                            createTempArea(household);
                                        }
                                    }
                                }else{
                                    createTempArea(household);
                                }

                            }
                            CRUDFlinger.getTempRegion().getAreas().addAll(areas);
                            areas.clear();
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {
                            Toast.makeText(activity,firebaseError.getMessage(),Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }
        CRUDFlinger.saveRegion();
    }

    public static Area createArea(Household household) {
        Area area = new Area();
        area.setCountry(household.getCountry());
        area.setRegion(household.getRegion());
        area.setName(household.getArea());
        area.addHousehold(household);
        areas.add(area);
        return area;
    }

    public static void createTempArea(Household household){

        Area area = new Area();
        area.setCountry(household.getCountry());
        area.setRegion(household.getRegion());
        area.setName(household.getArea());
        area.addHousehold(household);
        areas.add(area);
    }

    public static String capitalizeFirstLetter(String original){
        if(original.length() == 0)
            return original;
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }

    public static void downloadQuestions() {
        Firebase base = new Firebase("https://intense-inferno-7741.firebaseio.com/organizations/" + organization + "/question sets");
        base.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    QuestionSet question = data.getValue(QuestionSet.class);

                    CRUDFlinger.addQuestionSet(question);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public static void downloadTempQuestions() {
        Firebase base = new Firebase("https://intense-inferno-7741.firebaseio.com/organizations/" + organization + "/question sets");
        base.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    System.out.println("downloading temp questions");
                    QuestionSet question = data.getValue(QuestionSet.class);

                    CRUDFlinger.addTempQuestionSet(question);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }

    public static void downloadTempNotes() {
        Firebase fbNotes = new Firebase("https://intense-inferno-7741.firebaseio.com/organizations/"+organization+"/notes");
        fbNotes.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    System.out.println("downloading temp notes");
                    Note note = data.getValue(Note.class);

                    CRUDFlinger.addTempNote(note);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public static void downloadNotes(final String username) {
        try {

            Firebase fbNotes = new Firebase("https://intense-inferno-7741.firebaseio.com/organizations/" + organization + "/notes");
            fbNotes.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        Note note = data.getValue(Note.class);
                        if (note.getAuthor().equals(username)) {
                            System.out.println("Note downloaded: " + note.getNoteTitle());
                            CRUDFlinger.addNote(note);
                        }
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static HashMap buildMap(String json) throws IOException {
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);

        TypeReference<HashMap<String,Object>> typeRef
                = new TypeReference<HashMap<String,Object>>() {};

        HashMap<String,Object> o = mapper.readValue(json, typeRef);

        return o;
    }

}
