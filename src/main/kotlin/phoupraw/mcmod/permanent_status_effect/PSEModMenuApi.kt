package phoupraw.mcmod.permanent_status_effect

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import phoupraw.mcmod.permanent_status_effect.PSE.CONFIG

object PSEModMenuApi : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory {
            CONFIG.load()
            CONFIG.generateGui().generateScreen(it)
        }
    }
}