package patryk.zmijewski.polsl.pl.finalproject.model.settings;

/**
 * Created by Patryk on 05.01.2017.
 */

public class RecordSettings {

    /**
     * TAG used in logcat
     */
    private static final String TAG = RecordSettings.class.getSimpleName();

    /**
     * Instance of the class, we will operate on it with the use of getInstance() method.
     */
    private static RecordSettings instance = new RecordSettings();

    /**
     * Field holding the name of the file, if empty name is automatically generated in activity.
     */
    private String fileName;

    /**
     * Filed holding tha array of int values that correspond to visibility of sensor readings on the record.
     * Mapping is as follows:
     * 0 - delta
     * 1 - theta
     * 2 - low alpha
     * 3 - high alpha
     * 4 - low beta
     * 5 - high beta
     * 6 - low gamma
     * 7 - middle gamma
     * 8 - attention
     * 9 - meditation
     */
    private int[] activeSensorReadings;

    /**
     * Pivate consturctor, which assures, that only one instance of the class will exist at once
     */
    private RecordSettings(){
        activeSensorReadings = new int[8];
        //inserting default values
        fileName="";
        //initializing array with flases
        for(int i = 0;i<activeSensorReadings.length;i++){
            activeSensorReadings[i]=-1;
        }
        //by default attention, meditation and high and low alpha reading is visible
        activeSensorReadings[0]=0;
        activeSensorReadings[1]=1;
        activeSensorReadings[2]=8;
        activeSensorReadings[3]=9;

    }

    /**
     * Getter of the instance.
     * @return instance field.
     */
    public static RecordSettings getInstance(){
        return instance;
    }

    /**
     * Method setting the same values as the constructor does
     */
    public void restoreDefaults(){
        //same values as in the constructor
        fileName="";
        //filling array with flases
        for(int i = 0;i<activeSensorReadings.length;i++){
            activeSensorReadings[i]=-1;
        }
        //by default attention, meditation and high alpha reading is visible
        activeSensorReadings[0]=0;
        activeSensorReadings[1]=1;
        activeSensorReadings[2]=2;
        activeSensorReadings[3]=3;
    }

    /**
     * Getter of the fileName field.
     * @return fileName field, that will be used to save the record
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Setter of the fileName field.
     * @param fileName String, that will be used to save the record
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Getter of the activeSensorReadings field.
     * @return activeSensorReadings - array of boolean vaules that represent which values are present on the record.
     */
    public int[] getActiveSensorReadings() {
        return activeSensorReadings;
    }

    /**
     * Setter of the activeSensorReadings field.
     * @param activeSensorReadings - array of boolean vaules that represent which values are present on the record.
     */
    public void setActiveSensorReadings(int[] activeSensorReadings) {
        this.activeSensorReadings = activeSensorReadings;
    }
}
