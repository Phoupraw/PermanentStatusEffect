package phoupraw.mcmod.permanent_status_effect.mixin;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import phoupraw.mcmod.permanent_status_effect.PSE;

@Mixin(Items.class)
abstract class MItems {
    @ModifyArg(method = "<clinit>", at = @At(value="INVOKE", target = "Lnet/minecraft/item/PotionItem;<init>(Lnet/minecraft/item/Item$Settings;)V"))
    private static Item.Settings setPotionMaxCount(Item.Settings settings) {
        return settings.maxCount(PSE.CONFIG.instance().potionMaxStack);
    }
    @ModifyArg(method = "<clinit>",at = @At(value="INVOKE",target = "Lnet/minecraft/item/SuspiciousStewItem;<init>(Lnet/minecraft/item/Item$Settings;)V"))
    private static Item.Settings setStewMaxCount(Item.Settings settings) {
        return settings.maxCount(PSE.CONFIG.instance().stewMaxStack);
    }
}
