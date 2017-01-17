package patryk.zmijewski.polsl.pl.finalproject.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import patryk.zmijewski.polsl.pl.finalproject.R;
import patryk.zmijewski.polsl.pl.finalproject.model.settings.RecordSettings;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    private static final RecordSettings settingsInstance = RecordSettings.getInstance();

    private EditText fileNameEditText;

    private final List<Switch> switches = new ArrayList<>();

    private Switch theta;
    private Switch delta;
    private Switch lowAlpha;
    private Switch highAlpha;
    private Switch lowBeta;
    private Switch highBeta;
    private Switch lowGamma;
    private Switch mediumGamma;
    private Switch attenion;
    private Switch meditation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initView();
        initSwitches();

        fileNameEditText = (EditText) findViewById(R.id.videoFileName);

        fileNameEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileNameEditText.setText("");
            }
        });


        Button saveButton = (Button) findViewById(R.id.buttonSave);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyAndSave();
            }
        });

        Button restoreDefaultButton = (Button) findViewById(R.id.buttonRestoreDefault);
        restoreDefaultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileNameEditText.setText(R.string.insert_filename);
                setDefaultSwitches();

            }
        });
    }

    public void initView(){
        theta = (Switch)findViewById(R.id.switchT);
        delta = (Switch)findViewById(R.id.switchD);
        lowAlpha = (Switch)findViewById(R.id.switchLA);
        highAlpha = (Switch)findViewById(R.id.switchHA);
        lowBeta = (Switch)findViewById(R.id.switchLB);
        highBeta = (Switch)findViewById(R.id.switchHB);
        lowGamma = (Switch)findViewById(R.id.switchLG);
        mediumGamma = (Switch)findViewById(R.id.switchMG);
        attenion = (Switch)findViewById(R.id.switchAttention);
        meditation = (Switch)findViewById(R.id.switchMeditation);
    }
    public void initSwitches(){
        switches.add(theta);
        switches.add(delta);
        switches.add(lowAlpha);
        switches.add(highAlpha);
        switches.add(lowBeta);
        switches.add(highBeta);
        switches.add(lowGamma);
        switches.add(mediumGamma);
        switches.add(attenion);
        switches.add(meditation);

        lowAlpha.setChecked(true);
        highAlpha.setChecked(true);
        attenion.setChecked(true);
        meditation.setChecked(true);
    }

    public int[] setDefaultSwitches(){
        int[] result = new int[8];

        for(int i=0;i<result.length;i++){
            result[i]=-1;
        }
        for(Switch s : switches){
            s.setChecked(false);
        }
        lowAlpha.setChecked(true);
        highAlpha.setChecked(true);
        attenion.setChecked(true);
        meditation.setChecked(true);

        result[0]=2;
        result[1]=3;
        result[2]=8;
        result[3]=9;

        return result;
    }

    public void verifyAndSave() {
        String fileNameString = fileNameEditText.getText().toString();
        if(!fileNameString.contentEquals(getString(R.string.insert_filename))) {
            settingsInstance.setFileName(fileNameString);
        }
        int[] visibleReadings = getVisibleReadings();
        settingsInstance.setActiveSensorReadings(visibleReadings);
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    public int[] getVisibleReadings(){
        int position;
        int[] result = new int[8];

        for(int i=0;i<result.length;i++){
            result[i]=-1;
        }

        position = 0;
        for(int i = 0; i<switches.size();i++){
            if (switches.get(i).isChecked()) {
                if(position>7) {
                    Toast.makeText(this, "Up to 8 readings can be selected", Toast.LENGTH_LONG).show();
                    result = setDefaultSwitches();
                    break;
                }
                result[position]=i;
                position += 1;
            }
        }


        return result;
    }







}
