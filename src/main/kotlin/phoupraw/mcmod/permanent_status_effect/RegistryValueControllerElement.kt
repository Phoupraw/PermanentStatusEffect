package phoupraw.mcmod.permanent_status_effect

import dev.isxander.yacl3.api.utils.Dimension
import dev.isxander.yacl3.gui.YACLScreen
import dev.isxander.yacl3.gui.controllers.string.IStringController
import dev.isxander.yacl3.gui.controllers.string.StringControllerElement
import net.minecraft.text.Text

class RegistryValueControllerElement(control: IStringController<*>, screen: YACLScreen, dim: Dimension<Int>, instantApply: Boolean) : StringControllerElement(control, screen, dim, instantApply) {
    override fun getValueText(): Text =
        if (inputFieldFocused || inputField.isNotEmpty()) super.getValueText()
        else Text.translatable("yacl3.config.permanent_status_effect.RegistryValueControllerElement.emptyText")
}