package au.gov.ansto.bragg.echidna.ui;

import org.gumtree.gumnix.sics.core.IInstrumentProfile;
import org.gumtree.gumnix.sics.core.SicsCore;
import org.gumtree.ui.util.workbench.OpenBrowserViewAction;

@Deprecated
public class OpenMonitor3BorwserAction extends OpenBrowserViewAction {

	private String url;

	private String label;

	public OpenMonitor3BorwserAction() {
		super("Beam Monitor 3");
	}

	@Override
	public String getURL() {
//		if(url == null) {
//			IInstrumentProfile profile = SicsCore.getSicsManager().service().getCurrentInstrumentProfile();
//			if(profile != null) {
//				url = profile.getProperty(EchidnaUIConstants.PROP_BEAM_MONITOR_URL + "3");
//			}
//		}
		return url;
	}

	@Override
	public String getTitle() {
//		if(label == null) {
//			IInstrumentProfile profile = SicsCore.getSicsManager().service().getCurrentInstrumentProfile();
//			if(profile != null) {
//				label = profile.getProperty(EchidnaUIConstants.PROP_BEAM_MONITOR_LABEL + "3");
//			}
//		}
		return label;
	}

}
