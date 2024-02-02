package phoupraw.mcmod.permanent_status_effect

import dev.isxander.yacl3.api.Controller
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.controller.StringControllerBuilder
import dev.isxander.yacl3.api.utils.Dimension
import dev.isxander.yacl3.gui.AbstractWidget
import dev.isxander.yacl3.gui.YACLScreen
import dev.isxander.yacl3.gui.controllers.string.StringController
import net.minecraft.client.MinecraftClient
import net.minecraft.registry.Registry
import net.minecraft.registry.tag.TagKey
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier

class RegistryValueController<T>(option: Option<String>, val registry: Registry<T>) : StringController(option) {
    override fun formatValue(): Text {
        val input = option().pendingValue()
        val text = Text.literal(input)
        return when (isInputValidOr(input)) {
            true -> text.formatted(Formatting.GREEN)
            false -> text.formatted(Formatting.RED)
            null -> text.formatted(Formatting.YELLOW)
        }
    }

    fun isInputValidOr(input: String): Boolean? {
        if (!super.isInputValid(input)) return false
        if (!input.startsWith('#')) {
            val id = Identifier.tryParse(input) ?: return false
            return registry.containsId(id)
        }
        val id = Identifier.tryParse(input.substring(1)) ?: return false
        val tagKey = TagKey.of(registry.key, id)
        MinecraftClient.getInstance().world?.apply {
            return registry.getEntryList(tagKey).isPresent
        }
        return null
    }

    override fun isInputValid(input: String): Boolean = isInputValidOr(input) != false

    override fun provideWidget(screen: YACLScreen, widgetDimension: Dimension<Int>): AbstractWidget = RegistryValueControllerElement(this, screen, widgetDimension, true)

    inner class Builder : StringControllerBuilder {
        override fun build(): Controller<String> = this@RegistryValueController
    }
}