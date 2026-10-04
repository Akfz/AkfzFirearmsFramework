package v.akfz.aff.data.gun;

import v.akfz.aff.data.magazine.MagazineSetting;
import v.akfz.aff.gun.FireMode;

import java.util.List;

public record GunData(
		String id,
		String displayName,
		List<String> compatibleMagazineIds,
		List<String> compatibleAmmoIds, // если без магазина
		int magazineCapacity, // если без магазина
		long fireRate, // в мин
		boolean hasChamber,
		double recoilBase,
		double spreadBase,
		int burstTime, //в тиках
		FireMode[] fireMods, //разрешенные
		int burstSize,
		MagazineSetting magazineSetting // определяет можно ли заряжать магазинами,
) {
}
