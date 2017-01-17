package patryk.zmijewski.polsl.pl.finalproject.view;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import patryk.zmijewski.polsl.pl.finalproject.R;

/**
 * Created by Patryk on 05.01.2017.
 */

public class SensorPreviewAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private int[] visibleSensorReadings;
    private Context mContext;

    public SensorPreviewAdapter(Context context, int[] sensorReadings ) {
        super();
        mContext = context;
        visibleSensorReadings = sensorReadings;
        mInflater = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        int count =0;
        for(int i =0;i<visibleSensorReadings.length;i++){
            if(visibleSensorReadings[i]!=-1){
                count++;
            }
        }
        return count;
    }

    @Override
    public Object getItem(int position) {
        return visibleSensorReadings[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflater.inflate(R.layout.sensor_reading, null);
            viewHolder = new ViewHolder();
            viewHolder.score = (TextView) view.findViewById(R.id.scoreTV);
            viewHolder.name = (TextView) view.findViewById(R.id.readingNameTV);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }




        switch (visibleSensorReadings[position]) {
            case (-1): {
                break;
            }
            case (0): {
                viewHolder.name.setText("Delta");
                viewHolder.name.setTextColor(Color.YELLOW);
                viewHolder.score.setText("0");
                viewHolder.score.setTextColor(Color.YELLOW);
                viewHolder.score.setId(R.id.delta_score);
                break;
            }
            case (1): {
                viewHolder.name.setText("Theta");
                viewHolder.name.setTextColor(Color.YELLOW);
                viewHolder.score.setText("0");
                viewHolder.score.setTextColor(Color.YELLOW);
                viewHolder.score.setId(R.id.theta_score);
                break;
            }
            case (2): {
                viewHolder.name.setText("Low alpha");
                viewHolder.name.setTextColor(Color.GREEN);
                viewHolder.score.setText("0");
                viewHolder.score.setTextColor(Color.GREEN);
                viewHolder.score.setId(R.id.low_alpha_score);
                break;
            }
            case (3): {
                viewHolder.name.setText("High alpha");
                viewHolder.name.setTextColor(Color.GREEN);
                viewHolder.score.setText("0");
                viewHolder.score.setTextColor(Color.GREEN);
                viewHolder.score.setId(R.id.high_alpha_score);
                break;
            }
            case (4): {
                viewHolder.name.setText("Low beta");
                viewHolder.name.setTextColor(Color.MAGENTA);
                viewHolder.score.setText("0");
                viewHolder.score.setTextColor(Color.MAGENTA);
                viewHolder.score.setId(R.id.low_beta_score);
                break;
            }
            case (5): {
                viewHolder.name.setText("High beta");
                viewHolder.name.setTextColor(Color.MAGENTA);
                viewHolder.score.setText("0");
                viewHolder.score.setTextColor(Color.MAGENTA);
                viewHolder.score.setId(R.id.high_beta_score);
                break;
            }
            case (6): {
                viewHolder.name.setText("Low gamma");
                viewHolder.name.setTextColor(Color.CYAN);
                viewHolder.score.setText("0");
                viewHolder.score.setTextColor(Color.CYAN);
                viewHolder.score.setId(R.id.low_gamma_score);
                break;
            }
            case (7): {
                viewHolder.name.setText("Medium gamma");
                viewHolder.name.setTextColor(Color.CYAN);
                viewHolder.score.setText("0");
                viewHolder.score.setTextColor(Color.CYAN);
                viewHolder.score.setId(R.id.medium_gamma_score);
                break;
            }
            case (8): {
                viewHolder.name.setText("Attention");
                viewHolder.name.setTextColor(Color.RED);
                viewHolder.score.setText("0");
                viewHolder.score.setTextColor(Color.RED);
                viewHolder.score.setId(R.id.attention_score);
                break;
            }
            case (9): {
                viewHolder.name.setText("Meditation");
                viewHolder.name.setTextColor(Color.BLUE);
                viewHolder.score.setText("0");
                viewHolder.score.setTextColor(Color.BLUE);
                viewHolder.score.setId(R.id.meditation_score);
                break;
            }
        }




        return view;
    }

    static class ViewHolder {
        TextView score;
        TextView name;
    }
}
