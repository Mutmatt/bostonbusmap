package boston.Bus.Map.main;

import boston.Bus.Map.R;
import android.app.Activity;
import android.os.Bundle;

public class Instructions extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.instructions);
		
		setTitle("Instructions");
	}
}
