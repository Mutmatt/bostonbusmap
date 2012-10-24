package boston.Bus.Map.main;

import boston.Bus.Map.data.Selection;
import boston.Bus.Map.data.UpdateArguments;

public class AdjustUIAsyncTask extends UpdateAsyncTask
{

	public AdjustUIAsyncTask(UpdateArguments arguments,
			boolean doShowUnpredictable, int maxOverlays, boolean drawCircle,
			boolean doInit, Selection selection, UpdateHandler handler) {
		super(arguments, doShowUnpredictable, maxOverlays, drawCircle,
				doInit, selection, handler);
	}

	@Override
	protected boolean doUpdate() {
		return true;
	}

	@Override
	protected boolean areUpdatesSilenced() {
		return true;
	}

}