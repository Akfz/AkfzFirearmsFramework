package v.akfz.aff.physics.cpp;

/**
 * Call for CPP\C code, discard - exit
 */
public class NativeBallistics {
	public static native void simulateFrame(CSnapshot data);
	public static native void discard();
}
