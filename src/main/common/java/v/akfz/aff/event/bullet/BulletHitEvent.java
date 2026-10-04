package v.akfz.aff.event.bullet;

import v.akfz.aslib.event.api.Cancellable;
import v.akfz.aslib.event.api.Event;

/**
 * Calling from any hit of block or entity
 */
public class BulletHitEvent extends Event implements Cancellable {
	private final BulletHitContext ctx;
	private boolean cancelled;

	public BulletHitEvent(BulletHitContext ctxs) {
		this.ctx = ctxs;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean b) {
		cancelled = b;
	}

	public BulletHitContext getCtx() {
		return ctx;
	}
}
