package boston.Bus.Map;

import android.content.Context;
import android.graphics.drawable.Drawable;

public interface Location {

	int getId();

	boolean hasHeading();

	int getHeading();

	Drawable getDrawable(Context context, boolean shadow, boolean isSelected);

	String makeTitle();

	double getLatitudeAsDegrees();
	
	double getLongitudeAsDegrees();

	/**
	 * @param lat2 latitude in radians
	 * @param lon2 longitude in radians
	 * @return distance in miles
	 */
	double distanceFrom(double centerLatitude, double centerLongitude);
}
