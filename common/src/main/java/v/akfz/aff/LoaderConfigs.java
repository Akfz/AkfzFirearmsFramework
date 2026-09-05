package v.akfz.aff;

import v.akfz.aff.data.bind.KeyBinds;

public class LoaderConfigs {
	public static final LoaderConfigs INSTANCE = new LoaderConfigs();
	private boolean ready = false;
	private LoaderConfigs(){}

	public KeyBinds binds;

	//TODO
	public void load() {
		binds = new KeyBinds();
		//TODO LOAD

		binds.customBinds.add(binds.AIM);
		binds.customBinds.add(binds.CHAMBER);
		binds.customBinds.add(binds.CHANGE_FIREMOD);
		binds.customBinds.add(binds.CHECK);
		binds.customBinds.add(binds.RELOAD);
		binds.customBinds.add(binds.UNLOAD);
		binds.customBinds.add(binds.SHOOT);

		ready = true;
	}

	public boolean isReady() {
		return this.ready;
	}

	//TODO
	public void save() {

	}
}
