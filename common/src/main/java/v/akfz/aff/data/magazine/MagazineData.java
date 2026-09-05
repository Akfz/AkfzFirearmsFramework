package v.akfz.aff.data.magazine;

import java.util.List;

public record MagazineData(
		String id,
		String displayName,
		int capacity,
		List<String> allowedAmmoIds
) {
}