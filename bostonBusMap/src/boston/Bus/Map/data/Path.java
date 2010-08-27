package boston.Bus.Map.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import boston.Bus.Map.util.Box;
import boston.Bus.Map.util.CanBeSerialized;

import android.os.Parcel;
import android.os.Parcelable;

public class Path implements CanBeSerialized
{
	private final float[] points;
	private final int id;
	
	public Path(int id, ArrayList<Float> points)
	{
		this.id = id;
		this.points = new float[points.size()];
		for (int i = 0; i < points.size(); i++)
		{
			this.points[i] = points.get(i);
		}
	}
	
	public int getId()
	{
		return id;
	}

	@Override
	public void serialize(Box dest) throws IOException {
		dest.writeInt(id);
		dest.writeInt(points.length);
		for (float f : points)
		{
			dest.writeFloat(f);
		}
	}
	
	public Path(Box source) throws IOException {
		id = source.readInt();

		int size = source.readInt();
		points = new float[size];
		for (int i = 0; i < size; i++)
		{
			points[i] = source.readFloat();
		}
	}

	public float getPointLat(int i) {
		return points[i * 2];
	}
	public double getPointLon(int i) {
		return points[i*2 + 1];
	}

	public int getPointsSize() {
		return points.length / 2;
	}
}
