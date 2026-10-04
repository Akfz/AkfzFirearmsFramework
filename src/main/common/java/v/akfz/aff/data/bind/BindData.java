package v.akfz.aff.data.bind;

public record BindData(
		int[]   mouseBinds,
		int[]   keyboardBinds,
		boolean wheelUp,
		boolean wheelDown,
		BindMode mode,
		long    holdActivationTime,
		long    doubleClickWindow
) {
	public static BindData defaults() {
		return new BindData(
				new int[0], new int[0],
				false, false,
				BindMode.CLICK,
				0L, 250L
		);
	}
}