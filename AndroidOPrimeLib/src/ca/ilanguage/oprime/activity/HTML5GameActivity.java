package ca.ilanguage.oprime.activity;

import java.io.File;
import java.io.Serializable;
import java.util.Locale;

import ca.ilanguage.oprime.content.ExperimentJavaScriptInterface;
import ca.ilanguage.oprime.content.OPrime;
import ca.ilanguage.oprime.content.OPrimeApp;
import ca.ilanguage.oprime.content.Participant;
import ca.ilanguage.oprime.content.SubExperimentBlock;
import ca.ilanguage.oprime.datacollection.AudioRecorder;
import ca.ilanguage.oprime.datacollection.SubExperimentToJson;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class HTML5GameActivity extends HTML5Activity {

  protected int mCurrentSubex = 0;
  protected OPrimeApp app;
  protected Boolean mAutoAdvance = false;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    
    String outputDir = mOutputDir + "video/";
    new File(outputDir).mkdirs();

  }

  @Override
  protected void setUpVariables() {
    D = ((OPrimeApp) getApplication()).D;
    mOutputDir = ((OPrimeApp) getApplication()).getOutputDir();
    mInitialAppServerUrl = "file:///android_asset/sample_menu.html";// "http://192.168.0.180:3001/";
    mJavaScriptInterface = new ExperimentJavaScriptInterface(D, TAG,
        mOutputDir, getApplicationContext(), this, "");
    if (D)
      Log.d(TAG, "Using the OPrime experiment javascript interface.");

    this.app = (OPrimeApp) getApplication();
  }

  @Override
  protected void prepareWebView() {
    super.prepareWebView();
    checkIfNeedToPrepareExperiment();
  }

  protected void checkIfNeedToPrepareExperiment(){
    boolean prepareExperiment = getIntent().getExtras().getBoolean(
        OPrime.EXTRA_PLEASE_PREPARE_EXPERIMENT, false);
    if (prepareExperiment) {
      if (D) {
        Log.d(TAG, "HTML5GameActivity was asked to prepare the experiment.");
      }
      SharedPreferences prefs = getSharedPreferences(OPrimeApp.PREFERENCE_NAME,
          MODE_PRIVATE);
      String lang = prefs.getString(OPrimeApp.PREFERENCE_EXPERIMENT_LANGUAGE,
          "");
      if (app.getLanguage().getLanguage().equals(lang) && app.getExperiment() != null) {
        // do nothing if they didn't change the language
        if (D) {
          Log.d(TAG,
              "The Language has not changed, not preparing the experiment for "
                  + lang);
        }
      } else {
        if (lang == null) {
          lang = app.getLanguage().getLanguage();
          if (D) {
            Log.d(TAG,
                "The Language was null, setting it to the tablets default language "
                    + lang);
          }
        }
        if (D) {
          Log.d(TAG, "Preparing the experiment for " + lang);
        }
        app.createNewExperiment(lang);
        initExperiment();
      }
    }
  }
  
  protected void initExperiment() {
    getParticipantDetails(this.app);
    mWebView.loadUrl("file:///android_asset/sample_menu.html");
  }

  protected void getParticipantDetails(OPrimeApp app) {
    Participant p;
    try {
      p = app.getExperiment().getParticipant();
    } catch (Exception e) {
      p = new Participant();
    }
    SharedPreferences prefs = getSharedPreferences(OPrimeApp.PREFERENCE_NAME,
        MODE_PRIVATE);
    String firstname = prefs.getString(
        OPrimeApp.PREFERENCE_PARTICIPANT_FIRSTNAME, "");
    String lastname = prefs.getString(
        OPrimeApp.PREFERENCE_PARTICIPANT_LASTNAME, "");
    String experimenter = prefs.getString(
        OPrimeApp.PREFERENCE_EXPERIEMENTER_CODE, "NN");
    String details = prefs.getString(OPrimeApp.PREFERENCE_PARTICIPANT_DETAILS,
        "");
    String gender = prefs
        .getString(OPrimeApp.PREFERENCE_PARTICIPANT_GENDER, "");
    String birthdate = prefs.getString(
        OPrimeApp.PREFERENCE_PARTICIPANT_BIRTHDATE, "");
    String lang = prefs.getString(OPrimeApp.PREFERENCE_EXPERIMENT_LANGUAGE,
        "en");
    // String langs =
    // prefs.getString(OPrimeApp.PREFERENCE_PARTICIPANT_LANGUAGES,
    // "");
    String testDayNumber = prefs.getString(
        OPrimeApp.PREFERENCE_TESTING_DAY_NUMBER, "1");
    String participantNumberOnDay = prefs.getString(
        OPrimeApp.PREFERENCE_PARTICIPANT_NUMBER_IN_DAY, "1");
    /*
     * Build the participant ID and save the start time to the preferences.
     */
    p.setCode(testDayNumber + experimenter + participantNumberOnDay
        + firstname.substring(0, 1).toUpperCase()
        + lastname.substring(0, 1).toUpperCase());
    p.setFirstname(firstname);
    p.setLastname(lastname);
    p.setExperimenterCode(experimenter);
    p.setGender(gender);
    p.setBirthdate(birthdate);
    p.setDetails(details);

    if (app.getExperiment() == null) {
      app.createNewExperiment(lang);
    }
    app.getExperiment().setParticipant(p);
    // Toast.makeText(getApplicationContext(), p.toCSVPrivateString(),
    // Toast.LENGTH_LONG).show();

  }

  protected void trackCompletedExperiment(SubExperimentBlock completedExp) {
    // Place holder to be overriden by experiments if needed
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(app == null){
      app = this.getApp();
    }
    switch (requestCode) {
    case OPrime.EXPERIMENT_COMPLETED:
      if (data != null) {
        SubExperimentBlock completedExp = (SubExperimentBlock) data.getExtras()
            .getSerializable(OPrime.EXTRA_SUB_EXPERIMENT);
        app.getSubExperiments().set(mCurrentSubex, completedExp);

        Intent i = new Intent(this, SubExperimentToJson.class);
        i.putExtra(OPrime.EXTRA_SUB_EXPERIMENT, (Serializable) app
            .getSubExperiments().get(mCurrentSubex));
        startService(i);
        app.getExperiment()
            .getParticipant()
            .setStatus(
                app.getExperiment().getParticipant().getStatus()
                    + ":::"
                    + completedExp.getTitle()
                    + " in "
                    + (new Locale(completedExp.getLanguage()))
                        .getDisplayLanguage() + " --- "
                    + completedExp.getDisplayedStimuli() + "/"
                    + completedExp.getStimuli().size() + " Completed ");
        trackCompletedExperiment(completedExp);

        app.writePrivateParticipantToFile();

        String intentAfterSubExperiment = app.getExperiment()
            .getSubExperiments().get(mCurrentSubex)
            .getIntentToCallAfterThisSubExperiment();

        if (!"".equals(intentAfterSubExperiment)) {
          Intent takepicture = new Intent(intentAfterSubExperiment);
          takepicture.putExtra(OPrime.EXTRA_RESULT_FILENAME, completedExp
              .getResultsFileWithoutSuffix().replace("video", "images")
              + ".jpg");
          startActivity(takepicture);
        }
      }
      stopVideoRecorder();
      if (mAutoAdvance) {
        mCurrentSubex++;
        if (mCurrentSubex >= app.getExperiment().getSubExperiments().size()) {
          Toast.makeText(getApplicationContext(), "Experiment completed!",
              Toast.LENGTH_LONG).show();
        } else {
          mWebView.loadUrl("javascript:getPositionAsButton(0,0,"
              + mCurrentSubex + ")");
        }
      }
      break;
    case OPrime.PREPARE_TRIAL:
      initExperiment();
      break;
    case OPrime.SWITCH_LANGUAGE:
      SharedPreferences prefs = getSharedPreferences(OPrimeApp.PREFERENCE_NAME,
          MODE_PRIVATE);
      String lang = prefs.getString(OPrimeApp.PREFERENCE_EXPERIMENT_LANGUAGE,
          "en");
      if (lang.equals(app.getLanguage().getLanguage())) {
        // do nothing if they didnt change the languge
      } else {
        app.createNewExperiment(lang);
        initExperiment();
      }
      break;
    case OPrime.REPLAY_RESULTS:
      break;
    default:
      break;
    }
    super.onActivityResult(requestCode, resultCode, data);

  }

  protected void stopVideoRecorder() {
    Intent i = new Intent(OPrime.INTENT_STOP_VIDEO_RECORDING);
    sendBroadcast(i);
    Intent audio = new Intent(this, AudioRecorder.class);
    stopService(audio);
    // Toast.makeText(this, "Subexperiment complete. ",
    // Toast.LENGTH_LONG).show();
  }

  public int getCurrentSubex() {
    return mCurrentSubex;
  }

  public void setCurrentSubex(int mCurrentSubex) {
    this.mCurrentSubex = mCurrentSubex;
  }

  public OPrimeApp getApp() {
    return app;
  }

  public void setApp(OPrimeApp app) {
    this.app = app;
  }

  public Boolean getAutoAdvance() {
    return mAutoAdvance;
  }

  public void setAutoAdvance(Boolean mAutoAdvance) {
    this.mAutoAdvance = mAutoAdvance;
  }

}