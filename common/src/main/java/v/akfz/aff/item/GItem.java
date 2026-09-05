package v.akfz.aff.item;

import net.minecraft.server.level.ServerPlayer;

public interface GItem {
	void useByKeyEvent(String GIS_ID,ServerPlayer pPlayer, boolean triggered);
}
