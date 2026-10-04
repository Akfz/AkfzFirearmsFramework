package v.akfz.aff.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import v.akfz.aff.ServerContext;
import v.akfz.aff.bullet.BulletManager;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftSrvMixin {
	@Inject(method = "halt", at = @At("TAIL"))
	private void aff$onShutdown(CallbackInfo ci) {
		BulletManager.getInstance().shutdown();
		ServerContext.set(null);
	}

	@Inject(method = "tickServer", at = @At("HEAD"))
	private void aff$tick(BooleanSupplier hasTimeLeft,CallbackInfo ci) {
		for (ServerLevel level : ((MinecraftServer)(Object)this).getAllLevels()) {
			BulletManager.getInstance().updateSnapshot(level);
		}
	}

	@Inject(method = "loadLevel", at = @At("TAIL"))
	private void aff$onStart(CallbackInfo ci) {
		MinecraftServer server = (MinecraftServer) (Object) this;
		ServerContext.set(server);
		BulletManager.getInstance().start(server);
	}
}