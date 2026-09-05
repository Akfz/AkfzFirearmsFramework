package v.akfz.aff.gun.ammo;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

public class Magazine {
	private final int maxCapacity;
	private final Deque<AmmoType> bullets;

	public Magazine(int maxCapacity) {
		this.maxCapacity = maxCapacity;
		this.bullets = new ArrayDeque<>(maxCapacity);
	}

	public boolean addBullet(AmmoType ammo) {
		if (bullets.size() >= maxCapacity) return false;
		bullets.addLast(ammo);
		return true;
	}

	public AmmoType removeBullet() {
		return bullets.pollFirst();
	}

	public AmmoType peekBullet() {
		return bullets.peekFirst();
	}

	public int getCurrentCount() {
		return bullets.size();
	}

	public int getMaxCapacity() {
		return maxCapacity;
	}

	public boolean isEmpty() {
		return bullets.isEmpty();
	}

	public boolean isFull() {
		return bullets.size() >= maxCapacity;
	}

	public void clear() {
		bullets.clear();
	}

	public Collection<AmmoType> getBullets() {
		return bullets;
	}
}