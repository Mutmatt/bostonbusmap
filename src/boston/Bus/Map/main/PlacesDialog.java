package boston.Bus.Map.main;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import boston.Bus.Map.R;
import boston.Bus.Map.ui.TextViewBinder;
import android.app.Activity;
import android.os.Bundle;
import android.text.Spanned;
import android.text.SpannedString;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class PlacesDialog extends Activity {

	private ListView listView;
	
	public static final String textKey = "intersectionName";
	
	public static final String extrasIntersectionNames = "intersectionNames";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.places_dialog);
		
		listView = (ListView)findViewById(R.id.placesDialogListView);

		String[] intersectionNames = getIntent().getExtras().getStringArray(extrasIntersectionNames);
		
		//TODO: 
		
		List<Map<String, Spanned>> data = Lists.newArrayList();
		for (String name : intersectionNames) {
			Map<String, Spanned> map = Maps.newHashMap();
			map.put(textKey, new SpannedString(name));
			data.add(map);
		}
		SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.places_dialog_row,
				new String[]{textKey},
				new int[] {R.id.places_dialog_text});
		
		adapter.setViewBinder(new TextViewBinder());
		listView.setAdapter(adapter);
		
	}
}
