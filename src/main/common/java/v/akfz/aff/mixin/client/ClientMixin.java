package v.akfz.aff.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import v.akfz.aff.item.GunItem;

@Mixin(MouseHandler.class)
public class ClientMixin {
	@Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
	private void aff$cancelAttack(long pWindowPointer,int pButton,int pAction,int pModifiers,CallbackInfo ci) {
		Player player = Minecraft.getInstance().player;
		if (player != null && player.getMainHandItem().getItem() instanceof GunItem && Minecraft.getInstance().screen == null) {
			ci.cancel();
		}
	}
}