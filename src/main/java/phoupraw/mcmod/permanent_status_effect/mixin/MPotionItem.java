package phoupraw.mcmod.permanent_status_effect.mixin;

import net.minecraft.item.Item;
import net.minecraft.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PotionItem.class)
abstract class MPotionItem extends Item {
    public MPotionItem(Settings settings) {
        super(settings);
    }
    //@ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;<init>(Lnet/minecraft/item/Item$Settings;)V"))
    //private static Settings setMaxCount(Settings settings) {
    //    if (getClass())
    //        return settings;
    //    return settings.maxCount(PSE.CONFIG.instance().potionMaxStack);
    //}
}
